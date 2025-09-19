package org.cap.gold.auth

/**
 * iOS implementation of DeviceTokenProvider that reads from persistent storage.
 * The token is persisted via NSUserDefaults and survives app restarts.
 */
class IosDeviceTokenProvider : DeviceTokenProvider {
    override suspend fun getDeviceToken(): String? = PushTokenRegistry.get()
}
