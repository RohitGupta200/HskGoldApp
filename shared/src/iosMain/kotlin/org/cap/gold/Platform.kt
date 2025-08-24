package org.cap.gold

import platform.Foundation.NSBundle
import platform.UIKit.UIDevice

private class IosPlatform : Platform {
    override val name: String = UIDevice.currentDevice.systemName()
    
    // Debug detection for iOS
    override val isDebug: Boolean = NSBundle.mainBundle.bundlePath.endsWith(".app/PlugIns/")
        .not() // Not in test target
        .and(
            NSBundle.mainBundle.bundlePath.contains("DerivedData").not()
        )
}

actual fun setupPlatform() {
    PlatformInfo.platform = IosPlatform()
}
