package com.fundtrail.ui.common

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.fundtrail.R
import com.fundtrail.domain.model.SpendKind

/** Small form pieces shared by the categories editor and the quick-add sheet. */

@Composable
fun SectionLabel(@StringRes label: Int) {
    Text(
        stringResource(label).uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/**
 * Marks an entry whose LKR amount is still an estimate (#177). A neutral outline in the variant colour,
 * never red and never an alarm (NFR-14): the figure is honest about itself, not a failure.
 */
@Composable
fun EstimatedChip(modifier: Modifier = Modifier) {
    Text(
        stringResource(R.string.estimated_chip),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp),
    )
}

@Composable
fun KindPicker(selected: SpendKind, enabled: Boolean, onSelected: (SpendKind) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        listOf(
            SpendKind.COMMITTED to R.string.categories_kind_committed,
            SpendKind.DISCRETIONARY to R.string.categories_kind_discretionary,
        ).forEach { (kind, label) ->
            val isSelected = kind == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isSelected) MaterialTheme.colorScheme.surfaceContainerLowest else Color.Transparent)
                    .selectable(selected = isSelected, enabled = enabled, role = Role.RadioButton, onClick = { onSelected(kind) }),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    stringResource(label),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
