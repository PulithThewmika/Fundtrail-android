package com.fundtrail.domain.usecase.transaction

import com.fundtrail.domain.model.Result
import com.fundtrail.domain.model.SpendKind
import com.fundtrail.domain.model.Transaction
import com.fundtrail.domain.model.TransactionType
import com.fundtrail.fakes.FakeTransactionRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Test

class RestoreTransactionUseCaseTest {

    private val transactions = FakeTransactionRepository()
    private val restore = RestoreTransactionUseCase(transactions)

    @Test
    fun bringsBackASoftDeletedTransaction() = runBlocking {
        val txn = Transaction(
            id = "txn_1", type = TransactionType.EXPENSE, amountLkrMinor = 100_00, accountId = "acc_main_bank",
            categoryId = "cat_rent", spendKind = SpendKind.COMMITTED, occurredAt = System.currentTimeMillis(),
        )
        transactions.add(txn)
        transactions.delete("txn_1")

        restore("txn_1") as Result.Success

        assertNotNull(transactions.observeById("txn_1").first())
    }
}
