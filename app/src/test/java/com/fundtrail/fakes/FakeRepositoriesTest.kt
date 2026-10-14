package com.fundtrail.fakes

import com.fundtrail.domain.model.Account
import com.fundtrail.domain.model.AccountKind
import com.fundtrail.domain.model.Category
import com.fundtrail.domain.model.IncomeSource
import com.fundtrail.domain.model.SpendKind
import com.fundtrail.domain.model.Transaction
import com.fundtrail.domain.model.TransactionStatus
import com.fundtrail.domain.model.TransactionType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.YearMonth

class FakeRepositoriesTest {

    @Test
    fun `FakeTransactionRepository observes month and performs CRUD`() = runBlocking {
        val repo = FakeTransactionRepository()

        // Test observing Kavindu sample month (October 2026)
        val oct2026List = repo.observeMonth(YearMonth.of(2026, 10)).first()
        assertEquals(8, oct2026List.size)

        // Salary entry
        val salary = oct2026List.find { it.id == "tx_salary_001" }
        assertNotNull(salary)
        assertEquals(13_000_000L, salary!!.amountLkrMinor)
        assertEquals(IncomeSource.SALARY, salary.incomeSource)

        // Rent entry
        val rent = oct2026List.find { it.id == "tx_rent_001" }
        assertNotNull(rent)
        assertEquals(3_400_000L, rent!!.amountLkrMinor)

        // AdSense entry (EXPECTED)
        val adsense = oct2026List.find { it.id == "tx_adsense_001" }
        assertNotNull(adsense)
        assertEquals(TransactionStatus.EXPECTED, adsense!!.status)
        assertEquals("USD", adsense.original?.currency)

        // Crypto loss entry
        val cryptoLoss = oct2026List.find { it.id == "tx_crypto_loss_001" }
        assertNotNull(cryptoLoss)
        assertEquals(-372_124L, cryptoLoss!!.amountLkrMinor)

        // Transfer entry
        val transfer = oct2026List.find { it.id == "tx_transfer_001" }
        assertNotNull(transfer)
        assertEquals(TransactionType.TRANSFER, transfer!!.type)
        assertEquals("acc_secondary_bank", transfer.toAccountId)

        // Coffee entries total = 14,000 + 10,000 = 24,000 LKR
        val coffeeTotal = oct2026List.filter { it.categoryId == "cat_coffee_dining" }
            .sumOf { it.amountLkrMinor }
        assertEquals(2_400_000L, coffeeTotal)

        // PickMe ride entry = 1,500 LKR
        val ride = oct2026List.find { it.id == "tx_ride_001" }
        assertNotNull(ride)
        assertEquals(150_000L, ride!!.amountLkrMinor)

        // Test observing empty month
        val nov2026List = repo.observeMonth(YearMonth.of(2026, 11)).first()
        assertTrue(nov2026List.isEmpty())

        // Test observeById
        val observedSalary = repo.observeById("tx_salary_001").first()
        assertEquals("tx_salary_001", observedSalary?.id)

        // Test add
        val newTx = Transaction(
            id = "tx_new_1",
            type = TransactionType.EXPENSE,
            status = TransactionStatus.ACTUAL,
            amountLkrMinor = 500_00L,
            accountId = "acc_main_bank",
            categoryId = "cat_groceries",
            spendKind = SpendKind.DISCRETIONARY,
            occurredAt = System.currentTimeMillis()
        )
        val addResult = repo.add(newTx)
        assertTrue(addResult.isSuccess)

        // Test duplicate ID rejection
        val duplicateAddResult = repo.add(newTx)
        assertTrue(duplicateAddResult.isError)

        // Test update
        val updatedTx = newTx.copy(amountLkrMinor = 600_00L)
        val updateResult = repo.update(updatedTx)
        assertTrue(updateResult.isSuccess)

        // Test delete
        val deleteResult = repo.delete("tx_new_1")
        assertTrue(deleteResult.isSuccess)

        // Deleted transaction should not be returned by observeById
        val deletedObserved = repo.observeById("tx_new_1").first()
        assertNull(deletedObserved)

        // Subsequent delete or update on deleted transaction should fail
        assertTrue(repo.delete("tx_new_1").isError)
        assertTrue(repo.update(updatedTx).isError)
    }

    @Test
    fun `FakeAccountRepository performs CRUD and active filtering`() = runBlocking {
        val repo = FakeAccountRepository()
        repo.seedDefaultsIfEmpty()

        val activeList = repo.observeActive().first()
        assertEquals(4, activeList.size)

        // Test add
        val newAcc = Account(
            id = "acc_test",
            name = "Test Savings",
            kind = AccountKind.SAVINGS,
            currency = "LKR"
        )
        assertTrue(repo.add(newAcc).isSuccess)

        // Duplicate ID rejection
        assertTrue(repo.add(newAcc).isError)

        // Test rename
        assertTrue(repo.rename("acc_test", "MacBook Savings").isSuccess)
        val allAfterRename = repo.observeAll().first()
        assertEquals("MacBook Savings", allAfterRename.find { it.id == "acc_test" }?.name)

        // Test archive
        assertTrue(repo.archive("acc_test").isSuccess)
        val activeAfterArchive = repo.observeActive().first()
        assertNull(activeAfterArchive.find { it.id == "acc_test" })
    }

    @Test
    fun `FakeCategoryRepository performs CRUD and active filtering`() = runBlocking {
        val repo = FakeCategoryRepository()

        val activeList = repo.observeActive().first()
        assertEquals(6, activeList.size)

        // Test add
        val newCat = Category(
            id = "cat_utilities",
            name = "Utilities",
            defaultSpendKind = SpendKind.COMMITTED,
            icon = "bolt"
        )
        assertTrue(repo.add(newCat).isSuccess)

        // Duplicate ID rejection
        assertTrue(repo.add(newCat).isError)

        // Test archive
        assertTrue(repo.archive("cat_utilities").isSuccess)
        val activeAfterArchive = repo.observeActive().first()
        assertNull(activeAfterArchive.find { it.id == "cat_utilities" })

        // Test suggestedFor
        val suggested = repo.suggestedFor("acc_main_bank").first()
        assertEquals(6, suggested.size)
    }
}
