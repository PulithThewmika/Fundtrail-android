package com.fundtrail.data.remote.dto

import com.fundtrail.domain.model.Account
import com.fundtrail.domain.model.AccountKind
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * Firestore DTO for `users/{uid}/accounts/{id}` documents.
 */
data class AccountDto(
    @DocumentId val id: String = "",
    val name: String = "",
    val kind: String = "",
    val currency: String = "LKR",
    val openingBalanceLkrMinor: Long = 0L,
    val openingBalanceAt: Timestamp? = null,
    val archived: Boolean = false,
    val sortOrder: Int = 0,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

fun AccountDto.toDomain(documentId: String = id): Account {
    val resolvedKind = AccountKind.entries.find { it.name.equals(kind, ignoreCase = true) } ?: AccountKind.OTHER
    return Account(
        id = documentId.ifBlank { id },
        name = name,
        kind = resolvedKind,
        currency = currency.ifBlank { "LKR" },
        openingBalanceLkrMinor = openingBalanceLkrMinor,
        openingBalanceAt = openingBalanceAt?.toDate()?.time ?: 0L,
        archived = archived,
        sortOrder = sortOrder,
        createdAt = createdAt?.toDate()?.time,
        updatedAt = updatedAt?.toDate()?.time
    )
}

fun Account.toDto(): AccountDto {
    return AccountDto(
        id = id,
        name = name,
        kind = kind.name,
        currency = currency,
        openingBalanceLkrMinor = openingBalanceLkrMinor,
        openingBalanceAt = Timestamp(Date(openingBalanceAt)),
        archived = archived,
        sortOrder = sortOrder,
        createdAt = createdAt?.let { Timestamp(Date(it)) },
        updatedAt = updatedAt?.let { Timestamp(Date(it)) }
    )
}

fun AccountDto.toMap(isUpdate: Boolean): Map<String, Any?> {
    val map = mutableMapOf<String, Any?>(
        "name" to name,
        "kind" to kind,
        "currency" to currency,
        "openingBalanceLkrMinor" to openingBalanceLkrMinor,
        "openingBalanceAt" to openingBalanceAt,
        "archived" to archived,
        "sortOrder" to sortOrder,
        "updatedAt" to FieldValue.serverTimestamp()
    )
    if (!isUpdate) {
        map["createdAt"] = FieldValue.serverTimestamp()
    }
    return map
}
