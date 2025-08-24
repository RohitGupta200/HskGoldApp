package org.cap.gold.network

import io.ktor.client.engine.HttpClientEngineFactory

expect val httpClientEngine: HttpClientEngineFactory<*>

/**
 * Platform-specific initialization that should be called when the application starts.
 * On iOS, this ensures the Darwin engine is properly initialized.
 * On Android, this is a no-op.
 */
expect fun initKtor()
