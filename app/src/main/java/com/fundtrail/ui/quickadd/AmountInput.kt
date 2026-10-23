package com.fundtrail.ui.quickadd

import java.math.BigDecimal
import kotlin.math.abs

/** Typing rules for the quick-add amount field. Amounts are LKR with up to two decimals. */
object AmountInput {
    private const val MAX_WHOLE_DIGITS = 10
    private const val AMOUNT_DECIMALS = 2

    /** An FX rate is stored as micros, so six decimals is everything the field can carry (#174). */
    private const val RATE_DECIMALS = 6

    /**
     * Keeps digits and at most one dot, drops leading zeros ("005" becomes "5", "0.5" stays), and cuts to
     * two decimals and [MAX_WHOLE_DIGITS] whole digits.
     */
    fun sanitize(raw: String): String = sanitize(raw, AMOUNT_DECIMALS)

    /** The same rules for the rate field, which keeps up to six decimals instead of two. */
    fun sanitizeRate(raw: String): String = sanitize(raw, RATE_DECIMALS)

    private fun sanitize(raw: String, decimals: Int): String {
        // Commas are thousands separators in LKR, so they are dropped rather than read as a decimal point.
        val cleaned = raw.filter { it.isDigit() || it == '.' }
        val dot = cleaned.indexOf('.')
        if (dot == -1) return wholePart(cleaned)
        val whole = wholePart(cleaned.substring(0, dot))
        val fraction = cleaned.substring(dot + 1).filter { it.isDigit() }.take(decimals)
        return "$whole.$fraction"
    }

    private fun wholePart(digits: String): String =
        digits.trimStart('0').ifEmpty { if (digits.isEmpty()) "" else "0" }.take(MAX_WHOLE_DIGITS)

    /** Minor units (cents) of [text], or null when it is empty or not a number. Zero is returned as 0. */
    fun toMinor(text: String): Long? {
        val value = sanitize(text).removeSuffix(".").ifEmpty { return null }
        return value.toBigDecimalOrNull()?.movePointRight(AMOUNT_DECIMALS)?.longValueExact()
    }

    /** Micros (rate x 10^6) of [text], or null when it is empty or not a number (#174). */
    fun toMicros(text: String): Long? {
        val value = sanitizeRate(text).removeSuffix(".").ifEmpty { return null }
        return value.toBigDecimalOrNull()?.movePointRight(RATE_DECIMALS)?.longValueExact()
    }

    /**
     * Prefills the field when editing a saved transaction (#109). The field has no sign key, so a crypto
     * loss shows as its magnitude; the Gain/Loss toggle (#104) carries the sign instead.
     */
    fun fromMinor(minorUnits: Long): String = sanitize(BigDecimal.valueOf(abs(minorUnits), 2).toPlainString())
}
