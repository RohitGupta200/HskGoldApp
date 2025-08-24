package org.cap.gold.repositories

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.UserRecord
import org.cap.gold.exceptions.*
import org.cap.gold.models.User
import java.util.*

/**
 * Repository for handling user-related operations with Firebase Authentication
 */
class UserRepository {
    private val firebaseAuth = FirebaseAuth.getInstance()

    /**
     * Get user by phone number
     * @throws UserNotFoundException if user is not found
     * @throws AuthException for other Firebase Auth errors
     */
    @Throws(FirebaseAuthException::class)
    suspend fun getUserByPhoneNumber(phoneNumber: String): User {
        return try {
            val userRecord = firebaseAuth.getUserByPhoneNumber(phoneNumber)
            userRecord.toUser()
        } catch (e: FirebaseAuthException) {
            if (e.errorCode.name == "user-not-found") {
                throw UserNotFoundException("User with phone number $phoneNumber not found")
            } else {
                throw AuthException("Failed to get user: ${e.message}")
            }
        }
    }

    /**
     * Get user by UID
     * @throws UserNotFoundException if user is not found
     * @throws AuthException for other Firebase Auth errors
     */
    @Throws(FirebaseAuthException::class)
    suspend fun getUserById(uid: String): User {
        return try {
            val userRecord = firebaseAuth.getUser(uid)
            userRecord.toUser()
        } catch (e: FirebaseAuthException) {
            if (e.errorCode.name == "user-not-found") {
                throw UserNotFoundException("User with ID $uid not found")
            } else {
                throw AuthException("Failed to get user: ${e.message}")
            }
        }
    }

    /**
     * Create a new user with phone number and password
     * @throws UserAlreadyExistsException if user with phone number already exists
     * @throws BadRequestException for invalid input
     * @throws AuthException for other Firebase Auth errors
     */
    @Throws(FirebaseAuthException::class)
    suspend fun createUser(phoneNumber: String, password: String, displayName: String? = null): User {
        // Validate input
        if (phoneNumber.isBlank() || password.isBlank()) {
            throw BadRequestException("Phone number and password are required")
        }

        // Check if user already exists
        try {
            val existingUser = firebaseAuth.getUserByPhoneNumber(phoneNumber)
            throw UserAlreadyExistsException(phoneNumber)
        } catch (e: FirebaseAuthException) {
            if (e.errorCode.name != "user-not-found") {
                throw AuthException("Failed to check existing user: ${e.message}")
            }
            // User doesn't exist, proceed with creation
        }

        try {
            val request = com.google.firebase.auth.UserRecord.CreateRequest()
                .setPhoneNumber(phoneNumber)
                .setPassword(password)
                .setUid(UUID.randomUUID().toString())
                .setDisplayName(displayName ?: "User ${phoneNumber.takeLast(4)}")
                .setDisabled(false)

            val userRecord = firebaseAuth.createUser(request)
            return userRecord.toUser()
        } catch (e: FirebaseAuthException) {
            throw when (e.errorCode.name) {
                "phone-number-already-exists" -> UserAlreadyExistsException(phoneNumber)
                "invalid-phone-number" -> BadRequestException("Invalid phone number format")
                "weak-password" -> BadRequestException("Password is too weak")
                else -> AuthException("Failed to create user: ${e.message}")
            }
        }
    }

