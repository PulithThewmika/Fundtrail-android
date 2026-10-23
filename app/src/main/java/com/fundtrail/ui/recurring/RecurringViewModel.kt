package com.fundtrail.ui.recurring

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fundtrail.R
import com.fundtrail.domain.model.Account
import com.fundtrail.domain.model.Category
import com.fundtrail.domain.model.RecurringTemplate
import com.fundtrail.domain.model.Result
import com.fundtrail.domain.usecase.account.ObserveActiveAccountsUseCase
import com.fundtrail.domain.usecase.category.ObserveActiveCategoriesUseCase
import com.fundtrail.domain.usecase.recurring.AddRecurringUseCase
import com.fundtrail.domain.usecase.recurring.DeleteRecurringUseCase
import com.fundtrail.domain.usecase.recurring.ObserveRecurringUseCase
import com.fundtrail.domain.usecase.recurring.PauseRecurringUseCase
import com.fundtrail.domain.usecase.recurring.UpdateRecurringUseCase
import com.fundtrail.ui.common.mapErrorToMessage
import com.fundtrail.ui.quickadd.AmountInput
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** A row in the list (#204): everything the row needs, pre-shaped from the template and its names. */
data class RecurringRowUi(
    val id: String,
    val name: String,
    val expectedMinor: Long,
    val dayOfMonth: Int,
    val isPaused: Boolean,
    val isVariableAmount: Boolean,
)

/** The add/edit form (#205). [editingId] null means adding; a 1..28 [dayOfMonth] is enforced before save. */
data class RecurringFormState(
    val editingId: String? = null,
    val name: String = "",
    val categoryId: String? = null,
    val accountId: String? = null,
    val amountText: String = "",
    val isVariableAmount: Boolean = false,
    val dayOfMonthText: String = "1",
    val isSaving: Boolean = false,
    @StringRes val error: Int? = null,
) {
    val dayOfMonth: Int? get() = dayOfMonthText.toIntOrNull()
    val canSave: Boolean
        get() = !isSaving && name.isNotBlank() && categoryId != null && accountId != null &&
            (AmountInput.toMinor(amountText)?.let { it > 0 } == true) && (dayOfMonth?.let { it in 1..28 } == true)
}

data class RecurringUiState(
    val rows: List<RecurringRowUi> = emptyList(),
    /** Monthly committed total in LKR minor units; active templates only (#204). */
    val monthlyTotalMinor: Long = 0L,
    val categories: List<Category> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val form: RecurringFormState? = null,
)

@HiltViewModel
class RecurringViewModel @Inject constructor(
    observeRecurring: ObserveRecurringUseCase,
    observeCategories: ObserveActiveCategoriesUseCase,
    observeAccounts: ObserveActiveAccountsUseCase,
    private val addRecurring: AddRecurringUseCase,
    private val updateRecurring: UpdateRecurringUseCase,
    private val pauseRecurring: PauseRecurringUseCase,
    private val deleteRecurring: DeleteRecurringUseCase,
) : ViewModel() {

    private val form = MutableStateFlow<RecurringFormState?>(null)
    private val templates = MutableStateFlow<List<RecurringTemplate>>(emptyList())

    val uiState: StateFlow<RecurringUiState> =
        combine(observeRecurring(), observeCategories(), observeAccounts(), form) { all, categories, accounts, formState ->
            templates.value = all
            val active = all.filter { !it.isPaused }
            RecurringUiState(
                rows = all
                    .sortedWith(compareBy({ it.isPaused }, { it.dayOfMonth }, { it.name }))
                    .map { RecurringRowUi(it.id, it.name, it.expectedAmountMinor, it.dayOfMonth, it.isPaused, it.isVariableAmount) },
                monthlyTotalMinor = active.sumOf { it.expectedAmountMinor },
                categories = categories,
                accounts = accounts,
                form = formState,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RecurringUiState())

    fun onAddClick() = form.update {
        RecurringFormState(
            categoryId = uiState.value.categories.firstOrNull()?.id,
            accountId = uiState.value.accounts.firstOrNull()?.id,
        )
    }

    fun onEditClick(id: String) {
        val template = templates.value.firstOrNull { it.id == id } ?: return
        form.value = RecurringFormState(
            editingId = template.id,
            name = template.name,
            categoryId = template.categoryId,
            accountId = template.accountId,
            amountText = AmountInput.fromMinor(template.expectedAmountMinor),
            isVariableAmount = template.isVariableAmount,
            dayOfMonthText = template.dayOfMonth.toString(),
        )
    }

    fun onFormDismiss() { form.value = null }
    fun onNameChange(value: String) = form.update { it?.copy(name = value, error = null) }
    fun onCategoryChange(id: String) = form.update { it?.copy(categoryId = id) }
    fun onAccountChange(id: String) = form.update { it?.copy(accountId = id) }
    fun onAmountChange(value: String) = form.update { it?.copy(amountText = AmountInput.sanitize(value), error = null) }
    fun onVariableChange(value: Boolean) = form.update { it?.copy(isVariableAmount = value) }
    fun onDayOfMonthChange(value: String) = form.update { it?.copy(dayOfMonthText = value.filter { c -> c.isDigit() }.take(2), error = null) }

    fun onSave() {
        val f = form.value ?: return
        val amount = AmountInput.toMinor(f.amountText)?.takeIf { it > 0 }
        val day = f.dayOfMonth
        val categoryId = f.categoryId
        val accountId = f.accountId
        if (amount == null || day == null || day !in 1..28 || categoryId == null || accountId == null || f.name.isBlank()) {
            form.update { it?.copy(error = R.string.recurring_form_invalid) }
            return
        }
        form.update { it?.copy(isSaving = true, error = null) }
        viewModelScope.launch {
            val result = if (f.editingId == null) {
                addRecurring(f.name, categoryId, accountId, amount, f.isVariableAmount, day)
            } else {
                val original = templates.value.first { it.id == f.editingId }
                updateRecurring(
                    original.copy(
                        name = f.name.trim(), categoryId = categoryId, accountId = accountId,
                        expectedAmountMinor = amount, isVariableAmount = f.isVariableAmount, dayOfMonth = day,
                    ),
                )
            }
            when (result) {
                is Result.Success -> form.value = null
                is Result.Error -> form.update { it?.copy(isSaving = false, error = mapErrorToMessage(result.exception)) }
            }
        }
    }

    fun onPauseToggle(id: String) {
        val template = templates.value.firstOrNull { it.id == id } ?: return
        viewModelScope.launch {
            if (template.isPaused) pauseRecurring.resume(template) else pauseRecurring.pause(id)
        }
    }

    fun onDelete(id: String) {
        viewModelScope.launch { deleteRecurring(id) }
    }
}
