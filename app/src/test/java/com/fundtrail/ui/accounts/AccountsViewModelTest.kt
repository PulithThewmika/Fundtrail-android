package com.fundtrail.ui.accounts

import com.fundtrail.domain.model.AccountKind
import com.fundtrail.domain.usecase.account.ObserveAccountsUseCase
import com.fundtrail.fakes.FakeAccountRepository
import com.fundtrail.fakes.MainDispatcherRule
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
    private lateinit var observeAccountsUseCase: ObserveAccountsUseCase
    private lateinit var viewModel: AccountsViewModel

    @Before
    fun setUp() = runTest {
        accountRepository = FakeAccountRepository()
        accountRepository.seedDefaultsIfEmpty()
        observeAccountsUseCase = ObserveAccountsUseCase(accountRepository)
        viewModel = AccountsViewModel(
            observeAccounts = observeAccountsUseCase,
            accountRepository = accountRepository
        )
    }

    @Test
    fun `initial state loads accounts ordered by sortOrder`() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }

        val state = viewModel.uiState.value
        assertEquals(4, state.accounts.size)
        assertEquals("Main bank", state.accounts[0].name)
        assertEquals("Secondary bank", state.accounts[1].name)
        assertEquals("Cash", state.accounts[2].name)
        assertEquals("Binance", state.accounts[3].name)
    }

    @Test
    fun `add account with blank name sets addNameError`() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }

        viewModel.onAddClick()
        viewModel.onAddNameChange("   ")
        viewModel.onAddSubmit()

        val state = viewModel.uiState.value
        assertEquals("Account name must not be blank", state.addNameError)
        assertTrue(state.showAddDialog)
    }

    @Test
    fun `add account with duplicate name sets addNameError`() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }

        viewModel.onAddClick()
        viewModel.onAddNameChange("main bank") // case insensitive duplicate
        viewModel.onAddSubmit()

        val state = viewModel.uiState.value
        assertEquals("An account with this name already exists", state.addNameError)
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
        val added = state.accounts.find { it.name == "Investment Pot" }
        assertNotNull(added)
        assertEquals(AccountKind.SAVINGS, added?.kind)
    }

    @Test
    fun `rename account with blank name sets renameNameError`() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }

        val accountToRename = viewModel.uiState.value.accounts.first()

        viewModel.onRenameClick(accountToRename)
        viewModel.onRenameNameChange("  ")
        viewModel.onRenameSubmit()

        val state = viewModel.uiState.value
        assertEquals("Account name must not be blank", state.renameNameError)
        assertNotNull(state.renamingAccount)
    }

    @Test
    fun `rename account with duplicate name sets renameNameError`() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }

        val accounts = viewModel.uiState.value.accounts
        val firstAccount = accounts[0]
        val secondAccount = accounts[1]

        viewModel.onRenameClick(firstAccount)
        viewModel.onRenameNameChange(secondAccount.name.uppercase())
        viewModel.onRenameSubmit()

        val state = viewModel.uiState.value
        assertEquals("An account with this name already exists", state.renameNameError)
        assertNotNull(state.renamingAccount)
    }

    @Test
    fun `rename account with valid name succeeds and clears renamingAccount`() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }

        val accountToRename = viewModel.uiState.value.accounts.first()

        viewModel.onRenameClick(accountToRename)
        viewModel.onRenameNameChange("Primary Checking")
        viewModel.onRenameSubmit()

        val state = viewModel.uiState.value
        assertNull(state.renamingAccount)
        assertNull(state.renameNameError)
        assertTrue(state.accounts.any { it.name == "Primary Checking" })
    }

    @Test
    fun `archive account succeeds when multiple active accounts exist`() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }

        val target = viewModel.uiState.value.accounts.first()

        viewModel.onArchiveClick(target)

        val state = viewModel.uiState.value
        assertTrue(state.accounts.find { it.id == target.id }?.archived == true)
        assertNull(state.infoMessage)
    }

    @Test
    fun `archive account fails and shows explanatory message when only one active account remains`() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }

        val initialAccounts = viewModel.uiState.value.accounts
        // Archive 3 accounts so only 1 remains active
        accountRepository.archive(initialAccounts[1].id)
        accountRepository.archive(initialAccounts[2].id)
        accountRepository.archive(initialAccounts[3].id)

        val lastActive = viewModel.uiState.value.accounts.first { !it.archived }

        viewModel.onArchiveClick(lastActive)

        val finalState = viewModel.uiState.value
        assertEquals("Cannot archive the last active account. At least one active account is required.", finalState.infoMessage)
        assertFalse(finalState.accounts.first { it.id == lastActive.id }.archived)
    }
}
