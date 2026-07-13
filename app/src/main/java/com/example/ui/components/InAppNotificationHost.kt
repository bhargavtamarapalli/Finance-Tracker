package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.InAppNotification
import com.example.data.model.NotificationType

/**
 * Renders a stacked list of animated in-app notifications at the bottom of the screen,
 * positioned above bottom bars for clean layout compatibility.
 */
@Composable
fun InAppNotificationHost(
    notifications: List<InAppNotification>,
    onDismiss: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(bottom = 80.dp, start = 16.dp, end = 16.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            notifications.takeLast(3).forEach { notification ->
                key(notification.id) {
                    InAppNotificationItem(
                        notification = notification,
                        onDismiss = { onDismiss(notification.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun InAppNotificationItem(
    notification: InAppNotification,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when (notification.type) {
        NotificationType.SUCCESS -> Color(0xFFE8F5E9)
        NotificationType.ERROR -> Color(0xFFFFEBEE)
        NotificationType.WARNING -> Color(0xFFFFF3E0)
        NotificationType.INFO -> Color(0xFFE3F2FD)
    }

    val contentColor = when (notification.type) {
        NotificationType.SUCCESS -> Color(0xFF2E7D32)
        NotificationType.ERROR -> Color(0xFFC62828)
        NotificationType.WARNING -> Color(0xFFEF6C00)
        NotificationType.INFO -> Color(0xFF1565C0)
    }

    val icon = when (notification.type) {
        NotificationType.SUCCESS -> Icons.Default.CheckCircle
        NotificationType.ERROR -> Icons.Default.Error
        NotificationType.WARNING -> Icons.Default.Warning
        NotificationType.INFO -> Icons.Default.Info
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("in_app_notification_card_${notification.id}"),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor,
            contentColor = contentColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = notification.type.name,
                tint = contentColor,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = notification.message,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .size(24.dp)
                    .testTag("dismiss_notification_${notification.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Dismiss",
                    tint = contentColor.copy(alpha = 0.7f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
