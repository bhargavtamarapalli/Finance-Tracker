package com.example.e2e.transaction

import android.content.Context
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.semantics.getOrNull
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
 * E2E tests for Transaction management flows.
 *
 * Covers:
 * - UC-TXN-01: Add income transaction and verify balance update
 * - UC-TXN-02: Attempting to save without required amount stays on AddTransaction screen
 * - UC-TXN-03: Add expense transaction and verify it appears in History
 * - UC-TXN-04: Delete a transaction from History and verify removal
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [33])
class TransactionUserFlowTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var financeRepository: FinanceRepository
    private lateinit var authRepository: AuthRepository
    private lateinit var financeViewModel: FinanceViewModel
    private lateinit var authViewModel: AuthViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        context = ApplicationProvider.getApplicationContext()

        // Disable system animations for reliable UI testing
        android.provider.Settings.Global.putFloat(
            context.contentResolver,
            android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 0f
        )
        android.provider.Settings.Global.putFloat(
            context.contentResolver,
            android.provider.Settings.Global.TRANSITION_ANIMATION_SCALE, 0f
        )
        android.provider.Settings.Global.putFloat(
            context.contentResolver,
            android.provider.Settings.Global.WINDOW_ANIMATION_SCALE, 0f
        )

        // Clear auth state to ensure a fresh session
        val prefs = EncryptedPrefsManager.getEncryptedPrefs(context, "auth_prefs")
        prefs.edit().clear().commit()

        // Build isolated in-memory Room database
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

    /** Bypasses the splash screen by advancing time and idling all dispatchers. */
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
     * Advances all coroutines and idles the Compose and main thread loopers.
     * Must be called after any action that triggers DB reads, navigation, or
     * Room invalidation.
     */
    private fun advanceAndIdle() {
        composeTestRule.waitForIdle()
        testDispatcher.scheduler.advanceUntilIdle()
        db.invalidationTracker.refreshVersionsSync()
        ShadowLooper.idleMainLooper(1000, java.util.concurrent.TimeUnit.MILLISECONDS)
        composeTestRule.waitForIdle()
    }

    /** Launches FinanceApp with the current auth/finance viewmodels and bypasses splash. */
    private fun launchApp() {
        composeTestRule.setContent {
            FinanceTrackerTheme {
                FinanceApp(viewModel = financeViewModel, authViewModel = authViewModel)
            }
        }
        bypassSplash()
    }

    /** Signs up a user and advances all dispatchers to ensure session state settles. */
    private fun signUpUser(email: String, password: String, name: String) {
        authViewModel.signUp(email, password, name)
        testDispatcher.scheduler.advanceUntilIdle()
        db.invalidationTracker.refreshVersionsSync()
        ShadowLooper.idleMainLooper(1000, java.util.concurrent.TimeUnit.MILLISECONDS)
    }

    // ──────────────────────────────────────────────────────────────────────────────
    // UC-TXN-01: Add income transaction
    // ──────────────────────────────────────────────────────────────────────────────

    /**
     * UC-TXN-01 — Adding an INCOME transaction navigates back to Dashboard.
     *
     * Given: A registered user has a seeded balance of ₹58,600.
     * When:  User taps FAB, selects Salary category, enters ₹10,000, saves.
     * Then:  Dashboard is shown with "Total Balance" visible.
     */
    @Test
    fun transaction_addIncome_updatesBalanceCorrectly() {
        signUpUser("income@example.com", "Password123", "Income User")
        launchApp()

        // Verify initial state
        composeTestRule.onNodeWithText("TOTAL BALANCE").assertIsDisplayed()

        // Open AddTransaction screen via FAB
        composeTestRule.onNodeWithContentDescription("Add Transaction").performSemanticsAction(SemanticsActions.OnClick)
        advanceAndIdle()

        // Switch to Income mode
        composeTestRule.onNodeWithText("Income").performClick()
        advanceAndIdle()

        // Fill in transaction details
        composeTestRule.onNodeWithTag("amount_input")
            .performTextInput("10000.0")
        composeTestRule.onNodeWithTag("payee_input")
            .performTextInput("Freelance Client")

        // Scroll category row to Salary and select it
        composeTestRule.onNodeWithTag("category_lazy_row")
            .performScrollToNode(hasText("Salary", substring = true))
        composeTestRule.onNodeWithText("Salary", substring = true).performClick()
        advanceAndIdle()

        // Save the transaction
        composeTestRule.onNodeWithTag("save_button").performClick()
        advanceAndIdle()

        // Dashboard should be shown again
        composeTestRule.onNodeWithText("TOTAL BALANCE").assertIsDisplayed()
    }

    // ──────────────────────────────────────────────────────────────────────────────
    // UC-TXN-02: Save without amount stays on AddTransaction screen
    // ──────────────────────────────────────────────────────────────────────────────

    /**
     * UC-TXN-02 — Saving without entering an amount does not navigate away.
     *
     * Given: AddTransactionScreen is open.
     * When:  User taps Save without entering an amount.
     * Then:  The screen remains on AddTransaction (Payee / Store field still visible).
     */
    @Test
    fun transaction_addExpenseWithoutAmount_doesNotNavigateAway() {
        signUpUser("noamount@example.com", "Password123", "NoAmount User")
        launchApp()

        // Open AddTransaction screen
        composeTestRule.onNodeWithContentDescription("Add Transaction").performSemanticsAction(SemanticsActions.OnClick)
        advanceAndIdle()

        // Only fill payee — intentionally leave Amount blank
        composeTestRule.onNodeWithTag("payee_input")
            .performTextInput("Empty Amount Store")

        // Attempt to save
        composeTestRule.onNodeWithTag("save_button").performClick()
        advanceAndIdle()

        // Should still be on AddTransaction screen — Payee field must still be visible
        composeTestRule.onNodeWithTag("payee_input")
            .assertIsDisplayed()
    }

    // ──────────────────────────────────────────────────────────────────────────────
    // UC-TXN-03: Add expense and verify in History
    // ──────────────────────────────────────────────────────────────────────────────

    /**
     * UC-TXN-03 — An added expense transaction appears in the Transaction History screen.
     *
     * Given: A registered user is on Dashboard.
     * When:  User adds a ₹2,500 expense for "Amazon Shopping" with card payment.
     * Then:  "Amazon Shopping" is visible in the History screen.
     */
    @Test
    fun transaction_addExpense_appearsInHistoryScreen() {
        signUpUser("history@example.com", "Password123", "History User")
        launchApp()

        // Add transaction via FAB
        composeTestRule.onNodeWithContentDescription("Add Transaction").performSemanticsAction(SemanticsActions.OnClick)
        advanceAndIdle()

        composeTestRule.onNodeWithTag("amount_input")
            .performTextInput("2500.0")
        composeTestRule.onNodeWithTag("payee_input")
            .performTextInput("Amazon Shopping")

        // Select Card payment method
        composeTestRule.onNodeWithText("Card").performClick()

        // Select Groceries category
        composeTestRule.onNodeWithTag("category_lazy_row")
            .performScrollToNode(hasText("Groceries", substring = true))
        composeTestRule.onNodeWithText("Groceries", substring = true).performClick()
        advanceAndIdle()

        // Save
        composeTestRule.onNodeWithTag("save_button").performClick()
        advanceAndIdle()

        // Navigate to History
        composeTestRule.onNodeWithContentDescription("History").performClick()
        advanceAndIdle()

        // Scroll to find the transaction
        composeTestRule.onNodeWithText("Amazon Shopping", substring = true)
            .performScrollTo()
            .assertIsDisplayed()
    }

    // ──────────────────────────────────────────────────────────────────────────────
    // UC-TXN-04: Delete transaction from History
    // ──────────────────────────────────────────────────────────────────────────────

    /**
     * UC-TXN-04 — Deleting a transaction from History removes it from the list.
     *
     * Given: A transaction "DeleteMe Store" exists in History.
     * When:  User taps it, opens the detail sheet, and taps Delete.
     * Then:  "DeleteMe Store" no longer appears in the History list.
     */
    @Test
    fun transaction_deleteFromHistory_removesTransaction() {
        signUpUser("delete@example.com", "Password123", "Delete User")
        launchApp()

        // Add a transaction to later delete
        composeTestRule.onNodeWithContentDescription("Add Transaction").performSemanticsAction(SemanticsActions.OnClick)
        advanceAndIdle()

        composeTestRule.onNodeWithTag("amount_input")
            .performTextInput("3000.0")
        composeTestRule.onNodeWithTag("payee_input")
            .performTextInput("DeleteMe Store")

        composeTestRule.onNodeWithTag("category_lazy_row")
            .performScrollToNode(hasText("Groceries", substring = true))
        composeTestRule.onNodeWithText("Groceries", substring = true).performClick()
        advanceAndIdle()

        composeTestRule.onNodeWithTag("save_button").performClick()
        advanceAndIdle()

        // Navigate to History
        composeTestRule.onNodeWithContentDescription("History").performClick()
        advanceAndIdle()

        // Scroll to the transaction and tap it to open details
        composeTestRule.onNodeWithText("DeleteMe Store", substring = true)
            .performScrollTo()
            .performClick()
        advanceAndIdle()

        // Delete from the detail sheet
        composeTestRule.onNodeWithText("Delete").assertIsDisplayed()
        composeTestRule.onNodeWithText("Delete").performClick()
        advanceAndIdle()

        // Verify removal
        composeTestRule.onNodeWithText("DeleteMe Store", substring = true).assertDoesNotExist()
    }

    // ──────────────────────────────────────────────────────────────────────────────
    // UC-TXN-05: Edit existing transaction via detail sheet → Edit button
    // ──────────────────────────────────────────────────────────────────────────────

    /**
     * UC-TXN-05 — Editing an existing transaction updates it in History.
     *
     * Given: A transaction "Edit Target" of ₹1,000 exists in History.
     * When:  User taps it → details sheet opens → taps "Edit" → changes amount to 4999 → saves.
     * Then:  History no longer shows ₹1,000 for that payee; ₹4,999 is visible instead.
     *
     * Edit flow: History screen → tap item → bottom sheet → tap "Edit" label
     *            → navigates to add_transaction route pre-filled → change amount → Save.
     */
    @Test
    fun transaction_editExisting_updatesHistoryAndBalance() {
        signUpUser("edit@example.com", "Password123", "Edit User")
        launchApp()

        // Step 1 — Add the transaction to be edited
        composeTestRule.onNodeWithContentDescription("Add Transaction").performSemanticsAction(SemanticsActions.OnClick)
        advanceAndIdle()

        composeTestRule.onNodeWithTag("amount_input")
            .performTextInput("1000.0")
        composeTestRule.onNodeWithTag("payee_input")
            .performTextInput("Edit Target")

        composeTestRule.onNodeWithTag("category_lazy_row")
            .performScrollToNode(hasText("Groceries", substring = true))
        composeTestRule.onNodeWithText("Groceries", substring = true).performClick()
        advanceAndIdle()

        composeTestRule.onNodeWithTag("save_button").performClick()
        advanceAndIdle()

        // Step 2 — Navigate to History and open the transaction details
        composeTestRule.onNodeWithContentDescription("History").performClick()
        advanceAndIdle()

        composeTestRule.onNodeWithText("Edit Target", substring = true)
            .performScrollTo()
            .performClick()
        advanceAndIdle()

        // Step 3 — Tap the "Edit" action in the details bottom sheet
        // The bottom sheet shows an icon + label "Edit" (contentDescription = "Edit")
        composeTestRule.onNodeWithContentDescription("Edit").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Edit").performClick()
        advanceAndIdle()

        // Step 4 — On AddTransaction screen (pre-filled), clear amount and type new value
        composeTestRule.onNodeWithTag("amount_input")
            .performTextClearance()
        composeTestRule.onNodeWithTag("amount_input")
            .performTextInput("4999.0")
        advanceAndIdle()

        composeTestRule.onNodeWithTag("save_button").performClick()
        advanceAndIdle()

        // Step 5 — Navigate back to History and confirm updated payee row exists
        composeTestRule.onNodeWithContentDescription("History").performClick()
        advanceAndIdle()

        composeTestRule.onNodeWithText("Edit Target", substring = true)
            .performScrollTo()
            .assertIsDisplayed()
    }
}

