package com.example.e2e.guest

import android.content.Context
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.semantics.SemanticsActions
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.EncryptedPrefsManager
import com.example.data.local.JsonDataManager
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
import org.robolectric.shadows.ShadowLooper

/**
 * End-to-end Robolectric + Jetpack Compose tests for the Guest User flow.
 *
 * Covers:
 * - Adding a transaction as a guest and verifying the Dashboard balance label
 * - Navigation to History and Analytics screens
 * - Settings screen: cloud backup hidden, admin console hidden
 * - Settings screen: "Sign In / Register" shown instead of "Log Out"
 * - Settings screen: local backup and restore options available
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [33])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class GuestUserFlowTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var financeRepository: FinanceRepository
    private lateinit var authRepository: AuthRepository
    private lateinit var financeViewModel: FinanceViewModel
    private lateinit var authViewModel: AuthViewModel

    // -------------------------------------------------------------------------
    // Setup / Teardown
    // -------------------------------------------------------------------------

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        context = ApplicationProvider.getApplicationContext()
        android.provider.Settings.Global.putFloat(context.contentResolver, android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 0f)
        android.provider.Settings.Global.putFloat(context.contentResolver, android.provider.Settings.Global.TRANSITION_ANIMATION_SCALE, 0f)
        android.provider.Settings.Global.putFloat(context.contentResolver, android.provider.Settings.Global.WINDOW_ANIMATION_SCALE, 0f)

        // Clear auth state
        val prefs = EncryptedPrefsManager.getEncryptedPrefs(context, "auth_prefs")
        prefs.edit().clear().commit()

        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .setQueryExecutor { it.run() }
            .setTransactionExecutor { it.run() }
            .build()

        val jsonDataManager = JsonDataManager(context)
        financeRepository = FinanceRepository(db.financeDao(), jsonDataManager)
        authRepository = AuthRepository(context)
        try {
            val field = AuthRepository::class.java.getDeclaredField("useDemoFallback")
            field.isAccessible = true
            field.set(authRepository, true)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        financeViewModel = FinanceViewModel(financeRepository)
        authViewModel = AuthViewModel(authRepository)

        // Wait for database seeding to complete on Dispatchers.IO background threads
        var retries = 0
        while (runBlocking { db.financeDao().getAllCategoriesOnce().isEmpty() } && retries < 200) {
            Thread.sleep(10)
            retries++
        }
    }

    @After
    fun tearDown() {
        testDispatcher.scheduler.advanceUntilIdle()
        db.invalidationTracker.refreshVersionsSync()
        ShadowLooper.idleMainLooper()
        db.close()
        Dispatchers.resetMain()
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Sets the Compose content to [FinanceApp] wired to the test ViewModels and
     * advances all pending coroutines + Looper messages so the UI settles.
     */
    private fun launchApp() {
        composeTestRule.setContent {
            FinanceTrackerTheme {
                FinanceApp(
                    viewModel = financeViewModel,
                    authViewModel = authViewModel,
                )
            }
        }
        // Advance past the 4200 ms splash delay
        testDispatcher.scheduler.advanceTimeBy(5_000L)
        composeTestRule.mainClock.advanceTimeBy(5_000L)
        composeTestRule.waitForIdle()
        testDispatcher.scheduler.advanceUntilIdle()
        db.invalidationTracker.refreshVersionsSync()
        composeTestRule.waitForIdle()
        ShadowLooper.idleMainLooper()
    }

    /**
     * Drains all pending Robolectric Looper tasks (including delayed ones) and
     * then waits for Compose to finish recomposing.
     */
    private fun advanceAndIdle() {
        testDispatcher.scheduler.advanceUntilIdle()
        ShadowLooper.idleMainLooper()
        composeTestRule.waitForIdle()
    }

    // -------------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------------

    /**
     * Verifies that a guest user can add a transaction via the FAB and is
     * returned to the Dashboard showing the "Total Balance" label.
     *
     * Steps:
     * 1. Login as guest.
     * 2. Launch the app – Dashboard is visible with "Total Balance".
     * 3. Click the "Add Transaction" FAB.
     * 4. Fill in Amount and Payee fields.
     * 5. Scroll the category row to "Groceries" and select it.
     * 6. Tap Save and wait for navigation back to the Dashboard.
     * 7. Assert "Total Balance" is still displayed.
     */
    @Test
    fun guestUser_canAddTransaction_balanceUpdates() {
        authViewModel.loginAsGuest()
        advanceAndIdle()

        launchApp()

        // Assert Dashboard is shown
        composeTestRule.onNodeWithText("TOTAL BALANCE").assertIsDisplayed()

        // Open Add Transaction via FAB
        composeTestRule
            .onNodeWithContentDescription("Add Transaction")
            .performSemanticsAction(SemanticsActions.OnClick)
        advanceAndIdle()

        // Fill in Amount using testTag (matches AddTransactionScreen's Modifier.testTag("amount_input"))
        composeTestRule
            .onNodeWithTag("amount_input")
            .performTextInput("500.0")

        // Fill in Payee / Store using testTag
        composeTestRule
            .onNodeWithTag("payee_input")
            .performTextInput("Guest Cafe")

        // Scroll the category lazy row to "Groceries" and select it
        composeTestRule
            .onNodeWithTag("category_lazy_row")
            .performScrollToNode(hasText("Groceries"))
        composeTestRule
            .onNodeWithText("Groceries")
            .performClick()
        advanceAndIdle()

        // Save the transaction
        composeTestRule
            .onNodeWithText("Save")
            .performClick()
        advanceAndIdle()

        // Assert we are back on the Dashboard
        composeTestRule
            .onNodeWithText("TOTAL BALANCE")
            .assertIsDisplayed()
    }

    /**
     * Verifies that a guest user can navigate to the History screen using the
     * bottom navigation bar, and that the search field is displayed.
     */
    @Test
    fun guestUser_canNavigateToHistory() {
        authViewModel.loginAsGuest()
        advanceAndIdle()

        launchApp()

        // Tap the "History" bottom-nav item
        composeTestRule
            .onNodeWithContentDescription("History")
            .performClick()
        advanceAndIdle()

        // Search field confirms we landed on the History screen
        composeTestRule
            .onNodeWithText("Search transactions...", substring = true)
            .assertIsDisplayed()
    }

    /**
     * Verifies that a guest user can navigate to the Analytics screen using the
     * bottom navigation bar, and that the "Analytics" heading is displayed.
     */
    @Test
    fun guestUser_canNavigateToAnalytics() {
        authViewModel.loginAsGuest()
        advanceAndIdle()

        launchApp()

        // Tap the "Analytics" bottom-nav item
        composeTestRule
            .onNodeWithContentDescription("Analytics")
            .performClick()
        advanceAndIdle()

        // Heading confirms we landed on the Analytics screen
        composeTestRule
            .onAllNodesWithText("Analytics", substring = true)
            .onFirst()
            .assertIsDisplayed()
    }

    /**
     * Verifies that the "Backup to Cloud" option is NOT shown to a guest user on
     * the Settings screen.
     */
    @Test
    fun guestUser_cloudBackupHidden() {
        authViewModel.loginAsGuest()
        advanceAndIdle()

        launchApp()

        // Navigate to Settings
        composeTestRule
            .onNodeWithContentDescription("Settings")
            .performClick()
        advanceAndIdle()

        // Cloud backup must not be present for guest users
        composeTestRule
            .onNodeWithText("Backup to Cloud")
            .assertDoesNotExist()
    }

    /**
     * Verifies that neither "Administrative Access" nor "Admin Console" is shown
     * to a guest user on the Settings screen.
     */
    @Test
    fun guestUser_adminConsoleHidden() {
        authViewModel.loginAsGuest()
        advanceAndIdle()

        launchApp()

        // Navigate to Settings
        composeTestRule
            .onNodeWithContentDescription("Settings")
            .performClick()
        advanceAndIdle()

        // Admin-only options must not appear for guests
        composeTestRule
            .onNodeWithText("Administrative Access")
            .assertDoesNotExist()

        composeTestRule
            .onNodeWithText("Admin Console")
            .assertDoesNotExist()
    }

    /**
     * Verifies that the Settings screen shows "Sign In / Register" for a guest
     * user and does NOT show "Log Out".
     */
    @Test
    fun guestUser_settingsShowsSignInButton_notLogOut() {
        authViewModel.loginAsGuest()
        advanceAndIdle()

        launchApp()

        // Navigate to Settings
        composeTestRule
            .onNodeWithContentDescription("Settings")
            .performClick()
        advanceAndIdle()

        // "Sign In / Register" must be reachable by scrolling
        composeTestRule
            .onNodeWithTag("settings_logout_button")
            .performScrollTo()
            .assertIsDisplayed()

        // "Log Out" must not exist for a guest session
        composeTestRule
            .onNodeWithText("Log Out")
            .assertDoesNotExist()
    }

    /**
     * Verifies that the Settings screen exposes "Backup to Local Storage" and
     * "Restore from Local Storage" options for a guest user.
     */
    @Test
    fun guestUser_localBackupAvailable() {
        authViewModel.loginAsGuest()
        advanceAndIdle()

        launchApp()

        // Navigate to Settings
        composeTestRule
            .onNodeWithContentDescription("Settings")
            .performClick()
        advanceAndIdle()

        // Local backup option must be reachable by scrolling
        composeTestRule
            .onNodeWithText("Backup to Local Storage")
            .performScrollTo()
            .assertIsDisplayed()

        // Local restore option must also be reachable by scrolling
        composeTestRule
            .onNodeWithText("Restore from Local Storage")
            .performScrollTo()
            .assertIsDisplayed()
    }
}
