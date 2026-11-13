package com.fundtrail.ui.accounts

import com.fundtrail.ui.common.FormSheet
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fundtrail.R
import com.fundtrail.ui.common.ListRow
import com.fundtrail.ui.common.StatusChip
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.CurrencyBitcoin
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.IconButton
import androidx.compose.ui.graphics.vector.ImageVector
import com.fundtrail.ui.common.AppCard
import com.fundtrail.ui.common.AppScreen
import com.fundtrail.domain.model.Account
import com.fundtrail.domain.model.AccountKind
import com.fundtrail.domain.model.SUPPORTED_CURRENCIES
import com.fundtrail.ui.common.UiState
import com.fundtrail.ui.common.Skeleton
import com.fundtrail.ui.common.SkeletonCard
import com.fundtrail.ui.common.SkeletonCircle
import com.fundtrail.ui.common.SkeletonLine
import com.fundtrail.ui.theme.FundtrailAppTheme
import com.fundtrail.ui.common.EmptyView
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.CloudOff

@Composable
fun AccountsRoute(
    onBackClick: () -> Unit,
    viewModel: AccountsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    AccountsScreen(
        state = state,
        onBackClick = onBackClick,
        onAddClick = viewModel::onAddClick,
        onAddNameChange = viewModel::onAddNameChange,
        onAddKindChange = viewModel::onAddKindChange,
        onAddCurrencyChange = viewModel::onAddCurrencyChange,
        onAddSubmit = viewModel::onAddSubmit,
        onAddDismiss = viewModel::onAddDismiss,
        onRenameClick = viewModel::onRenameClick,
        onRenameNameChange = viewModel::onRenameNameChange,
        onRenameSubmit = viewModel::onRenameSubmit,
        onRenameDismiss = viewModel::onRenameDismiss,
        onArchiveClick = viewModel::onArchiveClick,
        onUnarchiveClick = viewModel::onUnarchiveClick,
        onDismissInfoMessage = viewModel::onDismissInfoMessage,
        onDismissError = viewModel::onDismissError,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsScreen(
    state: AccountsUiState,
    onBackClick: () -> Unit,
    onAddClick: () -> Unit,
    onAddNameChange: (String) -> Unit,
    onAddKindChange: (AccountKind) -> Unit,
    onAddCurrencyChange: (String) -> Unit,
    onAddSubmit: () -> Unit,
    onAddDismiss: () -> Unit,
    onRenameClick: (Account) -> Unit,
    onRenameNameChange: (String) -> Unit,
    onRenameSubmit: () -> Unit,
    onRenameDismiss: () -> Unit,
    onArchiveClick: (Account) -> Unit,
    onUnarchiveClick: (Account) -> Unit,
    onDismissInfoMessage: () -> Unit,
    onDismissError: () -> Unit,
) {
    AppScreen(
        title = stringResource(R.string.accounts_title),
        onBack = onBackClick,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.accessibility_add_account))
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val content = state.content) {
                is UiState.Loading -> AccountsSkeleton()
                is UiState.Empty -> EmptyView(
                    icon = Icons.Outlined.AccountBalanceWallet,
                    title = stringResource(R.string.accounts_empty_title),
                    body = stringResource(R.string.accounts_empty_body),
                    actionLabel = stringResource(R.string.accounts_add_account),
                    actionIcon = Icons.Default.Add,
                    onAction = onAddClick,
                )
                is UiState.Success -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(content.data, key = { it.id }) { account ->
                            AccountItemCard(
                                account = account,
                                onRenameClick = { onRenameClick(account) },
                                onArchiveClick = { onArchiveClick(account) },
                                onUnarchiveClick = { onUnarchiveClick(account) }
                            )
                        }
                    }
                }
                // Only "offline with nothing cached" reaches here, and it clears itself on reconnect: no button.
                is UiState.Error -> EmptyView(icon = Icons.Outlined.CloudOff, title = stringResource(content.message))
            }
        }

        if (state.showAddDialog) {
            AddAccountDialog(
                name = state.addName,
                kind = state.addKind,
                currency = state.addCurrency,
                nameError = state.addNameError,
                isLoading = state.isLoading,
                onNameChange = onAddNameChange,
                onKindChange = onAddKindChange,
                onCurrencyChange = onAddCurrencyChange,
                onSubmit = onAddSubmit,
                onDismiss = onAddDismiss,
            )
        }

        if (state.renamingAccount != null) {
            RenameAccountDialog(
                name = state.renameName,
                nameError = state.renameNameError,
                isLoading = state.isLoading,
                onNameChange = onRenameNameChange,
                onSubmit = onRenameSubmit,
                onDismiss = onRenameDismiss,
            )
        }

        if (state.infoMessage != null) {
            AlertDialog(
                onDismissRequest = onDismissInfoMessage,
                title = { Text(stringResource(R.string.notice_title)) },
                text = { Text(stringResource(state.infoMessage)) },
                confirmButton = {
                    TextButton(onClick = onDismissInfoMessage) {
                        Text(stringResource(R.string.ok))
                    }
                }
            )
        }

        if (state.error != null) {
            AlertDialog(
                onDismissRequest = onDismissError,
                title = { Text(stringResource(R.string.error_title)) },
                text = { Text(stringResource(state.error)) },
                confirmButton = {
                    TextButton(onClick = onDismissError) {
                        Text(stringResource(R.string.ok))
                    }
                }
            )
        }
    }
}

