package com.example.ui.components

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.data.model.InAppNotification
import com.example.data.model.NotificationType
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Robolectric UI tests validating custom in-app notification popup components.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class InAppNotificationHostTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testInAppNotificationHost_rendersMessages() {
        val list = listOf(
            InAppNotification(id = "1", message = "Successfully added", type = NotificationType.SUCCESS),
            InAppNotification(id = "2", message = "Connection lost", type = NotificationType.WARNING)
        )

        composeTestRule.setContent {
            InAppNotificationHost(
                notifications = list,
                onDismiss = {}
            )
        }

        composeTestRule.onNodeWithText("Successfully added").assertIsDisplayed()
        composeTestRule.onNodeWithText("Connection lost").assertIsDisplayed()
    }

    @Test
    fun testInAppNotificationHost_dismissTriggeredOnClick() {
        val list = listOf(
            InAppNotification(id = "1", message = "Dismiss me", type = NotificationType.INFO)
        )
        var dismissedId: String? = null

        composeTestRule.setContent {
            InAppNotificationHost(
                notifications = list,
                onDismiss = { dismissedId = it }
            )
        }

        composeTestRule
            .onNodeWithTag("dismiss_notification_1")
            .performClick()

        assertEquals("1", dismissedId)
    }
}
