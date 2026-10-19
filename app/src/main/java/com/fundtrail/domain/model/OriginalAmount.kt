package com.fundtrail.domain.model

/**
 * Details for transactions entered in a foreign currency.
 *
 * @property currency ISO currency code (e.g. "USD", "USDT")
 * @property amountMinor Amount in the currency's minor units
 * @property fxRateMicros LKR per 1 unit x 10^6 (for audit and display)
 */
data class OriginalAmount(
    val currency: String,
    val amountMinor: Long,
    val fxRateMicros: Long
)
