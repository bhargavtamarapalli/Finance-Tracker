package com.example.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
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
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        
        // Clear preferences
        val prefs = EncryptedPrefsManager.getEncryptedPrefs(context, "auth_prefs")
        prefs.edit().clear().commit()
        
        // Clear database
        val db = AppDatabase.getDatabase(context)
        db.clearAllTables()
    }

    @Test
    fun testGuestLoginAndLogout() {
        // Wait for splash screen to complete and transition to AuthScreen
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithTag("guest_login_button").fetchSemanticsNodes().isNotEmpty()
        }

        // Click guest login
        composeTestRule.onNodeWithTag("guest_login_button").performClick()

        // Verify Dashboard displays Guest greeting
        composeTestRule.onNodeWithText("Hello, Guest", substring = true).assertIsDisplayed()

        // Open drawer menu
        composeTestRule.onNodeWithContentDescription("Menu").performClick()

        // Click Settings drawer item
        composeTestRule.onNodeWithContentDescription("Settings").performClick()

        // Verify guest account indicators are shown and "Log Out" button exists
        composeTestRule.onNodeWithText("Guest Account", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Log Out").assertIsDisplayed()

        // Click Log Out
        composeTestRule.onNodeWithText("Log Out").performClick()

        // Verify navigation back to AuthScreen
        composeTestRule.onNodeWithTag("guest_login_button").assertIsDisplayed()
    }

    @Test
    fun testUserSignUpAndProfileUpdate() {
        // Wait for splash screen
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithTag("go_to_register_button").fetchSemanticsNodes().isNotEmpty()
        }

        // Go to registration mode
        composeTestRule.onNodeWithTag("go_to_register_button").performClick()

        // Fill registration form
        composeTestRule.onNodeWithTag("name_input").performTextInput("John E2E")
        composeTestRule.onNodeWithTag("email_input").performTextInput("john.e2e@example.com")
        composeTestRule.onNodeWithTag("password_input").performTextInput("Password123")
        composeTestRule.onNodeWithTag("confirm_password_input").performTextInput("Password123")

        // Click Register
        composeTestRule.onNodeWithTag("register_submit_button").performClick()

        // Verify redirected to Dashboard
        composeTestRule.onNodeWithText("Hello, John", substring = true).assertIsDisplayed()

        // Navigate to Settings
        composeTestRule.onNodeWithContentDescription("Menu").performClick()
        composeTestRule.onNodeWithContentDescription("Settings").performClick()

        // Verify Name and Email are listed
        composeTestRule.onNodeWithText("John E2E").assertIsDisplayed()
        composeTestRule.onNodeWithText("john.e2e@example.com").assertIsDisplayed()

        // Click Edit Profile Details
        composeTestRule.onNodeWithText("Edit Profile Details").performClick()

        // Change Name
        composeTestRule.onNodeWithText("Full Name").performTextReplacement("John Updated")
        composeTestRule.onNodeWithText("Save Changes").performClick()

        // Verify name updated in settings
        composeTestRule.onNodeWithText("John Updated").assertIsDisplayed()

        // Log Out
        composeTestRule.onNodeWithText("Log Out").performClick()

        // Verify we are back on AuthScreen
        composeTestRule.onNodeWithTag("login_submit_button").assertIsDisplayed()
    }
}
