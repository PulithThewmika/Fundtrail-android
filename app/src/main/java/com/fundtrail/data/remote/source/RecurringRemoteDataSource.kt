package com.fundtrail.data.remote.source

import android.util.Log
import com.fundtrail.data.remote.dto.RecurringDto
import com.fundtrail.data.remote.dto.toDomain
import com.fundtrail.data.remote.dto.toDto
import com.fundtrail.data.remote.dto.toDueTimestamp
import com.fundtrail.data.remote.dto.toMap
import com.fundtrail.data.remote.model.RemoteChange
import com.fundtrail.data.remote.model.RemoteSnapshot
import com.fundtrail.data.remote.model.toChangeType
import com.fundtrail.domain.model.RecurringTemplate
import com.fundtrail.domain.model.Result
import com.fundtrail.domain.model.Transaction
import com.fundtrail.domain.model.TransactionStatus
import com.fundtrail.domain.model.TransactionType
import com.fundtrail.domain.model.runCatchingResult
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.MetadataChanges
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Provider

interface RecurringRemoteDataSource {
    fun observeRecurring(uid: String): Flow<RemoteSnapshot<RecurringTemplate>>
    fun addRecurring(uid: String, template: RecurringTemplate): Result<String>
    fun updateRecurring(uid: String, template: RecurringTemplate): Result<Unit>
    fun pauseRecurring(uid: String, templateId: String): Result<Unit>
    fun deleteRecurring(uid: String, templateId: String): Result<Unit>

    /**
     * One atomic batch (schema.md §6.4): the confirmed EXPENSE transaction (with `recurringId`) is written and
     * the template cursor moves to [nextDueDate], or neither happens.
     */
    fun confirm(uid: String, templateId: String, transaction: Transaction, nextDueDate: LocalDate): Result<Unit>

    /** Moves the template cursor to [nextDueDate]; no transaction is written. */
    fun skip(uid: String, templateId: String, nextDueDate: LocalDate): Result<Unit>
}

