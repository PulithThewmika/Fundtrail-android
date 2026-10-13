package com.fundtrail.data.remote.dto

import com.fundtrail.domain.model.IncomeSource
import com.fundtrail.domain.model.SpendKind
import com.fundtrail.domain.model.Transaction
import com.fundtrail.domain.model.TransactionStatus
import com.fundtrail.domain.model.TransactionType
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * Firestore DTO for `users/{uid}/transactions/{id}` documents.
 */
data class TransactionDto(
    @DocumentId val id: String = "",
    val type: String = "",
    val status: String = "ACTUAL",
    val amountLkrMinor: Long = 0L,
    val accountId: String = "",
    val toAccountId: String? = null,
    val categoryId: String? = null,
    val spendKind: String? = null,
    val incomeSource: String? = null,
    val occurredAt: Timestamp? = null,
    val original: OriginalAmountDto? = null,
    val note: String? = null,
    val recurringId: String? = null,
    val expectedAmountLkrMinor: Long? = null,
    val invoiceId: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
    val deletedAt: Timestamp? = null
)

fun TransactionDto.toDomain(documentId: String = id): Transaction {
    val resolvedType = TransactionType.entries.find { it.name.equals(type, ignoreCase = true) }
        ?: TransactionType.EXPENSE
    val resolvedStatus = TransactionStatus.entries.find { it.name.equals(status, ignoreCase = true) }
        ?: TransactionStatus.ACTUAL
    val resolvedSpendKind = spendKind?.let { sk ->
        SpendKind.entries.find { it.name.equals(sk, ignoreCase = true) }
    }
    val resolvedIncomeSource = incomeSource?.let { isrc ->
        IncomeSource.entries.find { it.name.equals(isrc, ignoreCase = true) }
    }

    return Transaction(
        id = documentId.ifBlank { id },
        type = resolvedType,
        status = resolvedStatus,
        amountLkrMinor = amountLkrMinor,
        accountId = accountId,
        toAccountId = toAccountId,
        categoryId = categoryId,
        spendKind = resolvedSpendKind,
        incomeSource = resolvedIncomeSource,
        occurredAt = occurredAt?.toDate()?.time ?: 0L,
        original = original?.toDomain(),
        note = note,
        recurringId = recurringId,
        expectedAmountLkrMinor = expectedAmountLkrMinor,
        invoiceId = invoiceId,
        createdAt = createdAt?.toDate()?.time,
        updatedAt = updatedAt?.toDate()?.time,
        deletedAt = deletedAt?.toDate()?.time
    )
}

fun Transaction.toDto(): TransactionDto {
    return TransactionDto(
        id = id,
        type = type.name,
        status = status.name,
        amountLkrMinor = amountLkrMinor,
        accountId = accountId,
        toAccountId = toAccountId,
        categoryId = categoryId,
        spendKind = spendKind?.name,
        incomeSource = incomeSource?.name,
        occurredAt = Timestamp(Date(occurredAt)),
        original = original?.toDto(),
        note = note,
        recurringId = recurringId,
        expectedAmountLkrMinor = expectedAmountLkrMinor,
        invoiceId = invoiceId,
        createdAt = createdAt?.let { Timestamp(Date(it)) },
        updatedAt = updatedAt?.let { Timestamp(Date(it)) },
        deletedAt = deletedAt?.let { Timestamp(Date(it)) }
    )
}

fun TransactionDto.toMap(isUpdate: Boolean): Map<String, Any?> {
    val map = mutableMapOf<String, Any?>(
        "type" to type,
        "status" to status,
        "amountLkrMinor" to amountLkrMinor,
        "accountId" to accountId,
        "toAccountId" to toAccountId,
        "categoryId" to categoryId,
        "spendKind" to spendKind,
        "incomeSource" to incomeSource,
        "occurredAt" to occurredAt,
        "original" to original?.toMap(),
        "note" to note,
        "recurringId" to recurringId,
        "expectedAmountLkrMinor" to expectedAmountLkrMinor,
        "invoiceId" to invoiceId,
        "updatedAt" to FieldValue.serverTimestamp(),
        "deletedAt" to deletedAt
    )
    if (!isUpdate) {
        map["createdAt"] = FieldValue.serverTimestamp()
    }
    return map
}
