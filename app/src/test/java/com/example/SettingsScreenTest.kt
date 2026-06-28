package com.example

import android.content.Context
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.core.app.ActivityOptionsCompat
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.JsonDataManager
import com.example.data.model.Category
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.data.repository.AuthRepository
import com.example.data.repository.FinanceRepository
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.FinanceTrackerTheme
import com.example.ui.viewmodel.AuthViewModel
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
class SettingsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var db: AppDatabase
    private lateinit var repository: FinanceRepository
    private lateinit var viewModel: FinanceViewModel
    private lateinit var authRepository: AuthRepository
    private lateinit var authViewModel: AuthViewModel

    private val fakeRegistryOwner = object : ActivityResultRegistryOwner {
        override val activityResultRegistry = object : ActivityResultRegistry() {
            override fun <I, O> onLaunch(
                requestCode: Int,
                contract: ActivityResultContract<I, O>,
                input: I,
                options: ActivityOptionsCompat?
            ) {
                dispatchResult(requestCode, android.app.Activity.RESULT_OK, null)
            }
        }
    }

    @Before
    fun setup() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val jsonDataManager = JsonDataManager(context)
        repository = FinanceRepository(db.financeDao(), jsonDataManager)
        viewModel = FinanceViewModel(repository)

        authRepository = AuthRepository(context)
        authViewModel = AuthViewModel(authRepository)

        // Seed basic categories and transactions to allow backup content
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
    }

    @After
    fun tearDown() {
        // No explicit DB cleanup required for in-memory database to prevent test thread races
    }

    @Test
    fun settingsScreen_displaysOptionsAndDetails() {
        // Force guest mode user session for testing
        authViewModel.loginAsGuest()

        composeTestRule.setContent {
            FinanceTrackerTheme {
                CompositionLocalProvider(
                    LocalActivityResultRegistryOwner provides fakeRegistryOwner
                ) {
                    SettingsScreen(
                        viewModel = viewModel,
                        authViewModel = authViewModel,
                        onManageCategoriesClick = {},
                        onAdminConsoleClick = {},
                        onMenuClick = {}
                    )
                }
            }
        }

        composeTestRule.waitForIdle()

        // Assert Theme Section
        composeTestRule.onNodeWithText("Theme").assertIsDisplayed()
        composeTestRule.onNodeWithText("Light").assertIsDisplayed()
        composeTestRule.onNodeWithText("Dark").assertIsDisplayed()
        composeTestRule.onNodeWithText("System").assertIsDisplayed()

        // Assert Currency Section
        composeTestRule.onNodeWithText("Currency").assertIsDisplayed()
        composeTestRule.onNodeWithText("INR (₹)").assertIsDisplayed()
        composeTestRule.onNodeWithText("Future Options (Multiple currencies)").assertIsDisplayed()

        // Assert Profile Section
        composeTestRule.onNodeWithText("Profile").assertIsDisplayed()
        composeTestRule.onNodeWithText("Name").assertIsDisplayed()
        composeTestRule.onNodeWithText("Email").assertIsDisplayed()

        // Assert Notifications Section
        composeTestRule.onNodeWithText("Notifications").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Daily Expense Reminder").performScrollTo().assertIsDisplayed()

        // Assert that Preferences section is present (using performScrollTo as it's below the fold)
        composeTestRule.onNodeWithText("Preferences").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Manage Categories").performScrollTo().assertIsDisplayed()

        // Assert that Data Management section is present
        composeTestRule.onNodeWithText("Data Management").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Export to CSV").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Backup to Local Storage").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Restore from Local Storage").performScrollTo().assertIsDisplayed()

        // Assert that Cloud sync option is displayed as unavailable in Guest mode
        composeTestRule.onNodeWithText("Cloud Sync (Unavailable)").performScrollTo().assertIsDisplayed()

        // Assert App Info section is present
        composeTestRule.onNodeWithText("App Info").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("About").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Finance Tracker v1.1").performScrollTo().assertIsDisplayed()

        // Assert Log Out exists
        composeTestRule.onNodeWithText("Log Out").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun settingsScreen_localBackupAndRestoreFlow() {
        authViewModel.loginAsGuest()

        composeTestRule.setContent {
            FinanceTrackerTheme {
                CompositionLocalProvider(
                    LocalActivityResultRegistryOwner provides fakeRegistryOwner
                ) {
                    SettingsScreen(
                        viewModel = viewModel,
                        authViewModel = authViewModel,
                        onManageCategoriesClick = {},
                        onAdminConsoleClick = {},
                        onMenuClick = {}
                    )
                }
            }
        }

        composeTestRule.waitForIdle()

        // Click local backup
        composeTestRule.onNodeWithText("Backup to Local Storage").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        // Click local restore to show warning dialog
        composeTestRule.onNodeWithText("Restore from Local Storage").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        // Check warning dialog elements
        composeTestRule.onNodeWithText("Confirm Data Restore").assertIsDisplayed()
        composeTestRule.onNodeWithText("Restore").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cancel").assertIsDisplayed()

        // Click Cancel on warning dialog
        composeTestRule.onNodeWithText("Cancel").performClick()
        composeTestRule.waitForIdle()

        // Warning dialog should be gone
        composeTestRule.onNodeWithText("Confirm Data Restore").assertDoesNotExist()
    }

    @Test
    fun settingsScreen_toggleNotifications() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        
        // 1. Initially reminder is disabled
        assert(!viewModel.reminderEnabled.value)

        // 2. Set enabled directly via ViewModel
        viewModel.setReminderEnabled(true, context)
        assert(viewModel.reminderEnabled.value)

        // 3. Set disabled directly via ViewModel
        viewModel.setReminderEnabled(false, context)
        assert(!viewModel.reminderEnabled.value)
    }

    @Test
    fun settingsScreen_screenshot() {
        authViewModel.loginAsGuest()

        composeTestRule.setContent {
            FinanceTrackerTheme {
                CompositionLocalProvider(
                    LocalActivityResultRegistryOwner provides fakeRegistryOwner
                ) {
                    SettingsScreen(
                        viewModel = viewModel,
                        authViewModel = authViewModel,
                        onManageCategoriesClick = {},
                        onAdminConsoleClick = {},
                        onMenuClick = {}
                    )
                }
            }
        }

        composeTestRule.waitForIdle()

        // Capture screenshot of Settings Screen
        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/settings_screen.png")
    }
}
