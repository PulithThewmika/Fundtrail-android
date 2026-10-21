package com.fundtrail.data.remote.dto

import com.fundtrail.domain.model.Invoice
import com.fundtrail.domain.model.InvoiceStatus
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ServerTimestamp
import java.time.ZoneOffset
import java.util.Date

/**
 * Firestore DTO for `users/{uid}/invoices/{id}` documents. Field names follow `Docs/schema.md` section 3.5,
 * which differs from the domain model in four places: `clientName`, `projectName`, `dueAt` and `receivedTxId`.
 * The firestore.indexes.json exemptions for the free-text fields rely on these exact names.
 */
data class InvoiceDto(
    @DocumentId val id: String = "",
    val clientName: String = "",
    val projectName: String = "",
    val milestone: String? = null,
    val amountMinor: Long = 0L,
    val currency: String = "LKR",
    val issuedAt: Timestamp? = null,
    val dueAt: Timestamp? = null,
    val status: String = "PENDING",
    val receivedTxId: String? = null,
    val receivedAt: Timestamp? = null,
    val lastNotifiedAt: Timestamp? = null,
    val note: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
    val deletedAt: Timestamp? = null
)

/**
 * Throws when `dueAt` is missing. Defaulting it to a made-up date would quietly pin the invoice as
 * never overdue, and overdue drives FR-51 and the reminder (FR-53). The snapshot listener catches the
 * exception, logs the document id and skips it.
 */
fun InvoiceDto.toDomain(documentId: String = id): Invoice {
    val resolvedId = documentId.ifBlank { id }
    val resolvedStatus = InvoiceStatus.entries.find { it.name.equals(status, ignoreCase = true) }
        ?: InvoiceStatus.PENDING
    val due = requireNotNull(dueAt) { "Invoice $resolvedId has no dueAt" }
        .toDate().toInstant().atZone(ZoneOffset.UTC).toLocalDate()

    return Invoice(
        id = resolvedId,
        client = clientName,
        project = projectName,
        milestone = milestone,
        amountMinor = amountMinor,
        currency = currency.ifBlank { "LKR" },
        dueDate = due,
        status = resolvedStatus,
        receivedTransactionId = receivedTxId,
        receivedAt = receivedAt?.toDate()?.time,
        lastNotifiedAt = lastNotifiedAt?.toDate()?.time,
        note = note,
        createdAt = createdAt?.toDate()?.time,
        updatedAt = updatedAt?.toDate()?.time,
        deletedAt = deletedAt?.toDate()?.time,
        issuedAt = issuedAt?.toDate()?.time
    )
}

fun Invoice.toDto(): InvoiceDto = InvoiceDto(
    id = id,
    clientName = client,
    projectName = project,
    milestone = milestone,
    amountMinor = amountMinor,
    currency = currency,
    issuedAt = issuedAt?.let { Timestamp(Date(it)) },
    dueAt = Timestamp(Date.from(dueDate.atStartOfDay(ZoneOffset.UTC).toInstant())),
    status = status.name,
    receivedTxId = receivedTransactionId,
    receivedAt = receivedAt?.let { Timestamp(Date(it)) },
    lastNotifiedAt = lastNotifiedAt?.let { Timestamp(Date(it)) },
    note = note,
    createdAt = createdAt?.let { Timestamp(Date(it)) },
    updatedAt = updatedAt?.let { Timestamp(Date(it)) },
    deletedAt = deletedAt?.let { Timestamp(Date(it)) }
)

/**
 * The create map carries every field. The update map carries only what a user edits: `status`,
 * `receivedTxId` and `receivedAt` belong to `markReceived`, and `lastNotifiedAt` to the reminder, so an
 * edit built from a stale copy must not write them back (BR-15: a payment links to exactly one milestone).
 */
fun InvoiceDto.toMap(isUpdate: Boolean): Map<String, Any?> {
    val map = mutableMapOf<String, Any?>(
        "clientName" to clientName,
        "projectName" to projectName,
        "milestone" to milestone,
        "amountMinor" to amountMinor,
        "currency" to currency,
        "dueAt" to dueAt,
        "note" to note,
        "updatedAt" to FieldValue.serverTimestamp()
    )
    if (isUpdate) {
        issuedAt?.let { map["issuedAt"] = it }
        return map
    }
    map["issuedAt"] = issuedAt ?: FieldValue.serverTimestamp()
    map["status"] = status
    map["receivedTxId"] = receivedTxId
    map["receivedAt"] = receivedAt
    map["lastNotifiedAt"] = lastNotifiedAt
    map["createdAt"] = FieldValue.serverTimestamp()
    return map
}
