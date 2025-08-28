package org.cap.gold.controllers

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.UserRecord
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.*
import io.ktor.server.auth.*
import org.cap.gold.auth.PhoneSignInRequest
import org.cap.gold.config.JwtConfig
import org.cap.gold.exceptions.*
import org.cap.gold.models.*
import org.cap.gold.repositories.UserRepository
import java.util.*
import kotlin.time.Duration.Companion.hours
import com.google.firebase.auth.FirebaseToken
import org.cap.gold.models.AdminUsers
import org.cap.gold.config.DatabaseFactory.Companion.dbQuery
import org.cap.gold.service.NotificationService
import org.cap.gold.util.validatePassword
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.update
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Controller handling all authentication related endpoints
 */
class AuthController(
    private val firebaseAuth: FirebaseAuth,
    private val userRepository: UserRepository,
    private val jwtConfig: JwtConfig,
    private val notificationService: NotificationService
) {
    companion object {
        private const val REFRESH_TOKEN_EXPIRY_DAYS = 7L
        private const val ACCESS_TOKEN_EXPIRY_HOURS = 1L
    }

    // Normalize Indian phone numbers to E.164. If no country code, prepend +91.
    // Examples:
    //  - "9876543210" -> "+919876543210"
    //  - "09876543210" -> "+919876543210"
    //  - "91XXXXXXXXXX" (12 digits) -> "+91XXXXXXXXXX"
    //  - Already in E.164 (e.g., "+919876543210") -> unchanged
    private fun normalizeIndianPhone(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return trimmed

        // Already E.164
        if (trimmed.startsWith("+")) return trimmed

        // Strip non-digits
        val digits = trimmed.filter { it.isDigit() }
        if (digits.isEmpty()) return trimmed

        // Drop leading 0 for common local formats like 0XXXXXXXXXX
        val noLeadingZero = if (digits.length == 11 && digits.startsWith("0")) digits.drop(1) else digits

        return when {
            // 10-digit local Indian mobile number
            noLeadingZero.length == 10 -> "+91$noLeadingZero"
            // 12 digits starting with 91 (missing +)
            noLeadingZero.length == 12 && noLeadingZero.startsWith("91") -> "+$noLeadingZero"
            else -> "+$noLeadingZero" // Fallback: prefix + and pass through
        }
    }

    // Verify email/password using Firebase Identity Toolkit (signInWithPassword)
    // Returns true if credentials are valid, false otherwise. Does not persist tokens server-side.
    private suspend fun verifyWithFirebaseIdentity(email: String, password: String, apiKey: String): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val url = URL("https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=$apiKey")
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Accept", "application/json")
                    connectTimeout = 8000
                    readTimeout = 8000
                }

                val payload = """{"email":"$email","password":"$password","returnSecureToken":true}"""
                conn.outputStream.use { os ->
                    val bytes = payload.toByteArray(StandardCharsets.UTF_8)
                    os.write(bytes)
                }

                val code = conn.responseCode
                // 200 indicates valid credentials; 400 typically means INVALID_PASSWORD or EMAIL_NOT_FOUND
                code == HttpStatusCode.OK.value
            } catch (_: Exception) {
                false
            }
        }

    // Helper function to handle exceptions consistently
    private suspend fun ApplicationCall.handleException(
        e: Exception,
        defaultMessage: String = "An error occurred"
    ) {
        when (e) {
            is BadRequestException ->
                respond(HttpStatusCode.BadRequest, ErrorResponse(e.message ?: "Invalid request"))
            is UserNotFoundException ->
                respond(HttpStatusCode.NotFound, ErrorResponse(e.message ?: "User not found"))
            is UserAlreadyExistsException ->
                respond(HttpStatusCode.Conflict, ErrorResponse(e.message ?: "User already exists"))
            is InvalidCredentialsException ->
                respond(HttpStatusCode.Unauthorized, ErrorResponse("Invalid credentials"))
            is InvalidRefreshTokenException ->
                respond(HttpStatusCode.Unauthorized, ErrorResponse("Invalid or expired refresh token"))
            is ResourceConflictException ->
                respond(HttpStatusCode.Conflict, ErrorResponse(e.message ?: "Resource conflict"))
            is FirebaseAuthException -> {
                val code = e.errorCode.name
                // Map credential issues to 401 Unauthorized with a friendly message
                val (status, message) = when (code) {
                    "invalid-credential", "user-not-found" ->
                        HttpStatusCode.Unauthorized to "Invalid email or password"
                    "invalid-id-token", "id-token-expired", "id-token-revoked" ->
                        HttpStatusCode.Unauthorized to "Invalid or expired token"
                    "email-already-exists" -> HttpStatusCode.Conflict to "Email already in use"
                    "phone-number-already-exists" -> HttpStatusCode.Conflict to "Phone number already in use"
                    "invalid-argument" -> HttpStatusCode.BadRequest to "Invalid request data"
                    else -> HttpStatusCode.BadRequest to defaultMessage
                }
                respond(status, ErrorResponse(message))
            }
            else -> {
                e.printStackTrace()
                respond(HttpStatusCode.InternalServerError, ErrorResponse(defaultMessage))
            }
        }
    }

    /**
     * Register all authentication routes
     */
    fun Route.authRoutes() {
        // Health check endpoint
        get("/health") {
            call.respond(mapOf("st" to "o"))
        }

        route("/auth") {
            // Placeholder Firebase Web API Key (required later for Identity Toolkit signInWithPassword)
            val firebaseWebApiKey: String? = System.getenv("FIREBASE_WEB_API_KEY")

            // Sign in with email and password (placeholder verification until API key is configured)
            post("/signin/email") {
                val request = try {
                    call.receive<org.cap.gold.auth.EmailSignInRequest>().also { req ->
                        if (req.email.isBlank() || req.password.isBlank()) {
                            throw BadRequestException("Phone and password are required")
                        }
                    }
                } catch (e: Exception) {
                    call.handleException(e, "Invalid request format")
                    return@post
                }

                try {
                    if (firebaseWebApiKey.isNullOrBlank()) {
                        // Placeholder: without API key, we cannot verify password. Return 400 for now.
                        throw BadRequestException("Server not configured for email/password login (missing FIREBASE_WEB_API_KEY)")
                    }

                    // Verify credentials using Firebase Identity Toolkit signInWithPassword
                    val verified = verifyWithFirebaseIdentity(request.email+ "@test.com", request.password, firebaseWebApiKey)
                    if (!verified) {
                        throw InvalidCredentialsException()
                    }

                    // Fetch user by email using Firebase Admin SDK
                    val record = firebaseAuth.getUserByEmail(request.email+ "@test.com")
                    val user = org.cap.gold.models.User(
                        id = record.uid,
                        phoneNumber = record.phoneNumber ?: "",
                        email = record.email ,
                        displayName = record.displayName,
                        photoUrl = record.photoUrl,
                        emailVerified = record.isEmailVerified,
                        customClaims = record.customClaims ?: emptyMap()
                    )

                    // Save device token for admins (role == 0) if provided
                    val role = (record.customClaims?.get("role") as? Number)?.toInt()
                    val token = request.deviceToken?.trim()
                    if (role == 0 && !token.isNullOrBlank()) {
                        dbQuery {
                            val updated = AdminUsers.update({ AdminUsers.userId eq record.uid }) {
                                it[fireDeviceToken] = token
                            }
                            if (updated == 0) {
                                AdminUsers.insert {
                                    it[userId] = record.uid
                                    it[fireDeviceToken] = token
                                }
                            }
                        }
                    }

                    // Build tokens (JWT + refresh) as before
                    val accessToken = jwtConfig.generateAccessToken(userId = user.id, roles = emptyList())
                    val refreshToken = jwtConfig.generateRefreshToken(user.id)

                    call.respond(
                        HttpStatusCode.OK,
                        AuthResponse(
                            user = UserResponse.fromUser(user),
                            tokens = Tokens(
                                accessToken = accessToken,
                                refreshToken = refreshToken,
                                expiresIn = ACCESS_TOKEN_EXPIRY_HOURS.hours.inWholeSeconds.toInt()
                            )
                        )
                    )
                } catch (e: Exception) {
                    call.handleException(e, "Sign in failed")
                }
            }

            // Get current authenticated user using access token
            get("/me") {
                try {
                    // Extract Bearer token from Authorization header
                    val authHeader = call.request.headers[HttpHeaders.Authorization]
                    val token = authHeader?.removePrefix("Bearer ")?.trim()
                    if (token.isNullOrBlank()) {
                        call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Missing Authorization header"))
                        return@get
                    }

                    // Validate access token
                    val decoded = jwtConfig.validateToken(token)
                    if (decoded == null) {
                        call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Invalid or expired token"))
                        return@get
                    }

                    val userId = decoded.subject
                    // Fetch user from Firebase
                    val record = firebaseAuth.getUser(userId)
                    val user = org.cap.gold.models.User(
                        id = record.uid,
                        phoneNumber = record.phoneNumber ?: "",
                        email = record.email,
                        displayName = record.displayName,
                        photoUrl = record.photoUrl,
                        emailVerified = record.isEmailVerified,
                        customClaims = record.customClaims ?: emptyMap()
                    )

                    // Optionally rotate tokens; for simplicity, issue fresh tokens
                    val newAccessToken = jwtConfig.generateAccessToken(userId = user.id, roles = emptyList())
                    val newRefreshToken = jwtConfig.generateRefreshToken(user.id)

                    call.respond(
                        HttpStatusCode.OK,
                        AuthResponse(
                            user = UserResponse.fromUser(user),
                            tokens = Tokens(
                                accessToken = newAccessToken,
                                refreshToken = newRefreshToken,
                                expiresIn = ACCESS_TOKEN_EXPIRY_HOURS.hours.inWholeSeconds.toInt()
                            )
                        )
                    )
                } catch (e: Exception) {
                    call.handleException(e, "Failed to get current user")
                }
            }

            // Refresh access token using a valid refresh token
            post("/refresh") {
                try {
                    // Simple DTOs for refresh flow
                    @kotlinx.serialization.Serializable
                    data class RefreshTokenRequest(val refreshToken: String)
                    @kotlinx.serialization.Serializable
                    data class RefreshTokensResponse(
                        val accessToken: String,
                        val refreshToken: String,
                        val expiresIn: Int
                    )

                    val body = call.receive<RefreshTokenRequest>()
                    val provided = body.refreshToken.trim()
                    if (provided.isBlank()) {
                        call.respond(HttpStatusCode.BadRequest, ErrorResponse("Missing refresh token"))
                        return@post
                    }

                    // Validate refresh token using JwtConfig
                    val decoded = jwtConfig.validateToken(provided)
                    if (decoded == null || decoded.getClaim("type").asString() != "refresh") {
                        call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Invalid or expired refresh token"))
                        return@post
                    }

                    val userId = decoded.subject
                    if (userId.isNullOrBlank()) {
                        call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Invalid token subject"))
                        return@post
                    }

                    // Mint new tokens (rotate refresh token)
                    val accessToken = jwtConfig.generateAccessToken(userId = userId, roles = emptyList())
                    val refreshToken = jwtConfig.generateRefreshToken(userId)

                    call.respond(
                        HttpStatusCode.OK,
                        RefreshTokensResponse(
                            accessToken = accessToken,
                            refreshToken = refreshToken,
                            expiresIn = ACCESS_TOKEN_EXPIRY_HOURS.hours.inWholeSeconds.toInt()
                        )
                    )
                } catch (e: Exception) {
                    call.handleException(e, "Failed to refresh token")
                }
            }

            // Sign up with email + phone + password (no verification)
            post("/signup/email") {
                val request = try {
                    call.receive<org.cap.gold.auth.EmailSignUpRequest>().also { req ->
                        if (req.email.isBlank() || req.password.isBlank() || req.phoneNumber.isBlank()) {
                            throw BadRequestException("Email, phone number and password are required")
                        }
                        if (req.password.length < 6) {
                            throw BadRequestException("Password must be at least 6 characters long")
                        }
                    }
                } catch (e: Exception) {
                    call.handleException(e, "Invalid request format")
                    return@post
                }

                    println("shopName: ${request.shopName}")

                try {
                    val normalizedPhone = normalizeIndianPhone(request.phoneNumber)
                    // Create user in Firebase with email, phone, and password
                    val userRecord = firebaseAuth.createUser(
                        UserRecord.CreateRequest()
                            .setEmail(request.email + "@test.com")
                            .setPassword(request.password)
                            .setPhoneNumber(normalizedPhone)
                            .setDisplayName(request.displayName)
                    )
                    // Default role = 2 (unapproved). Save in custom claims, include shopName when provided.
                    val claims = buildMap<String, Any> {
                        put("role", 2)
                        val shop = request.shopName?.trim()
                        if (!shop.isNullOrEmpty()) put("shopName", shop)
                    }
                    firebaseAuth.setCustomUserClaims(userRecord.uid, claims)

                    // Build tokens (JWT + refresh)
                    val accessToken = jwtConfig.generateAccessToken(
                        userId = userRecord.uid,
                        roles = emptyList()
                    )
                    val refreshToken = jwtConfig.generateRefreshToken(userRecord.uid)

                    val user = org.cap.gold.models.User(
                        id = userRecord.uid,
                        phoneNumber = request.phoneNumber,
                        email = request.email,
                        displayName = request.displayName,
                        photoUrl = userRecord.photoUrl,
                        emailVerified = userRecord.isEmailVerified,
                        customClaims = claims
                    )

                    call.respond(
                        HttpStatusCode.Created,
                        AuthResponse(
                            user = UserResponse.fromUser(user),
                            tokens = Tokens(
                                accessToken = accessToken,
                                refreshToken = refreshToken,
                                expiresIn = ACCESS_TOKEN_EXPIRY_HOURS.hours.inWholeSeconds.toInt()
                            )
                        )
                    )
                    notificationService.sendAdminBroadcastAsync(
                        title = "A New user Just Registered",
                        body = user.displayName + " has just registered take a action "
                    )
                } catch (e: Exception) {
                    println(e.message)
                    call.handleException(e, "Registration failed")
                }
            }
            // Sign in with phone and password
            post("/signin/phone") {
                val request = try {
                    call.receive<PhoneSignInRequest>().also { req ->
                        if (req.phoneNumber.isBlank() || req.password.isBlank()) {
                            throw BadRequestException("Phone number and password are required")
                        }
                    }
                } catch (e: Exception) {
                    call.handleException(e, "Invalid request format")
                    return@post
                }

                try {
                    // Get user by phone number
                    val user = userRepository.getUserByPhoneNumber(request.phoneNumber)
                    
                    // In a real app, verify the password against your database
                    // For demo purposes, we'll just check if password is not empty
                    if (request.password.isBlank()) {
                        throw InvalidCredentialsException()
                    }
                    
                    // Attempt to fetch Firebase record to read custom claims and UID
                    try {
                        val record = try {
                            firebaseAuth.getUser(user.id)
                        } catch (e: Exception) {
                            // Fallback to lookup by phone
                            firebaseAuth.getUserByPhoneNumber(request.phoneNumber)
                        }
                        val role = (record.customClaims?.get("role") as? Number)?.toInt()
                        val token = request.deviceToken?.trim()
                        if (role == 0 && !token.isNullOrBlank()) {
                            dbQuery {
                                val updated = AdminUsers.update({ AdminUsers.userId eq record.uid }) {
                                    it[fireDeviceToken] = token
                                }
                                if (updated == 0) {
                                    AdminUsers.insert {
                                        it[userId] = record.uid
                                        it[fireDeviceToken] = token
                                    }
                                }
                            }
                        }
                    } catch (_: Exception) {
                        // If we cannot fetch claims, skip admin token save silently
                    }

                    // Generate tokens
                    val customToken = firebaseAuth.createCustomToken(user.id)
                    val refreshToken = "${user.id}:${UUID.randomUUID()}"
                    
                    call.respond(
                        HttpStatusCode.OK,
                        AuthResponse(
                            user = UserResponse.fromUser(user),
                            tokens = Tokens(
                                accessToken = customToken,
                                refreshToken = refreshToken,
                                expiresIn = ACCESS_TOKEN_EXPIRY_HOURS.hours.inWholeSeconds.toInt()
                            )
                        )
                    )
                } catch (e: Exception) {
                    call.handleException(e, "Sign in failed")
                }
            }

            // Register new user with phone
            post("/signup/phone") {
                val request = try {
                    call.receive<PhoneSignUpRequest>().also { req ->
                        if (req.phoneNumber.isBlank() || req.password.isBlank()) {
                            throw BadRequestException("Phone number and password are required")
                        }
                        if (req.password.length < 6) {
                            throw BadRequestException("Password must be at least 6 characters long")
                        }
                    }
                } catch (e: Exception) {
                    call.handleException(e, "Invalid request format")
                    return@post
                }

                try {
                    // Create user in Firebase
                    val userRecord = firebaseAuth.createUser(
                        UserRecord.CreateRequest()
                            .setPhoneNumber(request.phoneNumber)
                            .setDisplayName(request.displayName)
                            .setPassword(request.password)
                            .setEmail(request.phoneNumber)
                    )
                    
                    // Create user in our database
                    val user = userRepository.createUser(
                        phoneNumber = request.phoneNumber,
                        displayName = request.displayName,
                        password = request.password
                    )
                    
                    // Generate tokens
                    val accessToken = jwtConfig.generateAccessToken(
                        userId = user.id,
                        roles = emptyList() // New users have no roles by default
                    )
                    val refreshToken = jwtConfig.generateRefreshToken(user.id)
                    
                    // Return the created user and tokens
                    call.respond(
                        HttpStatusCode.Created,
                        AuthResponse(
                            user = UserResponse.fromUser(user),
                            tokens = Tokens(
                                accessToken = accessToken,
                                refreshToken = refreshToken,
                                expiresIn = ACCESS_TOKEN_EXPIRY_HOURS.hours.inWholeSeconds.toInt()
                            )
                        )
                    )
                } catch (e: Exception) {
                    call.handleException(e, "Registration failed")
                }
            }

            // Refresh access token
            post("/refresh") {
                val request = try {
                    call.receive<RefreshTokenRequest>()
                } catch (e: Exception) {
                    call.handleException(BadRequestException("Invalid request format"))
                    return@post
                }

                try {
                    // Validate the refresh token
                    val decodedToken = jwtConfig.validateToken(request.refreshToken)
                        ?: throw InvalidRefreshTokenException()
                        
                    // Verify it's a refresh token
                    if (decodedToken.getClaim("type").asString() != "refresh") {
                        throw InvalidRefreshTokenException()
                    }
                    
                    // Get the user ID from the token
                    val userId = decodedToken.subject
                    
                    // In a real app, you would verify the refresh token against your database
                    // and check if it's been revoked
                    
                    // Get the user
                    val user = userRepository.getUserById(userId)
                    
                    // Generate new tokens
                    val newAccessToken = jwtConfig.generateAccessToken(
                        userId = user.id,
                        roles = emptyList()
                    )
                    val newRefreshToken = jwtConfig.generateRefreshToken(user.id)
                    
                    // In a real app, update the refresh token in your database
                    
                    // Return the new tokens
                    call.respond(
                        HttpStatusCode.OK,
                        Tokens(
                            accessToken = newAccessToken,
                            refreshToken = newRefreshToken,
                            expiresIn = ACCESS_TOKEN_EXPIRY_HOURS.hours.inWholeSeconds.toInt()
                        )
                    )
                } catch (e: Exception) {
                    call.handleException(e, "Failed to refresh token")
                }
            }

            // Send password reset SMS
            post("/password/reset/sms") {
                val request = try {
                    call.receive<PasswordResetRequest>().also { req ->
                        if (req.phoneNumber.isBlank()) {
                            throw BadRequestException("Phone number is required")
                        }
                    }
                } catch (e: Exception) {
                    call.handleException(e, "Invalid request format")
                    return@post
                }

                try {
                    // In a real app, you would:
                    // 1. Verify the phone number exists
                    // 2. Generate a password reset code
                    // 3. Send an SMS with the code
                    // 4. Store the code in your database with an expiry time
                    
                    // For demo purposes, we'll just verify the user exists
                    try {
                        val user = userRepository.getUserByPhoneNumber(request.phoneNumber)
                        
                        // In a real app, you'd send an SMS here
                        // For example: sendResetSms(user.phoneNumber, resetCode)
                    } catch (e: UserNotFoundException) {
                        // Don't reveal if the user exists or not for security reasons
                    }
                    
                    // Return a success response (don't include the code in production)
                    call.respond(
                        HttpStatusCode.OK,
                        mapOf(
                            "success" to true,
                            "message" to "If an account exists with this phone number, a password reset link has been sent"
                        )
                    )
                } catch (e: Exception) {
                    call.handleException(e, "Failed to process password reset request")
                }
            }

            // Get current user profile (manual token validation)
            get("/me") {
                // Get current user from the JWT token
                val token = call.request.headers[HttpHeaders.Authorization]?.removePrefix("Bearer ")?.trim()
                    ?: throw UnauthorizedException("Missing or invalid Authorization header")
                    
                val decodedToken = jwtConfig.validateToken(token)
                    ?: throw UnauthorizedException("Invalid or expired token")
                    
                val userId = decodedToken.subject
                val user = userRepository.getUserById(userId)
                
                call.respond(
                    HttpStatusCode.OK,
                    UserResponse.fromUser(user)
                )
            }

            // Update user profile
            put("/me") {
                // Get current user ID from the token
                val token = call.request.headers[HttpHeaders.Authorization]?.removePrefix("Bearer ")?.trim()
                    ?: throw UnauthorizedException("Missing or invalid Authorization header")
                    
                val decodedToken = jwtConfig.validateToken(token)
                    ?: throw UnauthorizedException("Invalid or expired token")
                    
                val userId = decodedToken.subject
                
                val request = call.receive<UpdateUserRequest>()
                val updates = mutableMapOf<String, Any>()
                val user = firebaseAuth.getUser(userId)

                firebaseWebApiKey?.let {

                    request.currentPassword?.let { password ->
                        if(!verifyWithFirebaseIdentity(user.email,password,it)) {
                            throw InvalidCredentialsException()
                        }
                    }?: throw UnauthorizedException("Required current password")
                }?: throw UnauthorizedException("Invalid or expired token")
                
                request.displayName?.let { updates["displayName"] = it }
                request.email?.let { updates["email"] = it }
                request.phoneNumber?.let { updates["email"] = it + "@test.com"
                    updates["phoneNumber"] = it }
                request.photoUrl?.let { updates["photoUrl"] = it }
                request.shopName?.let { updates["CustomToken"] = user.customClaims
                user.customClaims["shopName"] = it}
                
                // Update user in Firebase
                val updateRequest = UserRecord.UpdateRequest(userId)
                
                request.displayName?.let { updateRequest.setDisplayName(it) }
                request.photoUrl?.let { updateRequest.setPhotoUrl(it) }
                
                firebaseAuth.updateUser(updateRequest)
                
                // Update user in our database
                val updatedUser = userRepository.updateUser(userId, updates)
                call.respond(HttpStatusCode.OK, UserResponse.fromUser(updatedUser))
            }

            // Change password
            post("/password/change") {
                // Get current user ID from the token
                val token = call.request.headers[HttpHeaders.Authorization]?.removePrefix("Bearer ")?.trim()
                    ?: throw UnauthorizedException("Missing or invalid Authorization header")
                    
                val decodedToken = jwtConfig.validateToken(token)
                    ?: throw UnauthorizedException("Invalid or expired token")
                    
                val userId = decodedToken.subject
                val user = firebaseAuth.getUser(userId)


                val request = call.receive<ChangePasswordRequest>()
                firebaseWebApiKey?.let {

                    if(!verifyWithFirebaseIdentity(user.email,request.currentPassword,it))
                    {
                        throw InvalidCredentialsException()
                    }
                }
                if (request.newPassword.isBlank() || request.newPassword.length < 6) {
                    throw BadRequestException("New password must be at least 6 characters long")
                }


                // In a real app, verify the current password before changing
                // For demo, we'll just update the password
                val updatedUser = userRepository.updateUser(
                    userId,
                    mapOf("password" to request.newPassword)
                )

                call.respond(HttpStatusCode.OK, UserResponse.fromUser(updatedUser))

            }

            // Delete account
            delete("/me") {
                // Get current user ID from the token
                val token = call.request.headers[HttpHeaders.Authorization]?.removePrefix("Bearer ")?.trim()
                    ?: throw UnauthorizedException("Missing or invalid Authorization header")
                    
                val decodedToken = jwtConfig.validateToken(token)
                    ?: throw UnauthorizedException("Invalid or expired token")
                    
                val userId = decodedToken.subject
                
                try {
                    // Delete from Firebase
                    firebaseAuth.deleteUser(userId)
                } catch (e: FirebaseAuthException) {
                    if (e.errorCode.name != "user-not-found") {
                        throw e
                    }
                    // User not found in Firebase, continue with database cleanup
                    call.handleException(e, "Failed to delete account")
                }
            }

            // (Removed trailing authenticate { } block; routes already perform manual JWT validation.)
        }
    }
}

// Request and response data classes
@kotlinx.serialization.Serializable
data class PhoneSignInRequest(
    val phoneNumber: String,
    val password: String
)

@kotlinx.serialization.Serializable
data class PhoneSignUpRequest(
    val phoneNumber: String,
    val password: String,
    val displayName: String? = null
)

@kotlinx.serialization.Serializable
data class RefreshTokenRequest(
    val refreshToken: String
)

@kotlinx.serialization.Serializable
data class PasswordResetRequest(
    val phoneNumber: String
)

@kotlinx.serialization.Serializable
data class ChangePasswordRequest(
    val currentPassword: String,
    val newPassword: String
)

@kotlinx.serialization.Serializable
data class AuthResponse(
    val user: UserResponse,
    val tokens: Tokens
)

@kotlinx.serialization.Serializable
data class Tokens(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Int
)

@kotlinx.serialization.Serializable
data class ErrorResponse(
    val error: String,
    val code: Int? = null
)
