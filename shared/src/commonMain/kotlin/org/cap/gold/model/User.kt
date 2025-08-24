package org.cap.gold.model

import kotlinx.serialization.Serializable
import kotlinx.datetime.Clock

/**
 * Represents an authenticated user in the application.
 * @property id The unique identifier for the user
 * @property phoneNumber The user's phone number (primary identifier)
 * @property email The user's email address (optional)
 * @property displayName The user's display name (optional)
 * @property photoUrl URL to the user's profile photo (optional)
 * @property isEmailVerified Whether the user's email is verified
 * @property createdAt When the user account was created
 * @property lastLogin When the user last logged in
 */
@Serializable
data class User(
    val id: String,
    val phoneNumber: String,
    val email: String? = null,
    val displayName: String? = null,
    val photoUrl: String? = null,
    val isEmailVerified: Boolean = false,
    val name: String? = null,
    val role:Int = 3,
    val createdAt: Long = Clock.System.now().toEpochMilliseconds(),
    val lastLogin: Long = Clock.System.now().toEpochMilliseconds()
)
