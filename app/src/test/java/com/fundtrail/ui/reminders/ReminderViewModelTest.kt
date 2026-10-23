package com.fundtrail.ui.reminders

import com.fundtrail.domain.usecase.reminder.ObserveReminderSettingsUseCase
import com.fundtrail.domain.usecase.reminder.SetReminderEnabledUseCase
import com.fundtrail.domain.usecase.reminder.SetReminderTimeUseCase
import com.fundtrail.fakes.FakeReminderScheduler
import com.fundtrail.fakes.FakeReminderSettingsRepository
import com.fundtrail.fakes.MainDispatcherRule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ReminderViewModelTest {
    @get:Rule val mainDispatcher = MainDispatcherRule()

    private val settings = FakeReminderSettingsRepository()
    private val scheduler = FakeReminderScheduler()

    private fun TestScope.viewModel() = ReminderViewModel(
        ObserveReminderSettingsUseCase(settings),
        SetReminderEnabledUseCase(settings, scheduler),
        SetReminderTimeUseCase(settings, scheduler),
    ).also { vm -> backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} } }

    @Test
    fun grantingEnablesAndSchedules() = runTest {
        val vm = viewModel()
        vm.onEnableGranted()
        assertTrue(settings.observe().first().enabled)
        assertEquals(21 * 60, scheduler.scheduledMinute) // default 21:00
    }

    @Test
    fun deniedPermissionTurnsTheToggleBackOff() = runTest {
        val vm = viewModel()
        vm.onEnableGranted()
        assertTrue(settings.observe().first().enabled)

        vm.onEnableDenied()
        assertFalse(settings.observe().first().enabled)
        assertTrue(vm.uiState.value.permissionDenied)
        assertTrue(scheduler.cancelled)
    }

    @Test
    fun changingTimeWhileEnabledReschedules() = runTest {
        val vm = viewModel()
        vm.onEnableGranted()
        vm.onTimeChange(7, 30)
        assertEquals(7 * 60 + 30, scheduler.scheduledMinute)
        assertEquals(7 * 60 + 30, settings.observe().first().minuteOfDay)
    }
}
