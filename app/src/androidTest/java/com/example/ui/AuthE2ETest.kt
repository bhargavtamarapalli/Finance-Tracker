package com.example.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.MainActivity
import com.example.data.local.AppDatabase
import com.example.data.local.EncryptedPrefsManager
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AuthE2ETest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        clearUserData()
    }

    @org.junit.After
    fun tearDown() {
        clearUserData()
    }

    private fun clearUserData() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        
        // Clear preferences
        val authPrefs = EncryptedPrefsManager.getEncryptedPrefs(context, "auth_prefs")
        authPrefs.edit().clear().commit()

        val settingsPrefs = EncryptedPrefsManager.getEncryptedPrefs(context, "settings_prefs")
        settingsPrefs.edit().clear().commit()
        
        // Clear database
        val db = AppDatabase.getDatabase(context)
        db.clearAllTables()
    }

    @Test
    fun testGuestLoginAndLogout() {
        // Wait for splash screen to complete and transition to AuthScreen (generous 20s timeout for slow emulators)
        composeTestRule.waitUntil(timeoutMillis = 20000) {
            composeTestRule.onAllNodesWithTag("guest_login_button").fetchSemanticsNodes().isNotEmpty()
        }

        // Scroll to guest login button and click it
        composeTestRule.onNodeWithTag("guest_login_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        // Wait for Dashboard to display and verify greeting
        composeTestRule.waitUntil(timeoutMillis = 15000) {
            composeTestRule.onAllNodesWithText("Hello, Guest", substring = true).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("Hello, Guest", substring = true).assertIsDisplayed()

        // Click Settings bottom navigation item directly
        composeTestRule.onNode(hasText("Settings") and hasAnyAncestor(hasTestTag("bottom_navigation_bar"))).performClick()
        composeTestRule.waitForIdle()

        // Wait for Settings screen to navigate and load
        composeTestRule.waitUntil(timeoutMillis = 15000) {
            composeTestRule.onAllNodesWithText("Guest Account").fetchSemanticsNodes().isNotEmpty()
        }

        // Verify guest account indicators are shown and "Log Out" button exists (scrolling to them first)
        composeTestRule.onNodeWithText("Guest Account").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("settings_logout_button").performScrollTo().assertIsDisplayed()

        // Click Log Out
        composeTestRule.onNodeWithTag("settings_logout_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        // Verify navigation back to AuthScreen
        composeTestRule.waitUntil(timeoutMillis = 15000) {
            composeTestRule.onAllNodesWithTag("guest_login_button").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag("guest_login_button").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun testUserSignUpAndProfileUpdate() {
        // Wait for splash screen (generous 20s timeout)
        composeTestRule.waitUntil(timeoutMillis = 20000) {
            composeTestRule.onAllNodesWithTag("go_to_register_button").fetchSemanticsNodes().isNotEmpty()
        }

        // Scroll to "Sign Up" / Register switcher and click it
        composeTestRule.onNodeWithTag("go_to_register_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        // Fill registration form
        composeTestRule.onNodeWithTag("name_input").performTextInput("John E2E")
        composeTestRule.onNodeWithTag("email_input").performTextInput("john.e2e@example.com")
        composeTestRule.onNodeWithTag("password_input").performTextInput("Password123")
        composeTestRule.onNodeWithTag("confirm_password_input").performTextInput("Password123")
        composeTestRule.waitForIdle()

        // Dismiss keyboard to ensure fields/buttons are not covered
        Espresso.closeSoftKeyboard()
        composeTestRule.waitForIdle()

        // Click Register (scrolling to it first)
        composeTestRule.onNodeWithTag("register_submit_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        // Wait for redirection to Dashboard and verify greeting
        composeTestRule.waitUntil(timeoutMillis = 15000) {
            composeTestRule.onAllNodesWithText("Hello, John", substring = true).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("Hello, John", substring = true).assertIsDisplayed()

        // Navigate to Settings directly using bottom navigation bar
        composeTestRule.onNode(hasText("Settings") and hasAnyAncestor(hasTestTag("bottom_navigation_bar"))).performClick()
        composeTestRule.waitForIdle()

        // Wait for Settings screen to navigate and load
        composeTestRule.waitUntil(timeoutMillis = 15000) {
            composeTestRule.onAllNodesWithText("Settings").fetchSemanticsNodes().isNotEmpty()
        }

        // Verify Name and Email are listed (scrolling to them first)
        composeTestRule.onNodeWithTag("profile_name_text", useUnmergedTree = true).performScrollTo().assertTextEquals("John E2E")
        composeTestRule.onNodeWithTag("profile_email_text", useUnmergedTree = true).performScrollTo().assertTextEquals("john.e2e@example.com")

        // Click Edit Profile Details
        composeTestRule.onNodeWithText("Edit Profile Details").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        // Change Name
        composeTestRule.onNodeWithText("Full Name").performTextReplacement("John Updated")
        Espresso.closeSoftKeyboard()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Save Changes").performClick()
        composeTestRule.waitForIdle()

        // Verify name updated in settings
        composeTestRule.waitUntil(timeoutMillis = 15000) {
            composeTestRule.onAllNodesWithTag("profile_name_text", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag("profile_name_text", useUnmergedTree = true).performScrollTo().assertTextEquals("John Updated")

        // Log Out
        composeTestRule.onNodeWithTag("settings_logout_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        // Verify we are back on AuthScreen
        composeTestRule.waitUntil(timeoutMillis = 15000) {
            composeTestRule.onAllNodesWithTag("login_submit_button").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag("login_submit_button").performScrollTo().assertIsDisplayed()
    }
}
