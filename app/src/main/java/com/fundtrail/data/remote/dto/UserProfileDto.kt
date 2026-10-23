package com.fundtrail.data.remote.dto

import com.fundtrail.domain.model.UserSettings
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ServerTimestamp

/** Current `schemaVersion` written for new profile documents (schema.md §3.1). */
const val CURRENT_SCHEMA_VERSION = 1

/** Firestore DTO for the `users/{uid}` profile document. */
data class UserProfileDto(
    val displayName: String = "",
    val incomeEstimateMinMinor: Long? = null,
    val incomeEstimateMaxMinor: Long? = null,
    val expectedSalaryMinor: Long? = null,
    val salaryPayday: Int = 25,
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
    val deletedAt: Timestamp? = null
)

fun UserProfileDto.toDomain(): UserSettings = UserSettings(
    incomeEstimateMinMinor = incomeEstimateMinMinor,
    incomeEstimateMaxMinor = incomeEstimateMaxMinor,
    expectedSalaryMinor = expectedSalaryMinor,
    salaryPayday = salaryPayday,
    createdAt = createdAt?.toDate()?.time,
)

/** [displayName] is only written when the profile document doesn't exist yet (schema.md §6.4: owner fields never change after creation). */
fun UserSettings.toMap(displayNameOnCreate: String?): Map<String, Any?> {
    val map = mutableMapOf<String, Any?>(
        "incomeEstimateMinMinor" to incomeEstimateMinMinor,
        "incomeEstimateMaxMinor" to incomeEstimateMaxMinor,
        "expectedSalaryMinor" to expectedSalaryMinor,
        "salaryPayday" to salaryPayday,
        "updatedAt" to FieldValue.serverTimestamp()
    )
    if (displayNameOnCreate != null) {
        map["displayName"] = displayNameOnCreate
        map["schemaVersion"] = CURRENT_SCHEMA_VERSION
        map["createdAt"] = FieldValue.serverTimestamp()
    }
    return map
}
