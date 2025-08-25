package org.cap.gold.auth

/**
 * Abstraction to retrieve a device push token (e.g., FCM/APNs) on each platform.
 */
interface DeviceTokenProvider {
    /** Returns current push token or null if unavailable. */
    suspend fun getDeviceToken(): String?

    object Noop : DeviceTokenProvider {
        override suspend fun getDeviceToken(): String? = null
    }
}