    /**
     * Update user information
     * @throws UserNotFoundException if user is not found
     * @throws ResourceConflictException for conflicts (e.g., duplicate phone number)
     * @throws BadRequestException for invalid input
     * @throws AuthException for other Firebase Auth errors
     */
    @Throws(FirebaseAuthException::class)
    suspend fun updateUser(uid: String, updates: Map<String, Any>): User {
        if (updates.isEmpty()) {
            throw BadRequestException("No updates provided")
        }

        try {
            val request = com.google.firebase.auth.UserRecord.UpdateRequest(uid)
            updates.forEach { (key, value) ->
                when (key) {
                    "displayName" -> {
                        val name = value as? String ?: throw BadRequestException("displayName must be a String")
                        request.setDisplayName(name)
                    }
                    "email" -> {
                        val email = value as? String ?: throw BadRequestException("email must be a String")
                        request.setEmail(email)
                    }
                    "phoneNumber" -> {
                        val phone = value as? String ?: throw BadRequestException("Phone number cannot be null")
                        // Check if phone number is already in use
                        try {
                            val existingUser = firebaseAuth.getUserByPhoneNumber(phone)
                            if (existingUser.uid != uid) {
                                throw ResourceConflictException("Phone number already in use")
                            }
                        } catch (e: FirebaseAuthException) {
                            if (e.errorCode.name != "user-not-found") {
                                throw e
                            }
                        }
                        request.setPhoneNumber(phone)
                    }
                    "photoUrl" -> {
                        val url = value as? String ?: throw BadRequestException("photoUrl must be a String")
                        request.setPhotoUrl(url)
                    }
                    "disabled" -> {
                        val disabled = value as? Boolean ?: throw BadRequestException("disabled must be a Boolean")
                        request.setDisabled(disabled)
                    }
                    "password" -> {
                        val password = value as? String ?: throw BadRequestException("Password cannot be null")
                        request.setPassword(password)
                    }
                    else -> {
                        // Ignore unknown keys or throw if you prefer strict handling
                    }
                }
            }
            val userRecord = firebaseAuth.updateUser(request)
            return userRecord.toUser()
        } catch (e: ClassCastException) {
            throw BadRequestException("Invalid data type for one or more fields")
        } catch (e: FirebaseAuthException) {
            throw when (e.errorCode.name) {
                "user-not-found" -> UserNotFoundException("User with ID $uid not found")
                "phone-number-already-exists" -> ResourceConflictException("Phone number already in use")
                "invalid-phone-number" -> BadRequestException("Invalid phone number format")
                else -> AuthException("Failed to update user: ${e.message}")
            }
        }
    }

    /**
     * Delete a user by UID
     * @throws UserNotFoundException if user is not found
     * @throws AuthException for other Firebase Auth errors
     */
    @Throws(FirebaseAuthException::class)
    suspend fun deleteUser(uid: String) {
        try {
            firebaseAuth.deleteUser(uid)
        } catch (e: FirebaseAuthException) {
            if (e.errorCode.name == "user-not-found") {
                throw UserNotFoundException("User with ID $uid not found")
            } else {
                throw AuthException("Failed to delete user: ${e.message}")
            }
        }
    }

    /**
     * Generate a password reset link for a user's email
     * @throws UserNotFoundException if user is not found
     * @throws BadRequestException if user has no email
     * @throws AuthException for other Firebase Auth errors
     */
    @Throws(FirebaseAuthException::class)
    suspend fun generatePasswordResetLink(email: String): String {
        return try {
            firebaseAuth.generatePasswordResetLink(email)
        } catch (e: FirebaseAuthException) {
            when (e.errorCode.name) {
                "user-not-found" -> throw UserNotFoundException("User with email $email not found")
                "invalid-email" -> throw BadRequestException("Invalid email address")
                else -> throw AuthException("Failed to generate password reset link: ${e.message}")
            }
        }
    }

    /**
     * Verify a password reset code and return the associated email if valid
     * @throws BadRequestException if the code is invalid or expired
     * @throws AuthException for other Firebase Auth errors
     */
    @Throws(FirebaseAuthException::class)
    suspend fun verifyPasswordResetCode(code: String): String {
        // Not supported by Firebase Admin SDK. This must be done on client SDKs.
        throw BadRequestException("Password reset code verification is not supported on the server")
    }

    /**
     * Confirm password reset with the given code and new password
     * @throws BadRequestException if the code is invalid or expired
     * @throws AuthException for other Firebase Auth errors
     */
    @Throws(FirebaseAuthException::class)
    suspend fun confirmPasswordReset(code: String, newPassword: String) {
        // Not supported by Firebase Admin SDK. This must be done on client SDKs.
        throw BadRequestException("Password reset confirmation is not supported on the server")
    }

    /**
     * Check if a phone number is already in use by another user
     * @return true if the phone number is in use, false otherwise
     */
    suspend fun isPhoneNumberInUse(phoneNumber: String): Boolean {
        return try {
            firebaseAuth.getUserByPhoneNumber(phoneNumber)
            true
        } catch (e: FirebaseAuthException) {
            if (e.errorCode.name == "user-not-found") {
                false
            } else {
                throw AuthException("Failed to check phone number: ${e.message}")
            }
        }
    }

    /**
     * Extension function to convert Firebase UserRecord to our User model
     */
    private fun UserRecord.toUser(): User {
        return User(
            id = uid,
            phoneNumber = phoneNumber ?: "",
            displayName = displayName,
            email = email,
            photoUrl = photoUrl,
            disabled = isDisabled,
            emailVerified = isEmailVerified,
            metadata = User.Metadata(
                creationTime = userMetadata?.creationTimestamp ?: 0,
                lastSignInTime = userMetadata?.lastSignInTimestamp ?: 0
            ),
            customClaims = customClaims as? Map<String, Any> ?: emptyMap()
        )
    }
}

 
