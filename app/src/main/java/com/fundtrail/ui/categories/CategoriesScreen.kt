package com.fundtrail.ui.categories

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.semantics.Role
import com.fundtrail.R
import com.fundtrail.domain.model.Category
import com.fundtrail.domain.model.DefaultCategories
import com.fundtrail.domain.model.SpendKind
import com.fundtrail.ui.common.KindPicker
import com.fundtrail.ui.common.SectionLabel
import com.fundtrail.ui.common.UiState
import com.fundtrail.ui.theme.FundtrailAppTheme

@Composable
fun CategoriesRoute(
    onBack: () -> Unit,
    viewModel: CategoriesViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    CategoriesScreen(
        state = state,
        onBack = onBack,
        onFilterSelected = viewModel::onFilterSelected,
        onAddClick = viewModel::onAddClick,
        onCategoryClick = viewModel::onCategoryClick,
        onEditorDismiss = viewModel::onEditorDismiss,
        onNameChange = viewModel::onNameChange,
        onKindChange = viewModel::onKindChange,
        onIconChange = viewModel::onIconChange,
        onSave = viewModel::onSave,
        onArchive = viewModel::onArchive,
        onMessageShown = viewModel::onMessageShown,
    )
}

@Composable
fun CategoriesScreen(
    state: CategoriesUiState,
    onBack: () -> Unit,
    onFilterSelected: (CategoryFilter) -> Unit,
    onAddClick: () -> Unit,
    onCategoryClick: (Category) -> Unit,
    onEditorDismiss: () -> Unit,
    onNameChange: (String) -> Unit,
    onKindChange: (SpendKind) -> Unit,
    onIconChange: (String) -> Unit,
    onSave: () -> Unit,
    onArchive: () -> Unit,
    onMessageShown: () -> Unit,
) {
    val snackbarHost = remember { SnackbarHostState() }
    val message = state.message?.let { stringResource(it) }
    LaunchedEffect(message) {
        if (message != null) {
            snackbarHost.showSnackbar(message)
            onMessageShown()
        }
    }

    Scaffold(
        // The app-level Scaffold already applies the system bar insets.
        contentWindowInsets = WindowInsets(0),
        snackbarHost = { SnackbarHost(snackbarHost) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            TopRow(onBack)
            Header(onAddClick)
            SnapshotRuleCard()
            when (val content = state.content) {
                UiState.Loading -> LoadingSkeleton()
                UiState.Empty -> MessageText(R.string.categories_empty)
                is UiState.Error -> MessageText(content.message)
                is UiState.Success -> {
                    FilterTabs(state.filter, content.data, onFilterSelected)
                    if (state.filter != CategoryFilter.DISCRETIONARY) {
                        CategorySection(
                            title = R.string.categories_committed_heading,
                            tag = R.string.categories_committed_tag,
                            kind = SpendKind.COMMITTED,
                            categories = content.data.committed,
                            onCategoryClick = onCategoryClick,
                        )
                    }
                    if (state.filter != CategoryFilter.COMMITTED) {
                        CategorySection(
                            title = R.string.categories_discretionary_heading,
                            tag = R.string.categories_discretionary_tag,
                            kind = SpendKind.DISCRETIONARY,
                            categories = content.data.discretionary,
                            onCategoryClick = onCategoryClick,
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }

    state.editor?.let { editor ->
        CategoryEditorSheet(editor, onEditorDismiss, onNameChange, onKindChange, onIconChange, onSave, onArchive)
    }
}

@Composable
private fun TopRow(onBack: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.categories_back))
        }
        Text(stringResource(R.string.categories_title), style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun Header(onAddClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f)) {
            Text(
                stringResource(R.string.categories_eyebrow).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary,
            )
            Text(stringResource(R.string.categories_heading), style = MaterialTheme.typography.headlineSmall)
        }
        Button(onClick = onAddClick, contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(4.dp))
            Text(stringResource(R.string.categories_add))
        }
    }
}

