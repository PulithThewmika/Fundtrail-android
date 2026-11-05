package com.fundtrail.ui.more

import com.fundtrail.domain.model.Result
import com.fundtrail.domain.usecase.auth.DeleteAccountUseCase
import com.fundtrail.fakes.FakeAppLockRepository
import com.fundtrail.fakes.FakeAuthRepository
import com.fundtrail.fakes.FakeLocalDataRepository
import com.fundtrail.fakes.FakeSyncRepository
import com.fundtrail.fakes.MainDispatcherRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** FR-117. */
class DeleteAccountViewModelTest {
    @get:Rule val mainDispatcher = MainDispatcherRule()

    private val auth = FakeAuthRepository()
    private val local = FakeLocalDataRepository(auth.calls)
    private val vm = DeleteAccountViewModel(DeleteAccountUseCase(auth, local, FakeSyncRepository(auth.calls), FakeAppLockRepository(auth.calls)))

    @Test
    fun deleteClickAsksForConfirmationFirst() {
        vm.onDeleteClick()
        assertTrue(vm.uiState.value.confirming)
        assertTrue(auth.calls.isEmpty())
    }

    @Test
    fun confirmingDeletesAndNavigates() {
        vm.onDeleteClick()
        vm.onConfirmDelete()
        assertTrue(vm.uiState.value.deleted)
        assertEquals(listOf("pauseSync", "deleteAccount", "clear", "appLock=false"), auth.calls)
    }

    @Test
    fun aFailedDeleteReportsAnErrorAndDoesNotNavigate() {
        auth.result = Result.Error(RuntimeException("requires recent login"))
        vm.onDeleteClick()
        vm.onConfirmDelete()
        assertFalse(vm.uiState.value.deleted)
        assertNotNull(vm.uiState.value.error)
    }

    @Test
    fun dismissingConfirmationDoesNotDelete() {
        vm.onDeleteClick()
        vm.onDismissConfirm()
        assertFalse(vm.uiState.value.confirming)
        assertTrue(auth.calls.isEmpty())
    }
}
