package com.fundtrail.data.remote.dto

import com.fundtrail.domain.model.Category
import com.fundtrail.domain.model.SpendKind
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * Firestore DTO for `users/{uid}/categories/{id}` documents.
 */
data class CategoryDto(
    @DocumentId val id: String = "",
    val name: String = "",
    val defaultSpendKind: String = "",
    val icon: String = "",
    val archived: Boolean = false,
    val sortOrder: Int = 0,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

fun CategoryDto.toDomain(documentId: String = id): Category {
    val resolvedSpendKind = SpendKind.entries.find { it.name.equals(defaultSpendKind, ignoreCase = true) }
        ?: SpendKind.COMMITTED
    return Category(
        id = documentId.ifBlank { id },
        name = name,
        defaultSpendKind = resolvedSpendKind,
        icon = icon,
        archived = archived,
        sortOrder = sortOrder,
        createdAt = createdAt?.toDate()?.time,
        updatedAt = updatedAt?.toDate()?.time
    )
}

fun Category.toDto(): CategoryDto {
    return CategoryDto(
        id = id,
        name = name,
        defaultSpendKind = defaultSpendKind.name,
        icon = icon,
        archived = archived,
        sortOrder = sortOrder,
        createdAt = createdAt?.let { Timestamp(Date(it)) },
        updatedAt = updatedAt?.let { Timestamp(Date(it)) }
    )
}

fun CategoryDto.toMap(isUpdate: Boolean): Map<String, Any?> {
    val map = mutableMapOf<String, Any?>(
        "name" to name,
        "defaultSpendKind" to defaultSpendKind,
        "icon" to icon,
        "archived" to archived,
        "sortOrder" to sortOrder,
        "updatedAt" to FieldValue.serverTimestamp()
    )
    if (!isUpdate) {
        map["createdAt"] = FieldValue.serverTimestamp()
    }
    return map
}
