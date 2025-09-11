package org.cap.gold.ios

import org.cap.gold.di.initKoin
import org.cap.gold.di.networkModule
import org.cap.gold.di.apiServicesModule
import org.cap.gold.di.repositoryModule
import org.cap.gold.di.uiViewModelModule

// Exposed to Swift via ComposeApp framework
fun doInitIos(baseUrl: String, enableNetworkLogs: Boolean = false) {
    // Initialize shared Koin and include ComposeApp modules so UI screens have their deps
    initKoin(baseUrl = baseUrl, enableNetworkLogs = enableNetworkLogs) {
        modules(
            networkModule,
            apiServicesModule,
            repositoryModule,
            uiViewModelModule,
        )
    }
}
