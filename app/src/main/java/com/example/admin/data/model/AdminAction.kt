package com.example.admin.data.model

import com.squareup.moshi.JsonClass
import java.util.UUID

/**
 * Record of an action performed by an administrator.
 * Used for persisting and auditing security-sensitive modifications.
 */
@JsonClass(generateAdapter = true)
data class AdminAction(
    val id: String = UUID.randomUUID().toString(),
    val type: AdminActionType,
    val targetUid: String,
    val performedAt: Long = System.currentTimeMillis(),
    val note: String = ""
)

enum class AdminActionType {
    SUSPEND, REACTIVATE, RESET_PASSWORD, DELETE
}
