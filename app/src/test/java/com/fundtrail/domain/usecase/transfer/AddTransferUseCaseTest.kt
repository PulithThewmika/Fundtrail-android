package com.fundtrail.domain.usecase.transfer

import com.fundtrail.domain.model.APP_ZONE
import com.fundtrail.domain.model.Result
import com.fundtrail.domain.model.SameAccountTransferException
import com.fundtrail.domain.model.TransactionStatus
import com.fundtrail.domain.model.TransactionType
import com.fundtrail.fakes.FakeTransactionRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.YearMonth

class AddTransferUseCaseTest {

    private val transactions = FakeTransactionRepository()
    private val addTransfer = AddTransferUseCase(transactions)
    // One fixed instant, and the month taken from it, so the test never depends on the real clock.
    private val now = 1_800_000_000_000L
    private val month = YearMonth.from(Instant.ofEpochMilli(now).atZone(APP_ZONE))

    @Test
    fun savesAnActualTransferBetweenTheTwoAccounts() = runBlocking {
        val result = addTransfer(10_000_00, "acc_main_bank", "acc_macbook_fund", now) as Result.Success

        val saved = transactions.observeById(result.data.id).first()!!
        assertEquals(TransactionType.TRANSFER, saved.type)
        assertEquals(TransactionStatus.ACTUAL, saved.status)
        assertEquals("acc_main_bank", saved.accountId)
        assertEquals("acc_macbook_fund", saved.toAccountId)
        assertEquals(10_000_00L, saved.amountLkrMinor)
    }

    @Test
    fun theSameAccountOnBothSidesIsRejectedAndNothingIsSaved() = runBlocking {
        val before = transactions.observeMonth(month).first().size

        val result = addTransfer(10_000_00, "acc_cash", "acc_cash", now)

        assertTrue((result as Result.Error).exception is SameAccountTransferException)
        assertEquals(before, transactions.observeMonth(month).first().size)
    }
}
