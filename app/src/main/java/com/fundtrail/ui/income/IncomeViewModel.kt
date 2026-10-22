package com.fundtrail.ui.income

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fundtrail.R
import com.fundtrail.domain.model.APP_ZONE
import com.fundtrail.domain.model.Account
import com.fundtrail.domain.model.CryptoYtd
import com.fundtrail.domain.model.IncomeSource
import com.fundtrail.domain.model.Invoice
import com.fundtrail.domain.model.InvoiceStatus
import com.fundtrail.domain.model.OriginalAmount
import com.fundtrail.domain.model.Result
import com.fundtrail.domain.model.SalaryStatus
import com.fundtrail.domain.model.Transaction
import com.fundtrail.domain.model.TransactionStatus
import com.fundtrail.domain.model.TransactionType
import com.fundtrail.domain.usecase.account.ObserveAccountsUseCase
import com.fundtrail.domain.usecase.income.ObserveCryptoYtdUseCase
import com.fundtrail.domain.usecase.income.ObserveSalaryStatusUseCase
import com.fundtrail.domain.usecase.invoice.AddInvoiceUseCase
import com.fundtrail.domain.usecase.invoice.MarkInvoiceReceivedUseCase
import com.fundtrail.domain.usecase.invoice.ObserveInvoicesUseCase
import com.fundtrail.domain.usecase.invoice.UpdateInvoiceUseCase
import com.fundtrail.domain.usecase.sync.ObserveSyncStatusUseCase
import com.fundtrail.domain.usecase.transaction.ObserveTransactionByIdUseCase
import com.fundtrail.ui.common.DateFormatter
import com.fundtrail.ui.common.MoneyFormatter
import com.fundtrail.ui.common.UiState
import com.fundtrail.ui.common.mapErrorToMessage
import com.fundtrail.ui.common.uiStateOf
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import java.util.UUID
import javax.inject.Inject

private data class FormInternalState(
    val invoiceForm: InvoiceFormState? = null,
    val markReceivedDialog: MarkReceivedDialogState? = null,
    val viewTransactionDialog: ViewTransactionDialogState? = null,
    val message: Int? = null,
    val accounts: List<Account> = emptyList()
)

