package com.fundtrail.data.remote.source

import android.util.Log
import com.fundtrail.data.remote.dto.TransactionDto
import com.fundtrail.data.remote.dto.toDto
import com.fundtrail.data.remote.dto.toDomain
import com.fundtrail.data.remote.dto.toMap
import com.fundtrail.data.remote.model.RemoteChange
import com.fundtrail.data.remote.model.RemoteSnapshot
import com.fundtrail.domain.model.Result
import com.fundtrail.domain.model.Transaction
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

interface TransactionRemoteDataSource {
    fun observeTransactions(uid: String): Flow<RemoteSnapshot<Transaction>>
    fun addTransaction(uid: String, transaction: Transaction): Result<String>
    fun updateTransaction(uid: String, transaction: Transaction): Result<Unit>
    fun updateTransaction(uid: String, transactionId: String, updates: Map<String, Any?>): Result<Unit>
    fun deleteTransaction(uid: String, transactionId: String): Result<Unit>
}

class TransactionRemoteDataSourceImpl @Inject constructor(
    private val firestoreProvider: Provider<FirebaseFirestore>
) : TransactionRemoteDataSource {

    override fun observeTransactions(uid: String): Flow<RemoteSnapshot<Transaction>> = callbackFlow {
        if (uid.isBlank()) {
            trySend(RemoteSnapshot(emptyList(), isFromCache = true))
            close()
            return@callbackFlow
        }

        val collectionRef = firestoreProvider.get()
            .collection("users").document(uid).collection("transactions")

        val registration = collectionRef.addSnapshotListener(
            MetadataChanges.EXCLUDE
        ) { snapshot, error ->
            if (error != null) {
                if (error.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                    Log.w(TAG, "PERMISSION_DENIED for transactions uid=$uid (user signed out)")
                    close()
                } else {
                    Log.e(TAG, "Snapshot listener error for transactions uid=$uid", error)
                    close(error)
                }
                return@addSnapshotListener
            }

            if (snapshot != null) {
                val changes = snapshot.documentChanges.mapNotNull { change ->
                    try {
                        val dto = change.document.toObject(
                            TransactionDto::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                        RemoteChange(change.type, dto.toDomain(change.document.id))
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing TransactionDto for doc ${change.document.id}", e)
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

    override fun addTransaction(uid: String, transaction: Transaction): Result<String> {
        return runCatchingResult {
            require(uid.isNotBlank()) { "User ID must not be blank" }

            val collection = firestoreProvider.get()
                .collection("users").document(uid).collection("transactions")
            val docId = transaction.id.ifBlank { collection.document().id }
            val docRef = collection.document(docId)

            val validatedTransaction = transaction.copy(id = docId)
            validatedTransaction.validate()

            val dto = validatedTransaction.toDto()
            val dataMap = dto.toMap(isUpdate = false)

            val task = docRef.set(dataMap)
            task.addOnFailureListener { e ->
                Log.e(TAG, "Async write failure for addTransaction id=$docId", e)
            }

            docId
        }
    }

    override fun updateTransaction(uid: String, transaction: Transaction): Result<Unit> {
        return runCatchingResult {
            require(uid.isNotBlank()) { "User ID must not be blank" }
            transaction.validate()

            val docRef = firestoreProvider.get()
                .collection("users").document(uid).collection("transactions").document(transaction.id)

            val dto = transaction.toDto()
            val dataMap = dto.toMap(isUpdate = true)

            val task = docRef.set(dataMap, SetOptions.merge())
            task.addOnFailureListener { e ->
                Log.e(TAG, "Async write failure for updateTransaction id=${transaction.id}", e)
            }
        }
    }

    override fun updateTransaction(uid: String, transactionId: String, updates: Map<String, Any?>): Result<Unit> {
        return runCatchingResult {
            require(uid.isNotBlank()) { "User ID must not be blank" }
            require(transactionId.isNotBlank()) { "Transaction ID must not be blank" }

            val docRef = firestoreProvider.get()
                .collection("users").document(uid).collection("transactions").document(transactionId)

            val dataMap = updates.toMutableMap()
            dataMap["updatedAt"] = FieldValue.serverTimestamp()

            val task = docRef.set(dataMap, SetOptions.merge())
            task.addOnFailureListener { e ->
                Log.e(TAG, "Async write failure for updateTransaction id=$transactionId", e)
            }
        }
    }

    override fun deleteTransaction(uid: String, transactionId: String): Result<Unit> {
        return runCatchingResult {
            require(uid.isNotBlank()) { "User ID must not be blank" }
            require(transactionId.isNotBlank()) { "Transaction ID must not be blank" }

            val docRef = firestoreProvider.get()
                .collection("users").document(uid).collection("transactions").document(transactionId)

            val updates = mapOf(
                "deletedAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )

            val task = docRef.set(updates, SetOptions.merge())
            task.addOnFailureListener { e ->
                Log.e(TAG, "Async write failure for deleteTransaction id=$transactionId", e)
            }
        }
    }

    companion object {
        private const val TAG = "TransactionRemoteDS"
    }
}
