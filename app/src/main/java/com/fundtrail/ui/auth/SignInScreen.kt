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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fundtrail.R

@Composable
fun SignInRoute(
    onSignedIn: () -> Unit,
    onNavigateToSignUp: () -> Unit,
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
    /** "Continue with Google" (#213); empty in previews. */
    googleSignIn: @Composable () -> Unit = {},
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
    ) {
        Text(stringResource(R.string.auth_sign_in_title), style = MaterialTheme.typography.headlineMedium)
        AuthTextField(state.email, onEmailChange, R.string.auth_email, state.emailError, !state.isLoading)
        AuthTextField(state.password, onPasswordChange, R.string.auth_password, state.passwordError, !state.isLoading, isPassword = true)
        FormError(state.error)
        Button(onClick = onSubmit, enabled = !state.isLoading, modifier = Modifier.fillMaxWidth()) {
            if (state.isLoading) CircularProgressIndicator(modifier = Modifier.padding(2.dp)) else Text(stringResource(R.string.auth_sign_in_action))
        }
        googleSignIn()
        TextButton(onClick = onNavigateToSignUp, enabled = !state.isLoading) { Text(stringResource(R.string.auth_to_sign_up)) }
    }
}
