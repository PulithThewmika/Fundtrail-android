package com.fundtrail.ui.more

import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import com.fundtrail.ui.common.StatusChip
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fundtrail.R
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextOverflow
import com.fundtrail.domain.model.UserProfile
import com.fundtrail.ui.common.Avatar
import com.fundtrail.ui.common.ProfileViewModel
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.EventRepeat
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Payments
import com.fundtrail.ui.common.AppCard
import com.fundtrail.ui.common.ListRow
import com.fundtrail.ui.common.AppScreen
import com.fundtrail.ui.common.SectionTitle
import com.fundtrail.ui.theme.Spacing
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
    profileViewModel: ProfileViewModel = hiltViewModel(),
    // The same instance the Income settings sheet uses, so the hint clears as soon as something is saved.
    incomeSettingsViewModel: IncomeSettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val security by securityViewModel.uiState.collectAsStateWithLifecycle()
    val deleteState by deleteAccountViewModel.uiState.collectAsStateWithLifecycle()
    val exportState by exportCsvViewModel.uiState.collectAsStateWithLifecycle()
    val profile by profileViewModel.profile.collectAsStateWithLifecycle()
    val incomeSettings by incomeSettingsViewModel.uiState.collectAsStateWithLifecycle()
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
        profile = profile,
        incomeNotSet = incomeSettings.nothingSet,
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
    profile: UserProfile? = null,
    incomeNotSet: Boolean = false,
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
    AppScreen(title = stringResource(R.string.more_title)) { padding ->
      Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())) {
        Column(
            Modifier.padding(horizontal = Spacing.screen).padding(bottom = Spacing.xl),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            if (profile != null) ProfileCard(profile)
            RowGroup(R.string.more_group_manage) {
                ListRow(
                    title = stringResource(R.string.accounts_title),
                    subtitle = stringResource(R.string.more_accounts_subtitle),
                    icon = Icons.Outlined.AccountBalanceWallet,
                    onClick = onNavigateToAccounts,
                )
                ListRow(
                    title = stringResource(R.string.more_categories),
                    subtitle = stringResource(R.string.more_categories_subtitle),
                    icon = Icons.Outlined.Category,
                    onClick = onOpenCategories,
                )
                ListRow(
                    title = stringResource(R.string.more_recurring),
                    subtitle = stringResource(R.string.more_recurring_subtitle),
                    icon = Icons.Outlined.EventRepeat,
                    onClick = onOpenRecurring,
                )
                ListRow(
                    title = stringResource(R.string.more_income_settings),
                    subtitle = stringResource(R.string.more_income_settings_subtitle),
                    icon = Icons.Outlined.Payments,
                    onClick = onOpenIncomeSettings,
                    trailing = {
                        if (incomeNotSet) StatusChip(stringResource(R.string.settings_not_set))
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
                    },
                )
                ListRow(
                    title = stringResource(R.string.more_reminders),
                    subtitle = stringResource(R.string.more_reminders_subtitle),
                    icon = Icons.Outlined.NotificationsNone,
                    onClick = onOpenReminders,
                )
            }
            RowGroup(R.string.more_security) {
                // Whole row is the toggle, so TalkBack reads one "App lock, switch, off" item (#222).
                ListRow(
                    title = stringResource(R.string.app_lock_title),
                    subtitle = stringResource(R.string.app_lock_subtitle),
                    icon = Icons.Outlined.Lock,
                    modifier = Modifier.toggleable(value = security.appLockEnabled, role = Role.Switch, onValueChange = onAppLockToggled),
                    trailing = { Switch(checked = security.appLockEnabled, onCheckedChange = null) },
                )
            }
            security.message?.let { FormError(it) }
            RowGroup(R.string.more_group_data) {
                ListRow(
                    title = stringResource(R.string.settings_export_csv),
                    icon = Icons.Outlined.FileDownload,
                    onClick = onExportCsv.takeUnless { exportLoading },
                )
            }
            // Leaving and deleting sit apart from everyday settings, and only the irreversible one is coloured.
            RowGroup(R.string.more_group_account) {
                ListRow(
                    title = stringResource(R.string.settings_sign_out),
                    icon = Icons.AutoMirrored.Outlined.Logout,
                    onClick = onSignOut.takeUnless { state.isLoading },
                )
                ListRow(
                    title = stringResource(R.string.delete_account_action),
                    titleColor = MaterialTheme.colorScheme.error,
                    icon = Icons.Outlined.DeleteOutline,
                    onClick = onDeleteAccount.takeUnless { deleteState.isLoading },
                )
            }
            FormError(state.error)
            FormError(deleteState.error)
            demoDataSection()
        }
      }
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

@Composable
private fun RowGroup(@StringRes title: Int, rows: @Composable ColumnScope.() -> Unit) {
    SectionTitle(stringResource(title), Modifier.padding(start = Spacing.xs, top = Spacing.md))
    AppCard(contentPadding = 0.dp, spacing = 0.dp, content = rows)
}

/** Photo, name, email and how the account signs in. Email/password accounts have no name, so a generic title leads. */
@Composable
private fun ProfileCard(profile: UserProfile) {
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.lg)) {
            Avatar(profile, size = 64.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    profile.name ?: stringResource(R.string.settings_profile_default_title),
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                profile.email?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    stringResource(if (profile.viaGoogle) R.string.settings_signed_in_google else R.string.settings_signed_in_email),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = Spacing.xs),
                )
            }
        }
    }
}
