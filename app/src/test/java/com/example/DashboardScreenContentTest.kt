package com.example

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.ui.screens.DashboardContent
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
class DashboardScreenContentTest {
    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun dashboardContent_rendersCards() {
        composeTestRule.setContent {
            FinanceTrackerTheme {
                DashboardContent(
                    periodTransactions = emptyList(),
                    allTransactions = emptyList(),
                    monthlyBudgetGoal = 100000.0,
                    userSession = null,
                    onMenuClick = {},
                    onAddTransactionClick = {},
                    onViewAllTransactionsClick = {},
                    onEditTransactionClick = {},
                    onDuplicateTransactionClick = {},
                    onDeleteTransaction = {}
                )
            }
        }
        // Verify "Total Balance" summary card title is displayed
        composeTestRule.onNodeWithText("Total Balance", ignoreCase = true).assertIsDisplayed()
    }
}
