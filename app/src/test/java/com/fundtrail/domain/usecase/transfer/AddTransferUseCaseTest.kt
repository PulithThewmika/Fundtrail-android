package com.fundtrail.domain.usecase.transfer

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
import java.time.YearMonth

class AddTransferUseCaseTest {

    private val transactions = FakeTransactionRepository()
    private val addTransfer = AddTransferUseCase(transactions)
    private val now = System.currentTimeMillis()

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
        val before = transactions.observeMonth(YearMonth.now()).first().size

        val result = addTransfer(10_000_00, "acc_cash", "acc_cash", now)

        assertTrue((result as Result.Error).exception is SameAccountTransferException)
        assertEquals(before, transactions.observeMonth(YearMonth.now()).first().size)
    }
}
