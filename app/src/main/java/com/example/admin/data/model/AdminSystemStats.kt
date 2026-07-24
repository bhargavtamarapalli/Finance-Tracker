package com.example.admin.data.model

import com.squareup.moshi.JsonClass

/**
 * Snapshot model for system-level summary analytics.
 * Computed via aggregate queries, containing zero individual transaction or user detail.
 */
@JsonClass(generateAdapter = true)
data class AdminSystemStats(
    val totalUsers: Int,
    val activeUsers: Int,
    val suspendedUsers: Int,
    val totalTransactions: Long,
    val totalCategories: Int,
    val announcementsCount: Int,
    val snapshotAt: Long = System.currentTimeMillis()
)
