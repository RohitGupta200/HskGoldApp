package org.cap.gold.auth

/**
 * JVM implementation of PushTokenRegistry.
 * Server doesn't need push token functionality, so this is a no-op implementation.
 */
actual object PushTokenRegistry {
    actual fun set(token: String?) {
        // No-op: Server doesn't handle client push tokens
    }

    actual fun get(): String? {
        // No-op: Server doesn't store client push tokens
        return null
    }
}