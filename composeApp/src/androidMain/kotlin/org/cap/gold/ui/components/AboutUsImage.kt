package org.cap.gold.ui.components

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import org.cap.gold.R

@Composable
actual fun AboutUsImage(modifier: Modifier) {
    Image(
        painter = painterResource(id = R.drawable.hsk_parents),
        contentDescription = "About Us image",
        modifier = modifier,
        contentScale = ContentScale.Crop
    )
}
