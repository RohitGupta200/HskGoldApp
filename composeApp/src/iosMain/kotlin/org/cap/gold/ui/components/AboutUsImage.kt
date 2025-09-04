package org.cap.gold.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import platform.UIKit.UIImage
import platform.UIKit.UIImageView

@Composable
@OptIn(ExperimentalForeignApi::class)
actual fun AboutUsImage(modifier: Modifier) {
    // Load image named "hsk_parents" from iOS Assets.xcassets (main bundle)
    UIKitView(
        factory = {
            val imageView = UIImageView()
            imageView.clipsToBounds = true
            imageView.image = UIImage.imageNamed("hsk_parents")
            imageView
        },
        modifier = modifier
    )
}
