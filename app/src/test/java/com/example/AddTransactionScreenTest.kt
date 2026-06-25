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
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.data.repository.FinanceRepository
import com.example.ui.screens.AddTransactionScreen
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
class AddTransactionScreenTest {

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

        // Seed categories
        val incomeCat = Category(id = 7, name = "Salary", type = TransactionType.INCOME, iconName = "attach_money")
        val expenseCat = Category(id = 2, name = "Groceries", type = TransactionType.EXPENSE, iconName = "shopping_cart")
        db.financeDao().insertCategories(listOf(incomeCat, expenseCat))

        // Seed a transaction for edit/duplicate tests
        db.financeDao().insertTransaction(
            TransactionEntity(
                id = 2,
                amount = 4200.0,
                source = "Reliance Smart Supermarket",
                date = System.currentTimeMillis() - 5000,
                categoryId = 2,
                type = TransactionType.EXPENSE,
                notes = "Weekly grocery refill",
                paymentMethod = "Cash"
            )
        )

        viewModel = FinanceViewModel(repository)
    }

    @Test
    fun addTransaction_createsNewTransaction() {
        runBlocking {
            composeTestRule.setContent {
                FinanceTrackerTheme {
                    val navController = rememberNavController()
                    AddTransactionScreen(
                        viewModel = viewModel,
                        navController = navController,
                        initialType = "EXPENSE"
                    )
                }
            }

            composeTestRule.waitForIdle()

            // Input details
            composeTestRule.onNodeWithText("Amount").performTextInput("150.0")
            composeTestRule.onNodeWithText("Source / Merchant").performTextInput("Starbucks")
            composeTestRule.onNodeWithText("Notes (Optional)").performTextInput("Coffee with client")

            // Select Payment Method "Credit Card"
            composeTestRule.onNodeWithText("Credit Card").performClick()

            // Select Category "Groceries"
            composeTestRule.onNodeWithText("Groceries").performClick()

            // Click Save
            composeTestRule.onNodeWithText("Save").performClick()
            composeTestRule.waitForIdle()

            // Assert that the transaction was successfully saved with correct fields in DB
            val transactions = db.financeDao().getAllTransactions().first()
            val created = transactions.find { it.transaction.source == "Starbucks" }
            
            assert(created != null)
            assert(created!!.transaction.amount == 150.0)
            assert(created.transaction.notes == "Coffee with client")
            assert(created.transaction.paymentMethod == "Credit Card")
            assert(created.transaction.categoryId == 2)
        }
    }

    @Test
    fun addTransaction_editsExistingTransaction() {
        runBlocking {
            composeTestRule.setContent {
                FinanceTrackerTheme {
                    val navController = rememberNavController()
                    AddTransactionScreen(
                        viewModel = viewModel,
                        navController = navController,
                        initialType = "EXPENSE",
                        transactionId = 2,
                        isDuplicate = false
                    )
                }
            }

            // Wait for Room database query to populate form fields
            composeTestRule.waitUntil(5000) {
                composeTestRule.onAllNodesWithText("4200.0").fetchSemanticsNodes().isNotEmpty()
            }

            // Verify initial values populated
            composeTestRule.onNodeWithText("4200.0").assertIsDisplayed()
            composeTestRule.onNodeWithText("Reliance Smart Supermarket").assertIsDisplayed()
            composeTestRule.onNodeWithText("Weekly grocery refill").assertIsDisplayed()

            // Change amount, payment method, notes
            composeTestRule.onNodeWithText("Amount").performTextClearance()
            composeTestRule.onNodeWithText("Amount").performTextInput("4500.0")
            
            composeTestRule.onNodeWithText("UPI").performClick()
            
            composeTestRule.onNodeWithText("Notes (Optional)").performTextClearance()
            composeTestRule.onNodeWithText("Notes (Optional)").performTextInput("Updated grocery refill notes")

            // Click Update
            composeTestRule.onNodeWithText("Update").performClick()
            composeTestRule.waitForIdle()

            // Verify that the transaction in database has been updated
            val transactions = db.financeDao().getAllTransactions().first()
            val updated = transactions.find { it.transaction.id == 2 }

            assert(updated != null)
            assert(updated!!.transaction.amount == 4500.0)
            assert(updated.transaction.notes == "Updated grocery refill notes")
            assert(updated.transaction.paymentMethod == "UPI")
        }
    }

    @Test
    fun addTransaction_duplicatesTransaction() {
        runBlocking {
            composeTestRule.setContent {
                FinanceTrackerTheme {
                    val navController = rememberNavController()
                    AddTransactionScreen(
                        viewModel = viewModel,
                        navController = navController,
                        initialType = "EXPENSE",
                        transactionId = 2,
                        isDuplicate = true
                    )
                }
            }

            // Wait for Room database query to populate form fields
            composeTestRule.waitUntil(5000) {
                composeTestRule.onAllNodesWithText("4200.0").fetchSemanticsNodes().isNotEmpty()
            }

            // Verify "Save" button is displayed instead of "Update" when duplicating
            composeTestRule.onNodeWithText("Save").assertIsDisplayed()
            composeTestRule.onNodeWithText("Update").assertDoesNotExist()

            // Clear and input the duplicate source to avoid caret placement issues
            composeTestRule.onNodeWithText("Source / Merchant").performTextClearance()
            composeTestRule.onNodeWithText("Source / Merchant").performTextInput("Reliance Smart Supermarket (Duplicate)")

            // Click Save
            composeTestRule.onNodeWithText("Save").performClick()
            composeTestRule.waitForIdle()

            // Verify we have 2 transactions now in the database
            val transactions = db.financeDao().getAllTransactions().first()
            assert(transactions.size == 2)

            val original = transactions.find { it.transaction.id == 2 }
            val duplicate = transactions.find { it.transaction.source == "Reliance Smart Supermarket (Duplicate)" }

            assert(original != null)
            assert(duplicate != null)
            assert(duplicate!!.transaction.id != 2) // Must be a new ID
            assert(duplicate.transaction.amount == 4200.0)
            assert(duplicate.transaction.paymentMethod == "Cash")
        }
    }

    @Test
    fun addTransaction_invalidAmount_showsError() {
        runBlocking {
            composeTestRule.setContent {
                FinanceTrackerTheme {
                    val navController = rememberNavController()
                    AddTransactionScreen(
                        viewModel = viewModel,
                        navController = navController,
                        initialType = "EXPENSE"
                    )
                }
            }

            composeTestRule.waitForIdle()

            // Input invalid numeric amount
            composeTestRule.onNodeWithText("Amount").performTextInput("abc")
            composeTestRule.onNodeWithText("Source / Merchant").performTextInput("Starbucks")
            composeTestRule.onNodeWithText("Groceries").performClick()

            // Click Save
            composeTestRule.onNodeWithText("Save").performClick()
            composeTestRule.waitForIdle()

            // Assert error message is displayed
            composeTestRule.onNodeWithText("Please enter a valid numeric amount").assertIsDisplayed()
        }
    }

    @Test
    fun addTransaction_negativeAmount_showsError() {
        runBlocking {
            composeTestRule.setContent {
                FinanceTrackerTheme {
                    val navController = rememberNavController()
                    AddTransactionScreen(
                        viewModel = viewModel,
                        navController = navController,
                        initialType = "EXPENSE"
                    )
                }
            }

            composeTestRule.waitForIdle()

            // Input negative/zero amount
            composeTestRule.onNodeWithText("Amount").performTextInput("-10.0")
            composeTestRule.onNodeWithText("Source / Merchant").performTextInput("Starbucks")
            composeTestRule.onNodeWithText("Groceries").performClick()

            // Click Save
            composeTestRule.onNodeWithText("Save").performClick()
            composeTestRule.waitForIdle()

            // Assert error message is displayed
            composeTestRule.onNodeWithText("Amount must be greater than zero").assertIsDisplayed()
        }
    }
}
