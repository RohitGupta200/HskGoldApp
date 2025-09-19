package org.cap.gold.ios

import org.cap.gold.di.initKoin
import org.cap.gold.di.networkModule
import org.cap.gold.di.apiServicesModule
import org.cap.gold.di.repositoryModule
import org.cap.gold.di.uiViewModelModule
import org.cap.gold.auth.PushTokenRegistry

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

// Exposed to Swift: set the current iOS push token so shared can send it on sign-in
fun setIosPushToken(token: String?) {
    PushTokenRegistry.set(token)
}
