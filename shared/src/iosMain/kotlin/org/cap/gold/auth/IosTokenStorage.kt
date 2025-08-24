package org.cap.gold.auth

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSUserDefaults
import platform.Foundation.NSUserDefaultsDidChangeNotification
import platform.Security.*
import platform.darwin.OSStatus
import kotlinx.cinterop.*

/**
 * iOS implementation of [TokenStorage] using Keychain for secure storage.
 */
class IosTokenStorage : TokenStorage {
    private val serviceName = "org.cap.gold.auth"
    
    override suspend fun saveTokens(tokens: TokenManager.Tokens) = withContext(Dispatchers.Default) {
        try {
            val accessTokenData = tokens.accessToken.encodeToByteArray()
            val refreshTokenData = tokens.refreshToken.encodeToByteArray()
            
            // Save access token
            saveToKeychain(
                account = "access_token",
                data = accessTokenData,
                additionalQuery = mapOf(
                    kSecAttrService to serviceName,
                    kSecAttrAccessible to kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly,
                    kSecAttrAccessGroup to null
                )
            )
            
            // Save refresh token
            saveToKeychain(
                account = "refresh_token",
                data = refreshTokenData,
                additionalQuery = mapOf(
                    kSecAttrService to serviceName,
                    kSecAttrAccessible to kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly,
                    kSecAttrAccessGroup to null
                )
            )
            
            // Save expiry and userId in UserDefaults (not sensitive)
            val defaults = NSUserDefaults.standardUserDefaults
            defaults.setDouble(
                tokens.accessTokenExpiry.toDouble(),
                forKey = "${serviceName}_access_token_expiry"
            )
            defaults.setObject(
                tokens.userId,
                forKey = "${serviceName}_user_id"
            )
            defaults.synchronize()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    override suspend fun loadTokens(): TokenManager.Tokens? = withContext(Dispatchers.Default) {
        try {
            val accessToken = loadFromKeychain("access_token")?.decodeToString()
            val refreshToken = loadFromKeychain("refresh_token")?.decodeToString()
            val expiry = NSUserDefaults.standardUserDefaults.doubleForKey("${serviceName}_access_token_expiry")
                .toLong()
            val userId = NSUserDefaults.standardUserDefaults.stringForKey("${serviceName}_user_id") ?: ""
            
            if (!accessToken.isNullOrEmpty() && !refreshToken.isNullOrEmpty() && expiry > 0 && userId.isNotEmpty()) {
                TokenManager.Tokens(
                    accessToken = accessToken,
                    refreshToken = refreshToken,
                    accessTokenExpiry = expiry,
                    userId = userId
                )
            } else {
                // Clean up if data is corrupted
                clearTokens()
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            clearTokens()
            null
        }
    }
    
    override suspend fun clearTokens() = withContext(Dispatchers.Default) {
        try {
            // Delete from Keychain
            deleteFromKeychain("access_token")
            deleteFromKeychain("refresh_token")
            
            // Delete from UserDefaults
            val defaults = NSUserDefaults.standardUserDefaults
            defaults.removeObjectForKey("${serviceName}_access_token_expiry")
            defaults.removeObjectForKey("${serviceName}_user_id")
            defaults.synchronize()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    override fun observeTokens(): Flow<TokenManager.Tokens?> = callbackFlow {
        // Emit the current tokens
        var currentTokens = loadTokens()
        trySend(currentTokens)
        
        // Observe UserDefaults changes
        val observer = NSNotificationCenter.defaultCenter.addObserverForName(
            name = NSUserDefaultsDidChangeNotification,
            `object` = null,
            queue = null
        ) { _ ->
            // When UserDefaults change, check if tokens have changed
            val newTokens = loadTokens()
            if (newTokens != currentTokens) {
                currentTokens = newTokens
                trySend(currentTokens)
            }
        }
        
        // Clean up when the flow is cancelled
        awaitClose {
            NSNotificationCenter.defaultCenter.removeObserver(observer)
        }
    }.flowOn(Dispatchers.Default)
    
    private fun saveToKeychain(account: String, data: ByteArray, additionalQuery: Map<CFTypeRef, Any?>): Boolean {
        val query = mutableMapOf<CFTypeRef, Any?>().apply {
            put(kSecClass, kSecClassGenericPassword)
            put(kSecAttrAccount, account)
            put(kSecAttrService, serviceName)
            putAll(additionalQuery)
        }
        
        val status = SecItemCopyMatching(query, null).toInt()
        
        val attributes = mutableMapOf<CFTypeRef, Any?>(
            kSecValueData to data
        )
        
        return when (status) {
            errSecSuccess -> {
                // Item exists, update it
                SecItemUpdate(query, attributes).let { result ->
                    result == errSecSuccess.toUInt()
                }
            }
            errSecItemNotFound -> {
                // Item doesn't exist, add it
                query.putAll(attributes)
                SecItemAdd(query, null).let { result ->
                    result == errSecSuccess.toUInt()
                }
            }
            else -> {
                println("Keychain save error: $status")
                false
            }
        }
    }
    
    private fun loadFromKeychain(account: String): ByteArray? {
        val query = mapOf<CFTypeRef, Any?>(
            kSecClass to kSecClassGenericPassword,
            kSecAttrService to serviceName,
            kSecAttrAccount to account,
            kSecReturnData to true,
            kSecMatchLimit to kSecMatchLimitOne
        )
        
        val result = mutableListOf<Any?>()
        val status = SecItemCopyMatching(query, result).toInt()
        
        return if (status == errSecSuccess) {
            (result.firstOrNull() as? ByteArray)
        } else {
            null
        }
    }
    
    private fun deleteFromKeychain(account: String): Boolean {
        val query = mapOf<CFTypeRef, Any?>(
            kSecClass to kSecClassGenericPassword,
            kSecAttrService to serviceName,
            kSecAttrAccount to account
        )
        
        return SecItemDelete(query).toInt() == errSecSuccess
    }
}
