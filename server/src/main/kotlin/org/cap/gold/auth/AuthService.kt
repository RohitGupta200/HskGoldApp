package org.cap.gold.auth

import org.koin.core.component.KoinComponent

class AuthService(private val jwtConfig: JwtConfig) : KoinComponent {
    suspend fun signInWithPhone(phoneNumber: String, password: String): AuthResponse {
        throw UnsupportedOperationException("Server-side phone/password sign-in is not supported here. Use Firebase client SDK or dedicated endpoints in AuthController.")
    }

    suspend fun createUserWithPhone(phoneNumber: String, password: String, displayName: String?): AuthResponse {
        throw UnsupportedOperationException("Server-side phone/password sign-up is not supported here. Use Firebase Admin via UserRepository endpoints.")
    }

    suspend fun refreshTokens(refreshToken: String): AuthResponse {
        throw UnsupportedOperationException("Refresh token flow not implemented in this service. Use JwtConfig/Ktor auth instead.")
    }

    suspend fun sendPasswordResetSms(phoneNumber: String) {
        throw UnsupportedOperationException("Password reset via SMS is not implemented on the server.")
    }
}

class AuthException(override val message: String, val statusCode: Int) : Exception(message)
