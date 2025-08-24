package org.cap.gold

/**
 * Platform interface for platform-specific functionality.
 */
interface Platform {
    val name: String
    val isDebug: Boolean
}

/**
 * Platform object that will be initialized by platform-specific code.
 */
object PlatformInfo {
    lateinit var platform: Platform
        internal set
    
    val isDebug: Boolean
        get() = platform.isDebug
}

/**
 * For backwards compatibility.
 * @return The platform-specific Platform implementation
 */
fun getPlatform(): Platform = PlatformInfo.platform

/**
 * Initializes PlatformInfo with platform-specific implementation.
 * Implemented per platform.
 */
expect fun setupPlatform()
