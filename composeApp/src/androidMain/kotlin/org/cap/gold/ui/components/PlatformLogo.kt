package org.cap.gold.ui.components

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import org.cap.gold.R

@Composable
actual fun PlatformLogo(modifier: Modifier) {
    Image(
        painter = painterResource(id = R.drawable.logo),
        contentDescription = "App Logo",
        modifier = modifier,
        contentScale = ContentScale.Fit,
        alignment = Alignment.Center
    )
}
