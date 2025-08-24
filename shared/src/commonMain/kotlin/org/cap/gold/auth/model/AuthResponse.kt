package org.cap.gold.auth.model

import kotlinx.serialization.Serializable
import org.cap.gold.model.User

/**
 * Response model for authentication operations.
 * @property user The authenticated user information
 * @property accessToken The JWT access token
 * @property refreshToken The refresh token for obtaining new access tokens
 * @property expiresIn The number of seconds until the access token expires
 * @property isNewUser Whether this is a newly created user
 */
@Serializable
data class AuthResponse(
    val user: User,
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Int,
    val isNewUser: Boolean = false
)
