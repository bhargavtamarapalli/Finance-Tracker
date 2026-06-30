package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.FinanceDao
import com.example.data.local.AppDatabase
import com.example.data.model.Category
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.system.measureTimeMillis
import org.junit.Assert.assertEquals
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class PerformanceTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: FinanceDao

    @Before
    fun createDb() {
        val context = RuntimeEnvironment.getApplication()
        db = Room.inMemoryDatabaseBuilder(
            context, AppDatabase::class.java).allowMainThreadQueries().build()
        dao = db.financeDao()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun benchmarkRestoreFromFirebase() = runBlocking {
        val transactions = (1..5000).map {
            TransactionEntity(
                id = it,
                amount = 10.0,
                categoryId = it % 50,
                date = System.currentTimeMillis(),
                notes = "Note $it",
                type = TransactionType.EXPENSE,
                source = "Test"
            )
        }

        val timeBaseline = measureTimeMillis {
            transactions.forEach {
                dao.insertTransaction(it)
            }
        }

        db.clearAllTables()

        val timeOptimized = measureTimeMillis {
            dao.insertTransactions(transactions)
        }

        println("===============================")
        println("Restoring ${transactions.size} transactions (forEach loop) took $timeBaseline ms")
        println("Restoring ${transactions.size} transactions (bulk insert) took $timeOptimized ms")
        println("===============================")

        assertEquals(5000, dao.getAllTransactionsOnce().size)
    }
}
