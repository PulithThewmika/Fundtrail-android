package com.fundtrail.ui.quickadd

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AmountInputTest {

    @Test
    fun convertsToMinorUnits() {
        assertEquals(125_000L, AmountInput.toMinor("1250"))
        assertEquals(125_050L, AmountInput.toMinor("1250.5"))
        assertEquals(125_005L, AmountInput.toMinor("1250.05"))
        assertEquals(50L, AmountInput.toMinor(".5"))
        assertEquals(100L, AmountInput.toMinor("1."))
    }

    @Test
    fun emptyOrNonNumericIsNull() {
        assertNull(AmountInput.toMinor(""))
        assertNull(AmountInput.toMinor("."))
        assertNull(AmountInput.toMinor("abc"))
    }

    @Test
    fun zeroParsesAsZeroSoTheViewModelCanReject() {
        assertEquals(0L, AmountInput.toMinor("0"))
        assertEquals(0L, AmountInput.toMinor("0.00"))
    }

    @Test
    fun sanitizeKeepsDigitsAndOneDotWithTwoDecimals() {
        assertEquals("1250.55", AmountInput.sanitize("1,250.559"))
        assertEquals("12.3", AmountInput.sanitize("1a2.3"))
        assertEquals("12.34", AmountInput.sanitize("12.3.4"))
        assertEquals("", AmountInput.sanitize("-"))
    }

    @Test
    fun sanitizeCapsTheWholePartSoTheLongCannotOverflow() {
        assertEquals("9999999999", AmountInput.sanitize("99999999999999"))
        assertEquals(999_999_999_900L, AmountInput.toMinor("99999999999999"))
    }
}