class RecurringRemoteDataSourceImpl @Inject constructor(
    private val firestoreProvider: Provider<FirebaseFirestore>
) : RecurringRemoteDataSource {

    override fun observeRecurring(uid: String): Flow<RemoteSnapshot<RecurringTemplate>> = callbackFlow {
        if (uid.isBlank()) {
            trySend(RemoteSnapshot(emptyList(), isFromCache = true))
            close()
            return@callbackFlow
        }

        val collectionRef = firestoreProvider.get()
            .collection("users").document(uid).collection("recurring")

        val registration = collectionRef.addSnapshotListener(
            MetadataChanges.EXCLUDE
        ) { snapshot, error ->
            if (error != null) {
                if (error.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                    Log.w(TAG, "PERMISSION_DENIED for recurring uid=$uid (user signed out)")
                    close()
                } else {
                    Log.e(TAG, "Snapshot listener error for recurring uid=$uid", error)
                    close(error)
                }
                return@addSnapshotListener
            }

            if (snapshot != null) {
                val changes = snapshot.documentChanges.mapNotNull { change ->
                    try {
                        val dto = change.document.toObject(
                            RecurringDto::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                        RemoteChange(change.type.toChangeType(), dto.toDomain(change.document.id))
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing RecurringDto for doc ${change.document.id}", e)
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

    override fun addRecurring(uid: String, template: RecurringTemplate): Result<String> {
        return runCatchingResult {
            require(uid.isNotBlank()) { "User ID must not be blank" }

            val collection = firestoreProvider.get()
                .collection("users").document(uid).collection("recurring")
            val docId = template.id.ifBlank { collection.document().id }

            val validated = template.copy(id = docId)
            validated.validate()

            val task = collection.document(docId).set(validated.toDto().toMap(isUpdate = false))
            task.addOnFailureListener { e ->
                Log.e(TAG, "Async write failure for addRecurring id=$docId", e)
            }

            docId
        }
    }

    override fun updateRecurring(uid: String, template: RecurringTemplate): Result<Unit> {
        return runCatchingResult {
            require(uid.isNotBlank()) { "User ID must not be blank" }
            template.validate()

            val docRef = firestoreProvider.get()
                .collection("users").document(uid).collection("recurring").document(template.id)

            // update(), not set(merge): a missing document must fail, not be created as a skeleton.
            val task = docRef.update(template.toDto().toMap(isUpdate = true))
            task.addOnFailureListener { e ->
                Log.e(TAG, "Async write failure for updateRecurring id=${template.id}", e)
            }
        }
    }

    override fun pauseRecurring(uid: String, templateId: String): Result<Unit> {
        return runCatchingResult {
            require(uid.isNotBlank()) { "User ID must not be blank" }
            require(templateId.isNotBlank()) { "Recurring template ID must not be blank" }

            val docRef = firestoreProvider.get()
                .collection("users").document(uid).collection("recurring").document(templateId)

            val task = docRef.update(mapOf("isPaused" to true, "updatedAt" to FieldValue.serverTimestamp()))
            task.addOnFailureListener { e ->
                Log.e(TAG, "Async write failure for pauseRecurring id=$templateId", e)
            }
        }
    }

    override fun deleteRecurring(uid: String, templateId: String): Result<Unit> {
        return runCatchingResult {
            require(uid.isNotBlank()) { "User ID must not be blank" }
            require(templateId.isNotBlank()) { "Recurring template ID must not be blank" }

            val docRef = firestoreProvider.get()
                .collection("users").document(uid).collection("recurring").document(templateId)

            val task = docRef.update(mapOf("deletedAt" to FieldValue.serverTimestamp(), "updatedAt" to FieldValue.serverTimestamp()))
            task.addOnFailureListener { e ->
                Log.e(TAG, "Async write failure for deleteRecurring id=$templateId", e)
            }
        }
    }

    override fun confirm(uid: String, templateId: String, transaction: Transaction, nextDueDate: LocalDate): Result<Unit> {
        return runCatchingResult {
            require(uid.isNotBlank()) { "User ID must not be blank" }
            require(templateId.isNotBlank()) { "Recurring template ID must not be blank" }
            transaction.validate()
            require(transaction.type == TransactionType.EXPENSE) { "Confirmed recurring item must be an EXPENSE" }
            require(transaction.status == TransactionStatus.ACTUAL) { "Confirmed recurring item must be ACTUAL" }
            require(transaction.recurringId == templateId) { "Transaction must carry recurringId=$templateId" }

            val firestore = firestoreProvider.get()
            val templateRef = firestore
                .collection("users").document(uid).collection("recurring").document(templateId)
            val txCollection = firestore
                .collection("users").document(uid).collection("transactions")
            val txDocId = transaction.id.ifBlank { txCollection.document().id }

            val batch = firestore.batch()
            batch.set(txCollection.document(txDocId), transaction.copy(id = txDocId).toDto().toMap(isUpdate = false))
            // update(), not set(merge): set would create a blank template if this id does not exist, and the batch
            // would still commit. update() fails the batch, so neither write lands.
            batch.update(templateRef, cursorUpdate(nextDueDate))

            val task = batch.commit()
            task.addOnFailureListener { e ->
                Log.e(TAG, "Async write failure for confirm templateId=$templateId txId=$txDocId", e)
            }
        }
    }

    override fun skip(uid: String, templateId: String, nextDueDate: LocalDate): Result<Unit> {
        return runCatchingResult {
            require(uid.isNotBlank()) { "User ID must not be blank" }
            require(templateId.isNotBlank()) { "Recurring template ID must not be blank" }

            val templateRef = firestoreProvider.get()
                .collection("users").document(uid).collection("recurring").document(templateId)

            // A single-document write is already atomic; a batch adds nothing here.
            val task = templateRef.update(cursorUpdate(nextDueDate))
            task.addOnFailureListener { e ->
                Log.e(TAG, "Async write failure for skip templateId=$templateId", e)
            }
        }
    }

    private fun cursorUpdate(nextDueDate: LocalDate): Map<String, Any> = mapOf(
        "nextDueDate" to nextDueDate.toDueTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    companion object {
        private const val TAG = "RecurringRemoteDS"
    }
}
