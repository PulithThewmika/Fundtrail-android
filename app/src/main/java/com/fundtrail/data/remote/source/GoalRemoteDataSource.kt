package com.fundtrail.data.remote.source

import android.util.Log
import com.fundtrail.data.remote.dto.GoalDto
import com.fundtrail.data.remote.dto.toDomain
import com.fundtrail.data.remote.dto.toMap
import com.fundtrail.data.remote.model.RemoteChange
import com.fundtrail.data.remote.model.RemoteSnapshot
import com.fundtrail.data.remote.model.toChangeType
import com.fundtrail.domain.model.Goal
import com.fundtrail.domain.model.Result
import com.fundtrail.domain.model.runCatchingResult
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.time.Clock
import javax.inject.Inject
import javax.inject.Provider

interface GoalRemoteDataSource {
    fun observeGoals(uid: String): Flow<RemoteSnapshot<Goal>>

    /**
     * Saves [goal]. [isUpdate] must be true only when [goal]'s document already exists, so the
     * immutable-on-create fields (`startedAt`, `createdAt`) are written once and never touched
     * again (schema.md §6.4) — an update that resent them would fail the rules' `keepsCreatedAt()`
     * check. When [archivedId] is set, archiving the previous active goal is written in the same
     * batch, so the two documents never disagree (schema.md §3.7: only one active goal per user).
     */
    fun saveGoal(uid: String, goal: Goal, isUpdate: Boolean, archivedId: String?): Result<String>
    fun archiveGoal(uid: String, goalId: String): Result<Unit>
}

class GoalRemoteDataSourceImpl @Inject constructor(
    private val firestoreProvider: Provider<FirebaseFirestore>,
    private val clock: Clock,
) : GoalRemoteDataSource {

    override fun observeGoals(uid: String): Flow<RemoteSnapshot<Goal>> = callbackFlow {
        if (uid.isBlank()) {
            trySend(RemoteSnapshot(emptyList(), isFromCache = true))
            close()
            return@callbackFlow
        }

        val collectionRef = firestoreProvider.get()
            .collection("users").document(uid).collection("goals")

        val registration = collectionRef.addSnapshotListener(
            MetadataChanges.EXCLUDE
        ) { snapshot, error ->
            if (error != null) {
                if (error.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                    Log.w(TAG, "PERMISSION_DENIED for goals uid=$uid (user signed out)")
                    close()
                } else {
                    Log.e(TAG, "Snapshot listener error for goals uid=$uid", error)
                    close(error)
                }
                return@addSnapshotListener
            }

            if (snapshot != null) {
                val changes = snapshot.documentChanges.mapNotNull { change ->
                    try {
                        val dto = change.document.toObject(
                            GoalDto::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                        RemoteChange(change.type.toChangeType(), dto.toDomain(change.document.id))
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing GoalDto for doc ${change.document.id}", e)
                        null
                    }
                }
                trySend(RemoteSnapshot(changes, snapshot.metadata.isFromCache))
            }
        }

        awaitClose {
            registration.remove()
        }
    }

    override fun saveGoal(uid: String, goal: Goal, isUpdate: Boolean, archivedId: String?): Result<String> {
        return runCatchingResult {
            require(uid.isNotBlank()) { "User ID must not be blank" }

            val firestore = firestoreProvider.get()
            val collection = firestore.collection("users").document(uid).collection("goals")
            val docId = goal.id.ifBlank { collection.document().id }

            val validatedGoal = goal.copy(id = docId)
            validatedGoal.validate()

            val batch = firestore.batch()
            batch.set(collection.document(docId), validatedGoal.toMap(isUpdate = isUpdate, now = clock.instant()), SetOptions.merge())
            if (archivedId != null && archivedId != docId) {
                batch.set(
                    collection.document(archivedId),
                    mapOf("isActive" to false, "updatedAt" to FieldValue.serverTimestamp()),
                    SetOptions.merge()
                )
            }

            val task = batch.commit()
            task.addOnFailureListener { e ->
                Log.e(TAG, "Async write failure for saveGoal id=$docId", e)
            }

            docId
        }
    }

    override fun archiveGoal(uid: String, goalId: String): Result<Unit> {
        return runCatchingResult {
            require(uid.isNotBlank()) { "User ID must not be blank" }
            require(goalId.isNotBlank()) { "Goal ID must not be blank" }

            val docRef = firestoreProvider.get()
                .collection("users").document(uid).collection("goals").document(goalId)

            val task = docRef.set(
                mapOf("isActive" to false, "updatedAt" to FieldValue.serverTimestamp()),
                SetOptions.merge()
            )
            task.addOnFailureListener { e ->
                Log.e(TAG, "Async write failure for archiveGoal id=$goalId", e)
            }
        }
    }

    private companion object {
        const val TAG = "GoalRemoteDS"
    }
}
