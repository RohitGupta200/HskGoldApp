package org.cap.gold.config

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.DecodedJWT
import io.ktor.server.config.*
import java.util.*

class JwtConfig(config: ApplicationConfig) {
    private val jwtAudience = config.property("ktor.security.jwt.audience").getString()
    private val jwtIssuer = config.property("ktor.security.jwt.issuer").getString()
    private val jwtRealm = config.property("ktor.security.jwt.realm").getString()
    
    // Get the secret from environment variable or use the one from config
    private val jwtSecret = System.getenv("JWT_SECRET") ?: 
        config.propertyOrNull("ktor.security.jwt.secret")?.getString() ?: 
        throw IllegalStateException("JWT Secret not configured")
    
    // Access token expiration (default: 15 minutes)
    private val accessTokenExpiration = parseDurationMillis(
        config.propertyOrNull("ktor.security.jwt.accessTokenExpiration")?.getString(),
        15 * 60 * 1000L
    )
    
    // Refresh token expiration (default: 7 days)
    private val refreshTokenExpiration = parseDurationMillis(
        config.propertyOrNull("ktor.security.jwt.refreshTokenExpiration")?.getString(),
        7 * 24 * 60 * 60 * 1000L
    )
    
    // Algorithm for signing tokens
    private val algorithm = Algorithm.HMAC256(jwtSecret)
    
    // JWT verifier
    val verifier: JWTVerifier = JWT
        .require(algorithm)
        .withAudience(jwtAudience)
        .withIssuer(jwtIssuer)
        .build()
    
    /**
     * Generate an access token for the given user ID and roles
     */
    fun generateAccessToken(userId: String, roles: List<String> = emptyList()): String {
        return JWT.create()
            .withSubject(userId)
            .withAudience(jwtAudience)
            .withIssuer(jwtIssuer)
            .withClaim("type", "access")
            .withClaim("roles", roles)
            .withIssuedAt(Date())
            .withExpiresAt(Date(System.currentTimeMillis() + accessTokenExpiration))
            .sign(algorithm)
    }
    
    /**
     * Generate a refresh token for the given user ID
     */
    fun generateRefreshToken(userId: String): String {
        return JWT.create()
            .withSubject(userId)
            .withAudience(jwtAudience)
            .withIssuer(jwtIssuer)
            .withClaim("type", "refresh")
            .withIssuedAt(Date())
            .withExpiresAt(Date(System.currentTimeMillis() + refreshTokenExpiration))
            .sign(algorithm)
    }
    
    /**
     * Validate a JWT token and return the decoded JWT if valid
     */
    fun validateToken(token: String): DecodedJWT? {
        return try {
            verifier.verify(token)
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Extract user ID from a JWT token
     */
    fun getUserIdFromToken(token: String): String? {
        return validateToken(token)?.subject
    }
    
    /**
     * Check if a token is expired
     */
    fun isTokenExpired(token: String): Boolean {
        return validateToken(token)?.expiresAt?.before(Date()) ?: true
    }
    
    /**
     * Get the remaining time in milliseconds until the token expires
     */
    fun getRemainingTime(token: String): Long {
        val expiresAt = validateToken(token)?.expiresAt?.time ?: return 0
        return (expiresAt - System.currentTimeMillis()).coerceAtLeast(0)
    }
}

private fun parseDurationMillis(input: String?, defaultMs: Long): Long {
    if (input == null) return defaultMs
    val s = input.trim().lowercase()
    if (s.isEmpty()) return defaultMs
    // If it's purely numeric, treat as milliseconds
    if (s.all { it.isDigit() }) return s.toLongOrNull() ?: defaultMs

    // Match patterns like 15m, 7d, 30s, 2h, 500ms
    val regex = Regex("^([0-9]+)\n?(ms|s|m|h|d)$")
    val match = regex.matchEntire(s) ?: return defaultMs
    val value = match.groupValues[1].toLongOrNull() ?: return defaultMs
    return when (match.groupValues[2]) {
        "ms" -> value
        "s" -> value * 1000L
        "m" -> value * 60_000L
        "h" -> value * 3_600_000L
        "d" -> value * 86_400_000L
        else -> defaultMs
    }
}
