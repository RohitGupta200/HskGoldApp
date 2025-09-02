package org.cap.gold.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
actual fun AboutUsImage(modifier: Modifier) {
    // Placeholder on iOS. Replace with real asset if available.
    Box(modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant)) {
        Text(
            text = "About image",
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.align(Alignment.Center)
        )
    }
}
