package com.fundtrail.domain.usecase.summary

import com.fundtrail.domain.model.IncomeSource
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

class GetIncomeBySourceTrendUseCaseTest {

    private val zone = ZoneId.of("Asia/Colombo")
    private val clock = Clock.fixed(LocalDate.of(2026, 10, 15).atTime(12, 0).atZone(zone).toInstant(), zone)
    private val repo = FakeTransactionRepository().apply { setTransactions(emptyList()) }
    private val useCase = GetIncomeBySourceTrendUseCase(GetMonthlySummaryUseCase(repo), clock)

    private fun income(id: String, month: YearMonth, amount: Long, source: IncomeSource) =
        Transaction(id = id, type = TransactionType.INCOME, amountLkrMinor = amount, accountId = "default-main-bank",
            incomeSource = source, occurredAt = month.atDay(25).atTime(12, 0).atZone(zone).toInstant().toEpochMilli())

    @Test
    fun everyMonthHasEverySourceWithZerosWhereThereIsNoIncome() = runTest {
        repo.add(income("s", YearMonth.of(2026, 10), 14_000_000L, IncomeSource.SALARY))
        repo.add(income("p", YearMonth.of(2026, 9), 9_000_000L, IncomeSource.SALARY))

        val trend = useCase(months = 3).first()
        assertEquals(3, trend.size)
        trend.forEach { point ->
            assertEquals(IncomeSource.entries.toSet(), point.bySource.keys)
        }
        val oct = trend.first { it.month == YearMonth.of(2026, 10) }
        assertEquals(14_000_000L, oct.bySource[IncomeSource.SALARY])
        assertEquals(0L, oct.bySource[IncomeSource.ADSENSE])
        // A second populated month, with its own amount: a defect that carries income forward would show
        // October's figure here too.
        val sep = trend.first { it.month == YearMonth.of(2026, 9) }
        assertEquals(9_000_000L, sep.bySource[IncomeSource.SALARY])
        assertEquals(0L, sep.bySource[IncomeSource.ADSENSE])
    }

    @Test
    fun splitsMultipleSourcesInOneMonth() = runTest {
        val oct = YearMonth.of(2026, 10)
        repo.add(income("s", oct, 14_000_000L, IncomeSource.SALARY))
        repo.add(income("a", oct, 1_350_000L, IncomeSource.ADSENSE))
        repo.add(income("c", oct, -305_000L, IncomeSource.CRYPTO))

        val point = useCase(months = 1).first().single()
        assertEquals(14_000_000L, point.bySource[IncomeSource.SALARY])
        assertEquals(1_350_000L, point.bySource[IncomeSource.ADSENSE])
        assertEquals(-305_000L, point.bySource[IncomeSource.CRYPTO])
        assertEquals(0L, point.bySource[IncomeSource.FREELANCE])
    }
}
