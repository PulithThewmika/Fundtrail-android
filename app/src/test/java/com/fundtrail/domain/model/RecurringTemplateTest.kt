package com.fundtrail.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class RecurringTemplateTest {

    private fun template(
        dayOfMonth: Int = 28,
        nextDueDate: LocalDate = LocalDate.of(2026, 1, 28),
        isPaused: Boolean = false
    ) = RecurringTemplate(
        id = "rec_01",
        name = "Rent",
        categoryId = "cat_rent",
        accountId = "acc_main_bank",
        expectedAmountMinor = 3_400_000L,
        dayOfMonth = dayOfMonth,
        nextDueDate = nextDueDate,
        isPaused = isPaused
    )

    @Test
    fun `advances from 28 Jan to 28 Feb`() {
        val advanced = template(nextDueDate = LocalDate.of(2026, 1, 28)).advanced()
        assertEquals(LocalDate.of(2026, 2, 28), advanced.nextDueDate)
    }

    @Test
    fun `advances from 28 Dec to 28 Jan of the next year`() {
        val advanced = template(nextDueDate = LocalDate.of(2026, 12, 28)).advanced()
        assertEquals(LocalDate.of(2027, 1, 28), advanced.nextDueDate)
    }

    @Test
    fun `due when next due date has arrived and not paused`() {
        val t = template(nextDueDate = LocalDate.of(2026, 10, 20))
        assertTrue(t.isDue(today = LocalDate.of(2026, 10, 20)))
        assertTrue(t.isDue(today = LocalDate.of(2026, 10, 21)))
        assertFalse(t.isDue(today = LocalDate.of(2026, 10, 19)))
    }

    @Test
    fun `paused template is never due`() {
        val t = template(nextDueDate = LocalDate.of(2026, 10, 20), isPaused = true)
        assertFalse(t.isDue(today = LocalDate.of(2026, 10, 21)))
    }

    @Test
    fun `valid template succeeds validation`() {
        template().validate()
        assertTrue(true)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `day of month above 28 fails validation`() {
        template(dayOfMonth = 29).validate()
    }

    @Test(expected = IllegalArgumentException::class)
    fun `day of month below 1 fails validation`() {
        template(dayOfMonth = 0).validate()
    }

    @Test(expected = IllegalArgumentException::class)
    fun `zero expected amount fails validation`() {
        template().copy(expectedAmountMinor = 0L).validate()
    }
}
