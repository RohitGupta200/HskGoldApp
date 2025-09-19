package org.cap.gold.di

import org.cap.gold.auth.IosTokenStorage
import org.cap.gold.auth.TokenStorage
import org.cap.gold.auth.DeviceTokenProvider
import org.cap.gold.auth.IosDeviceTokenProvider
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

/**
 * iOS-specific Koin module for authentication dependencies.
 */
val iosAuthModule = module {
    // Provide iOS-specific TokenStorage implementation
    single<TokenStorage> { IosTokenStorage() }
    // Provide iOS device token provider (reads from PushTokenRegistry set by Swift)
    single<DeviceTokenProvider> { IosDeviceTokenProvider() }
}
