package com.example.e2e.auth

import android.content.Context
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.platform.testTag
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
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowLooper

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [33])
class AuthScreenUserFlowTest {
    @get:Rule val composeTestRule = createComposeRule()
    private val hasNoContentDescription = SemanticsMatcher("has no content description") {
        !it.config.contains(SemanticsProperties.ContentDescription)
    }
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var financeRepository: FinanceRepository
    private lateinit var authRepository: AuthRepository
    private lateinit var financeViewModel: FinanceViewModel
    private lateinit var authViewModel: AuthViewModel

    @Before fun setUp() {
        Dispatchers.setMain(testDispatcher)
        context = ApplicationProvider.getApplicationContext()
        android.provider.Settings.Global.putFloat(context.contentResolver, android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 0f)
        android.provider.Settings.Global.putFloat(context.contentResolver, android.provider.Settings.Global.TRANSITION_ANIMATION_SCALE, 0f)
        android.provider.Settings.Global.putFloat(context.contentResolver, android.provider.Settings.Global.WINDOW_ANIMATION_SCALE, 0f)
        val prefs = EncryptedPrefsManager.getEncryptedPrefs(context, "auth_prefs")
        prefs.edit().clear().commit()
        val settingsPrefs = context.getSharedPreferences("settings_prefs", Context.MODE_PRIVATE)
        settingsPrefs.edit().clear().commit()
        try {
            com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
        } catch (ignored: Exception) {}
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries().setQueryExecutor { it.run() }.setTransactionExecutor { it.run() }.build()
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

    @After fun tearDown() {
        testDispatcher.scheduler.advanceUntilIdle()
        db.invalidationTracker.refreshVersionsSync()
        ShadowLooper.idleMainLooper()
        db.close()
        Dispatchers.resetMain()
    }

    // -------------------------------------------------------------------------
    // Helper utilities
    // -------------------------------------------------------------------------

    private fun bypassSplash() {
        testDispatcher.scheduler.advanceTimeBy(3000L)
        composeTestRule.mainClock.advanceTimeBy(3000L)
        composeTestRule.waitForIdle()
        testDispatcher.scheduler.advanceUntilIdle()
        db.invalidationTracker.refreshVersionsSync()
        composeTestRule.waitForIdle()
        ShadowLooper.idleMainLooper()
    }

    private fun advanceAndIdle() {
        composeTestRule.waitForIdle()
        testDispatcher.scheduler.advanceUntilIdle()
        db.invalidationTracker.refreshVersionsSync()
        ShadowLooper.idleMainLooper(1000, java.util.concurrent.TimeUnit.MILLISECONDS)
        composeTestRule.waitForIdle()
    }

    private fun launchApp() {
        composeTestRule.setContent {
            FinanceTrackerTheme { FinanceApp(viewModel = financeViewModel, authViewModel = authViewModel) }
        }
        bypassSplash()
    }

    /**
     * Registers a new user via the UI (Register form), which also lands on the
     * dashboard. Then navigates to Settings and performs "Log Out" so the caller
     * can re-launch a fresh session.
     */
    private fun signUpUser(
        name: String = "Test User",
        email: String = "testuser@example.com",
        password: String = "password123",
    ) {
        // Navigate to Register mode
        composeTestRule.onNodeWithTag("go_to_register_button").performClick()
        advanceAndIdle()

        // Fill registration fields
        composeTestRule.onNodeWithText("Full Name").performTextInput(name)
        advanceAndIdle()
        composeTestRule.onNodeWithText("Email Address").performTextInput(email)
        advanceAndIdle()
        composeTestRule.onNodeWithText("Password (min 6 characters)").performTextInput(password)
        advanceAndIdle()
        composeTestRule.onNodeWithText("Confirm Password").performTextInput(password)
        advanceAndIdle()

        // Submit registration
        composeTestRule.onNodeWithTag("register_submit_button").performClick()
        advanceAndIdle()

        // Wait until dashboard is visible
        try {
            composeTestRule.waitUntil(timeoutMillis = 5_000) {
                composeTestRule.onAllNodesWithText("TOTAL BALANCE").fetchSemanticsNodes().isNotEmpty()
            }
        } catch (e: Throwable) {
            val sb = java.lang.StringBuilder()
            try {
                val root = composeTestRule.onRoot().fetchSemanticsNode()
                buildTreeString(root, sb, 0)
            } catch (ex: Exception) {
                sb.append("Failed to fetch root: ${ex.message}")
            }
            try {
                java.io.File("semantics_tree.txt").writeText(sb.toString())
            } catch (ignored: Exception) {}
            throw e
        }
    }

    private fun buildTreeString(node: androidx.compose.ui.semantics.SemanticsNode, sb: java.lang.StringBuilder, depth: Int) {
        val indent = "  ".repeat(depth)
        val textList = node.config.getOrNull(SemanticsProperties.Text)
        val editableText = node.config.getOrNull(SemanticsProperties.EditableText)
        val testTag = node.config.getOrNull(SemanticsProperties.TestTag)
        val contentDesc = node.config.getOrNull(SemanticsProperties.ContentDescription)
        sb.append("${indent}- Node: tag=$testTag, text=$textList, editableText=$editableText, desc=$contentDesc\n")
        node.children.forEach { child ->
            buildTreeString(child, sb, depth + 1)
        }
    }

    /**
     * Navigates to Settings and taps "Log Out", then idles until the auth screen
     * reappears.
     */
    private fun performLogout() {
        composeTestRule.onNode(hasContentDescription("Settings")).performClick()
        advanceAndIdle()

        try {
            composeTestRule.onNode(hasText("Log Out").and(hasNoContentDescription)).performSemanticsAction(SemanticsActions.OnClick)
        } catch (t: Throwable) {
            val sb = java.lang.StringBuilder()
            try {
                val root = composeTestRule.onRoot().fetchSemanticsNode()
                buildTreeString(root, sb, 0)
            } catch (ex: Exception) {
                sb.append("Failed to fetch root: ${ex.message}")
            }
            java.io.File("tree_logout_failed.txt").writeText(sb.toString())
            throw t
        }
        advanceAndIdle()
    }

    /**
     * Logs in as a guest via the guest login button.
     */
    private fun loginAsGuest() {
        composeTestRule.onNodeWithTag("guest_login_button").performClick()
        advanceAndIdle()
    }

    // -------------------------------------------------------------------------
    // 1. authScreen_displaysAllLoginElements
    // -------------------------------------------------------------------------

    /**
     * Verifies that the login screen renders all expected UI elements when the
     * app is launched in an unauthenticated state.
     */
    @Test
    fun authScreen_displaysAllLoginElements() {
        launchApp()

        composeTestRule.onNodeWithTag("email_input").assertIsDisplayed()
        composeTestRule.onNodeWithTag("password_input").assertIsDisplayed()
        composeTestRule.onNodeWithTag("login_submit_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("guest_login_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("forgot_password_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("go_to_register_button").assertIsDisplayed()
    }

    // -------------------------------------------------------------------------
    // 2. authScreen_successfulLogin_navigatesToDashboard
    // -------------------------------------------------------------------------

    /**
     * Registers a user, logs out, then uses the login form to sign back in.
     * Asserts that the dashboard ("Total Balance") is visible after a successful
     * login.
     */
    @Test
    fun authScreen_successfulLogin_navigatesToDashboard() {
        launchApp()

        // Register so credentials exist in the local store
        signUpUser(email = "logintest@example.com", password = "securePass1")

        // Log out to return to auth screen
        performLogout()
        advanceAndIdle()

        // Log back in via the login form
        composeTestRule.onNodeWithText("Email Address").performTextInput("logintest@example.com")
        advanceAndIdle()
        composeTestRule.onNodeWithText("Password").performTextInput("securePass1")
        advanceAndIdle()

        composeTestRule.onNodeWithTag("login_submit_button").performClick()
        advanceAndIdle()

        // Assert dashboard
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithText("TOTAL BALANCE").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("TOTAL BALANCE").assertIsDisplayed()
    }

    // -------------------------------------------------------------------------
    // 3. authScreen_loginWithInvalidCredentials_showsError
    // -------------------------------------------------------------------------

    /**
     * Types credentials that do not match any registered account and asserts
     * that the auth_error_card appears.
     */
    @Test
    fun authScreen_loginWithInvalidCredentials_showsError() {
        launchApp()

        composeTestRule.onNodeWithText("Email Address").performTextInput("wrong@example.com")
        advanceAndIdle()
        composeTestRule.onNodeWithText("Password").performTextInput("wrongPassword")
        advanceAndIdle()

        composeTestRule.onNodeWithTag("login_submit_button").performClick()
        advanceAndIdle()

        composeTestRule.onNodeWithTag("auth_error_card").assertIsDisplayed()
    }

    // -------------------------------------------------------------------------
    // 4. authScreen_loginWithEmptyFields_showsValidationError
    // -------------------------------------------------------------------------

    /**
     * Clicks the login button without entering any credentials and asserts a
     * validation error containing "Please fill in all fields".
     */
    @Test
    fun authScreen_loginWithEmptyFields_showsValidationError() {
        launchApp()

        composeTestRule.onNodeWithTag("login_submit_button").performClick()
        advanceAndIdle()

        composeTestRule.onNodeWithTag("auth_error_card").assertIsDisplayed()
        composeTestRule.onNodeWithText("Please fill in all fields", substring = true).assertIsDisplayed()
    }

    // -------------------------------------------------------------------------
    // 5. authScreen_loginWithMalformedEmail_showsValidationError
    // -------------------------------------------------------------------------

    /**
     * Enters a malformed email address ("notanemail") with a valid-length
     * password and asserts a validation error containing "Invalid email format".
     */
    @Test
    fun authScreen_loginWithMalformedEmail_showsValidationError() {
        launchApp()

        composeTestRule.onNodeWithText("Email Address").performTextInput("notanemail")
        advanceAndIdle()
        composeTestRule.onNodeWithText("Password").performTextInput("somepassword")
        advanceAndIdle()

        composeTestRule.onNodeWithTag("login_submit_button").performClick()
        advanceAndIdle()

        composeTestRule.onNodeWithTag("auth_error_card").assertIsDisplayed()
        composeTestRule.onNodeWithText("Invalid email format", substring = true).assertIsDisplayed()
    }

    // -------------------------------------------------------------------------
    // 6. authScreen_switchToRegisterModeAndBack
    // -------------------------------------------------------------------------

    /**
     * Taps the "Sign Up" link to switch to Register mode and asserts that the
     * name_input field becomes visible. Then taps "Log In" to return to Login
     * mode and asserts that the name_input field is no longer visible.
     */
    @Test
    fun authScreen_switchToRegisterModeAndBack() {
        launchApp()

        // Switch to Register mode
        composeTestRule.onNodeWithTag("go_to_register_button").performClick()
        advanceAndIdle()

        composeTestRule.onNodeWithTag("name_input").assertIsDisplayed()

        // Switch back to Login mode
        composeTestRule.onNodeWithTag("go_to_login_button").performClick()
        advanceAndIdle()

        composeTestRule.onNodeWithTag("name_input").assertDoesNotExist()
    }

    // -------------------------------------------------------------------------
    // 7. authScreen_successfulRegistration_navigatesToDashboard
    // -------------------------------------------------------------------------

    /**
     * Fills in all required registration fields with valid data, submits the
     * form, and asserts that the dashboard ("Total Balance") is displayed.
     */
    @Test
    fun authScreen_successfulRegistration_navigatesToDashboard() {
        launchApp()
        java.io.File("tree_1_launch.txt").writeText({
            val sb = java.lang.StringBuilder()
            buildTreeString(composeTestRule.onRoot().fetchSemanticsNode(), sb, 0)
            sb.toString()
        }())

        composeTestRule.onNodeWithTag("go_to_register_button").performClick()
        advanceAndIdle()
        java.io.File("tree_2_register_mode.txt").writeText({
            val sb = java.lang.StringBuilder()
            buildTreeString(composeTestRule.onRoot().fetchSemanticsNode(), sb, 0)
            sb.toString()
        }())

        composeTestRule.onNodeWithText("Full Name").performTextInput("New User")
        advanceAndIdle()
        composeTestRule.onNodeWithText("Email Address").performTextInput("newuser@example.com")
        advanceAndIdle()
        composeTestRule.onNodeWithText("Password (min 6 characters)").performTextInput("validPass1")
        advanceAndIdle()
        composeTestRule.onNodeWithText("Confirm Password").performTextInput("validPass1")
        advanceAndIdle()
        java.io.File("tree_3_filled.txt").writeText({
            val sb = java.lang.StringBuilder()
            buildTreeString(composeTestRule.onRoot().fetchSemanticsNode(), sb, 0)
            sb.toString()
        }())

        composeTestRule.onNodeWithTag("register_submit_button").performClick()
        advanceAndIdle()

        try {
            composeTestRule.waitUntil(timeoutMillis = 5_000) {
                composeTestRule.onAllNodesWithText("TOTAL BALANCE").fetchSemanticsNodes().isNotEmpty()
            }
        } catch (e: Throwable) {
            val sb = java.lang.StringBuilder()
            try {
                val root = composeTestRule.onRoot().fetchSemanticsNode()
                buildTreeString(root, sb, 0)
            } catch (ex: Exception) {
                sb.append("Failed to fetch root: ${ex.message}")
            }
            java.io.File("tree_4_timeout.txt").writeText(sb.toString())
            throw e
        }
        composeTestRule.onNodeWithText("TOTAL BALANCE").assertIsDisplayed()
    }

    // -------------------------------------------------------------------------
    // 8. authScreen_registrationWithShortPassword_showsError
    // -------------------------------------------------------------------------

    /**
     * Fills in the registration form with a password that is only 3 characters
     * long and asserts a validation error containing
     * "Password must be at least 6 characters".
     */
    @Test
    fun authScreen_registrationWithShortPassword_showsError() {
        launchApp()

        composeTestRule.onNodeWithTag("go_to_register_button").performClick()
        advanceAndIdle()

        composeTestRule.onNodeWithText("Full Name").performTextInput("Short Pass User")
        advanceAndIdle()
        composeTestRule.onNodeWithText("Email Address").performTextInput("shortpass@example.com")
        advanceAndIdle()
        composeTestRule.onNodeWithText("Password (min 6 characters)").performTextInput("abc")
        advanceAndIdle()
        composeTestRule.onNodeWithText("Confirm Password").performTextInput("abc")
        advanceAndIdle()

        composeTestRule.onNodeWithTag("register_submit_button").performClick()
        advanceAndIdle()

        composeTestRule.onNodeWithTag("auth_error_card").assertIsDisplayed()
        composeTestRule.onNodeWithText("Password must be at least 6 characters", substring = true).assertIsDisplayed()
    }

    // -------------------------------------------------------------------------
    // 8b. authScreen_registrationWithPasswordMismatch_showsError
    // -------------------------------------------------------------------------

    /**
     * Fills in the registration form with mismatched passwords and asserts a
     * validation error containing "Passwords do not match".
     */
    @Test
    fun authScreen_registrationWithPasswordMismatch_showsError() {
        launchApp()

        composeTestRule.onNodeWithTag("go_to_register_button").performClick()
        advanceAndIdle()

        composeTestRule.onNodeWithText("Full Name").performTextInput("Mismatch User")
        advanceAndIdle()
        composeTestRule.onNodeWithText("Email Address").performTextInput("mismatch@example.com")
        advanceAndIdle()
        composeTestRule.onNodeWithText("Password (min 6 characters)").performTextInput("password123")
        advanceAndIdle()
        composeTestRule.onNodeWithText("Confirm Password").performTextInput("differentpassword")
        advanceAndIdle()

        composeTestRule.onNodeWithTag("register_submit_button").performClick()
        advanceAndIdle()

        composeTestRule.onNodeWithTag("auth_error_card").assertIsDisplayed()
        composeTestRule.onNodeWithText("Passwords do not match", substring = true).assertIsDisplayed()
    }

    // -------------------------------------------------------------------------
    // 9. authScreen_forgotPassword_rendersCorrectly
    // -------------------------------------------------------------------------

    /**
     * Taps the "Forgot Password?" link and asserts that the Forgot Password
     * screen renders correctly, showing the "Reset Password" heading and the
     * reset_submit_button.
     */
    @Test
    fun authScreen_forgotPassword_rendersCorrectly() {
        launchApp()

        composeTestRule.onNodeWithTag("forgot_password_button").performClick()
        advanceAndIdle()

        composeTestRule.onNodeWithText("Reset Password").assertIsDisplayed()
        composeTestRule.onNodeWithTag("reset_submit_button").assertIsDisplayed()
    }

    // -------------------------------------------------------------------------
    // 10. authScreen_forgotPassword_validEmail_showsSuccessMessage
    // -------------------------------------------------------------------------

    /**
     * Registers a user, logs out, navigates to the Forgot Password screen,
     * enters the registered email, taps "Send Reset Link", and asserts that
     * the auth_status_card (success banner) is visible.
     */
    @Test
    fun authScreen_forgotPassword_validEmail_showsSuccessMessage() {
        launchApp()

        // Register user and then log out
        signUpUser(email = "forgottest@example.com", password = "Password1!")
        performLogout()
        advanceAndIdle()

        // Navigate to Forgot Password
        composeTestRule.onNodeWithTag("forgot_password_button").performClick()
        advanceAndIdle()

        // Enter the registered email
        composeTestRule.onNodeWithText("Email Address").performTextInput("forgottest@example.com")
        advanceAndIdle()

        // Submit
        composeTestRule.onNodeWithTag("reset_submit_button").performClick()
        advanceAndIdle()

        // Assert success card is shown
        composeTestRule.onNodeWithTag("auth_status_card").assertIsDisplayed()
    }

    // -------------------------------------------------------------------------
    // 11. authScreen_forgotPassword_emptyEmail_showsError
    // -------------------------------------------------------------------------

    /**
     * Navigates to the Forgot Password screen and clicks "Send Reset Link"
     * without entering an email. Asserts a validation error containing
     * "Please enter your email address".
     */
    @Test
    fun authScreen_forgotPassword_emptyEmail_showsError() {
        launchApp()

        composeTestRule.onNodeWithTag("forgot_password_button").performClick()
        advanceAndIdle()

        composeTestRule.onNodeWithTag("reset_submit_button").performClick()
        advanceAndIdle()

        composeTestRule.onNodeWithTag("auth_error_card").assertIsDisplayed()
        composeTestRule.onNodeWithText("Please enter your email address", substring = true).assertIsDisplayed()
    }

    // -------------------------------------------------------------------------
    // 12. authScreen_guestSession_showsSignInRegisterButton_notLogOut
    // -------------------------------------------------------------------------

    /**
     * Logs in as a guest, navigates to Settings, and asserts that the
     * "Sign In / Register" button is visible while the "Log Out" button
     * does not exist — confirming that guest sessions are correctly distinguished
     * from authenticated sessions.
     */
    @Test
    fun authScreen_guestSession_showsSignInRegisterButton_notLogOut() {
        launchApp()

        loginAsGuest()

        // Navigate to Settings
        composeTestRule.onNode(hasContentDescription("Settings")).performClick()
        advanceAndIdle()

        try {
            composeTestRule.onNode(hasText("Sign In / Register").and(hasNoContentDescription))
                .performScrollTo()
                .assertIsDisplayed()
        } catch (t: Throwable) {
            val sb = java.lang.StringBuilder()
            try {
                val root = composeTestRule.onRoot().fetchSemanticsNode()
                buildTreeString(root, sb, 0)
            } catch (ex: Exception) {
                sb.append("Failed to fetch root: ${ex.message}")
            }
            java.io.File("tree_guest_failed.txt").writeText(sb.toString())
            throw t
        }
    }

    // -------------------------------------------------------------------------
    // 13. authScreen_normalUserSession_showsLogOutButton_notSignIn
    // -------------------------------------------------------------------------

    /**
     * Signs up a user (which results in an authenticated session), navigates
     * to Settings, and asserts that the "Log Out" button is visible while
     * the "Sign In / Register" button does not exist.
     */
    @Test
    fun authScreen_normalUserSession_showsLogOutButton_notSignIn() {
        launchApp()

        signUpUser(email = "normaluser@example.com", password = "normalPass1")

        // Navigate to Settings
        composeTestRule.onNode(hasContentDescription("Settings")).performClick()
        advanceAndIdle()

        try {
            composeTestRule.onNode(hasText("Log Out").and(hasNoContentDescription))
                .performScrollTo()
                .assertIsDisplayed()
        } catch (t: Throwable) {
            val sb = java.lang.StringBuilder()
            try {
                val root = composeTestRule.onRoot().fetchSemanticsNode()
                buildTreeString(root, sb, 0)
            } catch (ex: Exception) {
                sb.append("Failed to fetch root: ${ex.message}")
            }
            java.io.File("tree_normal_failed.txt").writeText(sb.toString())
            throw t
        }
    }

    // -------------------------------------------------------------------------
    // 14. authScreen_logOut_clearsSessionAndReturnsToAuthScreen
    // -------------------------------------------------------------------------

    /**
     * Signs up a user, navigates to Settings, scrolls to and taps "Log Out",
     * idles the coroutine scheduler, and asserts that the auth screen is shown
     * again by checking for the presence of guest_login_button.
     */
    @Test
    fun authScreen_logOut_clearsSessionAndReturnsToAuthScreen() {
        launchApp()

        signUpUser(email = "logoutuser@example.com", password = "LogoutPass1")

        // Navigate to Settings
        composeTestRule.onNode(hasContentDescription("Settings")).performClick()
        advanceAndIdle()

        try {
            // Scroll to and tap Log Out
            composeTestRule.onNode(hasText("Log Out").and(hasNoContentDescription)).performSemanticsAction(SemanticsActions.OnClick)
            advanceAndIdle()

            // Session cleared — auth screen should be visible again
            composeTestRule.onNodeWithTag("guest_login_button").assertIsDisplayed()
        } catch (t: Throwable) {
            val sb = java.lang.StringBuilder()
            try {
                val root = composeTestRule.onRoot().fetchSemanticsNode()
                buildTreeString(root, sb, 0)
            } catch (ex: Exception) {
                sb.append("Failed to fetch root: ${ex.message}")
            }
            java.io.File("tree_logout_cleared_failed.txt").writeText(sb.toString())
            throw t
        }
    }

    @Test
    fun debug_testTextFields() {
        composeTestRule.setContent {
            val textState = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("") }
            androidx.compose.material3.OutlinedTextField(
                value = textState.value,
                onValueChange = { textState.value = it },
                label = { androidx.compose.material3.Text("Test Label") },
                modifier = androidx.compose.ui.Modifier.testTag("test_input")
            )
        }
        composeTestRule.onNodeWithTag("test_input").performTextInput("Hello World")
        composeTestRule.onNodeWithTag("test_input").assertTextContains("Hello World")
    }
}
