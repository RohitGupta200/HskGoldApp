package org.cap.gold.ui.components

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.painterResource

@Composable
actual fun BrandingImage(modifier: Modifier) {
    Image(
        painter = painterResource("drawable/branding.png"),
        contentDescription = "Branding Logo",
        modifier = modifier
    )
}
