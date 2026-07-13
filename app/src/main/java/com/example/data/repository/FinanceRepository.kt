package com.example.data.repository

import com.example.data.local.FinanceDao
import com.example.data.local.JsonDataManager
import com.example.data.model.Category
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.data.model.TransactionWithCategory
import com.example.data.model.BackupPayload
import kotlinx.coroutines.flow.Flow
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.File

class FinanceRepository(
    private val dao: FinanceDao,
    private val jsonDataManager: JsonDataManager
) {
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val backupAdapter = moshi.adapter(BackupPayload::class.java)
    private val localBackupFile = File(jsonDataManager.context.filesDir, "local_backup.json")

    val allCategories: Flow<List<Category>> = dao.getAllCategories()
    val allTransactions: Flow<List<TransactionWithCategory>> = dao.getAllTransactions()

    fun getSettingsPreferences(): android.content.SharedPreferences {
        return com.example.data.local.EncryptedPrefsManager.getEncryptedPrefs(jsonDataManager.context, "settings_prefs")
    }

    fun getContext(): android.content.Context {
        return jsonDataManager.context
    }

    fun getCategoriesByType(type: TransactionType) = dao.getCategoriesByType(type)
    fun getTransactionsByDateRange(start: Long, end: Long) = dao.getTransactionsByDateRange(start, end)

    suspend fun insertCategory(category: Category) {
        dao.insertCategory(category)
        val allCat = dao.getAllCategoriesOnce()
        jsonDataManager.saveCategories(allCat)
    }

    suspend fun updateCategory(category: Category) {
        dao.updateCategory(category)
        val allCat = dao.getAllCategoriesOnce()
        jsonDataManager.saveCategories(allCat)
    }

    suspend fun deleteCategory(category: Category) {
        dao.deleteCategory(category)
        val allCat = dao.getAllCategoriesOnce()
        jsonDataManager.saveCategories(allCat)
    }

    suspend fun insertCategories(categories: List<Category>) {
        dao.insertCategories(categories)
        val allCat = dao.getAllCategoriesOnce()
        jsonDataManager.saveCategories(allCat)
    }

    suspend fun insertTransaction(transaction: TransactionEntity) {
        dao.insertTransaction(transaction)
        val allTx = dao.getAllTransactionsOnce()
        jsonDataManager.saveTransactions(allTx)
    }

    suspend fun updateTransaction(transaction: TransactionEntity) {
        dao.updateTransaction(transaction)
        val allTx = dao.getAllTransactionsOnce()
        jsonDataManager.saveTransactions(allTx)
    }

    suspend fun deleteTransaction(transaction: TransactionEntity) {
        dao.deleteTransaction(transaction)
        val allTx = dao.getAllTransactionsOnce()
        jsonDataManager.saveTransactions(allTx)
    }

    suspend fun getAllCategoriesOnce(): List<Category> {
        return dao.getAllCategoriesOnce()
    }

    suspend fun getAllTransactionsOnce(): List<TransactionEntity> {
        return dao.getAllTransactionsOnce()
    }

    suspend fun seedDataIfNeeded(isTestEnv: Boolean = false) {
        val categories = dao.getAllCategoriesOnce()
        if (categories.isEmpty()) {
            val seededCategories = jsonDataManager.loadCategories()
            dao.insertCategories(seededCategories)
        }
        if (isTestEnv) {
            val transactions = dao.getAllTransactionsOnce()
            if (transactions.size <= 7) {
                dao.deleteTransactions(transactions)
                val seededTransactions = jsonDataManager.loadTransactions()
                dao.insertTransactions(seededTransactions)
            }
        }
    }

    suspend fun seedDemoTransactionsOnly() {
        val seededCategories = jsonDataManager.loadCategories()
        dao.insertCategories(seededCategories)
        val seededTransactions = jsonDataManager.loadTransactions()
        dao.insertTransactions(seededTransactions)
    }

    suspend fun clearAllData() {
        dao.deleteAllTransactions()
        dao.deleteAllCategories()
    }

    suspend fun backupToFirebase(userId: String): Result<Unit> =
        Result.failure(UnsupportedOperationException("Cloud backup is unavailable until authenticated sync is configured."))

    suspend fun restoreFromFirebase(userId: String): Result<Unit> =
        Result.failure(UnsupportedOperationException("Cloud restore is unavailable until authenticated sync is configured."))

    suspend fun backupLocally(): Result<Unit> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            val categories = dao.getAllCategoriesOnce()
            val transactions = dao.getAllTransactionsOnce()
            val payload = BackupPayload(categories, transactions)
            val json = backupAdapter.toJson(payload)
            localBackupFile.writeText(json)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun restoreLocally(): Result<Unit> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            if (!localBackupFile.exists()) {
                return@withContext Result.failure(Exception("No local backup file found. Please create a backup first."))
            }
            val json = localBackupFile.readText()
            val payload = backupAdapter.fromJson(json)
                ?: return@withContext Result.failure(Exception("Failed to parse local backup file."))
            
            dao.deleteAllTransactions()
            dao.deleteAllCategories()
            dao.insertCategories(payload.categories)
            dao.insertTransactions(payload.transactions)
            
            jsonDataManager.saveCategories(dao.getAllCategoriesOnce())
            jsonDataManager.saveTransactions(dao.getAllTransactionsOnce())
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
