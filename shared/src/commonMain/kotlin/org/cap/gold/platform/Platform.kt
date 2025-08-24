package org.cap.gold.platform

/**
 * Common interface for platform-specific functionality.
 */
interface Platform {
    val name: String
    val isDebug: Boolean
}
