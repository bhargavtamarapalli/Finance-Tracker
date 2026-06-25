package com.example.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class BackupPayload(
    val categories: List<Category>,
    val transactions: List<TransactionEntity>,
    val backupTimestamp: Long = System.currentTimeMillis()
)
