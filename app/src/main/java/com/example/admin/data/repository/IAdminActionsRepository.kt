package com.example.admin.data.repository

import com.example.admin.data.model.AdminAction

/**
 * Interface definition for persisting administrative security actions.
 * Allows switching between local storage (dev/debug) and cloud database (production).
 */
interface IAdminActionsRepository {
    /**
     * Records a specific administrative action.
     */
    suspend fun recordAction(action: AdminAction): Result<Unit>

    /**
     * Retrieves all recorded actions for a specific user ID.
     */
    suspend fun getActionsForUser(uid: String): Result<List<AdminAction>>

    /**
     * Checks if a user is currently suspended.
     */
    suspend fun isUserSuspended(uid: String): Boolean

    /**
     * Suspends a user by recording a suspension action.
     */
    suspend fun suspendUser(uid: String, note: String = ""): Result<Unit>

    /**
     * Reactivates a user by recording a reactivation action.
     */
    suspend fun reactivateUser(uid: String): Result<Unit>
}
