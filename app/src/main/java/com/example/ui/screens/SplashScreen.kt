package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(
    onAnimationComplete: () -> Unit
) {
    // Animatable states
    val logoScale = remember { Animatable(0.2f) }
    val logoAlpha = remember { Animatable(0f) }
    val textAlpha = remember { Animatable(0f) }
    val textTranslationY = remember { Animatable(30f) }
    val taglineAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // Animate Logo pop & scale with bouncy spring effect
        launch {
            logoScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        }
        launch {
            logoAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 1000, easing = LinearOutSlowInEasing)
            )
        }

        // Animate Title Text fade-in & slide-up
        launch {
            delay(500)
            textAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 800, easing = LinearOutSlowInEasing)
            )
        }
        launch {
            delay(500)
            textTranslationY.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
        }

        // Animate Tagline fade-in
        launch {
            delay(900)
            taglineAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 600, easing = LinearOutSlowInEasing)
            )
        }

        // Keep screen visible for splash duration
        delay(2600)
        onAnimationComplete()
    }

    // High-contrast, premium dark background gradient to showcase the neon/gradient logo beautifully
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF141218), // Rich Dark Slate
                        Color(0xFF1C1B22),
                        Color(0xFF2E1A47)  // Subtle Cosmic Indigo glow
                    )
                )
            )
            .testTag("splash_screen"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            // Animated App Logo
            Image(
                painter = painterResource(id = R.drawable.ic_app_logo),
                contentDescription = "App Logo",
                modifier = Modifier
                    .size(140.dp)
                    .scale(logoScale.value)
                    .alpha(logoAlpha.value)
                    .testTag("splash_logo")
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Animated App Title
            Text(
                text = "Finance Tracker",
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.SansSerif,
                color = Color.White,
                letterSpacing = 1.5.sp,
                modifier = Modifier
                    .alpha(textAlpha.value)
                    .offset(y = textTranslationY.value.dp)
                    .testTag("splash_title")
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Animated Premium Tagline
            Text(
                text = "Smart wealth management at your fingertips",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.SansSerif,
                color = Color(0xFFD0BCFF),
                textAlign = TextAlign.Center,
                letterSpacing = 0.5.sp,
                modifier = Modifier
                    .alpha(taglineAlpha.value)
                    .padding(horizontal = 16.dp)
                    .testTag("splash_tagline")
            )
        }
    }
}
