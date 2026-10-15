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

    // Seeding is tested against the real repository in AccountRepositoryImplTest; a test here would only exercise the fake.

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

    @Test
    fun `ArchiveAccountUseCase rejects an unknown account`() = runTest {
        val repo = FakeAccountRepository()
        repo.seedDefaultsIfEmpty()

        val result = ArchiveAccountUseCase(repo)("missing")

        assertTrue(result is Result.Error)
    }

    @Test
    fun `ArchiveAccountUseCase allows re-archiving an archived account`() = runTest {
        val repo = FakeAccountRepository()
        val addUseCase = AddAccountUseCase(repo)
        addUseCase("Active", AccountKind.BANK, "LKR")
        addUseCase("Old", AccountKind.CASH, "LKR")
        val old = repo.observeAll().first().first { it.name == "Old" }
        repo.archive(old.id)

        val result = ArchiveAccountUseCase(repo)(old.id)

        assertTrue(result is Result.Success)
        assertEquals(1, repo.observeActive().first().size)
    }
}
