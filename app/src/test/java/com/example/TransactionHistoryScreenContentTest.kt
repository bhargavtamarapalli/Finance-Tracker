package com.example

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.ui.screens.TransactionHistoryContent
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
class TransactionHistoryScreenContentTest {
    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun transactionHistoryContent_rendersHistoryList() {
        composeTestRule.setContent {
            FinanceTrackerTheme {
                TransactionHistoryContent(
                    allTransactions = emptyList(),
                    periodTransactions = emptyList(),
                    selectedTimePeriod = TimePeriod.MONTH,
                    periodLabel = "July 2026",
                    activeDate = System.currentTimeMillis(),
                    isNextPeriodEnabled = false,
                    categories = emptyList(),
                    onMenuClick = {},
                    onPeriodSelected = {},
                    onPreviousClick = {},
                    onNextClick = {},
                    onDateSelected = {},
                    onEditTransactionClick = {},
                    onDuplicateTransactionClick = {},
                    onDeleteTransactionClick = {}
                )
            }
        }
        composeTestRule.onNodeWithText("Transaction History", ignoreCase = true).assertIsDisplayed()
    }
}
