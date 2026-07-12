package com.example.ui.viewmodel

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.JsonDataManager
import com.example.data.repository.FinanceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FinanceViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var db: AppDatabase
    private lateinit var repository: FinanceRepository
    private lateinit var viewModel: FinanceViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .setQueryExecutor { it.run() }
            .setTransactionExecutor { it.run() }
            .build()
        val jsonDataManager = JsonDataManager(context)
        repository = FinanceRepository(db.financeDao(), jsonDataManager)
        viewModel = FinanceViewModel(repository)
    }

    @After
    fun tearDown() {
        db.close()
        Dispatchers.resetMain()
    }

    @Test
    fun testSetTimePeriod_updatesPeriodAndResetsActiveDateToNow() {
        viewModel.setTimePeriod(TimePeriod.YEAR)
        testDispatcher.scheduler.advanceUntilIdle()
        db.invalidationTracker.refreshVersionsSync()

        assertEquals(TimePeriod.YEAR, viewModel.selectedTimePeriod.value)
        // Check activeDate is updated to near system time (within 1000ms offset)
        val now = System.currentTimeMillis()
        assertTrue(Math.abs(viewModel.activeDate.value - now) < 1000)
    }

    @Test
    fun testSetDateDirectly_pastDate_setsDate() {
        val cal = Calendar.getInstance().apply {
            add(Calendar.DATE, -5)
        }
        viewModel.setDateDirectly(cal.timeInMillis)
        testDispatcher.scheduler.advanceUntilIdle()
        db.invalidationTracker.refreshVersionsSync()

        assertEquals(cal.timeInMillis, viewModel.activeDate.value)
    }

    @Test
    fun testSetDateDirectly_futureDate_isBlocked() {
        val originalDate = viewModel.activeDate.value
        val futureCal = Calendar.getInstance().apply {
            add(Calendar.DATE, 5)
        }
        
        viewModel.setDateDirectly(futureCal.timeInMillis)
        testDispatcher.scheduler.advanceUntilIdle()
        db.invalidationTracker.refreshVersionsSync()

        // Active date should remain unchanged
        assertEquals(originalDate, viewModel.activeDate.value)
    }

    @Test
    fun testMoveToPreviousPeriod_day_subtractsOneDay() {
        viewModel.setTimePeriod(TimePeriod.DAY)
        val initialDate = viewModel.activeDate.value
        
        viewModel.moveToPreviousPeriod()
        testDispatcher.scheduler.advanceUntilIdle()
        db.invalidationTracker.refreshVersionsSync()

        val expectedCal = Calendar.getInstance().apply {
            timeInMillis = initialDate
            add(Calendar.DATE, -1)
        }
        assertEquals(expectedCal.timeInMillis, viewModel.activeDate.value)
    }

    @Test
    fun testMoveToNextPeriod_isBlockedIfFuture() {
        // Since activeDate starts at System.currentTimeMillis(), moving to next period (into the future)
        // should be blocked immediately.
        viewModel.setTimePeriod(TimePeriod.DAY)
        val initialDate = viewModel.activeDate.value

        viewModel.moveToNextPeriod()
        testDispatcher.scheduler.advanceUntilIdle()
        db.invalidationTracker.refreshVersionsSync()

        // Should not change because it's in the future
        assertEquals(initialDate, viewModel.activeDate.value)
    }

    @Test
    fun testMoveToNextPeriod_allowedIfWasInPast() {
        // 1. Move to previous period (past)
        viewModel.setTimePeriod(TimePeriod.DAY)
        viewModel.moveToPreviousPeriod()
        testDispatcher.scheduler.advanceUntilIdle()
        db.invalidationTracker.refreshVersionsSync()
        val pastDate = viewModel.activeDate.value

        // 2. Move to next period (which returns to today)
        viewModel.moveToNextPeriod()
        testDispatcher.scheduler.advanceUntilIdle()
        db.invalidationTracker.refreshVersionsSync()

        // Should successfully increment
        val expectedCal = Calendar.getInstance().apply {
            timeInMillis = pastDate
            add(Calendar.DATE, 1)
        }
        assertEquals(expectedCal.timeInMillis, viewModel.activeDate.value)
    }

    @Test
    fun testSeedDemoTransactions_launchesJob() = runTest {
        viewModel.seedDemoTransactions()
        testDispatcher.scheduler.advanceUntilIdle()
        db.invalidationTracker.refreshVersionsSync()
        val dbTx = db.financeDao().getAllTransactionsOnce()
        assertTrue(dbTx.isNotEmpty())
    }

    @Test
    fun testClearAllData_deletesAllData() = runTest {
        viewModel.seedDemoTransactions()
        testDispatcher.scheduler.advanceUntilIdle()
        db.invalidationTracker.refreshVersionsSync()
        assertTrue(db.financeDao().getAllTransactionsOnce().isNotEmpty())

        viewModel.clearAllData()
        testDispatcher.scheduler.advanceUntilIdle()
        db.invalidationTracker.refreshVersionsSync()
        assertTrue(db.financeDao().getAllTransactionsOnce().isEmpty())
        assertTrue(db.financeDao().getAllCategoriesOnce().isEmpty())
    }
}
