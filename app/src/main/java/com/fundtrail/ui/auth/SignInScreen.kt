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
fun SignInRoute(
    onSignedIn: () -> Unit,
    onNavigateToSignUp: () -> Unit,
    onNavigateToForgotPassword: () -> Unit,
    viewModel: SignInViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.signedIn) {
        if (state.signedIn) {
            onSignedIn()
            viewModel.onNavigated()
        }
    }
    SignInScreen(
        state = state,
        onEmailChange = viewModel::onEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onSubmit = viewModel::onSubmit,
        onNavigateToSignUp = onNavigateToSignUp,
        onNavigateToForgotPassword = onNavigateToForgotPassword,
        googleSignIn = { GoogleSignInSection(onSignedIn = onSignedIn, enabled = !state.isLoading) },
    )
}

@Composable
fun SignInScreen(
    state: SignInState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onNavigateToSignUp: () -> Unit,
    onNavigateToForgotPassword: () -> Unit,
    /** "Continue with Google" (#213); empty in previews. */
    googleSignIn: @Composable () -> Unit = {},
) {
    AuthScaffold(
        title = stringResource(R.string.auth_sign_in_title),
        subtitle = stringResource(R.string.auth_sign_in_subtitle),
    ) {
        AuthTextField(state.email, onEmailChange, R.string.auth_email, state.emailError, !state.isLoading)
        AuthTextField(state.password, onPasswordChange, R.string.auth_password, state.passwordError, !state.isLoading, isPassword = true)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onNavigateToForgotPassword, enabled = !state.isLoading) { Text(stringResource(R.string.auth_forgot_password_link)) }
        }
        FormError(state.error)
        PrimaryButton(stringResource(R.string.auth_sign_in_action), onClick = onSubmit, loading = state.isLoading)
        AuthDivider(stringResource(R.string.auth_or_divider))
        googleSignIn()
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            TextButton(onClick = onNavigateToSignUp, enabled = !state.isLoading) { Text(stringResource(R.string.auth_to_sign_up)) }
        }
    }
}
