package com.example.e2e.settings

import android.content.Context
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.EncryptedPrefsManager
import com.example.data.local.JsonDataManager
import com.example.data.model.Category
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.data.repository.AuthRepository
import com.example.data.repository.FinanceRepository
import com.example.ui.FinanceApp
import com.example.ui.theme.FinanceTrackerTheme
import com.example.ui.viewmodel.AuthViewModel
import com.example.ui.viewmodel.FinanceViewModel
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
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

/**
 * End-to-end Robolectric + Jetpack Compose tests for Settings screen user flows.
 *
 * Covers:
 * - Logout flow returning to auth screen
 * - Profile section displaying the signed-in user's name
 * - Notification toggle state change
 * - Currency change reflecting on the dashboard
 * - Manage Categories navigation
 * - Data & Backup section visibility for non-guest users
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w1000dp-h2000dp-xhdpi", sdk = [33])
class SettingsExtendedUserFlowTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var repository: FinanceRepository
    private lateinit var viewModel: FinanceViewModel
    private lateinit var authRepository: AuthRepository
    private lateinit var authViewModel: AuthViewModel

    // ---------------------------------------------------------------------------
    // Setup / Teardown
    // ---------------------------------------------------------------------------

    @Before
    fun setUp() = runBlocking {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        context = ApplicationProvider.getApplicationContext()

        // Clear any persisted auth / settings state between tests
        EncryptedPrefsManager.getEncryptedPrefs(context, "auth_prefs").edit().clear().commit()
        EncryptedPrefsManager.getEncryptedPrefs(context, "settings_prefs").edit().clear().commit()

        val directExecutor = java.util.concurrent.Executor { it.run() }
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .setQueryExecutor(directExecutor)
            .setTransactionExecutor(directExecutor)
            .build()

        val jsonDataManager = JsonDataManager(context)
        repository = FinanceRepository(db.financeDao(), jsonDataManager)

        // Seed categories before ViewModel initialises to avoid background seeding races
        val incomeCat = Category(
            id = 7,
            name = "Salary",
            type = TransactionType.INCOME,
            iconName = "attach_money"
        )
        val expenseCat = Category(
            id = 2,
            name = "Groceries",
            type = TransactionType.EXPENSE,
            iconName = "shopping_cart"
        )
        db.financeDao().insertCategories(listOf(incomeCat, expenseCat))

        // Seed transactions that produce the expected dashboard totals used by currency test
        db.financeDao().insertTransaction(
            TransactionEntity(
                id = 1,
                amount = 75000.0,
                source = "Tech Corp Inc.",
                date = System.currentTimeMillis() - 2000,
                categoryId = 7,
                type = TransactionType.INCOME,
                notes = "Monthly payroll payout"
            )
        )
        db.financeDao().insertTransaction(
            TransactionEntity(
                id = 2,
                amount = 16400.0,
                source = "Reliance Smart Supermarket",
                date = System.currentTimeMillis() - 1000,
                categoryId = 2,
                type = TransactionType.EXPENSE,
                notes = "Weekly grocery refill"
            )
        )

        viewModel = FinanceViewModel(repository)
        authRepository = AuthRepository(context)
        authViewModel = AuthViewModel(authRepository)
    }

    @After
    fun tearDown() {
        db.close()
        Dispatchers.resetMain()
    }

    // ---------------------------------------------------------------------------
    // Helper: sign up a local demo user, wait for session to propagate
    // ---------------------------------------------------------------------------

    /**
     * Signs up a local demo user via [AuthRepository] directly so the session is
     * immediately available before the Compose content is set (avoids async races).
     */
    private fun signUpUser(
        name: String = "Test User",
        email: String = "test@example.com",
        password: String = "password123"
    ) = runBlocking {
        authRepository.signUpWithEmail(email, password, name)
    }

    private fun launchApp() {
        composeTestRule.setContent {
            FinanceTrackerTheme {
                FinanceApp(viewModel = viewModel, authViewModel = authViewModel)
            }
        }
        composeTestRule.waitForIdle()
    }

    private fun advanceAndIdle() {
        composeTestRule.waitForIdle()
    }

    /** Clicks the "Settings" bottom-nav item and waits for idle. */
    private fun navigateToSettings() {
        composeTestRule.onNodeWithContentDescription("Settings").performClick()
        composeTestRule.waitForIdle()
    }

    /** Clicks the "Home" bottom-nav item and waits for idle. */
    private fun navigateToHome() {
        composeTestRule.onNodeWithContentDescription("Home").performClick()
        composeTestRule.waitForIdle()
    }

    // ---------------------------------------------------------------------------
    // Test 1 – Log Out returns to Auth screen
    // ---------------------------------------------------------------------------

    /**
     * Verifies that a signed-in user who clicks "Log Out" in Settings is
     * redirected back to the authentication screen (identified by [guest_login_button]).
     */
    @Test
    fun settings_normalUser_logOut_returnsToAuthScreen() {
        signUpUser()

        composeTestRule.setContent {
            FinanceTrackerTheme {
                FinanceApp(viewModel = viewModel, authViewModel = authViewModel)
            }
        }

        // Wait for the app to finish loading and show the main navigation
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule
                .onAllNodesWithContentDescription("Settings")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        navigateToSettings()

        // Scroll to and click the "Log Out" button
        composeTestRule.onNodeWithTag("settings_logout_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        // After logout the AuthScreen should be visible; wait for the guest login button
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule
                .onAllNodesWithTag("guest_login_button")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
    }

    // ---------------------------------------------------------------------------
    // Test 2 – Profile section shows the signed-in user's name
    // ---------------------------------------------------------------------------

    /**
     * Verifies that the Settings profile header displays the name used during
     * registration ("Profile User").
     */
    @Test
    fun settings_normalUser_profileSectionShowsName() {
        signUpUser(name = "Profile User")

        composeTestRule.setContent {
            FinanceTrackerTheme {
                FinanceApp(viewModel = viewModel, authViewModel = authViewModel)
            }
        }

        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule
                .onAllNodesWithContentDescription("Settings")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        navigateToSettings()

        // The profile row renders userSession?.name at the top of SettingsContent
        composeTestRule
            .onAllNodesWithText("Profile User")
            .onFirst()
            .assertIsDisplayed()
    }

    // ---------------------------------------------------------------------------
    // Test 3 – Notification toggle changes state (no crash assertion)
    // ---------------------------------------------------------------------------

    /**
     * Verifies that toggling the Notifications switch in Settings does not crash
     * the app and that the "Notifications" label remains visible afterwards.
     */
    @Test
    fun settings_notificationToggle_changesState() {
        signUpUser()

        composeTestRule.setContent {
            FinanceTrackerTheme {
                FinanceApp(viewModel = viewModel, authViewModel = authViewModel)
            }
        }

        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule
                .onAllNodesWithContentDescription("Settings")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        navigateToSettings()

        // Scroll the "Notifications" item into view then click the row so the
        // Switch's onCheckedChange fires (the Switch itself may not be easily
        // click-targeted without a testTag, but clicking the surrounding row works).
        composeTestRule
            .onNodeWithText("Notifications")
            .performScrollTo()
            .assertIsDisplayed()

        // Find the Switch adjacent to "Notifications" and click it
        composeTestRule
            .onNodeWithTag("settings_reminder_switch")
            .performScrollTo()
            .performClick()

        composeTestRule.waitForIdle()

        // Stability assertion – the label must still be present (no crash)
        composeTestRule
            .onNodeWithText("Notifications")
            .performScrollTo()
            .assertIsDisplayed()
    }

    // ---------------------------------------------------------------------------
    // Test 4 – Currency change reflects on the dashboard
    // ---------------------------------------------------------------------------

    /**
     * Changes the currency to USD via the Settings dialog, navigates back to the
     * dashboard, and asserts that the balance is now formatted with a "$" prefix.
     *
     * Seeded data: ₹75,000 income − ₹16,400 expense = ₹58,600 net balance.
     * When currency is USD ($) the dashboard must display "$58,600.00".
     */
    @Test
    fun settings_currencyChange_reflectsOnDashboard() {
        signUpUser()

        composeTestRule.setContent {
            FinanceTrackerTheme {
                FinanceApp(viewModel = viewModel, authViewModel = authViewModel)
            }
        }

        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule
                .onAllNodesWithContentDescription("Settings")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        navigateToSettings()

        // Open the Currency dialog
        composeTestRule
            .onNodeWithText("Currency")
            .performScrollTo()
            .performClick()

        composeTestRule.waitForIdle()

        // Assert the dialog appeared (it contains the title "Select Currency")
        composeTestRule
            .onNodeWithText("Select Currency")
            .assertIsDisplayed()

        // Click the USD option (label = "USD ($)")
        composeTestRule
            .onNodeWithText("USD ($)")
            .performClick()

        composeTestRule.waitForIdle()

        // Navigate back to the dashboard
        navigateToHome()

        // Assert balance is now rendered in USD
        composeTestRule
            .onNodeWithText("$58,600.00")
            .assertIsDisplayed()
    }

    // ---------------------------------------------------------------------------
    // Test 5 – Manage Categories navigates to Category Management screen
    // ---------------------------------------------------------------------------

    /**
     * Clicks "Manage Categories" in Settings and verifies that the Category
     * Management screen is pushed onto the back stack (detected via the
     * "Add Category" FAB content description).
     */
    @Test
    fun settings_manageCategories_navigatesToCategoryScreen() {
        signUpUser()

        composeTestRule.setContent {
            FinanceTrackerTheme {
                FinanceApp(viewModel = viewModel, authViewModel = authViewModel)
            }
        }

        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule
                .onAllNodesWithContentDescription("Settings")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        navigateToSettings()

        // Scroll to and tap "Manage Categories"
        composeTestRule
            .onNodeWithText("Manage Categories")
            .performScrollTo()
            .performClick()

        composeTestRule.waitForIdle()

        // The Category Management screen exposes an FAB with this content description
        composeTestRule
            .onNodeWithContentDescription("Add Category")
            .assertExists()
    }

    // ---------------------------------------------------------------------------
    // Test 6 – Data & Backup section visible for non-guest users
    // ---------------------------------------------------------------------------

    /**
     * Verifies that the Data & Backup section – including cloud backup and
     * cloud restore items only available to registered users – is visible in
     * Settings for a non-guest account.
     */
    @Test
    fun settings_dataAndBackup_sectionVisible() {
        signUpUser()

        composeTestRule.setContent {
            FinanceTrackerTheme {
                FinanceApp(viewModel = viewModel, authViewModel = authViewModel)
            }
        }

        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule
                .onAllNodesWithContentDescription("Settings")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        navigateToSettings()

        // Assert section header
        composeTestRule
            .onNodeWithText("Data & Backup")
            .performScrollTo()
            .assertIsDisplayed()

        // Assert local backup item (always visible)
        composeTestRule
            .onNodeWithText("Backup to Local Storage")
            .performScrollTo()
            .assertIsDisplayed()

        // Assert cloud backup item (only visible for non-guest users per SettingsContent L496)
        composeTestRule
            .onNodeWithText("Backup to Cloud")
            .performScrollTo()
            .assertIsDisplayed()
    }

    // ──────────────────────────────────────────────────────────────────────────────
    // UC-SETTINGS-07: Profile name update via Edit Profile dialog
    // ──────────────────────────────────────────────────────────────────────────────

    /**
     * UC-SETTINGS-07 — Editing the profile name updates the displayed name in Settings.
     *
     * Given: Normal user signed up as "Original Name".
     * When:  User taps "Edit" button in the profile row → "Edit Profile" dialog appears
     *        → clears Name field → types "Updated Name" → taps Save.
     * Then:  Settings profile row shows "Updated Name".
     *
     * UI path: Settings → profile row → FinanceButton("Edit") → AlertDialog
     *          → OutlinedTextField(label="Name") → TextButton("Save")
     */
    @Test
    fun settings_profileEdit_updatesDisplayedName() {
        signUpUser("Original Name", "profileedit@example.com", "Password123")
        launchApp()

        navigateToSettings()

        // The profile section shows "Original Name" at the top — confirm it
        composeTestRule.onAllNodesWithText("Original Name").onFirst().assertIsDisplayed()

        // Tap the "Edit" button next to the profile row (only visible for non-guest users)
        composeTestRule.onNodeWithText("Edit").performClick()
        advanceAndIdle()

        // "Edit Profile" dialog should appear
        composeTestRule.onNodeWithText("Edit Profile").assertIsDisplayed()

        // Clear the Name field and type the new name
        composeTestRule.onNodeWithTag("profile_name_input")
            .performTextClearance()
        composeTestRule.onNodeWithTag("profile_name_input")
            .performTextInput("Updated Name")
        advanceAndIdle()

        // Confirm with Save
        composeTestRule.onNodeWithText("Save").performClick()
        advanceAndIdle()

        // The profile section should now reflect the updated name
        composeTestRule.onAllNodesWithText("Updated Name").onFirst().assertIsDisplayed()
    }
}

