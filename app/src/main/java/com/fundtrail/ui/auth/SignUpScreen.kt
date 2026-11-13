package com.fundtrail.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fundtrail.R
import com.fundtrail.ui.common.PrimaryButton

@Composable
fun SignUpRoute(
    onRegistered: () -> Unit,
    onNavigateToSignIn: () -> Unit,
    viewModel: SignUpViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.registered) {
        if (state.registered) {
            onRegistered()
            viewModel.onNavigated()
        }
    }
    SignUpScreen(
        state = state,
        onEmailChange = viewModel::onEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onConfirmChange = viewModel::onConfirmChange,
        onSubmit = viewModel::onSubmit,
        onNavigateToSignIn = onNavigateToSignIn,
        googleSignIn = { GoogleSignInSection(onSignedIn = onRegistered, enabled = !state.isLoading) },
    )
}

@Composable
fun SignUpScreen(
    state: SignUpState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onNavigateToSignIn: () -> Unit,
    /** "Continue with Google" (#213); empty in previews. */
    googleSignIn: @Composable () -> Unit = {},
) {
    AuthScaffold(
        title = stringResource(R.string.auth_sign_up_title),
        subtitle = stringResource(R.string.auth_sign_up_subtitle),
    ) {
        AuthTextField(state.email, onEmailChange, R.string.auth_email, state.emailError, !state.isLoading)
        AuthTextField(state.password, onPasswordChange, R.string.auth_password, state.passwordError, !state.isLoading, isPassword = true)
        AuthTextField(state.confirmPassword, onConfirmChange, R.string.auth_confirm_password, state.confirmError, !state.isLoading, isPassword = true)
        FormError(state.error)
        PrimaryButton(stringResource(R.string.auth_sign_up_action), onClick = onSubmit, loading = state.isLoading)
        AuthDivider(stringResource(R.string.auth_or_divider))
        googleSignIn()
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            TextButton(onClick = onNavigateToSignIn, enabled = !state.isLoading) { Text(stringResource(R.string.auth_to_sign_in)) }
        }
    }
}
