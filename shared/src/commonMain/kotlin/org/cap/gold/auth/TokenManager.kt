package org.cap.gold.auth

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.*
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.CompletableDeferred
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.cap.gold.auth.model.AuthResponse
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

import org.cap.gold.model.User

/**
 * Manages authentication tokens and handles token refresh logic.
 * @property baseUrl The base URL of the authentication server
 * @property tokenStorage The storage mechanism for persisting tokens
 */
class TokenManager(
    private val baseUrl: String,
    private val tokenStorage: TokenStorage
) {
    private val mutex = Mutex()
    private val _tokens = MutableStateFlow<Tokens?>(null)
    // Gate to signal that initial load from storage has completed
    private val initialLoad = CompletableDeferred<Unit>()
    
    // Lightweight HTTP client used only for refresh flow to avoid wider coupling
    private val httpClient by lazy {
        HttpClient {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                })
            }
            defaultRequest {
                url(baseUrl)
                contentType(ContentType.Application.Json)
            }
        }
    }
    
    /**
     * The current tokens, or null if not authenticated.
     */
    val tokens: StateFlow<Tokens?> = _tokens
    
    init {
        // Initialize with the current tokens from storage in a coroutine
        CoroutineScope(Dispatchers.Default + SupervisorJob()).launch {
            try {
                _tokens.value = tokenStorage.loadTokens()
            } finally {
                if (!initialLoad.isCompleted) initialLoad.complete(Unit)
            }
            // Start observing token changes
            tokenStorage.observeTokens()
                .collect { newTokens ->
                    _tokens.value = newTokens
                }
        }
    }

    /**
     * Suspends until the initial token load from storage has completed.
     */
    suspend fun awaitInitialLoad() {
        try {
            initialLoad.await()
        } catch (_: Throwable) { /* no-op */ }
    }
    
    /**
     * Updates the current tokens with a new authentication response.
     */
    suspend fun updateTokens(response: AuthResponse) {
        val expiresIn = response.expiresIn.toLong().toDuration(DurationUnit.SECONDS)
        val tokens = Tokens(
            accessToken = response.accessToken,
            refreshToken = response.refreshToken,
            accessTokenExpiry = Clock.System.now().plus(expiresIn).toEpochMilliseconds(),
            userId = response.user.id
        )
        _tokens.value = tokens
        tokenStorage.saveTokens(tokens)
    }
    
    /**
     * Clears all tokens (on logout).
     */
    suspend fun clearTokens() = mutex.withLock {
        _tokens.value = null
        tokenStorage.clearTokens()
    }
    
    @Serializable
    private data class RefreshTokenRequest(val refreshToken: String)

    @Serializable
    private data class RefreshTokensResponse(
        val accessToken: String,
        val refreshToken: String,
        val expiresIn: Int
    )
    @kotlinx.serialization.Serializable
    private data class WrappedTokens(val tokens: RefreshTokensResponse? = null)

    /**
     * Refreshes the access token using the refresh token.
     * @return The new tokens if refresh was successful, null otherwise
     */
    suspend fun refreshToken(): Tokens? = mutex.withLock {
        val currentTokens = _tokens.value ?: return@withLock null
        
        try {
            // Real call to POST /auth/refresh with the stored refresh token
            val payload = RefreshTokenRequest(refreshToken = currentTokens.refreshToken)
            val httpResp: HttpResponse = httpClient.post("/api/auth/refresh") {
                setBody(payload)
            }
            if (httpResp.status.value !in 200..299) {
                // Do not clear here; caller handles unauthorized via auth flow
                return@withLock null
            }
            val res: RefreshTokensResponse = try {
                httpResp.body()
            } catch (_: Exception) {
                // Try wrapped structure { tokens: { ... } }
                val wrapped: WrappedTokens = try { httpResp.body() } catch (_: Exception) { WrappedTokens(null) }
                wrapped.tokens ?: run {
                    // As last resort, parse text and fail
                    val msg = try { httpResp.bodyAsText() } catch (_: Exception) { "Invalid refresh response" }
                    throw IllegalStateException(msg.ifBlank { "Invalid refresh response" })
                }
            }

            val expiresIn = res.expiresIn.toLong().toDuration(DurationUnit.SECONDS)
            val newTokens = Tokens(
                accessToken = res.accessToken,
                refreshToken = res.refreshToken,
                accessTokenExpiry = Clock.System.now().plus(expiresIn).toEpochMilliseconds(),
                userId = currentTokens.userId
            )

            // Save the new tokens
            tokenStorage.saveTokens(newTokens)
            _tokens.value = newTokens

            return@withLock newTokens
        } catch (ce: CancellationException) {
            // Propagate coroutine cancellation
            throw ce
        } catch (e: Exception) {
            // If refresh fails, clear tokens to force reauthentication
            clearTokens()
            return@withLock null
        }
    }
    
    /**
     * Gets a valid access token, refreshing it if necessary.
     * @return The access token, or null if not authenticated or refresh failed
     */
    suspend fun getValidAccessToken(): String? = mutex.withLock {
        val currentTokens = _tokens.value ?: return@withLock null
        
        // If token is expired or about to expire (within 5 minutes), refresh it
        val now = Clock.System.now().toEpochMilliseconds()
        val bufferTime = 5 * 60 * 1000 // 5 minutes in milliseconds
        
        return@withLock if (currentTokens.accessTokenExpiry - now <= bufferTime) {
            // Token is expired or about to expire, try to refresh
            refreshToken()?.accessToken
        } else {
            // Token is still valid
            currentTokens.accessToken
        }
    }
    
    /**
     * Represents a set of authentication tokens.
     */
    @Serializable
    data class Tokens(
        val accessToken: String,
        val refreshToken: String,
        val accessTokenExpiry: Long,
        val userId: String
    ) {
        /**
         * Gets the user ID from the access token.
         * In a real app, you would decode the JWT to get the user ID.
         */
        val actualUserId: String
            get() {
                // This is a simplified example - in a real app, you would decode the JWT
                // to get the actual user ID
                return userId
            }
        
        /**
         * Converts the tokens to a JSON string for storage.
         */
        fun toJson(): String = Json.encodeToString(serializer(), this)
        
        /**
         * Converts the tokens to a user object.
         */
        fun toUser(): org.cap.gold.model.User {
            // In a real app, you would decode the JWT to get user info
            return org.cap.gold.model.User(
                id = actualUserId,
                phoneNumber = "+1234567890" // This would come from token claims
            )
        }
        
        companion object {
            /**
             * Creates a Tokens instance from a JSON string.
             */
            fun fromJson(json: String): Tokens? {
                return try {
                    Json.decodeFromString(serializer(), json)
                } catch (e: Exception) {
                    null
                }
            }
        }
    }
}

/**
 * Interface for persisting and observing authentication tokens.
 */
interface TokenStorage {
    /**
     * Saves the tokens to persistent storage.
     * @param tokens The tokens to save
     * @return true if successful, false otherwise
     */
    suspend fun saveTokens(tokens: TokenManager.Tokens)
    
    /**
     * Loads the tokens from persistent storage.
     * @return The saved tokens, or null if none exist
     */
    suspend fun loadTokens(): TokenManager.Tokens?
    
    /**
     * Clears all tokens from persistent storage.
     * @return true if successful, false otherwise
     */
    suspend fun clearTokens()
    
    /**
     * Observes changes to the stored tokens.
     * @return A Flow that emits the current tokens whenever they change.
     */
    fun observeTokens(): Flow<TokenManager.Tokens?>
}
