package com.fundtrail.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fundtrail.R

@Composable
fun ConfirmResetPasswordRoute(
    oobCode: String,
    onDone: () -> Unit,
    viewModel: ConfirmResetPasswordViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ConfirmResetPasswordScreen(
        state = state,
        onPasswordChange = viewModel::onPasswordChange,
        onConfirmChange = viewModel::onConfirmChange,
        onSubmit = { viewModel.onSubmit(oobCode) },
        onDone = onDone,
    )
}

@Composable
fun ConfirmResetPasswordScreen(
    state: ConfirmResetPasswordState,
    onPasswordChange: (String) -> Unit,
    onConfirmChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onDone: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
    ) {
        Text(stringResource(R.string.auth_reset_password_title), style = MaterialTheme.typography.headlineMedium)
        if (state.done) {
            Text(stringResource(R.string.auth_reset_password_done))
            TextButton(onClick = onDone) { Text(stringResource(R.string.auth_to_sign_in)) }
        } else {
            AuthTextField(state.password, onPasswordChange, R.string.auth_new_password, state.passwordError, !state.isLoading, isPassword = true)
            AuthTextField(state.confirmPassword, onConfirmChange, R.string.auth_confirm_password, state.confirmError, !state.isLoading, isPassword = true)
            FormError(state.error)
            Button(onClick = onSubmit, enabled = !state.isLoading, modifier = Modifier.fillMaxWidth()) {
                if (state.isLoading) CircularProgressIndicator(modifier = Modifier.padding(2.dp)) else Text(stringResource(R.string.auth_reset_password_action))
            }
        }
    }
}
