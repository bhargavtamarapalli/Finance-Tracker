package com.example

import android.content.Context
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.compose.rememberNavController
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.JsonDataManager
import com.example.data.model.Category
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.data.repository.FinanceRepository
import com.example.ui.screens.TransactionHistoryScreen
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
class TransactionHistoryScreenTest {

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

        // Seed initial categories
        val incomeCat = Category(id = 7, name = "Salary", type = TransactionType.INCOME, iconName = "attach_money")
        val expenseCat = Category(id = 2, name = "Groceries", type = TransactionType.EXPENSE, iconName = "shopping_cart")
        db.financeDao().insertCategories(listOf(incomeCat, expenseCat))

        // Seed some transactions
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
    }

    @After
    fun closeDb() {
        Dispatchers.resetMain()
    }

    @Test
    fun historyScreen_displaysInitialTransactions() {
        composeTestRule.setContent {
            FinanceTrackerTheme {
                val navController = rememberNavController()
                TransactionHistoryScreen(
                    viewModel = viewModel,
                    navController = navController,
                    onMenuClick = {}
                )
            }
        }

        // Wait for Room background query to emit and populate UI
        composeTestRule.waitUntil(5000) {
            composeTestRule.onAllNodesWithText("Reliance Smart Supermarket").fetchSemanticsNodes().isNotEmpty()
        }

        // Verify the title is displayed
        composeTestRule.onNodeWithText("Transaction History").assertIsDisplayed()

        // Verify seeded transactions are visible
        composeTestRule.onNodeWithText("Tech Corp Inc.").assertIsDisplayed()
        composeTestRule.onNodeWithText("Reliance Smart Supermarket").assertIsDisplayed()
    }

    @Test
    fun historyScreen_performsSearch() {
        composeTestRule.setContent {
            FinanceTrackerTheme {
                val navController = rememberNavController()
                TransactionHistoryScreen(
                    viewModel = viewModel,
                    navController = navController,
                    onMenuClick = {}
                )
            }
        }

        // Wait for Room background query to emit and populate UI
        composeTestRule.waitUntil(5000) {
            composeTestRule.onAllNodesWithText("Reliance Smart Supermarket").fetchSemanticsNodes().isNotEmpty()
        }

        // Enter search text in search box for Reliance Smart Supermarket
        composeTestRule.onNodeWithText("Search transactions...").performTextInput("Smart")

        // Wait for list filter update
        composeTestRule.waitForIdle()

        // "Groceries" should be visible but "Salary" should be filtered out
        composeTestRule.onNodeWithText("Reliance Smart Supermarket").assertIsDisplayed()
        composeTestRule.onNodeWithText("Tech Corp Inc.").assertDoesNotExist()
    }

    @Test
    fun historyScreen_filterBottomSheetWorkflow() {
        composeTestRule.setContent {
            FinanceTrackerTheme {
                val navController = rememberNavController()
                TransactionHistoryScreen(
                    viewModel = viewModel,
                    navController = navController,
                    onMenuClick = {}
                )
            }
        }

        // Wait for Room background query to emit and populate UI
        composeTestRule.waitUntil(5000) {
            composeTestRule.onAllNodesWithText("Reliance Smart Supermarket").fetchSemanticsNodes().isNotEmpty()
        }

        // Click on the Filter Icon to open the Bottom Sheet
        composeTestRule.onNodeWithContentDescription("Filter").performClick()
        composeTestRule.waitForIdle()

        // Verify bottom sheet title is visible
        composeTestRule.onNodeWithText("Filter & Sort").assertIsDisplayed()

        // Click "Transaction Type" tab to view type filters
        composeTestRule.onNodeWithText("Transaction Type").performClick()
        composeTestRule.waitForIdle()

        // Choose "Expense Only" in the bottom sheet filter section
        composeTestRule.onNodeWithText("Expense Only").performClick()

        // Click "Apply Filters" button
        composeTestRule.onNodeWithText("Apply Filters").performClick()
        composeTestRule.waitForIdle()

        // Only the expense transaction ("Groceries") should be visible
        composeTestRule.onNodeWithText("Reliance Smart Supermarket").assertIsDisplayed()
        composeTestRule.onNodeWithText("Tech Corp Inc.").assertDoesNotExist()

        // Active filter chip should show up
        composeTestRule.onNodeWithText("Expense Only").assertIsDisplayed()

        // Clear filter using the active chip's close action
        composeTestRule.onNodeWithContentDescription("Clear Filter").performClick()
        composeTestRule.waitForIdle()

        // Both transactions should be visible again
        composeTestRule.onNodeWithText("Tech Corp Inc.").assertIsDisplayed()
        composeTestRule.onNodeWithText("Reliance Smart Supermarket").assertIsDisplayed()
    }

    @Test
    fun historyScreen_deletesTransaction() {
        runBlocking {
            composeTestRule.setContent {
                FinanceTrackerTheme {
                    val navController = rememberNavController()
                    TransactionHistoryScreen(
                        viewModel = viewModel,
                        navController = navController,
                        onMenuClick = {}
                    )
                }
            }

            // Wait for Room background query to emit and populate UI
            composeTestRule.waitUntil(5000) {
                composeTestRule.onAllNodesWithText("Reliance Smart Supermarket").fetchSemanticsNodes().isNotEmpty()
            }

            // Click on the Groceries transaction item to open Details bottom sheet
            composeTestRule.onNodeWithText("Reliance Smart Supermarket").performClick()
            composeTestRule.waitForIdle()

            // Click Delete button inside dropdown menu
            composeTestRule.onNodeWithText("Delete").performClick()
            composeTestRule.waitForIdle()

            // Wait for Room background query to emit the updated list excluding "Groceries"
            composeTestRule.waitUntil(5000) {
                composeTestRule.onAllNodesWithText("Reliance Smart Supermarket").fetchSemanticsNodes().isEmpty()
            }

            // Verify the Groceries is deleted from list
            composeTestRule.onNodeWithText("Reliance Smart Supermarket").assertDoesNotExist()

            // Salary should still be displayed
            composeTestRule.onNodeWithText("Tech Corp Inc.").assertIsDisplayed()
        }
    }

    @Test
    fun historyScreen_screenshot() {
        composeTestRule.setContent {
            FinanceTrackerTheme {
                val navController = rememberNavController()
                TransactionHistoryScreen(
                    viewModel = viewModel,
                    navController = navController,
                    onMenuClick = {}
                )
            }
        }

        composeTestRule.waitForIdle()

        // Capture screenshot of the full Transaction History Screen
        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/transaction_history_screen.png")
    }
}
