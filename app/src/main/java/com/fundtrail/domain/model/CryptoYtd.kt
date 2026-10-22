package com.fundtrail.domain.model

/**
 * Realised crypto running year-to-date net position (A-15, FR-55).
 *
 * @property year The calendar year
 * @property netLkrMinor Running net profit/loss in LKR minor units (signed)
 * @property transactionCount Total number of realised crypto transactions this year
 */
data class CryptoYtd(
    val year: Int,
    val netLkrMinor: Long,
    val transactionCount: Int
)
