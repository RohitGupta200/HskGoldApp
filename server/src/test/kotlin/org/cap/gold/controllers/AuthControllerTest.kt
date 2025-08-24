package org.cap.gold.controllers

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserRecord
import com.google.firebase.auth.UserRecord.CreateRequest
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.config.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import io.mockk.*
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.cap.gold.config.JwtConfig
import org.cap.gold.exceptions.*
import org.cap.gold.models.*
import org.cap.gold.repositories.UserRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.koin.dsl.module
import org.koin.ktor.ext.inject
import org.koin.test.*
import org.koin.test.mock.declareMock
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import java.time.Instant
import kotlin.test.*

// Test DTOs for JSON serialization
@Serializable
data class TestUserResponse(
    val id: String,
    val phoneNumber: String? = null,
    val displayName: String? = null,
    val email: String? = null,
    val photoUrl: String? = null,
    val disabled: Boolean = false,
    val emailVerified: Boolean = false
)

// duplicate class previously removed

@Serializable
data class TestSignUpRequest(
    val phoneNumber: String,
    val displayName: String? = null,
    val email: String? = null,
    val password: String
)

@Serializable
data class TestSignInRequest(
    val phoneNumber: String,
    val password: String
)

@Serializable
data class TestRefreshTokenRequest(
    val refreshToken: String
)

// Test utilities

// Helper extension function for JSON serialization on client requests
inline fun <reified T> HttpRequestBuilder.jsonBody(obj: T) {
    contentType(ContentType.Application.Json)
    setBody(Json.encodeToString(obj))
}

// Helper extension function for JSON deserialization from responses
suspend inline fun <reified T> HttpResponse.toData(): T {
    return Json.decodeFromString(bodyAsText())
}

data class TestAuthResponse(
    val user: TestUserResponse,
    val tokens: Tokens
)

@ExtendWith(MockKExtension::class)
class AuthControllerTest {
    @MockK
    private lateinit var mockFirebaseAuth: FirebaseAuth
    
    @MockK
    private lateinit var mockUserRepository: UserRepository
    
    private lateinit var jwtConfig: JwtConfig
    
    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this, relaxed = true)
        
        val testConfig = MapApplicationConfig(
            "ktor.environment" to "test",
            "jwt.secret" to "test-secret-very-secure-and-long-enough-for-hs256",
            "jwt.issuer" to "test-issuer",
            "jwt.audience" to "test-audience",
            "jwt.realm" to "test-realm",
            "jwt.accessTokenExpiration" to "3600",
            "jwt.refreshTokenExpiration" to "604800"
        )
        
        jwtConfig = JwtConfig(testConfig)
    }
    
    @AfterEach
    fun tearDown() {
        unmockkAll()
    }
    
    private fun createTestUser() = User(
        id = "test-user-123",
        phoneNumber = "+1234567890",
        displayName = "Test User",
        email = "test@example.com",
        photoUrl = null,
        disabled = false,
        emailVerified = true,
        metadata = User.Metadata(
            creationTime = System.currentTimeMillis(),
            lastSignInTime = System.currentTimeMillis()
        ),
        customClaims = emptyMap()
    )

    @Test
    fun `sign up with valid data should return user and tokens`() = testApplication {
        // Given
        val testUser = createTestUser()
        
        val userRecord = mockk<UserRecord> {
            every { uid } returns testUser.id
            every { phoneNumber } returns testUser.phoneNumber
            every { displayName } returns testUser.displayName
            every { email } returns testUser.email
            every { isDisabled } returns testUser.disabled
            every { isEmailVerified } returns testUser.emailVerified
        }
        
        coEvery { 
            mockFirebaseAuth.createUser(any<CreateRequest>())
        } returns userRecord
        
        coEvery { 
            mockUserRepository.createUser(
                id = testUser.id,
                phoneNumber = testUser.phoneNumber!!,
                displayName = testUser.displayName,
                email = testUser.email
            )
        } returns testUser
        
        // When
        // configure client with JSON
        val client = createClient {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
        val response = client.post("/auth/signup/phone") {
            jsonBody(TestSignUpRequest(
                phoneNumber = testUser.phoneNumber!!,
                displayName = testUser.displayName,
                email = testUser.email,
                password = "securePassword123"
            ))
        }
        
        // Then
        assertEquals(HttpStatusCode.Created, response.status)
        val responseBody = response.toData<TestAuthResponse>()
        assertEquals(testUser.id, responseBody.user.id)
        assertNotNull(responseBody.tokens.accessToken)
        assertNotNull(responseBody.tokens.refreshToken)
    }
    
    @Test
    fun `sign in with valid credentials should return tokens`() = testApplication {
        // Given
        val testUser = createTestUser()
        
        coEvery { 
            mockUserRepository.getUserByPhoneNumber(testUser.phoneNumber!!)
        } returns testUser
        
        // When
        val client = createClient { install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) } }
        val response = client.post("/auth/signin/phone") {
            jsonBody(TestSignInRequest(
                phoneNumber = testUser.phoneNumber!!,
                password = "securePassword123"
            ))
        }
        
        // Then
        assertEquals(HttpStatusCode.OK, response.status)
        val responseBody = response.toData<TestAuthResponse>()
        assertEquals(testUser.id, responseBody.user.id)
        assertNotNull(responseBody.tokens.accessToken)
        assertNotNull(responseBody.tokens.refreshToken)
    }
    
    @Test
    fun `refresh token with valid refresh token should return new tokens`() = testApplication {
        // Given
        val testUser = createTestUser()
        val refreshToken = jwtConfig.generateRefreshToken(testUser.id)
        
        coEvery { 
            mockUserRepository.getUserById(testUser.id)
        } returns testUser
        
        // When
        val client = createClient { install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) } }
        val response = client.post("/auth/refresh") {
            jsonBody(TestRefreshTokenRequest(refreshToken))
        }
        
        // Then
        assertEquals(HttpStatusCode.OK, response.status)
        val responseBody = response.toData<Tokens>()
        assertNotNull(responseBody.accessToken)
        assertNotNull(responseBody.refreshToken)
    }
    
    @Test
    fun `get current user with valid token should return user data`() = testApplication {
        // Given
        val testUser = createTestUser()
        val accessToken = jwtConfig.generateAccessToken(testUser.id, emptyList())
        
        coEvery { 
            mockUserRepository.getUserById(testUser.id)
        } returns testUser
        
        // When
        val client = createClient { install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) } }
        val response = client.get("/auth/me") {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
        }
        
        // Then
        assertEquals(HttpStatusCode.OK, response.status)
        val responseBody = response.toData<TestUserResponse>()
        assertEquals(testUser.id, responseBody.id)
        assertEquals(testUser.phoneNumber, responseBody.phoneNumber)
        assertEquals(testUser.displayName, responseBody.displayName)
        assertEquals(testUser.email, responseBody.email)
    }
    
    
    private inline fun <reified T> String.toKotlinObject(): T {
        return Json.decodeFromString(this)
    }
}
