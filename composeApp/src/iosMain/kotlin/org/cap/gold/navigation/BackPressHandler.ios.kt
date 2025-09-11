package org.cap.gold.navigation

import androidx.compose.runtime.Composable

@Composable
actual fun BackPressHandler(enabled: Boolean, onBack: () -> Unit) {
    // iOS has no system back button; no-op.
}
