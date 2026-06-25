package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFD0BCFF),
    secondary = Color(0xFFCCC2DC),
    tertiary = Color(0xFFEFB8C8),
    background = Color(0xFF141218),
    surface = Color(0xFF211F26),
    onPrimary = Color(0xFF381E72),
    onSecondary = Color(0xFF332D41),
    onTertiary = Color(0xFF492532),
    onBackground = Color(0xFFE6E1E5),
    onSurface = Color(0xFFE6E1E5),
    surfaceVariant = Color(0xFF49454F),
    onSurfaceVariant = Color(0xFFCAC4D0),
    outline = Color(0xFF938F99)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF6750A4),
    secondary = Color(0xFF625B71),
    tertiary = Color(0xFF7D5260),
    background = Color(0xFFFEF7FF),
    surface = Color(0xFFF3EDF7),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1D1B20),
    onSurface = Color(0xFF1D1B20),
    surfaceVariant = Color(0xFFE7E0EC),
    onSurfaceVariant = Color(0xFF49454F),
    outline = Color(0xFFCAC4D0)
)

@Composable
fun FinanceTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

object AppDimens {
    val paddingZero = 0.dp
    val paddingExtraSmall = 4.dp
    val paddingSmall = 8.dp
    val paddingMedium = 12.dp
    val paddingNormal = 16.dp
    val paddingLarge = 24.dp
    val paddingExtraLarge = 32.dp

    val heightButton = 56.dp
    val heightSpacerLarge = 80.dp
    val sizeIconSmall = 18.dp
    val sizeIconMedium = 40.dp
    val sizeAvatar = 48.dp
    val sizeDrawerAvatar = 60.dp
    val borderWidthThin = 1.dp
    val borderWidthThick = 2.dp
    val paddingIconInside = 10.dp
}

object AppShapes {
    val roundedCardMedium = RoundedCornerShape(16.dp)
    val roundedCardLarge = RoundedCornerShape(24.dp)
    val roundedCardExtraLarge = RoundedCornerShape(32.dp)
    val roundedButton = RoundedCornerShape(16.dp)
    val roundedIconContainer = RoundedCornerShape(12.dp)
}
