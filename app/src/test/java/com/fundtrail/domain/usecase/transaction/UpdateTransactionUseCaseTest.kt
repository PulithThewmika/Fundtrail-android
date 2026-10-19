package com.fundtrail.domain.usecase.transaction

import com.fundtrail.domain.model.DefaultCategories
import com.fundtrail.domain.model.OriginalAmount
import com.fundtrail.domain.model.Result
import com.fundtrail.domain.model.SpendKind
import com.fundtrail.domain.model.Transaction
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
        val original = OriginalAmount(currency = "USD", amountMinor = 10_00, fxRateMicros = 300_000_000)
        transactions.add(expense().copy(original = original))

        assertTrue(update(expense().copy(original = original, amountLkrMinor = 999_00)) is Result.Error)
    }

    @Test
    fun allowsANonAmountEditOnAForeignCurrencyEntry() = runBlocking {
        val original = OriginalAmount(currency = "USD", amountMinor = 10_00, fxRateMicros = 300_000_000)
        transactions.add(expense().copy(original = original))

        val result = update(expense().copy(original = original, note = "clarified"))

        assertTrue(result is Result.Success)
    }
}
