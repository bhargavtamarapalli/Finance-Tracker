package com.example.data.local

import android.content.Context
import android.util.Log
import com.example.data.model.Category
import com.example.data.model.TransactionEntity
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.File

class JsonDataManager(val context: Context) {
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val categoryListType = Types.newParameterizedType(List::class.java, Category::class.java)
    private val categoryAdapter = moshi.adapter<List<Category>>(categoryListType)

    private val transactionListType = Types.newParameterizedType(List::class.java, TransactionEntity::class.java)
    private val transactionAdapter = moshi.adapter<List<TransactionEntity>>(transactionListType)

    private val categoriesFile = File(context.filesDir, "categories.json")
    private val transactionsFile = File(context.filesDir, "transactions.json")

    private fun writeTextSecurely(file: File, text: String) {
        if (com.example.data.local.EncryptedPrefsManager.isTestEnvironment()) {
            file.writeText(text)
            return
        }
        try {
            if (file.exists()) {
                file.delete()
            }
            val masterKeyAlias = androidx.security.crypto.MasterKeys.getOrCreate(androidx.security.crypto.MasterKeys.AES256_GCM_SPEC)
            val encryptedFile = androidx.security.crypto.EncryptedFile.Builder(
                file,
                context,
                masterKeyAlias,
                androidx.security.crypto.EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
            ).build()
            encryptedFile.openFileOutput().use { outputStream ->
                outputStream.write(text.toByteArray(Charsets.UTF_8))
            }
        } catch (e: Exception) {
            Log.e("JsonDataManager", "Failed to write encrypted file: ${file.name}", e)
        }
    }

    private fun readTextSecurely(file: File): String? {
        if (!file.exists()) return null
        if (com.example.data.local.EncryptedPrefsManager.isTestEnvironment()) {
            return file.readText()
        }
        return try {
            val masterKeyAlias = androidx.security.crypto.MasterKeys.getOrCreate(androidx.security.crypto.MasterKeys.AES256_GCM_SPEC)
            val encryptedFile = androidx.security.crypto.EncryptedFile.Builder(
                file,
                context,
                masterKeyAlias,
                androidx.security.crypto.EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
            ).build()
            encryptedFile.openFileInput().use { inputStream ->
                inputStream.readBytes().toString(Charsets.UTF_8)
            }
        } catch (e: Exception) {
            Log.e("JsonDataManager", "Failed to read encrypted file: ${file.name}", e)
            null
        }
    }

    fun loadCategories(): List<Category> {
        return try {
            val json = readTextSecurely(categoriesFile)
            if (json != null) {
                categoryAdapter.fromJson(json) ?: emptyList()
            } else {
                val assetJson = context.assets.open("categories.json").bufferedReader().use { it.readText() }
                val list = categoryAdapter.fromJson(assetJson) ?: emptyList()
                saveCategories(list)
                list
            }
        } catch (e: Exception) {
            Log.e("JsonDataManager", "Failed to load categories", e)
            emptyList()
        }
    }

    fun saveCategories(categories: List<Category>) {
        try {
            val json = categoryAdapter.toJson(categories)
            writeTextSecurely(categoriesFile, json)
        } catch (e: Exception) {
            Log.e("JsonDataManager", "Failed to save categories", e)
        }
    }

    fun loadTransactions(): List<TransactionEntity> {
        return try {
            val json = readTextSecurely(transactionsFile)
            if (json != null) {
                transactionAdapter.fromJson(json) ?: emptyList()
            } else {
                val assetJson = context.assets.open("transactions.json").bufferedReader().use { it.readText() }
                val list = transactionAdapter.fromJson(assetJson) ?: emptyList()
                saveTransactions(list)
                list
            }
        } catch (e: Exception) {
            Log.e("JsonDataManager", "Failed to load transactions", e)
            emptyList()
        }
    }

    fun saveTransactions(transactions: List<TransactionEntity>) {
        try {
            val json = transactionAdapter.toJson(transactions)
            writeTextSecurely(transactionsFile, json)
        } catch (e: Exception) {
            Log.e("JsonDataManager", "Failed to save transactions", e)
        }
    }

    fun clearLocalFiles() {
        categoriesFile.delete()
        transactionsFile.delete()
    }
}
