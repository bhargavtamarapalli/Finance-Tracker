package com.example

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.ui.screens.SettingsContent
import com.example.ui.theme.FinanceTrackerTheme
import com.example.ui.viewmodel.AppTheme
import com.example.ui.utils.CurrencyOption
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w1000dp-h2000dp-xhdpi", sdk = [33])
class SettingsScreenContentTest {
    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun settingsContent_rendersPreferenceSections() {
        composeTestRule.setContent {
            FinanceTrackerTheme {
                SettingsContent(
                    userSession = null,
                    reminderEnabled = true,
                    biometricLockEnabled = false,
                    appTheme = AppTheme.SYSTEM,
                    currencyOption = CurrencyOption.INR,
                    isSyncing = false,
                    monthlyBudgetGoal = 100000.0,
                    onBudgetGoalChange = {},
                    onReminderToggle = {},
                    onBiometricToggle = {},
                    onThemeChange = {},
                    onCurrencyChange = {},
                    onManageCategoriesClick = {},
                    onAdminConsoleClick = {},
                    onMenuClick = {},
                    onExportCsv = {},
                    onBackupCloud = {},
                    onBackupLocal = {},
                    onRestoreCloud = {},
                    onRestoreLocal = {},
                    onUpdateProfile = { _, _, _, _ -> },
                    onSignOut = {}
                )
            }
        }
        composeTestRule.onNodeWithText("Daily Expense Reminder", ignoreCase = true).assertIsDisplayed()
    }
}
