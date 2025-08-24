package org.cap.gold.network

import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.darwin.Darwin

/**
 * iOS implementation of the HTTP client engine using Darwin (NSURLSession).
 */
actual val httpClientEngine: HttpClientEngineFactory<*> = Darwin

/**
 * Initializes Ktor for iOS platform.
 * This is a no-op on iOS as the Darwin engine doesn't require explicit initialization.
 */
actual fun initKtor() {
    // No-op on iOS
}
