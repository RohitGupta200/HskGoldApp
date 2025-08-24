package org.cap.gold.api

import io.ktor.client.*
import io.ktor.client.engine.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import org.cap.gold.PlatformInfo

// Import the httpClientEngine from Platform.kt
import org.cap.gold.api.httpClientEngine

/**
 * Factory for creating and managing Ktor HTTP client instances.
 */
object ApiClient {
    private lateinit var baseUrl: String
    private var authToken: String? = null
    
    // JSON configuration
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        prettyPrint = true
    }
    
    // Ktor HTTP client
    val httpClient = HttpClient(httpClientEngine) {
        install(ContentNegotiation) {
            json(json)
        }
        
        install(HttpTimeout) {
            requestTimeoutMillis = 30000
            connectTimeoutMillis = 30000
            socketTimeoutMillis = 30000
        }
        
        defaultRequest {
            url(baseUrl)
            contentType(ContentType.Application.Json)
            authToken?.let { token ->
                header(HttpHeaders.Authorization, "Bearer $token")
            }
        }
        
        if (PlatformInfo.isDebug) {
            install(Logging) {
                level = LogLevel.ALL
            }
        }
    }
    
    /**
     * Initialize the API client with the given base URL.
     *
     * @param baseUrl The base URL of the API server
     */
    fun init(baseUrl: String) {
        this.baseUrl = baseUrl.trimEnd('/')
    }
    
    /**
     * Get the current base URL.
     */
    fun getBaseUrl(): String = baseUrl
    
    /**
     * Update the authentication token for all subsequent requests
     */
    fun updateAuthToken(token: String) {
        this.authToken = token
    }
    
    /**
     * Clear the authentication token
     */
    fun clearAuthToken() {
        this.authToken = null
    }
}
