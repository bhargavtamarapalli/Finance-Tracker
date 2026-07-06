package com.example.ui.screens

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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AdminConsoleScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var db: AppDatabase
    private lateinit var repository: FinanceRepository
    private lateinit var viewModel: FinanceViewModel

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        android.provider.Settings.Global.putFloat(context.contentResolver, android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 0f)
        android.provider.Settings.Global.putFloat(context.contentResolver, android.provider.Settings.Global.TRANSITION_ANIMATION_SCALE, 0f)
        android.provider.Settings.Global.putFloat(context.contentResolver, android.provider.Settings.Global.WINDOW_ANIMATION_SCALE, 0f)
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .setQueryExecutor { it.run() }
            .setTransactionExecutor { it.run() }
            .build()
        val jsonDataManager = JsonDataManager(context)
        repository = FinanceRepository(db.financeDao(), jsonDataManager)
        viewModel = FinanceViewModel(repository)
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
        clearViewModel(viewModel)
        db.close()
    }

    @Test
    fun testAdminConsoleScreen_displaysElementsAndPublishesAnnouncement() {
        composeTestRule.setContent {
            FinanceTrackerTheme {
                AdminConsoleScreen(
                    viewModel = viewModel,
                    onBackClick = {}
                )
            }
        }
        composeTestRule.waitForIdle()
        ShadowLooper.idleMainLooper()

        // Verify initial headers
        composeTestRule.onNodeWithText("System Diagnostics & Privacy Guard").assertIsDisplayed()
        composeTestRule.onNodeWithText("Total Users").assertIsDisplayed()
        composeTestRule.onNodeWithText("Active Sessions").assertIsDisplayed()

        // Verify announcement publisher input fields exist (using performScrollTo() to ensure they are visible)
        composeTestRule.onNodeWithTag("announcement_title_input").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("announcement_content_input").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("publish_announcement_button").performScrollTo().assertIsDisplayed()

        // Enter input
        composeTestRule.onNodeWithTag("announcement_title_input").performTextInput("Server Update")
        composeTestRule.onNodeWithTag("announcement_content_input").performTextInput("Restarting at 2 AM.")
        
        // Click publish
        composeTestRule.onNodeWithTag("publish_announcement_button").performClick()
        composeTestRule.waitForIdle()
        ShadowLooper.idleMainLooper()

        // Verify announcement card is rendered in list (using performScrollTo() to ensure they are visible)
        composeTestRule.onNodeWithText("Server Update").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Restarting at 2 AM.").performScrollTo().assertIsDisplayed()
    }
}
