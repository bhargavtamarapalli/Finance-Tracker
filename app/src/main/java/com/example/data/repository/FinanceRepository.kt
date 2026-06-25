package com.example.data.repository

import com.example.data.local.FinanceDao
import com.example.data.local.JsonDataManager
import com.example.data.model.Category
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.data.model.TransactionWithCategory
import com.example.data.model.BackupPayload
import kotlinx.coroutines.flow.Flow
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.File

class FinanceRepository(
    private val dao: FinanceDao,
    private val jsonDataManager: JsonDataManager
) {
    private val client = OkHttpClient()
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

    suspend fun seedDataIfNeeded() {
        val categories = dao.getAllCategoriesOnce()
        if (categories.isEmpty()) {
            val seededCategories = jsonDataManager.loadCategories()
            dao.insertCategories(seededCategories)
        }
        val transactions = dao.getAllTransactionsOnce()
        if (transactions.size <= 7) {
            // Delete existing skeleton transactions
            transactions.forEach { dao.deleteTransaction(it) }
            val seededTransactions = jsonDataManager.loadTransactions()
            seededTransactions.forEach { dao.insertTransaction(it) }
        }
    }

    suspend fun backupToFirebase(userId: String): Result<Unit> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            val categories = dao.getAllCategoriesOnce()
            val transactions = dao.getAllTransactionsOnce()
            val payload = BackupPayload(categories, transactions)
            val json = backupAdapter.toJson(payload)
            
            val url = "https://finance-tracker-placeholder-default-rtdb.firebaseio.com/users/$userId/backup.json"
            val body = json.toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url(url)
                .put(body)
                .build()
                
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("Cloud backup returned error code: ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun restoreFromFirebase(userId: String): Result<Unit> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            val url = "https://finance-tracker-placeholder-default-rtdb.firebaseio.com/users/$userId/backup.json"
            val request = Request.Builder()
                .url(url)
                .get()
                .build()
                
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("Cloud restore returned error code: ${response.code}"))
                }
                val json = response.body?.string()
                if (json.isNullOrEmpty() || json == "null") {
                    return@withContext Result.failure(Exception("No cloud backup found for this account."))
                }
                val payload = backupAdapter.fromJson(json)
                    ?: return@withContext Result.failure(Exception("Failed to parse cloud backup data."))
                
                // Clear existing and overwrite/update database
                dao.insertCategories(payload.categories)
                payload.transactions.forEach {
                    dao.insertTransaction(it)
                }
                
                // Save to local JSON files to keep fully in sync
                jsonDataManager.saveCategories(dao.getAllCategoriesOnce())
                jsonDataManager.saveTransactions(dao.getAllTransactionsOnce())
                
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

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
            
            dao.insertCategories(payload.categories)
            payload.transactions.forEach {
                dao.insertTransaction(it)
            }
            
            jsonDataManager.saveCategories(dao.getAllCategoriesOnce())
            jsonDataManager.saveTransactions(dao.getAllTransactionsOnce())
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
