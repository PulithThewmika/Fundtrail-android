package com.fundtrail.util

import java.text.NumberFormat
import java.util.Locale

/**
 * Money formatting utility for FundTrail.
 * Formats minor units (Long) into LKR currency string (e.g. LKR 1,000.50, -LKR 1,000.50, LKR 0.00).
 */
object MoneyFormatter {

    /**
     * Formats minor units (e.g. cents / subunits) to LKR currency string.
     * Examples:
     * 100050 -> "LKR 1,000.50"
     * -100050 -> "-LKR 1,000.50"
     * 0 -> "LKR 0.00"
     */
    fun formatLkr(minorUnits: Long): String {
        val majorUnits = minorUnits / 100.0
        val absMajorUnits = kotlin.math.abs(majorUnits)
        
        val numberFormat = NumberFormat.getNumberInstance(Locale.US).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }
        val formattedNumber = numberFormat.format(absMajorUnits)

        return if (minorUnits < 0) {
            "-LKR $formattedNumber"
        } else {
            "LKR $formattedNumber"
        }
    }
}
