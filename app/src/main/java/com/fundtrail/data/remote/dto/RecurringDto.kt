package com.fundtrail.data.remote.dto

import com.fundtrail.domain.model.RecurringTemplate
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Date

/** Firestore DTO for `users/{uid}/recurring/{id}` documents (`Docs/schema.md` section 3.6). */
data class RecurringDto(
    @DocumentId val id: String = "",
    val name: String = "",
    val categoryId: String = "",
    val accountId: String = "",
    val expectedAmountMinor: Long = 0L,
    // Explicit names: Firestore's bean mapping would otherwise store `is`-prefixed booleans without the prefix.
    @get:PropertyName("isVariableAmount")
    @set:PropertyName("isVariableAmount")
    var isVariableAmount: Boolean = false,
    val dayOfMonth: Int = 1,
    val nextDueDate: Timestamp? = null,
    @get:PropertyName("isPaused")
    @set:PropertyName("isPaused")
    var isPaused: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
    val deletedAt: Timestamp? = null
)

/** The calendar day a `nextDueDate` timestamp stands for: stored at UTC midnight, like invoice `dueAt`. */
fun LocalDate.toDueTimestamp(): Timestamp = Timestamp(Date.from(atStartOfDay(ZoneOffset.UTC).toInstant()))

/**
 * Throws when `nextDueDate` is missing: a made-up date would make the template silently due (or never due).
 * The snapshot listener catches the exception, logs the document id and skips it.
 */
fun RecurringDto.toDomain(documentId: String = id): RecurringTemplate {
    val resolvedId = documentId.ifBlank { id }
    val due = requireNotNull(nextDueDate) { "Recurring template $resolvedId has no nextDueDate" }
        .toDate().toInstant().atZone(ZoneOffset.UTC).toLocalDate()
    return RecurringTemplate(
        id = resolvedId,
        name = name,
        categoryId = categoryId,
        accountId = accountId,
        expectedAmountMinor = expectedAmountMinor,
        isVariableAmount = isVariableAmount,
        dayOfMonth = dayOfMonth,
        nextDueDate = due,
        isPaused = isPaused,
        createdAt = createdAt?.toDate()?.time,
        updatedAt = updatedAt?.toDate()?.time,
        deletedAt = deletedAt?.toDate()?.time
    )
}

fun RecurringTemplate.toDto(): RecurringDto = RecurringDto(
    id = id,
    name = name,
    categoryId = categoryId,
    accountId = accountId,
    expectedAmountMinor = expectedAmountMinor,
    isVariableAmount = isVariableAmount,
    dayOfMonth = dayOfMonth,
    nextDueDate = nextDueDate.toDueTimestamp(),
    isPaused = isPaused,
    createdAt = createdAt?.let { Timestamp(Date(it)) },
    updatedAt = updatedAt?.let { Timestamp(Date(it)) },
    deletedAt = deletedAt?.let { Timestamp(Date(it)) }
)

/**
 * The create map carries every field. The update map leaves out `nextDueDate`: only `confirm`/`skip` move the
 * cursor, so an edit built from a stale copy can't rewind it and make an already-confirmed month due again.
 */
fun RecurringDto.toMap(isUpdate: Boolean): Map<String, Any?> {
    val map = mutableMapOf<String, Any?>(
        "name" to name,
        "categoryId" to categoryId,
        "accountId" to accountId,
        "expectedAmountMinor" to expectedAmountMinor,
        "isVariableAmount" to isVariableAmount,
        "dayOfMonth" to dayOfMonth,
        "isPaused" to isPaused,
        "updatedAt" to FieldValue.serverTimestamp()
    )
    if (isUpdate) return map
    map["nextDueDate"] = nextDueDate
    map["createdAt"] = FieldValue.serverTimestamp()
    return map
}
