package org.cap.gold.network

import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.android.Android

/**
 * Android implementation of the HTTP client engine using Android's native HTTP stack.
 */
actual val httpClientEngine: HttpClientEngineFactory<*> = Android

/**
 * Initializes Ktor for Android platform.
 * This is a no-op on Android as the Android engine doesn't require explicit initialization.
 */
actual fun initKtor() {
    // No-op on Android
}
