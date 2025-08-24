package org.cap.gold.di

import org.koin.core.module.Module

/**
 * Gets the platform-specific Koin modules for iOS.
 */
actual fun getPlatformModules(): List<Module> = listOf(
    iosAuthModule,
    // Add other iOS-specific modules here
)
