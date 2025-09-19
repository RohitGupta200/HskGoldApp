package org.cap.gold.auth

/**
 * Android implementation of PushTokenRegistry.
 * Android doesn't need persistent storage since DeviceTokenProvider queries FCM directly.
 * This is kept for compatibility but isn't used in practice on Android.
 */
actual object PushTokenRegistry {
    private var token: String? = null

    actual fun set(token: String?) {
        this.token = token?.trim().takeUnless { it.isNullOrEmpty() }
    }

    actual fun get(): String? = token
}