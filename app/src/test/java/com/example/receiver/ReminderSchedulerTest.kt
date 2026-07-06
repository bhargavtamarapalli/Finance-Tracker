package com.example.receiver

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import io.mockk.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Pure JVM tests for [ReminderScheduler].
 *
 * Because AlarmManager and PendingIntent are final Android system classes they
 * need to be mocked via MockK with [mockkStatic] / [mockkConstructor] /
 * [mockkObject]. The scheduler itself is a pure Kotlin object, so no Android
 * runtime is needed.
 */
class ReminderSchedulerTest {

    private lateinit var mockContext: Context
    private lateinit var mockAlarmManager: AlarmManager
    private lateinit var mockPendingIntent: PendingIntent

    @Before
    fun setUp() {
        mockContext = mockk(relaxed = true)
        mockAlarmManager = mockk(relaxed = true)
        mockPendingIntent = mockk(relaxed = true)

        // Context.getSystemService(String) → return our AlarmManager mock
        every { mockContext.getSystemService(Context.ALARM_SERVICE) } returns mockAlarmManager

        // Static PendingIntent factory methods → return our mock PendingIntent
        mockkStatic(PendingIntent::class)
        every {
            PendingIntent.getBroadcast(any(), any(), any<Intent>(), any())
        } returns mockPendingIntent
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun scheduleDailyReminder_obtainsAlarmManagerFromContext() {
        ReminderScheduler.scheduleDailyReminder(mockContext)
        verify { mockContext.getSystemService(Context.ALARM_SERVICE) }
    }

    @Test
    fun scheduleDailyReminder_callsSetInexactRepeatingWithRtcWakeup() {
        ReminderScheduler.scheduleDailyReminder(mockContext)

        verify {
            mockAlarmManager.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                any(),
                AlarmManager.INTERVAL_DAY,
                mockPendingIntent
            )
        }
    }

    @Test
    fun scheduleDailyReminder_createsCorrectPendingIntentForDailyReceiver() {
        ReminderScheduler.scheduleDailyReminder(mockContext)

        // PendingIntent.getBroadcast must have been called with the right code
        verify {
            PendingIntent.getBroadcast(
                mockContext,
                1002,  // ALARM_REQUEST_CODE
                any<Intent>(),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }

    @Test
    fun scheduleDailyReminder_doesNotCrashWhenAlarmManagerThrows() {
        every { mockAlarmManager.setInexactRepeating(any(), any(), any(), any()) } throws SecurityException("No permission")
        // Should swallow the exception internally
        ReminderScheduler.scheduleDailyReminder(mockContext)
        // No exception propagated to the caller
    }

    @Test
    fun cancelDailyReminder_withExistingPendingIntent_cancelsAndReleasesIt() {
        // getBroadcast with FLAG_NO_CREATE still returns a non-null PendingIntent
        every {
            PendingIntent.getBroadcast(
                mockContext, 1002, any<Intent>(),
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
        } returns mockPendingIntent

        ReminderScheduler.cancelDailyReminder(mockContext)

        verify { mockAlarmManager.cancel(mockPendingIntent) }
        verify { mockPendingIntent.cancel() }
    }

    @Test
    fun cancelDailyReminder_whenPendingIntentIsNull_doesNotCrash() {
        // Simulate FLAG_NO_CREATE returning null (alarm was never set)
        every {
            PendingIntent.getBroadcast(
                mockContext, 1002, any<Intent>(),
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
        } returns null

        ReminderScheduler.cancelDailyReminder(mockContext)

        verify(exactly = 0) { mockAlarmManager.cancel(any<PendingIntent>()) }
    }
}
