package com.fundtrail.ui.accounts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import com.fundtrail.domain.model.Account
import com.fundtrail.domain.model.AccountKind
import com.fundtrail.domain.model.SUPPORTED_CURRENCIES
import com.fundtrail.ui.common.UiState
import com.fundtrail.ui.theme.FundtrailAppTheme

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
    onDismissInfoMessage: () -> Unit,
    onDismissError: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.accounts_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.accessibility_back))
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddClick) {
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
                is UiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.accounts_loading))
                    }
                }
                is UiState.Empty -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(R.string.accounts_empty_title),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.accounts_empty_body),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
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
                                onArchiveClick = { onArchiveClick(account) }
                            )
                        }
                    }
                }
                is UiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(stringResource(content.message), color = MaterialTheme.colorScheme.error)
                    }
                }
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
private fun AccountKind.displayName(): String = when (this) {
    AccountKind.BANK -> stringResource(R.string.account_kind_bank)
    AccountKind.CASH -> stringResource(R.string.account_kind_cash)
    AccountKind.SAVINGS -> stringResource(R.string.account_kind_savings)
    AccountKind.CRYPTO_EXCHANGE -> stringResource(R.string.account_kind_crypto_exchange)
    AccountKind.OTHER -> stringResource(R.string.account_kind_other)
}

@Composable
private fun AccountItemCard(
    account: Account,
    onRenameClick: () -> Unit,
    onArchiveClick: () -> Unit,
) {
    val kindName = account.kind.displayName()
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = account.name,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "$kindName • ${account.currency}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (account.archived) {
                    Text(
                        text = stringResource(R.string.accounts_archived),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant,
                                MaterialTheme.shapes.small
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onRenameClick) {
                    Text(stringResource(R.string.accounts_rename))
                }
                Spacer(modifier = Modifier.width(8.dp))
                if (!account.archived) {
                    TextButton(onClick = onArchiveClick) {
                        Text(stringResource(R.string.accounts_archive))
                    }
                }
            }
        }
    }
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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.accounts_add_title)) },
        text = {
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
        confirmButton = {
            Button(onClick = onSubmit, enabled = !isLoading) {
                Text(stringResource(R.string.accounts_add_action))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) {
                Text(stringResource(R.string.accounts_cancel_action))
            }
        }
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
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.accounts_rename_title)) },
        text = {
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
        confirmButton = {
            Button(onClick = onSubmit, enabled = !isLoading) {
                Text(stringResource(R.string.accounts_save_action))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) {
                Text(stringResource(R.string.accounts_cancel_action))
            }
        }
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
            onDismissInfoMessage = {},
            onDismissError = {},
        )
    }
}
