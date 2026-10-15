package com.fundtrail.ui.more

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fundtrail.R
import com.fundtrail.ui.auth.FormError

// TODO(M2): replace with SCR-54 Settings; for now this hosts the sign-out action only.
@Composable
fun MoreRoute(
    onSignedOut: () -> Unit,
    onNavigateToAccounts: () -> Unit,
    viewModel: SignOutViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.signedOut) {
        if (state.signedOut) {
            onSignedOut()
            viewModel.onNavigated()
        }
    }
    MoreScreen(
        state = state,
        onNavigateToAccounts = onNavigateToAccounts,
        onSignOut = viewModel::onSignOutClick,
        onConfirmSignOut = viewModel::onConfirmSignOut,
        onDismissConfirm = viewModel::onDismissConfirm,
    )
}

@Composable
fun MoreScreen(
    state: SignOutState,
    onNavigateToAccounts: () -> Unit,
    onSignOut: () -> Unit,
    onConfirmSignOut: () -> Unit,
    onDismissConfirm: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        FormError(state.error)
        Button(
            onClick = onNavigateToAccounts,
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            Text("Accounts")
        }
        Button(onClick = onSignOut, enabled = !state.isLoading) {
            Text(stringResource(R.string.settings_sign_out))
        }
    }
    if (state.confirmUnsynced) {
        AlertDialog(
            onDismissRequest = onDismissConfirm,
            title = { Text(stringResource(R.string.sign_out_unsynced_title)) },
            text = { Text(stringResource(R.string.sign_out_unsynced_message)) },
            confirmButton = { TextButton(onClick = onConfirmSignOut) { Text(stringResource(R.string.sign_out_anyway)) } },
            dismissButton = { TextButton(onClick = onDismissConfirm) { Text(stringResource(R.string.sign_out_cancel)) } },
        )
    }
}
