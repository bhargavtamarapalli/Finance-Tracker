package com.example.admin.data.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// ─────────────────────────────────────────────────────────────
// Generic API envelope — mirrors backend ApiResponse<T>
// ─────────────────────────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class ApiResponseDto<T>(
    val success: Boolean,
    val data: T? = null,
    val meta: PaginationMetaDto? = null,
    val error: ApiErrorDto? = null
)

@JsonClass(generateAdapter = true)
data class PaginationMetaDto(
    val page: Int,
    val limit: Int,
    val total: Int
)

@JsonClass(generateAdapter = true)
data class ApiErrorDto(
    val code: String,
    val message: String
)

// ─────────────────────────────────────────────────────────────
// Auth DTOs
// ─────────────────────────────────────────────────────────────

/**
 * Request payload sent to auth-service to exchange a Firebase ID token
 * for a backend-signed JWT.
 */
@JsonClass(generateAdapter = true)
data class FirebaseTokenRequestDto(
    @Json(name = "idToken") val idToken: String
)

@JsonClass(generateAdapter = true)
data class AuthResponseDto(
    val success: Boolean,
    val data: AuthDataDto? = null,
    val error: ApiErrorDto? = null
)

@JsonClass(generateAdapter = true)
data class AuthDataDto(
    val user: RemoteUserDto,
    val accessToken: String
)

// ─────────────────────────────────────────────────────────────
// User DTOs
// ─────────────────────────────────────────────────────────────

/**
 * Remote user record returned by admin-service.
 * Contains zero financial data — only identity/admin metadata.
 */
@JsonClass(generateAdapter = true)
data class RemoteUserDto(
    val id: String,
    val email: String,
    val role: String,
    val status: String,
    @Json(name = "createdAt") val createdAt: String? = null,
    @Json(name = "lastLoginAt") val lastLoginAt: String? = null
)

// ─────────────────────────────────────────────────────────────
// Admin Action DTOs
// ─────────────────────────────────────────────────────────────

/** Request body for suspend / reactivate user calls. */
@JsonClass(generateAdapter = true)
data class UserActionRequestDto(
    @Json(name = "targetUid") val targetUid: String
)

/** Request body for role updates. */
@JsonClass(generateAdapter = true)
data class RoleUpdateRequestDto(
    @Json(name = "targetUid") val targetUid: String,
    val role: String
)

// ─────────────────────────────────────────────────────────────
// Announcement DTOs
// ─────────────────────────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class AnnouncementDto(
    val id: String,
    val title: String,
    val content: String,
    val category: String,
    val timestamp: Long,
    @Json(name = "authorId") val authorId: String,
    val status: String
)

@JsonClass(generateAdapter = true)
data class AnnouncementPublishRequestDto(
    val title: String,
    val content: String,
    val category: String
)

// ─────────────────────────────────────────────────────────────
// Audit Log DTOs
// ─────────────────────────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class AuditLogDto(
    val id: String,
    val type: String,
    @Json(name = "targetUserId") val targetUserId: String?,
    @Json(name = "performedBy") val performedBy: String,
    val description: String,
    val timestamp: Long
)
