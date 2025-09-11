package org.cap.gold.ui.components

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.painterResource
import capgold.composeapp.generated.resources.Res
import capgold.composeapp.generated.resources.branding

@Composable
actual fun BrandingImage(modifier: Modifier) {
    Image(
        painter = painterResource(Res.drawable.branding),
        contentDescription = "Branding Logo",
        modifier = modifier
    )
}
