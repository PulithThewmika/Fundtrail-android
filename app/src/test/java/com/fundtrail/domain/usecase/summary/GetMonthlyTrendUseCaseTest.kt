package com.fundtrail.domain.usecase.summary

import com.fundtrail.domain.model.IncomeSource
import com.fundtrail.domain.model.SpendKind
import com.fundtrail.domain.model.Transaction
import com.fundtrail.domain.model.TransactionType
import com.fundtrail.fakes.FakeTransactionRepository
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class GetMonthlyTrendUseCaseTest {

    private val zone = ZoneId.of("Asia/Colombo")
    // 15 Oct 2026, noon Colombo: safely inside October whatever UTC thinks.
    private val clock = Clock.fixed(LocalDate.of(2026, 10, 15).atTime(12, 0).atZone(zone).toInstant(), zone)
    private val repo = FakeTransactionRepository().apply { setTransactions(emptyList()) }
    private val useCase = GetMonthlyTrendUseCase(GetMonthlySummaryUseCase(repo), clock)

    private fun at(month: YearMonth, day: Int, hour: Int = 12) =
        month.atDay(day).atTime(hour, 0).atZone(zone).toInstant().toEpochMilli()

    private fun income(id: String, month: YearMonth, amount: Long, source: IncomeSource = IncomeSource.SALARY) =
        Transaction(id = id, type = TransactionType.INCOME, amountLkrMinor = amount, accountId = "default-main-bank",
            incomeSource = source, occurredAt = at(month, 25))

    private fun expense(id: String, month: YearMonth, amount: Long) =
        Transaction(id = id, type = TransactionType.EXPENSE, amountLkrMinor = amount, accountId = "default-main-bank",
            categoryId = "cat_groceries", spendKind = SpendKind.DISCRETIONARY, occurredAt = at(month, 2))

    private suspend fun add(vararg txs: Transaction) = txs.forEach { repo.add(it) }

    @Test
    fun returnsTheRequestedWindowOldestFirstEndingThisMonth() = runTest {
        val trend = useCase(months = 6).first()
        assertEquals(6, trend.size)
        assertEquals(
            listOf("2026-05", "2026-06", "2026-07", "2026-08", "2026-09", "2026-10").map { YearMonth.parse(it) },
            trend.map { it.month },
        )
    }

    @Test
    fun emptyMonthsAreZeroRowsNotGaps() = runTest {
        val trend = useCase(months = 3).first()
        assertEquals(3, trend.size)
        trend.forEach {
            assertEquals(0L, it.receivedMinor)
            assertEquals(0L, it.spentMinor)
            assertEquals(0L, it.netSavedMinor)
        }
    }

    @Test
    fun followsTheSeededThreeMonthShape() = runTest {
        val aug = YearMonth.of(2026, 8)
        val sep = YearMonth.of(2026, 9)
        val oct = YearMonth.of(2026, 10)
        // Salaries like the #157 demo seeder (130k / 120k / 140k), with some spending.
        add(
            income("s8", aug, 13_000_000L), expense("e8", aug, 9_000_000L),
            income("s9", sep, 12_000_000L), expense("e9", sep, 10_000_000L),
            income("s10", oct, 14_000_000L), expense("e10", oct, 9_600_000L),
        )

        val trend = useCase(months = 3).first().associateBy { it.month }

        assertEquals(13_000_000L, trend.getValue(aug).receivedMinor)
        assertEquals(4_000_000L, trend.getValue(aug).netSavedMinor)
        assertEquals(12_000_000L, trend.getValue(sep).receivedMinor)
        assertEquals(2_000_000L, trend.getValue(sep).netSavedMinor)
        assertEquals(14_000_000L, trend.getValue(oct).receivedMinor)
        assertEquals(9_600_000L, trend.getValue(oct).spentMinor)
        assertEquals(4_400_000L, trend.getValue(oct).netSavedMinor)
    }

    @Test
    fun cutsMonthsOnColomboBoundaries() = runTest {
        // 1 Oct 00:00 Colombo (UTC+5:30) is still 30 Sep in UTC, so UTC grouping would file this under September.
        add(income("early", YearMonth.of(2026, 10), 5_000_000L).copy(occurredAt = at(YearMonth.of(2026, 10), 1, 0)))
        val trend = useCase(months = 3).first().associateBy { it.month }
        assertEquals(5_000_000L, trend.getValue(YearMonth.of(2026, 10)).receivedMinor)
        assertEquals(0L, trend.getValue(YearMonth.of(2026, 9)).receivedMinor)
    }
}
