package org.cap.gold

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseToken
import com.google.firebase.auth.UserRecord
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.UserIdPrincipal
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import org.cap.gold.config.DatabaseFactory
import org.cap.gold.config.FirebaseConfig
import org.cap.gold.config.JwtConfig
import org.cap.gold.controllers.AuthController
import org.cap.gold.controllers.UserController
import org.cap.gold.controllers.ProductController
import org.cap.gold.controllers.CategoryController
import org.cap.gold.controllers.OrderController
import org.cap.gold.dao.OrderDao
import org.cap.gold.exceptions.*
import org.cap.gold.models.CreateOrderRequest
import org.cap.gold.models.Order
import org.cap.gold.models.OrderStatus
import org.cap.gold.models.OrderStatusGroup
import org.cap.gold.models.Orders
import org.cap.gold.models.ProductsApproved
import org.cap.gold.models.ProductsUnapproved
import org.cap.gold.models.UnapprovedProduct
import org.cap.gold.models.Categories
import org.cap.gold.repositories.OrderRepository
import org.cap.gold.repositories.ProductRepository
import org.cap.gold.repositories.CategoryRepository
import org.cap.gold.repositories.UserRepository
import org.cap.gold.service.OrderService
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import org.koin.dsl.module
import org.koin.ktor.ext.inject
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger
import java.io.File
import java.io.FileInputStream
import java.time.LocalDateTime
import java.util.UUID

// Koin DI Module (requires ApplicationEnvironment)
fun appModule(env: ApplicationEnvironment) = module {
    // Firebase: ensure initialize() is called before providing auth
    single<FirebaseAuth> {
        FirebaseConfig.initialize(env.config)
        FirebaseConfig.auth
    }

    // JWT
    single { JwtConfig(env.config) }

    // Repositories
    single { UserRepository() }
    single { ProductRepository() }
    single { CategoryRepository() }
    single { OrderRepository() }

    // Controllers
    single { AuthController(get(), get(), get()) }
    single { ProductController(get()) }
    single { CategoryController(get()) }
    single { UserController(get()) }
    single { OrderController(get()) }
}

// Configure Firebase
private fun Application.configureFirebase() {
    try {
        // Try to load Firebase credentials from environment variable or config file
        val serviceAccount = System.getenv("GOOGLE_APPLICATION_CREDENTIALS")?.let { 
            FileInputStream(it) 
        } ?: run {
            val configFile = File("service-account.json")
            if (configFile.exists()) {
                FileInputStream(configFile)
            } else {
                // For development only - in production, use proper credentials
                val resource = this::class.java.classLoader.getResourceAsStream("service-account.json")
                resource ?: throw IllegalStateException("Firebase service account credentials not found")
            }
        }

        val options = FirebaseOptions.builder()
            .setCredentials(GoogleCredentials.fromStream(serviceAccount))
            .build()

        FirebaseApp.initializeApp(options)
        log.info("Firebase initialized successfully")
    } catch (e: Exception) {
        log.error("Failed to initialize Firebase", e)
        throw e
    }
}

// Configure CORS
private fun Application.configureCORS() {
    install(CORS) {
        anyHost()
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Patch)
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Authorization)
        allowNonSimpleContentTypes = true
        allowCredentials = true
        maxAgeInSeconds = 24 * 60 * 60
    }
}

// Configure Content Negotiation
private fun Application.configureSerialization() {
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
            encodeDefaults = true
        })
    }
}

