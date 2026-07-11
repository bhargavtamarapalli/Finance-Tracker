package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * A custom animated startup splash screen that bypasses Android 12+ circular clipping
 * to render the app logo in its intended premium square format.
 *
 * @param onAnimationComplete Callback triggered when the splash animation completes.
 */
@Composable
fun CustomSplashScreen(
    onAnimationComplete: () -> Unit
) {
    val alpha = remember { Animatable(0f) }
    val scale = remember { Animatable(0.85f) }

    LaunchedEffect(key1 = true) {
        // Run fade-in and scale-up animation in parallel
        launch {
            alpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 1000)
            )
        }
        launch {
            scale.animateTo(
                targetValue = 1.0f,
                animationSpec = tween(durationMillis = 1000)
            )
        }
        // Brief pause at the end of animation before transitioning
        delay(1400)
        onAnimationComplete()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0E14)), // Dark background matching starting theme
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_app_logo),
            contentDescription = "Finance Manager Logo",
            modifier = Modifier
                .size(160.dp) // Maintain square aspect ratio
                .scale(scale.value)
                .alpha(alpha.value)
        )
    }
}
