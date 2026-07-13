package com.example

import android.content.Context
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.compose.rememberNavController
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.JsonDataManager
import com.example.data.model.Category
import com.example.data.model.TransactionType
import com.example.data.repository.FinanceRepository
import com.example.ui.screens.CategoryManagementScreen
import com.example.ui.theme.FinanceTrackerTheme
import com.example.ui.viewmodel.FinanceViewModel
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import kotlinx.coroutines.runBlocking
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
class CategoryManagementScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var db: AppDatabase
    private lateinit var repository: FinanceRepository
    private lateinit var viewModel: FinanceViewModel

    @Before
    fun createDb() = runBlocking {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        val context = ApplicationProvider.getApplicationContext<Context>()
        val directExecutor = java.util.concurrent.Executor { it.run() }
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .setQueryExecutor(directExecutor)
            .setTransactionExecutor(directExecutor)
            .build()
        val jsonDataManager = JsonDataManager(context)
        repository = FinanceRepository(db.financeDao(), jsonDataManager)

        // Seed initial categories
        val incomeCat = Category(id = 16, name = "Salary", type = TransactionType.INCOME, iconName = "attach_money", isDefault = true)
        val expenseCat = Category(id = 2, name = "Groceries", type = TransactionType.EXPENSE, iconName = "shopping_cart", isDefault = true)
        db.financeDao().insertCategories(listOf(incomeCat, expenseCat))

        viewModel = FinanceViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun categoryManagement_displaysList() {
        composeTestRule.setContent {
            FinanceTrackerTheme {
                val navController = rememberNavController()
                CategoryManagementScreen(viewModel = viewModel, navController = navController)
            }
        }

        // Wait for Room background query to populate UI
        composeTestRule.waitUntil(5000) {
            composeTestRule.onAllNodesWithText("Groceries").fetchSemanticsNodes().isNotEmpty()
        }

        // Verify expense categories displays Groceries
        composeTestRule.onNodeWithText("Groceries").assertIsDisplayed()

        // Click Income Tab
        composeTestRule.onNodeWithText("Income").performClick()

        // Wait for Room background query to populate UI
        composeTestRule.waitUntil(5000) {
            composeTestRule.onAllNodesWithText("Salary").fetchSemanticsNodes().isNotEmpty()
        }

        // Verify income categories displays Salary
        composeTestRule.onNodeWithText("Salary").assertIsDisplayed()
    }

    @Test
    fun categoryManagement_addsNewCategory() {
        composeTestRule.setContent {
            FinanceTrackerTheme {
                val navController = rememberNavController()
                CategoryManagementScreen(viewModel = viewModel, navController = navController)
            }
        }

        // Wait for list to load
        composeTestRule.waitUntil(5000) {
            composeTestRule.onAllNodesWithText("Groceries").fetchSemanticsNodes().isNotEmpty()
        }

        // Click Add FAB
        composeTestRule.onNodeWithContentDescription("Add Category").performClick()
        ShadowLooper.idleMainLooper(1000, TimeUnit.MILLISECONDS)

        // Verify Dialog title
        composeTestRule.onNodeWithText("Create Custom Category").assertIsDisplayed()

        // Input new category name
        composeTestRule.onNodeWithText("Category Name").performTextInput("Coffee")
        composeTestRule.onNodeWithText("Category Name").performImeAction()

        // Select an icon from grid (e.g. restaurant)
        composeTestRule.onNodeWithText("Food").performClick()

        // Click Create button
        composeTestRule.onNodeWithText("Create").performClick()
        ShadowLooper.idleMainLooper(1000, TimeUnit.MILLISECONDS)

        // Wait for item to appear in the list
        composeTestRule.waitUntil(5000) {
            composeTestRule.onAllNodesWithText("Coffee").fetchSemanticsNodes().isNotEmpty()
        }

        // Verify Coffee category is added and visible
        composeTestRule.onNodeWithText("Coffee").assertIsDisplayed()
    }

    @Test
    fun categoryManagement_renamesCategory() {
        composeTestRule.setContent {
            FinanceTrackerTheme {
                val navController = rememberNavController()
                CategoryManagementScreen(viewModel = viewModel, navController = navController)
            }
        }

        // Wait for list to load
        composeTestRule.waitUntil(5000) {
            composeTestRule.onAllNodesWithText("Groceries").fetchSemanticsNodes().isNotEmpty()
        }

        // Long press chip to open menu and click Rename
        composeTestRule.onNodeWithText("Groceries").performTouchInput { longClick() }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Rename").performClick()
        composeTestRule.waitForIdle()
        ShadowLooper.idleMainLooper(1000, TimeUnit.MILLISECONDS)

        // Verify Dialog
        composeTestRule.onNodeWithText("Rename Category").assertIsDisplayed()

        // Clear text and input new name
        composeTestRule.onNodeWithText("Category Name").performTextReplacement("Household")
        composeTestRule.onNodeWithText("Category Name").performImeAction()

        // Click Save button
        composeTestRule.onNodeWithText("Save").performClick()
        ShadowLooper.idleMainLooper(1000, TimeUnit.MILLISECONDS)

        // Wait for update
        composeTestRule.waitUntil(5000) {
            composeTestRule.onAllNodesWithText("Household").fetchSemanticsNodes().isNotEmpty()
        }

        // Verify updated name
        composeTestRule.onNodeWithText("Household").assertIsDisplayed()
        composeTestRule.onNodeWithText("Groceries").assertDoesNotExist()
    }

    @Test
    fun categoryManagement_archivesAndUnarchivesCategory() {
        composeTestRule.setContent {
            FinanceTrackerTheme {
                val navController = rememberNavController()
                CategoryManagementScreen(viewModel = viewModel, navController = navController)
            }
        }

        // Wait for list to load
        composeTestRule.waitUntil(5000) {
            composeTestRule.onAllNodesWithText("Groceries").fetchSemanticsNodes().isNotEmpty()
        }

        // Long press chip to open menu and click Archive
        composeTestRule.onNodeWithText("Groceries").performTouchInput { longClick() }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Archive").performClick()
        composeTestRule.waitForIdle()

        // Wait for Archived label/badge to appear
        composeTestRule.waitUntil(5000) {
            composeTestRule.onAllNodesWithText("Archived").fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onNodeWithText("Archived").assertIsDisplayed()

        // Long press chip to open menu and click Unarchive
        composeTestRule.onNodeWithText("Groceries").performTouchInput { longClick() }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Unarchive").performClick()
        composeTestRule.waitForIdle()

        // Wait for Archived label/badge to disappear
        composeTestRule.waitUntil(5000) {
            composeTestRule.onAllNodesWithText("Archived").fetchSemanticsNodes().isEmpty()
        }

        composeTestRule.onNodeWithText("Archived").assertDoesNotExist()
    }
}
