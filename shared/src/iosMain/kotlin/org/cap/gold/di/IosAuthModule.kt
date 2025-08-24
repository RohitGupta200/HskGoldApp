package org.cap.gold.di

import org.cap.gold.auth.IosTokenStorage
import org.cap.gold.auth.TokenStorage
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

/**
 * iOS-specific Koin module for authentication dependencies.
 */
val iosAuthModule = module {
    // Provide iOS-specific TokenStorage implementation
    single<TokenStorage> { IosTokenStorage() }
}
