package com.fundtrail.domain.usecase.expense

import com.fundtrail.domain.model.DefaultCategories
import com.fundtrail.domain.model.Result
import com.fundtrail.domain.model.SpendKind
import com.fundtrail.domain.model.TransactionStatus
import com.fundtrail.domain.model.TransactionType
import com.fundtrail.fakes.FakeCategoryRepository
import com.fundtrail.fakes.FakeTransactionRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.YearMonth

class AddExpenseUseCaseTest {

    // The fake starts with a few sample categories; add the real defaults (ids already present are skipped by add()).
    private val categories = FakeCategoryRepository().also { repo ->
        runBlocking { DefaultCategories.all.forEach { repo.add(it) } }
    }
    private val transactions = FakeTransactionRepository()
    private val addExpense = AddExpenseUseCase(transactions, categories)

    private val now = System.currentTimeMillis()

    private suspend fun saved(id: String) = transactions.observeById(id).first()

    @Test
    fun copiesTheCategoryTypeOntoTheExpense() = runBlocking {
        val committed = addExpense(500_00, "cat_rent", "acc_main_bank", now) as Result.Success
        val discretionary = addExpense(500_00, "cat_shopping", "acc_main_bank", now) as Result.Success

        assertEquals(SpendKind.COMMITTED, saved(committed.data.id)!!.spendKind)
        assertEquals(SpendKind.DISCRETIONARY, saved(discretionary.data.id)!!.spendKind)
    }

    @Test
    fun savesAnActualExpenseWithTheGivenFields() = runBlocking {
        val result = addExpense(1_250_50, "cat_coffee_dining", "acc_cash", now, note = "  PickMe  ") as Result.Success

        val tx = saved(result.data.id)!!
        assertEquals(TransactionType.EXPENSE, tx.type)
        assertEquals(TransactionStatus.ACTUAL, tx.status)
        assertEquals(1_250_50L, tx.amountLkrMinor)
        assertEquals("cat_coffee_dining", tx.categoryId)
        assertEquals("acc_cash", tx.accountId)
        assertEquals("PickMe", tx.note)
    }

    @Test
    fun anExplicitTypeOverridesTheCategoryForThatExpenseOnly() = runBlocking {
        val overridden = addExpense(100_00, "cat_rent", "acc_main_bank", now, spendKind = SpendKind.DISCRETIONARY) as Result.Success
        val plain = addExpense(100_00, "cat_rent", "acc_main_bank", now) as Result.Success

        assertEquals(SpendKind.DISCRETIONARY, saved(overridden.data.id)!!.spendKind)
        assertEquals(SpendKind.COMMITTED, saved(plain.data.id)!!.spendKind)
    }

    @Test
    fun changingTheCategoryTypeLaterDoesNotMoveAnAlreadySavedExpense() = runBlocking {
        val before = addExpense(100_00, "cat_rent", "acc_main_bank", now) as Result.Success

        val rent = categories.observeAll().first().first { it.id == "cat_rent" }
        categories.update(rent.copy(defaultSpendKind = SpendKind.DISCRETIONARY))
        val after = addExpense(100_00, "cat_rent", "acc_main_bank", now) as Result.Success

        assertEquals(SpendKind.COMMITTED, saved(before.data.id)!!.spendKind)
        assertEquals(SpendKind.DISCRETIONARY, saved(after.data.id)!!.spendKind)
    }

    @Test
    fun aBlankNoteIsStoredAsNull() = runBlocking {
        val result = addExpense(100_00, "cat_rent", "acc_main_bank", now, note = "   ") as Result.Success

        assertNull(saved(result.data.id)!!.note)
    }

    @Test
    fun rejectsAnArchivedOrUnknownCategory() = runBlocking {
        categories.archive("cat_gym")

        assertTrue(addExpense(100_00, "cat_gym", "acc_main_bank", now) is Result.Error)
        assertTrue(addExpense(100_00, "cat_missing", "acc_main_bank", now) is Result.Error)
    }

    @Test
    fun rejectsANonPositiveAmount() = runBlocking {
        val before = transactions.observeMonth(YearMonth.now()).first().size

        assertTrue(addExpense(0, "cat_rent", "acc_main_bank", now) is Result.Error)
        assertEquals(before, transactions.observeMonth(YearMonth.now()).first().size)
    }
}
