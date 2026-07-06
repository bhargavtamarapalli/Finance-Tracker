package com.example.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.JsonDataManager
import com.example.data.model.Category
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FinanceRepositoryTest {

    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var jsonDataManager: JsonDataManager
    private lateinit var repository: FinanceRepository
    private lateinit var localBackupFile: File

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .setQueryExecutor { it.run() }
            .setTransactionExecutor { it.run() }
            .build()
        jsonDataManager = JsonDataManager(context)
        repository = FinanceRepository(db.financeDao(), jsonDataManager)
        
        localBackupFile = File(context.filesDir, "local_backup.json")
        if (localBackupFile.exists()) {
            localBackupFile.delete()
        }
    }

    @After
    fun tearDown() {
        db.close()
        if (localBackupFile.exists()) {
            localBackupFile.delete()
        }
    }

    @Test
    fun testSeedDataIfNeeded_whenEmpty_insertsCategoriesAndTransactions() = runBlocking {
        // Initially empty
        val initialCategories = db.financeDao().getAllCategoriesOnce()
        val initialTransactions = db.financeDao().getAllTransactionsOnce()
        assertTrue(initialCategories.isEmpty())
        assertTrue(initialTransactions.isEmpty())

        repository.seedDataIfNeeded()

        val seededCategories = db.financeDao().getAllCategoriesOnce()
        val seededTransactions = db.financeDao().getAllTransactionsOnce()
        assertFalse(seededCategories.isEmpty())
        assertFalse(seededTransactions.isEmpty())
    }

    @Test
    fun testSeedDataIfNeeded_whenNotEmpty_doesNotOverwrite() = runBlocking {
        repository.seedDataIfNeeded()
        
        val categoryCount = db.financeDao().getAllCategoriesOnce().size
        val transactionCount = db.financeDao().getAllTransactionsOnce().size

        // Add a custom category
        db.financeDao().insertCategories(listOf(Category(name = "Custom", type = TransactionType.EXPENSE, iconName = "store")))
        
        repository.seedDataIfNeeded()

        // Count should have increased by 1 and not reset
        assertEquals(categoryCount + 1, db.financeDao().getAllCategoriesOnce().size)
    }

    @Test
    fun testLocalBackupAndRestore_success() = runBlocking {
        // Seed database
        repository.seedDataIfNeeded()
        val originalCategories = db.financeDao().getAllCategoriesOnce()
        val originalTransactions = db.financeDao().getAllTransactionsOnce()

        // Backup
        val backupResult = repository.backupLocally()
        assertTrue(backupResult.isSuccess)
        assertTrue(localBackupFile.exists())
        assertTrue(localBackupFile.length() > 0)

        // Clear DB
        db.financeDao().deleteTransactions(originalTransactions)
        assertTrue(db.financeDao().getAllTransactionsOnce().isEmpty())

        // Restore
        val restoreResult = repository.restoreLocally()
        assertTrue(restoreResult.isSuccess)

        // Verify content restored
        val restoredTransactions = db.financeDao().getAllTransactionsOnce()
        assertEquals(originalTransactions.size, restoredTransactions.size)
    }

    @Test
    fun testRestoreLocally_noBackupFile_returnsFailure() = runBlocking {
        assertFalse(localBackupFile.exists())
        val restoreResult = repository.restoreLocally()
        assertTrue(restoreResult.isFailure)
        assertEquals("No local backup file found. Please create a backup first.", restoreResult.exceptionOrNull()?.message)
    }
}
