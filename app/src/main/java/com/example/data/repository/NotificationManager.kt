package com.example.data.repository

import android.app.NotificationChannel
import android.app.NotificationManager as AndroidNotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.model.InAppNotification
import com.example.data.model.NotificationChannelType
import com.example.data.model.NotificationType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Service managing in-app bottom popups (Toasts/Snackbars) and system-level push notifications.
 */
interface NotificationManager {
    val activeInAppNotifications: StateFlow<List<InAppNotification>>
    fun postInApp(message: String, type: NotificationType = NotificationType.SUCCESS, durationMs: Long = 3000L)
    fun dismissInApp(id: String)
    fun postSystemNotification(title: String, content: String, channel: NotificationChannelType)
}

class NotificationManagerImpl(
    private val context: Context,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main)
) : NotificationManager {

    private val _activeInAppNotifications = MutableStateFlow<List<InAppNotification>>(emptyList())
    override val activeInAppNotifications: StateFlow<List<InAppNotification>> = _activeInAppNotifications.asStateFlow()

    private val androidNotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as AndroidNotificationManager

    override fun postInApp(message: String, type: NotificationType, durationMs: Long) {
        val notification = InAppNotification(
            message = message,
            type = type,
            durationMs = durationMs
        )
        _activeInAppNotifications.value = _activeInAppNotifications.value + notification

        scope.launch {
            delay(durationMs)
            dismissInApp(notification.id)
        }
    }

    override fun dismissInApp(id: String) {
        _activeInAppNotifications.value = _activeInAppNotifications.value.filter { it.id != id }
    }

    override fun postSystemNotification(title: String, content: String, channel: NotificationChannelType) {
        val channelId = channel.name.lowercase()
        val channelName = when (channel) {
            NotificationChannelType.REMINDERS -> "Daily Reminders"
            NotificationChannelType.ALERTS -> "Budget Alerts"
            NotificationChannelType.ANNOUNCEMENTS -> "System Announcements"
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = AndroidNotificationManager.IMPORTANCE_DEFAULT
            val mChannel = NotificationChannel(channelId, channelName, importance)
            androidNotificationManager.createNotificationChannel(mChannel)
        }

        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            channel.hashCode(),
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val appLogoDrawable = context.resources.getIdentifier("ic_app_logo", "drawable", context.packageName)
        val icon = if (appLogoDrawable != 0) appLogoDrawable else android.R.drawable.ic_popup_reminder

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(icon)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        androidNotificationManager.notify(channel.ordinal + 100, notification)
    }
}

object NoOpNotificationManager : NotificationManager {
    override val activeInAppNotifications: kotlinx.coroutines.flow.StateFlow<List<InAppNotification>> = 
        kotlinx.coroutines.flow.MutableStateFlow<List<InAppNotification>>(emptyList()).asStateFlow()
    override fun postInApp(message: String, type: NotificationType, durationMs: Long) {}
    override fun dismissInApp(id: String) {}
    override fun postSystemNotification(title: String, content: String, channel: NotificationChannelType) {}
}
