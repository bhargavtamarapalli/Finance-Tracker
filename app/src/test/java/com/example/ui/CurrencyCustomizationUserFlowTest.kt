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
class CurrencyCustomizationUserFlowTest {

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
    fun testCurrencyChangeFlow() {
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

        // 2. Verify default currency INR is used initially on Dashboard
        composeTestRule.onNodeWithText("₹58,600.00").assertIsDisplayed()

        // 3. Open Drawer and navigate to Settings
        composeTestRule.onNodeWithContentDescription("Menu").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription("Settings").performClick()
        composeTestRule.waitForIdle()

        // 4. Change currency option to USD
        composeTestRule.onNodeWithText("USD ($)", substring = true).performScrollTo().performClick()
        composeTestRule.waitForIdle()

        // 5. Navigate back to Dashboard
        composeTestRule.onNodeWithContentDescription("Menu").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription("Home").performClick()
        composeTestRule.waitForIdle()

        // 6. Verify Dashboard displays values with USD $ symbol
        printSemanticsTreeText()
        composeTestRule.onNodeWithText("$58,600.00").assertIsDisplayed()
        composeTestRule.onNodeWithText("₹58,600.00").assertDoesNotExist()
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
