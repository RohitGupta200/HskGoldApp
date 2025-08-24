package org.cap.gold.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.server.application.*
import io.ktor.server.auth.jwt.*
import java.util.*

class JwtConfig(environment: ApplicationEnvironment) {
    private val secret = environment.config.property("jwt.secret").getString()
    private val issuer = environment.config.property("jwt.issuer").getString()
    private val audience = environment.config.property("jwt.audience").getString()
    private val accessTokenExpiry = 15 * 60 * 1000L // 15 minutes
    private val refreshTokenExpiry = 7 * 24 * 60 * 60 * 1000L // 7 days

    val verifier: JWTVerifier = JWT
        .require(Algorithm.HMAC256(secret))
        .withAudience(audience)
        .withIssuer(issuer)
        .build()

    fun generateTokens(userId: String, phoneNumber: String, displayName: String? = null): Map<String, String> {
        val accessToken = JWT.create()
            .withAudience(audience)
            .withIssuer(issuer)
            .withClaim("userId", userId)
            .withClaim("phoneNumber", phoneNumber)
            .withClaim("displayName", displayName)
            .withExpiresAt(Date(System.currentTimeMillis() + accessTokenExpiry))
            .sign(Algorithm.HMAC256(secret))

        val refreshToken = JWT.create()
            .withAudience(audience)
            .withIssuer(issuer)
            .withClaim("userId", userId)
            .withExpiresAt(Date(System.currentTimeMillis() + refreshTokenExpiry))
            .sign(Algorithm.HMAC256(secret))

        return mapOf(
            "accessToken" to accessToken,
            "refreshToken" to refreshToken
        )
    }

    fun validateToken(token: String): JWTPrincipal? {
        return try {
            val jwt = JWT.require(Algorithm.HMAC256(secret))
                .withIssuer(issuer)
                .build()
                .verify(token)
            JWTPrincipal(jwt)
        } catch (e: Exception) {
            null
        }
    }
}
