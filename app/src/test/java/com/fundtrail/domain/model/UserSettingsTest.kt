package com.fundtrail.domain.model

import org.junit.Assert.assertTrue
import org.junit.Test

class UserSettingsTest {

    @Test
    fun `valid settings succeeds validation`() {
        UserSettings(
            incomeEstimateMinMinor = 14_000_000L,
            incomeEstimateMaxMinor = 18_000_000L,
            expectedSalaryMinor = 13_000_000L,
            salaryPayday = 25
        ).validate()
        assertTrue(true)
    }

    @Test
    fun `null estimates succeed validation`() {
        UserSettings().validate()
        assertTrue(true)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `estimate min above max fails validation`() {
        UserSettings(incomeEstimateMinMinor = 18_000_000L, incomeEstimateMaxMinor = 14_000_000L).validate()
    }

    @Test(expected = IllegalArgumentException::class)
    fun `payday below 1 fails validation`() {
        UserSettings(salaryPayday = 0).validate()
    }

    @Test(expected = IllegalArgumentException::class)
    fun `payday above 28 fails validation`() {
        UserSettings(salaryPayday = 29).validate()
    }
}
