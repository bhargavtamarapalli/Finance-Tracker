package com.example

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.data.model.Category
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.data.model.TransactionWithCategory
import com.example.ui.screens.AnalyticsScreenContent
import com.example.ui.theme.FinanceTrackerTheme
import com.example.ui.viewmodel.TimePeriod
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w1000dp-h2000dp-xhdpi", sdk = [33])
class AnalyticsScreenContentTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun analyticsScreenContent_rendersDefaultEmptyState() {
        composeTestRule.setContent {
            FinanceTrackerTheme {
                AnalyticsScreenContent(
                    periodTransactions = emptyList(),
                    selectedTimePeriod = TimePeriod.MONTH,
                    periodLabel = "July 2026",
                    activeDate = System.currentTimeMillis(),
                    isNextPeriodEnabled = false,
                    isLoading = false,
                    onMenuClick = {},
                    setTimePeriod = {},
                    moveToPreviousPeriod = {},
                    moveToNextPeriod = {},
                    setDateDirectly = {}
                )
            }
        }

        // Verify "No analytics data" is displayed when transactions are empty
        composeTestRule.onNodeWithText("No analytics data").assertIsDisplayed()
    }

    @Test
    fun analyticsScreenContent_rendersTransactionsAndBreakdown() {
        val incomeCategory = Category(id = 7, name = "Salary", type = TransactionType.INCOME, iconName = "attach_money")
        val expenseCategory = Category(id = 2, name = "Groceries", type = TransactionType.EXPENSE, iconName = "shopping_cart")
        
        val transactions = listOf(
            TransactionWithCategory(
                transaction = TransactionEntity(
                    id = 1,
                    amount = 75000.0,
                    source = "Tech Corp Inc.",
                    date = System.currentTimeMillis(),
                    categoryId = 7,
                    type = TransactionType.INCOME,
                    notes = "Monthly payroll payout"
                ),
                category = incomeCategory
            ),
            TransactionWithCategory(
                transaction = TransactionEntity(
                    id = 2,
                    amount = 4200.0,
                    source = "Reliance Smart Supermarket",
                    date = System.currentTimeMillis() - 100,
                    categoryId = 2,
                    type = TransactionType.EXPENSE,
                    notes = "Weekly grocery refill"
                ),
                category = expenseCategory
            )
        )

        composeTestRule.setContent {
            FinanceTrackerTheme {
                AnalyticsScreenContent(
                    periodTransactions = transactions,
                    selectedTimePeriod = TimePeriod.MONTH,
                    periodLabel = "July 2026",
                    activeDate = System.currentTimeMillis(),
                    isNextPeriodEnabled = false,
                    isLoading = false,
                    onMenuClick = {},
                    setTimePeriod = {},
                    moveToPreviousPeriod = {},
                    moveToNextPeriod = {},
                    setDateDirectly = {}
                )
            }
        }

        // Verify title
        composeTestRule.onNodeWithText("Analytics").assertIsDisplayed()

        // By default, Expenses tab is active, so we should see Groceries but not Salary
        composeTestRule.onNodeWithText("Groceries").assertIsDisplayed()
        composeTestRule.onNodeWithText("Salary").assertDoesNotExist()

        // Center card shows "All Categories" by default
        composeTestRule.onNodeWithTag("donut_center_title", useUnmergedTree = true).assertTextEquals("All Categories")
    }
}
