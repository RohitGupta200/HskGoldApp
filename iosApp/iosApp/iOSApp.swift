import SwiftUI
import Foundation
import shared

@main
struct iOSApp: App {
    init() {
        // Initialize platform-specific code
        setupPlatform()

        // Configure shared URLCache for Ktor Darwin engine (NSURLSession)
        // 20 MB memory, 100 MB disk
        URLCache.shared = URLCache(
            memoryCapacity: 20 * 1024 * 1024,
            diskCapacity: 100 * 1024 * 1024,
            directory: nil
        )
        
        // Initialize Koin with the iOS app context and server URL
        let koinApplication = KoinKt.doInitKoin(
            baseUrl: "http://localhost:8080", // For iOS simulator, localhost points to the host machine
            appDeclaration: { _ in }
        )
        
        // Store the Koin application reference
        _koin = koinApplication
    }
    
    // Store the Koin application reference
    @State private var koin: KoinApplication?
    
    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}

// Global reference to keep Koin alive
private var _koin: KoinApplication?