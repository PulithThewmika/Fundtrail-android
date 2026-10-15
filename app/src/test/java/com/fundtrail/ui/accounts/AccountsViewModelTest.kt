package com.fundtrail.ui.accounts

import com.fundtrail.R
import com.fundtrail.domain.model.AccountKind
import com.fundtrail.domain.usecase.account.AddAccountUseCase
import com.fundtrail.domain.usecase.account.ArchiveAccountUseCase
import com.fundtrail.domain.usecase.account.ObserveAccountsUseCase
import com.fundtrail.domain.usecase.account.RenameAccountUseCase
import com.fundtrail.domain.usecase.sync.ObserveSyncStatusUseCase
import com.fundtrail.fakes.FakeAccountRepository
import com.fundtrail.fakes.FakeSyncStatusRepository
import com.fundtrail.fakes.MainDispatcherRule
import com.fundtrail.ui.common.UiState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AccountsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var accountRepository: FakeAccountRepository
    private lateinit var viewModel: AccountsViewModel

    @Before
    fun setUp() = runTest {
        accountRepository = FakeAccountRepository()
        accountRepository.seedDefaultsIfEmpty()
        viewModel = AccountsViewModel(
            observeAccounts = ObserveAccountsUseCase(accountRepository),
            observeSyncStatus = ObserveSyncStatusUseCase(FakeSyncStatusRepository()),
            addAccount = AddAccountUseCase(accountRepository),
            renameAccount = RenameAccountUseCase(accountRepository),
            archiveAccount = ArchiveAccountUseCase(accountRepository)
        )
    }

    @Test
    fun `initial state loads accounts ordered by sortOrder`() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }

        val state = viewModel.uiState.value
        val accounts = (state.content as UiState.Success).data
        assertEquals(5, accounts.size)
        assertEquals("Main bank", accounts[0].name)
        assertEquals("Secondary bank", accounts[1].name)
        assertEquals("Cash", accounts[2].name)
        assertEquals("Binance", accounts[3].name)
        assertEquals("Other", accounts[4].name)
    }

    @Test
    fun `add account with blank name sets addNameError`() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }

        viewModel.onAddClick()
        viewModel.onAddNameChange("   ")
        viewModel.onAddSubmit()

        val state = viewModel.uiState.value
        assertEquals(R.string.accounts_error_name_blank, state.addNameError)
        assertTrue(state.showAddDialog)
    }

    @Test
    fun `add account with duplicate name sets addNameError`() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }

        viewModel.onAddClick()
        viewModel.onAddNameChange("main bank") // case insensitive duplicate
        viewModel.onAddSubmit()

        val state = viewModel.uiState.value
        assertEquals(R.string.accounts_name_taken, state.addNameError)
        assertTrue(state.showAddDialog)
    }

    @Test
    fun `add account with valid name succeeds and closes dialog`() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }

        viewModel.onAddClick()
        viewModel.onAddNameChange("Investment Pot")
        viewModel.onAddKindChange(AccountKind.SAVINGS)
        viewModel.onAddCurrencyChange("LKR")
        viewModel.onAddSubmit()

        val state = viewModel.uiState.value
        assertFalse(state.showAddDialog)
        assertNull(state.addNameError)
        val accounts = (state.content as UiState.Success).data
        val added = accounts.find { it.name == "Investment Pot" }
        assertNotNull(added)
        assertEquals(AccountKind.SAVINGS, added?.kind)
    }

    @Test
    fun `rename account with blank name sets renameNameError`() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }

        val accounts = (viewModel.uiState.value.content as UiState.Success).data
        val accountToRename = accounts.first()

        viewModel.onRenameClick(accountToRename)
        viewModel.onRenameNameChange("  ")
        viewModel.onRenameSubmit()

        val state = viewModel.uiState.value
        assertEquals(R.string.accounts_error_name_blank, state.renameNameError)
        assertNotNull(state.renamingAccount)
    }

    @Test
    fun `rename account with duplicate name sets renameNameError`() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }

        val accounts = (viewModel.uiState.value.content as UiState.Success).data
        val firstAccount = accounts[0]
        val secondAccount = accounts[1]

        viewModel.onRenameClick(firstAccount)
        viewModel.onRenameNameChange(secondAccount.name.uppercase())
        viewModel.onRenameSubmit()

        val state = viewModel.uiState.value
        assertEquals(R.string.accounts_name_taken, state.renameNameError)
        assertNotNull(state.renamingAccount)
    }

    @Test
    fun `rename account with valid name succeeds and clears renamingAccount`() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }

        val accounts = (viewModel.uiState.value.content as UiState.Success).data
        val accountToRename = accounts.first()

        viewModel.onRenameClick(accountToRename)
        viewModel.onRenameNameChange("Primary Checking")
        viewModel.onRenameSubmit()

        val state = viewModel.uiState.value
        assertNull(state.renamingAccount)
        assertNull(state.renameNameError)
        val updatedAccounts = (state.content as UiState.Success).data
        assertTrue(updatedAccounts.any { it.name == "Primary Checking" })
    }

    @Test
    fun `archive account succeeds when multiple active accounts exist`() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }

        val accounts = (viewModel.uiState.value.content as UiState.Success).data
        val target = accounts.first()

        viewModel.onArchiveClick(target)

        val updatedAccounts = (viewModel.uiState.value.content as UiState.Success).data
        assertTrue(updatedAccounts.find { it.id == target.id }?.archived == true)
    }

    @Test
    fun `archive account fails and shows explanatory message when only one active account remains`() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }

        val initialAccounts = (viewModel.uiState.value.content as UiState.Success).data
        // Archive 4 accounts so only 1 remains active
        accountRepository.archive(initialAccounts[1].id)
        accountRepository.archive(initialAccounts[2].id)
        accountRepository.archive(initialAccounts[3].id)
        accountRepository.archive(initialAccounts[4].id)

        val lastActive = (viewModel.uiState.value.content as UiState.Success).data.first { !it.archived }

        viewModel.onArchiveClick(lastActive)

        val finalState = viewModel.uiState.value
        assertEquals(R.string.accounts_last_active, finalState.infoMessage)
        val activeAccounts = (finalState.content as UiState.Success).data
        assertFalse(activeAccounts.first { it.id == lastActive.id }.archived)
    }
}
