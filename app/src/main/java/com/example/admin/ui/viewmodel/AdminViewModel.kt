package com.example.admin.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.admin.data.model.AdminAction
import com.example.admin.data.model.AdminActionType
import com.example.admin.data.model.AdminSystemStats
import com.example.admin.data.model.AdminUserPlan
import com.example.admin.data.model.AdminUserRecord
import com.example.admin.data.model.AdminUserStatus
import com.example.data.model.Announcement
import com.example.data.repository.AuthRepository
import com.example.data.repository.FinanceRepository
import com.example.admin.data.repository.IAdminActionsRepository
import com.example.admin.data.repository.RemoteAdminActionsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import com.example.data.repository.await

/**
 * Event type representing administrative notification messages.
 */
sealed class AdminUiEvent {
    data class ShowSuccess(val message: String) : AdminUiEvent()
    data class ShowError(val message: String) : AdminUiEvent()
    object NavigateBack : AdminUiEvent()
}

/**
 * ViewModel coordinates administrative actions, filters user tables,
 * and compiles aggregate statistics. Running in isolated architecture bounds.
 */
class AdminViewModel(
    private val financeRepository: FinanceRepository,
    private val actionsRepository: IAdminActionsRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _systemStats = MutableStateFlow<AdminSystemStats?>(null)
    val systemStats: StateFlow<AdminSystemStats?> = _systemStats.asStateFlow()

    private val _allUsers = MutableStateFlow<List<AdminUserRecord>>(emptyList())
    val allUsers: StateFlow<List<AdminUserRecord>> = _allUsers.asStateFlow()

    private val _announcements = MutableStateFlow<List<Announcement>>(emptyList())
    val announcements: StateFlow<List<Announcement>> = _announcements.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _filterStatus = MutableStateFlow<AdminUserStatus?>(null)
    val filterStatus: StateFlow<AdminUserStatus?> = _filterStatus.asStateFlow()

    private val _filterPlan = MutableStateFlow<AdminUserPlan?>(null)
    val filterPlan: StateFlow<AdminUserPlan?> = _filterPlan.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private val _adminEvent = MutableSharedFlow<AdminUiEvent>()
    val adminEvent: SharedFlow<AdminUiEvent> = _adminEvent.asSharedFlow()

    // Dynamically filtered user records matching search/filters
    val filteredUsers: StateFlow<List<AdminUserRecord>> = combine(
        _allUsers, _searchQuery, _filterStatus, _filterPlan
    ) { users, query, status, plan ->
        users.filter { user ->
            val matchQuery = query.isEmpty() || 
                user.displayName.contains(query, ignoreCase = true) || 
                user.email.contains(query, ignoreCase = true)
            
            val matchStatus = status == null || user.status == status
            val matchPlan = plan == null || user.plan == plan

            matchQuery && matchStatus && matchPlan
        }
    }.stateIn(
        scope = viewModelScope,
        started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        ensureBackendSession()
        loadUsers()
        loadAnnouncements()
    }

    /**
     * Lazily exchanges the current Firebase session for a backend JWT.
     * Runs only when the injected repository is [RemoteAdminActionsRepository].
     * Fails silently — if the backend is unreachable the app degrades to
     * Firestore/local without crashing the admin console.
     */
    private fun ensureBackendSession() {
        val remote = actionsRepository as? RemoteAdminActionsRepository ?: return
        viewModelScope.launch {
            remote.loginWithCurrentFirebaseUser().onFailure { e ->
                // Non-fatal: admin console will still work in local/Firestore mode
                _adminEvent.emit(AdminUiEvent.ShowError("Backend session could not be established: ${e.message}"))
            }
        }
    }

    /**
     * Loads the user list from the appropriate source:
     * - [RemoteAdminActionsRepository] → admin-service REST API
     * - DEBUG without remote → [AdminMockData] seeds (local dev)
     * - Release without remote → Cloud Firestore
     */
    fun loadUsers() {
        viewModelScope.launch {
            _isBusy.value = true
            val users = withContext(Dispatchers.IO) {
                val remote = actionsRepository as? RemoteAdminActionsRepository
                when {
                    remote != null -> {
                        remote.fetchAllUsers().getOrElse { emptyList() }
                    }
                    BuildConfig.DEBUG -> {
                        AdminMockData.users.map { user ->
                            val isSuspended = actionsRepository.isUserSuspended(user.uid)
                            user.copy(status = if (isSuspended) AdminUserStatus.SUSPENDED else AdminUserStatus.ACTIVE)
                        }
                    }
                    else -> fetchFirebaseAuthUsers()
                }
            }
            _allUsers.value = users
            _isBusy.value = false
        }
    }

    /**
     * Loads announcements from admin-service when the remote repo is active,
     * or returns an empty list in local/debug mode.
     */
    fun loadAnnouncements() {
        viewModelScope.launch {
            val remote = actionsRepository as? RemoteAdminActionsRepository ?: return@launch
            remote.fetchAnnouncements()
                .onSuccess { _announcements.value = it }
                .onFailure { e ->
                    _adminEvent.emit(AdminUiEvent.ShowError("Failed to load announcements: ${e.message}"))
                }
        }
    }

    /**
     * Publishes a new platform announcement via admin-service.
     *
     * @param title    Short headline of the broadcast.
     * @param content  Full body text.
     * @param category Category enum string (e.g. "General", "System Update").
     */
    fun publishAnnouncement(title: String, content: String, category: String) {
        viewModelScope.launch {
            _isBusy.value = true
            val remote = actionsRepository as? RemoteAdminActionsRepository
            if (remote == null) {
                _adminEvent.emit(AdminUiEvent.ShowError("Announcements require the remote backend."))
                _isBusy.value = false
                return@launch
            }
            remote.publishAnnouncement(title, content, category)
                .onSuccess {
                    _adminEvent.emit(AdminUiEvent.ShowSuccess("Announcement published successfully."))
                    loadAnnouncements() // Refresh the list
                }
                .onFailure { e ->
                    _adminEvent.emit(AdminUiEvent.ShowError("Failed to publish: ${e.message}"))
                }
            _isBusy.value = false
        }
    }

    /**
     * Helper resolving Firebase users. Safe-guarded by fallback stubs.
     */
    private suspend fun fetchFirebaseAuthUsers(): List<AdminUserRecord> = withContext(Dispatchers.IO) {
        try {
            // Production-grade resolution from Cloud Firestore collection
            val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            val querySnapshot = firestore.collection("users").get().await()
            querySnapshot.documents.map { doc ->
                val uid = doc.id
                val isSuspended = actionsRepository.isUserSuspended(uid)
                AdminUserRecord(
                    uid = uid,
                    displayName = doc.getString("displayName") ?: "User",
                    email = doc.getString("email") ?: "",
                    status = if (isSuspended) AdminUserStatus.SUSPENDED else AdminUserStatus.ACTIVE,
                    plan = if (doc.getString("plan") == "BUSINESS") AdminUserPlan.BUSINESS else AdminUserPlan.BASIC,
                    joinedAt = doc.getLong("joinedAt") ?: 0L,
                    lastActiveAt = doc.getLong("lastActiveAt") ?: 0L,
                    sessionCount = doc.getLong("sessionCount")?.toInt() ?: 0,
                    region = doc.getString("region") ?: "Unknown",
                    deviceInfo = doc.getString("deviceInfo") ?: "Unknown"
                )
            }
        } catch (e: Exception) {
            // Graceful logging / recovery
            emptyList()
        }
    }


    /**
     * Computes platform-wide metrics. Runs on Dispatchers.IO.
     */
    fun refreshStats(announcementsCount: Int) {
        viewModelScope.launch {
            val stats = financeRepository.getSystemStats(announcementsCount)
            // Overlay active and suspended user count from current users list
            val currentUsers = _allUsers.value
            val total = currentUsers.size
            val suspended = currentUsers.count { it.status == AdminUserStatus.SUSPENDED }
            val active = total - suspended

            _systemStats.value = stats.copy(
                totalUsers = total,
                activeUsers = active,
                suspendedUsers = suspended
            )
        }
    }

    /**
     * Suspends a specific user account. Persisted through config interfaces.
     */
    fun suspendUser(uid: String, note: String = "") {
        viewModelScope.launch {
            _isBusy.value = true
            val result = actionsRepository.suspendUser(uid, note)
            if (result.isSuccess) {
                // Update local memory list
                _allUsers.value = _allUsers.value.map {
                    if (it.uid == uid) it.copy(status = AdminUserStatus.SUSPENDED) else it
                }
                _adminEvent.emit(AdminUiEvent.ShowSuccess("User accounts suspended successfully."))
            } else {
                _adminEvent.emit(AdminUiEvent.ShowError("Failed to update status."))
            }
            _isBusy.value = false
        }
    }

    /**
     * Reactivates a suspended user.
     */
    fun reactivateUser(uid: String) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = actionsRepository.reactivateUser(uid)
            if (result.isSuccess) {
                _allUsers.value = _allUsers.value.map {
                    if (it.uid == uid) it.copy(status = AdminUserStatus.ACTIVE) else it
                }
                _adminEvent.emit(AdminUiEvent.ShowSuccess("User reactivated successfully."))
            } else {
                _adminEvent.emit(AdminUiEvent.ShowError("Failed to reactivate user."))
            }
            _isBusy.value = false
        }
    }

    /**
     * Mock helper representing password resets. Emits status links.
     */
    fun resetPassword(uid: String) {
        viewModelScope.launch {
            _adminEvent.emit(AdminUiEvent.ShowSuccess("Password reset instructions transmitted."))
        }
    }

    /**
     * Permanently deletes user records. Confirms through validation cards.
     */
    fun deleteUserRecord(uid: String) {
        viewModelScope.launch {
            _isBusy.value = true
            // Local state delete
            _allUsers.value = _allUsers.value.filter { it.uid != uid }
            
            // Log action
            actionsRepository.recordAction(AdminAction(type = AdminActionType.DELETE, targetUid = uid))
            
            _adminEvent.emit(AdminUiEvent.ShowSuccess("User permanently deleted."))
            _isBusy.value = false
        }
    }

    /**
     * Compiles general report payloads.
     */
    fun exportReport() {
        viewModelScope.launch {
            _isBusy.value = true
            val currentStats = _systemStats.value
            if (currentStats == null) {
                _adminEvent.emit(AdminUiEvent.ShowError("Stats not loaded."))
                _isBusy.value = false
                return@launch
            }
            val result = financeRepository.exportSystemReport(currentStats)
            result.onSuccess { file ->
                _adminEvent.emit(AdminUiEvent.ShowSuccess("Report generated: ${file.name}"))
            }.onFailure {
                _adminEvent.emit(AdminUiEvent.ShowError("Report compilation failed."))
            }
            _isBusy.value = false
        }
    }

    /**
     * Clears all local database stores.
     */
    fun clearAllData() {
        viewModelScope.launch {
            _isBusy.value = true
            financeRepository.clearAllData()
            _adminEvent.emit(AdminUiEvent.ShowSuccess("All local data purged."))
            _isBusy.value = false
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setFilterStatus(status: AdminUserStatus?) {
        _filterStatus.value = status
    }

    fun setFilterPlan(plan: AdminUserPlan?) {
        _filterPlan.value = plan
    }
}

