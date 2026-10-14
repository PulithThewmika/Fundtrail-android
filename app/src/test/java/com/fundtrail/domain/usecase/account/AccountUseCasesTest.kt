package com.fundtrail.domain.usecase.account

import com.fundtrail.domain.model.AccountKind
import com.fundtrail.domain.model.DuplicateAccountNameException
import com.fundtrail.domain.model.InvalidCurrencyException
import com.fundtrail.domain.model.LastActiveAccountException
import com.fundtrail.domain.model.Result
import com.fundtrail.fakes.FakeAccountRepository
import com.fundtrail.fakes.MainDispatcherRule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AccountUseCasesTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `seeding does nothing if repository is not empty`() = runTest {
        val repo = FakeAccountRepository()
        val addUseCase = AddAccountUseCase(repo)
        val seedUseCase = SeedDefaultAccountsUseCase(repo)

        // Add custom account
        addUseCase("Custom", AccountKind.BANK, "LKR")
        val beforeCount = repo.observeAll().first().size
        assertEquals(1, beforeCount)

        // Run seed
        val result = seedUseCase()
        assertTrue(result is Result.Success)

        val afterAccounts = repo.observeAll().first()
        assertEquals(1, afterAccounts.size)
        assertEquals("Custom", afterAccounts[0].name)
    }

    @Test
    fun `seeding populates 5 default accounts if repository is empty`() = runTest {
        val repo = FakeAccountRepository()
        val seedUseCase = SeedDefaultAccountsUseCase(repo)

        assertEquals(0, repo.observeAll().first().size)

        val result = seedUseCase()
        assertTrue(result is Result.Success)

        val accounts = repo.observeAll().first()
        assertEquals(5, accounts.size)
        assertEquals("Main bank", accounts[0].name)
    }

    @Test
    fun `AddAccountUseCase rejects blank names`() = runTest {
        val repo = FakeAccountRepository()
        val addUseCase = AddAccountUseCase(repo)

        val result = addUseCase("   ", AccountKind.BANK, "LKR")
        assertTrue(result is Result.Error)
    }

    @Test
    fun `AddAccountUseCase rejects unsupported currency`() = runTest {
        val repo = FakeAccountRepository()
        val addUseCase = AddAccountUseCase(repo)

        val result = addUseCase("Wallet", AccountKind.CASH, "EUR")
        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).exception is InvalidCurrencyException)
    }

    @Test
    fun `AddAccountUseCase rejects duplicate name case-insensitively`() = runTest {
        val repo = FakeAccountRepository()
        val addUseCase = AddAccountUseCase(repo)

        addUseCase("Savings", AccountKind.SAVINGS, "LKR")
        val result = addUseCase("SAVINGS", AccountKind.SAVINGS, "LKR")

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).exception is DuplicateAccountNameException)
    }

    @Test
    fun `RenameAccountUseCase allows renaming to self name`() = runTest {
        val repo = FakeAccountRepository()
        val addUseCase = AddAccountUseCase(repo)
        val renameUseCase = RenameAccountUseCase(repo)

        addUseCase("Main", AccountKind.BANK, "LKR")
        val account = repo.observeAll().first().first()

        val result = renameUseCase(account.id, "Main")
        assertTrue(result is Result.Success)
    }

    @Test
    fun `RenameAccountUseCase rejects duplicate name on another account`() = runTest {
        val repo = FakeAccountRepository()
        val addUseCase = AddAccountUseCase(repo)
        val renameUseCase = RenameAccountUseCase(repo)

        addUseCase("Account A", AccountKind.BANK, "LKR")
        addUseCase("Account B", AccountKind.BANK, "LKR")
        val accounts = repo.observeAll().first()

        val result = renameUseCase(accounts[1].id, "Account A")
        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).exception is DuplicateAccountNameException)
    }

    @Test
    fun `ArchiveAccountUseCase rejects archiving the last active account`() = runTest {
        val repo = FakeAccountRepository()
        val addUseCase = AddAccountUseCase(repo)
        val archiveUseCase = ArchiveAccountUseCase(repo)

        addUseCase("Only One", AccountKind.BANK, "LKR")
        val account = repo.observeAll().first().first()

        val result = archiveUseCase(account.id)
        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).exception is LastActiveAccountException)
    }
}
