package com.example.data.local

import android.content.Context
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

    fun loadCategories(): List<Category> {
        return try {
            if (categoriesFile.exists()) {
                val json = categoriesFile.readText()
                categoryAdapter.fromJson(json) ?: emptyList()
            } else {
                val json = context.assets.open("categories.json").bufferedReader().use { it.readText() }
                val list = categoryAdapter.fromJson(json) ?: emptyList()
                saveCategories(list)
                list
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    fun saveCategories(categories: List<Category>) {
        try {
            val json = categoryAdapter.toJson(categories)
            categoriesFile.writeText(json)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadTransactions(): List<TransactionEntity> {
        return try {
            if (transactionsFile.exists()) {
                val json = transactionsFile.readText()
                transactionAdapter.fromJson(json) ?: emptyList()
            } else {
                val json = context.assets.open("transactions.json").bufferedReader().use { it.readText() }
                val list = transactionAdapter.fromJson(json) ?: emptyList()
                saveTransactions(list)
                list
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    fun saveTransactions(transactions: List<TransactionEntity>) {
        try {
            val json = transactionAdapter.toJson(transactions)
            transactionsFile.writeText(json)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
