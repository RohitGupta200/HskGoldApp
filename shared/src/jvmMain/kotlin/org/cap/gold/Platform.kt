@file:JvmName("PlatformJvm")

package org.cap.gold

private class JvmPlatform : Platform {
    override val name: String = "JVM"
    // Use a JVM system property to toggle debug if desired: -Dorg.cap.gold.debug=true
    override val isDebug: Boolean = java.lang.Boolean.getBoolean("org.cap.gold.debug")
}

actual fun setupPlatform() {
    PlatformInfo.platform = JvmPlatform()
}
