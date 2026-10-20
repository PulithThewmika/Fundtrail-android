package com.fundtrail.data.remote.dto

import com.fundtrail.domain.model.Goal
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
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
    val isActive: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
    val deletedAt: Timestamp? = null
)

fun GoalDto.toDomain(documentId: String = id): Goal {
    // createdAt reads back null while a create is still pending offline (schema.md §6.5);
    // startedAt is client-set and always present, so it's the fallback.
    val createdMillis = createdAt?.toDate()?.time ?: startedAt?.toDate()?.time
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
 * are set only on create, never touched again (schema.md §6.4).
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
        map["startedAt"] = Timestamp(Date.from(now))
        map["createdAt"] = FieldValue.serverTimestamp()
    }
    return map
}
