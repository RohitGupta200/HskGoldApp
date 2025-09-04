package org.cap.gold.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import platform.UIKit.UIImage
import platform.UIKit.UIImageView

@Composable
@OptIn(ExperimentalForeignApi::class)
actual fun PlatformLogo(modifier: Modifier) {
    // Load image named "logo" from iOS Assets.xcassets (main bundle)
    UIKitView(
        factory = {
            val imageView = UIImageView()
            imageView.image = UIImage.imageNamed("logo")
            imageView
        },
        modifier = modifier
    )
}
