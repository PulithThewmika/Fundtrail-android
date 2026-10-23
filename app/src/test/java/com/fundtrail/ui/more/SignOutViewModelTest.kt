package com.fundtrail.ui.more

import com.fundtrail.domain.usecase.auth.SignOutUseCase
import com.fundtrail.domain.usecase.reminder.CancelReminderUseCase
import com.fundtrail.fakes.FakeAuthRepository
import com.fundtrail.fakes.FakeReminderScheduler
import com.fundtrail.fakes.FakeReminderSettingsRepository
import com.fundtrail.fakes.FakeLocalDataRepository
import com.fundtrail.fakes.FakeSyncRepository
import com.fundtrail.fakes.MainDispatcherRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SignOutViewModelTest {
    @get:Rule val mainDispatcher = MainDispatcherRule()

    private val auth = FakeAuthRepository()
    private val local = FakeLocalDataRepository(auth.calls)
    private val reminderSettings = FakeReminderSettingsRepository()
    private val reminderScheduler = FakeReminderScheduler()
    private val vm = SignOutViewModel(
        SignOutUseCase(auth, local, FakeSyncRepository(auth.calls)),
        CancelReminderUseCase(reminderSettings, reminderScheduler),
    )

    @Test
    fun cleanSignOutNavigates() {
        vm.onSignOutClick()
        assertTrue(vm.uiState.value.signedOut)
        assertEquals(listOf("pauseSync", "signOut", "clear"), auth.calls)
        assertTrue(reminderScheduler.cancelled)
    }

    @Test
    fun unsyncedWritesAskForConfirmationThenForceSignOut() {
        local.pendingWritesDone = false
        vm.onSignOutClick()
        assertTrue(vm.uiState.value.confirmUnsynced)
        assertFalse(vm.uiState.value.signedOut)
        assertTrue(auth.calls.isEmpty())

        vm.onConfirmSignOut()
        assertTrue(vm.uiState.value.signedOut)
        assertFalse(vm.uiState.value.confirmUnsynced)
    }
}
