package org.cap.gold.di

import org.cap.gold.auth.AuthService
import org.cap.gold.auth.KtorAuthService
import org.cap.gold.auth.TokenManager
import org.cap.gold.auth.TokenStorage
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

/**
 * Koin module that provides authentication-related dependencies.
 */
val authModule = module {
    // Token storage is platform-specific
    single<TokenStorage> {
        // This will be overridden by platform-specific modules
        error("No TokenStorage implementation provided. Make sure to include the platform-specific module.")
    }
    
    single {
        TokenManager(
            baseUrl = getProperty<String>("api.base.url"),
            tokenStorage = get()
        )
    }
    
    single<AuthService> {
        KtorAuthService(
            baseUrl = getProperty("api.base.url"),
            tokenManager = get()
        )
    }
}

/**
 * Common configuration for the auth module.
 */
fun configureAuth() {
    // Any common auth configuration can go here
}
