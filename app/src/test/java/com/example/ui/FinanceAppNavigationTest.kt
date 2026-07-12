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
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowLooper

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w1000dp-h2000dp-xhdpi", sdk = [33])
class FinanceAppNavigationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var financeRepository: FinanceRepository
    private lateinit var authRepository: AuthRepository
    private lateinit var financeViewModel: FinanceViewModel
    private lateinit var authViewModel: AuthViewModel

    @Before
    fun setUp() {
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

    private fun clearViewModel(vm: androidx.lifecycle.ViewModel) {
        try {
            val method = androidx.lifecycle.ViewModel::class.java.getDeclaredMethod("onCleared")
            method.isAccessible = true
            method.invoke(vm)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @After
    fun tearDown() {
        clearViewModel(financeViewModel)
        clearViewModel(authViewModel)
        db.invalidationTracker.refreshVersionsSync()
        ShadowLooper.idleMainLooper()
        db.close()
    }

    private fun bypassSplash() {
        composeTestRule.mainClock.advanceTimeBy(3000L)
        composeTestRule.waitForIdle()
        db.invalidationTracker.refreshVersionsSync()
        composeTestRule.waitForIdle()
        ShadowLooper.idleMainLooper()
    }

    @Test
    fun testNavigation_whenNotLoggedIn_showsAuthScreen() {
        composeTestRule.setContent {
            FinanceTrackerTheme {
                FinanceApp(viewModel = financeViewModel, authViewModel = authViewModel)
            }
        }
        bypassSplash()

        // Should render AuthScreen, which has guest_login_button
        composeTestRule.onNodeWithTag("guest_login_button").assertExists()
        composeTestRule.onNodeWithTag("email_input").assertExists()
    }

    @Test
    fun testNavigation_whenGuestLoggedIn_hidesAdminAndCloudBackup() {
        // Log in as guest
        authViewModel.loginAsGuest()
        ShadowLooper.idleMainLooper()
        db.invalidationTracker.refreshVersionsSync()
        ShadowLooper.idleMainLooper()

        composeTestRule.setContent {
            FinanceTrackerTheme {
                FinanceApp(viewModel = financeViewModel, authViewModel = authViewModel)
            }
        }
        bypassSplash()

        // 1. Check we are on Dashboard
        composeTestRule.onNodeWithText("Total Balance", ignoreCase = true).assertIsDisplayed()

        // 2. Navigate to settings using the bottom bar
        composeTestRule.onNodeWithContentDescription("Settings").performClick()
        composeTestRule.waitForIdle()
        ShadowLooper.idleMainLooper()

        // 3. Verify Admin Console and Cloud Backup are hidden for Guest
        composeTestRule.onNodeWithText("Administrative Access").assertDoesNotExist()
        composeTestRule.onNodeWithText("Admin Console").assertDoesNotExist()
        composeTestRule.onNodeWithText("Backup to Cloud").assertDoesNotExist()
    }

    @Test
    fun testNavigation_whenNormalUserLoggedIn_hidesAdminShowsCloudBackup() {
        // Register & Log in as normal user
        authViewModel.signUp("user@example.com", "Password123", "Normal User")
        ShadowLooper.idleMainLooper()
        db.invalidationTracker.refreshVersionsSync()
        ShadowLooper.idleMainLooper()

        composeTestRule.setContent {
            FinanceTrackerTheme {
                FinanceApp(viewModel = financeViewModel, authViewModel = authViewModel)
            }
        }
        bypassSplash()

        // 1. Verify dashboard shows first-name badge
        composeTestRule.onNodeWithText("Normal").assertIsDisplayed()

        // 2. Navigate to Settings using the bottom bar
        composeTestRule.onNodeWithContentDescription("Settings").performClick()
        composeTestRule.waitForIdle()
        ShadowLooper.idleMainLooper()

        // 3. Verify User has access to Cloud Backup but not Admin Console
        composeTestRule.onNodeWithText("Backup to Cloud").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Administrative Access").assertDoesNotExist()
        composeTestRule.onNodeWithText("Admin Console").assertDoesNotExist()
    }

    private fun printSemanticsTreeText() {
        try {
            val root = composeTestRule.onRoot().fetchSemanticsNode()
            val file = java.io.File("/Users/bhargavtamarapalli/.gemini/antigravity/brain/8d14cdc5-b5bf-4076-ae32-0b43c6d2e6f6/scratch/semantics_tree.txt")
            if (file.parentFile != null) {
                file.parentFile.mkdirs()
            }
            val sb = java.lang.StringBuilder()
            buildTreeString(root, sb, 0)
            file.writeText(sb.toString())
        } catch (e: Exception) {
            val file = java.io.File("/Users/bhargavtamarapalli/.gemini/antigravity/brain/8d14cdc5-b5bf-4076-ae32-0b43c6d2e6f6/scratch/semantics_tree.txt")
            if (file.parentFile != null) {
                file.parentFile.mkdirs()
            }
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

    @Test
    fun testNavigation_whenAdminLoggedIn_showsAdminAndCloudBackup() {
        // Register & Log in as admin
        authViewModel.signUp("admin@example.com", "Password123", "Admin User")
        ShadowLooper.idleMainLooper()
        db.invalidationTracker.refreshVersionsSync()
        ShadowLooper.idleMainLooper()

        composeTestRule.setContent {
            FinanceTrackerTheme {
                FinanceApp(viewModel = financeViewModel, authViewModel = authViewModel)
            }
        }
        bypassSplash()

        // 1. Navigate to Settings using bottom bar
        composeTestRule.onNodeWithContentDescription("Settings").performClick()
        composeTestRule.waitForIdle()
        ShadowLooper.idleMainLooper()

        // 2. Verify Admin has access to both Cloud Backup and Admin Console
        composeTestRule.onNodeWithText("Backup to Cloud").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Administrative Access").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Admin Console").performScrollTo().assertIsDisplayed()

        // 3. Click Admin Console and verify navigation to AdminConsoleScreen succeeded.
        composeTestRule.onNodeWithText("Admin Console").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        ShadowLooper.idleMainLooper()
        composeTestRule.waitForIdle()

        printSemanticsTreeText()

        // Verify we are on Admin Console screen by checking its unique section title
        composeTestRule.onNodeWithText("System Diagnostics & Privacy Guard").performScrollTo().assertIsDisplayed()
    }
}
