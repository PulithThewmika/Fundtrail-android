package com.fundtrail.domain.usecase.summary

import com.fundtrail.domain.model.IncomeSource
import com.fundtrail.domain.model.OriginalAmount
import com.fundtrail.domain.model.SpendKind
import com.fundtrail.domain.model.Transaction
import com.fundtrail.domain.model.TransactionStatus
import com.fundtrail.domain.model.TransactionType
import com.fundtrail.fakes.FakeTransactionRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId

class GetMonthlySummaryUseCaseTest {

    private val repo = FakeTransactionRepository()
    private val summary = GetMonthlySummaryUseCase(repo)

    private val oct = YearMonth.of(2026, 10)
    private val nov = YearMonth.of(2026, 11) // empty in the sample data

    private fun at(month: YearMonth, day: Int, hour: Int = 12, minute: Int = 0) =
        LocalDateTime.of(month.year, month.month, day, hour, minute).atZone(ZoneId.of("Asia/Colombo")).toInstant().toEpochMilli()

    private fun income(id: String, amount: Long, source: IncomeSource = IncomeSource.SALARY, occurredAt: Long = at(nov, 1),
                       status: TransactionStatus = TransactionStatus.ACTUAL, original: OriginalAmount? = null) =
        Transaction(id = id, type = TransactionType.INCOME, status = status, amountLkrMinor = amount, accountId = "acc_main_bank",
            incomeSource = source, occurredAt = occurredAt, original = original)

    private fun expense(id: String, amount: Long, occurredAt: Long = at(nov, 2), status: TransactionStatus = TransactionStatus.ACTUAL) =
        Transaction(id = id, type = TransactionType.EXPENSE, status = status, amountLkrMinor = amount, accountId = "acc_main_bank",
            categoryId = "cat_groceries", spendKind = SpendKind.DISCRETIONARY, occurredAt = occurredAt)

    private fun transfer(id: String, amount: Long) =
        Transaction(id = id, type = TransactionType.TRANSFER, amountLkrMinor = amount, accountId = "acc_main_bank",
            toAccountId = "acc_secondary_bank", occurredAt = at(nov, 3))

    private suspend fun add(vararg txs: Transaction) = txs.forEach { assertEquals(true, repo.add(it).isSuccess) }

    @Test
    fun `empty month is all zero`() = runTest {
        val s = summary(nov).first()
        assertEquals(0L, s.receivedIncomeMinor)
        assertEquals(0L, s.spentMinor)
        assertEquals(0L, s.netSavedMinor)
        assertEquals(emptyMap<IncomeSource, Long>(), s.incomeBySource)
    }

    @Test
    fun `transfers only are all zero`() = runTest {
        add(transfer("t1", 5_000_000L), transfer("t2", 1_000_000L))
        val s = summary(nov).first()
        assertEquals(0L, s.receivedIncomeMinor)
        assertEquals(0L, s.spentMinor)
        assertEquals(0L, s.netSavedMinor)
        assertEquals(emptyMap<IncomeSource, Long>(), s.incomeBySource)
    }

    @Test
    fun `crypto loss lowers received income`() = runTest {
        add(
            income("salary", 13_000_000L),
            income("crypto_loss", -372_124L, IncomeSource.CRYPTO, original = OriginalAmount("USDT", -1_240L, 300_100_000L)),
        )
        val s = summary(nov).first()
        assertEquals(12_627_876L, s.receivedIncomeMinor)
        assertEquals(mapOf(IncomeSource.SALARY to 13_000_000L, IncomeSource.CRYPTO to -372_124L), s.incomeBySource)
    }

    @Test
    fun `back-dated entry at 23-59 on the last day counts in its own month, not the next`() = runTest {
        add(expense("late_oct", 50_000L, occurredAt = at(oct, 31, 23, 59)), expense("nov_midnight", 70_000L, occurredAt = at(nov, 1, 0, 0)))
        assertEquals(3_400_000L + 1_400_000L + 1_000_000L + 150_000L + 50_000L, summary(oct).first().spentMinor)
        assertEquals(70_000L, summary(nov).first().spentMinor)
    }

    @Test
    fun `spending more than received gives a negative net saving`() = runTest {
        add(income("freelance", 2_000_000L, IncomeSource.FREELANCE), expense("big", 3_500_000L))
        val s = summary(nov).first()
        assertEquals(-1_500_000L, s.netSavedMinor)
    }

    @Test
    fun `expected entries are excluded until confirmed`() = runTest {
        add(
            income("adsense", 1_492_250L, IncomeSource.ADSENSE, status = TransactionStatus.EXPECTED),
            expense("rent_due", 3_400_000L, status = TransactionStatus.EXPECTED),
        )
        val s = summary(nov).first()
        assertEquals(0L, s.receivedIncomeMinor)
        assertEquals(0L, s.spentMinor)
    }

    @Test
    fun `kavindu sample month matches a hand calculation`() = runTest {
        val s = summary(oct).first()
        // Salary 130,000 - crypto loss 3,721.24; AdSense is EXPECTED; the transfer is ignored.
        assertEquals(13_000_000L - 372_124L, s.receivedIncomeMinor)
        // Rent 34,000 + coffee 14,000 + 10,000 + PickMe 1,500
        assertEquals(5_950_000L, s.spentMinor)
        assertEquals(13_000_000L - 372_124L - 5_950_000L, s.netSavedMinor)
    }

    @Test
    fun `summary updates live when an entry is added or deleted`() = runTest {
        add(expense("e1", 100_000L))
        assertEquals(100_000L, summary(nov).first().spentMinor)
        repo.delete("e1")
        assertEquals(0L, summary(nov).first().spentMinor)
    }
}
