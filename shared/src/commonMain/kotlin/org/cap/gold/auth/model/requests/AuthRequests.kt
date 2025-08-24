package org.cap.gold.auth.model.requests

import kotlinx.serialization.Serializable

/**
 * Request model for phone number sign-in. Deprecated in favor of EmailSignInRequest
 */
@Serializable
data class PhoneSignInRequest(
    val phoneNumber: String,
    val password: String
)

/**
 * Request model for phone number sign-up. Deprecated in favor of EmailSignUpRequest
 */
@Serializable
data class PhoneSignUpRequest(
    val phoneNumber: String,
    val password: String,
    val displayName: String? = null
)

/**
 * Request model for email sign-in.
 */
@Serializable
data class EmailSignInRequest(
    val email: String,
    val password: String
)

/**
 * Request model for email sign-up. Phone number is mandatory.
 */
@Serializable
data class EmailSignUpRequest(
    val email: String,
    val password: String,
    val phoneNumber: String,
    val displayName: String? = null
)

/**
 * Request model for refreshing an access token.
 * @property refreshToken The refresh token
 */
@Serializable
data class TokenRefreshRequest(
    val refreshToken: String
)
