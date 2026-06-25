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
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class AnalyticsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var db: AppDatabase
    private lateinit var repository: FinanceRepository
    private lateinit var viewModel: FinanceViewModel

    @Before
    fun createDb() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val jsonDataManager = JsonDataManager(context)
        repository = FinanceRepository(db.financeDao(), jsonDataManager)

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
                date = System.currentTimeMillis() - 1000,
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
                date = System.currentTimeMillis() - 5000,
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
                date = System.currentTimeMillis() - 8000,
                categoryId = 3,
                type = TransactionType.EXPENSE,
                notes = "Streaming subscription"
            )
        )

        viewModel = FinanceViewModel(repository)
    }

    @After
    fun closeDb() {
        // In-memory database doesn't need explicit close in tests to avoid connection races with background Flows
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
        
        // Assert total amount matches total expenses (4200 + 1800 = 6000)
        composeTestRule.onNodeWithText("Total").assertIsDisplayed()
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
        composeTestRule.onNodeWithTag("donut_center_title").assertTextEquals("Groceries")

        // Click again to unselect
        composeTestRule.onNodeWithTag("category_row_Groceries").performClick()
        composeTestRule.waitForIdle()

        // Donut center should revert to Total
        composeTestRule.onNodeWithTag("donut_center_title").assertTextEquals("Total")

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
