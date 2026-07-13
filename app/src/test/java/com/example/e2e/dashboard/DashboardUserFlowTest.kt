package com.example.e2e.dashboard

import android.content.Context
import androidx.compose.ui.test.*
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.junit4.createComposeRule
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
import org.robolectric.shadows.ShadowLooper

/**
 * End-to-end Robolectric + Jetpack Compose tests for the Dashboard screen.
 *
 * Covers:
 * - Correct balance display after sign-up with seeded demo data
 * - User first name badge rendering
 * - Budget progress section visibility when a budget goal is set
 * - Bottom-nav navigation to History and Analytics destinations
 * - Guest user dashboard access
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [33],
    qualifiers = RobolectricDeviceQualifiers.MediumPhone,
)
class DashboardUserFlowTest {

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
    // Helper
    // -------------------------------------------------------------------------

    private fun bypassSplash() {
        testDispatcher.scheduler.advanceTimeBy(3000L)
        composeTestRule.mainClock.advanceTimeBy(3000L)
        composeTestRule.waitForIdle()
        testDispatcher.scheduler.advanceUntilIdle()
        db.invalidationTracker.refreshVersionsSync()
        composeTestRule.waitForIdle()
        ShadowLooper.idleMainLooper()
    }

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
        bypassSplash()
    }

    /**
     * Drains all pending coroutines dispatched on [testDispatcher] and then
     * advances the main Looper so Compose can recompose.
     */
    private fun advanceAndIdle() {
        composeTestRule.waitForIdle()
        testDispatcher.scheduler.advanceUntilIdle()
        db.invalidationTracker.refreshVersionsSync()
        ShadowLooper.idleMainLooper(1000, java.util.concurrent.TimeUnit.MILLISECONDS)
        composeTestRule.waitForIdle()
    }

    // -------------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------------

    /**
     * Verifies that after signing up a new user the Dashboard displays the
     * correct seeded balance ("₹58,600.00") and the "Total Balance" label.
     */
    @Test
    fun dashboard_displaysCorrectBalance_withSeededData() {
        // Sign up seeds demo data: Income ₹92,000 | Expenses ₹33,400 | Balance ₹58,600
        authViewModel.signUp("dash@example.com", "password123", "Dash User")
        advanceAndIdle()

        launchApp()

        composeTestRule
            .onNodeWithText("TOTAL BALANCE")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("₹58,600.00")
            .assertIsDisplayed()

        // User first name should appear as a greeting / badge
        composeTestRule
            .onNodeWithText("Dash")
            .assertIsDisplayed()
    }

    /**
     * Verifies that the Dashboard shows only the first name from the full name
     * supplied during sign-up ("Bhargav" from "Bhargav Tamarapalli").
     */
    @Test
    fun dashboard_displaysUserFirstNameBadge() {
        authViewModel.signUp(
            email = "bhargav@example.com",
            password = "password123",
            name = "Bhargav Tamarapalli",
        )
        advanceAndIdle()

        launchApp()

        composeTestRule
            .onNodeWithText("Bhargav")
            .assertIsDisplayed()
    }

    /**
     * Verifies that when a monthly budget goal is set the "Left" section
     * is visible on the Dashboard.
     */
    @Test
    fun dashboard_withBudgetGoal_displaysBudgetProgress() {
        authViewModel.signUp("budget@example.com", "password123", "Budget User")
        advanceAndIdle()

        financeViewModel.updateMonthlyBudgetGoal(100_000.0)
        advanceAndIdle()

        launchApp()

        // "Left" (budget left) may be below the fold on smaller screens; scroll to it.
        composeTestRule
            .onNodeWithText("Left", substring = true)
            .performScrollTo()
            .assertIsDisplayed()
    }

    /**
     * Verifies that tapping the "History" bottom-navigation item navigates to
     * the History screen, which surfaces a "Search transactions…" search bar.
     */
    @Test
    fun dashboard_navigatesToHistory_viaBottomNav() {
        authViewModel.signUp("history@example.com", "password123", "History User")
        advanceAndIdle()

        launchApp()

        composeTestRule
            .onNodeWithContentDescription("History")
            .performClick()

        advanceAndIdle()

        composeTestRule
            .onNodeWithText("Search transactions...", substring = true)
            .assertIsDisplayed()
    }

    /**
     * Verifies that tapping the "Analytics" bottom-navigation item navigates to
     * the Analytics screen, which displays an "Analytics" heading.
     */
    @Test
    fun dashboard_navigatesToAnalytics_viaBottomNav() {
        authViewModel.signUp("analytics@example.com", "password123", "Analytics User")
        advanceAndIdle()

        launchApp()

        composeTestRule
            .onNodeWithContentDescription("Analytics")
            .performClick()

        advanceAndIdle()

        composeTestRule
            .onNode(hasText("Analytics").and(hasNoClickAction()))
            .assertIsDisplayed()
    }

    /**
     * Verifies that a guest user can log in and view the Dashboard with the
     * "TOTAL BALANCE" label displayed.
     */
    @Test
    fun dashboard_guestUser_canViewDashboard() {
        authViewModel.loginAsGuest()
        advanceAndIdle()

        launchApp()

        composeTestRule
            .onNodeWithText("TOTAL BALANCE")
            .assertIsDisplayed()
    }

    // ──────────────────────────────────────────────────────────────────────────────
    // UC-DASH-06: Budget exceeded — negative Budget Left value is shown
    // ──────────────────────────────────────────────────────────────────────────────

    /**
     * UC-DASH-06 — When expenses exceed the monthly budget goal, the "Left"
     * row displays a negative formatted amount.
     *
     * Given: Seeded expenses are ₹33,400 (total).
     * When:  Monthly budget goal is set to ₹10,000 (well below expenses).
     * Then:  Dashboard shows "Left" with a value that starts with "-₹"
     *        (i.e. the text is negative), confirming the over-budget state is rendered.
     *
     * Note: Color (ExpenseRed) cannot be asserted in Robolectric; we assert the
     *       text value instead, which is the primary behavioural contract.
     */
    @Test
    fun dashboard_budgetExceeded_showsNegativeBudgetLeft() {
        authViewModel.signUp("budget@example.com", "Password123", "Budget User")
        testDispatcher.scheduler.advanceUntilIdle()
        db.invalidationTracker.refreshVersionsSync()
        ShadowLooper.idleMainLooper(1000, java.util.concurrent.TimeUnit.MILLISECONDS)

        // Set goal to ₹10,000 — seeded expenses are ₹33,400 so this will be exceeded
        financeViewModel.updateMonthlyBudgetGoal(10000.0)
        testDispatcher.scheduler.advanceUntilIdle()

        launchApp()

        // The "Left" label must be visible
        composeTestRule.onNodeWithText("Left", substring = true).assertIsDisplayed()

        // The adjacent amount text must contain the correct formatted negative value
        composeTestRule.onNodeWithText(com.example.ui.utils.CurrencyUtils.formatRupees(-23400.0)).assertIsDisplayed()
    }
}

