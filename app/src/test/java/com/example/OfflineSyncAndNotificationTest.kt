package com.example

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.JsonDataManager
import com.example.data.model.Category
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.data.repository.FinanceRepository
import com.example.receiver.DailyReminderReceiver
import com.example.receiver.ReminderScheduler
import com.example.ui.utils.NetworkMonitor
import com.example.ui.viewmodel.FinanceViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlarmManager
import org.robolectric.shadows.ShadowNotificationManager

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class OfflineSyncAndNotificationTest {

    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var repository: FinanceRepository
    private lateinit var viewModel: FinanceViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        context = ApplicationProvider.getApplicationContext()
        
        // Clear shared preferences to ensure a clean slate for each test method
        context.getSharedPreferences("settings_prefs", Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE).edit().clear().commit()

        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val jsonDataManager = JsonDataManager(context, com.example.fakes.PlainFileStorage())
        repository = FinanceRepository(db.financeDao(), jsonDataManager)
        viewModel = FinanceViewModel(repository, injectedPrefs = com.example.fakes.FakeSharedPreferences())
    }

    @After
    fun tearDown() {
        db.close()
        Dispatchers.resetMain()
    }

    @Test
    fun testNetworkMonitor_initialState() {
        val networkMonitor = NetworkMonitor(context)
        // By default on Robolectric, connectivity is usually available or we can read its state
        val isOnline = networkMonitor.isCurrentlyOnline()
        assertEquals(isOnline, networkMonitor.isOnline.value)
    }

    @Test
    fun testOfflinePendingSyncOnAddTransaction() = kotlinx.coroutines.runBlocking {
        // Given starting state is false
        assertFalse(viewModel.pendingSync.value)
        
        // When a transaction is added
        viewModel.addTransaction(
            amount = 150.0,
            source = "Walmart",
            date = System.currentTimeMillis(),
            categoryId = 1,
            type = TransactionType.EXPENSE,
            notes = "Test Note"
        )

        // Then pendingSync becomes true (wait for the Flow to emit true)
        val hasPendingSync = kotlinx.coroutines.withTimeout(3000) {
            viewModel.pendingSync.first { it }
        }
        assertTrue(hasPendingSync)
    }

    @Test
    fun testOfflinePendingSyncOnAddCategory() = kotlinx.coroutines.runBlocking {
        // Given starting state is false
        assertFalse(viewModel.pendingSync.value)

        // When a category is added
        viewModel.addCategory(
            name = "Rent",
            type = TransactionType.EXPENSE,
            iconName = "home"
        )

        // Then pendingSync becomes true (wait for the Flow to emit true)
        val hasPendingSync = kotlinx.coroutines.withTimeout(3000) {
            viewModel.pendingSync.first { it }
        }
        assertTrue(hasPendingSync)
    }

    @Test
    fun testDailyReminderReceiver_triggersNotification() {
        val receiver = DailyReminderReceiver()
        val intent = Intent(context, DailyReminderReceiver::class.java)
        
        receiver.onReceive(context, intent)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val shadowNotificationManager: ShadowNotificationManager = shadowOf(notificationManager)
        
        // Assert a notification was posted
        assertEquals(1, shadowNotificationManager.size())
        val notification = shadowNotificationManager.allNotifications[0]
        assertNotNull(notification)
        
        // Check notification channels (for Oreo+)
        val channel = notificationManager.getNotificationChannel("daily_expense_reminder")
        assertNotNull(channel)
        assertEquals("Daily Expense Reminder", channel.name)
    }

    @Test
    fun testReminderScheduler_schedulesAlarm() {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val shadowAlarmManager: ShadowAlarmManager = shadowOf(alarmManager)

        // 1. Initially no alarms scheduled
        var nextAlarm = shadowAlarmManager.nextScheduledAlarm
        assertTrue(nextAlarm == null)

        // 2. Schedule alarm
        ReminderScheduler.scheduleDailyReminder(context)

        // 3. Alarm should now be scheduled
        nextAlarm = shadowAlarmManager.nextScheduledAlarm
        assertNotNull(nextAlarm)
        assertEquals(AlarmManager.RTC_WAKEUP, nextAlarm?.type)
        assertEquals(AlarmManager.INTERVAL_DAY, nextAlarm?.interval)

        // 4. Cancel alarm
        ReminderScheduler.cancelDailyReminder(context)
        
        // 5. Alarm should be canceled
        nextAlarm = shadowAlarmManager.nextScheduledAlarm
        assertTrue(nextAlarm == null)
    }
}
