package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.system.measureTimeMillis

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PerformanceTest {

    private lateinit var db: AppDatabase

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun benchmarkBatchVsLoop() = runBlocking {
        val dao = db.financeDao()
        val numTransactions = 1000
        val transactions = (1..numTransactions).map { i ->
            TransactionEntity(
                id = i,
                amount = 10.0 + i,
                source = "Source $i",
                date = System.currentTimeMillis(),
                categoryId = 1,
                type = TransactionType.EXPENSE,
                notes = "Note $i"
            )
        }

        // Measure Loop performance
        val loopTime = measureTimeMillis {
            transactions.forEach { dao.insertTransaction(it) }
        }

        // Clean up
        dao.getAllTransactionsOnce().forEach { dao.deleteTransaction(it) }

        // Measure Batch performance
        val batchTime = measureTimeMillis {
            dao.insertTransactions(transactions)
        }

        println("Performance Benchmark:")
        println("Loop Insert Time: ${loopTime}ms")
        println("Batch Insert Time: ${batchTime}ms")
        if (loopTime > 0) {
            println("Improvement: ${((loopTime - batchTime).toDouble() / loopTime * 100).toInt()}%")
        }
    }
}
