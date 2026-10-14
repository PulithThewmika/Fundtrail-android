package com.fundtrail.ui.accounts

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.SuggestionChip
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
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fundtrail.R
import com.fundtrail.domain.model.Account
import com.fundtrail.domain.model.AccountKind

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
                title = { Text("Accounts") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddClick) {
                Icon(Icons.Default.Add, contentDescription = "Add account")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(state.accounts, key = { it.id }) { account ->
                AccountItemCard(
                    account = account,
                    onRenameClick = { onRenameClick(account) },
                    onArchiveClick = { onArchiveClick(account) }
                )
            }
        }

        if (state.showAddDialog) {
            AddAccountDialog(
                name = state.addName,
                kind = state.addKind,
                currency = state.addCurrency,
                nameError = state.addNameError,
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
                onNameChange = onRenameNameChange,
                onSubmit = onRenameSubmit,
                onDismiss = onRenameDismiss,
            )
        }

        if (state.infoMessage != null) {
            AlertDialog(
                onDismissRequest = onDismissInfoMessage,
                title = { Text("Notice") },
                text = { Text(state.infoMessage) },
                confirmButton = {
                    TextButton(onClick = onDismissInfoMessage) {
                        Text("OK")
                    }
                }
            )
        }

        if (state.error != null) {
            AlertDialog(
                onDismissRequest = onDismissError,
                title = { Text("Error") },
                text = { Text(state.error) },
                confirmButton = {
                    TextButton(onClick = onDismissError) {
                        Text("OK")
                    }
                }
            )
        }
    }
}

@Composable
private fun AccountItemCard(
    account: Account,
    onRenameClick: () -> Unit,
    onArchiveClick: () -> Unit,
) {
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
                        text = "${account.kind.name} • ${account.currency}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (account.archived) {
                    SuggestionChip(
                        onClick = { },
                        label = { Text("Archived") }
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onRenameClick) {
                    Text("Rename")
                }
                Spacer(modifier = Modifier.width(8.dp))
                if (!account.archived) {
                    TextButton(onClick = onArchiveClick) {
                        Text("Archive")
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
    nameError: String?,
    onNameChange: (String) -> Unit,
    onKindChange: (AccountKind) -> Unit,
    onCurrencyChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Account") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    label = { Text("Account Name") },
                    isError = nameError != null,
                    supportingText = nameError?.let { { Text(it) } },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = kind.name,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Account Kind") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        AccountKind.entries.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.name) },
                                onClick = {
                                    onKindChange(option)
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = currency,
                    onValueChange = onCurrencyChange,
                    label = { Text("Currency") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = onSubmit) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun RenameAccountDialog(
    name: String,
    nameError: String?,
    onNameChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename Account") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    label = { Text("New Account Name") },
                    isError = nameError != null,
                    supportingText = nameError?.let { { Text(it) } },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = onSubmit) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
