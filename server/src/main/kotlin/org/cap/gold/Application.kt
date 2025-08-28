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
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
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
import org.cap.gold.models.ProductImages
import org.cap.gold.models.UnapprovedProduct
import org.cap.gold.models.Categories
import org.cap.gold.models.AdminUsers
import org.cap.gold.repositories.OrderRepository
import org.cap.gold.repositories.ProductRepository
import org.cap.gold.repositories.CategoryRepository
import org.cap.gold.repositories.UserRepository
import org.cap.gold.service.NotificationService
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

    // Services
    single { NotificationService() }

    // Controllers
    single { AuthController(get(), get(), get(),get()) }
    single { ProductController(get(), get()) }
    single { CategoryController(get()) }
    single { UserController(get()) }
    single { OrderController(get(),get(),get()) }
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
            prettyPrint = false
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
            call.respond(mapOf("st" to "U"))
        }

        route("/api") {
            // Auth routes mounted under /api/auth
            authController.apply { this@route.authRoutes() }

            // Protected routes
            authenticate("auth-jwt") {
                // Product routes
                val productController: ProductController by inject()
                productController.apply { this@authenticate.productRoutes() }

                // Category routes
                val categoryController: CategoryController by inject()
                categoryController.apply { this@authenticate.categoryRoutes() }

                // Users routes
                val userController: UserController by inject()
                userController.apply { this@authenticate.userRoutes() }

                // Orders routes
                val orderController: OrderController by inject()
                orderController.apply { this@authenticate.orderRoutes() }
            }
        }
    }
}

// Main Application Module
@Suppress("unused") // Referenced in application.conf
fun Application.module() {
    // Firebase will be initialized via Koin provider (FirebaseConfig.initialize)
    
    // Initialize Database
    DatabaseFactory(environment.config)
    // Auto-create schema and run lightweight migration for newly added non-null columns
    transaction {
        // 1) Ensure 'name' column exists and is backfilled for existing rows, then enforce NOT NULL
        // Approved table
        exec("""
            ALTER TABLE products_approved ADD COLUMN IF NOT EXISTS name VARCHAR(200);
        """.trimIndent())
        exec("""
            UPDATE products_approved SET name = COALESCE(name, '') WHERE name IS NULL;
        """.trimIndent())
        exec("""
            ALTER TABLE products_approved ALTER COLUMN name SET NOT NULL;
        """.trimIndent())

        // Unapproved table
        exec("""
            ALTER TABLE products_unapproved ADD COLUMN IF NOT EXISTS name VARCHAR(200);
        """.trimIndent())
        exec("""
            UPDATE products_unapproved SET name = COALESCE(name, '') WHERE name IS NULL;
        """.trimIndent())
        exec("""
            ALTER TABLE products_unapproved ALTER COLUMN name SET NOT NULL;
        """.trimIndent())

        // 1.b) Ensure 'description' column exists, backfill, and enforce NOT NULL (both tables)
        // Approved table
        exec("""
            ALTER TABLE products_approved ADD COLUMN IF NOT EXISTS description VARCHAR(1000);
        """.trimIndent())
        exec("""
            UPDATE products_approved SET description = COALESCE(description, '') WHERE description IS NULL;
        """.trimIndent())
        exec("""
            ALTER TABLE products_approved ALTER COLUMN description SET NOT NULL;
        """.trimIndent())

        // Unapproved table
        exec("""
            ALTER TABLE products_unapproved ADD COLUMN IF NOT EXISTS description VARCHAR(1000);
        """.trimIndent())
        exec("""
            UPDATE products_unapproved SET description = COALESCE(description, '') WHERE description IS NULL;
        """.trimIndent())
        exec("""
            ALTER TABLE products_unapproved ALTER COLUMN description SET NOT NULL;
        """.trimIndent())

        // 2) Create any missing tables/columns for the rest of the schema
        SchemaUtils.createMissingTablesAndColumns(
            ProductsApproved,
            ProductsUnapproved,
            Orders,
            Categories,
            ProductImages,
            AdminUsers
        )

        // 3) Migrate product weight column type from numeric to varchar(50) if needed (both tables)
        // This block is idempotent: it only alters when the existing type is not character varying
        exec(
            """
            DO $$
            BEGIN
                IF EXISTS (
                    SELECT 1 FROM information_schema.columns
                    WHERE table_name = 'products_approved'
                      AND column_name = 'weight'
                      AND data_type <> 'character varying'
                ) THEN
                    EXECUTE 'ALTER TABLE products_approved ALTER COLUMN weight TYPE varchar(50) USING weight::varchar(50)';
                END IF;
            END $$;
            """.trimIndent()
        )

        exec(
            """
            DO $$
            BEGIN
                IF EXISTS (
                    SELECT 1 FROM information_schema.columns
                    WHERE table_name = 'products_unapproved'
                      AND column_name = 'weight'
                      AND data_type <> 'character varying'
                ) THEN
                    EXECUTE 'ALTER TABLE products_unapproved ALTER COLUMN weight TYPE varchar(50) USING weight::varchar(50)';
                END IF;
            END $$;
            """.trimIndent()
        )
    }
    
    // Configure Koin for dependency injection
    install(Koin) {
        slf4jLogger(level = org.koin.core.logger.Level.ERROR)
        modules(appModule(environment))
    }
    
    // Get dependencies
    val authController: AuthController by inject()
    val jwtConfig: JwtConfig by inject()
    
    // Configure server features
    configureCORS()
    configureSerialization()
    configureStatusPages()
    configureMonitoring()
    // Authentication: protect API with server JWTs
    install(Authentication) {
        jwt("auth-jwt") {
            realm = "capgold"
            verifier(jwtConfig.verifier)
            validate { credentials ->
                // Accept only access tokens (optional check)
                val type = credentials.payload.getClaim("type").asString()
                if (type == null || type == "access") JWTPrincipal(credentials.payload) else null
            }
            challenge { _, _ ->
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Invalid or missing token"))
            }
        }
    }
    
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
