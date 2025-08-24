package org.cap.gold.network

import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.java.Java

// JVM implementation of the HTTP client engine
actual val httpClientEngine: HttpClientEngineFactory<*> = Java

// JVM implementation of Ktor initialization
actual fun initKtor() {
    // No special initialization needed for JVM
}
