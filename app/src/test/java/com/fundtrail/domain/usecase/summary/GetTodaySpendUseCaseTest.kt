package com.fundtrail.domain.usecase.summary

import com.fundtrail.domain.model.IncomeSource
import com.fundtrail.domain.model.SpendKind
import com.fundtrail.domain.model.Transaction
import com.fundtrail.domain.model.TransactionStatus
import com.fundtrail.domain.model.TransactionType
import com.fundtrail.fakes.FakeTransactionRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

private val ZONE = ZoneId.of("Asia/Colombo")

/** Today's spend for the home-screen widget (#226). */
@OptIn(ExperimentalCoroutinesApi::class)
class GetTodaySpendUseCaseTest {

    private val today = LocalDate.of(2026, 10, 20)
    private val clock = Clock.fixed(today.atTime(14, 0).atZone(ZONE).toInstant(), ZONE)
    private val repo = FakeTransactionRepository().apply { setTransactions(emptyList()) }
    private val useCase = GetTodaySpendUseCase(repo, clock)

    private fun at(date: LocalDate, hour: Int = 12, minute: Int = 0) =
        date.atTime(hour, minute).atZone(ZONE).toInstant().toEpochMilli()

    private fun expense(
        id: String,
        amount: Long,
        occurredAt: Long = at(today),
        status: TransactionStatus = TransactionStatus.ACTUAL,
        deletedAt: Long? = null,
    ) = Transaction(
        id = id, type = TransactionType.EXPENSE, status = status, amountLkrMinor = amount,
        accountId = "default-main-bank", categoryId = "cat_food", spendKind = SpendKind.DISCRETIONARY,
        occurredAt = occurredAt, deletedAt = deletedAt,
    )

    @Test
    fun `no entries today is zero`() = runTest {
        assertEquals(0L, useCase().first())
    }

    @Test
    fun `adds up today's actual expenses`() = runTest {
        repo.setTransactions(listOf(expense("a", 120_000L), expense("b", 80_000L)))
        assertEquals(200_000L, useCase().first())
    }

    @Test
    fun `yesterday and tomorrow do not count`() = runTest {
        repo.setTransactions(
            listOf(
                expense("today", 100_000L),
                expense("yesterday", 500_000L, occurredAt = at(today.minusDays(1))),
                expense("tomorrow", 700_000L, occurredAt = at(today.plusDays(1))),
            ),
        )
        assertEquals(100_000L, useCase().first())
    }

    @Test
    fun `the day boundary is Colombo midnight, not UTC`() = runTest {
        // 00:30 Colombo today is 19:00 UTC yesterday; 23:30 Colombo today is 18:00 UTC today.
        repo.setTransactions(
            listOf(
                expense("early", 100_000L, occurredAt = at(today, 0, 30)),
                expense("late", 200_000L, occurredAt = at(today, 23, 30)),
                expense("justBefore", 900_000L, occurredAt = at(today.minusDays(1), 23, 59)),
            ),
        )
        assertEquals(300_000L, useCase().first())
    }

    @Test
    fun `income transfers expected and deleted entries are ignored`() = runTest {
        repo.setTransactions(
            listOf(
                expense("real", 100_000L),
                expense("planned", 400_000L, status = TransactionStatus.EXPECTED),
                expense("removed", 300_000L, deletedAt = 1L),
                Transaction(
                    id = "pay", type = TransactionType.INCOME, amountLkrMinor = 9_000_000L, accountId = "default-main-bank",
                    incomeSource = IncomeSource.SALARY, occurredAt = at(today),
                ),
                Transaction(
                    id = "move", type = TransactionType.TRANSFER, amountLkrMinor = 600_000L, accountId = "default-main-bank",
                    toAccountId = "default-savings", occurredAt = at(today),
                ),
            ),
        )
        assertEquals(100_000L, useCase().first())
    }

    @Test
    fun `updates when a new entry is added`() = runTest {
        val seen = mutableListOf<Long>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { useCase().collect { seen += it } }

        repo.setTransactions(listOf(expense("a", 100_000L)))
        repo.setTransactions(listOf(expense("a", 100_000L), expense("b", 50_000L)))

        assertEquals(listOf(0L, 100_000L, 150_000L), seen)
    }

    @Test
    fun `a new day starts from zero`() = runTest {
        repo.setTransactions(listOf(expense("a", 100_000L)))
        val tomorrow = Clock.fixed(Instant.parse("2026-10-20T19:00:00Z"), ZONE) // 00:30 on 21 Oct in Colombo
        assertEquals(0L, GetTodaySpendUseCase(repo, tomorrow)().first())
    }
}
