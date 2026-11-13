package com.fundtrail.ui.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fundtrail.R
import com.fundtrail.ui.common.PrimaryButton

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
    AuthScaffold(
        title = stringResource(R.string.auth_reset_password_title),
        subtitle = stringResource(if (state.done) R.string.auth_reset_password_done else R.string.auth_reset_password_subtitle),
    ) {
        if (state.done) {
            PrimaryButton(stringResource(R.string.auth_to_sign_in), onClick = onDone)
        } else {
            AuthTextField(state.password, onPasswordChange, R.string.auth_new_password, state.passwordError, !state.isLoading, isPassword = true)
            AuthTextField(state.confirmPassword, onConfirmChange, R.string.auth_confirm_password, state.confirmError, !state.isLoading, isPassword = true)
            FormError(state.error)
            PrimaryButton(stringResource(R.string.auth_reset_password_action), onClick = onSubmit, loading = state.isLoading)
        }
    }
}
