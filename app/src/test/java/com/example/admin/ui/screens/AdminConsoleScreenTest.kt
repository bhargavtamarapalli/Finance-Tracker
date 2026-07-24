package com.example.admin.ui.screens

import android.content.Context
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.JsonDataManager
import com.example.data.repository.FinanceRepository
import com.example.ui.theme.FinanceTrackerTheme
import com.example.ui.viewmodel.FinanceViewModel
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.robolectric.shadows.ShadowLooper
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.resetMain

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w1000dp-h2000dp-xhdpi", sdk = [33])
class AdminConsoleScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var db: AppDatabase
    private lateinit var repository: FinanceRepository
    private lateinit var viewModel: FinanceViewModel

    @OptIn(ExperimentalCoroutinesApi::class)
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        val context = ApplicationProvider.getApplicationContext<Context>()
        android.provider.Settings.Global.putFloat(context.contentResolver, android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 0f)
        android.provider.Settings.Global.putFloat(context.contentResolver, android.provider.Settings.Global.TRANSITION_ANIMATION_SCALE, 0f)
        android.provider.Settings.Global.putFloat(context.contentResolver, android.provider.Settings.Global.WINDOW_ANIMATION_SCALE, 0f)
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .setQueryExecutor { it.run() }
            .setTransactionExecutor { it.run() }
            .build()
        val jsonDataManager = JsonDataManager(context, com.example.fakes.PlainFileStorage())
        repository = FinanceRepository(db.financeDao(), jsonDataManager)
        viewModel = FinanceViewModel(repository, injectedPrefs = com.example.fakes.FakeSharedPreferences())
        
        // Mock AdminViewModel
        adminViewModel = io.mockk.mockk(relaxed = true)
        io.mockk.every { adminViewModel.systemStats } returns kotlinx.coroutines.flow.MutableStateFlow(
            com.example.admin.data.model.AdminSystemStats(8, 6, 2, 150L, 12, 3)
        )
        io.mockk.every { adminViewModel.allUsers } returns kotlinx.coroutines.flow.MutableStateFlow(emptyList())
        io.mockk.every { adminViewModel.searchQuery } returns kotlinx.coroutines.flow.MutableStateFlow("")
        io.mockk.every { adminViewModel.filterStatus } returns kotlinx.coroutines.flow.MutableStateFlow(null)
        io.mockk.every { adminViewModel.filterPlan } returns kotlinx.coroutines.flow.MutableStateFlow(null)
        io.mockk.every { adminViewModel.isBusy } returns kotlinx.coroutines.flow.MutableStateFlow(false)
        io.mockk.every { adminViewModel.filteredUsers } returns kotlinx.coroutines.flow.MutableStateFlow(emptyList())
        io.mockk.every { adminViewModel.adminEvent } returns kotlinx.coroutines.flow.MutableSharedFlow()
    }

    private lateinit var adminViewModel: com.example.admin.ui.viewmodel.AdminViewModel

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
        clearViewModel(viewModel)
        db.close()
    }

    @Test
    fun testAdminConsoleScreen_displaysElementsAndPublishesAnnouncement() {
        composeTestRule.setContent {
            FinanceTrackerTheme {
                AdminConsoleScreen(
                    viewModel = viewModel,
                    adminViewModel = adminViewModel,
                    onBackClick = {}
                )
            }
        }
        // Force the host activity's lifecycle to RESUMED to enable collectAsStateWithLifecycle collections
        (composeTestRule as? androidx.compose.ui.test.junit4.AndroidComposeTestRule<*, *>)?.let { androidRule ->
            composeTestRule.runOnUiThread {
                (androidRule.activity.lifecycle as? androidx.lifecycle.LifecycleRegistry)?.currentState = androidx.lifecycle.Lifecycle.State.RESUMED
            }
        }
        composeTestRule.waitForIdle()
        ShadowLooper.idleMainLooper()

        // 1. Verify initial headers on Dashboard tab
        composeTestRule.onNodeWithText("Platform Aggregate Metrics").assertIsDisplayed()
        composeTestRule.onNodeWithText("Total Users").assertIsDisplayed()
        composeTestRule.onNodeWithText("Active Sessions").assertIsDisplayed()

        // 2. Click Broadcasts Tab to publish announcement
        composeTestRule.onNodeWithTag("tab_broadcasts").performClick()
        composeTestRule.waitForIdle()
        ShadowLooper.idleMainLooper()

        // Verify announcement publisher input fields exist
        composeTestRule.onNodeWithTag("announcement_title_input").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("announcement_content_input").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("publish_announcement_button").performScrollTo().assertIsDisplayed()

        // Verify default announcement card is rendered in list immediately
        composeTestRule.onNodeWithText("System Privacy Guard: Enabled").performScrollTo().assertIsDisplayed()

        // Enter input for a new announcement
        composeTestRule.onNodeWithTag("announcement_title_input").performScrollTo().performTextInput("New Alert")
        composeTestRule.onNodeWithTag("announcement_content_input").performScrollTo().performTextInput("System down for 10m.")
        
        // Click publish
        composeTestRule.onNodeWithTag("publish_announcement_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        ShadowLooper.idleMainLooper()
        
        // Assert state updated in ViewModel (now has 3 announcements including the new one)
        org.junit.Assert.assertEquals(3, viewModel.announcements.value.size)
        org.junit.Assert.assertTrue(viewModel.announcements.value.any { it.title == "New Alert" && it.content == "System down for 10m." })
    }
}
