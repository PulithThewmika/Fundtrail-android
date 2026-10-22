package com.fundtrail.data.remote.dto

import com.fundtrail.domain.model.Goal
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Date

/**
 * Firestore DTO for `users/{uid}/goals/{id}` documents.
 *
 * [deadline] and [startedAt] are stored at UTC midnight / the UTC instant they were set, so the
 * stored value doesn't depend on the writing device's timezone (schema.md §3.7).
 */
data class GoalDto(
    @DocumentId val id: String = "",
    val name: String = "",
    val targetLkrMinor: Long = 0L,
    val baselineSavedMinor: Long = 0L,
    val startedAt: Timestamp? = null,
    val deadline: Timestamp? = null,
    // Explicit name: Firestore's bean mapper would otherwise read the Kotlin getter isActive() as a field
    // called `active`, ignore the stored `isActive`, and bring every archived goal back as active.
    @get:PropertyName("isActive")
    @set:PropertyName("isActive")
    var isActive: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
    val deletedAt: Timestamp? = null
)

fun GoalDto.toDomain(documentId: String = id): Goal {
    // The goal's start is startedAt: client-set at creation and always present, even while the create is still
    // pending offline (schema.md §6.5) and createdAt reads back null. Only a document without it falls back.
    val createdMillis = startedAt?.toDate()?.time ?: createdAt?.toDate()?.time
    return Goal(
        id = documentId.ifBlank { id },
        name = name,
        targetLkrMinor = targetLkrMinor,
        deadline = deadline?.toDate()?.toInstant()?.atZone(ZoneOffset.UTC)?.toLocalDate() ?: LocalDate.MIN,
        baselineSavedMinor = baselineSavedMinor,
        createdAt = createdMillis,
        updatedAt = updatedAt?.toDate()?.time,
        isActive = isActive
    )
}

fun Goal.deadlineTimestamp(): Timestamp = Timestamp(Date.from(deadline.atStartOfDay(ZoneOffset.UTC).toInstant()))

/**
 * [isUpdate] controls which immutable-on-create fields are written: `startedAt` and `createdAt`
 * are set only on create, never touched again (schema.md §6.4). `startedAt` is [now] unless the goal
 * already carries a start ([Goal.createdAt]), e.g. a back-dated demo goal; progress is measured from it.
 */
fun Goal.toMap(isUpdate: Boolean, now: Instant): Map<String, Any?> {
    val map = mutableMapOf<String, Any?>(
        "name" to name,
        "targetLkrMinor" to targetLkrMinor,
        "baselineSavedMinor" to baselineSavedMinor,
        "deadline" to deadlineTimestamp(),
        "isActive" to isActive,
        "updatedAt" to FieldValue.serverTimestamp()
    )
    if (!isUpdate) {
        map["startedAt"] = Timestamp(Date.from(createdAt?.let(Instant::ofEpochMilli) ?: now))
        map["createdAt"] = FieldValue.serverTimestamp()
    }
    return map
}
