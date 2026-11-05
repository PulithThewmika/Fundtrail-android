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
fun ForgotPasswordRoute(
    onNavigateToSignIn: () -> Unit,
    viewModel: ForgotPasswordViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ForgotPasswordScreen(
        state = state,
        onEmailChange = viewModel::onEmailChange,
        onSubmit = viewModel::onSubmit,
        onNavigateToSignIn = onNavigateToSignIn,
    )
}

@Composable
fun ForgotPasswordScreen(
    state: ForgotPasswordState,
    onEmailChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onNavigateToSignIn: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
    ) {
        Text(stringResource(R.string.auth_forgot_password_title), style = MaterialTheme.typography.headlineMedium)
        if (state.emailSent) {
            Text(stringResource(R.string.auth_forgot_password_sent, state.email))
        } else {
            Text(stringResource(R.string.auth_forgot_password_body), style = MaterialTheme.typography.bodyMedium)
            AuthTextField(state.email, onEmailChange, R.string.auth_email, state.emailError, !state.isLoading)
            FormError(state.error)
            Button(onClick = onSubmit, enabled = !state.isLoading, modifier = Modifier.fillMaxWidth()) {
                if (state.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.padding(2.dp))
                } else {
                    Text(stringResource(R.string.auth_forgot_password_action))
                }
            }
        }
        TextButton(onClick = onNavigateToSignIn, enabled = !state.isLoading) { Text(stringResource(R.string.auth_to_sign_in)) }
    }
}
