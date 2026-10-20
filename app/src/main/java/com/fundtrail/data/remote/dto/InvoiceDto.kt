package com.fundtrail.data.remote.dto

import com.fundtrail.domain.model.Invoice
import com.fundtrail.domain.model.InvoiceStatus
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ServerTimestamp
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Date

/**
 * Firestore DTO for `users/{uid}/invoices/{id}` documents.
 */
data class InvoiceDto(
    @DocumentId val id: String = "",
    val client: String = "",
    val project: String = "",
    val milestone: String? = null,
    val amountMinor: Long = 0L,
    val currency: String = "LKR",
    val dueDate: Timestamp? = null,
    val status: String = "PENDING",
    val receivedTransactionId: String? = null,
    val receivedAt: Timestamp? = null,
    val lastNotifiedAt: Timestamp? = null,
    val note: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
    val deletedAt: Timestamp? = null
)

fun InvoiceDto.toDomain(documentId: String = id): Invoice {
    val resolvedStatus = InvoiceStatus.entries.find { it.name.equals(status, ignoreCase = true) }
        ?: InvoiceStatus.PENDING
    val resolvedDueDate = dueDate?.toDate()?.toInstant()?.atZone(ZoneOffset.UTC)?.toLocalDate()
        ?: LocalDate.now()

    return Invoice(
        id = documentId.ifBlank { id },
        client = client,
        project = project,
        milestone = milestone,
        amountMinor = amountMinor,
        currency = currency.ifBlank { "LKR" },
        dueDate = resolvedDueDate,
        status = resolvedStatus,
        receivedTransactionId = receivedTransactionId,
        receivedAt = receivedAt?.toDate()?.time,
        lastNotifiedAt = lastNotifiedAt?.toDate()?.time,
        note = note,
        createdAt = createdAt?.toDate()?.time,
        updatedAt = updatedAt?.toDate()?.time,
        deletedAt = deletedAt?.toDate()?.time
    )
}

fun Invoice.toDto(): InvoiceDto {
    val dueDateTimestamp = Timestamp(Date.from(dueDate.atStartOfDay(ZoneOffset.UTC).toInstant()))
    return InvoiceDto(
        id = id,
        client = client,
        project = project,
        milestone = milestone,
        amountMinor = amountMinor,
        currency = currency,
        dueDate = dueDateTimestamp,
        status = status.name,
        receivedTransactionId = receivedTransactionId,
        receivedAt = receivedAt?.let { Timestamp(Date(it)) },
        lastNotifiedAt = lastNotifiedAt?.let { Timestamp(Date(it)) },
        note = note,
        createdAt = createdAt?.let { Timestamp(Date(it)) },
        updatedAt = updatedAt?.let { Timestamp(Date(it)) },
        deletedAt = deletedAt?.let { Timestamp(Date(it)) }
    )
}

fun InvoiceDto.toMap(isUpdate: Boolean): Map<String, Any?> {
    val map = mutableMapOf<String, Any?>(
        "client" to client,
        "project" to project,
        "milestone" to milestone,
        "amountMinor" to amountMinor,
        "currency" to currency,
        "dueDate" to dueDate,
        "status" to status,
        "receivedTransactionId" to receivedTransactionId,
        "receivedAt" to receivedAt,
        "lastNotifiedAt" to lastNotifiedAt,
        "note" to note,
        "updatedAt" to FieldValue.serverTimestamp()
    )
    if (!isUpdate) {
        map["createdAt"] = FieldValue.serverTimestamp()
        deletedAt?.let { map["deletedAt"] = it }
    }
    return map
}
