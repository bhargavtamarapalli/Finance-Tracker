package com.example.admin.ui.screens

import android.content.Context
import androidx.compose.ui.test.*
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
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowLooper

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w1000dp-h2000dp-xhdpi", sdk = [33])
class AdminConsoleUserFlowTest {

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
        val prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()

        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .setQueryExecutor { it.run() }
            .setTransactionExecutor { it.run() }
            .build()
        val jsonDataManager = JsonDataManager(context, com.example.fakes.PlainFileStorage())
        financeRepository = FinanceRepository(db.financeDao(), jsonDataManager)
        authRepository = AuthRepository(context, injectedAuthPrefs = com.example.fakes.FakeSharedPreferences(), forceDemoFallback = true)

        financeViewModel = FinanceViewModel(financeRepository, injectedPrefs = com.example.fakes.FakeSharedPreferences())
        authViewModel = AuthViewModel(authRepository)
    }

    @After
    fun tearDown() {
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
    fun testAdminPublishAnnouncementFlow() {
        // 1. Sign up/Log in as Admin user
        authViewModel.signUp("admin@example.com", "Password123", "Admin User")
        ShadowLooper.idleMainLooper()

        composeTestRule.setContent {
            FinanceTrackerTheme {
                FinanceApp(viewModel = financeViewModel, authViewModel = authViewModel)
            }
        }
        bypassSplash()

        // 2. Open Settings
        composeTestRule.onNodeWithContentDescription("Menu").performClick()
        composeTestRule.waitForIdle()
        ShadowLooper.idleMainLooper()

        composeTestRule.onNodeWithContentDescription("Settings").performClick()
        composeTestRule.waitForIdle()
        ShadowLooper.idleMainLooper()

        // 3. Verify that Admin Console settings items are visible for Admin
        composeTestRule.onNodeWithText("Administrative Access").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Admin Console").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        ShadowLooper.idleMainLooper()

        // 4. Verify we are on Admin Console screen
        composeTestRule.onNodeWithText("Platform Aggregate Metrics").assertIsDisplayed()

        // Switch to Broadcasts Tab
        composeTestRule.onNodeWithTag("tab_broadcasts").performClick()
        composeTestRule.waitForIdle()
        ShadowLooper.idleMainLooper()

        // 5. Input Title & Content, then Publish
        composeTestRule.onNodeWithTag("announcement_title_input").performScrollTo().performTextInput("Midnight Maintenance")
        composeTestRule.onNodeWithTag("announcement_content_input").performScrollTo().performTextInput("Database cleanup scheduled at 12 AM.")
        
        composeTestRule.onNodeWithTag("publish_announcement_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        ShadowLooper.idleMainLooper()

        // 6. Verify that published announcement is shown in list on Admin Console
        composeTestRule.onNodeWithText("Midnight Maintenance").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Database cleanup scheduled at 12 AM.").performScrollTo().assertIsDisplayed()
    }
}
