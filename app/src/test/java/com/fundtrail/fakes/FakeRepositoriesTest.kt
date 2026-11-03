package com.fundtrail.fakes

import com.fundtrail.domain.model.Account
import com.fundtrail.domain.model.AccountKind
import com.fundtrail.domain.model.Category
import com.fundtrail.domain.model.Goal
import com.fundtrail.domain.model.IncomeSource
import com.fundtrail.domain.model.Invoice
import com.fundtrail.domain.model.InvoiceStatus
import com.fundtrail.domain.model.RecurringTemplate
import com.fundtrail.domain.model.SpendKind
import com.fundtrail.domain.model.Transaction
import com.fundtrail.domain.model.TransactionStatus
import com.fundtrail.domain.model.TransactionType
import com.fundtrail.domain.model.UserSettings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
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
        assertEquals(5, activeList.size)

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

    @Test
    fun `FakeGoalRepository observes the seeded active goal and supports save and archive`() = runBlocking {
        // Pinned: the seeded goal's deadline is 2026-12-31 and has no createdAt, so a real
        // clock would make this test fail from that date on (deterministic tests, no real time).
        val repo = FakeGoalRepository(today = { LocalDate.of(2026, 10, 20) })

        val active = repo.observeActive().first()
        assertNotNull(active)
        assertEquals("goal_macbook", active!!.id)
        assertTrue(active.isActive)

        val renamed = active.copy(name = "MacBook Pro M5")
        assertTrue(repo.save(renamed).isSuccess)
        assertEquals("MacBook Pro M5", repo.observeActive().first()!!.name)

        assertTrue(repo.archive("goal_macbook").isSuccess)
        assertFalse(repo.observeActive().first()!!.isActive)

        assertTrue(repo.archive("missing").isError)
    }

    @Test
    fun `FakeSettingsRepository observes seeded settings and validates on update`() = runBlocking {
        val repo = FakeSettingsRepository()

        val seeded = repo.observe().first()
        assertEquals(25, seeded.salaryPayday)

        assertTrue(repo.update(seeded.copy(salaryPayday = 1)).isSuccess)
        assertEquals(1, repo.observe().first().salaryPayday)

        assertTrue(repo.update(UserSettings(salaryPayday = 30)).isError)
    }

    @Test
    fun `FakeInvoiceRepository performs CRUD and markReceived as one write`() = runBlocking {
        val repo = FakeInvoiceRepository()

        val seeded = repo.observeAll().first()
        assertEquals(3, seeded.size)

        val newInvoice = Invoice(
            id = "inv_new_001",
            client = "Delta Inc",
            project = "Design system",
            amountMinor = 1_000_000L,
            currency = "LKR",
            dueDate = LocalDate.of(2026, 11, 1)
        )
        assertTrue(repo.add(newInvoice).isSuccess)
        assertTrue(repo.add(newInvoice).isError) // duplicate id

        val settlingTx = Transaction(
            id = "tx_delta_received_001",
            type = TransactionType.INCOME,
            amountLkrMinor = 1_000_000L,
            accountId = "acc_main_bank",
            incomeSource = IncomeSource.FREELANCE,
            occurredAt = 0L
        )
        assertTrue(repo.markReceived("inv_new_001", settlingTx).isSuccess)
        val updated = repo.observeAll().first().find { it.id == "inv_new_001" }
        assertEquals(InvoiceStatus.RECEIVED, updated!!.status)
        assertEquals("tx_delta_received_001", updated.receivedTransactionId)
        assertEquals(1, repo.recordedTransactions.size)
        assertEquals("inv_new_001", repo.recordedTransactions.first().invoiceId) // back-link, §3.4

        // Already RECEIVED: a second call must not double-count the income.
        assertTrue(repo.markReceived("inv_new_001", settlingTx.copy(id = "tx_delta_received_002")).isError)
        assertEquals(1, repo.recordedTransactions.size)

        // Seeded inv_gamma_001 is already RECEIVED for the same reason.
        assertTrue(repo.markReceived("inv_gamma_001", settlingTx.copy(id = "tx_gamma_received_002")).isError)

        // Settling transaction must be an ACTUAL INCOME, never EXPENSE or EXPECTED.
        val expenseTx = settlingTx.copy(
            id = "tx_wrong_type",
            type = TransactionType.EXPENSE,
            incomeSource = null,
            categoryId = "cat_rent",
            spendKind = SpendKind.COMMITTED
        )
        assertTrue(repo.markReceived("inv_beta_001", expenseTx).isError)
        val expectedTx = settlingTx.copy(id = "tx_wrong_status", status = TransactionStatus.EXPECTED)
        assertTrue(repo.markReceived("inv_beta_001", expectedTx).isError)
        assertEquals(1, repo.recordedTransactions.size)

        assertTrue(repo.delete("inv_new_001").isSuccess)
        assertTrue(repo.delete("inv_new_001").isError)
    }

    @Test
    fun `FakeRecurringRepository confirms with a transaction and advances, skip advances without one`() = runBlocking {
        val repo = FakeRecurringRepository()

        val seeded = repo.observeAll().first()
        assertEquals(3, seeded.size)
        val rent = seeded.find { it.id == "rec_rent" }!!

        assertTrue(repo.confirm(rent.id, 3_500_000L, date = 1L, expectedDueDate = rent.nextDueDate).isSuccess)
        val afterConfirm = repo.observeAll().first().find { it.id == rent.id }!!
        assertEquals(rent.nextDueDate.plusMonths(1), afterConfirm.nextDueDate)
        assertEquals(1, repo.recordedTransactions.size)
        assertEquals(3_500_000L, repo.recordedTransactions.first().amountLkrMinor)

        val subscriptions = afterConfirm.let { seeded.find { t -> t.id == "rec_subscriptions" }!! }
        assertTrue(repo.skip(subscriptions.id, subscriptions.nextDueDate).isSuccess)
        val afterSkip = repo.observeAll().first().find { it.id == subscriptions.id }!!
        assertEquals(subscriptions.nextDueDate.plusMonths(1), afterSkip.nextDueDate)
        assertEquals(1, repo.recordedTransactions.size) // skip writes no transaction

        assertTrue(repo.pause(subscriptions.id).isSuccess)
        assertTrue(repo.observeAll().first().find { it.id == subscriptions.id }!!.isPaused)
    }
}
