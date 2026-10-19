package com.fundtrail.domain.usecase.income

import com.fundtrail.domain.model.IncomeSource
import com.fundtrail.domain.model.Result
import com.fundtrail.domain.model.TransactionStatus
import com.fundtrail.domain.model.TransactionType
import com.fundtrail.fakes.FakeTransactionRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AddIncomeUseCaseTest {

    private val transactions = FakeTransactionRepository()
    private val addIncome = AddIncomeUseCase(transactions)
    private val now = 1_800_000_000_000L

    @Test
    fun savesAnActualIncomeWithItsSourceSoItCountsTowardTheMonth() = runBlocking {
        val result = addIncome(85_000_00, IncomeSource.SALARY, "acc_main_bank", now, note = "  October salary ") as Result.Success

        val saved = transactions.observeById(result.data.id).first()!!
        assertEquals(TransactionType.INCOME, saved.type)
        assertEquals(TransactionStatus.ACTUAL, saved.status)
        assertEquals(IncomeSource.SALARY, saved.incomeSource)
        assertEquals(85_000_00L, saved.amountLkrMinor)
        assertEquals("October salary", saved.note)
    }

    @Test
    fun aBlankNoteIsStoredAsNoNote() = runBlocking {
        val result = addIncome(5_000_00, IncomeSource.FREELANCE, "acc_main_bank", now, note = "   ") as Result.Success

        assertNull(transactions.observeById(result.data.id).first()!!.note)
    }

    @Test
    fun aZeroAmountIsRejectedByTheTransactionRules() = runBlocking {
        assertTrue(addIncome(0, IncomeSource.SALARY, "acc_main_bank", now) is Result.Error)
    }
}
