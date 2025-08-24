package org.cap.gold.api

import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.java.Java

// JVM actuals for common expect declarations
actual val httpClientEngine: HttpClientEngineFactory<*> = Java

actual fun initKtor() {
    // No special initialization needed on JVM
}
