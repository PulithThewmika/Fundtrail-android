package com.fundtrail.ui.more

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fundtrail.R
import com.fundtrail.ui.auth.FormError

// TODO(M3): replace with SCR-54 Settings; this lists SCR-53 Categories, the income estimate (DLG-02)
// and the sign-out action. Reminder time, subscription-review interval, crypto toggle, CSV export
// and delete-account are still missing.

@Composable
fun MoreRoute(
    onOpenCategories: () -> Unit,
    onSignedOut: () -> Unit,
    onNavigateToAccounts: () -> Unit,
    viewModel: SignOutViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showIncomeSettings by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(state.signedOut) {
        if (state.signedOut) {
            onSignedOut()
            viewModel.onNavigated()
        }
    }
    MoreScreen(
        state = state,
        onOpenCategories = onOpenCategories,
        onOpenIncomeSettings = { showIncomeSettings = true },
        onNavigateToAccounts = onNavigateToAccounts,
        onSignOut = viewModel::onSignOutClick,
        onConfirmSignOut = viewModel::onConfirmSignOut,
        onDismissConfirm = viewModel::onDismissConfirm,
        demoDataSection = { DemoDataSection() },
    )

    if (showIncomeSettings) {
        IncomeSettingsDialog(onDismiss = { showIncomeSettings = false })
    }
}

@Composable
fun MoreScreen(
    state: SignOutState,
    onOpenCategories: () -> Unit,
    onOpenIncomeSettings: () -> Unit,
    onNavigateToAccounts: () -> Unit,
    onSignOut: () -> Unit,
    onConfirmSignOut: () -> Unit,
    onDismissConfirm: () -> Unit,
    /** Debug builds pass the demo data tools (#157); release and previews leave it empty. */
    demoDataSection: @Composable () -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(stringResource(R.string.more_title), style = MaterialTheme.typography.headlineSmall)
        MoreRow(
            title = stringResource(R.string.more_categories),
            subtitle = stringResource(R.string.more_categories_subtitle),
            icon = Icons.Outlined.Category,
            onClick = onOpenCategories,
        )
        MoreRow(
            title = stringResource(R.string.more_income_settings),
            subtitle = stringResource(R.string.more_income_settings_subtitle),
            icon = Icons.Outlined.Payments,
            onClick = onOpenIncomeSettings,
        )
        Spacer(Modifier.height(8.dp))
        FormError(state.error)
        OutlinedButton(onClick = onNavigateToAccounts, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.accounts_title))
        }
        OutlinedButton(onClick = onSignOut, enabled = !state.isLoading, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.settings_sign_out))
        }
        demoDataSection()
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

@Composable
private fun MoreRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(20.dp),
                )
            }
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.labelLarge)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outlineVariant,
            )
        }
    }
}