@Composable
internal fun AccountKind.displayName(): String = when (this) {
    AccountKind.BANK -> stringResource(R.string.account_kind_bank)
    AccountKind.CASH -> stringResource(R.string.account_kind_cash)
    AccountKind.SAVINGS -> stringResource(R.string.account_kind_savings)
    AccountKind.CRYPTO_EXCHANGE -> stringResource(R.string.account_kind_crypto_exchange)
    AccountKind.OTHER -> stringResource(R.string.account_kind_other)
}

/** Account cards as [AccountItemCard] draws them: name, kind line and the menu button. */
@Composable
private fun AccountsSkeleton() {
    Skeleton(stringResource(R.string.accounts_loading), Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        repeat(4) {
            SkeletonCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SkeletonLine(0.45f, 16.dp)
                        SkeletonLine(0.25f, 10.dp)
                    }
                    SkeletonCircle(24.dp)
                }
            }
        }
    }
}

/** One compact row per account; rename and archive sit behind the overflow menu instead of taking a second line. */
@Composable
private fun AccountItemCard(
    account: Account,
    onRenameClick: () -> Unit,
    onArchiveClick: () -> Unit,
    onUnarchiveClick: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    AppCard(contentPadding = 0.dp) {
        ListRow(
            title = account.name,
            subtitle = "${account.kind.displayName()} • ${account.currency}",
            icon = account.kind.icon(),
            trailing = {
                if (account.archived) StatusChip(stringResource(R.string.accounts_archived))
                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.accounts_more_actions, account.name))
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.accounts_rename)) },
                            onClick = { menuOpen = false; onRenameClick() },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(if (account.archived) R.string.accounts_unarchive else R.string.accounts_archive)) },
                            onClick = { menuOpen = false; if (account.archived) onUnarchiveClick() else onArchiveClick() },
                        )
                    }
                }
            },
        )
    }
}

