package com.fundtrail.ui.categories

import com.fundtrail.R
import com.fundtrail.domain.model.SpendKind
import com.fundtrail.domain.model.SyncStatus
import com.fundtrail.domain.usecase.category.ArchiveCategoryUseCase
import com.fundtrail.domain.usecase.category.ObserveActiveCategoriesUseCase
import com.fundtrail.domain.usecase.category.SaveCategoryUseCase
import com.fundtrail.domain.usecase.sync.ObserveSyncStatusUseCase
import com.fundtrail.fakes.FakeCategoryRepository
import com.fundtrail.fakes.FakeSyncStatusRepository
import com.fundtrail.fakes.MainDispatcherRule
import com.fundtrail.ui.common.UiState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class CategoriesViewModelTest {
    @get:Rule val mainDispatcher = MainDispatcherRule()

    private val repo = FakeCategoryRepository()
    private val sync = FakeSyncStatusRepository()

    private fun TestScope.viewModel() = CategoriesViewModel(
        ObserveActiveCategoriesUseCase(repo),
        ObserveSyncStatusUseCase(sync),
        SaveCategoryUseCase(repo),
        ArchiveCategoryUseCase(repo),
    ).also { vm -> backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} } }

    private fun CategoriesViewModel.data() = (uiState.value.content as UiState.Success).data

    @Test
    fun groupsCategoriesByTypeInSortOrder() = runTest {
        val vm = viewModel()

        val data = vm.data()
        assertTrue(data.committed.all { it.defaultSpendKind == SpendKind.COMMITTED })
        assertTrue(data.discretionary.all { it.defaultSpendKind == SpendKind.DISCRETIONARY })
        assertEquals(data.committed.sortedBy { it.sortOrder }, data.committed)
        assertEquals(repo.observeActive().first().size, data.total)
    }

    @Test
    fun showsLoadingUntilTheFirstSyncAndEmptyAfterIt() = runTest {
        repo.observeAll().first().forEach { repo.archive(it.id) }
        sync.status.value = SyncStatus(initialSyncDone = false, isOnline = true)
        val vm = viewModel()
        assertEquals(UiState.Loading, vm.uiState.value.content)

        sync.status.value = SyncStatus(initialSyncDone = true, isOnline = true)
        assertEquals(UiState.Empty, vm.uiState.value.content)
    }

    @Test
    fun filterSelectionIsKept() = runTest {
        val vm = viewModel()
        vm.onFilterSelected(CategoryFilter.COMMITTED)
        assertEquals(CategoryFilter.COMMITTED, vm.uiState.value.filter)
    }

    @Test
    fun tappingARowOpensTheEditorFilledWithItsValues() = runTest {
        val vm = viewModel()
        val rent = vm.data().committed.first { it.id == "cat_rent" }

        vm.onCategoryClick(rent)

        val editor = vm.uiState.value.editor
        assertNotNull(editor)
        assertEquals("cat_rent", editor!!.id)
        assertEquals(rent.name, editor.name)
        assertEquals(SpendKind.COMMITTED, editor.kind)
    }

    @Test
    fun addOpensABlankEditorThatCreatesACategory() = runTest {
        val vm = viewModel()

        vm.onAddClick()
        assertNull(vm.uiState.value.editor!!.id)
        vm.onNameChange("Health")
        vm.onKindChange(SpendKind.DISCRETIONARY)
        vm.onSave()

        assertNull(vm.uiState.value.editor)
        assertEquals(R.string.categories_saved, vm.uiState.value.message)
        assertTrue(vm.data().discretionary.any { it.name == "Health" })
    }

    @Test
    fun changingTheTypeMovesTheCategoryToTheOtherGroup() = runTest {
        val vm = viewModel()
        vm.onCategoryClick(vm.data().committed.first { it.id == "cat_rent" })

        vm.onKindChange(SpendKind.DISCRETIONARY)
        vm.onSave()

        assertFalse(vm.data().committed.any { it.id == "cat_rent" })
        assertTrue(vm.data().discretionary.any { it.id == "cat_rent" })
    }

    @Test
    fun blankNameShowsAnErrorAndKeepsTheSheetOpen() = runTest {
        val vm = viewModel()
        vm.onAddClick()

        vm.onSave()

        assertEquals(R.string.category_name_required, vm.uiState.value.editor!!.nameError)
        vm.onNameChange("x")
        assertNull(vm.uiState.value.editor!!.nameError)
    }

    @Test
    fun duplicateNameIsRejectedIgnoringCaseButTheCategoryMayKeepItsOwnName() = runTest {
        val vm = viewModel()
        vm.onAddClick()
        vm.onNameChange("rent")
        vm.onSave()
        assertEquals(R.string.category_name_duplicate, vm.uiState.value.editor!!.nameError)

        vm.onEditorDismiss()
        vm.onCategoryClick(vm.data().committed.first { it.id == "cat_rent" })
        vm.onSave()
        assertNull(vm.uiState.value.editor)
        assertEquals(R.string.categories_saved, vm.uiState.value.message)
    }

    @Test
    fun archiveRemovesTheCategoryFromTheListAndClosesTheSheet() = runTest {
        val vm = viewModel()
        vm.onCategoryClick(vm.data().committed.first { it.id == "cat_rent" })

        vm.onArchive()

        assertNull(vm.uiState.value.editor)
        assertEquals(R.string.categories_archived, vm.uiState.value.message)
        assertFalse(vm.data().committed.any { it.id == "cat_rent" })
        // Still stored, so past spending keeps its category name.
        assertTrue(repo.observeAll().first().any { it.id == "cat_rent" && it.archived })
    }

    @Test
    fun messageIsClearedOnceShown() = runTest {
        val vm = viewModel()
        vm.onAddClick()
        vm.onNameChange("Health")
        vm.onSave()
        assertNotNull(vm.uiState.value.message)

        vm.onMessageShown()

        assertNull(vm.uiState.value.message)
    }
}
