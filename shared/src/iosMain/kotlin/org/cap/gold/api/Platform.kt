package org.cap.gold.api

import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.darwin.Darwin
import org.cap.gold.network.httpClientEngine as networkHttpClientEngine
import org.cap.gold.network.initKtor as initNetworkKtor

// HTTP Client Configuration
actual val httpClientEngine: HttpClientEngineFactory<*> = Darwin
actual fun initKtor() = initNetworkKtor()
