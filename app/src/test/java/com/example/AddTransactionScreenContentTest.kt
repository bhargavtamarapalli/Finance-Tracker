package com.example

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.ui.screens.AddTransactionContent
import com.example.ui.theme.FinanceTrackerTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w1000dp-h2000dp-xhdpi", sdk = [33])
class AddTransactionScreenContentTest {
    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun addTransactionContent_rendersSaveButton() {
        composeTestRule.setContent {
            FinanceTrackerTheme {
                AddTransactionContent(
                    categories = emptyList(),
                    initialType = "EXPENSE",
                    onDismiss = {},
                    onSave = { _, _, _, _, _, _, _ -> }
                )
            }
        }
        composeTestRule.onNodeWithText("Save").assertIsDisplayed()
    }
}
