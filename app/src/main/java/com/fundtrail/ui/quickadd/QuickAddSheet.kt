package com.fundtrail.ui.quickadd

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fundtrail.R
import com.fundtrail.domain.model.APP_ZONE
import com.fundtrail.domain.model.Account
import com.fundtrail.domain.model.IncomeSource
import com.fundtrail.domain.model.LKR
import com.fundtrail.domain.model.SUPPORTED_CURRENCIES
import com.fundtrail.domain.model.SpendKind
import com.fundtrail.domain.model.TransactionType
import com.fundtrail.ui.categories.CategoryIcons
import com.fundtrail.ui.common.DateFormatter
import com.fundtrail.ui.common.EstimatedChip
import com.fundtrail.ui.common.KindPicker
import com.fundtrail.ui.common.MoneyFormatter
import com.fundtrail.ui.common.SectionLabel
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Quick-add sheet supporting Expense, Income, and Transfer (SCR-20, EPIC-009), shown over the Insights dashboard.
 * [onSaved] gets an `undo` action for the snackbar that the host shows after the sheet has closed. The action
 * takes a callback that receives the string resource of the error the host must show if the undo fails.
 */
@Composable
fun QuickAddRoute(
    onDismiss: () -> Unit,
    onSaved: (undo: (onFailed: (messageRes: Int) -> Unit) -> Unit) -> Unit,
    editTransactionId: String? = null,
    viewModel: QuickAddViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(editTransactionId) {
        if (editTransactionId != null) viewModel.startEdit(editTransactionId)
    }

    LaunchedEffect(state.savedTransactionId) {
        val id = state.savedTransactionId ?: return@LaunchedEffect
        // An edit has nothing to undo; History already offers Undo for its own delete (#109).
        if (!state.isEditing) onSaved { onFailed -> viewModel.undo(id, onFailed) }
        viewModel.onSavedHandled()
        onDismiss()
    }
    // The ViewModel outlives the sheet, so clear the form on close or the next open would show the old draft.
    DisposableEffect(Unit) { onDispose { viewModel.reset() } }

    QuickAddSheet(
        state = state,
        onDismiss = onDismiss,
        onTransactionTypeSelected = viewModel::onTransactionTypeSelected,
        onKeypadDigit = viewModel::onKeypadDigit,
        onKeypadDot = viewModel::onKeypadDot,
        onKeypadBackspace = viewModel::onKeypadBackspace,
        onCategorySelected = viewModel::onCategorySelected,
        onAccountSelected = viewModel::onAccountSelected,
        onToAccountSelected = viewModel::onToAccountSelected,
        onIncomeSourceSelected = viewModel::onIncomeSourceSelected,
        onCurrencySelected = viewModel::onCurrencySelected,
        onFxModeSelected = viewModel::onFxModeSelected,
        onRateChange = viewModel::onRateChange,
        onLkrReceivedChange = viewModel::onLkrReceivedChange,
        onCryptoLossChange = viewModel::onCryptoLossChange,
        onDateChange = viewModel::onDateChange,
        onNoteChange = viewModel::onNoteChange,
        onSpendKindSelected = viewModel::onSpendKindSelected,
        onToggleMoreOptions = viewModel::onToggleMoreOptions,
        onSave = viewModel::onSave,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickAddSheet(
    state: QuickAddUiState,
    onDismiss: () -> Unit,
    onTransactionTypeSelected: (TransactionType) -> Unit,
    onKeypadDigit: (String) -> Unit,
    onKeypadDot: () -> Unit,
    onKeypadBackspace: () -> Unit,
    onCategorySelected: (String) -> Unit,
    onAccountSelected: (String) -> Unit,
    onToAccountSelected: (String) -> Unit,
    onIncomeSourceSelected: (IncomeSource) -> Unit,
    onCurrencySelected: (String) -> Unit,
    onFxModeSelected: (FxMode) -> Unit,
    onRateChange: (String) -> Unit,
    onLkrReceivedChange: (String) -> Unit,
    onCryptoLossChange: (Boolean) -> Unit,
    onDateChange: (LocalDate) -> Unit,
    onNoteChange: (String) -> Unit,
    onSpendKindSelected: (SpendKind) -> Unit,
    onToggleMoreOptions: () -> Unit,
    onSave: () -> Unit,
) {
    var showDatePicker by remember { mutableStateOf(false) }
    val firstKeyFocusRequester = remember { FocusRequester() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Header Row
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    stringResource(if (state.isEditing) R.string.quick_add_edit_title else R.string.quick_add_title),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f),
                )
                // Giving the LKR received below confirms it and makes the entry ACTUAL (#177).
                if (state.isEstimated) EstimatedChip()
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.quick_add_close))
                }
            }

            // Segmented Control: Expense | Income | Transfer
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                val types = listOf(TransactionType.EXPENSE, TransactionType.INCOME, TransactionType.TRANSFER)
                types.forEachIndexed { index, type ->
                    SegmentedButton(
                        selected = state.transactionType == type,
                        onClick = { onTransactionTypeSelected(type) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = types.size),
                        // Locked while editing (#109): switching type would desync the sheet from the saved entry.
                        enabled = !state.isSaving && !state.isEditing,
                    ) {
                        Text(
                            when (type) {
                                TransactionType.EXPENSE -> stringResource(R.string.quick_add_type_expense)
                                TransactionType.INCOME -> stringResource(R.string.quick_add_type_income)
                                TransactionType.TRANSFER -> stringResource(R.string.quick_add_type_transfer)
                            },
                        )
                    }
                }
            }

            // Live Formatted Money Display & Amount Input (S1)
            val magnitudeMinorUnits = remember(state.amountText) { AmountInput.toMinor(state.amountText) ?: 0L }
            // A Crypto loss shows its minus sign before saving, so the sign is never a surprise (#104).
            val isCryptoLoss = state.transactionType == TransactionType.INCOME &&
                state.incomeSource == IncomeSource.CRYPTO && state.cryptoLoss
            val minorUnits = if (isCryptoLoss) -magnitudeMinorUnits else magnitudeMinorUnits
            // The headline figure is what the user typed, so a USD entry reads in USD (#175); the LKR it converts
            // to is shown under the rate field instead.
            val formattedMoney = remember(minorUnits, state.currency) { MoneyFormatter.format(minorUnits, state.currency) }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            ) {
                val amountDescription = stringResource(R.string.quick_add_amount_a11y, formattedMoney)
                Text(
                    text = formattedMoney,
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.primary,
                    // TalkBack reads "Amount, LKR 12.34" and announces each change while typing on the keypad.
                    modifier = Modifier.semantics {
                        contentDescription = amountDescription
                        liveRegion = LiveRegionMode.Polite
                    },
                )
                if (state.amountError != null) {
                    Text(
                        stringResource(state.amountError),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                // Neutral, never blocking: a plain secondary-colour line, not an error (A-22, A-23).
                state.goalCost?.let { hint ->
                    Text(
                        stringResource(R.string.quick_add_goal_cost, hint.days, hint.goalName),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    )
                }
            }

            // Custom Amount Keypad (S1)
            QuickAddKeypad(
                onDigitClick = onKeypadDigit,
                onDotClick = onKeypadDot,
                onBackspaceClick = onKeypadBackspace,
                enabled = !state.isSaving,
                firstKeyFocusRequester = firstKeyFocusRequester,
            )
            // Inside the sheet content, so it runs after the keypad above has attached the requester (S1).
            LaunchedEffect(Unit) { firstKeyFocusRequester.requestFocus() }

            // Dynamic fields per transaction type
            when (state.transactionType) {
                TransactionType.EXPENSE -> {
                    // Category Chips (S2)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SectionLabel(R.string.quick_add_category)
                        if (state.categories.isEmpty()) {
                            Text(
                                stringResource(R.string.quick_add_no_categories),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        } else {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(vertical = 2.dp)) {
                                items(state.categories, key = { it.id }) { category ->
                                    FilterChip(
                                        selected = category.id == state.selectedCategoryId,
                                        onClick = { onCategorySelected(category.id) },
                                        enabled = !state.isSaving,
                                        label = { Text(category.name) },
                                        leadingIcon = {
                                            Icon(CategoryIcons.vectorFor(category.icon), contentDescription = null, modifier = Modifier.size(18.dp))
                                        },
                                    )
                                }
                            }
                            if (state.categorySuggested) {
                                Text(
                                    stringResource(R.string.quick_add_category_suggested),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }

                    // Account Chips (S2)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SectionLabel(R.string.quick_add_account)
                        AccountChipRow(
                            accounts = state.accounts,
                            selectedAccountId = state.selectedAccountId,
                            enabled = !state.isSaving,
                            onAccountSelected = onAccountSelected,
                        )
                    }
                }

                TransactionType.INCOME -> {
                    // Income Source Chips
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SectionLabel(R.string.quick_add_income_source)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(vertical = 2.dp)) {
                            items(IncomeSource.entries, key = { it.name }) { source ->
                                FilterChip(
                                    selected = source == state.incomeSource,
                                    onClick = { onIncomeSourceSelected(source) },
                                    enabled = !state.isSaving,
                                    label = { Text(stringResource(source.labelRes)) },
                                )
                            }
                        }
                    }

                    // Currency chips, Income tab only (#174): the Expense and Transfer tabs stay LKR.
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SectionLabel(R.string.quick_add_currency_label)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(vertical = 2.dp)) {
                            items(SUPPORTED_CURRENCIES, key = { it }) { code ->
                                FilterChip(
                                    selected = code == state.currency,
                                    onClick = { onCurrencySelected(code) },
                                    enabled = !state.isSaving,
                                    label = { Text(code) },
                                )
                            }
                        }
                    }

                    if (state.currency != LKR) {
                        FxEntry(
                            state = state,
                            onFxModeSelected = onFxModeSelected,
                            onRateChange = onRateChange,
                            onLkrReceivedChange = onLkrReceivedChange,
                        )
                    }

                    // Gain / Loss toggle, Crypto only (#104): neutral wording, no red (A-22, NFR-14).
                    if (state.incomeSource == IncomeSource.CRYPTO) {
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            SegmentedButton(
                                selected = !state.cryptoLoss,
                                onClick = { onCryptoLossChange(false) },
                                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                                enabled = !state.isSaving,
                            ) { Text(stringResource(R.string.quick_add_crypto_gain)) }
                            SegmentedButton(
                                selected = state.cryptoLoss,
                                onClick = { onCryptoLossChange(true) },
                                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                                enabled = !state.isSaving,
                            ) { Text(stringResource(R.string.quick_add_crypto_loss)) }
                        }
                    }

                    // Deposit Account Chips
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SectionLabel(R.string.quick_add_account)
                        AccountChipRow(
                            accounts = state.accounts,
                            selectedAccountId = state.selectedAccountId,
                            enabled = !state.isSaving,
                            onAccountSelected = onAccountSelected,
                        )
                    }
                }

                TransactionType.TRANSFER -> {
                    // From Account Chips
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SectionLabel(R.string.quick_add_from_account)
                        AccountChipRow(
                            accounts = state.accounts,
                            selectedAccountId = state.selectedAccountId,
                            enabled = !state.isSaving,
                            onAccountSelected = onAccountSelected,
                        )
                    }

                    // To Account Chips
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SectionLabel(R.string.quick_add_to_account)
                        AccountChipRow(
                            accounts = state.accounts,
                            selectedAccountId = state.selectedToAccountId,
                            enabled = !state.isSaving,
                            onAccountSelected = onToAccountSelected,
                        )
                    }
                }
            }

            // Date Picker Row (S2)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionLabel(R.string.quick_add_date)
                AssistChip(
                    onClick = { showDatePicker = true },
                    enabled = !state.isSaving,
                    label = {
                        Text(
                            if (state.date == LocalDate.now(APP_ZONE)) stringResource(R.string.quick_add_today)
                            else DateFormatter.format(state.date),
                        )
                    },
                    leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(18.dp)) },
                )
            }

            // Optional Note & SpendKind Override
            TextButton(onClick = onToggleMoreOptions) {
                Text(stringResource(R.string.quick_add_more_options))
                Icon(
                    if (state.moreOptionsExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                )
            }
            if (state.moreOptionsExpanded) {
                OutlinedTextField(
                    value = state.note,
                    onValueChange = onNoteChange,
                    label = {
                        Text(
                            stringResource(
                                if (state.transactionType == TransactionType.INCOME) R.string.quick_add_income_note_label
                                else R.string.quick_add_note_label,
                            ),
                        )
                    },
                    singleLine = true,
                    enabled = !state.isSaving,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth(),
                )

                if (state.transactionType == TransactionType.EXPENSE) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SectionLabel(R.string.category_type_label)
                        KindPicker(
                            selected = state.spendKind ?: SpendKind.DISCRETIONARY,
                            enabled = !state.isSaving && state.spendKind != null,
                            onSelected = onSpendKindSelected,
                        )
                        Text(
                            stringResource(
                                if (state.spendKindOverridden) R.string.quick_add_type_overridden else R.string.quick_add_type_follows,
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            state.errorMessage?.let {
                Text(stringResource(it), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
            }

            Button(onClick = onSave, enabled = state.canSave, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Text(stringResource(R.string.quick_add_save))
            }
        }
    }

    if (showDatePicker) {
        QuickAddDatePicker(
            selected = state.date,
            onSelected = { onDateChange(it); showDatePicker = false },
            onDismiss = { showDatePicker = false },
        )
    }
}

/**
 * The second input a non-LKR income entry needs (#176): either the LKR that actually landed or the rate the
 * user expects, with the other figure derived live underneath. Never red: an incomplete pair is a hint, not
 * an error (NFR-14).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FxEntry(
    state: QuickAddUiState,
    onFxModeSelected: (FxMode) -> Unit,
    onRateChange: (String) -> Unit,
    onLkrReceivedChange: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            val modes = listOf(FxMode.LkrReceived, FxMode.Rate)
            modes.forEachIndexed { index, mode ->
                SegmentedButton(
                    selected = state.fxMode == mode,
                    onClick = { onFxModeSelected(mode) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = modes.size),
                    enabled = !state.isSaving,
                ) {
                    Text(
                        stringResource(
                            if (mode == FxMode.LkrReceived) R.string.quick_add_fx_mode_lkr
                            else R.string.quick_add_fx_mode_rate,
                        ),
                    )
                }
            }
        }

        val isRateMode = state.fxMode == FxMode.Rate
        OutlinedTextField(
            value = if (isRateMode) state.rateText else state.lkrText,
            onValueChange = if (isRateMode) onRateChange else onLkrReceivedChange,
            label = {
                Text(
                    when {
                        // The remembered rate is prefilled but editable; the label says so (#179).
                        isRateMode && state.ratePrefilled ->
                            stringResource(R.string.quick_add_rate_label, state.currency) +
                                " (" + stringResource(R.string.quick_add_rate_last_used) + ")"
                        isRateMode -> stringResource(R.string.quick_add_rate_label, state.currency)
                        else -> stringResource(R.string.quick_add_lkr_received_label)
                    },
                )
            },
            singleLine = true,
            enabled = !state.isSaving,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
        )

        val fx = state.fx
        Text(
            text = when {
                fx == null -> stringResource(R.string.quick_add_fx_hint)
                isRateMode -> stringResource(R.string.quick_add_fx_derived_lkr, MoneyFormatter.formatLkr(fx.lkrMinor))
                else -> stringResource(R.string.quick_add_fx_derived_rate, formatRate(fx.rateMicros), state.currency)
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
    }
}

/** A derived rate reads as LKR per unit to four decimals; micros is a storage detail the user never sees. */
private fun formatRate(micros: Long): String =
    BigDecimal.valueOf(micros, 6).setScale(4, RoundingMode.HALF_UP).toPlainString()

@Composable
private fun AccountChipRow(
    accounts: List<Account>,
    selectedAccountId: String?,
    enabled: Boolean,
    onAccountSelected: (String) -> Unit,
) {
    if (accounts.isEmpty()) {
        Text(
            stringResource(R.string.quick_add_no_accounts),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    } else {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(vertical = 2.dp)) {
            items(accounts, key = { it.id }) { account ->
                FilterChip(
                    selected = account.id == selectedAccountId,
                    onClick = { onAccountSelected(account.id) },
                    enabled = enabled,
                    label = { Text(account.name) },
                )
            }
        }
    }
}

/** Past days and today only: a transaction cannot be in the future. The picker works in UTC days. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickAddDatePicker(selected: LocalDate, onSelected: (LocalDate) -> Unit, onDismiss: () -> Unit) {
    val today = LocalDate.now(APP_ZONE)
    val state = rememberDatePickerState(
        initialSelectedDateMillis = selected.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) =
                !Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneOffset.UTC).toLocalDate().isAfter(today)
        },
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val millis = state.selectedDateMillis
                    if (millis == null) onDismiss()
                    else onSelected(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
                },
            ) { Text(stringResource(R.string.quick_add_date_ok)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.category_cancel)) } },
    ) {
        DatePicker(state = state)
    }
}

@get:StringRes
private val IncomeSource.labelRes: Int
    get() = when (this) {
        IncomeSource.SALARY -> R.string.income_source_salary
        IncomeSource.FREELANCE -> R.string.income_source_freelance
        IncomeSource.ADSENSE -> R.string.income_source_adsense
        IncomeSource.CRYPTO -> R.string.income_source_crypto
        IncomeSource.OTHER -> R.string.income_source_other
    }
