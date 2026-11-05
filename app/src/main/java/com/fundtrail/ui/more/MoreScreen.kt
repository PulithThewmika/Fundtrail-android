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
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fundtrail.R
import com.fundtrail.ui.auth.FormError
import androidx.compose.ui.platform.LocalContext
import android.content.Intent
import java.io.File

// TODO(M3): replace with SCR-54 Settings; this lists SCR-53 Categories, recurring costs, reminders,
// the income estimate (DLG-02) and the sign-out action. Subscription-review interval and the crypto
// toggle are still missing.
@Composable
fun MoreRoute(
    onOpenCategories: () -> Unit,
    onOpenRecurring: () -> Unit = {},
    onOpenReminders: () -> Unit = {},
    onSignedOut: () -> Unit,
    onNavigateToAccounts: () -> Unit,
    viewModel: SignOutViewModel = hiltViewModel(),
    securityViewModel: SecuritySettingsViewModel = hiltViewModel(),
    deleteAccountViewModel: DeleteAccountViewModel = hiltViewModel(),
    exportCsvViewModel: ExportCsvViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val security by securityViewModel.uiState.collectAsStateWithLifecycle()
    val deleteState by deleteAccountViewModel.uiState.collectAsStateWithLifecycle()
    val exportState by exportCsvViewModel.uiState.collectAsStateWithLifecycle()
    var showIncomeSettings by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(state.signedOut) {
        if (state.signedOut) {
            onSignedOut()
            viewModel.onNavigated()
        }
    }
    LaunchedEffect(deleteState.deleted) {
        if (deleteState.deleted) {
            onSignedOut()
            deleteAccountViewModel.onNavigated()
        }
    }
    LaunchedEffect(exportState.csv) {
        val csv = exportState.csv ?: return@LaunchedEffect
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, "fundtrail-export.csv")
        file.writeText(csv)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, null))
        exportCsvViewModel.onHandled()
    }
    MoreScreen(
        state = state,
        security = security,
        deleteState = deleteState,
        exportLoading = exportState.isLoading,
        onAppLockToggled = securityViewModel::onAppLockToggled,
        onOpenCategories = onOpenCategories,
        onOpenIncomeSettings = { showIncomeSettings = true },
        onOpenRecurring = onOpenRecurring,
        onOpenReminders = onOpenReminders,
        onNavigateToAccounts = onNavigateToAccounts,
        onSignOut = viewModel::onSignOutClick,
        onConfirmSignOut = viewModel::onConfirmSignOut,
        onDismissConfirm = viewModel::onDismissConfirm,
        onDeleteAccount = deleteAccountViewModel::onDeleteClick,
        onConfirmDeleteAccount = deleteAccountViewModel::onConfirmDelete,
        onDismissDeleteConfirm = deleteAccountViewModel::onDismissConfirm,
        onExportCsv = exportCsvViewModel::onExportClick,
        demoDataSection = { DemoDataSection() },
    )

    if (showIncomeSettings) {
        IncomeSettingsDialog(onDismiss = { showIncomeSettings = false })
    }
}

@Composable
fun MoreScreen(
    state: SignOutState,
    security: SecuritySettingsUiState,
    deleteState: DeleteAccountState = DeleteAccountState(),
    exportLoading: Boolean = false,
    onAppLockToggled: (Boolean) -> Unit,
    onOpenCategories: () -> Unit,
    onOpenIncomeSettings: () -> Unit,
    onOpenRecurring: () -> Unit = {},
    onOpenReminders: () -> Unit = {},
    onNavigateToAccounts: () -> Unit,
    onSignOut: () -> Unit,
    onConfirmSignOut: () -> Unit,
    onDismissConfirm: () -> Unit,
    onDeleteAccount: () -> Unit = {},
    onConfirmDeleteAccount: () -> Unit = {},
    onDismissDeleteConfirm: () -> Unit = {},
    onExportCsv: () -> Unit = {},
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
            onClick = onOpenCategories,
        )
        MoreRow(
            title = stringResource(R.string.more_income_settings),
            subtitle = stringResource(R.string.more_income_settings_subtitle),
            onClick = onOpenIncomeSettings,
        )
        MoreRow(
            title = stringResource(R.string.more_recurring),
            subtitle = stringResource(R.string.more_recurring_subtitle),
            onClick = onOpenRecurring,
        )
        MoreRow(
            title = stringResource(R.string.more_reminders),
            subtitle = stringResource(R.string.more_reminders_subtitle),
            onClick = onOpenReminders,
        )

        Text(stringResource(R.string.more_security), style = MaterialTheme.typography.titleSmall)
        AppLockRow(security, onAppLockToggled)
        Spacer(Modifier.height(8.dp))
        FormError(state.error)
        OutlinedButton(onClick = onNavigateToAccounts, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.accounts_title))
        }
        OutlinedButton(onClick = onExportCsv, enabled = !exportLoading, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.settings_export_csv))
        }
        OutlinedButton(onClick = onSignOut, enabled = !state.isLoading, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.settings_sign_out))
        }
        FormError(deleteState.error)
        OutlinedButton(onClick = onDeleteAccount, enabled = !deleteState.isLoading, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.delete_account_action), color = MaterialTheme.colorScheme.error)
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
    if (deleteState.confirming) {
        AlertDialog(
            onDismissRequest = onDismissDeleteConfirm,
            title = { Text(stringResource(R.string.delete_account_confirm_title)) },
            text = { Text(stringResource(R.string.delete_account_confirm_message)) },
            confirmButton = { TextButton(onClick = onConfirmDeleteAccount) { Text(stringResource(R.string.delete_account_confirm_action)) } },
            dismissButton = { TextButton(onClick = onDismissDeleteConfirm) { Text(stringResource(R.string.delete_account_cancel)) } },
        )
    }
}

/** Whole row is the toggle, so TalkBack reads one "App lock, switch, off" item (#222). */
@Composable
private fun AppLockRow(security: SecuritySettingsUiState, onToggled: (Boolean) -> Unit) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .toggleable(value = security.appLockEnabled, role = Role.Switch, onValueChange = onToggled)
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.app_lock_title), style = MaterialTheme.typography.labelLarge)
                Text(
                    stringResource(R.string.app_lock_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = security.appLockEnabled, onCheckedChange = null)
        }
        security.message?.let {
            Text(stringResource(it), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun MoreRow(title: String, subtitle: String, onClick: () -> Unit) {
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
                    Icons.Outlined.Category,
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
