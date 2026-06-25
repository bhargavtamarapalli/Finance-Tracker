package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

@Entity(tableName = "categories")
@JsonClass(generateAdapter = true)
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val type: TransactionType,
    val iconName: String,
    val isDefault: Boolean = false,
    val isArchived: Boolean = false
)
