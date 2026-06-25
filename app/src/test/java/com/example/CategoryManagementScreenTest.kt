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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class CategoryManagementScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var db: AppDatabase
    private lateinit var repository: FinanceRepository
    private lateinit var viewModel: FinanceViewModel

    @Before
    fun createDb() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val jsonDataManager = JsonDataManager(context)
        repository = FinanceRepository(db.financeDao(), jsonDataManager)

        // Seed initial categories
        val incomeCat = Category(id = 16, name = "Salary", type = TransactionType.INCOME, iconName = "attach_money", isDefault = true)
        val expenseCat = Category(id = 2, name = "Groceries", type = TransactionType.EXPENSE, iconName = "shopping_cart", isDefault = true)
        db.financeDao().insertCategories(listOf(incomeCat, expenseCat))

        viewModel = FinanceViewModel(repository)
    }

    @Test
    fun categoryManagement_displaysList() {
        runBlocking {
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
            composeTestRule.onNodeWithText("Income Categories").performClick()

            // Wait for Room background query to populate UI
            composeTestRule.waitUntil(5000) {
                composeTestRule.onAllNodesWithText("Salary").fetchSemanticsNodes().isNotEmpty()
            }

            // Verify income categories displays Salary
            composeTestRule.onNodeWithText("Salary").assertIsDisplayed()
        }
    }

    @Test
    fun categoryManagement_addsNewCategory() {
        runBlocking {
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

            // Verify Dialog title
            composeTestRule.onNodeWithText("Create Custom Category").assertIsDisplayed()

            // Input new category name
            composeTestRule.onNodeWithText("Category Name").performTextInput("Coffee")

            // Select an icon from grid (e.g. restaurant)
            composeTestRule.onNodeWithText("Food").performClick()

            // Click Create button
            composeTestRule.onNodeWithText("Create").performClick()

            // Wait for item to appear in the list
            composeTestRule.waitUntil(5000) {
                composeTestRule.onAllNodesWithText("Coffee").fetchSemanticsNodes().isNotEmpty()
            }

            // Verify Coffee category is added and visible
            composeTestRule.onNodeWithText("Coffee").assertIsDisplayed()
        }
    }

    @Test
    fun categoryManagement_renamesCategory() {
        runBlocking {
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

            // Click Rename/Edit icon
            composeTestRule.onNodeWithContentDescription("Rename Category").performClick()

            // Verify Dialog
            composeTestRule.onNodeWithText("Rename Category").assertIsDisplayed()

            // Clear text and input new name
            composeTestRule.onNodeWithText("Category Name").performTextReplacement("Household")

            // Click Save button
            composeTestRule.onNodeWithText("Save").performClick()

            // Wait for update
            composeTestRule.waitUntil(5000) {
                composeTestRule.onAllNodesWithText("Household").fetchSemanticsNodes().isNotEmpty()
            }

            // Verify updated name
            composeTestRule.onNodeWithText("Household").assertIsDisplayed()
            composeTestRule.onNodeWithText("Groceries").assertDoesNotExist()
        }
    }

    @Test
    fun categoryManagement_archivesAndUnarchivesCategory() {
        runBlocking {
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

            // Click Archive button
            composeTestRule.onNodeWithContentDescription("Archive Category").performClick()

            // Wait for Archived label/badge to appear
            composeTestRule.waitUntil(5000) {
                composeTestRule.onAllNodesWithText("Archived").fetchSemanticsNodes().isNotEmpty()
            }

            composeTestRule.onNodeWithText("Archived").assertIsDisplayed()

            // Click Unarchive button
            composeTestRule.onNodeWithContentDescription("Unarchive Category").performClick()

            // Wait for Archived label/badge to disappear
            composeTestRule.waitUntil(5000) {
                composeTestRule.onAllNodesWithText("Archived").fetchSemanticsNodes().isEmpty()
            }

            composeTestRule.onNodeWithText("Archived").assertDoesNotExist()
        }
    }
}
