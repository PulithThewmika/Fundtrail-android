package com.fundtrail.ui.income

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fundtrail.R
import com.fundtrail.domain.model.APP_ZONE
import com.fundtrail.domain.model.CryptoYtd
import com.fundtrail.domain.model.Invoice
import com.fundtrail.domain.model.InvoiceStatus
import com.fundtrail.domain.model.SUPPORTED_CURRENCIES
import com.fundtrail.domain.model.SalaryStatus
import com.fundtrail.ui.common.DateFormatter
import com.fundtrail.ui.common.MoneyFormatter
import com.fundtrail.ui.common.UiState
import com.fundtrail.ui.theme.FundtrailAppTheme
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

/**
 * SCR-12 Income Screen Route (EPIC-014/T2).
 */
@Composable
fun IncomeRoute(
    viewModel: IncomeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val resources = LocalResources.current

    state.message?.let { resId ->
        LaunchedEffect(resId) {
            snackbarHostState.showSnackbar(resources.getString(resId))
            viewModel.onMessageShown()
        }
    }

    IncomeScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        onRetry = viewModel::onRetry,
        onAddInvoiceClick = viewModel::onAddInvoiceClick,
        onEditInvoiceClick = viewModel::onEditInvoiceClick,
        onMarkReceivedClick = viewModel::onMarkReceivedClick,
        onViewTransactionClick = viewModel::onViewTransactionClick,
        onFormClientChange = viewModel::onFormClientChange,
        onFormProjectChange = viewModel::onFormProjectChange,
        onFormMilestoneChange = viewModel::onFormMilestoneChange,
        onFormAmountChange = viewModel::onFormAmountChange,
        onFormCurrencyChange = viewModel::onFormCurrencyChange,
        onFormDueDateChange = viewModel::onFormDueDateChange,
        onFormNoteChange = viewModel::onFormNoteChange,
        onFormSave = viewModel::onFormSave,
        onFormDismiss = viewModel::onFormDismiss,
        onMarkReceivedAmountChange = viewModel::onMarkReceivedAmountChange,
        onMarkReceivedAccountChange = viewModel::onMarkReceivedAccountChange,
        onMarkReceivedDateChange = viewModel::onMarkReceivedDateChange,
        onMarkReceivedFxRateChange = viewModel::onMarkReceivedFxRateChange,
        onMarkReceivedConfirm = viewModel::onMarkReceivedConfirm,
        onMarkReceivedDismiss = viewModel::onMarkReceivedDismiss,
        onViewTransactionDismiss = viewModel::onViewTransactionDismiss
    )
}

/**
 * Stateless Income Screen Composable.
 */
@Composable
fun IncomeScreen(
    state: IncomeUiState,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onRetry: () -> Unit,
    onAddInvoiceClick: () -> Unit,
    onEditInvoiceClick: (Invoice) -> Unit,
    onMarkReceivedClick: (Invoice) -> Unit,
    onViewTransactionClick: (Invoice) -> Unit,
    onFormClientChange: (String) -> Unit,
    onFormProjectChange: (String) -> Unit,
    onFormMilestoneChange: (String) -> Unit,
    onFormAmountChange: (String) -> Unit,
    onFormCurrencyChange: (String) -> Unit,
    onFormDueDateChange: (LocalDate) -> Unit,
    onFormNoteChange: (String) -> Unit,
    onFormSave: () -> Unit,
    onFormDismiss: () -> Unit,
    onMarkReceivedAmountChange: (String) -> Unit,
    onMarkReceivedAccountChange: (String) -> Unit,
    onMarkReceivedDateChange: (LocalDate) -> Unit,
    onMarkReceivedFxRateChange: (String) -> Unit,
    onMarkReceivedConfirm: () -> Unit,
    onMarkReceivedDismiss: () -> Unit,
    onViewTransactionDismiss: () -> Unit
) {
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.income_title),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                OutlinedButton(
                    onClick = onAddInvoiceClick
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.income_add_invoice))
                }
            }

            when (val content = state.content) {
                UiState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                UiState.Empty -> {
                    EmptyIncomeState(onAddInvoiceClick)
                }
                is UiState.Error -> {
                    ErrorIncomeState(content.message, onRetry)
                }
                is UiState.Success -> {
                    ExpectedSalaryCard(content.data.salary)
                    CryptoYtdCard(content.data.cryptoYtd)
                    InvoicesSection(
                        data = content.data,
                        onAddInvoiceClick = onAddInvoiceClick,
                        onEditInvoiceClick = onEditInvoiceClick,
                        onMarkReceivedClick = onMarkReceivedClick,
                        onViewTransactionClick = onViewTransactionClick
                    )
                }
            }
        }
    }

    state.invoiceForm?.let { form ->
        InvoiceFormDialog(
            form = form,
            onClientChange = onFormClientChange,
            onProjectChange = onFormProjectChange,
            onMilestoneChange = onFormMilestoneChange,
            onAmountChange = onFormAmountChange,
            onCurrencyChange = onFormCurrencyChange,
            onDueDateChange = onFormDueDateChange,
            onNoteChange = onFormNoteChange,
            onSave = onFormSave,
            onDismiss = onFormDismiss
        )
    }

    state.markReceivedDialog?.let { dialogState ->
        MarkReceivedDialog(
            dialogState = dialogState,
            onAmountChange = onMarkReceivedAmountChange,
            onAccountChange = onMarkReceivedAccountChange,
            onDateChange = onMarkReceivedDateChange,
            onFxRateChange = onMarkReceivedFxRateChange,
            onConfirm = onMarkReceivedConfirm,
            onDismiss = onMarkReceivedDismiss
        )
    }

    state.viewTransactionDialog?.let { viewState ->
        ViewTransactionDialog(
            dialogState = viewState,
            onDismiss = onViewTransactionDismiss
        )
    }
}

