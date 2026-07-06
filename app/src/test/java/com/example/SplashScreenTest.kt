package com.example

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.ui.screens.SplashScreen
import com.example.ui.theme.FinanceTrackerTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [33])
class SplashScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun splashScreen_displaysLogoAndText() {
        var isComplete = false

        composeTestRule.setContent {
            FinanceTrackerTheme {
                SplashScreen(onAnimationComplete = { isComplete = true })
            }
        }

        // Verify splash screen elements exist using testTags
        composeTestRule.onNodeWithTag("splash_screen").assertExists().assertIsDisplayed()
        composeTestRule.onNodeWithTag("splash_logo").assertExists().assertIsDisplayed()
        composeTestRule.onNodeWithTag("splash_title").assertExists().assertIsDisplayed()
        composeTestRule.onNodeWithTag("splash_tagline").assertExists().assertIsDisplayed()

        // Verify correct app title string is displayed on screen
        composeTestRule.onNodeWithText("Finance Tracker").assertIsDisplayed()
    }
}
