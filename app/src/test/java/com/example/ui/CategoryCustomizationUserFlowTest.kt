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
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowLooper
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w1000dp-h2000dp-xhdpi", sdk = [33])
class CategoryCustomizationUserFlowTest {

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
    fun testCategoryCreationAndSelectionFlow() {
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

        // 2. Open Settings
        composeTestRule.onNodeWithContentDescription("Menu").performClick()
        composeTestRule.waitForIdle()
        ShadowLooper.idleMainLooper()

        composeTestRule.onNodeWithContentDescription("Settings").performClick()
        composeTestRule.waitForIdle()
        ShadowLooper.idleMainLooper()

        // 3. Navigate to Category Management
        composeTestRule.onNodeWithText("Manage Categories").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        ShadowLooper.idleMainLooper()

        // 4. Click Add Category FAB to open dialog
        composeTestRule.onNodeWithContentDescription("Add Category").performClick()
        composeTestRule.waitForIdle()
        ShadowLooper.idleMainLooper()

        // 5. Input name "Organic Tea", select "Food" icon, and click Create
        composeTestRule.onNodeWithText("Category Name").performTextInput("Organic Tea")
        composeTestRule.onNodeWithText("Category Name").performImeAction()
        composeTestRule.onAllNodesWithText("Food").onLast().performClick()
        composeTestRule.onNodeWithText("Create").performClick()
        composeTestRule.waitForIdle()
        ShadowLooper.idleMainLooper()

        // 6. Navigate back to Settings, then back to Dashboard
        composeTestRule.onNodeWithContentDescription("Back").performClick()
        composeTestRule.waitForIdle()
        ShadowLooper.idleMainLooper()
        composeTestRule.onNodeWithContentDescription("Menu").performClick()
        composeTestRule.waitForIdle()
        ShadowLooper.idleMainLooper()
        composeTestRule.onNodeWithContentDescription("Home").performClick()
        composeTestRule.waitForIdle()
        ShadowLooper.idleMainLooper()

        // 7. Click Add Expense and verify the custom category "Organic Tea" is selectable
        composeTestRule.onNodeWithText("Add Expense").performClick()
        composeTestRule.waitForIdle()
        ShadowLooper.idleMainLooper()
        
        composeTestRule.onNodeWithText("Organic Tea").performScrollTo().assertIsDisplayed()
    }
}
