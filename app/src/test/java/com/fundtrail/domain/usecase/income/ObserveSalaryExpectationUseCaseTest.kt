package com.fundtrail.domain.usecase.income

import com.fundtrail.domain.model.SalaryExpectation
import com.fundtrail.domain.model.UserSettings
import com.fundtrail.fakes.FakeSettingsRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ObserveSalaryExpectationUseCaseTest {
    private val settings = FakeSettingsRepository()
    private val observe = ObserveSalaryExpectationUseCase(settings)

    @Test
    fun `reads the expected amount and payday from settings`() = runTest {
        settings.update(UserSettings(expectedSalaryMinor = 13_000_000L, salaryPayday = 25))

        assertEquals(SalaryExpectation(expectedAmountMinor = 13_000_000L, payday = 25), observe().first())
    }

    @Test
    fun `no expected salary set is null, with the payday kept`() = runTest {
        settings.update(UserSettings(expectedSalaryMinor = null, salaryPayday = 10))

        assertEquals(SalaryExpectation(expectedAmountMinor = null, payday = 10), observe().first())
    }
}
