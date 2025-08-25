package org.cap.gold.di

import android.content.Context
import org.cap.gold.auth.AndroidTokenStorage
import org.cap.gold.auth.TokenStorage
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import org.cap.gold.auth.DeviceTokenProvider
import org.cap.gold.auth.AndroidDeviceTokenProvider

/**
 * Android-specific Koin module for authentication dependencies.
 */
val androidAuthModule = module {
    // Provide Android-specific TokenStorage implementation
    single<TokenStorage> { 
        AndroidTokenStorage(
            context = androidContext()
        )
    }

    // Provide Android device push token provider (FCM)
    single<DeviceTokenProvider> { AndroidDeviceTokenProvider() }
}
