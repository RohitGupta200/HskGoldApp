package org.cap.gold.di

import org.cap.gold.api.ApiClient
import org.cap.gold.auth.KtorAuthService
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

/**
 * Initialize Koin dependency injection with the given base URL.
 *
 * @param baseUrl The base URL of the API server
 * @param enableNetworkLogs Whether to enable network request/response logging
 * @param appDeclaration Additional Koin configuration
 */
fun initKoin(
    baseUrl: String = "http://10.0.2.2:8080", // Default to Android emulator's localhost
    enableNetworkLogs: Boolean = false,
    appDeclaration: KoinAppDeclaration = {}
) = startKoin {
    appDeclaration()
    
    // Initialize API client with the base URL
    ApiClient.init(baseUrl)
    
    // Configure auth
    configureAuth()
    
    // Load the common modules
    val commonModules = mutableListOf(
        commonModule(),
        authModule
    )
    
    // Add platform-specific modules
    val platformModules = getPlatformModules()
    
    modules(commonModules + platformModules)
    
    // Set properties
    properties(
        mapOf(
            "api.base.url" to baseUrl,
            "api.logging.enabled" to enableNetworkLogs
        )
    )
}

/**
 * Common Koin module that provides shared dependencies.
 */
fun commonModule() = module {
    includes(authModule, productModule, orderModule, userModule, profileModule)
}

/**
 * Gets the platform-specific Koin modules.
 */
expect fun getPlatformModules(): List<Module>

// For iOS
fun initKoin(baseUrl: String, enableNetworkLogs: Boolean = false) = 
    initKoin(baseUrl, enableNetworkLogs) {}

// For tests
fun initKoinForTests() = initKoin("http://localhost:8080")
