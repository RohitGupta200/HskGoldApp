package org.cap.gold.auth

import platform.Foundation.NSUserDefaults

/**
 * iOS implementation of PushTokenRegistry using NSUserDefaults for persistence.
 * This ensures FCM tokens survive app restarts.
 */
actual object PushTokenRegistry {
    private const val PUSH_TOKEN_KEY = "org.cap.gold.auth.push_token"

    actual fun set(token: String?) {
        val cleanToken = token?.trim().takeUnless { it.isNullOrEmpty() }
        val defaults = NSUserDefaults.standardUserDefaults

        if (cleanToken != null) {
            defaults.setObject(cleanToken, forKey = PUSH_TOKEN_KEY)
        } else {
            defaults.removeObjectForKey(PUSH_TOKEN_KEY)
        }
        defaults.synchronize()
    }

    actual fun get(): String? {
        val defaults = NSUserDefaults.standardUserDefaults
        return defaults.stringForKey(PUSH_TOKEN_KEY)?.takeUnless { it.isEmpty() }
    }
}