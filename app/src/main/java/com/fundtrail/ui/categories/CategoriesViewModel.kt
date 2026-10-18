package com.fundtrail.ui.categories

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fundtrail.R
import com.fundtrail.domain.model.Category
import com.fundtrail.domain.model.DuplicateCategoryNameException
import com.fundtrail.domain.model.Result
import com.fundtrail.domain.model.SpendKind
import com.fundtrail.domain.usecase.category.ArchiveCategoryUseCase
import com.fundtrail.domain.usecase.category.ObserveActiveCategoriesUseCase
import com.fundtrail.domain.usecase.category.SaveCategoryUseCase
import com.fundtrail.domain.usecase.sync.ObserveSyncStatusUseCase
import com.fundtrail.ui.common.UiState
import com.fundtrail.ui.common.mapErrorToMessage
import com.fundtrail.ui.common.uiStateOf
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Which group the segmented control shows (SCR-53). */
enum class CategoryFilter { ALL, COMMITTED, DISCRETIONARY }

/** Both lists are in `sortOrder`, so the seeded order is kept. */
data class CategoriesData(
    val committed: List<Category>,
    val discretionary: List<Category>,
) {
    val total: Int get() = committed.size + discretionary.size
}

/** The add/edit sheet (DLG-08). [id] is null while creating. */
data class CategoryEditorState(
    val id: String?,
    val name: String,
    val kind: SpendKind,
    val icon: String,
    @StringRes val nameError: Int? = null,
    val isSaving: Boolean = false,
)

data class CategoriesUiState(
    val content: UiState<CategoriesData> = UiState.Loading,
    val filter: CategoryFilter = CategoryFilter.ALL,
    /** Non-null while the sheet is open. */
    val editor: CategoryEditorState? = null,
    /** One-off snackbar text; the UI shows it, then calls [CategoriesViewModel.onMessageShown]. */
    @StringRes val message: Int? = null,
)

@HiltViewModel
class CategoriesViewModel @Inject constructor(
    observeActive: ObserveActiveCategoriesUseCase,
    observeSyncStatus: ObserveSyncStatusUseCase,
    private val saveCategory: SaveCategoryUseCase,
    private val archiveCategory: ArchiveCategoryUseCase,
) : ViewModel() {

    private val local = MutableStateFlow(CategoriesUiState())

    val uiState: StateFlow<CategoriesUiState> = combine(
        local,
        observeActive().combine(observeSyncStatus()) { categories, sync ->
            uiStateOf(categories.isNotEmpty(), sync) {
                CategoriesData(
                    committed = categories.filter { it.defaultSpendKind == SpendKind.COMMITTED },
                    discretionary = categories.filter { it.defaultSpendKind == SpendKind.DISCRETIONARY },
                )
            }
        }.catch { emit(UiState.Error(mapErrorToMessage(it))) },
    ) { state, content -> state.copy(content = content) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CategoriesUiState())

    fun onFilterSelected(filter: CategoryFilter) = local.update { it.copy(filter = filter) }

    fun onAddClick() = local.update {
        it.copy(editor = CategoryEditorState(id = null, name = "", kind = SpendKind.COMMITTED, icon = DEFAULT_ICON))
    }

    fun onCategoryClick(category: Category) = local.update {
        it.copy(editor = CategoryEditorState(category.id, category.name, category.defaultSpendKind, category.icon))
    }

    fun onEditorDismiss() = local.update { it.copy(editor = null) }

    fun onNameChange(value: String) = updateEditor { it.copy(name = value, nameError = null) }

    fun onKindChange(kind: SpendKind) = updateEditor { it.copy(kind = kind) }

    fun onIconChange(icon: String) = updateEditor { it.copy(icon = icon) }

    fun onSave() {
        val editor = local.value.editor ?: return
        if (editor.isSaving) return
        val name = editor.name.trim()
        if (name.isEmpty()) {
            updateEditor { it.copy(nameError = R.string.category_name_required) }
            return
        }
        updateEditor { it.copy(isSaving = true) }
        viewModelScope.launch {
            when (val result = saveCategory(editor.id, name, editor.kind, editor.icon)) {
                is Result.Success -> local.update { it.copy(editor = null, message = R.string.categories_saved) }
                is Result.Error -> local.update {
                    if (result.exception is DuplicateCategoryNameException) {
                        it.copy(editor = it.editor?.copy(isSaving = false, nameError = R.string.category_name_duplicate))
                    } else {
                        it.copy(editor = it.editor?.copy(isSaving = false), message = mapErrorToMessage(result.exception))
                    }
                }
            }
        }
    }

    fun onArchive() {
        val editor = local.value.editor ?: return
        val id = editor.id ?: return
        if (editor.isSaving) return
        updateEditor { it.copy(isSaving = true) }
        viewModelScope.launch {
            when (val result = archiveCategory(id)) {
                is Result.Success -> local.update { it.copy(editor = null, message = R.string.categories_archived) }
                is Result.Error -> local.update {
                    it.copy(editor = it.editor?.copy(isSaving = false), message = mapErrorToMessage(result.exception))
                }
            }
        }
    }

    fun onMessageShown() = local.update { it.copy(message = null) }

    private fun updateEditor(transform: (CategoryEditorState) -> CategoryEditorState) =
        local.update { state -> state.copy(editor = state.editor?.let(transform)) }

    companion object {
        const val DEFAULT_ICON = "payments"
    }
}
