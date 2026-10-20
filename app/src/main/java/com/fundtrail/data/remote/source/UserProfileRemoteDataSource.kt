package com.fundtrail.data.remote.source

import android.util.Log
import com.fundtrail.data.remote.dto.UserProfileDto
import com.fundtrail.data.remote.model.ChangeType
import com.fundtrail.data.remote.model.RemoteChange
import com.fundtrail.data.remote.model.RemoteSnapshot
import com.fundtrail.domain.model.Result
import com.fundtrail.domain.model.runCatchingResult
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Provider

/** The single `users/{uid}` profile document, shaped as a one-item [RemoteSnapshot] so it mirrors through the same `SyncManager` machinery as a collection. */
interface UserProfileRemoteDataSource {
    fun observeProfile(uid: String): Flow<RemoteSnapshot<UserProfileDto>>
    fun writeProfile(uid: String, updates: Map<String, Any?>): Result<Unit>
}

class UserProfileRemoteDataSourceImpl @Inject constructor(
    private val firestoreProvider: Provider<FirebaseFirestore>
) : UserProfileRemoteDataSource {

    override fun observeProfile(uid: String): Flow<RemoteSnapshot<UserProfileDto>> = callbackFlow {
        if (uid.isBlank()) {
            trySend(RemoteSnapshot(emptyList(), isFromCache = true))
            close()
            return@callbackFlow
        }

        val docRef = firestoreProvider.get().collection("users").document(uid)

        // INCLUDE, not EXCLUDE: for a user with no profile document yet, both the cache and the
        // server snapshot have an empty change list (nothing "changed" — the doc still doesn't
        // exist), so EXCLUDE can suppress the cache-to-server transition entirely and the profile
        // source would never report a non-cache snapshot to SyncManager's initial-sync gate.
        val registration = docRef.addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
            if (error != null) {
                if (error.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                    Log.w(TAG, "PERMISSION_DENIED for profile uid=$uid (user signed out)")
                    close()
                } else {
                    Log.e(TAG, "Snapshot listener error for profile uid=$uid", error)
                    close(error)
                }
                return@addSnapshotListener
            }

            if (snapshot != null) {
                val changes = if (snapshot.exists()) {
                    try {
                        val dto = snapshot.toObject(
                            UserProfileDto::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                        if (dto != null) listOf(RemoteChange(ChangeType.MODIFIED, dto)) else emptyList()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing UserProfileDto for uid=$uid", e)
                        emptyList()
                    }
                } else {
                    emptyList()
                }
                trySend(RemoteSnapshot(changes, snapshot.metadata.isFromCache))
            }
        }

        awaitClose {
            registration.remove()
        }
    }

    override fun writeProfile(uid: String, updates: Map<String, Any?>): Result<Unit> {
        return runCatchingResult {
            require(uid.isNotBlank()) { "User ID must not be blank" }

            val docRef = firestoreProvider.get().collection("users").document(uid)

            val task = docRef.set(updates, SetOptions.merge())
            task.addOnFailureListener { e ->
                Log.e(TAG, "Async write failure for writeProfile uid=$uid", e)
            }
        }
    }

    private companion object {
        const val TAG = "UserProfileRemoteDS"
    }
}
