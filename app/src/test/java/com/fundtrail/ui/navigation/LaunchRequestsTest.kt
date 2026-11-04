package com.fundtrail.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

/** Deep links and the widget's "+ Add" wait for the app lock instead of acting through it or being lost (#222). */
class LaunchRequestsTest {

    private val both = LaunchRequests(invoiceId = "inv_1", quickAdd = true)

    @Test
    fun `while locked navigation sees no request at all`() {
        val seen = both.releasedWhen(locked = true)

        assertNull(seen.invoiceId)
        assertFalse(seen.quickAdd)
    }

    @Test
    fun `once unlocked the held requests are released unchanged`() {
        assertEquals(both, both.releasedWhen(locked = false))
    }

    @Test
    fun `holding does not consume the request, so the unlock still delivers it`() {
        val whileLocked = both.releasedWhen(locked = true)
        val afterUnlock = both.releasedWhen(locked = false)

        assertEquals(LaunchRequests(), whileLocked)
        assertEquals("inv_1", afterUnlock.invoiceId)
        assertEquals(true, afterUnlock.quickAdd)
    }

    @Test
    fun `an empty request stays empty either way`() {
        assertEquals(LaunchRequests(), LaunchRequests().releasedWhen(locked = true))
        assertEquals(LaunchRequests(), LaunchRequests().releasedWhen(locked = false))
    }
}
