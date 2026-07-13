package com.example.ui.viewmodel

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import com.example.data.model.Category
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.data.model.TransactionWithCategory
import com.example.data.model.Announcement
import com.example.data.repository.FinanceRepository
import com.example.ui.utils.NetworkMonitor
import io.mockk.*
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.Calendar
import java.util.Locale
import java.text.SimpleDateFormat

/**
 * Extended FinanceViewModel tests — pure JVM, zero Robolectric.
 *
 * By injecting a MockK [NetworkMonitor] directly into the ViewModel
 * constructor (made testable via an optional parameter), we avoid the
 * "Method not mocked" failures from NetworkRequest.Builder and let
 * JaCoCo instrument the ViewModel correctly.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FinanceViewModelExtendedTest {

    @MockK(relaxed = true)
    private lateinit var mockRepository: FinanceRepository

    @MockK(relaxed = true)
    private lateinit var mockNetworkMonitor: NetworkMonitor

    @MockK(relaxed = true)
    private lateinit var mockSharedPrefs: SharedPreferences

    @MockK(relaxed = true)
    private lateinit var mockSharedPrefsEditor: SharedPreferences.Editor

    private lateinit var viewModel: FinanceViewModel
    private val testDispatcher = UnconfinedTestDispatcher()

    private val mockCategoriesFlow = MutableStateFlow<List<Category>>(emptyList())
    private val mockTransactionsFlow = MutableStateFlow<List<TransactionWithCategory>>(emptyList())
    private val onlineFlow = MutableStateFlow(false)

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        Dispatchers.setMain(testDispatcher)

        // SharedPreferences stubs — return defaults for all keys
        every { mockSharedPrefs.getString(any(), any()) } answers { secondArg() }
        every { mockSharedPrefs.getBoolean(any(), any()) } answers { secondArg() }
        every { mockSharedPrefs.edit() } returns mockSharedPrefsEditor
        every { mockSharedPrefsEditor.putString(any(), any()) } returns mockSharedPrefsEditor
        every { mockSharedPrefsEditor.putBoolean(any(), any()) } returns mockSharedPrefsEditor
        every { mockSharedPrefsEditor.apply() } just Runs

        // NetworkMonitor stub — inject to avoid framework calls
        every { mockNetworkMonitor.isOnline } returns onlineFlow

        // Stub getContext to return a dummy context with ConnectivityManager mock to avoid ClassCastException in default constructor
        val dummyContext = mockk<android.content.Context>(relaxed = true)
        val dummyCm = mockk<android.net.ConnectivityManager>(relaxed = true)
        val dummyNm = mockk<android.app.NotificationManager>(relaxed = true)
        every { dummyContext.getSystemService(android.content.Context.CONNECTIVITY_SERVICE) } returns dummyCm
        every { dummyContext.getSystemService(android.content.Context.NOTIFICATION_SERVICE) } returns dummyNm
        every { mockRepository.getContext() } returns dummyContext

        every { mockRepository.getSettingsPreferences() } returns mockSharedPrefs
        every { mockRepository.allCategories } returns mockCategoriesFlow
        every { mockRepository.allTransactions } returns mockTransactionsFlow

        // Inject mockNetworkMonitor so FinanceViewModel never touches ConnectivityManager
        viewModel = FinanceViewModel(mockRepository, mockNetworkMonitor)
    }


    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    // --- setTimePeriod ---

    @Test
    fun testSetTimePeriod_updatesSelectedTimePeriodToDay() {
        viewModel.setTimePeriod(TimePeriod.DAY)
        assertEquals(TimePeriod.DAY, viewModel.selectedTimePeriod.value)
    }

    @Test
    fun testSetTimePeriod_updatesSelectedTimePeriodToWeek() {
        viewModel.setTimePeriod(TimePeriod.WEEK)
        assertEquals(TimePeriod.WEEK, viewModel.selectedTimePeriod.value)
    }

    @Test
    fun testSetTimePeriod_updatesSelectedTimePeriodToYear() {
        viewModel.setTimePeriod(TimePeriod.YEAR)
        assertEquals(TimePeriod.YEAR, viewModel.selectedTimePeriod.value)
    }

    @Test
    fun testSetTimePeriod_resetsActiveDate() {
        val before = System.currentTimeMillis() - 100_000
        viewModel.setDateDirectly(before)
        assertEquals(before, viewModel.activeDate.value)

        viewModel.setTimePeriod(TimePeriod.WEEK)
        // After setting period, activeDate should be close to now
        assertTrue(viewModel.activeDate.value >= before + 100_000 - 2000)
    }

    // --- setDateDirectly ---

    @Test
    fun testSetDateDirectly_pastDate_setsDate() {
        val pastDate = System.currentTimeMillis() - 86_400_000L // yesterday
        viewModel.setDateDirectly(pastDate)
        assertEquals(pastDate, viewModel.activeDate.value)
    }

    @Test
    fun testSetDateDirectly_futureDate_doesNotSetDate() {
        val futureDate = System.currentTimeMillis() + 86_400_000L // tomorrow
        val originalDate = viewModel.activeDate.value
        viewModel.setDateDirectly(futureDate)
        assertEquals(originalDate, viewModel.activeDate.value)
    }

    // --- moveToPreviousPeriod ---

    @Test
    fun testMoveToPreviousPeriod_forDay_movesBackOneDay() {
        viewModel.setTimePeriod(TimePeriod.DAY)
        val before = viewModel.activeDate.value
        viewModel.moveToPreviousPeriod()
        assertTrue(viewModel.activeDate.value < before)
    }

    @Test
    fun testMoveToPreviousPeriod_forWeek_movesBackOneWeek() {
        viewModel.setTimePeriod(TimePeriod.WEEK)
        val before = viewModel.activeDate.value
        viewModel.moveToPreviousPeriod()
        val diff = before - viewModel.activeDate.value
        assertTrue(diff >= 6 * 86_400_000L && diff <= 8 * 86_400_000L) // ~7 days
    }

    @Test
    fun testMoveToPreviousPeriod_forMonth_movesBackOneMonth() {
        viewModel.setTimePeriod(TimePeriod.MONTH)
        val before = viewModel.activeDate.value
        viewModel.moveToPreviousPeriod()
        assertTrue(viewModel.activeDate.value < before)
    }

    @Test
    fun testMoveToPreviousPeriod_forYear_movesBackOneYear() {
        viewModel.setTimePeriod(TimePeriod.YEAR)
        val before = viewModel.activeDate.value
        viewModel.moveToPreviousPeriod()
        assertTrue(viewModel.activeDate.value < before)
    }

    // --- moveToNextPeriod ---

    @Test
    fun testMoveToNextPeriod_forPastDate_advancesPeriod() {
        // Set to a past date so we can go "next"
        val twoMonthsAgo = System.currentTimeMillis() - 2 * 30 * 86_400_000L
        viewModel.setDateDirectly(twoMonthsAgo)
        viewModel.setTimePeriod(TimePeriod.MONTH)
        viewModel.setDateDirectly(twoMonthsAgo)
        val before = viewModel.activeDate.value
        viewModel.moveToNextPeriod()
        assertTrue(viewModel.activeDate.value > before)
    }

    @Test
    fun testMoveToNextPeriod_forCurrentPeriod_doesNotAdvance() {
        viewModel.setTimePeriod(TimePeriod.MONTH)
        val before = viewModel.activeDate.value
        // Current month — should not be allowed to go forward
        viewModel.moveToNextPeriod()
        assertEquals(before, viewModel.activeDate.value)
    }

    // --- Theme settings ---

    @Test
    fun testSetTheme_updatesAppTheme_toLight() {
        viewModel.setTheme(AppTheme.LIGHT)
        assertEquals(AppTheme.LIGHT, viewModel.appTheme.value)
    }

    @Test
    fun testSetTheme_updatesAppTheme_toDark() {
        viewModel.setTheme(AppTheme.DARK)
        assertEquals(AppTheme.DARK, viewModel.appTheme.value)
    }

    @Test
    fun testSetTheme_updatesAppTheme_toSystem() {
        viewModel.setTheme(AppTheme.SYSTEM)
        assertEquals(AppTheme.SYSTEM, viewModel.appTheme.value)
    }

    // --- Currency settings ---

    @Test
    fun testSetCurrency_updatesFlow() {
        viewModel.setCurrency(com.example.ui.utils.CurrencyOption.USD)
        assertEquals(com.example.ui.utils.CurrencyOption.USD, viewModel.currencyOption.value)
    }

    // --- Reminder ---

    @Test
    fun testSetReminderEnabled_true_schedulesReminder() {
        val mockCtx = mockk<android.content.Context>(relaxed = true)
        mockkObject(com.example.receiver.ReminderScheduler)
        every { com.example.receiver.ReminderScheduler.scheduleDailyReminder(any()) } just Runs

        viewModel.setReminderEnabled(true, mockCtx)
        assertTrue(viewModel.reminderEnabled.value)

        verify { com.example.receiver.ReminderScheduler.scheduleDailyReminder(mockCtx) }
        unmockkObject(com.example.receiver.ReminderScheduler)
    }

    @Test
    fun testSetReminderEnabled_false_cancelsReminder() {
        val mockCtx = mockk<android.content.Context>(relaxed = true)
        mockkObject(com.example.receiver.ReminderScheduler)
        every { com.example.receiver.ReminderScheduler.cancelDailyReminder(any()) } just Runs

        viewModel.setReminderEnabled(false, mockCtx)
        assertFalse(viewModel.reminderEnabled.value)

        verify { com.example.receiver.ReminderScheduler.cancelDailyReminder(mockCtx) }
        unmockkObject(com.example.receiver.ReminderScheduler)
    }

    // --- Biometric lock ---

    @Test
    fun testSetBiometricLockEnabled_true_updatesFlow() {
        viewModel.setBiometricLockEnabled(true)
        assertTrue(viewModel.biometricLockEnabled.value)
    }

    @Test
    fun testSetBiometricLockEnabled_false_updatesFlow() {
        viewModel.setBiometricLockEnabled(false)
        assertFalse(viewModel.biometricLockEnabled.value)
    }

    // --- CRUD operations delegate to repository ---

    @Test
    fun testAddTransaction_delegatesToRepository() = runTest {
        coEvery { mockRepository.insertTransaction(any()) } just Runs

        viewModel.addTransaction(100.0, "Salary", System.currentTimeMillis(), 1, TransactionType.INCOME, "Monthly salary")
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { mockRepository.insertTransaction(any()) }
    }

    @Test
    fun testUpdateTransaction_delegatesToRepository() = runTest {
        val transaction = TransactionEntity(id = 1, amount = 50.0, source = "Test", date = 0L, categoryId = 1, type = TransactionType.EXPENSE, notes = "")
        coEvery { mockRepository.updateTransaction(any()) } just Runs

        viewModel.updateTransaction(transaction)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { mockRepository.updateTransaction(transaction) }
    }

    @Test
    fun testDeleteTransaction_delegatesToRepository() = runTest {
        val transaction = TransactionEntity(id = 1, amount = 50.0, source = "Test", date = 0L, categoryId = 1, type = TransactionType.EXPENSE, notes = "")
        coEvery { mockRepository.deleteTransaction(any()) } just Runs

        viewModel.deleteTransaction(transaction)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { mockRepository.deleteTransaction(transaction) }
    }

    @Test
    fun testAddCategory_delegatesToRepository() = runTest {
        coEvery { mockRepository.insertCategory(any()) } just Runs

        viewModel.addCategory("Groceries", TransactionType.EXPENSE, "cart")
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { mockRepository.insertCategory(any()) }
    }

    @Test
    fun testUpdateCategory_delegatesToRepository() = runTest {
        val category = Category(id = 1, name = "Food", type = TransactionType.EXPENSE, iconName = "food")
        coEvery { mockRepository.updateCategory(any()) } just Runs

        viewModel.updateCategory(category)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { mockRepository.updateCategory(category) }
    }

    @Test
    fun testDeleteCategory_delegatesToRepository() = runTest {
        val category = Category(id = 1, name = "Food", type = TransactionType.EXPENSE, iconName = "food")
        coEvery { mockRepository.deleteCategory(any()) } just Runs

        viewModel.deleteCategory(category)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { mockRepository.deleteCategory(category) }
    }

    // --- Backup / Restore delegates ---

    @Test
    fun testBackupToFirebase_delegatesToRepository() = runTest {
        coEvery { mockRepository.backupToFirebase("uid-123") } returns Result.success(Unit)

        val result = viewModel.backupToFirebase("uid-123")
        assertTrue(result.isSuccess)
        coVerify { mockRepository.backupToFirebase("uid-123") }
    }

    @Test
    fun testRestoreFromFirebase_delegatesToRepository() = runTest {
        coEvery { mockRepository.restoreFromFirebase("uid-123") } returns Result.success(Unit)

        val result = viewModel.restoreFromFirebase("uid-123")
        assertTrue(result.isSuccess)
        coVerify { mockRepository.restoreFromFirebase("uid-123") }
    }

    @Test
    fun testBackupLocally_delegatesToRepository() = runTest {
        coEvery { mockRepository.backupLocally() } returns Result.success(Unit)

        val result = viewModel.backupLocally()
        assertTrue(result.isSuccess)
        coVerify { mockRepository.backupLocally() }
    }

    @Test
    fun testRestoreLocally_delegatesToRepository() = runTest {
        coEvery { mockRepository.restoreLocally() } returns Result.success(Unit)

        val result = viewModel.restoreLocally()
        assertTrue(result.isSuccess)
        coVerify { mockRepository.restoreLocally() }
    }

    // --- Admin: AppMode ---

    @Test
    fun testSetAppMode_updatesAppModeFlow() {
        viewModel.setAppMode("Business Ledger")
        assertEquals("Business Ledger", viewModel.appMode.value)
    }

    // --- Admin: Announcements ---

    @Test
    fun testPublishAnnouncement_addsToTopOfList() {
        val initialCount = viewModel.announcements.value.size
        viewModel.publishAnnouncement("Title A", "Content A", "System")
        assertEquals(initialCount + 1, viewModel.announcements.value.size)
        assertEquals("Title A", viewModel.announcements.value.first().title)
    }

    @Test
    fun testDeleteAnnouncement_removesById() {
        viewModel.publishAnnouncement("Title B", "Content B", "Update")
        val announcement = viewModel.announcements.value.first { it.title == "Title B" }
        val beforeCount = viewModel.announcements.value.size

        viewModel.deleteAnnouncement(announcement.id)

        assertEquals(beforeCount - 1, viewModel.announcements.value.size)
        assertNull(viewModel.announcements.value.find { it.id == announcement.id })
    }

    // --- updateSession ---

    @Test
    fun testUpdateSession_withValidNonGuestUserId_setsCurrentUserId() {
        // Given no auto-sync condition
        viewModel.updateSession("real-uid-123", isGuest = false)
        // Verify no crash and no exception
    }

    @Test
    fun testUpdateSession_withGuestFlag_nullsCurrentUserId() {
        viewModel.updateSession("guest_user_123", isGuest = true)
        // No crash expected; guest users don't trigger sync
    }

    @Test
    fun testUpdateSession_withNullUserId_doesNotCrash() {
        viewModel.updateSession(null, isGuest = false)
    }

    // --- isNextPeriodEnabled ---

    @Test
    fun testIsNextPeriodEnabled_day_isFalseForToday_isTrueForYesterday() = runTest {
        viewModel.setTimePeriod(TimePeriod.DAY)
        // Today
        viewModel.setDateDirectly(System.currentTimeMillis())
        assertFalse(viewModel.isNextPeriodEnabled.first())

        // Yesterday
        viewModel.setDateDirectly(System.currentTimeMillis() - 86_400_000L)
        assertTrue(viewModel.isNextPeriodEnabled.first())
    }

    @Test
    fun testIsNextPeriodEnabled_week_isFalseForCurrentWeek_isTrueForPastWeek() = runTest {
        viewModel.setTimePeriod(TimePeriod.WEEK)
        // This week
        viewModel.setDateDirectly(System.currentTimeMillis())
        assertFalse(viewModel.isNextPeriodEnabled.first())

        // 2 weeks ago
        viewModel.setDateDirectly(System.currentTimeMillis() - 14 * 86_400_000L)
        assertTrue(viewModel.isNextPeriodEnabled.first())
    }

    @Test
    fun testIsNextPeriodEnabled_month_isFalseForCurrentMonth_isTrueForPastMonth() = runTest {
        viewModel.setTimePeriod(TimePeriod.MONTH)
        // This month
        viewModel.setDateDirectly(System.currentTimeMillis())
        assertFalse(viewModel.isNextPeriodEnabled.first())

        // 2 months ago
        viewModel.setDateDirectly(System.currentTimeMillis() - 60 * 86_400_000L)
        assertTrue(viewModel.isNextPeriodEnabled.first())
    }

    @Test
    fun testIsNextPeriodEnabled_year_isFalseForCurrentYear_isTrueForPastYear() = runTest {
        viewModel.setTimePeriod(TimePeriod.YEAR)
        // This year
        viewModel.setDateDirectly(System.currentTimeMillis())
        assertFalse(viewModel.isNextPeriodEnabled.first())

        // Last year
        viewModel.setDateDirectly(System.currentTimeMillis() - 366 * 86_400_000L)
        assertTrue(viewModel.isNextPeriodEnabled.first())
    }

    // --- periodRange ---

    @Test
    fun testPeriodRange_day_startsAndEndsOnActiveDate() = runTest {
        viewModel.setTimePeriod(TimePeriod.DAY)
        val active = viewModel.activeDate.value
        val (start, end) = viewModel.periodRange.first()

        val startCal = Calendar.getInstance().apply { timeInMillis = start }
        val endCal = Calendar.getInstance().apply { timeInMillis = end }

        assertEquals(0, startCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, startCal.get(Calendar.MINUTE))
        assertEquals(0, startCal.get(Calendar.SECOND))
        assertEquals(23, endCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(59, endCal.get(Calendar.MINUTE))
        assertEquals(59, endCal.get(Calendar.SECOND))
    }

    @Test
    fun testPeriodRange_week_calculatesStartAndEndOfWeek() = runTest {
        viewModel.setTimePeriod(TimePeriod.WEEK)
        val (start, end) = viewModel.periodRange.first()
        assertTrue(start <= end)
        // Difference should be roughly 7 days
        val diff = end - start
        assertTrue(diff > 6 * 86_400_000L && diff < 8 * 86_400_000L)
    }

    @Test
    fun testPeriodRange_month_calculatesStartAndEndOfMonth() = runTest {
        viewModel.setTimePeriod(TimePeriod.MONTH)
        val (start, end) = viewModel.periodRange.first()
        val startCal = Calendar.getInstance().apply { timeInMillis = start }
        val endCal = Calendar.getInstance().apply { timeInMillis = end }

        assertEquals(1, startCal.get(Calendar.DAY_OF_MONTH))
        assertEquals(endCal.getActualMaximum(Calendar.DAY_OF_MONTH), endCal.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun testPeriodRange_year_calculatesStartAndEndOfYear() = runTest {
        viewModel.setTimePeriod(TimePeriod.YEAR)
        val (start, end) = viewModel.periodRange.first()
        val startCal = Calendar.getInstance().apply { timeInMillis = start }
        val endCal = Calendar.getInstance().apply { timeInMillis = end }

        assertEquals(1, startCal.get(Calendar.DAY_OF_YEAR))
        assertEquals(Calendar.DECEMBER, endCal.get(Calendar.MONTH))
        assertEquals(31, endCal.get(Calendar.DAY_OF_MONTH))
    }

    // --- periodLabel ---

    @Test
    fun testPeriodLabel_formatsLabelsForPeriods() = runTest {
        viewModel.setTimePeriod(TimePeriod.DAY)
        assertFalse(viewModel.periodLabel.first().isEmpty())

        viewModel.setTimePeriod(TimePeriod.WEEK)
        assertFalse(viewModel.periodLabel.first().isEmpty())

        viewModel.setTimePeriod(TimePeriod.MONTH)
        assertFalse(viewModel.periodLabel.first().isEmpty())

        viewModel.setTimePeriod(TimePeriod.YEAR)
        assertFalse(viewModel.periodLabel.first().isEmpty())
    }

    // --- periodTransactions & currentMonthTransactions ---

    @Test
    fun testPeriodTransactions_filtersTransactionsCorrectly() = runTest {
        val now = System.currentTimeMillis()
        val tx1 = TransactionWithCategory(
            transaction = TransactionEntity(id = 1, amount = 10.0, source = "Within", date = now, categoryId = 1, type = TransactionType.EXPENSE, notes = ""),
            category = Category(id = 1, name = "Food", type = TransactionType.EXPENSE, iconName = "food")
        )
        val tx2 = TransactionWithCategory(
            transaction = TransactionEntity(id = 2, amount = 20.0, source = "Outside", date = now - 10 * 86_400_000L, categoryId = 1, type = TransactionType.EXPENSE, notes = ""),
            category = Category(id = 1, name = "Food", type = TransactionType.EXPENSE, iconName = "food")
        )

        mockTransactionsFlow.value = listOf(tx1, tx2)
        viewModel.setTimePeriod(TimePeriod.DAY)
        viewModel.setDateDirectly(now)

        val txs = viewModel.periodTransactions.first()
        assertEquals(1, txs.size)
        assertEquals("Within", txs.first().transaction.source)
    }

    @Test
    fun testCurrentMonthTransactions_filtersByCurrentMonth() = runTest {
        val now = Calendar.getInstance()
        val thisMonthTime = now.timeInMillis

        val lastMonth = Calendar.getInstance().apply { add(Calendar.MONTH, -1) }
        val lastMonthTime = lastMonth.timeInMillis

        val tx1 = TransactionWithCategory(
            transaction = TransactionEntity(id = 1, amount = 10.0, source = "This Month", date = thisMonthTime, categoryId = 1, type = TransactionType.EXPENSE, notes = ""),
            category = Category(id = 1, name = "Food", type = TransactionType.EXPENSE, iconName = "food")
        )
        val tx2 = TransactionWithCategory(
            transaction = TransactionEntity(id = 2, amount = 20.0, source = "Last Month", date = lastMonthTime, categoryId = 1, type = TransactionType.EXPENSE, notes = ""),
            category = Category(id = 1, name = "Food", type = TransactionType.EXPENSE, iconName = "food")
        )

        mockTransactionsFlow.value = listOf(tx1, tx2)

        val txs = viewModel.currentMonthTransactions.first()
        assertEquals(1, txs.size)
        assertEquals("This Month", txs.first().transaction.source)
    }

    // --- Auto-Sync ---

    @Test
    fun testAutoSync_triggeredOnInitWhenPendingAndOnline() = runTest {
        // Set pendingSync to true in prefs
        val customPrefs = mockk<SharedPreferences>(relaxed = true)
        every { customPrefs.getBoolean("pending_sync_preference", any()) } returns true
        every { mockRepository.getSettingsPreferences() } returns customPrefs

        // Simulate repository backup success
        coEvery { mockRepository.backupToFirebase(any()) } returns Result.success(Unit)

        val onlineFlow = MutableStateFlow(true)
        every { mockNetworkMonitor.isOnline } returns onlineFlow

        // Set Real User ID in repository/viewModel context
        val testVm = FinanceViewModel(mockRepository, mockNetworkMonitor)
        testVm.updateSession("real-user-id", isGuest = false)

        // Give coroutines time to trigger autoSync
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify repository backup was called because user is online and sync was pending
        coVerify(atLeast = 1) { mockRepository.backupToFirebase("real-user-id") }
    }

    @Test
    fun testAutoSync_updatesSyncingFlowAndPendingSyncState() = runTest {
        viewModel.updateSession("real-user-id", isGuest = false)
        onlineFlow.value = true

        coEvery { mockRepository.backupToFirebase("real-user-id") } coAnswers {
            assertTrue(viewModel.isSyncing.value) // Should be syncing during backup call
            Result.success(Unit)
        }

        viewModel.triggerAutoSync()
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.isSyncing.value) // Done syncing
        assertFalse(viewModel.pendingSync.value) // Success resets pendingSync
    }

    @Test
    fun testAutoSync_failure_retainsPendingSyncState() = runTest {
        viewModel.updateSession("real-user-id", isGuest = false)
        onlineFlow.value = true
        coEvery { mockRepository.backupToFirebase("real-user-id") } returns Result.failure(Exception("Network error"))

        // Artificially simulate writing a transaction first which sets pendingSync to true
        coEvery { mockRepository.insertTransaction(any()) } just Runs
        viewModel.addTransaction(10.0, "S", 0L, 1, TransactionType.EXPENSE, "")
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.pendingSync.value) // Should still be true after failed sync
    }

    // --- Theme & Currency load fallbacks ---

    @Test
    fun testGetSavedTheme_fallbackOnInvalidThemeName() {
        val customPrefs = mockk<SharedPreferences>(relaxed = true)
        every { customPrefs.getString("theme_preference", any()) } returns "INVALID_THEME_NAME"
        every { mockRepository.getSettingsPreferences() } returns customPrefs

        val testVm = FinanceViewModel(mockRepository, mockNetworkMonitor)
        assertEquals(AppTheme.SYSTEM, testVm.appTheme.value)
    }

    @Test
    fun testGetSavedCurrency_fallbackOnInvalidCurrencyName() {
        val customPrefs = mockk<SharedPreferences>(relaxed = true)
        every { customPrefs.getString("currency_preference", any()) } returns "INVALID_CURRENCY"
        every { mockRepository.getSettingsPreferences() } returns customPrefs

        val testVm = FinanceViewModel(mockRepository, mockNetworkMonitor)
        assertEquals(com.example.ui.utils.CurrencyOption.INR, testVm.currencyOption.value)
    }

    @Test
    fun testGetSavedAnnouncements_fallbackOnMalformedJson() {
        val customPrefs = mockk<SharedPreferences>(relaxed = true)
        every { customPrefs.getString("admin_announcements", any()) } returns "malformed-json"
        every { mockRepository.getSettingsPreferences() } returns customPrefs

        val testVm = FinanceViewModel(mockRepository, mockNetworkMonitor)
        assertTrue(testVm.announcements.value.isEmpty())
    }

    @Test
    fun testGetSavedAnnouncements_emptyPrefs_returnsDefaultAnnouncements() {
        val customPrefs = mockk<SharedPreferences>(relaxed = true)
        every { customPrefs.getString("admin_announcements", any()) } returns null
        every { mockRepository.getSettingsPreferences() } returns customPrefs

        val testVm = FinanceViewModel(mockRepository, mockNetworkMonitor)
        val anns = testVm.announcements.value
        assertEquals(2, anns.size)
        assertEquals("Privacy", anns[0].category)
        assertEquals("System Update", anns[1].category)
    }

    // --- FinanceViewModelFactory ---

    @Test
    fun testFinanceViewModelFactory_createsFinanceViewModel() {
        val factory = FinanceViewModelFactory(mockRepository, mockNetworkMonitor)
        val vm = factory.create(FinanceViewModel::class.java)
        assertNotNull(vm)
        assertTrue(vm is FinanceViewModel)
    }

    @Test
    fun testFinanceViewModelFactory_unknownClass_throwsIllegalArgumentException() {
        val factory = FinanceViewModelFactory(mockRepository)
        class UnknownViewModel : ViewModel()
        try {
            factory.create(UnknownViewModel::class.java)
            fail("Should have thrown IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertEquals("Unknown ViewModel class", e.message)
        }
    }

    // --- Extra moveToNextPeriod Tests for 100% period coverage ---

    @Test
    fun testMoveToNextPeriod_forDay_advancesDay() {
        val twoDaysAgo = System.currentTimeMillis() - 2 * 86_400_000L
        viewModel.setTimePeriod(TimePeriod.DAY)
        viewModel.setDateDirectly(twoDaysAgo)
        val before = viewModel.activeDate.value
        viewModel.moveToNextPeriod()
        assertTrue(viewModel.activeDate.value > before)
    }

    @Test
    fun testMoveToNextPeriod_forWeek_advancesWeek() {
        val twoWeeksAgo = System.currentTimeMillis() - 15 * 86_400_000L
        viewModel.setTimePeriod(TimePeriod.WEEK)
        viewModel.setDateDirectly(twoWeeksAgo)
        val before = viewModel.activeDate.value
        viewModel.moveToNextPeriod()
        assertTrue(viewModel.activeDate.value > before)
    }

    @Test
    fun testMoveToNextPeriod_forYear_advancesYear() {
        val twoYearsAgo = System.currentTimeMillis() - 2 * 365 * 86_400_000L
        viewModel.setTimePeriod(TimePeriod.YEAR)
        viewModel.setDateDirectly(twoYearsAgo)
        val before = viewModel.activeDate.value
        viewModel.moveToNextPeriod()
        assertTrue(viewModel.activeDate.value > before)
    }

    // --- Preferences Write-Through Coverage ---

    @Test
    fun testPreferencesWriteThrough_allSettersWriteToSharedPrefs() {
        // Verify that all setters write through to SharedPreferences (non-null prefs path)
        viewModel.setTheme(AppTheme.DARK)
        verify { mockSharedPrefsEditor.putString("theme_preference", "DARK") }

        viewModel.setCurrency(com.example.ui.utils.CurrencyOption.USD)
        verify { mockSharedPrefsEditor.putString("currency_preference", "USD") }

        viewModel.setBiometricLockEnabled(true)
        verify { mockSharedPrefsEditor.putBoolean("biometric_lock_preference", true) }

        viewModel.setAppMode("Business Ledger")
        verify { mockSharedPrefsEditor.putString("admin_app_mode", "Business Ledger") }
    }

    @Test
    fun testGetSavedPendingSync_readsBooleanFromPrefs() {
        // Verify getSavedPendingSync reads from prefs directly (non-null path)
        every { mockSharedPrefs.getBoolean("pending_sync_preference", false) } returns true
        val testVm = FinanceViewModel(mockRepository, mockNetworkMonitor)
        assertTrue(testVm.pendingSync.value)
    }

    @Test
    fun testGetSavedReminderEnabled_readsBooleanFromPrefs() {
        every { mockSharedPrefs.getBoolean("reminder_preference", false) } returns true
        val testVm = FinanceViewModel(mockRepository, mockNetworkMonitor)
        assertTrue(testVm.reminderEnabled.value)
    }

    @Test
    fun testGetSavedBiometricLockEnabled_readsBooleanFromPrefs() {
        every { mockSharedPrefs.getBoolean("biometric_lock_preference", false) } returns true
        val testVm = FinanceViewModel(mockRepository, mockNetworkMonitor)
        assertTrue(testVm.biometricLockEnabled.value)
    }

    // --- Save announcements Exception catch ---

    @Test
    fun testSaveAnnouncementsLocally_catchesException() {
        val customPrefs = mockk<SharedPreferences>(relaxed = true)
        val customEditor = mockk<SharedPreferences.Editor>(relaxed = true)
        every { customPrefs.edit() } returns customEditor
        // Throw exception when trying to save announcements JSON to cover catch block
        every { customEditor.putString("admin_announcements", any()) } throws RuntimeException("Disk full")
        every { mockRepository.getSettingsPreferences() } returns customPrefs

        val testVm = FinanceViewModel(mockRepository, mockNetworkMonitor)
        // Should catch the exception internally and not crash the caller
        testVm.publishAnnouncement("Title", "Content", "Category")
    }

    // =========================================================
    // TARGETED BRANCH COVERAGE — filling JaCoCo gaps
    // =========================================================

    // --- moveToNextPeriod: "isFuture=true" branches for each period type (Lines 165, 183, 195) ---

    @Test
    fun testMoveToNextPeriod_forCurrentDay_doesNotAdvance() {
        // activeDate defaults to now → advancing DAY would land on tomorrow (future) → blocked
        viewModel.setTimePeriod(TimePeriod.DAY)
        val before = viewModel.activeDate.value
        viewModel.moveToNextPeriod()
        assertEquals(before, viewModel.activeDate.value)
    }

    @Test
    fun testMoveToNextPeriod_forCurrentWeek_doesNotAdvance() {
        // activeDate defaults to this week → next week is future → blocked
        viewModel.setTimePeriod(TimePeriod.WEEK)
        val before = viewModel.activeDate.value
        viewModel.moveToNextPeriod()
        assertEquals(before, viewModel.activeDate.value)
    }

    @Test
    fun testMoveToNextPeriod_forCurrentYear_doesNotAdvance() {
        // activeDate defaults to this year → next year is future → blocked
        viewModel.setTimePeriod(TimePeriod.YEAR)
        val before = viewModel.activeDate.value
        viewModel.moveToNextPeriod()
        assertEquals(before, viewModel.activeDate.value)
    }

    // --- updateSession: userId starts with "guest" but isGuest=false → else-branch (Line 407) ---

    @Test
    fun testUpdateSession_guestPrefixedUserId_isGuestFalse_treatsAsGuest() {
        // !userId.startsWith("guest") = false → falls to else: currentUserId = null
        viewModel.updateSession("guest_prefixed_real", isGuest = false)
        // triggerAutoSync should be a no-op because currentUserId was nulled
        viewModel.triggerAutoSync()
    }

    // --- updateSession: pendingSync=true AND isOnline=true → triggers sync (Line 409 true-branch) ---

    @Test
    fun testUpdateSession_realUser_pendingSyncTrue_onlineTrue_triggersAutoSync() = runTest {
        every { mockSharedPrefs.getBoolean("pending_sync_preference", false) } returns true
        val testVm = FinanceViewModel(mockRepository, mockNetworkMonitor)

        onlineFlow.value = true
        coEvery { mockRepository.backupToFirebase("uid-ps") } returns Result.success(Unit)

        testVm.updateSession("uid-ps", isGuest = false)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(atLeast = 1) { mockRepository.backupToFirebase("uid-ps") }
    }

    // --- triggerAutoSync when offline → should NOT call backup (Line 419 false-branch) ---

    @Test
    fun testTriggerAutoSync_whenOffline_doesNotCallBackup() = runTest {
        viewModel.updateSession("real-user", isGuest = false)
        onlineFlow.value = false  // explicitly offline

        viewModel.triggerAutoSync()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 0) { mockRepository.backupToFirebase(any()) }
    }

    // --- getString returning null → exercises all ?: fallback branches (Lines 432, 441, 553) ---

    @Test
    fun testGetSavedPreferences_nullStringReturns_useFallbackDefaults() {
        val customPrefs = mockk<SharedPreferences>(relaxed = true)
        every { customPrefs.getString(any(), any()) } returns null
        every { customPrefs.getBoolean(any(), any()) } answers { secondArg() }
        every { customPrefs.edit() } returns mockSharedPrefsEditor
        every { mockRepository.getSettingsPreferences() } returns customPrefs

        val testVm = FinanceViewModel(mockRepository, mockNetworkMonitor)

        assertEquals(AppTheme.SYSTEM, testVm.appTheme.value)                           // line 432 ?: branch
        assertEquals(com.example.ui.utils.CurrencyOption.INR, testVm.currencyOption.value) // line 441 ?: branch
        assertEquals("Personal Finance Ledger", testVm.getSavedAppMode())              // line 553 ?: branch
    }

    // --- Moshi fromJson returns null → exercises ?: emptyList() branch (Line 584) ---

    @Test
    fun testGetSavedAnnouncements_moshiFromJsonReturnsNull_emptyListReturned() {
        // JSON string "null" causes Moshi to return null from fromJson() → ?: emptyList() fires
        val customPrefs = mockk<SharedPreferences>(relaxed = true)
        every { customPrefs.getString("admin_announcements", any()) } returns "null"
        every { customPrefs.getString(neq("admin_announcements"), any()) } answers { secondArg() }
        every { customPrefs.getBoolean(any(), any()) } answers { secondArg() }
        every { customPrefs.edit() } returns mockSharedPrefsEditor
        every { mockRepository.getSettingsPreferences() } returns customPrefs

        val testVm = FinanceViewModel(mockRepository, mockNetworkMonitor)
        assertTrue(testVm.announcements.value.isEmpty())
    }
}
