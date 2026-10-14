package com.fundtrail.ui.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fundtrail.domain.model.Account
import com.fundtrail.domain.model.AccountKind
import com.fundtrail.domain.model.Result
import com.fundtrail.domain.usecase.account.AddAccountUseCase
import com.fundtrail.domain.usecase.account.ArchiveAccountUseCase
import com.fundtrail.domain.usecase.account.ObserveAccountsUseCase
import com.fundtrail.domain.usecase.account.RenameAccountUseCase
import com.fundtrail.domain.usecase.sync.ObserveSyncStatusUseCase
import com.fundtrail.ui.common.UiState
import com.fundtrail.ui.common.uiStateOf
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AccountsUiState(
    val content: UiState<List<Account>> = UiState.Loading,
    val hasLoaded: Boolean = false,
    val accountsForValidation: List<Account> = emptyList(),
    val isLoading: Boolean = false,
    val showAddDialog: Boolean = false,
    val renamingAccount: Account? = null,
    val addName: String = "",
    val addKind: AccountKind = AccountKind.BANK,
    val addCurrency: String = "LKR",
    val renameName: String = "",
    val addNameError: String? = null,
    val renameNameError: String? = null,
    val infoMessage: String? = null,
    val error: String? = null,
)

@HiltViewModel
class AccountsViewModel @Inject constructor(
    observeAccounts: ObserveAccountsUseCase,
    observeSyncStatus: ObserveSyncStatusUseCase,
    private val addAccount: AddAccountUseCase,
    private val renameAccount: RenameAccountUseCase,
    private val archiveAccount: ArchiveAccountUseCase,
) : ViewModel() {

    private val _formState = MutableStateFlow(FormState())

    val uiState: StateFlow<AccountsUiState> = combine(
        observeAccounts(),
        observeSyncStatus(),
        _formState
    ) { accounts, sync, form ->
        val sorted = accounts.sortedBy { it.sortOrder }
        val content = uiStateOf(sorted.isNotEmpty(), sync) { sorted }
        AccountsUiState(
            content = content,
            hasLoaded = true,
            accountsForValidation = sorted,
            isLoading = form.isLoading,
            showAddDialog = form.showAddDialog,
            renamingAccount = form.renamingAccount,
            addName = form.addName,
            addKind = form.addKind,
            addCurrency = form.addCurrency,
            renameName = form.renameName,
            addNameError = form.addNameError,
            renameNameError = form.renameNameError,
            infoMessage = form.infoMessage,
            error = form.error,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AccountsUiState()
    )

    fun onAddClick() {
        _formState.update {
            it.copy(
                showAddDialog = true,
                addName = "",
                addKind = AccountKind.BANK,
                addCurrency = "LKR",
                addNameError = null
            )
        }
    }

    fun onAddNameChange(name: String) {
        _formState.update { it.copy(addName = name, addNameError = null) }
    }

    fun onAddKindChange(kind: AccountKind) {
        _formState.update { it.copy(addKind = kind) }
    }

    fun onAddCurrencyChange(currency: String) {
        _formState.update { it.copy(addCurrency = currency) }
    }

    fun onAddSubmit() {
        val currentState = uiState.value
        if (!currentState.hasLoaded) return

        val currentForm = _formState.value
        val name = currentForm.addName.trim()
        if (name.isBlank()) {
            _formState.update { it.copy(addNameError = "Account name must not be blank") }
            return
        }
        if (currentState.accountsForValidation.any { it.name.equals(name, ignoreCase = true) }) {
            _formState.update { it.copy(addNameError = "An account with this name already exists") }
            return
        }

        _formState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            when (val res = addAccount(name, currentForm.addKind, currentForm.addCurrency)) {
                is Result.Success -> {
                    _formState.update { it.copy(isLoading = false, showAddDialog = false) }
                }
                is Result.Error -> {
                    _formState.update { it.copy(isLoading = false, error = res.exception.message ?: "Failed to add account") }
                }
            }
        }
    }

    fun onAddDismiss() {
        _formState.update { it.copy(showAddDialog = false) }
    }

    fun onRenameClick(account: Account) {
        _formState.update {
            it.copy(
                renamingAccount = account,
                renameName = account.name,
                renameNameError = null
            )
        }
    }

    fun onRenameNameChange(name: String) {
        _formState.update { it.copy(renameName = name, renameNameError = null) }
    }

    fun onRenameSubmit() {
        val currentState = uiState.value
        if (!currentState.hasLoaded) return

        val currentForm = _formState.value
        val target = currentForm.renamingAccount ?: return
        val name = currentForm.renameName.trim()

        if (name.isBlank()) {
            _formState.update { it.copy(renameNameError = "Account name must not be blank") }
            return
        }
        if (currentState.accountsForValidation.any { it.id != target.id && it.name.equals(name, ignoreCase = true) }) {
            _formState.update { it.copy(renameNameError = "An account with this name already exists") }
            return
        }

        _formState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            when (val res = renameAccount(target.id, name)) {
                is Result.Success -> {
                    _formState.update { it.copy(isLoading = false, renamingAccount = null) }
                }
                is Result.Error -> {
                    _formState.update { it.copy(isLoading = false, error = res.exception.message ?: "Failed to rename account") }
                }
            }
        }
    }

    fun onRenameDismiss() {
        _formState.update { it.copy(renamingAccount = null) }
    }

    fun onArchiveClick(account: Account) {
        _formState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            when (val res = archiveAccount(account.id)) {
                is Result.Success -> {
                    _formState.update { it.copy(isLoading = false) }
                }
                is Result.Error -> {
                    val message = res.exception.message ?: "Failed to archive account"
                    _formState.update {
                        it.copy(
                            isLoading = false,
                            infoMessage = if (message.contains("last active account")) message else null,
                            error = if (!message.contains("last active account")) message else null
                        )
                    }
                }
            }
        }
    }

    fun onDismissInfoMessage() {
        _formState.update { it.copy(infoMessage = null) }
    }

    fun onDismissError() {
        _formState.update { it.copy(error = null) }
    }

    private data class FormState(
        val isLoading: Boolean = false,
        val showAddDialog: Boolean = false,
        val renamingAccount: Account? = null,
        val addName: String = "",
        val addKind: AccountKind = AccountKind.BANK,
        val addCurrency: String = "LKR",
        val renameName: String = "",
        val addNameError: String? = null,
        val renameNameError: String? = null,
        val infoMessage: String? = null,
        val error: String? = null,
    )
}
