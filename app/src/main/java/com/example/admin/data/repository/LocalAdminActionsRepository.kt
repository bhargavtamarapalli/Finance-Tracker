package com.example.admin.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.local.EncryptedPrefsManager
import com.example.admin.data.model.AdminAction
import com.example.admin.data.model.AdminActionType
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Local implementation of [IAdminActionsRepository] backed by SharedPreferences.
 * Used for development, sandbox, and local testing.
 */
class LocalAdminActionsRepository(
    context: Context,
    private val moshi: Moshi
) : IAdminActionsRepository {

    private val prefs: SharedPreferences = try {
        EncryptedPrefsManager.getEncryptedPrefs(context, "admin_actions_prefs")
    } catch (e: Exception) {
        context.getSharedPreferences("admin_actions_prefs", Context.MODE_PRIVATE)
    }
    
    private val actionListAdapter = moshi.adapter<List<AdminAction>>(
        Types.newParameterizedType(List::class.java, AdminAction::class.java)
    )

    private val PREF_ACTIONS_KEY = "admin_actions_list"

    private suspend fun getAllActions(): List<AdminAction> = withContext(Dispatchers.IO) {
        val json = prefs.getString(PREF_ACTIONS_KEY, null) ?: return@withContext emptyList()
        try {
            actionListAdapter.fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private suspend fun saveActions(actions: List<AdminAction>) = withContext(Dispatchers.IO) {
        val json = actionListAdapter.toJson(actions)
        prefs.edit().putString(PREF_ACTIONS_KEY, json).commit()
    }

    override suspend fun recordAction(action: AdminAction): Result<Unit> = runCatching {
        val current = getAllActions().toMutableList()
        current.add(action)
        saveActions(current)
    }

    override suspend fun getActionsForUser(uid: String): Result<List<AdminAction>> = runCatching {
        getAllActions().filter { it.targetUid == uid }
    }

    override suspend fun isUserSuspended(uid: String): Boolean {
        val userActions = getActionsForUser(uid).getOrDefault(emptyList())
        if (userActions.isEmpty()) return false
        
        // Find the last status action (SUSPEND or REACTIVATE)
        val lastStatusAction = userActions
            .filter { it.type == AdminActionType.SUSPEND || it.type == AdminActionType.REACTIVATE }
            .maxByOrNull { it.performedAt }
            
        return lastStatusAction?.type == AdminActionType.SUSPEND
    }

    override suspend fun suspendUser(uid: String, note: String): Result<Unit> {
        val action = AdminAction(
            type = AdminActionType.SUSPEND,
            targetUid = uid,
            note = note
        )
        return recordAction(action)
    }

    override suspend fun reactivateUser(uid: String): Result<Unit> {
        val action = AdminAction(
            type = AdminActionType.REACTIVATE,
            targetUid = uid
        )
        return recordAction(action)
    }
}