@HiltViewModel
class IncomeViewModel @Inject constructor(
    observeInvoices: ObserveInvoicesUseCase,
    observeSalaryStatus: ObserveSalaryStatusUseCase,
    observeCryptoYtd: ObserveCryptoYtdUseCase,
    observeAccounts: ObserveAccountsUseCase,
    observeSyncStatus: ObserveSyncStatusUseCase,
    private val addInvoice: AddInvoiceUseCase,
    private val updateInvoice: UpdateInvoiceUseCase,
    private val markInvoiceReceived: MarkInvoiceReceivedUseCase,
    private val observeTransactionById: ObserveTransactionByIdUseCase,
    private val clock: Clock
) : ViewModel() {

    private val local = MutableStateFlow(FormInternalState())
    private val retryTrigger = MutableStateFlow(0)

    val uiState: StateFlow<IncomeUiState> = combine(
        combine(retryTrigger, observeInvoices(), observeSalaryStatus()) { retry, invoices, salary ->
            Triple(retry, invoices, salary)
        },
        combine(observeCryptoYtd(), observeAccounts(), observeSyncStatus()) { crypto, accounts, sync ->
            Triple(crypto, accounts, sync)
        },
        local
    ) { (retry, invoices, salary), (crypto, accounts, sync), form ->
        val today = LocalDate.now(clock.withZone(APP_ZONE))
        val activeAccounts = accounts.filter { !it.archived }
        if (form.accounts != activeAccounts) {
            local.update { it.copy(accounts = activeAccounts) }
        }

        val (overdueGroup, pendingGroup, receivedGroup) = buildInvoiceGroups(invoices, today)
        val hasData = invoices.isNotEmpty() ||
            salary.expectedAmountMinor != null ||
            salary.isReceived ||
            crypto.transactionCount > 0

        val content = uiStateOf(hasData, sync) {
            IncomeData(
                salary = salary,
                cryptoYtd = crypto,
                overdueGroup = overdueGroup,
                pendingGroup = pendingGroup,
                receivedGroup = receivedGroup,
                totalInvoicesCount = invoices.size
            )
        }

        IncomeUiState(
            content = content,
            invoiceForm = form.invoiceForm,
            markReceivedDialog = form.markReceivedDialog,
            viewTransactionDialog = form.viewTransactionDialog,
            message = form.message
        )
    }.catch { error ->
        emit(
            IncomeUiState(
                content = UiState.Error(mapErrorToMessage(error)),
                invoiceForm = local.value.invoiceForm,
                markReceivedDialog = local.value.markReceivedDialog,
                viewTransactionDialog = local.value.viewTransactionDialog,
                message = local.value.message
            )
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), IncomeUiState())

    fun onAddInvoiceClick() {
        val today = LocalDate.now(clock.withZone(APP_ZONE))
        local.update {
            it.copy(invoiceForm = InvoiceFormState(dueDate = today))
        }
    }

    fun onEditInvoiceClick(invoice: Invoice) {
        val amountStr = BigDecimal.valueOf(invoice.amountMinor, 2).stripTrailingZeros().toPlainString()
        local.update {
            it.copy(
                invoiceForm = InvoiceFormState(
                    id = invoice.id,
                    client = invoice.client,
                    project = invoice.project,
                    milestone = invoice.milestone.orEmpty(),
                    amount = amountStr,
                    currency = invoice.currency,
                    dueDate = invoice.dueDate,
                    note = invoice.note.orEmpty()
                )
            )
        }
    }

    fun onFormClientChange(client: String) = updateForm { it.copy(client = client, clientError = null) }
    fun onFormProjectChange(project: String) = updateForm { it.copy(project = project, projectError = null) }
    fun onFormMilestoneChange(milestone: String) = updateForm { it.copy(milestone = milestone) }
    fun onFormAmountChange(amount: String) = updateForm { it.copy(amount = amount, amountError = null) }
    fun onFormCurrencyChange(currency: String) = updateForm { it.copy(currency = currency) }
    fun onFormDueDateChange(dueDate: LocalDate) = updateForm { it.copy(dueDate = dueDate) }
    fun onFormNoteChange(note: String) = updateForm { it.copy(note = note) }
    fun onFormDismiss() = local.update { it.copy(invoiceForm = null) }

    fun onFormSave() {
        val form = local.value.invoiceForm ?: return
        if (form.isSaving) return

        val client = form.client.trim()
        val project = form.project.trim()
        val amountStr = form.amount.trim()

        var hasError = false
        if (client.isBlank()) {
            updateForm { it.copy(clientError = R.string.income_client_required) }
            hasError = true
        }
        if (project.isBlank()) {
            updateForm { it.copy(projectError = R.string.income_project_required) }
            hasError = true
        }

        val amountDecimal = amountStr.toBigDecimalOrNull()
        if (amountDecimal == null || amountDecimal.signum() <= 0) {
            updateForm { it.copy(amountError = R.string.income_amount_required) }
            hasError = true
        }

        if (hasError) return

        val amountMinor = try {
            amountDecimal!!.movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact()
        } catch (_: ArithmeticException) {
            updateForm { it.copy(amountError = R.string.income_amount_required) }
            return
        }
        updateForm { it.copy(isSaving = true) }

        viewModelScope.launch {
            val isEdit = form.id != null
            val invoice = Invoice(
                id = form.id ?: UUID.randomUUID().toString(),
                client = client,
                project = project,
                milestone = form.milestone.trim().takeIf { it.isNotEmpty() },
                amountMinor = amountMinor,
                currency = form.currency,
                dueDate = form.dueDate,
                note = form.note.trim().takeIf { it.isNotEmpty() }
            )

            val result = if (isEdit) updateInvoice(invoice) else addInvoice(invoice)
            when (result) {
                is Result.Success -> {
                    local.update {
                        it.copy(invoiceForm = null, message = R.string.income_invoice_saved)
                    }
                }
                is Result.Error -> {
                    updateForm { it.copy(isSaving = false) }
                    local.update { it.copy(message = mapErrorToMessage(result.exception)) }
                }
            }
        }
    }

    fun onMarkReceivedClick(invoice: Invoice) {
        val today = LocalDate.now(clock.withZone(APP_ZONE))
        val activeAccounts = local.value.accounts
        val prefillAmount = BigDecimal.valueOf(invoice.amountMinor, 2).stripTrailingZeros().toPlainString()

        local.update {
            it.copy(
                markReceivedDialog = MarkReceivedDialogState(
                    invoice = invoice,
                    actualAmount = prefillAmount,
                    selectedAccountId = activeAccounts.firstOrNull()?.id.orEmpty(),
                    accounts = activeAccounts,
                    receivedDate = today,
                    fxRate = "1.0"
                )
            )
        }
    }

    fun onMarkReceivedAmountChange(amount: String) =
        updateMarkReceived { it.copy(actualAmount = amount, amountError = null) }

    fun onMarkReceivedAccountChange(accountId: String) =
        updateMarkReceived { it.copy(selectedAccountId = accountId) }

    fun onMarkReceivedDateChange(date: LocalDate) =
        updateMarkReceived { it.copy(receivedDate = date) }

    fun onMarkReceivedFxRateChange(fxRate: String) =
        updateMarkReceived { it.copy(fxRate = fxRate) }

    fun onMarkReceivedDismiss() = local.update { it.copy(markReceivedDialog = null) }

    fun onMarkReceivedConfirm() {
        val dialog = local.value.markReceivedDialog ?: return
        if (dialog.isConfirming) return

        val amountStr = dialog.actualAmount.trim()
        val amountDecimal = amountStr.toBigDecimalOrNull()
        if (amountDecimal == null || amountDecimal.signum() <= 0) {
            updateMarkReceived { it.copy(amountError = R.string.income_amount_required) }
            return
        }

        if (dialog.selectedAccountId.isBlank()) {
            return
        }

        updateMarkReceived { it.copy(isConfirming = true) }
        viewModelScope.launch {
            val actualMinor = try {
                amountDecimal.movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact()
            } catch (_: ArithmeticException) {
                updateMarkReceived { it.copy(amountError = R.string.income_amount_required, isConfirming = false) }
                return@launch
            }
            val isForeign = dialog.isForeignCurrency

            val fxRateMicros = if (isForeign) {
                val fxRateDecimal = dialog.fxRate.trim().toBigDecimalOrNull()
                if (fxRateDecimal == null || fxRateDecimal.signum() <= 0) {
                    updateMarkReceived { it.copy(amountError = R.string.income_amount_required, isConfirming = false) }
                    return@launch
                }
                try {
                    fxRateDecimal.multiply(BigDecimal(1_000_000)).setScale(0, RoundingMode.HALF_UP).longValueExact().coerceAtLeast(1L)
                } catch (_: ArithmeticException) {
                    updateMarkReceived { it.copy(amountError = R.string.income_amount_required, isConfirming = false) }
                    return@launch
                }
            } else {
                1_000_000L
            }

            val amountLkrMinor = if (isForeign) {
                BigDecimal(actualMinor).multiply(BigDecimal(fxRateMicros)).divide(BigDecimal(1_000_000), 0, RoundingMode.HALF_UP).toLong()
            } else {
                actualMinor
            }

            val original = if (isForeign) {
                OriginalAmount(
                    currency = dialog.invoice.currency,
                    amountMinor = actualMinor,
                    fxRateMicros = fxRateMicros
                )
            } else {
                null
            }

            val noteParts = listOfNotNull(
                dialog.invoice.client,
                dialog.invoice.project,
                dialog.invoice.milestone.takeUnless { it.isNullOrBlank() }
            )

            val settlingTx = Transaction(
                id = UUID.randomUUID().toString(),
                type = TransactionType.INCOME,
                status = TransactionStatus.ACTUAL,
                amountLkrMinor = amountLkrMinor,
                accountId = dialog.selectedAccountId,
                incomeSource = IncomeSource.FREELANCE,
                occurredAt = dialog.receivedDate.atStartOfDay(APP_ZONE).toInstant().toEpochMilli(),
                original = original,
                invoiceId = dialog.invoice.id,
                note = noteParts.joinToString(" - ")
            )

            when (val result = markInvoiceReceived(dialog.invoice.id, settlingTx)) {
                is Result.Success -> {
                    local.update {
                        it.copy(
                            markReceivedDialog = null,
                            message = R.string.income_invoice_received_success
                        )
                    }
                }
                is Result.Error -> {
                    updateMarkReceived { it.copy(isConfirming = false) }
                    local.update { it.copy(message = mapErrorToMessage(result.exception)) }
                }
            }
        }
    }

    fun onViewTransactionClick(invoice: Invoice) {
        val txId = invoice.receivedTransactionId ?: return
        viewModelScope.launch {
            val tx = observeTransactionById(txId).first()
            val accountName = tx?.let { t -> local.value.accounts.find { it.id == t.accountId }?.name }
            local.update {
                it.copy(
                    viewTransactionDialog = ViewTransactionDialogState(
                        transactionId = txId,
                        invoice = invoice,
                        transaction = tx,
                        accountName = accountName
                    )
                )
            }
        }
    }

    fun onViewTransactionDismiss() = local.update { it.copy(viewTransactionDialog = null) }

    fun onMessageShown() = local.update { it.copy(message = null) }

    fun onRetry() = retryTrigger.update { it + 1 }

    private fun updateForm(transform: (InvoiceFormState) -> InvoiceFormState) =
        local.update { state -> state.copy(invoiceForm = state.invoiceForm?.let(transform)) }

    private fun updateMarkReceived(transform: (MarkReceivedDialogState) -> MarkReceivedDialogState) =
        local.update { state -> state.copy(markReceivedDialog = state.markReceivedDialog?.let(transform)) }

    companion object {
        fun buildInvoiceGroups(
            invoices: List<Invoice>,
            today: LocalDate
        ): Triple<InvoiceGroupData, InvoiceGroupData, InvoiceGroupData> {
            val overdue = invoices.filter { it.isOverdue(today) }.sortedBy { it.dueDate }
            val pending = invoices.filter { it.status == InvoiceStatus.PENDING && !it.isOverdue(today) }
                .sortedBy { it.dueDate }
            val received = invoices.filter { it.status == InvoiceStatus.RECEIVED }
                .sortedByDescending { it.receivedAt ?: it.dueDate.atStartOfDay(APP_ZONE).toInstant().toEpochMilli() }

            val overdueUi = overdue.map {
                InvoiceItemUiModel(
                    invoice = it,
                    daysOverdue = ChronoUnit.DAYS.between(it.dueDate, today).coerceAtLeast(1L),
                    formattedAmount = MoneyFormatter.format(it.amountMinor, it.currency),
                    formattedDate = DateFormatter.format(it.dueDate)
                )
            }

            val pendingUi = pending.map {
                InvoiceItemUiModel(
                    invoice = it,
                    daysOverdue = null,
                    formattedAmount = MoneyFormatter.format(it.amountMinor, it.currency),
                    formattedDate = DateFormatter.format(it.dueDate)
                )
            }

            val receivedUi = received.map {
                InvoiceItemUiModel(
                    invoice = it,
                    daysOverdue = null,
                    formattedAmount = MoneyFormatter.format(it.amountMinor, it.currency),
                    formattedDate = it.receivedAt?.let { ms -> DateFormatter.format(ms) } ?: DateFormatter.format(it.dueDate)
                )
            }

            return Triple(
                InvoiceGroupData(overdue.size, formatGroupTotals(overdue), overdueUi),
                InvoiceGroupData(pending.size, formatGroupTotals(pending), pendingUi),
                InvoiceGroupData(received.size, formatGroupTotals(received), receivedUi)
            )
        }

        fun formatGroupTotals(invoices: List<Invoice>): String {
            if (invoices.isEmpty()) return "LKR 0.00"
            val byCurrency = invoices.groupBy { it.currency }
            val parts = mutableListOf<String>()

            val lkrList = byCurrency["LKR"]
            if (!lkrList.isNullOrEmpty()) {
                val sum = lkrList.sumOf { it.amountMinor }
                parts.add(MoneyFormatter.formatLkr(sum))
            }

            byCurrency.forEach { (curr, list) ->
                if (curr != "LKR") {
                    val sum = list.sumOf { it.amountMinor }
                    parts.add(MoneyFormatter.format(sum, curr))
                }
            }
            return parts.joinToString(" + ")
        }
    }
}
