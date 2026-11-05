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
        isPaused: Boolean = false,
        cadence: RecurringCadence = RecurringCadence.MONTHLY,
    ) = RecurringTemplate(
        id = "rec_01",
        name = "Rent",
        categoryId = "cat_rent",
        accountId = "acc_main_bank",
        expectedAmountMinor = 3_400_000L,
        cadence = cadence,
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

    @Test
    fun `weekly cadence advances by one week`() {
        val advanced = template(cadence = RecurringCadence.WEEKLY, dayOfMonth = 1, nextDueDate = LocalDate.of(2026, 10, 19)).advanced()
        assertEquals(LocalDate.of(2026, 10, 26), advanced.nextDueDate)
    }

    @Test
    fun `yearly cadence advances by one year`() {
        val advanced = template(cadence = RecurringCadence.YEARLY, nextDueDate = LocalDate.of(2026, 1, 28)).advanced()
        assertEquals(LocalDate.of(2027, 1, 28), advanced.nextDueDate)
    }

    @Test
    fun `weekly cadence accepts day 1 to 7, not 1 to 28`() {
        template(cadence = RecurringCadence.WEEKLY, dayOfMonth = 7).validate()
        assertTrue(true)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `weekly cadence rejects a day of month above 7`() {
        template(cadence = RecurringCadence.WEEKLY, dayOfMonth = 8).validate()
    }
}