/** Explains the snapshot rule: a category's type only affects entries saved after the change. */
@Composable
private fun SnapshotRuleCard() {
    Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(
                Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(stringResource(R.string.categories_snapshot_title), style = MaterialTheme.typography.labelLarge)
                Text(
                    stringResource(R.string.categories_snapshot_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun FilterTabs(selected: CategoryFilter, data: CategoriesData, onSelected: (CategoryFilter) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(4.dp),
    ) {
        FilterTab(
            label = stringResource(R.string.categories_filter_all, data.total),
            selected = selected == CategoryFilter.ALL,
            onClick = { onSelected(CategoryFilter.ALL) },
            modifier = Modifier.weight(1f),
        )
        FilterTab(
            label = stringResource(R.string.categories_filter_committed, data.committed.size),
            selected = selected == CategoryFilter.COMMITTED,
            onClick = { onSelected(CategoryFilter.COMMITTED) },
            modifier = Modifier.weight(1f),
        )
        FilterTab(
            label = stringResource(R.string.categories_filter_discretionary, data.discretionary.size),
            selected = selected == CategoryFilter.DISCRETIONARY,
            onClick = { onSelected(CategoryFilter.DISCRETIONARY) },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun FilterTab(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val background = if (selected) MaterialTheme.colorScheme.surfaceContainerLowest else Color.Transparent
    val content = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        modifier = modifier
            .height(40.dp)
            .clip(CircleShape)
            .background(background)
            .selectable(selected = selected, role = Role.Tab, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = content, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun CategorySection(
    @StringRes title: Int,
    @StringRes tag: Int,
    kind: SpendKind,
    categories: List<Category>,
    onCategoryClick: (Category) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(kindDotColor(kind)))
            Text(stringResource(title), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            KindChip(stringResource(tag), kind)
        }
        categories.forEach { category -> CategoryRow(category, onClick = { onCategoryClick(category) }) }
    }
}

@Composable
private fun CategoryRow(category: Category, onClick: () -> Unit) {
    val description = categoryDescriptionRes(category.id)?.let { stringResource(it) }
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
            KindIcon(category.icon, category.defaultSpendKind)
            Column(Modifier.weight(1f)) {
                Text(category.name, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (description != null) {
                    Text(
                        description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            KindChip(
                stringResource(
                    if (category.defaultSpendKind == SpendKind.COMMITTED) R.string.categories_kind_committed
                    else R.string.categories_kind_discretionary,
                ),
                category.defaultSpendKind,
            )
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outlineVariant,
            )
        }
    }
}

@Composable
private fun KindIcon(iconKey: String, kind: SpendKind, size: Int = 40) {
    Box(
        modifier = Modifier.size(size.dp).clip(CircleShape).background(kindContainerColor(kind)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(CategoryIcons.vectorFor(iconKey), contentDescription = null, tint = kindContentColor(kind), modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun KindChip(label: String, kind: SpendKind) {
    Surface(shape = CircleShape, color = kindContainerColor(kind)) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = kindContentColor(kind),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
            maxLines = 1,
        )
    }
}

// Committed uses the green secondary container, discretionary the neutral surface variant (same pairing as the dashboard split).
@Composable
private fun kindContainerColor(kind: SpendKind): Color =
    if (kind == SpendKind.COMMITTED) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant

@Composable
private fun kindContentColor(kind: SpendKind): Color =
    if (kind == SpendKind.COMMITTED) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant

@Composable
private fun kindDotColor(kind: SpendKind): Color =
    if (kind == SpendKind.COMMITTED) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary

@Composable
private fun LoadingSkeleton() {
    val description = stringResource(R.string.categories_loading)
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.semantics { contentDescription = description },
    ) {
        repeat(6) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            )
        }
    }
}

@Composable
private fun MessageText(@StringRes message: Int) {
    Text(
        stringResource(message),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = 24.dp),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryEditorSheet(
    editor: CategoryEditorState,
    onDismiss: () -> Unit,
    onNameChange: (String) -> Unit,
    onKindChange: (SpendKind) -> Unit,
    onIconChange: (String) -> Unit,
    onSave: () -> Unit,
    onArchive: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                KindIcon(editor.icon, editor.kind, size = 44)
                Text(
                    stringResource(if (editor.id == null) R.string.category_new_title else R.string.category_edit_title),
                    style = MaterialTheme.typography.titleLarge,
                )
            }

            OutlinedTextField(
                value = editor.name,
                onValueChange = onNameChange,
                label = { Text(stringResource(R.string.category_name_label)) },
                placeholder = { Text(stringResource(R.string.category_name_placeholder)) },
                isError = editor.nameError != null,
                supportingText = editor.nameError?.let { { Text(stringResource(it)) } },
                singleLine = true,
                enabled = !editor.isSaving,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidth(),
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionLabel(R.string.category_type_label)
                KindPicker(editor.kind, enabled = !editor.isSaving, onKindChange)
                if (editor.id != null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            stringResource(R.string.category_snapshot_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionLabel(R.string.category_icon_label)
                IconPicker(selected = editor.icon, kind = editor.kind, onSelected = onIconChange)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = onDismiss, enabled = !editor.isSaving, modifier = Modifier.weight(1f).height(48.dp)) {
                    Text(stringResource(R.string.category_cancel))
                }
                Button(onClick = onSave, enabled = !editor.isSaving, modifier = Modifier.weight(1f).height(48.dp)) {
                    Text(stringResource(R.string.category_save))
                }
            }
            if (editor.id != null) {
                TextButton(
                    onClick = onArchive,
                    enabled = !editor.isSaving,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                ) {
                    Text(stringResource(R.string.category_archive), color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun IconPicker(selected: String, kind: SpendKind, onSelected: (String) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(vertical = 4.dp)) {
        items(CategoryIcons.keys) { key ->
            val isSelected = key == selected
            Box(
                modifier = Modifier
                    .size(48.dp) // 48dp touch target
                    .clip(CircleShape)
                    .background(if (isSelected) kindContainerColor(kind) else MaterialTheme.colorScheme.surfaceContainerHigh)
                    .selectable(selected = isSelected, role = Role.RadioButton, onClick = { onSelected(key) }),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    CategoryIcons.vectorFor(key),
                    contentDescription = null,
                    tint = if (isSelected) kindContentColor(kind) else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CategoriesScreenPreview() {
    FundtrailAppTheme {
        CategoriesScreen(
            state = CategoriesUiState(
                content = UiState.Success(
                    CategoriesData(DefaultCategories.committed, DefaultCategories.discretionary),
                ),
            ),
            onBack = {},
            onFilterSelected = {},
            onAddClick = {},
            onCategoryClick = {},
            onEditorDismiss = {},
            onNameChange = {},
            onKindChange = {},
            onIconChange = {},
            onSave = {},
            onArchive = {},
            onMessageShown = {},
        )
    }
}
