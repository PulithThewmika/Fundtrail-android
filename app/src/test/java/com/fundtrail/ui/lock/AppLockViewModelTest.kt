package com.fundtrail.ui.lock

import com.fundtrail.domain.usecase.security.ObserveAppLockEnabledUseCase
import com.fundtrail.domain.usecase.security.SetAppLockEnabledUseCase
import com.fundtrail.fakes.FakeAppLockRepository
import com.fundtrail.fakes.FakeDeviceSecurityRepository
import com.fundtrail.fakes.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AppLockViewModelTest {
    @get:Rule val mainDispatcher = MainDispatcherRule()

    private val appLock = FakeAppLockRepository()

    private val device = FakeDeviceSecurityRepository()

    private fun viewModel() = AppLockViewModel(
        ObserveAppLockEnabledUseCase(appLock),
        SetAppLockEnabledUseCase(appLock, device),
    )

    private fun AppLockViewModel.background() {
        onBackgrounded()
        onForegrounded()
    }

    @Test
    fun coldStartWithTheLockOnIsLocked() = runTest {
        appLock.enabled.value = true
        val state = viewModel().uiState.value
        assertTrue(state.resolved)
        assertTrue(state.locked)
        assertTrue(state.enabled)
    }

    @Test
    fun coldStartWithTheLockOffIsOpen() = runTest {
        val state = viewModel().uiState.value
        assertTrue(state.resolved)
        assertFalse(state.locked)
        assertFalse(state.enabled)
    }

    @Test
    fun unlockingOpensTheApp() = runTest {
        appLock.enabled.value = true
        val vm = viewModel()
        vm.onUnlocked()
        assertFalse(vm.uiState.value.locked)
    }

    @Test
    fun anyTripToTheBackgroundLocks() = runTest {
        // #322: no grace period - the in-app copy promises a prompt "when FundTrail opens", every time.
        appLock.enabled.value = true
        val vm = viewModel().also { it.onUnlocked() }

        vm.background()

        assertTrue(vm.uiState.value.locked)
    }

    @Test
    fun turningTheLockOnMidSessionDoesNotLockStraightAway() = runTest {
        val vm = viewModel()
        appLock.enabled.value = true
        assertFalse(vm.uiState.value.locked)
        assertTrue(vm.uiState.value.enabled)
    }

    @Test
    fun withTheLockOffTheBackgroundNeverLocks_evenAfterTurningItOnLater() = runTest {
        val vm = viewModel()
        vm.background()
        appLock.enabled.value = true
        assertFalse(vm.uiState.value.locked)
    }

    @Test
    fun turningTheLockOffWhileLockedOpensTheApp() = runTest {
        appLock.enabled.value = true
        val vm = viewModel()
        appLock.enabled.value = false
        assertFalse(vm.uiState.value.locked)
    }

    @Test
    fun losingTheDeviceCredentialTurnsTheLockOffInsteadOfTrappingTheUser() = runTest {
        appLock.enabled.value = true
        device.canAuthenticate = false
        val vm = viewModel()
        assertTrue(vm.uiState.value.locked)

        vm.onAuthenticationUnavailable()

        assertFalse(appLock.enabled.value)
        assertFalse(vm.uiState.value.locked)
        assertFalse(vm.uiState.value.enabled)
    }
}
