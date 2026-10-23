package com.fundtrail.ui.income

import androidx.annotation.StringRes
import com.fundtrail.domain.model.Account
import com.fundtrail.domain.model.CryptoYtd
import com.fundtrail.domain.model.Invoice
import com.fundtrail.domain.model.LKR
import com.fundtrail.domain.model.SalaryExpectation
import com.fundtrail.domain.model.SalaryStatus
import com.fundtrail.domain.model.Transaction
import com.fundtrail.ui.common.UiState
import java.time.LocalDate

/**
 * Single invoice item UI presentation model with pre-formatted fields.
 * Overdue is derived, never stored.
 */
data class InvoiceItemUiModel(
    val invoice: Invoice,
    val daysOverdue: Long? = null,
    val formattedAmount: String,
    val formattedDate: String
)

/**
 * Group of invoices with total count and formatted aggregate total.
 */
data class InvoiceGroupData(
    val count: Int,
    val totalFormatted: String,
    val invoices: List<InvoiceItemUiModel>
)

/**
 * Loaded content for the Income screen (SCR-12).
 */
data class IncomeData(
    val salary: SalaryStatus,
    val salaryExpectation: SalaryExpectation,
    val cryptoYtd: CryptoYtd,
    val overdueGroup: InvoiceGroupData,
    val pendingGroup: InvoiceGroupData,
    val receivedGroup: InvoiceGroupData,
    val totalInvoicesCount: Int
)

/**
 * Add / Edit invoice sheet state (S2 #188).
 */
data class InvoiceFormState(
    val id: String? = null,
    val client: String = "",
    val project: String = "",
    val milestone: String = "",
    val amount: String = "",
    val currency: String = LKR,
    val dueDate: LocalDate = LocalDate.now(),
    val note: String = "",
    @StringRes val clientError: Int? = null,
    @StringRes val projectError: Int? = null,
    @StringRes val amountError: Int? = null,
    val isSaving: Boolean = false
)

/**
 * Mark received dialog state (DLG-01, S2 #188).
 */
data class MarkReceivedDialogState(
    val invoice: Invoice,
    val actualAmount: String,
    val selectedAccountId: String,
    val accounts: List<Account>,
    val receivedDate: LocalDate = LocalDate.now(),
    val fxRate: String = "1.0",
    @StringRes val amountError: Int? = null,
    @StringRes val fxRateError: Int? = null,
    val varianceFormatted: String? = null,
    val isConfirming: Boolean = false
) {
    val isForeignCurrency: Boolean get() = invoice.currency != LKR
}

/**
 * Dialog state for viewing the transaction linked to a received invoice.
 */
data class ViewTransactionDialogState(
    val transactionId: String,
    val invoice: Invoice,
    val transaction: Transaction? = null,
    val accountName: String? = null
)

/**
 * Complete state for the Income screen.
 */
data class IncomeUiState(
    val content: UiState<IncomeData> = UiState.Loading,
    val invoiceForm: InvoiceFormState? = null,
    val markReceivedDialog: MarkReceivedDialogState? = null,
    val viewTransactionDialog: ViewTransactionDialogState? = null,
    @StringRes val message: Int? = null
)
