package com.example.ui

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

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [33])
class FinanceLedgerUserFlowTest {

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
        android.provider.Settings.Global.putFloat(context.contentResolver, android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 0f)
        android.provider.Settings.Global.putFloat(context.contentResolver, android.provider.Settings.Global.TRANSITION_ANIMATION_SCALE, 0f)
        android.provider.Settings.Global.putFloat(context.contentResolver, android.provider.Settings.Global.WINDOW_ANIMATION_SCALE, 0f)
        
        // Clear auth prefs
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

        financeViewModel = FinanceViewModel(financeRepository)
        authViewModel = AuthViewModel(authRepository)
    }

    @After
    fun tearDown() {
        ShadowLooper.idleMainLooper()
        db.close()
        Dispatchers.resetMain()
    }

    private fun bypassSplash() {
        testDispatcher.scheduler.advanceTimeBy(3000L)
        composeTestRule.mainClock.advanceTimeBy(3000L)
        composeTestRule.waitForIdle()
        testDispatcher.scheduler.advanceUntilIdle()
        db.invalidationTracker.refreshVersionsSync()
        composeTestRule.waitForIdle()
        ShadowLooper.idleMainLooper()
    }

    @Test
    fun testTransactionLedgerFlow() {
        // 1. Sign up/Log in a normal user
        authViewModel.signUp("user@example.com", "Password123", "Normal User")
        testDispatcher.scheduler.advanceUntilIdle()
        db.invalidationTracker.refreshVersionsSync()
        ShadowLooper.idleMainLooper()

        composeTestRule.setContent {
            FinanceTrackerTheme {
                FinanceApp(viewModel = financeViewModel, authViewModel = authViewModel)
            }
        }
        bypassSplash()

        // 2. Verify we are on Dashboard showing user initials/badge
        composeTestRule.onNodeWithText("Normal").assertIsDisplayed()

        // 3. Click "Add Transaction" FAB to open Add Transaction screen
        composeTestRule.onNodeWithContentDescription("Add Transaction").performClick()
        composeTestRule.waitForIdle()
        testDispatcher.scheduler.advanceUntilIdle()
        db.invalidationTracker.refreshVersionsSync()
        ShadowLooper.idleMainLooper()
        composeTestRule.waitForIdle()

        // 4. Fill in amount, payee, notes, payment method, category, and save
        composeTestRule.onNodeWithText("Amount").performTextInput("1500.0")
        composeTestRule.onNodeWithText("Payee / Store").performTextInput("Target Supermarket")
        composeTestRule.onNodeWithText("Notes (Optional)").performTextInput("Monthly snacks")
        
        composeTestRule.onNodeWithText("Card").performClick()
        composeTestRule.onNodeWithText("Groceries").performClick()
        
        composeTestRule.onNodeWithText("Save").performClick()
        composeTestRule.waitForIdle()

        // 5. Verify we are navigated back to Dashboard and balance / average updates
        composeTestRule.onNodeWithText("Normal").assertIsDisplayed()
        
        // The default seeded income is ₹92,000.00 and seeded expenses sum to ₹33,400.00
        // Adding an expense of ₹1,500.00 should change the total balance:
        // Initial Balance = 92000 - 33400 = ₹58,600.00
        // New Balance = 58600 - 1500 = ₹57,100.00
        composeTestRule.onNodeWithText("₹57,100.00").assertIsDisplayed()
        
        // Navigate to History screen to verify all transactions list
        composeTestRule.onNodeWithContentDescription("History").performClick()
        composeTestRule.waitForIdle()

        printSemanticsTreeText()

        // Verify the added transaction is listed on the History screen by scrolling to it
        composeTestRule.onNode(hasScrollAction())
            .performScrollToNode(hasText("Target Supermarket", substring = true))

        composeTestRule.onNodeWithText("Target Supermarket", substring = true)
            .assertIsDisplayed()
    }

    private fun printSemanticsTreeText() {
        try {
            val root = composeTestRule.onRoot().fetchSemanticsNode()
            val file = java.io.File("/Users/bhargavtamarapalli/.gemini/antigravity-ide/brain/0405ccdd-c3b9-4efd-bba1-821470563c76/scratch/semantics_tree.txt")
            val sb = java.lang.StringBuilder()
            buildTreeString(root, sb, 0)
            file.writeText(sb.toString())
        } catch (e: Exception) {
            val file = java.io.File("/Users/bhargavtamarapalli/.gemini/antigravity-ide/brain/0405ccdd-c3b9-4efd-bba1-821470563c76/scratch/semantics_tree.txt")
            file.writeText("Error: ${e.message}\n")
        }
    }

    private fun buildTreeString(node: androidx.compose.ui.semantics.SemanticsNode, sb: java.lang.StringBuilder, depth: Int) {
        val indent = "  ".repeat(depth)
        val textList = node.config.getOrNull(androidx.compose.ui.semantics.SemanticsProperties.Text)
        val testTag = node.config.getOrNull(androidx.compose.ui.semantics.SemanticsProperties.TestTag)
        val contentDesc = node.config.getOrNull(androidx.compose.ui.semantics.SemanticsProperties.ContentDescription)
        
        sb.append("${indent}- Node: tag=$testTag, text=$textList, desc=$contentDesc\n")
        
        node.children.forEach { child ->
            buildTreeString(child, sb, depth + 1)
        }
    }
}
