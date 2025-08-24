package org.cap.gold.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import org.cap.gold.ui.components.PlatformLogo
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
private fun LogoImage() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // App Icon
        PlatformLogo(modifier = Modifier.size(120.dp))
        
        Spacer(modifier = Modifier.height(16.dp))

    }
}

@Composable
fun SplashScreen(
    onTimeout: () -> Unit,
    modifier: Modifier = Modifier,
    timeoutMillis: Long = 2000L
) {
    val scale = remember { Animatable(1f) }
    
    LaunchedEffect(key1 = true) {
        // Scale up animation
        scale.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = 800,
                delayMillis = 200
            )
        )
        
        // Wait for the specified timeout
        delay(timeoutMillis)
        
        // Notify parent that animation is complete
        onTimeout()
    }
    
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.primary
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            // App Logo with Animation
            Box(
                modifier = Modifier.scale(scale.value)
            ) {
                LogoImage()
            }
        }
    }
}

@Composable
@Preview
fun SplashScreenPreview() {
    MaterialTheme {
        SplashScreen(
            onTimeout = {},
            modifier = Modifier.fillMaxSize()
        )
    }
}
