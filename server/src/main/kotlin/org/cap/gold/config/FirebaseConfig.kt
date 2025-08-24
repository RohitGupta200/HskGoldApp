package org.cap.gold.config

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.*
import io.ktor.server.config.*
import org.cap.gold.exceptions.*
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import java.util.*

/**
 * Configuration and utility class for Firebase Authentication.
 * Handles initialization and provides methods for user management.
 */
object FirebaseConfig {
    private var isInitialized = false
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firebaseApp: FirebaseApp
    
    /**
     * Initialize Firebase Admin SDK with the provided configuration.
     * @param config Application configuration containing Firebase settings
     */
    fun initialize(config: ApplicationConfig) {
        if (!isInitialized) {
            try {
                // Try to load credentials from environment variable or config file
                val credentials = loadCredentials(config)
                
                val options = FirebaseOptions.builder()
                    .setCredentials(credentials)
                    .apply {
                        config.propertyOrNull("firebase.projectId")?.getString()?.let { setProjectId(it) }
                        config.propertyOrNull("firebase.storageBucket")?.getString()?.let { setStorageBucket(it) }
                    }
                    .build()
                
                // Initialize Firebase
                firebaseApp = FirebaseApp.initializeApp(options)
                firebaseAuth = FirebaseAuth.getInstance(firebaseApp)
                isInitialized = true
                
                println("Firebase Admin SDK initialized successfully")
            } catch (e: Exception) {
                throw IllegalStateException("Failed to initialize Firebase: ${e.message}", e)
            }
        }
    }
    
    private fun loadCredentials(config: ApplicationConfig): GoogleCredentials {
        return try {
            // 1) Try to load explicit path from application.conf if provided
            val configPath = config.propertyOrNull("firebase.credentials")?.getString()?.trim()?.takeIf { it.isNotEmpty() }
            println("[FirebaseConfig] firebase.credentials from config = '" + (configPath ?: "<null>") + "'")
            if (configPath != null) {
                val file = File(configPath)
                println("[FirebaseConfig] Checking config path exists: ${file.absolutePath} -> ${file.exists()}")
                if (file.exists()) {
                    return FileInputStream(file).use { GoogleCredentials.fromStream(it) }
                }
            }

            // 2) Try to load from environment variable
            val envPath = System.getenv("GOOGLE_APPLICATION_CREDENTIALS")?.trim()?.takeIf { it.isNotEmpty() }
            println("[FirebaseConfig] GOOGLE_APPLICATION_CREDENTIALS = '" + (envPath ?: "<null>") + "'")
            if (envPath != null) {
                val file = File(envPath)
                println("[FirebaseConfig] Checking env path exists: ${file.absolutePath} -> ${file.exists()}")
                if (file.exists()) {
                    return FileInputStream(file).use { GoogleCredentials.fromStream(it) }
                }
            }

            // 3) Try to load from default file named service-account.json near working directory
            val candidates = mutableListOf<File>()
            candidates += File("service-account.json")
            // Also check parent directories up to two levels to handle multi-project run dirs
            File("").absoluteFile.parentFile?.let { parent ->
                candidates += File(parent, "service-account.json")
                parent.parentFile?.let { grand -> candidates += File(grand, "service-account.json") }
            }
            val found = candidates.firstOrNull { it.exists() }
            println("[FirebaseConfig] Candidate search paths: " + candidates.joinToString(" | ") { it.absolutePath + ":" + it.exists() })
            if (found != null) {
                return FileInputStream(found).use { GoogleCredentials.fromStream(it) }
            }

            // 4) Try to load from resources on classpath
            val resource = FirebaseConfig::class.java.classLoader.getResourceAsStream("service-account.json")
                ?: throw IllegalStateException("Firebase service account file not found")

            GoogleCredentials.fromStream(resource)
        } catch (e: Exception) {
            throw IllegalStateException("Failed to load Firebase credentials: ${e.message}", e)
        }
    }
    
    /**
     * Get the Firebase Auth instance.
     * @throws IllegalStateException if Firebase is not initialized
     */
    val auth: FirebaseAuth
        @Synchronized get() {
            check(isInitialized) { "Firebase is not initialized. Call initialize() first." }
            return firebaseAuth
        }
    
    /**
     * Create a custom token for the given user ID and optional claims.
     * @param uid The user ID to create the token for
     * @param claims Optional claims to include in the token
     * @return The generated custom token
     * @throws FirebaseAuthException if the token cannot be created
     */
    @Throws(FirebaseAuthException::class)
    fun createCustomToken(uid: String, claims: Map<String, Any>? = null): String {
        return auth.createCustomToken(uid, claims)
    }
    
    /**
     * Get a user by their phone number.
     * @param phoneNumber The phone number to look up
     * @return The user record if found
     * @throws UserNotFoundException if the user is not found
     * @throws FirebaseAuthException for other Firebase Auth errors
     */
    @Throws(FirebaseAuthException::class, UserNotFoundException::class)
    suspend fun getUserByPhone(phoneNumber: String): UserRecord {
        return try {
            auth.getUserByPhoneNumber(phoneNumber)
        } catch (e: FirebaseAuthException) {
            if (e.errorCode.name == "user-not-found") {
                throw UserNotFoundException("No user found with phone number: $phoneNumber")
            }
            throw e
        }
    }
    
