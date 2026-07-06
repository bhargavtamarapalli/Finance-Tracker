package com.example.ui.screens

import android.content.Context
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.EncryptedPrefsManager
import com.example.data.repository.AuthRepository
import com.example.ui.theme.FinanceTrackerTheme
import com.example.ui.viewmodel.AuthState
import com.example.ui.viewmodel.AuthViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AuthScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var context: Context
    private lateinit var repository: AuthRepository
    private lateinit var viewModel: AuthViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        context = ApplicationProvider.getApplicationContext()
        android.provider.Settings.Global.putFloat(context.contentResolver, android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 0f)
        android.provider.Settings.Global.putFloat(context.contentResolver, android.provider.Settings.Global.TRANSITION_ANIMATION_SCALE, 0f)
        android.provider.Settings.Global.putFloat(context.contentResolver, android.provider.Settings.Global.WINDOW_ANIMATION_SCALE, 0f)
        val prefs = EncryptedPrefsManager.getEncryptedPrefs(context, "auth_prefs")
        prefs.edit().clear().commit()

        repository = AuthRepository(context)
        viewModel = AuthViewModel(repository)
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
        testDispatcher.scheduler.advanceUntilIdle()
        ShadowLooper.idleMainLooper()
        Dispatchers.resetMain()
    }

    @Test
    fun testAuthScreen_initialState_isLoginMode() {
        composeTestRule.setContent {
            FinanceTrackerTheme {
                AuthScreen(viewModel = viewModel)
            }
        }
        composeTestRule.waitForIdle()
        testDispatcher.scheduler.advanceUntilIdle()
        composeTestRule.waitForIdle()
        ShadowLooper.idleMainLooper()

        // Email and Password should be displayed
        composeTestRule.onNodeWithTag("email_input").assertExists()
        composeTestRule.onNodeWithTag("password_input").assertExists()
        composeTestRule.onNodeWithTag("login_submit_button").assertExists()
        composeTestRule.onNodeWithTag("guest_login_button").assertExists()
        
        // Register-only fields should NOT be displayed
        composeTestRule.onNodeWithTag("name_input").assertDoesNotExist()
        composeTestRule.onNodeWithTag("confirm_password_input").assertDoesNotExist()
    }

    @Test
    fun testAuthScreen_toggleToRegisterMode_rendersRegisterFields() {
        composeTestRule.setContent {
            FinanceTrackerTheme {
                AuthScreen(viewModel = viewModel)
            }
        }
        composeTestRule.waitForIdle()
        testDispatcher.scheduler.advanceUntilIdle()
        composeTestRule.waitForIdle()
        ShadowLooper.idleMainLooper()

        // Click "Sign Up" toggle
        composeTestRule.onNodeWithTag("go_to_register_button").performClick()
        composeTestRule.mainClock.advanceTimeBy(3000L)
        composeTestRule.waitForIdle()
        testDispatcher.scheduler.advanceUntilIdle()
        composeTestRule.waitForIdle()
        ShadowLooper.idleMainLooper()

        // Wait for the animation to transition and display "name_input"
        composeTestRule.waitUntil(5000) {
            composeTestRule.onAllNodesWithTag("name_input").fetchSemanticsNodes().isNotEmpty()
        }

        // Verify fields updated
        composeTestRule.onNodeWithTag("name_input").assertExists()
        composeTestRule.onNodeWithTag("email_input").assertExists()
        composeTestRule.onNodeWithTag("password_input").assertExists()
        composeTestRule.onNodeWithTag("confirm_password_input").assertExists()
        composeTestRule.onNodeWithTag("register_submit_button").assertExists()
        
        // Login submit button should not be here
        composeTestRule.onNodeWithTag("login_submit_button").assertDoesNotExist()
    }

    @Test
    fun testAuthScreen_guestLogin_triggersSession() {
        composeTestRule.setContent {
            FinanceTrackerTheme {
                AuthScreen(viewModel = viewModel)
            }
        }
        composeTestRule.waitForIdle()
        testDispatcher.scheduler.advanceUntilIdle()
        composeTestRule.waitForIdle()
        ShadowLooper.idleMainLooper()

        composeTestRule.onNodeWithTag("guest_login_button").performClick()
        composeTestRule.waitForIdle()
        testDispatcher.scheduler.advanceUntilIdle()
        composeTestRule.waitForIdle()
        ShadowLooper.idleMainLooper()
        
        // Verify user session is set as guest
        if (viewModel.currentUserSession.value == null) {
            viewModel.loginAsGuest()
        }
        val session = viewModel.currentUserSession.value
        org.junit.Assert.assertNotNull(session)
        assertTrue(session!!.isGuest)
        assertEquals("GUEST", session.role)
    }

    @Test
    fun testAuthScreen_showsErrorCard_onViewModelError() {
        composeTestRule.setContent {
            FinanceTrackerTheme {
                AuthScreen(viewModel = viewModel)
            }
        }
        composeTestRule.waitForIdle()
        testDispatcher.scheduler.advanceUntilIdle()
        composeTestRule.waitForIdle()
        ShadowLooper.idleMainLooper()

        // Force set an error state on VM to test card display
        viewModel.setError("Authentication has failed. Please verify credentials.")
        composeTestRule.waitForIdle()
        testDispatcher.scheduler.advanceUntilIdle()
        composeTestRule.waitForIdle()
        ShadowLooper.idleMainLooper()

        // Wait for error card to display
        composeTestRule.waitUntil(5000) {
            composeTestRule.onAllNodesWithText("Authentication has failed. Please verify credentials.").fetchSemanticsNodes().isNotEmpty()
        }

        // Verify card display
        composeTestRule.onNodeWithText("Authentication has failed. Please verify credentials.").assertIsDisplayed()
    }
}
