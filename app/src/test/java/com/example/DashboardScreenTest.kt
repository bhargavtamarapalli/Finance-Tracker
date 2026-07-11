package com.example

import android.content.Context
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.navigation.compose.rememberNavController
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.JsonDataManager
import com.example.data.model.Category
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.data.repository.FinanceRepository
import com.example.ui.screens.DashboardScreen
import com.example.ui.theme.FinanceTrackerTheme
import com.example.ui.viewmodel.FinanceViewModel
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [33])
class DashboardScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var db: AppDatabase
    private lateinit var repository: FinanceRepository
    private lateinit var viewModel: FinanceViewModel

    @Before
    fun createDb() = runBlocking {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        val context = ApplicationProvider.getApplicationContext<Context>()
        val directExecutor = java.util.concurrent.Executor { it.run() }
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .setQueryExecutor(directExecutor)
            .setTransactionExecutor(directExecutor)
            .build()
        val jsonDataManager = JsonDataManager(context)
        repository = FinanceRepository(db.financeDao(), jsonDataManager)

        // Seed FIRST before creating ViewModel to prevent background seeding crashes/races
        val incomeCat = Category(id = 7, name = "Salary", type = TransactionType.INCOME, iconName = "attach_money")
        val expenseCat = Category(id = 2, name = "Groceries", type = TransactionType.EXPENSE, iconName = "shopping_cart")
        db.financeDao().insertCategories(listOf(incomeCat, expenseCat))

        db.financeDao().insertTransaction(
            TransactionEntity(
                id = 1,
                amount = 75000.0,
                source = "Tech Corp Inc.",
                date = System.currentTimeMillis() - 1000,
                categoryId = 7,
                type = TransactionType.INCOME,
                notes = "Monthly payroll payout"
            )
        )
        db.financeDao().insertTransaction(
            TransactionEntity(
                id = 2,
                amount = 4200.0,
                source = "Reliance Smart Supermarket",
                date = System.currentTimeMillis() - 5000,
                categoryId = 2,
                type = TransactionType.EXPENSE,
                notes = "Weekly grocery refill"
            )
        )

        viewModel = FinanceViewModel(repository)
    }

    @After
    fun closeDb() {
        Dispatchers.resetMain()
    }

    @Test
    fun dashboardScreen_displaysHeaderAndSummary() {
        var menuClicked = false
        composeTestRule.setContent {
            FinanceTrackerTheme {
                val navController = rememberNavController()
                DashboardScreen(
                    viewModel = viewModel,
                    navController = navController,
                    onMenuClick = { menuClicked = true }
                )
            }
        }

        // Wait for UI to settle
        composeTestRule.waitForIdle()

        // Assert that the Header elements are displayed
        composeTestRule.onNodeWithText("Finance Manager").assertIsDisplayed()
        


        composeTestRule.onNodeWithText("Total Balance", ignoreCase = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Income").assertIsDisplayed()
        composeTestRule.onNodeWithText("Expense").assertIsDisplayed()

        // Assert that Menu Button exists and clicking triggers onMenuClick
        composeTestRule.onNodeWithContentDescription("Menu").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Menu").performClick()
        
        assert(menuClicked)
    }

    @Test
    fun dashboardScreen_screenshot() {
        composeTestRule.setContent {
            FinanceTrackerTheme {
                val navController = rememberNavController()
                DashboardScreen(
                    viewModel = viewModel,
                    navController = navController,
                    onMenuClick = {}
                )
            }
        }

        composeTestRule.waitForIdle()

        // Capture screenshot of the Dashboard Screen
        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/dashboard_screen.png")
    }
}
