package org.cap.gold.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.Contextual
import java.time.Instant
import java.util.*

/**
 * Data class representing a user in the system
 */
@Serializable
data class User(
    /**
     * Firebase UID of the user
     */
    val id: String,
    
    /**
     * User's phone number in E.164 format (e.g., +15551234567)
     */
    val phoneNumber: String,
    
    /**
     * User's display name
     */
    val displayName: String? = null,
    
    /**
     * User's email address (optional)
     */
    val email: String? = null,
    
    /**
     * URL to the user's profile photo (optional)
     */
    val photoUrl: String? = null,
    
    /**
     * Whether the user is disabled (true) or enabled (false)
     */
    val disabled: Boolean = false,
    
    /**
     * Whether the user's email is verified
     */
    val emailVerified: Boolean = false,
    
    /**
     * User metadata including creation and last sign-in times
     */
    val metadata: Metadata = Metadata(),
    
    /**
     * Custom claims associated with the user
     */
    val customClaims: Map<String, @Contextual Any> = emptyMap()
) {
    /**
     * Data class for user metadata
     */
    @Serializable
    data class Metadata(
        /**
         * The time the user was created, in milliseconds since epoch
         */
        val creationTime: Long = 0,
        
        /**
         * The last time the user signed in, in milliseconds since epoch
         */
        val lastSignInTime: Long = 0
    )
    
    /**
     * Converts the user to a map of claims for JWT tokens
     */
    fun toClaims(): Map<String, Any> {
        return customClaims.toMutableMap().apply {
            put("uid", id)
            put("phone_number", phoneNumber)
            displayName?.let { put("name", it) }
            email?.let { put("email", it) }
            put("email_verified", emailVerified)
            put("disabled", disabled)
        }
    }
}

/**
 * Data class for user creation request
 */
@Serializable
data class CreateUserRequest(
    val phoneNumber: String,
    val password: String,
    val displayName: String? = null,
    val email: String? = null
)

/**
 * Data class for user update request
 */
@Serializable
data class UpdateUserRequest(
    val displayName: String? = null,
    val email: String? = null,
    val phoneNumber: String? = null,
    val photoUrl: String? = null,
    val disabled: Boolean? = null,
    val customClaims: Map<String, @Contextual Any>? = null
)

/**
 * Data class for user response
 */
@Serializable
data class UserResponse(
    val id: String,
    val phoneNumber: String,
    val displayName: String? = null,
    val email: String? = null,
    val photoUrl: String? = null,
    val disabled: Boolean = false,
    val emailVerified: Boolean = false,
    val metadata: User.Metadata = User.Metadata(),
    val role: Int = 3
) {
    companion object {
        fun fromUser(user: User): UserResponse {
            val role = (user.customClaims["role"] as? Number)?.toInt() ?: 3
            return UserResponse(
                id = user.id,
                phoneNumber = user.phoneNumber,
                displayName = user.displayName,
                email = user.email,
                photoUrl = user.photoUrl,
                disabled = user.disabled,
                emailVerified = user.emailVerified,
                metadata = user.metadata,
                role = role
            )
        }
    }
}
