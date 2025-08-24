package org.cap.gold.data.network

import io.ktor.client.request.*
import io.ktor.http.*
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Manages authentication state and provides authentication headers for API requests.
 */
interface AuthManager {
    /**
     * Adds the authentication header to the request if a token is available.
     */
    fun HttpRequestBuilder.addAuthHeader()
    
    /**
     * Updates the authentication token.
     */
    fun updateToken(token: String?)
    
    /**
     * Clears the authentication token (logout).
     */
    fun clearToken()
    
    /**
     * Gets the current authentication token.
     */
    val currentToken: String?
}

/**
 * Implementation of [AuthManager] that stores the token in memory.
 */
class AuthManagerImpl : AuthManager, KoinComponent {
    private val preferencesManager: PreferencesManager by inject()
    
    override var currentToken: String? = null
        private set
    
    init {
        // Load token from secure storage on initialization
        currentToken = preferencesManager.getAuthToken()
    }
    
    override fun HttpRequestBuilder.addAuthHeader() {
        currentToken?.let { token ->
            headers[NetworkConstants.HEADER_AUTHORIZATION] = "${NetworkConstants.HEADER_BEARER} $token"
        }
    }
    
    override fun updateToken(token: String?) {
        currentToken = token
        // Persist token to secure storage
        token?.let { preferencesManager.saveAuthToken(it) } ?: preferencesManager.clearAuthToken()
    }
    
    override fun clearToken() {
        updateToken(null)
    }
}

/**
 * Interface for secure storage of authentication tokens.
 */
interface PreferencesManager {
    fun saveAuthToken(token: String)
    fun getAuthToken(): String?
    fun clearAuthToken()
}
