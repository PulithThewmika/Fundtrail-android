package com.fundtrail.domain.model

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * The only place a foreign income amount becomes LKR, or an LKR amount becomes a rate (#174, ADR-0003).
 *
 * Every step is `BigDecimal` with [RoundingMode.HALF_UP] and rounds exactly once, at entry: money never
 * passes through `Float`/`Double`, and the rate stored with the entry locks the LKR value for good, so no
 * later rate change can move history.
 */
object FxConversion {
    /** A rate is held as LKR per 1 unit x 10^6, so micros is the scale both directions divide by. */
    private val MICROS: BigDecimal = BigDecimal.valueOf(1_000_000L)

    /**
     * LKR minor units for [originalMinor] minor units of a foreign currency at [rateMicros].
     * A negative [originalMinor] (a realised crypto loss) gives a negative LKR amount of the same size.
     */
    fun lkrFromRate(originalMinor: Long, rateMicros: Long): Long {
        require(rateMicros > 0) { "FX rate must be positive" }
        return BigDecimal.valueOf(originalMinor)
            .multiply(BigDecimal.valueOf(rateMicros))
            .divide(MICROS, 0, RoundingMode.HALF_UP)
            .longValueExact()
    }

    /**
     * The rate in micros implied by receiving [lkrMinor] for [originalMinor]: what the entry stores when the
     * user types the LKR that actually landed instead of a rate.
     */
    fun rateFromLkr(originalMinor: Long, lkrMinor: Long): Long {
        require(originalMinor != 0L) { "Original amount must not be zero" }
        return BigDecimal.valueOf(lkrMinor)
            .multiply(MICROS)
            .divide(BigDecimal.valueOf(originalMinor), 0, RoundingMode.HALF_UP)
            .longValueExact()
    }
}
