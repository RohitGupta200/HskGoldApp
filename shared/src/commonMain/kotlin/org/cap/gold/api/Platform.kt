package org.cap.gold.api

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.HttpClientEngineFactory

// This is a common interface for platform-specific HTTP client engines
expect val httpClientEngine: HttpClientEngineFactory<*>

/**
 * Platform-specific initialization that should be called when the application starts.
 * On iOS, this ensures the Darwin engine is properly initialized.
 * On Android, this is a no-op.
 */
expect fun initKtor()
