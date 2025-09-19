package org.cap.gold.auth

/**
 * Registry to store the current device push token with persistent storage.
 * On Android, DeviceTokenProvider queries FCM directly.
 * On iOS, this provides persistent storage for FCM tokens.
 */
expect object PushTokenRegistry {
    fun set(token: String?)
    fun get(): String?
}
