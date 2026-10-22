package com.fundtrail.domain.usecase.income

import com.fundtrail.domain.model.IncomeSource
import com.fundtrail.domain.model.SalaryStatus
import com.fundtrail.domain.model.Transaction
import com.fundtrail.domain.model.TransactionStatus
import com.fundtrail.domain.model.TransactionType
import com.fundtrail.domain.model.UserSettings
import com.fundtrail.fakes.FakeSettingsRepository
import com.fundtrail.fakes.FakeTransactionRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.YearMonth

class GetSalaryStatusUseCaseTest {

    private val fixedClock: Clock = Clock.fixed(
        Instant.parse("2026-10-20T10:00:00Z"), // Oct 20, 2026
        ZoneId.of("UTC")
    )

    private fun salaryTransaction(
        id: String,
        amountLkrMinor: Long,
        occurredAt: Long = Instant.parse("2026-10-18T10:00:00Z").toEpochMilli(),
    ) = Transaction(
        id = id,
        type = TransactionType.INCOME,
        status = TransactionStatus.ACTUAL,
        amountLkrMinor = amountLkrMinor,
        accountId = "default-main-bank",
        incomeSource = IncomeSource.SALARY,
        occurredAt = occurredAt,
    )

    @Test
    fun compute_notDue_whenBeforePaydayAndNoSalaryReceived() {
        val today = LocalDate.of(2026, 10, 20)
        val status = GetSalaryStatusUseCase.compute(
            month = YearMonth.of(2026, 10),
            expectedSalaryMinor = 13_000_000L,
            salaryPayday = 25,
            transactions = emptyList(),
            today = today,
        )
        assertEquals(SalaryStatus.NotDue, status)
    }

    @Test
    fun compute_due_whenOnOrAfterPaydayAndNoSalaryReceived() {
        val today = LocalDate.of(2026, 10, 25)
        val status = GetSalaryStatusUseCase.compute(
            month = YearMonth.of(2026, 10),
            expectedSalaryMinor = 13_000_000L,
            salaryPayday = 25,
            transactions = emptyList(),
            today = today,
        )
        assertEquals(SalaryStatus.Due, status)
    }

    @Test
    fun compute_due_whenPastMonthAndNoSalaryReceived() {
        val today = LocalDate.of(2026, 11, 10)
        val status = GetSalaryStatusUseCase.compute(
            month = YearMonth.of(2026, 10),
            expectedSalaryMinor = 13_000_000L,
            salaryPayday = 25,
            transactions = emptyList(),
            today = today,
        )
        assertEquals(SalaryStatus.Due, status)
    }

    @Test
    fun compute_notDue_whenFutureMonthAndNoSalaryReceived() {
        val today = LocalDate.of(2026, 10, 20)
        val status = GetSalaryStatusUseCase.compute(
            month = YearMonth.of(2026, 11),
            expectedSalaryMinor = 13_000_000L,
            salaryPayday = 25,
            transactions = emptyList(),
            today = today,
        )
        assertEquals(SalaryStatus.NotDue, status)
    }

    @Test
    fun compute_received_whenSalaryReceivedOnTime() {
        val today = LocalDate.of(2026, 10, 26)
        val tx = salaryTransaction("tx1", 13_000_000L)
        val status = GetSalaryStatusUseCase.compute(
            month = YearMonth.of(2026, 10),
            expectedSalaryMinor = 13_000_000L,
            salaryPayday = 25,
            transactions = listOf(tx),
            today = today,
        )
        assertEquals(SalaryStatus.Received(13_000_000L, 0L), status)
    }

    @Test
    fun compute_received_whenSalaryReceivedWithBonus() {
        val today = LocalDate.of(2026, 10, 26)
        val tx = salaryTransaction("tx1", 13_800_000L)
        val status = GetSalaryStatusUseCase.compute(
            month = YearMonth.of(2026, 10),
            expectedSalaryMinor = 13_000_000L,
            salaryPayday = 25,
            transactions = listOf(tx),
            today = today,
        )
        assertEquals(SalaryStatus.Received(13_800_000L, 800_000L), status)
    }

    @Test
    fun compute_received_aSalaryReceivedEarly() {
        // Today is 20th Oct, payday is 25th Oct. Salary received on 18th Oct.
        val today = LocalDate.of(2026, 10, 20)
        val tx = salaryTransaction("tx1", 13_500_000L)
        val status = GetSalaryStatusUseCase.compute(
            month = YearMonth.of(2026, 10),
            expectedSalaryMinor = 13_000_000L,
            salaryPayday = 25,
            transactions = listOf(tx),
            today = today,
        )
        assertEquals(SalaryStatus.Received(13_500_000L, 500_000L), status)
    }

    @Test
    fun compute_received_twoSalaryEntriesInOneMonth() {
        val today = LocalDate.of(2026, 10, 28)
        val tx1 = salaryTransaction("tx1", 10_000_000L)
        val tx2 = salaryTransaction("tx2", 3_800_000L)
        val status = GetSalaryStatusUseCase.compute(
            month = YearMonth.of(2026, 10),
            expectedSalaryMinor = 13_000_000L,
            salaryPayday = 25,
            transactions = listOf(tx1, tx2),
            today = today,
        )
        assertEquals(SalaryStatus.Received(13_800_000L, 800_000L), status)
    }

    @Test
    fun invoke_withFlows_emitsCorrectStatus() = runTest {
        val settingsRepo = FakeSettingsRepository()
        val transactionRepo = FakeTransactionRepository()
        val useCase = GetSalaryStatusUseCase(settingsRepo, transactionRepo, fixedClock)

        settingsRepo.update(UserSettings(expectedSalaryMinor = 13_000_000L, salaryPayday = 25))

        // Month Oct 2026 in KavinduSampleData has a salary entry of 130,000 LKR
        val result = useCase(YearMonth.of(2026, 10)).first()
        assertTrue(result is SalaryStatus.Received)
        val received = result as SalaryStatus.Received
        assertEquals(13_000_000L, received.amountMinor)
        assertEquals(0L, received.differenceFromExpected)
    }
}
