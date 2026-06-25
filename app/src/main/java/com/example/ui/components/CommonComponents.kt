package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.AppDimens
import com.example.ui.theme.AppShapes

/**
 * Primary central button for the application, following Material 3 guidelines and app theme dimensions.
 */
@Composable
fun FinanceButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    icon: Any? = null, // Can be ImageVector or Painter
    iconTint: Color = Color.Unspecified,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary,
    shape: Shape = AppShapes.roundedButton,
    height: Dp = AppDimens.heightButton,
    testTag: String? = null
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .height(height)
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier),
        enabled = enabled && !loading,
        shape = shape,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = containerColor.copy(alpha = 0.5f),
            disabledContentColor = contentColor.copy(alpha = 0.5f)
        ),
        contentPadding = PaddingValues(horizontal = AppDimens.paddingNormal, vertical = AppDimens.paddingSmall)
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = contentColor,
                strokeWidth = 2.5.dp
            )
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (icon != null) {
                    when (icon) {
                        is ImageVector -> {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = if (iconTint == Color.Unspecified) contentColor else iconTint,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        is Painter -> {
                            Icon(
                                painter = icon,
                                contentDescription = null,
                                tint = if (iconTint == Color.Unspecified) contentColor else iconTint,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(AppDimens.paddingSmall))
                }
                Text(
                    text = text,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * Secondary central outlined button for the application.
 */
@Composable
fun FinanceOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: Any? = null,
    iconTint: Color = Color.Unspecified,
    contentColor: Color = MaterialTheme.colorScheme.primary,
    borderColor: Color = MaterialTheme.colorScheme.outline,
    shape: Shape = AppShapes.roundedButton,
    height: Dp = AppDimens.heightButton,
    testTag: String? = null
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .height(height)
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier),
        enabled = enabled,
        shape = shape,
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = contentColor,
            disabledContentColor = contentColor.copy(alpha = 0.5f)
        ),
        border = BorderStroke(AppDimens.borderWidthThin, if (enabled) borderColor else borderColor.copy(alpha = 0.5f)),
        contentPadding = PaddingValues(horizontal = AppDimens.paddingNormal, vertical = AppDimens.paddingSmall)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                when (icon) {
                    is ImageVector -> {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (iconTint == Color.Unspecified) contentColor else iconTint,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    is Painter -> {
                        Icon(
                            painter = icon,
                            contentDescription = null,
                            tint = if (iconTint == Color.Unspecified) contentColor else iconTint,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(AppDimens.paddingSmall))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Reusable simple text button for less prominent actions.
 */
@Composable
fun FinanceTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentColor: Color = MaterialTheme.colorScheme.primary,
    fontWeight: FontWeight = FontWeight.Bold,
    icon: Any? = null,
    iconTint: Color = Color.Unspecified,
    testTag: String? = null
) {
    TextButton(
        onClick = onClick,
        modifier = modifier.then(if (testTag != null) Modifier.testTag(testTag) else Modifier),
        enabled = enabled,
        colors = ButtonDefaults.textButtonColors(
            contentColor = contentColor,
            disabledContentColor = contentColor.copy(alpha = 0.5f)
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                when (icon) {
                    is ImageVector -> {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (iconTint == Color.Unspecified) contentColor else iconTint,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    is androidx.compose.ui.graphics.painter.Painter -> {
                        Icon(
                            painter = icon,
                            contentDescription = null,
                            tint = if (iconTint == Color.Unspecified) contentColor else iconTint,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(AppDimens.paddingSmall))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = fontWeight
            )
        }
    }
}

/**
 * Unified Icon Button component that ensures a minimum touch target size of 48dp x 48dp.
 */
@Composable
fun FinanceIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    backgroundColor: Color = Color.Transparent,
    size: Dp = 24.dp,
    testTag: String? = null
) {
    IconButton(
        onClick = onClick,
        modifier = modifier
            .size(48.dp) // Minimum touch target size
            .clip(CircleShape)
            .background(backgroundColor)
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier),
        enabled = enabled
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (enabled) tint else tint.copy(alpha = 0.4f),
            modifier = Modifier.size(size)
        )
    }
}

/**
 * Consistent segment button for switching states (e.g., Income vs Expense).
 */
@Composable
fun FinanceSegmentedButton(
    selected: Boolean,
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
    selectedContainerColor: Color = MaterialTheme.colorScheme.primary,
    selectedContentColor: Color = MaterialTheme.colorScheme.onPrimary,
    unselectedContainerColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    unselectedContentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    shape: Shape = CircleShape,
    testTag: String? = null
) {
    val containerColor = if (selected) selectedContainerColor else unselectedContainerColor
    val contentColor = if (selected) selectedContentColor else unselectedContentColor

    Box(
        modifier = modifier
            .height(48.dp)
            .clip(shape)
            .background(containerColor)
            .clickable(
                onClick = onClick,
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = contentColor)
            )
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = contentColor,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Unified Finance Card with centralized design values.
 */
@Composable
fun FinanceCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    shape: Shape = AppShapes.roundedCardMedium,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    elevation: Dp = 1.dp,
    border: BorderStroke? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = modifier,
            shape = shape,
            colors = CardDefaults.cardColors(containerColor = containerColor),
            elevation = CardDefaults.cardElevation(defaultElevation = elevation),
            border = border,
            content = content
        )
    } else {
        Card(
            modifier = modifier,
            shape = shape,
            colors = CardDefaults.cardColors(containerColor = containerColor),
            elevation = CardDefaults.cardElevation(defaultElevation = elevation),
            border = border,
            content = content
        )
    }
}