    /**
     * Get a user by their UID.
     * @param uid The user ID to look up
     * @return The user record if found
     * @throws UserNotFoundException if the user is not found
     * @throws FirebaseAuthException for other Firebase Auth errors
     */
    @Throws(FirebaseAuthException::class, UserNotFoundException::class)
    suspend fun getUserById(uid: String): UserRecord {
        return try {
            auth.getUser(uid)
        } catch (e: FirebaseAuthException) {
            if (e.errorCode.name == "user-not-found") {
                throw UserNotFoundException("No user found with UID: $uid")
            }
            throw e
        }
    }
    
    /**
     * Create a new user with the given phone number and optional display name.
     * @param phoneNumber The user's phone number
     * @param displayName Optional display name for the user
     * @param email Optional email for the user
     * @param password Optional password for the user
     * @return The created user record
     * @throws UserAlreadyExistsException if a user with the given phone number already exists
     * @throws FirebaseAuthException for other Firebase Auth errors
     */
    @Throws(FirebaseAuthException::class, UserAlreadyExistsException::class)
    suspend fun createUser(
        phoneNumber: String,
        displayName: String? = null,
        email: String? = null,
        password: String? = null
    ): UserRecord {
        return try {
            val createRequest = UserRecord.CreateRequest()
                .setPhoneNumber(phoneNumber)
                .setDisplayName(displayName)
                .setEmail(email)
                .apply {
                    if (!password.isNullOrBlank()) {
                        setPassword(password)
                    }
                }
                
            auth.createUser(createRequest)
        } catch (e: FirebaseAuthException) {
            if (e.errorCode.name == "phone-number-already-exists" || e.errorCode.name == "email-already-exists") {
                throw UserAlreadyExistsException("User with this phone number or email already exists")
            }
            throw e
        }
    }
    
    /**
     * Update an existing user's information.
     * @param uid The user ID to update
     * @param updates Map of fields to update
     * @return The updated user record
     * @throws UserNotFoundException if the user is not found
     * @throws FirebaseAuthException for other Firebase Auth errors
     */
    @Throws(FirebaseAuthException::class, UserNotFoundException::class)
    suspend fun updateUser(uid: String, updates: Map<String, Any>): UserRecord {
        return try {
            val updateRequest = UserRecord.UpdateRequest(uid)
            
            updates.forEach { (key, value) ->
                when (key) {
                    "displayName" -> updateRequest.setDisplayName(value as String)
                    "email" -> updateRequest.setEmail(value as String)
                    "phoneNumber" -> updateRequest.setPhoneNumber(value as String)
                    "password" -> updateRequest.setPassword(value as String)
                    // Add other fields as needed
                }
            }
            
            auth.updateUser(updateRequest)
        } catch (e: FirebaseAuthException) {
            if (e.errorCode.name == "user-not-found") {
                throw UserNotFoundException("No user found with UID: $uid")
            }
            throw e
        }
    }
    
    /**
     * Delete a user by their UID.
     * @param uid The user ID to delete
     * @throws UserNotFoundException if the user is not found
     * @throws FirebaseAuthException for other Firebase Auth errors
     */
    @Throws(FirebaseAuthException::class, UserNotFoundException::class)
    suspend fun deleteUser(uid: String) {
        try {
            auth.deleteUser(uid)
        } catch (e: FirebaseAuthException) {
            if (e.errorCode.name == "user-not-found") {
                throw UserNotFoundException("No user found with UID: $uid")
            }
            throw e
        }
    }
    
    /**
     * Generate a password reset link for the given email.
     * @param email The user's email address
     * @param settings Action code settings for the reset link
     * @return The generated password reset link
     * @throws UserNotFoundException if no user is found with the given email
     * @throws FirebaseAuthException for other Firebase Auth errors
     */
    @Throws(FirebaseAuthException::class, UserNotFoundException::class)
    suspend fun generatePasswordResetLink(email: String, settings: ActionCodeSettings): String {
        return try {
            auth.generatePasswordResetLink(email, settings)
        } catch (e: FirebaseAuthException) {
            if (e.errorCode.name == "user-not-found") {
                throw UserNotFoundException("No user found with email: $email")
            }
            throw e
        }
    }
    
    /**
     * Not supported on server: Firebase Admin SDK does not verify password reset codes.
     */
    @Throws(FirebaseAuthException::class)
    suspend fun verifyPasswordResetCode(code: String): String {
        throw org.cap.gold.exceptions.BadRequestException(
            "Password reset code verification is not supported on the server"
        )
    }

    /**
     * Not supported on server: Firebase Admin SDK does not confirm password resets.
     */
    @Throws(FirebaseAuthException::class)
    suspend fun confirmPasswordReset(code: String, newPassword: String) {
        throw org.cap.gold.exceptions.BadRequestException(
            "Password reset confirmation is not supported on the server"
        )
    }
    
    /**
     * Check if a phone number is already in use by another user.
     * @param phoneNumber The phone number to check
     * @return true if the phone number is already in use, false otherwise
     */
    suspend fun isPhoneNumberInUse(phoneNumber: String): Boolean {
        return try {
            auth.getUserByPhoneNumber(phoneNumber)
            true
        } catch (e: FirebaseAuthException) {
            if (e.errorCode.name == "user-not-found") {
                false
            } else {
                throw e
            }
        }
    }
}
