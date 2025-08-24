package org.cap.gold.network

import io.ktor.client.HttpClient
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.*
import io.ktor.client.plugins.defaultRequest
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import org.cap.gold.auth.TokenManager

/**
 * Centralized Ktor HttpClient for the app.
 *
 * NOTE: This currently duplicates configuration from KtorAuthService; we should
 * refactor KtorAuthService to reuse this client in a follow-up.
 */
class NetworkClient(
    private val baseUrl: String,
    private val tokenManager: TokenManager
) {
    val client: HttpClient = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = false
                isLenient = true
                ignoreUnknownKeys = true
            })
        }

        defaultRequest {
            url(baseUrl)
            contentType(ContentType.Application.Json)
        }

        install(HttpTimeout) {
            requestTimeoutMillis = 30000
            connectTimeoutMillis = 30000
            socketTimeoutMillis = 30000
        }

        install(Logging) { level = LogLevel.INFO }

        install(Auth) {
            bearer {
                loadTokens {
                    tokenManager.tokens.value?.let { t ->
                        BearerTokens(t.accessToken, t.refreshToken)
                    }
                }
                refreshTokens {
                    val newTokens = tokenManager.refreshToken()
                    newTokens?.let { BearerTokens(it.accessToken, it.refreshToken) }
                }
                sendWithoutRequest { req ->
                    // Only attach auth by default; allow public endpoints to skip if needed
                    true
                }
            }
        }
    }
}
