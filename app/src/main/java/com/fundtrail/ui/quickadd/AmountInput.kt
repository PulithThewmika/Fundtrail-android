package com.fundtrail.ui.quickadd

/** Typing rules for the quick-add amount field. Amounts are LKR with up to two decimals. */
object AmountInput {
    private const val MAX_WHOLE_DIGITS = 10

    /**
     * Keeps digits and at most one dot, drops leading zeros ("005" becomes "5", "0.5" stays), and cuts to
     * two decimals and [MAX_WHOLE_DIGITS] whole digits.
     */
    fun sanitize(raw: String): String {
        // Commas are thousands separators in LKR, so they are dropped rather than read as a decimal point.
        val cleaned = raw.filter { it.isDigit() || it == '.' }
        val dot = cleaned.indexOf('.')
        if (dot == -1) return wholePart(cleaned)
        val whole = wholePart(cleaned.substring(0, dot))
        val fraction = cleaned.substring(dot + 1).filter { it.isDigit() }.take(2)
        return "$whole.$fraction"
    }

    private fun wholePart(digits: String): String =
        digits.trimStart('0').ifEmpty { if (digits.isEmpty()) "" else "0" }.take(MAX_WHOLE_DIGITS)

    /** Minor units (cents) of [text], or null when it is empty or not a number. Zero is returned as 0. */
    fun toMinor(text: String): Long? {
        val value = sanitize(text).removeSuffix(".").ifEmpty { return null }
        return value.toBigDecimalOrNull()?.movePointRight(2)?.longValueExact()
    }
}