@Composable
private fun ExpectedSalaryCard(salary: SalaryStatus) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.income_expected_salary_title).uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (salary.expectedAmountMinor != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.income_salary_expected, MoneyFormatter.formatLkr(salary.expectedAmountMinor)),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = stringResource(R.string.income_salary_due_date, salary.payday),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (salary.isReceived && salary.receivedAmountMinor != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.income_salary_received, MoneyFormatter.formatLkr(salary.receivedAmountMinor)),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        salary.varianceMinor?.let { variance ->
                            val sign = if (variance > 0) "+" else ""
                            Text(
                                text = stringResource(R.string.income_salary_variance, sign + MoneyFormatter.formatLkr(variance)),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    Text(
                        text = stringResource(R.string.income_salary_pending),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                if (salary.isReceived && salary.receivedAmountMinor != null) {
                    Text(
                        text = stringResource(R.string.income_salary_received, MoneyFormatter.formatLkr(salary.receivedAmountMinor)),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                } else {
                    Text(
                        text = stringResource(R.string.income_salary_not_set),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun CryptoYtdCard(crypto: CryptoYtd) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.income_crypto_ytd_title).uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            val netFormatted = if (crypto.netLkrMinor > 0) {
                "+" + MoneyFormatter.formatLkr(crypto.netLkrMinor)
            } else {
                MoneyFormatter.formatLkr(crypto.netLkrMinor)
            }

            Text(
                text = netFormatted,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = if (crypto.transactionCount > 0) {
                    stringResource(R.string.income_crypto_ytd_caption, crypto.year)
                } else {
                    stringResource(R.string.income_crypto_ytd_no_entries)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun InvoicesSection(
    data: IncomeData,
    onAddInvoiceClick: () -> Unit,
    onEditInvoiceClick: (Invoice) -> Unit,
    onMarkReceivedClick: (Invoice) -> Unit,
    onViewTransactionClick: (Invoice) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = stringResource(R.string.income_invoices_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )

        if (data.totalInvoicesCount == 0) {
            OutlinedCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.income_invoices_empty_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = stringResource(R.string.income_invoices_empty_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Button(onClick = onAddInvoiceClick) {
                        Text(stringResource(R.string.income_add_invoice))
                    }
                }
            }
        } else {
            // Group 1: Overdue group (sits first at top!)
            if (data.overdueGroup.count > 0) {
                InvoiceGroupBlock(
                    title = stringResource(R.string.income_group_overdue, data.overdueGroup.count),
                    totalFormatted = data.overdueGroup.totalFormatted,
                    invoices = data.overdueGroup.invoices,
                    onEditInvoiceClick = onEditInvoiceClick,
                    onMarkReceivedClick = onMarkReceivedClick,
                    onViewTransactionClick = onViewTransactionClick
                )
            }

            // Group 2: Pending group
            if (data.pendingGroup.count > 0) {
                InvoiceGroupBlock(
                    title = stringResource(R.string.income_group_pending, data.pendingGroup.count),
                    totalFormatted = data.pendingGroup.totalFormatted,
                    invoices = data.pendingGroup.invoices,
                    onEditInvoiceClick = onEditInvoiceClick,
                    onMarkReceivedClick = onMarkReceivedClick,
                    onViewTransactionClick = onViewTransactionClick
                )
            }

            // Group 3: Received group
            if (data.receivedGroup.count > 0) {
                InvoiceGroupBlock(
                    title = stringResource(R.string.income_group_received, data.receivedGroup.count),
                    totalFormatted = data.receivedGroup.totalFormatted,
                    invoices = data.receivedGroup.invoices,
                    onEditInvoiceClick = onEditInvoiceClick,
                    onMarkReceivedClick = onMarkReceivedClick,
                    onViewTransactionClick = onViewTransactionClick
                )
            }
        }
    }
}

@Composable
private fun InvoiceGroupBlock(
    title: String,
    totalFormatted: String,
    invoices: List<InvoiceItemUiModel>,
    onEditInvoiceClick: (Invoice) -> Unit,
    onMarkReceivedClick: (Invoice) -> Unit,
    onViewTransactionClick: (Invoice) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = totalFormatted,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        invoices.forEach { item ->
            InvoiceCard(
                item = item,
                onEditInvoiceClick = onEditInvoiceClick,
                onMarkReceivedClick = onMarkReceivedClick,
                onViewTransactionClick = onViewTransactionClick
            )
        }
    }
}

@Composable
private fun InvoiceCard(
    item: InvoiceItemUiModel,
    onEditInvoiceClick: (Invoice) -> Unit,
    onMarkReceivedClick: (Invoice) -> Unit,
    onViewTransactionClick: (Invoice) -> Unit
) {
    val invoice = item.invoice
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = invoice.client,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    val projectText = if (!invoice.milestone.isNullOrBlank()) {
                        "${invoice.project} · ${invoice.milestone}"
                    } else {
                        invoice.project
                    }
                    Text(
                        text = projectText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = item.formattedAmount,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (item.daysOverdue != null) {
                    // Overdue badge in neutral styling (NFR-14)
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Text(
                            text = stringResource(R.string.income_days_overdue, item.daysOverdue),
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else if (invoice.status == InvoiceStatus.RECEIVED) {
                    Text(
                        text = stringResource(R.string.income_received_date, item.formattedDate),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = stringResource(R.string.income_due_date, item.formattedDate),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (invoice.status == InvoiceStatus.PENDING) {
                        IconButton(onClick = { onEditInvoiceClick(invoice) }) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = stringResource(R.string.income_edit_invoice),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        OutlinedButton(
                            onClick = { onMarkReceivedClick(invoice) },
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text(stringResource(R.string.income_mark_received), style = MaterialTheme.typography.labelMedium)
                        }
                    } else if (invoice.status == InvoiceStatus.RECEIVED && invoice.receivedTransactionId != null) {
                        TextButton(
                            onClick = { onViewTransactionClick(invoice) }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Link,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.income_linked_tx),
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InvoiceFormDialog(
    form: InvoiceFormState,
    onClientChange: (String) -> Unit,
    onProjectChange: (String) -> Unit,
    onMilestoneChange: (String) -> Unit,
    onAmountChange: (String) -> Unit,
    onCurrencyChange: (String) -> Unit,
    onDueDateChange: (LocalDate) -> Unit,
    onNoteChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    var showDatePicker by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (form.id == null) {
                        stringResource(R.string.income_add_invoice)
                    } else {
                        stringResource(R.string.income_edit_invoice)
                    },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = form.client,
                    onValueChange = onClientChange,
                    label = { Text(stringResource(R.string.income_invoice_client)) },
                    isError = form.clientError != null,
                    supportingText = form.clientError?.let { { Text(stringResource(it)) } },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = form.project,
                    onValueChange = onProjectChange,
                    label = { Text(stringResource(R.string.income_invoice_project)) },
                    isError = form.projectError != null,
                    supportingText = form.projectError?.let { { Text(stringResource(it)) } },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = form.milestone,
                    onValueChange = onMilestoneChange,
                    label = { Text(stringResource(R.string.income_invoice_milestone)) },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = form.amount,
                        onValueChange = onAmountChange,
                        label = { Text(stringResource(R.string.income_invoice_amount)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        isError = form.amountError != null,
                        supportingText = form.amountError?.let { { Text(stringResource(it)) } },
                        modifier = Modifier.weight(1f)
                    )

                    var expandedCurrency by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = expandedCurrency,
                        onExpandedChange = { expandedCurrency = it },
                        modifier = Modifier.width(110.dp)
                    ) {
                        OutlinedTextField(
                            value = form.currency,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(R.string.income_invoice_currency)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCurrency) },
                            modifier = Modifier.menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = expandedCurrency,
                            onDismissRequest = { expandedCurrency = false }
                        ) {
                            SUPPORTED_CURRENCIES.forEach { curr ->
                                DropdownMenuItem(
                                    text = { Text(curr) },
                                    onClick = {
                                        onCurrencyChange(curr)
                                        expandedCurrency = false
                                    }
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = DateFormatter.format(form.dueDate),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.income_invoice_due_date)) },
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Default.CalendarToday, contentDescription = null)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePicker = true }
                )

                OutlinedTextField(
                    value = form.note,
                    onValueChange = onNoteChange,
                    label = { Text(stringResource(R.string.income_invoice_note)) },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.category_cancel))
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = onSave,
                        enabled = !form.isSaving
                    ) {
                        Text(stringResource(R.string.category_save))
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = form.dueDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val picked = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                            onDueDateChange(picked)
                        }
                        showDatePicker = false
                    }
                ) {
                    Text(stringResource(R.string.quick_add_date_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.category_cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MarkReceivedDialog(
    dialogState: MarkReceivedDialogState,
    onAmountChange: (String) -> Unit,
    onAccountChange: (String) -> Unit,
    onDateChange: (LocalDate) -> Unit,
    onFxRateChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val invoice = dialogState.invoice
    var showDatePicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.income_confirm_received_title)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "${invoice.client} — ${invoice.project}${invoice.milestone?.let { " ($it)" } ?: ""}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = "Expected: ${MoneyFormatter.format(invoice.amountMinor, invoice.currency)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = dialogState.actualAmount,
                    onValueChange = onAmountChange,
                    label = { Text(stringResource(R.string.income_amount_received)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = dialogState.amountError != null,
                    supportingText = dialogState.amountError?.let { { Text(stringResource(it)) } },
                    modifier = Modifier.fillMaxWidth()
                )

                if (dialogState.isForeignCurrency) {
                    OutlinedTextField(
                        value = dialogState.fxRate,
                        onValueChange = onFxRateChange,
                        label = { Text(stringResource(R.string.income_fx_rate, invoice.currency)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        isError = dialogState.fxRateError != null,
                        supportingText = dialogState.fxRateError?.let { { Text(stringResource(it)) } },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Account picker
                var accountExpanded by remember { mutableStateOf(false) }
                val selectedAccount = dialogState.accounts.find { it.id == dialogState.selectedAccountId }
                ExposedDropdownMenuBox(
                    expanded = accountExpanded,
                    onExpandedChange = { accountExpanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedAccount?.name ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.income_account)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = accountExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = accountExpanded,
                        onDismissRequest = { accountExpanded = false }
                    ) {
                        dialogState.accounts.forEach { acc ->
                            DropdownMenuItem(
                                text = { Text(acc.name) },
                                onClick = {
                                    onAccountChange(acc.id)
                                    accountExpanded = false
                                }
                            )
                        }
                    }
                }

                // Date picker
                OutlinedTextField(
                    value = DateFormatter.format(dialogState.receivedDate),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.income_received_date_label)) },
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Default.CalendarToday, contentDescription = null)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePicker = true }
                )

                // Live variance calculation
                dialogState.varianceFormatted?.let { varianceFormatted ->
                    Text(
                        text = stringResource(R.string.income_variance_label, varianceFormatted),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = stringResource(R.string.income_confirm_received_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !dialogState.isConfirming && dialogState.selectedAccountId.isNotBlank()
            ) {
                Text(stringResource(R.string.quick_add_date_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.category_cancel))
            }
        }
    )

    if (showDatePicker) {
        val today = LocalDate.now(APP_ZONE)
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = dialogState.receivedDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) =
                    !Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneOffset.UTC).toLocalDate().isAfter(today)
            }
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val picked = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                            onDateChange(picked)
                        }
                        showDatePicker = false
                    }
                ) {
                    Text(stringResource(R.string.quick_add_date_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.category_cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun ViewTransactionDialog(
    dialogState: ViewTransactionDialogState,
    onDismiss: () -> Unit
) {
    val tx = dialogState.transaction
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.income_view_tx_title)) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "${dialogState.invoice.client} — ${dialogState.invoice.project}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                if (tx != null) {
                    Text(
                        text = stringResource(R.string.income_view_tx_amount, MoneyFormatter.formatLkr(tx.amountLkrMinor)),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    dialogState.accountName?.let { accName ->
                        Text(
                            text = stringResource(R.string.income_view_tx_account, accName),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Text(
                        text = stringResource(R.string.income_view_tx_date, DateFormatter.format(tx.occurredAt)),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = stringResource(R.string.income_view_tx_id, tx.id),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = stringResource(R.string.income_view_tx_id, dialogState.transactionId),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text(stringResource(R.string.income_view_tx_close))
            }
        }
    )
}

@Composable
private fun EmptyIncomeState(onAddInvoice: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(R.string.income_invoices_empty_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = stringResource(R.string.income_invoices_empty_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Button(onClick = onAddInvoice) {
            Text(stringResource(R.string.income_add_invoice))
        }
    }
}

@Composable
private fun ErrorIncomeState(message: Int, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(message),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center
        )
        Button(onClick = onRetry) {
            Text(stringResource(R.string.retry))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun IncomeScreenPreview() {
    FundtrailAppTheme {
        IncomeScreen(
            state = IncomeUiState(
                content = UiState.Success(
                    IncomeData(
                        salary = SalaryStatus(
                            expectedAmountMinor = 13_000_000L,
                            payday = 25,
                            receivedAmountMinor = 13_450_000L,
                            isReceived = true,
                            varianceMinor = 450_000L
                        ),
                        cryptoYtd = CryptoYtd(
                            year = 2026,
                            netLkrMinor = 5_000_000L,
                            transactionCount = 3
                        ),
                        overdueGroup = InvoiceGroupData(
                            count = 1,
                            totalFormatted = "LKR 30,000.00",
                            invoices = listOf(
                                InvoiceItemUiModel(
                                    invoice = Invoice(
                                        id = "1",
                                        client = "Nova Cafe",
                                        project = "Logo design",
                                        milestone = "Milestone 2",
                                        amountMinor = 3_000_000L,
                                        currency = "LKR",
                                        dueDate = LocalDate.of(2026, 10, 1),
                                        status = InvoiceStatus.PENDING
                                    ),
                                    daysOverdue = 6,
                                    formattedAmount = "LKR 30,000.00",
                                    formattedDate = "01/10/2026"
                                )
                            )
                        ),
                        pendingGroup = InvoiceGroupData(
                            count = 1,
                            totalFormatted = "LKR 45,000.00",
                            invoices = listOf(
                                InvoiceItemUiModel(
                                    invoice = Invoice(
                                        id = "2",
                                        client = "Ceylon Tours",
                                        project = "Web app",
                                        milestone = "Milestone 2",
                                        amountMinor = 4_500_000L,
                                        currency = "LKR",
                                        dueDate = LocalDate.of(2026, 10, 20),
                                        status = InvoiceStatus.PENDING
                                    ),
                                    daysOverdue = null,
                                    formattedAmount = "LKR 45,000.00",
                                    formattedDate = "20/10/2026"
                                )
                            )
                        ),
                        receivedGroup = InvoiceGroupData(
                            count = 1,
                            totalFormatted = "LKR 20,000.00",
                            invoices = listOf(
                                InvoiceItemUiModel(
                                    invoice = Invoice(
                                        id = "3",
                                        client = "Gamma Ltd",
                                        project = "API integration",
                                        amountMinor = 2_000_000L,
                                        currency = "LKR",
                                        dueDate = LocalDate.of(2026, 9, 1),
                                        status = InvoiceStatus.RECEIVED,
                                        receivedTransactionId = "tx_01"
                                    ),
                                    daysOverdue = null,
                                    formattedAmount = "LKR 20,000.00",
                                    formattedDate = "03/09/2026"
                                )
                            )
                        ),
                        totalInvoicesCount = 3
                    )
                )
            ),
            onRetry = {},
            onAddInvoiceClick = {},
            onEditInvoiceClick = {},
            onMarkReceivedClick = {},
            onViewTransactionClick = {},
            onFormClientChange = {},
            onFormProjectChange = {},
            onFormMilestoneChange = {},
            onFormAmountChange = {},
            onFormCurrencyChange = {},
            onFormDueDateChange = {},
            onFormNoteChange = {},
            onFormSave = {},
            onFormDismiss = {},
            onMarkReceivedAmountChange = {},
            onMarkReceivedAccountChange = {},
            onMarkReceivedDateChange = {},
            onMarkReceivedFxRateChange = {},
            onMarkReceivedConfirm = {},
            onMarkReceivedDismiss = {},
            onViewTransactionDismiss = {}
        )
    }
}
