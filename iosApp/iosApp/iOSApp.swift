import SwiftUI
import Foundation
import ComposeApp

@main
struct iOSApp: App {
    init() {
        // Initialize platform-specific code from Kotlin
        PlatformKt.setupPlatform()

        // Configure shared URLCache for Ktor Darwin engine (NSURLSession)
        // 20 MB memory, 100 MB disk
        URLCache.shared = URLCache(
            memoryCapacity: 20 * 1024 * 1024,
            diskCapacity: 100 * 1024 * 1024,
            directory: nil
        )
        
        // Initialize Koin with the iOS app context and server URL
        print("[CapGold] About to init Koin via ComposeApp (iOS)")
        BootstrapKt.doInitIos(
            baseUrl: "https://capgold-server.onrender.com",
            enableNetworkLogs: false
        )
        print("[CapGold] Koin init completed (iOS)")
    }
    
    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}

// Koin is initialized in init(); no explicit reference needed on iOS
