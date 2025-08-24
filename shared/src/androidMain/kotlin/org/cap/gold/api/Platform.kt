package org.cap.gold.api

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.android.Android
import org.cap.gold.BuildConfig
import org.cap.gold.network.httpClientEngine as networkHttpClientEngine
import org.cap.gold.network.initKtor as initNetworkKtor

// HTTP Client Configuration
actual val httpClientEngine: HttpClientEngineFactory<*> = Android
actual fun initKtor() = initNetworkKtor()
