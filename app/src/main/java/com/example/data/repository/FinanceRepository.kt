package com.example.data.repository

import com.example.data.local.FinanceDao
import com.example.data.local.JsonDataManager
import com.example.data.model.Category
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.data.model.TransactionWithCategory
import com.example.data.model.BackupPayload
import com.example.admin.data.model.AdminSystemStats
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
        validateTransaction(transaction)
        dao.insertTransaction(transaction)
        val allTx = dao.getAllTransactionsOnce()
        jsonDataManager.saveTransactions(allTx)
    }

    suspend fun updateTransaction(transaction: TransactionEntity) {
        validateTransaction(transaction)
        dao.updateTransaction(transaction)
        val allTx = dao.getAllTransactionsOnce()
        jsonDataManager.saveTransactions(allTx)
    }

    suspend fun deleteTransaction(transaction: TransactionEntity) {
        dao.deleteTransaction(transaction)
        val allTx = dao.getAllTransactionsOnce()
        jsonDataManager.saveTransactions(allTx)
    }

    private fun validateTransaction(transaction: TransactionEntity) {
        if (transaction.amount <= 0) {
            throw IllegalArgumentException("Transaction amount must be greater than zero")
        }
        if (transaction.amount > 10_000_000_000.0) {
            throw IllegalArgumentException("Transaction amount exceeds maximum allowed value")
        }
        if (transaction.categoryId <= 0) {
            throw IllegalArgumentException("Invalid category ID")
        }
        if (transaction.date <= 0) {
            throw IllegalArgumentException("Invalid transaction date")
        }
        if (transaction.source.isBlank()) {
            throw IllegalArgumentException("Transaction source cannot be blank")
        }
        if (transaction.source.length > 100) {
            throw IllegalArgumentException("Transaction source exceeds maximum length")
        }
    }

    suspend fun getAllCategoriesOnce(): List<Category> {
        return dao.getAllCategoriesOnce()
    }

    suspend fun getAllTransactionsOnce(): List<TransactionEntity> {
        return dao.getAllTransactionsOnce()
    }

    /**
     * Seeds the initial categories from the JSON asset cache if the database is empty.
     * This is a safe, idempotent operation — it does nothing if categories already exist.
     * Tests should pre-populate their in-memory database directly rather than relying
     * on this method to accept a test-specific flag.
     */
    suspend fun seedDataIfNeeded() {
        val categories = dao.getAllCategoriesOnce()
        if (categories.isEmpty()) {
            val seededCategories = jsonDataManager.loadCategories()
            dao.insertCategories(seededCategories)
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
            
            // Encrypt the backup data
            val encryptedData = encryptData(json, jsonDataManager.context)
            localBackupFile.writeBytes(encryptedData)
            
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
            val encryptedData = localBackupFile.readBytes()
            val json = decryptData(encryptedData, jsonDataManager.context)
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

    private fun encryptData(data: String, context: android.content.Context): ByteArray {
        val key = getOrCreateEncryptionKey(context)
        val cipher = javax.crypto.Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, key)
        val iv = cipher.iv
        val encrypted = cipher.doFinal(data.toByteArray(Charsets.UTF_8))
        return iv + encrypted
    }

    private fun decryptData(encryptedData: ByteArray, context: android.content.Context): String {
        val key = getOrCreateEncryptionKey(context)
        val cipher = javax.crypto.Cipher.getInstance("AES/GCM/NoPadding")
        val ivSize = 12 // GCM standard IV size
        val iv = encryptedData.copyOfRange(0, ivSize)
        val encrypted = encryptedData.copyOfRange(ivSize, encryptedData.size)
        
        val spec = javax.crypto.spec.GCMParameterSpec(128, iv)
        cipher.init(javax.crypto.Cipher.DECRYPT_MODE, key, spec)
        val decrypted = cipher.doFinal(encrypted)
        return String(decrypted, Charsets.UTF_8)
    }

    private fun getOrCreateEncryptionKey(context: android.content.Context): javax.crypto.SecretKey {
        val prefs = context.getSharedPreferences("backup_encryption", android.content.Context.MODE_PRIVATE)
        val keyString = prefs.getString("encryption_key", null)
        
        if (keyString != null) {
            val keyBytes = android.util.Base64.decode(keyString, android.util.Base64.DEFAULT)
            return javax.crypto.spec.SecretKeySpec(keyBytes, "AES")
        }
        
        // Generate new key
        val keyGenerator = javax.crypto.KeyGenerator.getInstance("AES")
        keyGenerator.init(256)
        val key = keyGenerator.generateKey()
        val keyStringEncoded = android.util.Base64.encodeToString(key.encoded, android.util.Base64.DEFAULT)
        prefs.edit().putString("encryption_key", keyStringEncoded).apply()
        return key
    }

    /**
     * Aggregates platform-wide metrics. Runs on Dispatchers.IO.
     * Note: User statistics require backend integration. Currently returns local data only.
     */
    suspend fun getSystemStats(announcementsCount: Int): AdminSystemStats = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val totalTransactions = dao.getTotalTransactionCount()
        val totalCategories = dao.getTotalCategoryCount()
        AdminSystemStats(
            totalUsers = 0, // Requires backend integration
            activeUsers = 0, // Requires backend integration
            suspendedUsers = 0, // Requires backend integration
            totalTransactions = totalTransactions,
            totalCategories = totalCategories,
            announcementsCount = announcementsCount,
            snapshotAt = System.currentTimeMillis()
        )
    }

    /**
     * Exports current metrics to a local file. Runs on Dispatchers.IO.
     */
    suspend fun exportSystemReport(stats: AdminSystemStats): Result<File> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            val reportFile = File(jsonDataManager.context.filesDir, "admin_report_${System.currentTimeMillis()}.json")
            val adapter = moshi.adapter(AdminSystemStats::class.java)
            val json = adapter.toJson(stats)
            reportFile.writeText(json)
            Result.success(reportFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
