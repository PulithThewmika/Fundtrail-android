package com.fundtrail.ui.lock

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.fundtrail.R

/**
 * Full-screen cover shown while the app is locked (#222). Opaque and swallowing every touch, so nothing behind it
 * is visible or reachable. [onUnlock] shows the system prompt again after a cancel.
 */
@Composable
fun AppLockScreen(onUnlock: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier
            .fillMaxSize()
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {}),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                Icons.Outlined.Lock,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                stringResource(R.string.app_lock_locked_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                stringResource(R.string.app_lock_locked_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(onClick = onUnlock) { Text(stringResource(R.string.app_lock_unlock)) }
        }
    }
}
