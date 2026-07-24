package com.example.admin.data.repository

import com.example.admin.data.model.AdminAction
import com.example.admin.data.model.AdminActionType
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Production implementation of [IAdminActionsRepository] backed by Cloud Firestore.
 */
class FirestoreAdminActionsRepository : IAdminActionsRepository {

    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    
    private val ACTIONS_COLLECTION = "admin_actions"
    private val LOGS_SUBCOLLECTION = "logs"

    override suspend fun recordAction(action: AdminAction): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            // Write action log
            db.collection(ACTIONS_COLLECTION)
                .document(action.targetUid)
                .collection(LOGS_SUBCOLLECTION)
                .document(action.id)
                .set(action)
                .await()

            // Update main status field
            val statusValue = when (action.type) {
                AdminActionType.SUSPEND -> "SUSPENDED"
                AdminActionType.REACTIVATE -> "ACTIVE"
                else -> null
            }

            if (statusValue != null) {
                db.collection(ACTIONS_COLLECTION)
                    .document(action.targetUid)
                    .set(mapOf("status" to statusValue))
                    .await()
            }
        }
    }

    override suspend fun getActionsForUser(uid: String): Result<List<AdminAction>> = withContext(Dispatchers.IO) {
        runCatching {
            val snapshot = db.collection(ACTIONS_COLLECTION)
                .document(uid)
                .collection(LOGS_SUBCOLLECTION)
                .orderBy("performedAt")
                .get()
                .await()
                
            snapshot.toObjects(AdminAction::class.java)
        }
    }

    override suspend fun isUserSuspended(uid: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val doc = db.collection(ACTIONS_COLLECTION)
                .document(uid)
                .get()
                .await()
                
            doc.getString("status") == "SUSPENDED"
        } catch (e: Exception) {
            false
        }
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
