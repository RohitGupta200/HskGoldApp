package org.cap.gold.di

import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.http.HttpHeaders
import org.cap.gold.auth.TokenManager
import org.koin.dsl.module
import kotlinx.serialization.json.Json

/**
 * Koin module for network-related dependencies.
 */
val networkModule = module {
    // HTTP Client
    single {
        val tokenManager: TokenManager = get()
        // Read base URL from Koin properties set by shared initKoin
        val baseUrl: String = getKoin().getProperty("api.base.url") ?: "http://10.0.2.2:8080"
        HttpClient() {
            // JSON serialization
            install(ContentNegotiation) {
                json(Json {
                    prettyPrint = false
                    isLenient = true
                    ignoreUnknownKeys = true
                    encodeDefaults = true
                })
            }
            // Set default request timeout
            install(HttpTimeout) {
                requestTimeoutMillis = 30_000L
                connectTimeoutMillis = 30_000L
                socketTimeoutMillis = 30_000L
            }
            
            // Default request config
            defaultRequest {
                url(baseUrl)
                headers.append("Accept", "application/json")
                headers.append("Content-Type", "application/json")
            }

            // HTTP logging
            install(Logging) {
                level = LogLevel.ALL
                logger = Logger.DEFAULT
                sanitizeHeader { it.equals(HttpHeaders.Authorization, ignoreCase = true) }
            }

            // Attach Auth plugin backed by shared TokenManager
            install(Auth) {
                bearer {
                    loadTokens {
                        tokenManager.tokens.value?.let { t ->
                            BearerTokens(t.accessToken, t.refreshToken)
                        }
                    }
                    refreshTokens {
                        val nt = tokenManager.refreshToken()
                        nt?.let { BearerTokens(it.accessToken, it.refreshToken) }
                    }
                    // Avoid sending auth on auth endpoints
                    sendWithoutRequest { request ->
                        val urlStr = request.url.buildString()
                        !(urlStr.endsWith("/auth/signin/email") ||
                          urlStr.endsWith("/auth/signup/email") ||
                          urlStr.endsWith("/auth/refresh"))
                    }
                }
            }
        }
    }
}

// Platform-specific PreferencesManager bindings should be provided in androidMain/iosMain modules.

