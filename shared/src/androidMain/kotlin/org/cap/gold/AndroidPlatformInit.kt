package org.cap.gold

import org.cap.gold.BuildConfig

private class AndroidPlatform : Platform {
    override val name: String = "Android"
    override val isDebug: Boolean = BuildConfig.DEBUG
}

actual fun setupPlatform() {
    PlatformInfo.platform = AndroidPlatform()
}
