@file:kotlin.jvm.JvmName("ComposeAppDi")
package org.cap.gold.di

import org.cap.gold.data.network.AuthManager
import org.koin.core.context.startKoin
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.cap.gold.ui.screens.admin.AdminOrdersViewModel
import org.cap.gold.ui.screens.orders.OrdersViewModel

/**
 * Main entry point for Koin dependency injection setup
 * @param baseUrl Base URL for API endpoints
 * @param isDebug Enable debug features like logging
 */
fun initKoin(
    baseUrl: String,
    isDebug: Boolean = false,
    authToken: String? = null
) {
    // Store base URL in Koin
    val configModule = module {
        single(named("baseUrl")) { baseUrl }
        single(named("isDebug")) { isDebug }
    }
    
    val koinApp = startKoin {
        modules(
            configModule,
            networkModule,
            repositoryModule,
            uiViewModelModule
        )
    }
    
    // Initialize auth token if provided
    if (authToken != null) {
        val authManager: AuthManager = koinApp.koin.get()
        authManager.updateToken(authToken)
    }
}

/**
 * ViewModel dependencies (exposed for platform init to include)
 */
val uiViewModelModule = module {
    // ViewModels
    single { AdminOrdersViewModel() }
    single { OrdersViewModel() }
}
