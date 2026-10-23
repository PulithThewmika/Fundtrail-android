package com.fundtrail.domain.usecase.transaction

import com.fundtrail.domain.model.DefaultCategories
import com.fundtrail.domain.model.OriginalAmount
import com.fundtrail.domain.model.Result
import com.fundtrail.domain.model.SameAccountTransferException
import com.fundtrail.domain.model.SpendKind
import com.fundtrail.domain.model.Transaction
import com.fundtrail.domain.model.TransactionStatus
import com.fundtrail.domain.model.TransactionType
import com.fundtrail.fakes.FakeCategoryRepository
import com.fundtrail.fakes.FakeTransactionRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateTransactionUseCaseTest {

    private val categories = FakeCategoryRepository().also { repo ->
        runBlocking { DefaultCategories.all.forEach { repo.add(it) } }
    }
    private val transactions = FakeTransactionRepository()
    private val update = UpdateTransactionUseCase(transactions, categories)

    private val now = System.currentTimeMillis()

    private fun expense() = Transaction(
        id = "txn_1", type = TransactionType.EXPENSE, amountLkrMinor = 100_00, accountId = "acc_main_bank",
        categoryId = "cat_rent", spendKind = SpendKind.COMMITTED, occurredAt = now,
    )

    /** USD 10.00 at 300 LKR/USD, i.e. LKR 3,000.00: the rate and the LKR amount agree (ADR-0003). */
    private fun foreignExpense() = expense().copy(
        amountLkrMinor = 300_000,
        original = OriginalAmount(currency = "USD", amountMinor = 10_00, fxRateMicros = 300_000_000),
    )

    private fun transfer() = Transaction(
        id = "txn_transfer", type = TransactionType.TRANSFER, status = TransactionStatus.ACTUAL,
        amountLkrMinor = 100_00, accountId = "acc_main_bank", toAccountId = "acc_cash", occurredAt = now,
    )

    @Test
    fun savesTheEditedFieldsIncludingABackDate() = runBlocking {
        transactions.add(expense())
        val backDated = now - 86_400_000L

        update(expense().copy(amountLkrMinor = 250_00, occurredAt = backDated, note = "corrected"))

        val saved = transactions.observeById("txn_1").first()!!
        assertEquals(250_00L, saved.amountLkrMinor)
        assertEquals(backDated, saved.occurredAt)
        assertEquals("corrected", saved.note)
    }

    @Test
    fun recategorisingAnExpenseTakesTheNewCategoryTypeByDefault() = runBlocking {
        transactions.add(expense())

        update(expense().copy(categoryId = "cat_shopping"))

        assertEquals(SpendKind.DISCRETIONARY, transactions.observeById("txn_1").first()!!.spendKind)
    }

    @Test
    fun anExplicitOverrideWinsOverTheCategoryDefault() = runBlocking {
        transactions.add(expense())

        update(expense().copy(categoryId = "cat_shopping"), spendKindOverride = SpendKind.COMMITTED)

        assertEquals(SpendKind.COMMITTED, transactions.observeById("txn_1").first()!!.spendKind)
    }

    @Test
    fun keepsAnAlreadyArchivedCategoryOnAnAmountOnlyEdit() = runBlocking {
        transactions.add(expense())
        categories.archive("cat_rent")

        val result = update(expense().copy(amountLkrMinor = 150_00))

        assertTrue(result is Result.Success)
        assertEquals(150_00L, transactions.observeById("txn_1").first()!!.amountLkrMinor)
    }

    @Test
    fun rejectsSwitchingToADifferentArchivedCategory() = runBlocking {
        transactions.add(expense())
        categories.archive("cat_shopping")

        assertTrue(update(expense().copy(categoryId = "cat_shopping")) is Result.Error)
    }

    @Test
    fun rejectsChangingTheAmountOfAForeignCurrencyEntry() = runBlocking {
        transactions.add(foreignExpense())

        assertTrue(update(foreignExpense().copy(amountLkrMinor = 999_00)) is Result.Error)
    }

    @Test
    fun rejectsARateThatDoesNotProduceTheLkrAmount() = runBlocking {
        // USD 10.00 at 310 LKR/USD is LKR 3,100.00, i.e. 310_000 minor units — not 310_00.
        transactions.add(foreignExpense())

        val result = update(
            foreignExpense().copy(
                amountLkrMinor = 310_00,
                original = OriginalAmount(currency = "USD", amountMinor = 10_00, fxRateMicros = 310_000_000),
            ),
        )

        assertTrue(result is Result.Error)
    }

    @Test
    fun allowsAnAmountEditThatReDerivesTheRate() = runBlocking {
        // #177: confirming an estimate changes the LKR amount and the rate together, so the pair stays honest.
        transactions.add(foreignExpense())

        val result = update(
            foreignExpense().copy(
                amountLkrMinor = 310_000,
                original = OriginalAmount(currency = "USD", amountMinor = 10_00, fxRateMicros = 310_000_000),
            ),
        )

        assertTrue(result is Result.Success)
    }

    @Test
    fun allowsANonAmountEditOnAForeignCurrencyEntry() = runBlocking {
        transactions.add(foreignExpense())

        val result = update(foreignExpense().copy(note = "clarified"))

        assertTrue(result is Result.Success)
    }

    @Test
    fun savesTheEditedAmountAndDestinationOfATransfer() = runBlocking {
        transactions.add(transfer())

        update(transfer().copy(amountLkrMinor = 200_00, toAccountId = "acc_secondary_bank"))

        val saved = transactions.observeById("txn_transfer").first()!!
        assertEquals(200_00L, saved.amountLkrMinor)
        assertEquals("acc_secondary_bank", saved.toAccountId)
    }

    @Test
    fun rejectsEditingATransferToTheSameAccountOnBothSides() = runBlocking {
        transactions.add(transfer())

        val result = update(transfer().copy(toAccountId = "acc_main_bank"))

        assertTrue((result as Result.Error).exception is SameAccountTransferException)
        assertEquals("acc_cash", transactions.observeById("txn_transfer").first()!!.toAccountId)
    }
}
