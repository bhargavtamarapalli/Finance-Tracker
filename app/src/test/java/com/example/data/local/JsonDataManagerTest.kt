package com.example.data.local

import android.content.Context
import android.content.res.AssetManager
import com.example.data.model.Category
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import io.mockk.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream
import java.io.File

class JsonDataManagerTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testSaveAndLoadCategories_fromFile() {
        val filesDir = tempFolder.newFolder()
        val mockContext = mockk<Context>()
        every { mockContext.filesDir } returns filesDir

        val jsonDataManager = JsonDataManager(mockContext)
        val categories = listOf(
            Category(id = 1, name = "Food", type = TransactionType.EXPENSE, iconName = "restaurant"),
            Category(id = 2, name = "Salary", type = TransactionType.INCOME, iconName = "attach_money")
        )

        jsonDataManager.saveCategories(categories)

        val loaded = jsonDataManager.loadCategories()
        assertEquals(2, loaded.size)
        assertEquals("Food", loaded[0].name)
        assertEquals("Salary", loaded[1].name)
    }

    @Test
    fun testLoadCategories_fromAssetsWhenFileDoesNotExist() {
        val filesDir = tempFolder.newFolder()
        val mockContext = mockk<Context>()
        val mockAssetManager = mockk<AssetManager>()

        every { mockContext.filesDir } returns filesDir
        every { mockContext.assets } returns mockAssetManager

        val dummyJson = """
            [
              {"id": 1, "name": "Food", "type": "EXPENSE", "iconName": "restaurant"}
            ]
        """.trimIndent()
        every { mockAssetManager.open("categories.json") } returns ByteArrayInputStream(dummyJson.toByteArray())

        val jsonDataManager = JsonDataManager(mockContext)
        val loaded = jsonDataManager.loadCategories()

        assertEquals(1, loaded.size)
        assertEquals("Food", loaded[0].name)
    }

    @Test
    fun testSaveAndLoadTransactions_fromFile() {
        val filesDir = tempFolder.newFolder()
        val mockContext = mockk<Context>()
        every { mockContext.filesDir } returns filesDir

        val jsonDataManager = JsonDataManager(mockContext)
        val transactions = listOf(
            TransactionEntity(id = 1, amount = 150.0, source = "Local", date = 123456789L, categoryId = 1, type = TransactionType.EXPENSE, notes = "Lunch"),
            TransactionEntity(id = 2, amount = 5000.0, source = "Local", date = 123456790L, categoryId = 2, type = TransactionType.INCOME, notes = "Salary payout")
        )

        jsonDataManager.saveTransactions(transactions)

        val loaded = jsonDataManager.loadTransactions()
        assertEquals(2, loaded.size)
        assertEquals(150.0, loaded[0].amount, 0.001)
        assertEquals("Salary payout", loaded[1].notes)
    }

    @Test
    fun testLoadTransactions_fromAssetsWhenFileDoesNotExist() {
        val filesDir = tempFolder.newFolder()
        val mockContext = mockk<Context>()
        val mockAssetManager = mockk<AssetManager>()

        every { mockContext.filesDir } returns filesDir
        every { mockContext.assets } returns mockAssetManager

        val dummyJson = """
            [
              {"id": 1, "amount": 100.0, "source": "Local", "categoryId": 1, "date": 12345, "notes": "Snacks", "type": "EXPENSE"}
            ]
        """.trimIndent()
        every { mockAssetManager.open("transactions.json") } returns ByteArrayInputStream(dummyJson.toByteArray())

        val jsonDataManager = JsonDataManager(mockContext)
        val loaded = jsonDataManager.loadTransactions()

        assertEquals(1, loaded.size)
        assertEquals("Snacks", loaded[0].notes)
    }
}
