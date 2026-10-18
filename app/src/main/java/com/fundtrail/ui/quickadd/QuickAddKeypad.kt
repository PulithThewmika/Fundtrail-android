package com.fundtrail.ui.quickadd

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.fundtrail.R
import com.fundtrail.ui.theme.FundtrailAppTheme

/**
 * Custom keypad (0–9, 00, ., backspace) for fast, effortless amount entry (NFR-01, S1).
 */
@Composable
fun QuickAddKeypad(
    onDigitClick: (String) -> Unit,
    onDotClick: () -> Unit,
    onBackspaceClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    firstKeyFocusRequester: FocusRequester? = null,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val rows = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
        )

        rows.forEach { rowKeys ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                rowKeys.forEach { key ->
                    val buttonModifier = if (key == "1" && firstKeyFocusRequester != null) {
                        Modifier.weight(1f).focusRequester(firstKeyFocusRequester)
                    } else {
                        Modifier.weight(1f)
                    }
                    KeypadButton(
                        text = key,
                        onClick = { onDigitClick(key) },
                        enabled = enabled,
                        modifier = buttonModifier,
                    )
                }
            }
        }

        // Bottom row: '.', '0', '00', and Backspace
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            KeypadButton(
                text = ".",
                onClick = onDotClick,
                enabled = enabled,
                modifier = Modifier.weight(1f),
            )
            KeypadButton(
                text = "0",
                onClick = { onDigitClick("0") },
                enabled = enabled,
                modifier = Modifier.weight(1f),
            )
            KeypadButton(
                text = "00",
                onClick = { onDigitClick("00") },
                enabled = enabled,
                modifier = Modifier.weight(1f),
            )
            KeypadIconButton(
                onClick = onBackspaceClick,
                enabled = enabled,
                contentDescription = stringResource(R.string.quick_add_keypad_backspace),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun KeypadButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val contentColor = MaterialTheme.colorScheme.onSurface

    Box(
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (enabled) containerColor else containerColor.copy(alpha = 0.5f))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
            color = if (enabled) contentColor else contentColor.copy(alpha = 0.5f),
        )
    }
}

@Composable
private fun KeypadIconButton(
    onClick: () -> Unit,
    enabled: Boolean,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    val containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val contentColor = MaterialTheme.colorScheme.onSurface

    Box(
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (enabled) containerColor else containerColor.copy(alpha = 0.5f))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = contentDescription,
            tint = if (enabled) contentColor else contentColor.copy(alpha = 0.5f),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun QuickAddKeypadPreview() {
    FundtrailAppTheme {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(16.dp),
        ) {
            Text("Enabled", style = MaterialTheme.typography.titleMedium)
            QuickAddKeypad(
                onDigitClick = {},
                onDotClick = {},
                onBackspaceClick = {},
                enabled = true,
            )
            Text("Disabled", style = MaterialTheme.typography.titleMedium)
            QuickAddKeypad(
                onDigitClick = {},
                onDotClick = {},
                onBackspaceClick = {},
                enabled = false,
            )
        }
    }
}
