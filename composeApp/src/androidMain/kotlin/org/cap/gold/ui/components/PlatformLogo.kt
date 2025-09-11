package org.cap.gold.ui.components

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.ContentScale
import org.jetbrains.compose.resources.painterResource
import capgold.composeapp.generated.resources.Res
import capgold.composeapp.generated.resources.logo

@Composable
actual fun PlatformLogo(modifier: Modifier) {
    Image(
        painter = painterResource(Res.drawable.logo),
        contentDescription = "App Logo",
        modifier = modifier,
        contentScale = ContentScale.Fit,
        alignment = Alignment.Center
    )
}
