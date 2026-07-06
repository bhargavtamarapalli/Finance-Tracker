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
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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
@Config(sdk = [33])
class AuthScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val testDispatcher = UnconfinedTestDispatcher()
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
        // Seed a user for login/forgot password tests to succeed
        kotlinx.coroutines.runBlocking {
            repository.signUpWithEmail("test@example.com", "Password123", "Test User")
            repository.logout()
        }
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

        // Click "Sign Up" toggle
        composeTestRule.onNodeWithTag("go_to_register_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()

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

        composeTestRule.onNodeWithTag("guest_login_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        
        // Verify user session is set as guest
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

        // Force set an error state on VM to test card display
        viewModel.setError("Authentication has failed. Please verify credentials.")
        composeTestRule.waitForIdle()

        // Verify card display
        composeTestRule.onNodeWithText("Authentication has failed. Please verify credentials.").assertIsDisplayed()
    }

    @Test
    fun testAuthScreen_successfulLoginFlow() {
        composeTestRule.setContent {
            FinanceTrackerTheme {
                AuthScreen(viewModel = viewModel)
            }
        }
        composeTestRule.waitForIdle()

        // Enter credentials
        composeTestRule.onNodeWithTag("email_input").performTextInput("test@example.com")
        composeTestRule.onNodeWithTag("password_input").performTextInput("Password123")
        
        // Click login submit
        composeTestRule.onNodeWithTag("login_submit_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        // Verify the AuthViewModel sign-in state transitions to success
        val session = viewModel.currentUserSession.value
        org.junit.Assert.assertNotNull(session)
        assertEquals("test@example.com", session!!.email)
    }

    @Test
    fun testAuthScreen_registerPasswordMismatch_showsError() {
        composeTestRule.setContent {
            FinanceTrackerTheme {
                AuthScreen(viewModel = viewModel)
            }
        }
        composeTestRule.waitForIdle()

        // Toggle to register mode
        composeTestRule.onNodeWithTag("go_to_register_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        // Input matching details except password
        composeTestRule.onNodeWithTag("name_input").performTextInput("Test User")
        composeTestRule.onNodeWithTag("email_input").performTextInput("user@example.com")
        composeTestRule.onNodeWithTag("password_input").performTextInput("Password123")
        composeTestRule.onNodeWithTag("confirm_password_input").performTextInput("DifferentPassword456")

        // Click register
        composeTestRule.onNodeWithTag("register_submit_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        // Assert error message displays
        composeTestRule.onNodeWithTag("auth_error_card").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Passwords do not match").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun testAuthScreen_forgotPasswordFlow_navigationAndReset() {
        composeTestRule.setContent {
            FinanceTrackerTheme {
                AuthScreen(viewModel = viewModel)
            }
        }
        composeTestRule.waitForIdle()

        // Click Forgot Password?
        composeTestRule.onNodeWithTag("forgot_password_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        // Reset password fields should be displayed
        composeTestRule.onNodeWithText("Reset Password").assertExists()
        composeTestRule.onNodeWithTag("email_input").assertExists()
        composeTestRule.onNodeWithTag("reset_submit_button").assertExists()

        // Enter email and send reset link
        composeTestRule.onNodeWithTag("email_input").performTextInput("reset@example.com")
        composeTestRule.onNodeWithTag("reset_submit_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        // Assert status success card displays
        composeTestRule.onNodeWithTag("auth_status_card").assertIsDisplayed()
        composeTestRule.onNodeWithText("A password reset link has been sent to reset@example.com.").assertIsDisplayed()

        // Navigate back to Login
        composeTestRule.onNodeWithText("Back to Login").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        // Verify we are back on Login Screen
        composeTestRule.onNodeWithTag("login_submit_button").assertExists()
    }

    @Test
    fun testAuthScreen_googleSignIn_triggersSuccess() {
        composeTestRule.setContent {
            FinanceTrackerTheme {
                AuthScreen(viewModel = viewModel)
            }
        }
        composeTestRule.waitForIdle()

        // Click Google Login
        composeTestRule.onNodeWithTag("google_login_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        // Verify user logged in as google user session
        val session = viewModel.currentUserSession.value
        org.junit.Assert.assertNotNull(session)
        assertEquals("bhargav1999.t@gmail.com", session!!.email)
        assertEquals("Bhargav T", session.name)
    }
}
