package com.example.admin.data.network

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit interface for the admin-service microservice.
 *
 * All endpoints require a valid backend JWT delivered via the
 * [AuthTokenInterceptor] — callers do not attach headers manually.
 *
 * Base URL is read from BuildConfig.ADMIN_API_BASE_URL (injected from .env
 * via the Secrets Gradle Plugin — never hardcoded).
 */
interface AdminApiService {

    /**
     * Returns a paginated list of all platform users.
     * Requires `admin` or `superAdmin` role claim.
     */
    @GET("api/admin/users")
    suspend fun listUsers(
        @Query("page") page: Int,
        @Query("limit") limit: Int
    ): ApiResponseDto<List<RemoteUserDto>>

    /**
     * Suspends the given user account and revokes their Firebase session.
     */
    @POST("api/admin/users/suspend")
    suspend fun suspendUser(
        @Body body: UserActionRequestDto
    ): ApiResponseDto<Unit>

    /**
     * Reactivates a previously suspended user account.
     */
    @POST("api/admin/users/reactivate")
    suspend fun reactivateUser(
        @Body body: UserActionRequestDto
    ): ApiResponseDto<Unit>

    /**
     * Updates the role of a target user and syncs Firebase Custom Claims.
     */
    @PATCH("api/admin/users/role")
    suspend fun updateUserRole(
        @Body body: RoleUpdateRequestDto
    ): ApiResponseDto<Unit>

    /**
     * Returns the immutable administrative audit trail, newest first.
     */
    @GET("api/admin/audit-logs")
    suspend fun getAuditLogs(
        @Query("page") page: Int,
        @Query("limit") limit: Int
    ): ApiResponseDto<List<AuditLogDto>>

    /**
     * Publishes a new platform-wide announcement broadcast.
     */
    @POST("api/admin/announcements")
    suspend fun publishAnnouncement(
        @Body body: AnnouncementPublishRequestDto
    ): ApiResponseDto<Unit>

    /**
     * Returns all announcements (any status) for the admin console view.
     */
    @GET("api/admin/announcements")
    suspend fun getAnnouncements(): ApiResponseDto<List<AnnouncementDto>>

    /**
     * Archives a published announcement by ID.
     */
    @DELETE("api/admin/announcements/{id}")
    suspend fun archiveAnnouncement(
        @Path("id") id: String
    ): ApiResponseDto<Unit>
}
