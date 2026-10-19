package com.fundtrail.domain.usecase.transaction

import com.fundtrail.domain.model.APP_ZONE
import com.fundtrail.domain.model.IncomeSource
import com.fundtrail.domain.model.Result
import com.fundtrail.domain.model.SpendKind
import com.fundtrail.domain.model.Transaction
import com.fundtrail.domain.model.TransactionStatus
import com.fundtrail.domain.model.TransactionType
import com.fundtrail.fakes.FakeTransactionRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class ObserveTransactionHistoryUseCaseTest {

    private val transactions = FakeTransactionRepository()
    private val history = ObserveTransactionHistoryUseCase(transactions)
    private val month = YearMonth.of(2027, 3)

    private fun millisOn(day: Int, hour: Int = 10) = LocalDate.of(2027, 3, day).atTime(hour, 0).atZone(APP_ZONE).toInstant().toEpochMilli()

    private suspend fun seed(vararg txns: Transaction) = txns.forEach { transactions.add(it) as Result.Success }

    private fun expense(id: String, day: Int, amount: Long = 100_00, hour: Int = 10) = Transaction(
        id = id, type = TransactionType.EXPENSE, amountLkrMinor = amount, accountId = "acc_1",
        categoryId = "cat_1", spendKind = SpendKind.DISCRETIONARY, occurredAt = millisOn(day, hour),
    )

    private fun income(id: String, day: Int, amount: Long = 500_00, hour: Int = 10) = Transaction(
        id = id, type = TransactionType.INCOME, amountLkrMinor = amount, accountId = "acc_1",
        incomeSource = IncomeSource.SALARY, occurredAt = millisOn(day, hour),
    )

    private fun transfer(id: String, day: Int, amount: Long = 200_00, hour: Int = 10) = Transaction(
        id = id, type = TransactionType.TRANSFER, amountLkrMinor = amount, accountId = "acc_1",
        toAccountId = "acc_2", occurredAt = millisOn(day, hour),
    )

    @Test
    fun groupsByDayNewestFirstWithinAndAcrossDays() = runBlocking {
        seed(expense("e1", 5), expense("e2", 10, hour = 9), income("i1", 10, hour = 15))

        val days = history(month).first()

        assertEquals(listOf(LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 5)), days.map { it.date })
        assertEquals(listOf("i1", "e2"), days.first().transactions.map { it.id })
    }

    @Test
    fun daySubtotalIsActualIncomeMinusActualExpenseExcludingTransfers() = runBlocking {
        seed(expense("e1", 5, 300_00), income("i1", 5, 1_000_00), transfer("t1", 5, 999_00))

        val subtotal = history(month).first().first { it.date == LocalDate.of(2027, 3, 5) }.netMinor

        assertEquals(700_00L, subtotal)
    }

    @Test
    fun cryptoLossSubtractsFromTheSubtotal() = runBlocking {
        val loss = income("i1", 5, amount = -50_00).copy(incomeSource = IncomeSource.CRYPTO)
        seed(loss)

        val subtotal = history(month).first().first { it.date == LocalDate.of(2027, 3, 5) }.netMinor

        assertEquals(-50_00L, subtotal)
    }

    @Test
    fun expectedEntriesNeverCountTowardTheSubtotal() = runBlocking {
        seed(income("i1", 5).copy(status = TransactionStatus.EXPECTED))

        val subtotal = history(month).first().first { it.date == LocalDate.of(2027, 3, 5) }.netMinor

        assertEquals(0L, subtotal)
    }

    @Test
    fun typeFilterLimitsTheRowsOnEachDay() = runBlocking {
        seed(expense("e1", 5), income("i1", 5), transfer("t1", 5))

        val days = history(month, TransactionType.INCOME).first()

        assertEquals(listOf("i1"), days.single().transactions.map { it.id })
    }
}