// Configure Status Pages
private fun Application.configureStatusPages() {
    install(StatusPages) {
        exception<BadRequestException> { call, cause ->
            call.respond(
                HttpStatusCode.BadRequest,
                mapOf("error" to "Bad Request", "message" to (cause.message ?: "Invalid request"))
            )
        }
        exception<AuthenticationException> { call, cause ->
            call.respond(
                HttpStatusCode.Unauthorized,
                mapOf("error" to "Authentication failed", "message" to (cause.message ?: "Invalid credentials"))
            )
        }
        exception<AuthorizationException> { call, cause ->
            call.respond(
                HttpStatusCode.Forbidden,
                mapOf("error" to "Access denied", "message" to (cause.message ?: "Insufficient permissions"))
            )
        }
        exception<UserNotFoundException> { call, cause ->
            call.respond(
                HttpStatusCode.NotFound,
                mapOf("error" to "Not found", "message" to (cause.message ?: "User not found"))
            )
        }
        exception<UserAlreadyExistsException> { call, cause ->
            call.respond(
                HttpStatusCode.Conflict,
                mapOf("error" to "Conflict", "message" to (cause.message ?: "User already exists"))
            )
        }
        exception<InvalidCredentialsException> { call, _ ->
            call.respond(
                HttpStatusCode.Unauthorized,
                mapOf("error" to "Authentication failed", "message" to "Invalid credentials")
            )
        }
        exception<InvalidRefreshTokenException> { call, _ ->
            call.respond(
                HttpStatusCode.Unauthorized,
                mapOf("error" to "Authentication failed", "message" to "Invalid or expired refresh token")
            )
        }
        exception<Throwable> { call, cause ->
            this@configureStatusPages.log.error("Unhandled exception", cause)
            call.respond(
                HttpStatusCode.InternalServerError,
                mapOf(
                    "error" to "Internal Server Error",
                    "message" to "An unexpected error occurred"
                )
            )
        }
    }
}

// Configure Call Logging
private fun Application.configureMonitoring() {
    install(CallLogging) {
        level = org.slf4j.event.Level.INFO
        filter { call -> call.request.path().startsWith("/") }
    }
}

// Configure Routing
private fun Application.configureRouting(authController: AuthController) {
    routing {
        // Health check endpoint
        get("/health") {
            call.respond(mapOf("status" to "UP"))
        }

        route("/api") {
            // Product routes
            val productController: ProductController by inject()
            productController.apply { this@route.productRoutes() }

            // Category routes
            val categoryController: CategoryController by inject()
            categoryController.apply { this@route.categoryRoutes() }

            // Auth routes mounted under /api/auth
            authController.apply { this@route.authRoutes() }

            // Users routes mounted under /api/users
            val userController: UserController by inject()
            userController.apply { this@route.userRoutes() }

            // Orders routes mounted under /api/orders
            val orderController: OrderController by inject()
            orderController.apply { this@route.orderRoutes() }
        }
    }
}

// Main Application Module
@Suppress("unused") // Referenced in application.conf
fun Application.module() {
    // Firebase will be initialized via Koin provider (FirebaseConfig.initialize)
    
    // Initialize Database
    DatabaseFactory(environment.config)
    // Auto-create schema in development environments (safe idempotent call)
    transaction {
        SchemaUtils.create(
            ProductsApproved,
            ProductsUnapproved,
            Orders,
            Categories
        )
    }
    
    // Configure Koin for dependency injection
    install(Koin) {
        slf4jLogger(level = org.koin.core.logger.Level.ERROR)
        modules(appModule(environment))
    }
    
    // Get dependencies
    val authController: AuthController by inject()
    
    // Configure server features
    configureCORS()
    configureSerialization()
    configureStatusPages()
    configureMonitoring()
    
    // Configure routing
    configureRouting(authController)
}

// Main function for running the server directly
fun main(args: Array<String>) = EngineMain.main(args)

// Custom Exceptions
class AuthenticationException(override val message: String) : RuntimeException()
class AuthorizationException(override val message: String) : RuntimeException()
class NotFoundException(override val message: String) : RuntimeException()
class BadRequestException(override val message: String) : RuntimeException()

// Extension function to validate token from Authorization header
suspend fun ApplicationCall.validateToken(): FirebaseToken? {
    val token = request.header(HttpHeaders.Authorization)?.substringAfter("Bearer ")
        ?: return null
    
    return try {
        FirebaseConfig.auth.verifyIdToken(token)
    } catch (e: Exception) {
        respond(HttpStatusCode.Unauthorized, mapOf("error" to "Invalid or expired token"))
        null
    }
}
