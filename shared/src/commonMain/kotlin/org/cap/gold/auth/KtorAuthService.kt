package org.cap.gold.auth

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.util.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import kotlin.coroutines.coroutineContext
import org.cap.gold.auth.model.AuthResult
import org.cap.gold.auth.model.AuthResponse
import org.cap.gold.auth.model.requests.EmailSignInRequest
import org.cap.gold.auth.model.requests.EmailSignUpRequest
import org.cap.gold.model.User
import org.cap.gold.util.validatePhoneNumber
import org.cap.gold.util.validatePassword
import org.cap.gold.PlatformInfo
import org.cap.gold.api.ApiClient

/**
 * Implementation of [AuthService] using Ktor client for network requests.
 * Handles user authentication, token management, and user session state.
 *
 * @property baseUrl The base URL of the authentication server
 * @property tokenManager Handles token storage and refresh logic
 */
class KtorAuthService(
    private val baseUrl: String,
    private val tokenManager: TokenManager,
    private val deviceTokenProvider: DeviceTokenProvider
) : AuthService, CoroutineScope by CoroutineScope(Dispatchers.Default + SupervisorJob()) {
    // Server response shape: { user: User, tokens: { accessToken, refreshToken, expiresIn } }
    @kotlinx.serialization.Serializable
    private data class ServerTokens(
        val accessToken: String,
        val refreshToken: String,
        val expiresIn: Int
    )

    @kotlinx.serialization.Serializable
    private data class ServerAuthResponse(
        val user: User,
        val tokens: ServerTokens
    )

    private fun ServerAuthResponse.toClient(): AuthResponse = AuthResponse(
        user = this.user,
        accessToken = this.tokens.accessToken,
        refreshToken = this.tokens.refreshToken,
        expiresIn = this.tokens.expiresIn,
        isNewUser = false
    )
    
    @kotlinx.serialization.Serializable
    private data class ServerError(
        val error: String? = null,
        val code: Int? = null
    )

    private suspend fun HttpResponse.safeParseAuth(): Result<AuthResponse> {
        return try {
            // Try server shape first
            Result.success(this.body<ServerAuthResponse>().toClient())
        } catch (_: Exception) {
            try {
                // Try client flat shape (if backend changed)
                Result.success(this.body<AuthResponse>())
            } catch (_: Exception) {
                // Fallback to text to surface meaningful error
                val text = try { this.bodyAsText() } catch (_: Exception) { "" }
                Result.failure(IllegalStateException(text.ifBlank { "Invalid auth response from server" }))
            }
        }
    }
    // Private properties for managing state
    private var _currentUserValue: User? = null
    private val _authState = MutableStateFlow<User?>(null)
    private val _isLoading = MutableStateFlow(false)
    private val _error = MutableStateFlow<AuthErrorHandler.AuthErrorType?>(null)
    // Monotonic epoch used to invalidate in-flight auth checks on logout
    private var authEpoch: Int = 0
    private fun log(msg: String) {
        if (PlatformInfo.isDebug) println("[Auth] $msg")
    }
    // Cache device token to avoid repeated slow fetches
    private var cachedDeviceToken: String? = null
    init {
        // Prefetch device token in background; ignore failures
        launch {
            val t = runCatching { withTimeoutOrNull(300) { deviceTokenProvider.getDeviceToken() } }.getOrNull()
            if (t != null) {
                cachedDeviceToken = t
                log("prefetched device token")
            }
        }
    }
    private suspend fun getDeviceTokenFast(): String? {
        cachedDeviceToken?.let { return it }
        val t = withTimeoutOrNull(300) { deviceTokenProvider.getDeviceToken() }
        if (t != null) cachedDeviceToken = t
        return t
    }
    
    // Build a new HTTP client with all necessary configurations
    private fun buildClient(): HttpClient = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            })
        }

        defaultRequest {
            url(baseUrl)
            contentType(ContentType.Application.Json)
        }

        // Configure timeouts
        install(HttpTimeout) {
            requestTimeoutMillis = 30000
            connectTimeoutMillis = 30000
            socketTimeoutMillis = 30000
        }

        // Only enable logging in debug builds
        if (PlatformInfo.isDebug) {
            install(Logging) {
                level = LogLevel.ALL
            }
        }

        // Install auth provider for automatic token refresh
        install(Auth) {
            bearer {
                loadTokens {
                    tokenManager.tokens.value?.let { tokens ->
                        BearerTokens(
                            accessToken = tokens.accessToken,
                            refreshToken = tokens.refreshToken
                        )
                    }
                }

                refreshTokens {
                    try {
                        // Try to refresh the token
                        val newTokens = tokenManager.refreshToken()
                        if (newTokens != null) {
                            BearerTokens(
                                accessToken = newTokens.accessToken,
                                refreshToken = newTokens.refreshToken
                            )
                        } else {
                            // If refresh fails, clear tokens and return null
                            tokenManager.clearTokens()
                            null
                        }
                    } catch (e: Exception) {
                        // If there's an error, clear tokens and return null
                        tokenManager.clearTokens()
                        null
                    }
                }

                sendWithoutRequest { request ->
                    // Send Authorization for all requests EXCEPT public auth endpoints
                    val path = request.url.encodedPath
                    !(path.endsWith("/api/auth/signin/email") ||
                      path.endsWith("/api/auth/signup/email") ||
                      path.endsWith("/api/auth/refresh"))
                }
            }
        }
    }

    // Mutable client instance so we can rebuild it on logout to drop any cached tokens
    private var client: HttpClient = buildClient()
    
    // Region: AuthService Implementation
    
    override val isLoading: StateFlow<Boolean> = _isLoading
    override val error: StateFlow<String?> = _error.map { it?.message }.stateIn(
        scope = this,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )
    
    val errorType: StateFlow<AuthErrorHandler.AuthErrorType?> = _error.asStateFlow()
    override val currentUser: User? get() = _currentUserValue
    override val authState: StateFlow<User?> = _authState

    private fun clearError() {
        _error.value = null
    }
    
    private fun setError(errorType: AuthErrorHandler.AuthErrorType) {
        _error.value = errorType
    }

    override suspend fun signInWithEmail(email: String, password: String): AuthResult<User> =
        withContext(coroutineContext) {
        _isLoading.value = true
        _error.value = null
        
        try {
            if (!validatePhoneNumber(email).success) {
                return@withContext AuthResult.Error("Invalid Phone Number")
            }

            if (password.isBlank() || password.length < 6) {
                return@withContext AuthResult.Error("Password cannot be empty")
            }
            
            // Make the API call with manual status handling; do not block on device token
            val token = try { getDeviceTokenFast() } catch (_: Exception) { null }
            val httpResponse = client.post("$baseUrl/api/auth/signin/email") {
                contentType(ContentType.Application.Json)
                setBody(EmailSignInRequest(email = email, password = password, deviceToken = token))
            }
            val response: AuthResponse = if (httpResponse.status.value in 200..299) {
                val parsed = httpResponse.safeParseAuth()
                if (parsed.isSuccess) parsed.getOrThrow()
                else return@withContext AuthResult.Error(parsed.exceptionOrNull()?.message ?: "Sign in failed")
            } else {
                // Try parse structured server error, else text
                val message = try {
                    httpResponse.body<ServerError>().error ?: httpResponse.bodyAsText()
                } catch (_: Exception) {
                    httpResponse.bodyAsText()
                }
                return@withContext AuthResult.Error(message.ifBlank { "Sign in failed" })
            }
            
            // Update tokens and state
            tokenManager.updateTokens(response)
            // Sync legacy ApiClient header for other services still using it
            ApiClient.updateAuthToken(response.accessToken)
            _currentUserValue = response.user
            _authState.value = response.user
            
            AuthResult.Success(response.user)
        } catch (ce: CancellationException) {
            // Propagate coroutine cancellation without converting to error
            throw ce
        } catch (e: Exception) {
            val errorType = AuthErrorHandler.handleAuthError(e)
            _error.value = errorType
            AuthResult.Error(errorType.message, null) // Using null as error code
        } finally {
            _isLoading.value = false
        }
    }

    override suspend fun createUserWithEmail(
        email: String,
        password: String,
        phoneNumber: String,
        displayName: String?
    ): AuthResult<User> = withContext(coroutineContext) {
        _isLoading.value = true
        _error.value = null
        
        try {
//            if (!email.contains("@") || !email.contains(".")) {
//                return@withContext AuthResult.Error("Invalid email address")
//            }
            if (password.length < 6) {
                return@withContext AuthResult.Error("Password must be at least 6 characters")
            }
            if (phoneNumber.isBlank() || phoneNumber.length < 10) {
                return@withContext AuthResult.Error("Please enter a valid phone number (at least 10 digits)")
            }
            
            // Make the API call with manual status handling
            val token = try { getDeviceTokenFast() } catch (_: Exception) { null }
            val httpResponse = client.post("$baseUrl/api/auth/signup/email") {
                contentType(ContentType.Application.Json)
                setBody(EmailSignUpRequest(email = email, password = password, phoneNumber = phoneNumber, displayName = displayName))
            }
            val response: AuthResponse = if (httpResponse.status.value in 200..299) {
                val parsed = httpResponse.safeParseAuth()
                if (parsed.isSuccess) parsed.getOrThrow()
                else return@withContext AuthResult.Error(parsed.exceptionOrNull()?.message ?: "Registration failed")
            } else {
                val message = try {
                    httpResponse.body<ServerError>().error ?: httpResponse.bodyAsText()
                } catch (_: Exception) {
                    httpResponse.bodyAsText()
                }
                return@withContext AuthResult.Error(message.ifBlank { "Registration failed" })
            }
            
            // Update tokens and state
            tokenManager.updateTokens(response)
            // Sync legacy ApiClient header for other services still using it
            ApiClient.updateAuthToken(response.accessToken)
            _currentUserValue = response.user
            _authState.value = response.user
            
            AuthResult.Success(response.user)
        } catch (ce: CancellationException) {
            // Propagate coroutine cancellation without converting to error
            throw ce
        } catch (e: Exception) {
            val errorType = AuthErrorHandler.handleAuthError(e)
            _error.value = errorType
            AuthResult.Error(errorType.message, null) // Using null as error code
        } finally {
            _isLoading.value = false
        }
    }

    
    override suspend fun signOut() {
        _isLoading.value = true
        _error.value = null
        
        try {
            // Cancel any in-flight child jobs (e.g., checkAuthState) to avoid races
            coroutineContext.cancelChildren()
            // Clear tokens
            tokenManager.clearTokens()
            // Clear legacy ApiClient header used by other API services
            ApiClient.clearAuthToken()
            
            // Update state
            _currentUserValue = null
            _authState.value = null
            log("signOut(): cleared tokens and state")

            // Rebuild client to ensure any cached bearer tokens in plugin are dropped
            try {
                client.close()
            } catch (_: Exception) { /* ignore */ }
            client = buildClient()

            // Bump epoch so any in-flight checkAuthState started before this point is ignored
            authEpoch++
            log("signOut(): bumped epoch to $authEpoch and rebuilt client")
        } catch (ce: CancellationException) {
            // Propagate coroutine cancellation without converting to error
            throw ce
        } catch (ce: CancellationException) {
            // Propagate coroutine cancellation without converting to error
            throw ce
        } catch (e: Exception) {
            val errorType = AuthErrorHandler.handleAuthError(e)
            _error.value = errorType
        } finally {
            _isLoading.value = false
        }
    }
    
    override suspend fun checkAuthState() {
        _isLoading.value = true
        _error.value = null
        
        try {
            // Capture the current epoch to guard against races with logout
            val startEpoch = authEpoch
            log("checkAuthState(start): epoch=$startEpoch tokensLoaded=${tokenManager.tokens.value != null}")
            // Ensure tokens have been loaded from storage before we decide
            tokenManager.awaitInitialLoad()
            // Check if we have valid tokens
            val tokens = tokenManager.tokens.value
            if (tokens == null) {
                _currentUserValue = null
                _authState.value = null
                log("checkAuthState: no tokens -> set state=null and return")
                return
            }
            
            // Try to get the current user
            val user = try {
                val httpResponse = client.get("$baseUrl/api/auth/me") {
                    contentType(ContentType.Application.Json)
                }
                val response: AuthResponse = if (httpResponse.status.value in 200..299) {
                    val parsed = httpResponse.safeParseAuth()
                    if (parsed.isSuccess) parsed.getOrThrow()
                    else throw ClientRequestException(httpResponse, parsed.exceptionOrNull()?.message ?: "Invalid session")
                } else {
                    throw ClientRequestException(httpResponse, "Unauthorized or invalid session")
                }
                
                // Update tokens in case they were refreshed
                tokenManager.updateTokens(response)
                log("checkAuthState: /me success, tokens refreshed; candidate user=${response.user.id}")
                response.user
            } catch (ce: CancellationException) {
                // Propagate coroutine cancellation to caller
                throw ce
            } catch (e: Exception) {
                // If we can't get the current user, clear tokens and state
                if (e is ClientRequestException && e.response.status == HttpStatusCode.Unauthorized) {
                    // Try a one-time refresh then retry /me
                    val refreshed = tokenManager.refreshToken()
                    if (refreshed != null) {
                        log("checkAuthState: unauthorized -> refreshed tokens, retrying /me")
                        val retryResp = client.get("$baseUrl/api/auth/me") { contentType(ContentType.Application.Json) }
                        if (retryResp.status.value in 200..299) {
                            val parsed = retryResp.safeParseAuth()
                            if (parsed.isSuccess) {
                                val ok = parsed.getOrThrow()
                                tokenManager.updateTokens(ok)
                                log("checkAuthState: retry /me success for user=${ok.user.id}")
                                ok.user
                            } else {
                                tokenManager.clearTokens()
                                _currentUserValue = null
                                _authState.value = null
                                log("checkAuthState: retry parse failed -> cleared tokens")
                                return
                            }
                        } else {
                            tokenManager.clearTokens()
                            _currentUserValue = null
                            _authState.value = null
                            log("checkAuthState: retry /me still unauthorized -> cleared tokens")
                            return
                        }
                    } else {
                        tokenManager.clearTokens()
                        _currentUserValue = null
                        _authState.value = null
                        log("checkAuthState: unauthorized and refresh failed -> cleared tokens and state")
                    }
                } else {
                    // For other errors, just log them but don't clear the auth state
                    val errorType = AuthErrorHandler.handleAuthError(e)
                    _error.value = errorType
                    log("checkAuthState: error=${e.message}")
                }
                return
            }
            
            // Update state only if epoch has not changed (i.e., no logout since start)
            if (startEpoch == authEpoch) {
                _currentUserValue = user
                _authState.value = user
                log("checkAuthState(end): epoch ok ($authEpoch) -> set state to user=${user.id}")
            } else {
                log("checkAuthState(end): epoch changed (start=$startEpoch current=$authEpoch) -> ignore result")
            }
        } catch (e: Exception) {
            val errorType = AuthErrorHandler.handleAuthError(e)
            _error.value = errorType
        } finally {
            _isLoading.value = false
        }
    }
    
    /**
     * Clean up resources when this service is no longer needed
     */
    fun dispose() {
        client.close()
        cancel()
    }
}
