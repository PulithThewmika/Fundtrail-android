package com.fundtrail.domain.usecase.income

import com.fundtrail.domain.model.APP_ZONE
import com.fundtrail.domain.model.IncomeSource
import com.fundtrail.domain.model.Transaction
import com.fundtrail.domain.model.TransactionStatus
import com.fundtrail.domain.model.TransactionType
import com.fundtrail.domain.model.UserSettings
import com.fundtrail.domain.model.toMillisRange
import com.fundtrail.fakes.FakeSettingsRepository
import com.fundtrail.fakes.FakeTransactionRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth

class ObserveSalaryStatusUseCaseTest {

    private val settingsRepo = FakeSettingsRepository()
    private val transactionRepo = FakeTransactionRepository()
    private val oct2026 = YearMonth.of(2026, 10)
    private val fixedClock = Clock.fixed(oct2026.atDay(15).atStartOfDay(APP_ZONE).toInstant(), APP_ZONE)

    private val useCase = ObserveSalaryStatusUseCase(
        settingsRepository = settingsRepo,
        transactionRepository = transactionRepo,
        clock = fixedClock
    )

    @Test
    fun `when expected salary is set and salary is received, variance is calculated correctly`() = runTest {
        // Kavindu sample data has expectedSalary = 130,000 LKR and a received salary of 130,000 LKR in Oct 2026
        settingsRepo.update(
            UserSettings(
                expectedSalaryMinor = 13_000_000L,
                salaryPayday = 25
            )
        )

        val status = useCase(oct2026).first()
        assertTrue(status.isReceived)
        assertEquals(13_000_000L, status.expectedAmountMinor)
        assertEquals(25, status.payday)
        assertEquals(13_000_000L, status.receivedAmountMinor)
        assertEquals(0L, status.varianceMinor)
    }

    @Test
    fun `salary with bonus shows positive variance`() = runTest {
        val nov2026 = YearMonth.of(2026, 11)
        val (start, _) = nov2026.toMillisRange()

        settingsRepo.update(
            UserSettings(
                expectedSalaryMinor = 13_000_000L, // 130,000 LKR
                salaryPayday = 25
            )
        )

        transactionRepo.add(
            Transaction(
                id = "nov_salary",
                type = TransactionType.INCOME,
                status = TransactionStatus.ACTUAL,
                amountLkrMinor = 13_450_000L, // 134,500 LKR (+4,500 LKR bonus)
                accountId = "acc_main_bank",
                incomeSource = IncomeSource.SALARY,
                occurredAt = start + 86400000L
            )
        )

        val status = useCase(nov2026).first()
        assertTrue(status.isReceived)
        assertEquals(13_450_000L, status.receivedAmountMinor)
        assertEquals(450_000L, status.varianceMinor)
    }

    @Test
    fun `when salary has not been received, isReceived is false`() = runTest {
        val dec2026 = YearMonth.of(2026, 12)
        settingsRepo.update(
            UserSettings(
                expectedSalaryMinor = 13_000_000L,
                salaryPayday = 25
            )
        )

        val status = useCase(dec2026).first()
        assertFalse(status.isReceived)
        assertNull(status.receivedAmountMinor)
        assertNull(status.varianceMinor)
    }

    @Test
    fun `when expected salary is null, expectedAmountMinor and variance are null`() = runTest {
        settingsRepo.update(
            UserSettings(
                expectedSalaryMinor = null,
                salaryPayday = 25
            )
        )

        val status = useCase(oct2026).first()
        assertTrue(status.isReceived)
        assertNull(status.expectedAmountMinor)
        assertNull(status.varianceMinor)
    }
}
