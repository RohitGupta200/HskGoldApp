package org.cap.gold.auth

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.cap.gold.auth.TokenManager.Tokens

/**
 * Android implementation of [TokenStorage] using EncryptedSharedPreferences.
 * @property context The Android context
 */
class AndroidTokenStorage(
    private val context: Context
) : TokenStorage {
    private val prefs by lazy {
        val masterKey = MasterKey.Builder(context, "auth_tokens_master_key")
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            "secure_auth_tokens",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    override suspend fun saveTokens(tokens: TokenManager.Tokens) = withContext(Dispatchers.IO) {
        try {
            prefs.edit()
                .putString(KEY_ACCESS_TOKEN, tokens.accessToken)
                .putString(KEY_REFRESH_TOKEN, tokens.refreshToken)
                .putLong(KEY_ACCESS_TOKEN_EXPIRY, tokens.accessTokenExpiry)
                .putString(KEY_USER_ID, tokens.userId) // Save userId
                .apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override suspend fun loadTokens(): TokenManager.Tokens? = withContext(Dispatchers.IO) {
        try {
            val accessToken = prefs.getString(KEY_ACCESS_TOKEN, null)
            val refreshToken = prefs.getString(KEY_REFRESH_TOKEN, null)
            val expiry = prefs.getLong(KEY_ACCESS_TOKEN_EXPIRY, 0L)
            val userId = prefs.getString(KEY_USER_ID, "") ?: ""

            if (!accessToken.isNullOrEmpty() && !refreshToken.isNullOrEmpty() && expiry > 0 && userId.isNotEmpty()) {
                TokenManager.Tokens(
                    accessToken = accessToken,
                    refreshToken = refreshToken,
                    accessTokenExpiry = expiry,
                    userId = userId
                )
            } else {
                clearTokens()
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            clearTokens()
            null
        }
    }

    override suspend fun clearTokens() = withContext(Dispatchers.IO) {
        try {
            prefs.edit()
                .remove(KEY_ACCESS_TOKEN)
                .remove(KEY_REFRESH_TOKEN)
                .remove(KEY_ACCESS_TOKEN_EXPIRY)
                .remove(KEY_USER_ID) // Clear userId as well
                .apply()
        } catch (e: Exception) {
            // Log the error if needed
            e.printStackTrace()
        }
    }

    override fun observeTokens(): Flow<TokenManager.Tokens?> = callbackFlow {
        // Emit the current tokens
        var currentTokens = loadTokens()
        trySend(currentTokens)
        
        // Create a listener for preference changes
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_ACCESS_TOKEN || key == KEY_REFRESH_TOKEN || 
                key == KEY_ACCESS_TOKEN_EXPIRY || key == KEY_USER_ID) {
                // Tokens changed, load and emit the new ones
                CoroutineScope(Dispatchers.Default).launch {
                currentTokens = loadTokens()
                trySend(currentTokens)
            }
            }
        }
        
        // Register the listener
        prefs.registerOnSharedPreferenceChangeListener(listener)
        
        // Unregister the listener when the flow is cancelled
        awaitClose {
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }.flowOn(Dispatchers.IO)

    companion object {
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_ACCESS_TOKEN_EXPIRY = "access_token_expiry"
        private const val KEY_USER_ID = "user_id"
    }
}
