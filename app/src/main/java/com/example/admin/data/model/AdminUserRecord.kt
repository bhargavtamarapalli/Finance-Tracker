package com.example.admin.data.model

import com.squareup.moshi.JsonClass

/**
 * UI model for the administrator user management console.
 * This model contains zero user financial data to preserve confidentiality.
 */
@JsonClass(generateAdapter = true)
data class AdminUserRecord(
    val uid: String,
    val displayName: String,
    val email: String,
    val status: AdminUserStatus,
    val plan: AdminUserPlan,
    val joinedAt: Long,
    val lastActiveAt: Long,
    val sessionCount: Int,
    val region: String,
    val deviceInfo: String
)

enum class AdminUserStatus {
    ACTIVE, SUSPENDED
}

enum class AdminUserPlan {
    BASIC, BUSINESS
}
