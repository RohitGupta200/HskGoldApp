package org.cap.gold

import android.app.Application
import org.cap.gold.di.initKoin
import org.cap.gold.setupPlatform
import org.koin.android.ext.koin.androidContext
import org.cap.gold.di.repositoryModule
import org.cap.gold.di.networkModule
import org.cap.gold.di.apiServicesModule
import org.cap.gold.di.uiViewModelModule

class CapGoldApp : Application() {
    override fun onCreate() {
        super.onCreate()
        setupPlatform()
        
        // Initialize Koin with the base URL of your API (shared initKoin)
        initKoin(
            baseUrl = "http://10.0.2.2:8081",
            enableNetworkLogs = false,
            appDeclaration = {
                // Provide Android context for androidAuthModule
                androidContext(this@CapGoldApp)
                // Include app-layer modules providing ProductRepository, HttpClient, etc.
                modules(apiServicesModule, networkModule, repositoryModule, uiViewModelModule)
            }
        )
    }
}
