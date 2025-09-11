package org.cap.gold.di

// Minimal iOS-visible initializer that avoids exposing Koin types to Swift
fun initKoinIos(baseUrl: String, enableNetworkLogs: Boolean = false) {
    initKoin(baseUrl, enableNetworkLogs)
}
