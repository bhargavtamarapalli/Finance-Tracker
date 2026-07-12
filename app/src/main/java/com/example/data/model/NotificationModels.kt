package com.example.data.model

import java.util.UUID

/**
 * Represents the visual style and category of an in-app toast/snackbar alert.
 */
enum class NotificationType {
    SUCCESS,
    ERROR,
    INFO,
    WARNING
}

/**
 * Represents Android system notification channels for push notifications.
 */
enum class NotificationChannelType {
    REMINDERS,
    ALERTS,
    ANNOUNCEMENTS
}

/**
 * Model representation of an in-app popup message.
 */
data class InAppNotification(
    val id: String = UUID.randomUUID().toString(),
    val message: String,
    val type: NotificationType = NotificationType.SUCCESS,
    val durationMs: Long = 3000L,
    val actionLabel: String? = null,
    val onAction: (() -> Unit)? = null
)
