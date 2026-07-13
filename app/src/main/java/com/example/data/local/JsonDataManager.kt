package com.example.data.local

import android.content.Context
import android.util.Log
import com.example.data.model.Category
import com.example.data.model.TransactionEntity
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.File

/**
 * Manages serialisation and persistence of [Category] and [TransactionEntity] lists
 * to encrypted JSON cache files on the device's internal storage.
 *
 * File I/O is delegated to [IFileStorage], which defaults to [EncryptedFileStorage]
 * (AES-256-GCM) in production. Tests inject [com.example.fakes.PlainFileStorage] to
 * avoid requiring the Android Keystore.
 *
 * @param context  Application context used to resolve [Context.getFilesDir] and assets.
 * @param storage  File I/O strategy. Defaults to [EncryptedFileStorage].
 */
class JsonDataManager(
    val context: Context,
    private val storage: IFileStorage = EncryptedFileStorage(context)
) {
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val categoryListType = Types.newParameterizedType(List::class.java, Category::class.java)
    private val categoryAdapter = moshi.adapter<List<Category>>(categoryListType)

    private val transactionListType = Types.newParameterizedType(List::class.java, TransactionEntity::class.java)
    private val transactionAdapter = moshi.adapter<List<TransactionEntity>>(transactionListType)

    private val categoriesFile = File(context.filesDir, "categories.json")
    private val transactionsFile = File(context.filesDir, "transactions.json")

    fun loadCategories(): List<Category> {
        return try {
            val json = storage.read(categoriesFile)
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
            storage.write(categoriesFile, json)
        } catch (e: Exception) {
            Log.e("JsonDataManager", "Failed to save categories", e)
        }
    }

    fun loadTransactions(): List<TransactionEntity> {
        return try {
            val json = storage.read(transactionsFile)
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
            storage.write(transactionsFile, json)
        } catch (e: Exception) {
            Log.e("JsonDataManager", "Failed to save transactions", e)
        }
    }

    fun clearLocalFiles() {
        categoriesFile.delete()
        transactionsFile.delete()
    }
}
