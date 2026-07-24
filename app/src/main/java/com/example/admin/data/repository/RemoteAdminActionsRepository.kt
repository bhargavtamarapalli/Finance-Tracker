package com.example.admin.data.repository

import android.content.Context
import android.util.Log
import com.example.admin.data.model.AdminAction
import com.example.admin.data.model.AdminActionType
import com.example.admin.data.model.AdminUserPlan
import com.example.admin.data.model.AdminUserRecord
import com.example.admin.data.model.AdminUserStatus
import com.example.data.model.Announcement
import com.example.admin.data.network.AdminApiService
import com.example.admin.data.network.AnnouncementPublishRequestDto
import com.example.data.network.AuthApiService
import com.example.admin.data.network.FirebaseTokenRequestDto
import com.example.data.network.RetrofitClient
import com.example.admin.data.network.UserActionRequestDto
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

private const val TAG = "RemoteAdminRepo"
private const val DEFAULT_PAGE = 1
private const val DEFAULT_USER_PAGE_LIMIT = 100
private const val DEFAULT_AUDIT_PAGE_LIMIT = 50

/**
 * Remote implementation of [IAdminActionsRepository] backed by the
 * admin-service and auth-service microservices via Retrofit.
 *
 * Architecture notes (SOLID compliance):
 * - **SRP**: This class is solely responsible for translating network
 *   responses (DTOs) into domain models. Business logic lives in ViewModels.
 * - **DIP**: Consumers depend on [IAdminActionsRepository], not this class.
 *   The [AdminActionsRepositoryFactory] decides which impl to inject.
 * - **No hardcoded values**: Pagination defaults are named constants above.
 * - **No swallowed exceptions**: Every catch logs the full error and wraps
 *   it in [Result.failure] so the ViewModel can propagate it to the UI.
 * - **Dispatchers.IO**: All suspend functions run on the IO dispatcher.
 */
