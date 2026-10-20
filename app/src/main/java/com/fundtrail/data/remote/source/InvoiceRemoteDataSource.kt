package com.fundtrail.data.remote.source

import android.util.Log
import com.fundtrail.data.remote.dto.InvoiceDto
import com.fundtrail.data.remote.dto.toDto
import com.fundtrail.data.remote.dto.toDomain
import com.fundtrail.data.remote.dto.toMap
import com.fundtrail.data.remote.model.RemoteChange
import com.fundtrail.data.remote.model.RemoteSnapshot
import com.fundtrail.data.remote.model.toChangeType
import com.fundtrail.domain.model.Invoice
import com.fundtrail.domain.model.InvoiceStatus
import com.fundtrail.domain.model.Result
import com.fundtrail.domain.model.Transaction
import com.fundtrail.domain.model.TransactionStatus
import com.fundtrail.domain.model.TransactionType
import com.fundtrail.domain.model.runCatchingResult
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.Date
import javax.inject.Inject
import javax.inject.Provider

interface InvoiceRemoteDataSource {
    fun observeInvoices(uid: String): Flow<RemoteSnapshot<Invoice>>
    fun addInvoice(uid: String, invoice: Invoice): Result<String>
    fun updateInvoice(uid: String, invoice: Invoice): Result<Unit>
    fun updateInvoice(uid: String, invoiceId: String, updates: Map<String, Any?>): Result<Unit>
    fun deleteInvoice(uid: String, invoiceId: String): Result<Unit>
    fun markReceived(uid: String, invoiceId: String, transaction: Transaction): Result<Unit>
}

class InvoiceRemoteDataSourceImpl @Inject constructor(
    private val firestoreProvider: Provider<FirebaseFirestore>
) : InvoiceRemoteDataSource {

    override fun observeInvoices(uid: String): Flow<RemoteSnapshot<Invoice>> = callbackFlow {
        if (uid.isBlank()) {
            trySend(RemoteSnapshot(emptyList(), isFromCache = true))
            close()
            return@callbackFlow
        }

        val collectionRef = firestoreProvider.get()
            .collection("users").document(uid).collection("invoices")

        val registration = collectionRef.addSnapshotListener(
            MetadataChanges.EXCLUDE
        ) { snapshot, error ->
            if (error != null) {
                if (error.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                    Log.w(TAG, "PERMISSION_DENIED for invoices uid=$uid (user signed out)")
                    close()
                } else {
                    Log.e(TAG, "Snapshot listener error for invoices uid=$uid", error)
                    close(error)
                }
                return@addSnapshotListener
            }

            if (snapshot != null) {
                val changes = snapshot.documentChanges.mapNotNull { change ->
                    try {
                        val dto = change.document.toObject(
                            InvoiceDto::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                        RemoteChange(change.type.toChangeType(), dto.toDomain(change.document.id))
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing InvoiceDto for doc ${change.document.id}", e)
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

    override fun addInvoice(uid: String, invoice: Invoice): Result<String> {
        return runCatchingResult {
            require(uid.isNotBlank()) { "User ID must not be blank" }

            val collection = firestoreProvider.get()
                .collection("users").document(uid).collection("invoices")
            val docId = invoice.id.ifBlank { collection.document().id }
            val docRef = collection.document(docId)

            val validatedInvoice = invoice.copy(id = docId)
            validatedInvoice.validate()

            val dto = validatedInvoice.toDto()
            val dataMap = dto.toMap(isUpdate = false)

            val task = docRef.set(dataMap)
            task.addOnFailureListener { e ->
                Log.e(TAG, "Async write failure for addInvoice id=$docId", e)
            }

            docId
        }
    }

    override fun updateInvoice(uid: String, invoice: Invoice): Result<Unit> {
        return runCatchingResult {
            require(uid.isNotBlank()) { "User ID must not be blank" }
            invoice.validate()

            val docRef = firestoreProvider.get()
                .collection("users").document(uid).collection("invoices").document(invoice.id)

            val dto = invoice.toDto()
            val dataMap = dto.toMap(isUpdate = true)

            val task = docRef.set(dataMap, SetOptions.merge())
            task.addOnFailureListener { e ->
                Log.e(TAG, "Async write failure for updateInvoice id=${invoice.id}", e)
            }
        }
    }

    override fun updateInvoice(uid: String, invoiceId: String, updates: Map<String, Any?>): Result<Unit> {
        return runCatchingResult {
            require(uid.isNotBlank()) { "User ID must not be blank" }
            require(invoiceId.isNotBlank()) { "Invoice ID must not be blank" }

            val docRef = firestoreProvider.get()
                .collection("users").document(uid).collection("invoices").document(invoiceId)

            val dataMap = updates.toMutableMap()
            dataMap["updatedAt"] = FieldValue.serverTimestamp()

            val task = docRef.set(dataMap, SetOptions.merge())
            task.addOnFailureListener { e ->
                Log.e(TAG, "Async write failure for updateInvoice id=$invoiceId", e)
            }
        }
    }

    override fun deleteInvoice(uid: String, invoiceId: String): Result<Unit> {
        return runCatchingResult {
            require(uid.isNotBlank()) { "User ID must not be blank" }
            require(invoiceId.isNotBlank()) { "Invoice ID must not be blank" }

            val docRef = firestoreProvider.get()
                .collection("users").document(uid).collection("invoices").document(invoiceId)

            val updates = mapOf(
                "deletedAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )

            val task = docRef.set(updates, SetOptions.merge())
            task.addOnFailureListener { e ->
                Log.e(TAG, "Async write failure for deleteInvoice id=$invoiceId", e)
            }
        }
    }

    override fun markReceived(uid: String, invoiceId: String, transaction: Transaction): Result<Unit> {
        return runCatchingResult {
            require(uid.isNotBlank()) { "User ID must not be blank" }
            require(invoiceId.isNotBlank()) { "Invoice ID must not be blank" }
            transaction.validate()
            require(transaction.type == TransactionType.INCOME) { "Settling transaction must be INCOME" }
            require(transaction.status == TransactionStatus.ACTUAL) { "Settling transaction must be ACTUAL" }

            val firestore = firestoreProvider.get()
            val invoiceRef = firestore
                .collection("users").document(uid).collection("invoices").document(invoiceId)

            val txCollection = firestore
                .collection("users").document(uid).collection("transactions")
            val txDocId = transaction.id.ifBlank { txCollection.document().id }
            val txRef = txCollection.document(txDocId)

            val settlingTx = transaction.copy(id = txDocId, invoiceId = invoiceId)
            settlingTx.validate()

            val batch = firestore.batch()

            val invoiceUpdates = mapOf(
                "status" to InvoiceStatus.RECEIVED.name,
                "receivedTransactionId" to txDocId,
                "receivedAt" to Timestamp(Date(settlingTx.occurredAt)),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            batch.set(invoiceRef, invoiceUpdates, SetOptions.merge())

            val txDto = settlingTx.toDto()
            val txDataMap = txDto.toMap(isUpdate = false)
            batch.set(txRef, txDataMap)

            val task = batch.commit()
            task.addOnFailureListener { e ->
                Log.e(TAG, "Async write failure for markReceived invoiceId=$invoiceId txId=$txDocId", e)
            }
        }
    }

    companion object {
        private const val TAG = "InvoiceRemoteDS"
    }
}
