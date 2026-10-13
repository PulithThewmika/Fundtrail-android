package com.fundtrail.data.remote.dto

import com.fundtrail.domain.model.OriginalAmount

/**
 * Embedded map for foreign currency details in a Firestore transaction document.
 */
data class OriginalAmountDto(
    val currency: String = "",
    val amountMinor: Long = 0L,
    val fxRateMicros: Long = 0L
)

fun OriginalAmountDto.toDomain(): OriginalAmount {
    return OriginalAmount(
        currency = currency,
        amountMinor = amountMinor,
        fxRateMicros = fxRateMicros
    )
}

fun OriginalAmount.toDto(): OriginalAmountDto {
    return OriginalAmountDto(
        currency = currency,
        amountMinor = amountMinor,
        fxRateMicros = fxRateMicros
    )
}

fun OriginalAmountDto.toMap(): Map<String, Any> {
    return mapOf(
        "currency" to currency,
        "amountMinor" to amountMinor,
        "fxRateMicros" to fxRateMicros
    )
}
