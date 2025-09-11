package org.cap.gold.auth

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSUserDefaults
import platform.Foundation.NSUserDefaultsDidChangeNotification

/**
 * iOS implementation of [TokenStorage] using NSUserDefaults for storage.
 */
class IosTokenStorage : TokenStorage {
    private val serviceName = "org.cap.gold.auth"
    
    override suspend fun saveTokens(tokens: TokenManager.Tokens): Unit {
        withContext(Dispatchers.Default) {
            try {
                val defaults = NSUserDefaults.standardUserDefaults
                defaults.setObject(tokens.accessToken, forKey = "${serviceName}_access_token")
                defaults.setObject(tokens.refreshToken, forKey = "${serviceName}_refresh_token")
                defaults.setDouble(tokens.accessTokenExpiry.toDouble(), forKey = "${serviceName}_access_token_expiry")
                defaults.setObject(tokens.userId, forKey = "${serviceName}_user_id")
                defaults.synchronize()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    override suspend fun loadTokens(): TokenManager.Tokens? = withContext(Dispatchers.Default) {
        try {
            val defaults = NSUserDefaults.standardUserDefaults
            val accessToken = defaults.stringForKey("${serviceName}_access_token")
            val refreshToken = defaults.stringForKey("${serviceName}_refresh_token")
            val expiry = defaults.doubleForKey("${serviceName}_access_token_expiry").toLong()
            val userId = defaults.stringForKey("${serviceName}_user_id") ?: ""
            
            if (!accessToken.isNullOrEmpty() && !refreshToken.isNullOrEmpty() && expiry > 0 && userId.isNotEmpty()) {
                TokenManager.Tokens(
                    accessToken = accessToken,
                    refreshToken = refreshToken,
                    accessTokenExpiry = expiry,
                    userId = userId
                )
            } else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    override suspend fun clearTokens(): Unit {
        withContext(Dispatchers.Default) {
            try {
                val defaults = NSUserDefaults.standardUserDefaults
                defaults.removeObjectForKey("${serviceName}_access_token")
                defaults.removeObjectForKey("${serviceName}_refresh_token")
                defaults.removeObjectForKey("${serviceName}_access_token_expiry")
                defaults.removeObjectForKey("${serviceName}_user_id")
                defaults.synchronize()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    override fun observeTokens(): Flow<TokenManager.Tokens?> = callbackFlow {
        var currentTokens: TokenManager.Tokens? = null
        // Emit initial value from storage in a coroutine
        val initialJob = launch(Dispatchers.Default) {
            currentTokens = loadTokens()
            trySend(currentTokens)
        }
        
        // Observe UserDefaults changes
        val observer = NSNotificationCenter.defaultCenter.addObserverForName(
            name = NSUserDefaultsDidChangeNotification,
            `object` = null,
            queue = null
        ) { _ ->
            launch(Dispatchers.Default) {
                val newTokens = loadTokens()
                if (newTokens != currentTokens) {
                    currentTokens = newTokens
                    trySend(currentTokens)
                }
            }
        }
        
        // Clean up when the flow is cancelled
        awaitClose {
            try { initialJob.cancel() } catch (_: Throwable) {}
            NSNotificationCenter.defaultCenter.removeObserver(observer)
        }
    }.flowOn(Dispatchers.Default)
}
