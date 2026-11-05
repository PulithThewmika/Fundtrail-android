package com.fundtrail.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fundtrail.R

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
        Button(
            onClick = onSubmit,
            enabled = !state.isLoading,
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.onSurface,
                contentColor = MaterialTheme.colorScheme.surface,
            ),
            modifier = Modifier.fillMaxWidth().height(50.dp),
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.surface)
            } else {
                Text(stringResource(R.string.auth_sign_up_action), style = MaterialTheme.typography.titleMedium)
            }
        }
        AuthDivider(stringResource(R.string.auth_or_divider))
        googleSignIn()
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            TextButton(onClick = onNavigateToSignIn, enabled = !state.isLoading) { Text(stringResource(R.string.auth_to_sign_in)) }
        }
    }
}
