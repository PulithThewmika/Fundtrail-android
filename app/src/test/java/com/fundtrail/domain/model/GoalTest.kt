package com.fundtrail.domain.model

import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class GoalTest {

    private fun goal(
        targetLkrMinor: Long = 49_000_000L,
        deadline: LocalDate = LocalDate.of(2027, 1, 1),
        baselineSavedMinor: Long = 0L
    ) = Goal(
        id = "goal_01",
        name = "MacBook Pro M4",
        targetLkrMinor = targetLkrMinor,
        deadline = deadline,
        baselineSavedMinor = baselineSavedMinor
    )

    @Test
    fun `valid goal succeeds validation`() {
        goal().validate(today = LocalDate.of(2026, 10, 20))
        assertTrue(true)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `zero target fails validation`() {
        goal(targetLkrMinor = 0L).validate(today = LocalDate.of(2026, 10, 20))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `negative target fails validation`() {
        goal(targetLkrMinor = -1L).validate(today = LocalDate.of(2026, 10, 20))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `negative baseline saved fails validation`() {
        goal(baselineSavedMinor = -1L).validate(today = LocalDate.of(2026, 10, 20))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `deadline on or before creation date fails validation`() {
        goal(deadline = LocalDate.of(2026, 10, 20)).validate(today = LocalDate.of(2026, 10, 20))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `blank id fails validation`() {
        goal().copy(id = "  ").validate(today = LocalDate.of(2026, 10, 20))
    }

    @Test
    fun `existing goal validates against its creation date, not today`() {
        val zone = ZoneId.of("Asia/Colombo")
        val createdAt = LocalDate.of(2026, 1, 1).atStartOfDay(zone).toInstant().toEpochMilli()
        val existing = goal(deadline = LocalDate.of(2026, 6, 1)).copy(createdAt = createdAt)
        // today is after the deadline - this must still pass, since it compares against createdAt
        existing.validate(today = LocalDate.of(2026, 10, 20), zone = zone)
        assertTrue(true)
    }
}
