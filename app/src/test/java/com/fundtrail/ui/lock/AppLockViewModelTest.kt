package com.fundtrail.ui.lock

import com.fundtrail.domain.usecase.security.ObserveAppLockEnabledUseCase
import com.fundtrail.fakes.FakeAppLockRepository
import com.fundtrail.fakes.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

class AppLockViewModelTest {
    @get:Rule val mainDispatcher = MainDispatcherRule()

    private val appLock = FakeAppLockRepository()

    /** A clock the test moves by hand. */
    private class MutableClock(var now: Long = 0L) : Clock() {
        override fun getZone(): ZoneId = ZoneOffset.UTC
        override fun withZone(zone: ZoneId?): Clock = this
        override fun instant(): Instant = Instant.ofEpochMilli(now)
    }

    private val clock = MutableClock()

    private fun viewModel() = AppLockViewModel(ObserveAppLockEnabledUseCase(appLock), clock)

    private fun AppLockViewModel.backgroundFor(millis: Long) {
        onBackgrounded()
        clock.now += millis
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
    fun aShortTripToTheBackgroundDoesNotLock() = runTest {
        appLock.enabled.value = true
        val vm = viewModel().also { it.onUnlocked() }

        vm.backgroundFor(AppLockViewModel.LOCK_AFTER_MILLIS)

        assertFalse(vm.uiState.value.locked)
    }

    @Test
    fun moreThanSixtySecondsInTheBackgroundLocks() = runTest {
        appLock.enabled.value = true
        val vm = viewModel().also { it.onUnlocked() }

        vm.backgroundFor(2 * 60_000L)

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
        vm.backgroundFor(10 * 60_000L)
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
}
