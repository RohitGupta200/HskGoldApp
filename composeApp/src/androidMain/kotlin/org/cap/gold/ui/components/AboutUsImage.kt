package org.cap.gold.ui.components

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import org.jetbrains.compose.resources.painterResource
import capgold.composeapp.generated.resources.Res
import capgold.composeapp.generated.resources.hsk_parents

@Composable
actual fun AboutUsImage(modifier: Modifier) {
    Image(
        painter = painterResource(Res.drawable.hsk_parents),
        contentDescription = "About Us image",
        modifier = modifier,
        contentScale = ContentScale.Crop
    )
}
