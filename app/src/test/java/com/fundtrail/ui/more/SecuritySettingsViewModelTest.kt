package com.fundtrail.ui.more

import com.fundtrail.R
import com.fundtrail.domain.usecase.security.ObserveAppLockEnabledUseCase
import com.fundtrail.domain.usecase.security.SetAppLockEnabledUseCase
import com.fundtrail.fakes.FakeAppLockRepository
import com.fundtrail.fakes.FakeDeviceSecurityRepository
import com.fundtrail.fakes.MainDispatcherRule
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SecuritySettingsViewModelTest {
    @get:Rule val mainDispatcher = MainDispatcherRule()

    private val appLock = FakeAppLockRepository()
    private val device = FakeDeviceSecurityRepository()

    private fun TestScope.viewModel() = SecuritySettingsViewModel(
        ObserveAppLockEnabledUseCase(appLock),
        SetAppLockEnabledUseCase(appLock, device),
    ).also { vm -> backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} } }

    @Test
    fun togglingOnEnablesTheLock() = runTest {
        val vm = viewModel()
        vm.onAppLockToggled(true)
        assertTrue(vm.uiState.value.appLockEnabled)
        assertNull(vm.uiState.value.message)
    }

    @Test
    fun withoutAScreenLockTheToggleStaysOffAndSaysWhy() = runTest {
        device.canAuthenticate = false
        val vm = viewModel()

        vm.onAppLockToggled(true)

        assertFalse(vm.uiState.value.appLockEnabled)
        assertEquals(R.string.app_lock_needs_screen_lock, vm.uiState.value.message)
    }

    @Test
    fun theNextToggleClearsTheMessage() = runTest {
        device.canAuthenticate = false
        val vm = viewModel()
        vm.onAppLockToggled(true)

        device.canAuthenticate = true
        vm.onAppLockToggled(true)

        assertTrue(vm.uiState.value.appLockEnabled)
        assertNull(vm.uiState.value.message)
    }
}
