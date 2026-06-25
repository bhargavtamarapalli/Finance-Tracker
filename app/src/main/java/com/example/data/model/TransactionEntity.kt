package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

@Entity(tableName = "transactions")
@JsonClass(generateAdapter = true)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val amount: Double,
    val source: String,
    val date: Long,
    val categoryId: Int,
    val type: TransactionType,
    val notes: String,
    val paymentMethod: String = "Cash"
)
