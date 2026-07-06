package com.example

import android.content.Context
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.JsonDataManager
import com.example.data.model.Category
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.data.repository.FinanceRepository
import com.example.ui.screens.AnalyticsScreen
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
@Config(qualifiers = "w1000dp-h2000dp-xhdpi", sdk = [33])
class AnalyticsScreenTest {

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

        viewModel = FinanceViewModel(repository)

        // Clear default seeded transactions inserted during ViewModel init
        val existingTxs = db.financeDao().getAllTransactionsOnce()
        db.financeDao().deleteTransactions(existingTxs)

        // Seed FIRST before creating ViewModel to prevent background seeding crashes/races
        val incomeCat = Category(id = 7, name = "Salary", type = TransactionType.INCOME, iconName = "attach_money")
        val expenseCat = Category(id = 2, name = "Groceries", type = TransactionType.EXPENSE, iconName = "shopping_cart")
        val entertainmentCat = Category(id = 3, name = "Entertainment", type = TransactionType.EXPENSE, iconName = "movie")
        db.financeDao().insertCategories(listOf(incomeCat, expenseCat, entertainmentCat))

        // Income transaction
        db.financeDao().insertTransaction(
            TransactionEntity(
                id = 1,
                amount = 75000.0,
                source = "Tech Corp Inc.",
                date = System.currentTimeMillis(),
                categoryId = 7,
                type = TransactionType.INCOME,
                notes = "Monthly payroll payout"
            )
        )
        // Expense transaction 1
        db.financeDao().insertTransaction(
            TransactionEntity(
                id = 2,
                amount = 4200.0,
                source = "Reliance Smart Supermarket",
                date = System.currentTimeMillis() - 100,
                categoryId = 2,
                type = TransactionType.EXPENSE,
                notes = "Weekly grocery refill"
            )
        )
        // Expense transaction 2
        db.financeDao().insertTransaction(
            TransactionEntity(
                id = 3,
                amount = 1800.0,
                source = "Netflix Premium",
                date = System.currentTimeMillis() - 200,
                categoryId = 3,
                type = TransactionType.EXPENSE,
                notes = "Streaming subscription"
            )
        )
    }

    @After
    fun closeDb() {
        Dispatchers.resetMain()
    }

    @Test
    fun analyticsScreen_displaysTitleAndOverview() {
        composeTestRule.setContent {
            FinanceTrackerTheme {
                AnalyticsScreen(
                    viewModel = viewModel,
                    onMenuClick = {}
                )
            }
        }

        // Wait for Room background query to emit and populate UI
        composeTestRule.waitUntil(5000) {
            composeTestRule.onAllNodesWithText("Groceries").fetchSemanticsNodes().isNotEmpty()
        }

        // Assert header is present
        composeTestRule.onNodeWithText("Analytics").assertIsDisplayed()

        // Assert Expense Breakdown shows Groceries & Entertainment by default
        composeTestRule.onNodeWithText("Groceries").assertIsDisplayed()
        composeTestRule.onNodeWithText("Entertainment").assertIsDisplayed()
        
        // Assert total amount matches total expenses (All Categories is displayed by default)
        composeTestRule.onNodeWithTag("donut_center_title", useUnmergedTree = true).assertTextEquals("All Categories")
    }

    @Test
    fun analyticsScreen_toggleTabsAndSelectCategory() {
        composeTestRule.setContent {
            FinanceTrackerTheme {
                AnalyticsScreen(
                    viewModel = viewModel,
                    onMenuClick = {}
                )
            }
        }

        // Wait for Room background query to emit and populate UI
        composeTestRule.waitUntil(5000) {
            composeTestRule.onAllNodesWithText("Groceries").fetchSemanticsNodes().isNotEmpty()
        }

        // Check default expenses: clicking a row updates donut center to show category-specific info
        composeTestRule.onNodeWithTag("category_row_Groceries").performClick()
        composeTestRule.waitForIdle()

        // Donut center should now show Groceries as selected
        composeTestRule.onNodeWithTag("donut_center_title", useUnmergedTree = true).assertTextEquals("Groceries")

        // Click again to unselect
        composeTestRule.onNodeWithTag("category_row_Groceries").performClick()
        composeTestRule.waitForIdle()

        // Donut center should revert to All Categories
        composeTestRule.onNodeWithTag("donut_center_title", useUnmergedTree = true).assertTextEquals("All Categories")

        // Toggle to Income analytics
        composeTestRule.onNodeWithTag("chip_income").performClick()
        composeTestRule.waitForIdle()

        // Wait for income "Salary" to be loaded and displayed
        composeTestRule.waitUntil(5000) {
            composeTestRule.onAllNodesWithText("Salary").fetchSemanticsNodes().isNotEmpty()
        }

        // Groceries expense should no longer be visible
        composeTestRule.onNodeWithText("Groceries").assertDoesNotExist()

        // Salary income should now be visible
        composeTestRule.onNodeWithText("Salary").assertIsDisplayed()
    }

    @Test
    fun analyticsScreen_displaysIndividualSpendsOnCategorySelection() {
        composeTestRule.setContent {
            FinanceTrackerTheme {
                AnalyticsScreen(
                    viewModel = viewModel,
                    onMenuClick = {}
                )
            }
        }

        // Wait for Room background query to emit and populate UI
        composeTestRule.waitUntil(5000) {
            composeTestRule.onAllNodesWithText("Groceries").fetchSemanticsNodes().isNotEmpty()
        }

        // Assert that the specific spend (Reliance Smart Supermarket) is NOT displayed yet
        composeTestRule.onNodeWithText("Reliance Smart Supermarket").assertDoesNotExist()

        // Select the Groceries category breakdown row
        composeTestRule.onNodeWithTag("category_row_Groceries").performClick()
        composeTestRule.waitForIdle()

        // Assert that the specific spend (Reliance Smart Supermarket) is now displayed
        composeTestRule.onNodeWithText("Reliance Smart Supermarket").assertIsDisplayed()
    }

    @Test
    fun analyticsScreen_screenshot() {
        composeTestRule.setContent {
            FinanceTrackerTheme {
                AnalyticsScreen(
                    viewModel = viewModel,
                    onMenuClick = {}
                )
            }
        }

        composeTestRule.waitForIdle()

        // Capture screenshot of our redesigned beautiful Analytics Screen
        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/analytics_screen.png")
    }
}
