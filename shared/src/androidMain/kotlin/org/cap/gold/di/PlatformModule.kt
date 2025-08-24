package org.cap.gold.di

import org.cap.gold.auth.AndroidTokenStorage
import org.cap.gold.auth.TokenStorage
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Gets the platform-specific Koin modules for Android.
 */
actual fun getPlatformModules(): List<Module> = listOf(
    androidAuthModule,
    module {
        // Add other Android-specific dependencies here
    }
)