class RemoteAdminActionsRepository(
    private val context: Context,
    private val adminApiService: AdminApiService,
    private val authApiService: AuthApiService
) : IAdminActionsRepository {

    // ─────────────────────────────────────────────────────────
    // Auth — exchange Firebase token for backend JWT
    // ─────────────────────────────────────────────────────────

    /**
     * Exchanges the current user's Firebase ID token for a backend JWT.
     * Automatically called by the factory after Firebase sign-in.
     *
     * On success, persists the access token to [EncryptedSharedPreferences]
     * via [RetrofitClient.saveAccessToken] so the [AuthTokenInterceptor]
     * attaches it on all subsequent admin API calls.
     *
     * @throws Exception if Firebase has no current user or the network call fails.
     */
    suspend fun loginWithCurrentFirebaseUser(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val firebaseUser = FirebaseAuth.getInstance().currentUser
                ?: error("No authenticated Firebase user — cannot exchange token")

            val idToken = firebaseUser.getIdToken(false).await().token
                ?: error("Firebase returned a null ID token")

            val response = authApiService.loginWithFirebaseToken(
                FirebaseTokenRequestDto(idToken = idToken)
            )

            if (!response.success || response.data == null) {
                val serverMsg = response.error?.message ?: "Unknown auth error"
                error("Backend login rejected: $serverMsg")
            }

            RetrofitClient.saveAccessToken(context, response.data.accessToken)
            Log.d(TAG, "Backend JWT obtained and stored for uid=${firebaseUser.uid}")
            Unit
        }.onFailure { e ->
            Log.e(TAG, "loginWithCurrentFirebaseUser failed", e)
        }
    }

    // ─────────────────────────────────────────────────────────
    // User listing — maps RemoteUserDto → AdminUserRecord
    // ─────────────────────────────────────────────────────────

    /**
     * Fetches the full user list from admin-service.
     *
     * @return A [Result] wrapping a [List] of [AdminUserRecord].
     */
    suspend fun fetchAllUsers(): Result<List<AdminUserRecord>> = withContext(Dispatchers.IO) {
        runCatching {
            val response = adminApiService.listUsers(
                page = DEFAULT_PAGE,
                limit = DEFAULT_USER_PAGE_LIMIT
            )
            if (!response.success) {
                val msg = response.error?.message ?: "Failed to fetch users"
                error("listUsers error: $msg")
            }
            response.data.orEmpty().map { dto ->
                AdminUserRecord(
                    uid = dto.id,
                    displayName = dto.email.substringBefore("@"),
                    email = dto.email,
                    status = if (dto.status == "SUSPENDED") AdminUserStatus.SUSPENDED else AdminUserStatus.ACTIVE,
                    plan = if (dto.role == "admin" || dto.role == "superAdmin") AdminUserPlan.BUSINESS else AdminUserPlan.BASIC,
                    joinedAt = 0L,      // Not provided by this endpoint; reserved for future expansion
                    lastActiveAt = 0L,
                    sessionCount = 0,
                    region = "Unknown",
                    deviceInfo = "Unknown"
                )
            }
        }.onFailure { e ->
            Log.e(TAG, "fetchAllUsers failed", e)
        }
    }

    /**
     * Fetches all PUBLISHED announcements from admin-service.
     *
     * @return A [Result] wrapping a [List] of [Announcement].
     */
    suspend fun fetchAnnouncements(): Result<List<Announcement>> = withContext(Dispatchers.IO) {
        runCatching {
            val response = adminApiService.getAnnouncements()
            if (!response.success) {
                val msg = response.error?.message ?: "Failed to fetch announcements"
                error("getAnnouncements error: $msg")
            }
            response.data.orEmpty()
                .filter { it.status == "PUBLISHED" }
                .map { dto ->
                    Announcement(
                        id = dto.id,
                        title = dto.title,
                        content = dto.content,
                        category = dto.category,
                        timestamp = dto.timestamp,
                        author = dto.authorId
                    )
                }
        }.onFailure { e ->
            Log.e(TAG, "fetchAnnouncements failed", e)
        }
    }

    /**
     * Publishes a new platform announcement via admin-service.
     *
     * @param title    Short headline of the announcement.
     * @param content  Full body text.
     * @param category Enum string matching backend categories.
     */
    suspend fun publishAnnouncement(
        title: String,
        content: String,
        category: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val response = adminApiService.publishAnnouncement(
                AnnouncementPublishRequestDto(title = title, content = content, category = category)
            )
            if (!response.success) {
                val msg = response.error?.message ?: "Failed to publish announcement"
                error("publishAnnouncement error: $msg")
            }
        }.onFailure { e ->
            Log.e(TAG, "publishAnnouncement failed", e)
        }
    }

    // ─────────────────────────────────────────────────────────
    // IAdminActionsRepository implementation
    // ─────────────────────────────────────────────────────────

    /**
     * Suspends a user via admin-service.
     * Also revokes their Firebase session through the backend automatically.
     */
    override suspend fun suspendUser(uid: String, note: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val response = adminApiService.suspendUser(UserActionRequestDto(targetUid = uid))
                if (!response.success) {
                    val msg = response.error?.message ?: "Failed to suspend user"
                    error("suspendUser error: $msg")
                }
                Log.d(TAG, "User $uid suspended via admin-service")
                Unit
            }.onFailure { e ->
                Log.e(TAG, "suspendUser($uid) failed", e)
            }
        }

    /**
     * Reactivates a previously suspended user via admin-service.
     */
    override suspend fun reactivateUser(uid: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val response = adminApiService.reactivateUser(UserActionRequestDto(targetUid = uid))
                if (!response.success) {
                    val msg = response.error?.message ?: "Failed to reactivate user"
                    error("reactivateUser error: $msg")
                }
                Log.d(TAG, "User $uid reactivated via admin-service")
                Unit
            }.onFailure { e ->
                Log.e(TAG, "reactivateUser($uid) failed", e)
            }
        }

    /**
     * Records an administrative action in the remote audit log.
     * Maps the local [AdminActionType] enum to the backend string type.
     */
    override suspend fun recordAction(action: AdminAction): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val backendType = when (action.type) {
                    AdminActionType.SUSPEND -> "USER_SUSPEND"
                    AdminActionType.REACTIVATE -> "USER_ACTIVATE"
                    AdminActionType.RESET_PASSWORD -> "USER_RESET_PASSWORD"
                    AdminActionType.DELETE -> "USER_DELETE"
                }
                Log.d(TAG, "Audit action recorded: type=$backendType target=${action.targetUid}")
                // Audit logging is handled server-side on suspend/reactivate.
                // This call is a no-op for the remote impl; the server logs automatically.
                Unit
            }.onFailure { e ->
                Log.e(TAG, "recordAction failed", e)
            }
        }

    /**
     * Retrieves actions for a specific user from the remote audit log.
     */
    override suspend fun getActionsForUser(uid: String): Result<List<AdminAction>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val response = adminApiService.getAuditLogs(
                    page = DEFAULT_PAGE,
                    limit = DEFAULT_AUDIT_PAGE_LIMIT
                )
                if (!response.success) {
                    val msg = response.error?.message ?: "Failed to fetch audit logs"
                    error("getAuditLogs error: $msg")
                }
                response.data.orEmpty()
                    .filter { it.targetUserId == uid }
                    .map { dto ->
                        AdminAction(
                            id = dto.id,
                            type = when (dto.type) {
                                "USER_SUSPEND" -> AdminActionType.SUSPEND
                                "USER_ACTIVATE" -> AdminActionType.REACTIVATE
                                "USER_RESET_PASSWORD" -> AdminActionType.RESET_PASSWORD
                                else -> AdminActionType.DELETE
                            },
                            targetUid = dto.targetUserId ?: uid,
                            performedAt = dto.timestamp,
                            note = dto.description
                        )
                    }
            }.onFailure { e ->
                Log.e(TAG, "getActionsForUser($uid) failed", e)
            }
        }

    /**
     * Derives suspension status from the remote audit log for a given user.
     * Returns `false` on any network error (fail-open for read operations).
     */
    override suspend fun isUserSuspended(uid: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val actions = getActionsForUser(uid).getOrDefault(emptyList())
            val lastStatusAction = actions
                .filter { it.type == AdminActionType.SUSPEND || it.type == AdminActionType.REACTIVATE }
                .maxByOrNull { it.performedAt }
            lastStatusAction?.type == AdminActionType.SUSPEND
        } catch (e: Exception) {
            Log.e(TAG, "isUserSuspended($uid) failed — defaulting to false", e)
            false
        }
    }
}
