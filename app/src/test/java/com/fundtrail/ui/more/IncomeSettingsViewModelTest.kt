package com.fundtrail.ui.more

import com.fundtrail.R
import com.fundtrail.domain.model.Result
import com.fundtrail.domain.model.UserSettings
import com.fundtrail.domain.repository.SettingsRepository
import com.fundtrail.fakes.FakeSettingsRepository
import com.fundtrail.domain.usecase.settings.ObserveSettingsUseCase
import com.fundtrail.domain.usecase.settings.UpdateIncomeEstimateUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class IncomeSettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var settingsRepository: FakeSettingsRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        settingsRepository = FakeSettingsRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_loadsFromRepository() = runTest {
        settingsRepository.update(
            UserSettings(
                incomeEstimateMinMinor = 16_000_000L,
                incomeEstimateMaxMinor = 21_000_000L,
            )
        )

        val vm = viewModel(settingsRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals("160000", state.lowEstimateInput)
        assertEquals("210000", state.highEstimateInput)
    }

    @Test
    fun onSave_validationErrorWhenMinGreaterThanMax() = runTest {
        val vm = viewModel(settingsRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        vm.onLowEstimateChanged("220000")
        vm.onHighEstimateChanged("160000")
        vm.onSave()

        val state = vm.uiState.value
        assertEquals(R.string.income_estimate_error_min_greater_than_max, state.errorResId)
    }

    @Test
    fun onSave_validationErrorWhenInvalidNumber() = runTest {
        val vm = viewModel(settingsRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        vm.onLowEstimateChanged("abc")
        vm.onHighEstimateChanged("160000")
        vm.onSave()

        val state = vm.uiState.value
        assertEquals(R.string.income_estimate_error_invalid_number, state.errorResId)
    }

    @Test
    fun onSave_validMinAndMax_savesToRepository() = runTest {
        val vm = viewModel(settingsRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        vm.onLowEstimateChanged("160000")
        vm.onHighEstimateChanged("210000")
        vm.onSave()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue(state.isSaved)

        val updatedSettings = settingsRepository.observe().first()
        assertEquals(16_000_000L, updatedSettings.incomeEstimateMinMinor)
        assertEquals(21_000_000L, updatedSettings.incomeEstimateMaxMinor)
    }

    @Test
    fun onSave_clearingEstimates_savesNulls() = runTest {
        settingsRepository.update(
            UserSettings(
                incomeEstimateMinMinor = 16_000_000L,
                incomeEstimateMaxMinor = 21_000_000L,
            )
        )
        val vm = viewModel(settingsRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        vm.onLowEstimateChanged("")
        vm.onHighEstimateChanged("")
        vm.onSave()
        testDispatcher.scheduler.advanceUntilIdle()

        val updatedSettings = settingsRepository.observe().first()
        assertNull(updatedSettings.incomeEstimateMinMinor)
        assertNull(updatedSettings.incomeEstimateMaxMinor)
    }

    @Test
    fun onSave_whenRepositoryUpdateFails_updatesStateWithErrorGeneric() = runTest {
        val failingRepo = object : SettingsRepository {
            override fun observe() = flowOf(UserSettings())
            override suspend fun update(settings: UserSettings): Result<Unit> = Result.Error(Exception("DB error"))
        }
        val vm = viewModel(failingRepo)
        testDispatcher.scheduler.advanceUntilIdle()

        vm.onLowEstimateChanged("160000")
        vm.onHighEstimateChanged("210000")
        vm.onSave()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertFalse(state.isSaved)
        assertEquals(R.string.error_generic, state.errorResId)
    }

    @Test
    fun parseLkrToMinor_appliesHalfUpRoundingAndHandlesOverflow() {
        assertEquals(10056L, IncomeSettingsViewModel.parseLkrToMinor("100.555"))
        assertEquals(10055L, IncomeSettingsViewModel.parseLkrToMinor("100.554"))
        assertEquals(10000L, IncomeSettingsViewModel.parseLkrToMinor("100"))
        assertNull(IncomeSettingsViewModel.parseLkrToMinor("9999999999999999999999999999999999"))
        assertNull(IncomeSettingsViewModel.parseLkrToMinor("-10"))
        assertNull(IncomeSettingsViewModel.parseLkrToMinor("abc"))
        assertNull(IncomeSettingsViewModel.parseLkrToMinor(" "))
    }

    @Test
    fun onSave_readsCurrentSettingsBeforeUpdating_estimateFieldsOnly() = runTest {
        settingsRepository.update(
            UserSettings(
                expectedSalaryMinor = 20_000_000L,
                salaryPayday = 15,
            ),
        )
        val vm = viewModel(settingsRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        settingsRepository.update(
            UserSettings(
                expectedSalaryMinor = 25_000_000L,
                salaryPayday = 20,
            ),
        )
        vm.onLowEstimateChanged("160000")
        vm.onHighEstimateChanged("210000")
        vm.onSave()
        testDispatcher.scheduler.advanceUntilIdle()

        val updatedSettings = settingsRepository.observe().first()
        assertEquals(16_000_000L, updatedSettings.incomeEstimateMinMinor)
        assertEquals(21_000_000L, updatedSettings.incomeEstimateMaxMinor)
        assertEquals(25_000_000L, updatedSettings.expectedSalaryMinor)
        assertEquals(20, updatedSettings.salaryPayday)
    }

    @Test
    fun onDismissed_restoresPersistedSettingsAndClearsErrors() = runTest {
        settingsRepository.update(
            UserSettings(
                incomeEstimateMinMinor = 16_000_000L,
                incomeEstimateMaxMinor = 21_000_000L,
            ),
        )
        val vm = viewModel(settingsRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        vm.onLowEstimateChanged("invalid")
        vm.onHighEstimateChanged("999999")
        vm.onSave()

        assertEquals(R.string.income_estimate_error_invalid_number, vm.uiState.value.errorResId)

        vm.onDismissed()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals("160000", state.lowEstimateInput)
        assertEquals("210000", state.highEstimateInput)
        assertNull(state.errorResId)
    }

    private fun viewModel(repository: SettingsRepository) = IncomeSettingsViewModel(
        observeSettings = ObserveSettingsUseCase(repository),
        updateIncomeEstimate = UpdateIncomeEstimateUseCase(repository),
    )
}
