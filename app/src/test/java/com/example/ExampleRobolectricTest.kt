package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.JsonDataManager
import com.example.data.repository.FinanceRepository
import com.example.ui.viewmodel.FinanceViewModel
import com.example.ui.viewmodel.TimePeriod
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Finance Tracker", appName)
    }

    @Test
    fun `future date selection is blocked in FinanceViewModel`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val jsonDataManager = JsonDataManager(context)
        val repository = FinanceRepository(db.financeDao(), jsonDataManager)
        val viewModel = FinanceViewModel(repository)

        // Ensure active date is initialized to around now
        val initialDate = viewModel.activeDate.value
        val now = System.currentTimeMillis()
        assertTrue(initialDate <= now + 1000)

        // Try setting a future date (e.g. tomorrow or next year)
        val tomorrowCal = Calendar.getInstance().apply {
            add(Calendar.DATE, 2) // Definitely in the future
        }
        viewModel.setDateDirectly(tomorrowCal.timeInMillis)

        // Value should remain unchanged (still <= initial date/now)
        val dateAfterSettingFuture = viewModel.activeDate.value
        assertTrue(dateAfterSettingFuture <= now + 1000)
        assertNotEquals(tomorrowCal.timeInMillis, dateAfterSettingFuture)

        // Try moving to next period when we are currently at "now" month (activeDate = now)
        viewModel.setTimePeriod(TimePeriod.MONTH)
        viewModel.moveToNextPeriod()

        // Since it would be in the future, active date must still be the same/not in the future
        val dateAfterNextPeriod = viewModel.activeDate.value
        assertTrue(dateAfterNextPeriod <= now + 1000)

        // Try setting a past date and make sure it is successfully set
        val pastCal = Calendar.getInstance().apply {
            add(Calendar.MONTH, -2) // 2 months ago
        }
        viewModel.setDateDirectly(pastCal.timeInMillis)
        assertEquals(pastCal.timeInMillis, viewModel.activeDate.value)

        db.close()
    }
}
