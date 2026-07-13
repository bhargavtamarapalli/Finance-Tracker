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
    primary = BrandPrimary,
    secondary = BrandSecondary,
    tertiary = BrandTertiary,
    background = DeepBlack,
    surface = DarkGrey,
    onPrimary = Color.Black,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    surfaceVariant = DarkGrey,
    onSurfaceVariant = TextSecondary,
    outline = BorderColor
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF1976D2),
    secondary = Color(0xFF0288D1),
    tertiary = Color(0xFF0097A7),
    background = Color(0xFFFDFBFF),
    surface = Color(0xFFF5F5F5),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1A1C1E),
    onSurface = Color(0xFF1A1C1E),
    surfaceVariant = Color(0xFFDFE2EB),
    onSurfaceVariant = Color(0xFF455A64),
    outline = Color(0xFF74777F)
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
    val sizeIconNormal = 24.dp
    val sizeIconMedium = 40.dp
    val sizeIconContainer = 48.dp
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
