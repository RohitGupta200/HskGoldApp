package org.cap.gold.ui.components

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.painterResource

@Composable
actual fun PlatformLogo(modifier: Modifier) {
    Image(
        painter = painterResource("drawable/logo.png"),
        contentDescription = "App Logo",
        modifier = modifier
    )
}
