package com.fundtrail.ui.accounts

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fundtrail.R
import com.fundtrail.domain.model.Account
import com.fundtrail.domain.model.AccountKind
import com.fundtrail.domain.model.DuplicateAccountNameException
import com.fundtrail.domain.model.LastActiveAccountException
import com.fundtrail.domain.model.Result
import com.fundtrail.domain.usecase.account.AddAccountUseCase
import com.fundtrail.domain.usecase.account.ArchiveAccountUseCase
import com.fundtrail.domain.usecase.account.ObserveAccountsUseCase
import com.fundtrail.domain.usecase.account.RenameAccountUseCase
import com.fundtrail.domain.usecase.sync.ObserveSyncStatusUseCase
import com.fundtrail.ui.common.UiState
import com.fundtrail.ui.common.mapErrorToMessage
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
    val isLoading: Boolean = false,
    val showAddDialog: Boolean = false,
    val renamingAccount: Account? = null,
    val addName: String = "",
    val addKind: AccountKind = AccountKind.BANK,
    val addCurrency: String = "LKR",
    val renameName: String = "",
    @StringRes val addNameError: Int? = null,
    @StringRes val renameNameError: Int? = null,
    @StringRes val infoMessage: Int? = null,
    @StringRes val error: Int? = null,
)

@HiltViewModel
class AccountsViewModel @Inject constructor(
    observeAccounts: ObserveAccountsUseCase,
    observeSyncStatus: ObserveSyncStatusUseCase,
    private val addAccount: AddAccountUseCase,
    private val renameAccount: RenameAccountUseCase,
    private val archiveAccount: ArchiveAccountUseCase,
) : ViewModel() {

    /** Form fields only; [AccountsUiState.content] is filled in from the accounts flow below. */
    private val _formState = MutableStateFlow(AccountsUiState())

    val uiState: StateFlow<AccountsUiState> = combine(
        observeAccounts(),
        observeSyncStatus(),
        _formState
    ) { accounts, sync, form ->
        form.copy(content = uiStateOf(accounts.isNotEmpty(), sync) { accounts })
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
        val currentForm = _formState.value
        val name = currentForm.addName.trim()
        if (name.isBlank()) {
            _formState.update { it.copy(addNameError = R.string.accounts_error_name_blank) }
            return
        }

        _formState.update { it.copy(isLoading = true, addNameError = null) }
        viewModelScope.launch {
            when (val res = addAccount(name, currentForm.addKind, currentForm.addCurrency)) {
                is Result.Success -> {
                    _formState.update { it.copy(isLoading = false, showAddDialog = false) }
                }
                is Result.Error -> {
                    if (res.exception is DuplicateAccountNameException) {
                        _formState.update { it.copy(isLoading = false, addNameError = R.string.accounts_name_taken) }
                    } else {
                        _formState.update { it.copy(isLoading = false, error = mapErrorToMessage(res.exception)) }
                    }
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
        val currentForm = _formState.value
        val target = currentForm.renamingAccount ?: return
        val name = currentForm.renameName.trim()

        if (name.isBlank()) {
            _formState.update { it.copy(renameNameError = R.string.accounts_error_name_blank) }
            return
        }

        _formState.update { it.copy(isLoading = true, renameNameError = null) }
        viewModelScope.launch {
            when (val res = renameAccount(target.id, name)) {
                is Result.Success -> {
                    _formState.update { it.copy(isLoading = false, renamingAccount = null) }
                }
                is Result.Error -> {
                    if (res.exception is DuplicateAccountNameException) {
                        _formState.update { it.copy(isLoading = false, renameNameError = R.string.accounts_name_taken) }
                    } else {
                        _formState.update { it.copy(isLoading = false, error = mapErrorToMessage(res.exception)) }
                    }
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
                    if (res.exception is LastActiveAccountException) {
                        _formState.update { it.copy(isLoading = false, infoMessage = R.string.accounts_last_active) }
                    } else {
                        _formState.update { it.copy(isLoading = false, error = mapErrorToMessage(res.exception)) }
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
}
