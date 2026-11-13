package com.fundtrail.ui.auth

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fundtrail.R
import com.fundtrail.ui.common.PrimaryButton

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
    AuthScaffold(
        title = stringResource(R.string.auth_forgot_password_title),
        subtitle = stringResource(R.string.auth_forgot_password_body),
    ) {
        if (state.emailSent) {
            Text(stringResource(R.string.auth_forgot_password_sent, state.email), style = MaterialTheme.typography.bodyLarge)
        } else {
            AuthTextField(state.email, onEmailChange, R.string.auth_email, state.emailError, !state.isLoading)
            FormError(state.error)
            PrimaryButton(stringResource(R.string.auth_forgot_password_action), onClick = onSubmit, loading = state.isLoading)
        }
        TextButton(onClick = onNavigateToSignIn, enabled = !state.isLoading, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.auth_to_sign_in))
        }
    }
}
