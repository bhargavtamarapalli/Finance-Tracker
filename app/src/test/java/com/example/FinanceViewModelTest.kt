package com.example

import android.content.Context
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.Category
import com.example.data.model.TransactionType
import com.example.data.repository.FinanceRepository
import com.example.ui.viewmodel.FinanceViewModel
import com.example.ui.viewmodel.TimePeriod
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class FinanceViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var repository: FinanceRepository
    private lateinit var sharedPrefs: SharedPreferences
    private lateinit var viewModel: FinanceViewModel

    private val mockCategoriesFlow = MutableStateFlow<List<Category>>(emptyList())
    private val mockTransactionsFlow = MutableStateFlow<List<com.example.data.model.TransactionWithCategory>>(emptyList())

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)

        val context = ApplicationProvider.getApplicationContext<Context>()
        sharedPrefs = context.getSharedPreferences("test_prefs", Context.MODE_PRIVATE)

        every { repository.getContext() } returns context
        every { repository.getSettingsPreferences() } returns sharedPrefs
        every { repository.allCategories } returns mockCategoriesFlow
        every { repository.allTransactions } returns mockTransactionsFlow

        viewModel = FinanceViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun setTimePeriod_updatesTimePeriodFlow() = runTest {
        assertEquals(TimePeriod.MONTH, viewModel.selectedTimePeriod.value)

        viewModel.setTimePeriod(TimePeriod.WEEK)

        assertEquals(TimePeriod.WEEK, viewModel.selectedTimePeriod.value)
    }

    @Test
    fun addCategory_delegatesToRepository() = runTest {
        val categorySlot = slot<Category>()
        coEvery { repository.insertCategory(capture(categorySlot)) } returns Unit

        viewModel.addCategory("Groceries", TransactionType.EXPENSE, "shopping_cart")

        coVerify(exactly = 1) { repository.insertCategory(any()) }
        assertEquals("Groceries", categorySlot.captured.name)
        assertEquals(TransactionType.EXPENSE, categorySlot.captured.type)
        assertEquals("shopping_cart", categorySlot.captured.iconName)
    }
}
