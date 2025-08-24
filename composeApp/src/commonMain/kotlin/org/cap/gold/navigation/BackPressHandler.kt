package org.cap.gold.navigation

import androidx.compose.runtime.Composable

@Composable
expect fun BackPressHandler(enabled: Boolean = true, onBack: () -> Unit)
