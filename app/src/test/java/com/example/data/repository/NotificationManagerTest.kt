package com.example.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.NotificationChannelType
import com.example.data.model.NotificationType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Test-driven unit tests for the core NotificationManager logic.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class NotificationManagerTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private lateinit var context: Context
    private lateinit var notificationManager: NotificationManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        notificationManager = NotificationManagerImpl(context, testScope)
    }

    @Test
    fun testPostInApp_addsNotificationToFlow() = testScope.runTest {
        notificationManager.postInApp("Test message", NotificationType.SUCCESS, 3000L)
        val notifications = notificationManager.activeInAppNotifications.value
        assertEquals(1, notifications.size)
        assertEquals("Test message", notifications[0].message)
        assertEquals(NotificationType.SUCCESS, notifications[0].type)
    }

    @Test
    fun testDismissInApp_removesNotification() = testScope.runTest {
        notificationManager.postInApp("Test message", NotificationType.SUCCESS, 3000L)
        val originalList = notificationManager.activeInAppNotifications.value
        assertEquals(1, originalList.size)
        val notificationId = originalList[0].id

        notificationManager.dismissInApp(notificationId)
        val updatedList = notificationManager.activeInAppNotifications.value
        assertTrue(updatedList.isEmpty())
    }

    @Test
    fun testAutoDismiss_afterDuration_removesNotification() = testScope.runTest {
        notificationManager.postInApp("Auto dismiss", NotificationType.INFO, 2000L)
        assertEquals(1, notificationManager.activeInAppNotifications.value.size)

        testDispatcher.scheduler.advanceTimeBy(2100L)
        assertTrue(notificationManager.activeInAppNotifications.value.isEmpty())
    }

    @Test
    fun testPostSystemNotification_doesNotCrash() {
        notificationManager.postSystemNotification(
            title = "Test System Title",
            content = "Test System Content",
            channel = NotificationChannelType.ALERTS
        )
    }
}
