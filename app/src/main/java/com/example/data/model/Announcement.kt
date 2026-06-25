package com.example.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Announcement(
    val id: String,
    val title: String,
    val content: String,
    val category: String, // e.g. "General", "System Update", "Chitti Info", "Business Tip"
    val timestamp: Long,
    val author: String = "Administrator"
)
