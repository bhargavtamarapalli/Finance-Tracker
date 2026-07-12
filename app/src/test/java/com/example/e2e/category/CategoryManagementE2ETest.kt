package com.example.e2e.category

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
import androidx.compose.ui.semantics.getOrNull

/**
 * E2E tests for Category Management flows.
 *
 * Navigation path: Dashboard → Settings (bottom nav) → Manage Categories
 *
 * Key UI facts from CategoryManagementScreen.kt:
 * - Chips use long-press to open a DropdownMenu with "Rename" and "Archive"/"Unarchive" items.
 * - AddCategoryDialog has a TextField with label "Category Name" and shows
 *   "Category already exists" for duplicates.
 * - EditCategoryDialog confirms with a Save/Create button.
 * - contentDescription "Back" closes the screen.
 * - contentDescription "Add Category" is the FAB.
 *
 * Covers:
 * - UC-CAT-01: Create a new category and verify it appears in the list
 * - UC-CAT-02: Rename an existing category via long-press → Rename
 * - UC-CAT-03: Archive a category — it no longer shows in the active list
 * - UC-CAT-04: Duplicate category name shows "Category already exists" error
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w1000dp-h2000dp-xhdpi", sdk = [33])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CategoryManagementE2ETest {

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

        android.provider.Settings.Global.putFloat(
            context.contentResolver,
            android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 0f
        )
        android.provider.Settings.Global.putFloat(
            context.contentResolver,
            android.provider.Settings.Global.TRANSITION_ANIMATION_SCALE, 0f
        )
        android.provider.Settings.Global.putFloat(
            context.contentResolver,
            android.provider.Settings.Global.WINDOW_ANIMATION_SCALE, 0f
        )

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

    @After
    fun tearDown() {
        testDispatcher.scheduler.advanceUntilIdle()
        db.invalidationTracker.refreshVersionsSync()
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

    private fun advanceAndIdle() {
        composeTestRule.waitForIdle()
        testDispatcher.scheduler.advanceUntilIdle()
        db.invalidationTracker.refreshVersionsSync()
        ShadowLooper.idleMainLooper()
        composeTestRule.waitForIdle()
    }

    private fun launchApp() {
        composeTestRule.setContent {
            FinanceTrackerTheme {
                FinanceApp(viewModel = financeViewModel, authViewModel = authViewModel)
            }
        }
        bypassSplash()
    }

    private fun signUpUser(email: String, password: String, name: String) {
        authViewModel.signUp(email, password, name)
        testDispatcher.scheduler.advanceUntilIdle()
        db.invalidationTracker.refreshVersionsSync()
        ShadowLooper.idleMainLooper()
    }

    /**
     * Navigates from Dashboard to the CategoryManagementScreen via Settings.
     * Caller must call advanceAndIdle() after this if further state settling is needed.
     */
    private fun navigateToCategoryManagement() {
        composeTestRule.onNodeWithContentDescription("Settings").performClick()
        advanceAndIdle()
        composeTestRule.onNodeWithText("Manage Categories", substring = true)
            .performScrollTo()
        composeTestRule.onNodeWithText("Manage Categories", substring = true).performClick()
        advanceAndIdle()
    }

    // ──────────────────────────────────────────────────────────────────────────────
    // UC-CAT-01: Create new category
    // ──────────────────────────────────────────────────────────────────────────────

    /**
     * UC-CAT-01 — A newly created custom category appears in the category list.
     *
     * Given: User is on Category Management.
     * When:  User taps Add Category FAB, enters "Tea Expenses", taps Create.
     * Then:  "Tea Expenses" chip is visible in the list.
     */
    @Test
    fun category_createNew_appearsInList() {
        signUpUser("catcreate@example.com", "Password123", "Cat Create User")
        launchApp()
        navigateToCategoryManagement()

        // Open the add dialog
        composeTestRule.onNodeWithContentDescription("Add Category").performClick()
        advanceAndIdle()

        // Fill in category name
        composeTestRule.onNode(hasSetTextAction())
            .performTextInput("Tea Expenses")
        advanceAndIdle()

        // Save / Create
        composeTestRule.onNodeWithText("Create").performClick()
        advanceAndIdle()

        // Verify the new category is in the list
        composeTestRule.onNodeWithText("Tea Expenses", substring = true)
            .performScrollTo()
        composeTestRule.onNodeWithText("Tea Expenses", substring = true).assertIsDisplayed()
    }

    // ──────────────────────────────────────────────────────────────────────────────
    // UC-CAT-02: Rename existing category
    // ──────────────────────────────────────────────────────────────────────────────

    /**
     * UC-CAT-02 — Renaming a category via long-press → Rename updates the chip label.
     *
     * Given: "Groceries" category chip is visible.
     * When:  User long-presses chip, selects Rename, enters "Supermarket", saves.
     * Then:  "Supermarket" is displayed instead of "Groceries".
     */
    @Test
    fun category_rename_updatesListItem() {
        signUpUser("catrename@example.com", "Password123", "Cat Rename User")
        launchApp()
        navigateToCategoryManagement()

        // Long-press the Groceries chip to open the context menu
        composeTestRule.onNodeWithText("Groceries", substring = true)
            .performScrollTo()
        composeTestRule.onNodeWithText("Groceries", substring = true)
            .performTouchInput { longClick() }
        advanceAndIdle()

        // Select Rename from the dropdown
        composeTestRule.onNodeWithText("Rename").performClick()
        advanceAndIdle()

        // Clear and retype the name in the edit dialog
        composeTestRule.onNode(hasSetTextAction())
            .performTextClearance()
        composeTestRule.onNode(hasSetTextAction())
            .performTextInput("Supermarket")
        advanceAndIdle()

        // Save the rename
        composeTestRule.onNodeWithText("Save").performClick()
        advanceAndIdle()

        // Verify the updated name appears
        composeTestRule.onNodeWithText("Supermarket", substring = true)
            .performScrollTo()
        composeTestRule.onNodeWithText("Supermarket", substring = true).assertIsDisplayed()
    }

    // ──────────────────────────────────────────────────────────────────────────────
    // UC-CAT-03: Archive a category
    // ──────────────────────────────────────────────────────────────────────────────

    /**
     * UC-CAT-03 — Archiving a category removes it from the active chip grid.
     *
     * Given: A custom category "ArchiveTest" was created.
     * When:  User long-presses it and selects Archive.
     * Then:  "ArchiveTest" chip no longer appears in the active category list
     *        (it may appear with an "Archived" badge if the UI shows archived items separately,
     *        or disappear entirely depending on filter state).
     */
    @Test
    fun category_archive_removesFromActiveList() {
        signUpUser("catarchive@example.com", "Password123", "Cat Archive User")
        launchApp()
        navigateToCategoryManagement()

        // First create a category to archive
        composeTestRule.onNodeWithContentDescription("Add Category").performClick()
        advanceAndIdle()
        composeTestRule.onNode(hasSetTextAction())
            .performTextInput("ArchiveTest")
        composeTestRule.onNodeWithText("Create").performClick()
        advanceAndIdle()

        // Long-press the ArchiveTest chip
        composeTestRule.onNodeWithText("ArchiveTest", substring = true)
            .performScrollTo()
        composeTestRule.onNodeWithText("ArchiveTest", substring = true)
            .performTouchInput { longClick() }
        advanceAndIdle()

        // Tap Archive from dropdown
        composeTestRule.onNodeWithText("Archive").performClick()
        advanceAndIdle()

        // The chip should now display an "Archived" badge/label in the grid
        composeTestRule.onNodeWithText("Archived", substring = true).assertIsDisplayed()
    }

    // ──────────────────────────────────────────────────────────────────────────────
    // UC-CAT-04: Duplicate category name shows validation error
    // ──────────────────────────────────────────────────────────────────────────────

    /**
     * UC-CAT-04 — Attempting to create a category with a name that already exists
     * shows a "Category already exists" validation message.
     *
     * Given: "Groceries" already exists (seeded default).
     * When:  User opens AddCategoryDialog and types "Groceries".
     * Then:  The dialog shows "Category already exists" supporting text.
     */
    @Test
    fun category_createDuplicate_handledGracefully() {
        signUpUser("catdup@example.com", "Password123", "Cat Dup User")
        launchApp()
        navigateToCategoryManagement()

        // Open the add dialog
        composeTestRule.onNodeWithContentDescription("Add Category").performClick()
        advanceAndIdle()

        // Type an existing category name
        composeTestRule.onNode(hasSetTextAction())
            .performTextInput("Groceries")
        advanceAndIdle()

        // The duplicate error should appear immediately (driven by isDuplicate state)
        composeTestRule.onNodeWithText("Category already exists", substring = true)
            .assertIsDisplayed()
    }
}