private fun AccountKind.icon(): ImageVector = when (this) {
    AccountKind.BANK -> Icons.Outlined.AccountBalance
    AccountKind.CASH -> Icons.Outlined.Payments
    AccountKind.SAVINGS -> Icons.Outlined.Savings
    AccountKind.CRYPTO_EXCHANGE -> Icons.Outlined.CurrencyBitcoin
    AccountKind.OTHER -> Icons.Outlined.AccountBalanceWallet
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddAccountDialog(
    name: String,
    kind: AccountKind,
    currency: String,
    @androidx.annotation.StringRes nameError: Int?,
    isLoading: Boolean,
    onNameChange: (String) -> Unit,
    onKindChange: (AccountKind) -> Unit,
    onCurrencyChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit,
) {
    var kindExpanded by remember { mutableStateOf(false) }
    var currencyExpanded by remember { mutableStateOf(false) }
    val kindName = kind.displayName()

    FormSheet(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.accounts_add_title),
        confirmLabel = stringResource(R.string.accounts_add_action),
        onConfirm = onSubmit,
        loading = isLoading,
        dismissLabel = stringResource(R.string.accounts_cancel_action),
        content = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    label = { Text(stringResource(R.string.accounts_name_label)) },
                    isError = nameError != null,
                    supportingText = nameError?.let { { Text(stringResource(it)) } },
                    singleLine = true,
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth()
                )

                ExposedDropdownMenuBox(
                    expanded = kindExpanded,
                    onExpandedChange = { if (!isLoading) kindExpanded = it }
                ) {
                    OutlinedTextField(
                        value = kindName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.accounts_kind_label)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = kindExpanded) },
                        enabled = !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = kindExpanded,
                        onDismissRequest = { kindExpanded = false }
                    ) {
                        AccountKind.entries.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.displayName()) },
                                onClick = {
                                    onKindChange(option)
                                    kindExpanded = false
                                }
                            )
                        }
                    }
                }

                ExposedDropdownMenuBox(
                    expanded = currencyExpanded,
                    onExpandedChange = { if (!isLoading) currencyExpanded = it }
                ) {
                    OutlinedTextField(
                        value = currency,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.accounts_currency_label)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = currencyExpanded) },
                        enabled = !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = currencyExpanded,
                        onDismissRequest = { currencyExpanded = false }
                    ) {
                        SUPPORTED_CURRENCIES.forEach { curr ->
                            DropdownMenuItem(
                                text = { Text(curr) },
                                onClick = {
                                    onCurrencyChange(curr)
                                    currencyExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
    )
}

@Composable
private fun RenameAccountDialog(
    name: String,
    @androidx.annotation.StringRes nameError: Int?,
    isLoading: Boolean,
    onNameChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit,
) {
    FormSheet(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.accounts_rename_title),
        confirmLabel = stringResource(R.string.accounts_save_action),
        onConfirm = onSubmit,
        loading = isLoading,
        dismissLabel = stringResource(R.string.accounts_cancel_action),
        content = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    label = { Text(stringResource(R.string.accounts_name_label)) },
                    isError = nameError != null,
                    supportingText = nameError?.let { { Text(stringResource(it)) } },
                    singleLine = true,
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
    )
}

@Preview(showBackground = true)
@Composable
private fun AccountsScreenPreview() {
    FundtrailAppTheme {
        AccountsScreen(
            state = AccountsUiState(
                content = UiState.Success(
                    listOf(
                        Account(id = "1", name = "Main bank", kind = AccountKind.BANK, currency = "LKR", sortOrder = 1),
                        Account(id = "2", name = "Cash Wallet", kind = AccountKind.CASH, currency = "LKR", sortOrder = 2),
                        Account(id = "3", name = "Binance", kind = AccountKind.CRYPTO_EXCHANGE, currency = "USDT", sortOrder = 3, archived = true),
                    )
                )
            ),
            onBackClick = {},
            onAddClick = {},
            onAddNameChange = {},
            onAddKindChange = {},
            onAddCurrencyChange = {},
            onAddSubmit = {},
            onAddDismiss = {},
            onRenameClick = {},
            onRenameNameChange = {},
            onRenameSubmit = {},
            onRenameDismiss = {},
            onArchiveClick = {},
            onUnarchiveClick = {},
            onDismissInfoMessage = {},
            onDismissError = {},
        )
    }
}
