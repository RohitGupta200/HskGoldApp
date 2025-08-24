package org.cap.gold.auth

import kotlinx.serialization.Serializable

@Serializable
data class PhoneSignInRequest(
    val phoneNumber: String,
    val password: String
)

@Serializable
data class EmailSignInRequest(
    val email: String,
    val password: String
)

@Serializable
data class PhoneSignUpRequest(
    val phoneNumber: String,
    val password: String,
    val displayName: String? = null
)

@Serializable
data class EmailSignUpRequest(
    val email: String,
    val password: String,
    val phoneNumber: String,
    val displayName: String? = null
)

@Serializable
data class PasswordResetRequest(
    val phoneNumber: String
)

@Serializable
data class Tokens(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Int
)

@Serializable
data class User(
    val id: String,
    val phoneNumber: String,
    val displayName: String? = null,
    val photoUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
data class AuthResponse(
    val user: User,
    val tokens: Tokens
)

@Serializable
data class ErrorResponse(
    val error: String,
    val code: Int = 400
)
