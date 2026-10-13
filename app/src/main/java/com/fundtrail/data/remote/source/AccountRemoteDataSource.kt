package com.fundtrail.data.remote.source

import android.util.Log
import com.fundtrail.data.remote.dto.AccountDto
import com.fundtrail.data.remote.dto.toDto
import com.fundtrail.data.remote.dto.toDomain
import com.fundtrail.data.remote.dto.toMap
import com.fundtrail.data.remote.model.RemoteChange
import com.fundtrail.data.remote.model.RemoteSnapshot
import com.fundtrail.domain.model.Account
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
import javax.inject.Inject
import javax.inject.Provider

interface AccountRemoteDataSource {
    fun observeAccounts(uid: String): Flow<RemoteSnapshot<Account>>
    fun addAccount(uid: String, account: Account): Result<String>
    fun updateAccount(uid: String, account: Account): Result<Unit>
    fun updateAccount(uid: String, accountId: String, updates: Map<String, Any?>): Result<Unit>
    fun deleteAccount(uid: String, accountId: String): Result<Unit>
}

class AccountRemoteDataSourceImpl @Inject constructor(
    private val firestoreProvider: Provider<FirebaseFirestore>
) : AccountRemoteDataSource {

    override fun observeAccounts(uid: String): Flow<RemoteSnapshot<Account>> = callbackFlow {
        if (uid.isBlank()) {
            trySend(RemoteSnapshot(emptyList(), isFromCache = true))
            close()
            return@callbackFlow
        }

        val collectionRef = firestoreProvider.get()
            .collection("users").document(uid).collection("accounts")

        val registration = collectionRef.addSnapshotListener(
            MetadataChanges.EXCLUDE
        ) { snapshot, error ->
            if (error != null) {
                if (error.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                    Log.w(TAG, "PERMISSION_DENIED for accounts uid=$uid (user signed out)")
                    close()
                } else {
                    Log.e(TAG, "Snapshot listener error for accounts uid=$uid", error)
                    close(error)
                }
                return@addSnapshotListener
            }

            if (snapshot != null) {
                val changes = snapshot.documentChanges.mapNotNull { change ->
                    try {
                        val dto = change.document.toObject(
                            AccountDto::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                        RemoteChange(change.type, dto.toDomain(change.document.id))
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing AccountDto for doc ${change.document.id}", e)
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

    override fun addAccount(uid: String, account: Account): Result<String> {
        return runCatchingResult {
            require(uid.isNotBlank()) { "User ID must not be blank" }

            val collection = firestoreProvider.get()
                .collection("users").document(uid).collection("accounts")
            val docId = account.id.ifBlank { collection.document().id }
            val docRef = collection.document(docId)

            val validatedAccount = account.copy(id = docId)
            validatedAccount.validate()

            val dto = validatedAccount.toDto()
            val dataMap = dto.toMap(isUpdate = false)

            val task = docRef.set(dataMap)
            task.addOnFailureListener { e ->
                Log.e(TAG, "Async write failure for addAccount id=$docId", e)
            }

            docId
        }
    }

    override fun updateAccount(uid: String, account: Account): Result<Unit> {
        return runCatchingResult {
            require(uid.isNotBlank()) { "User ID must not be blank" }
            account.validate()

            val docRef = firestoreProvider.get()
                .collection("users").document(uid).collection("accounts").document(account.id)

            val dto = account.toDto()
            val dataMap = dto.toMap(isUpdate = true)

            val task = docRef.set(dataMap, SetOptions.merge())
            task.addOnFailureListener { e ->
                Log.e(TAG, "Async write failure for updateAccount id=${account.id}", e)
            }
        }
    }

    override fun updateAccount(uid: String, accountId: String, updates: Map<String, Any?>): Result<Unit> {
        return runCatchingResult {
            require(uid.isNotBlank()) { "User ID must not be blank" }
            require(accountId.isNotBlank()) { "Account ID must not be blank" }

            val docRef = firestoreProvider.get()
                .collection("users").document(uid).collection("accounts").document(accountId)

            val dataMap = updates.toMutableMap()
            dataMap["updatedAt"] = FieldValue.serverTimestamp()

            val task = docRef.set(dataMap, SetOptions.merge())
            task.addOnFailureListener { e ->
                Log.e(TAG, "Async write failure for updateAccount id=$accountId", e)
            }
        }
    }

    override fun deleteAccount(uid: String, accountId: String): Result<Unit> {
        return runCatchingResult {
            require(uid.isNotBlank()) { "User ID must not be blank" }
            require(accountId.isNotBlank()) { "Account ID must not be blank" }

            val docRef = firestoreProvider.get()
                .collection("users").document(uid).collection("accounts").document(accountId)

            val updates = mapOf(
                "deletedAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )

            val task = docRef.set(updates, SetOptions.merge())
            task.addOnFailureListener { e ->
                Log.e(TAG, "Async write failure for deleteAccount id=$accountId", e)
            }
        }
    }

    companion object {
        private const val TAG = "AccountRemoteDS"
    }
}
