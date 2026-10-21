package com.fundtrail.ui.auth

import com.fundtrail.R
import com.fundtrail.domain.model.EmailUsesPasswordSignInException
import com.fundtrail.domain.model.Result
import com.fundtrail.domain.usecase.auth.SignInWithGoogleUseCase
import com.fundtrail.fakes.FakeAuthRepository
import com.fundtrail.fakes.FakeLocalDataRepository
import com.fundtrail.fakes.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class GoogleSignInViewModelTest {
    @get:Rule val mainDispatcher = MainDispatcherRule()

    private val auth = FakeAuthRepository()
    private val vm = GoogleSignInViewModel(SignInWithGoogleUseCase(auth, FakeLocalDataRepository(auth.calls)))

    @Test
    fun aTokenSignsIn() = runTest {
        assertTrue(vm.onPickerStarted())
        vm.onPickerResult(GoogleIdTokenResult.Token("t"))

        assertTrue(vm.uiState.value.signedIn)
        assertFalse(vm.uiState.value.isLoading)
        assertTrue("google:t" in auth.calls)
    }

    @Test
    fun cancellingReturnsToTheFormWithNoError() = runTest {
        vm.onPickerStarted()
        vm.onPickerResult(GoogleIdTokenResult.Cancelled)

        assertEquals(GoogleSignInState(), vm.uiState.value)
    }

    @Test
    fun anEmailWithAPasswordAccountSaysSo() = runTest {
        auth.result = Result.Error(EmailUsesPasswordSignInException())
        vm.onPickerStarted()
        vm.onPickerResult(GoogleIdTokenResult.Token("t"))

        assertEquals(R.string.auth_google_email_uses_password, vm.uiState.value.error)
        assertFalse(vm.uiState.value.signedIn)
    }

    @Test
    fun noAccountAndNotConfiguredExplainThemselves() = runTest {
        vm.onPickerStarted()
        vm.onPickerResult(GoogleIdTokenResult.NoAccount)
        assertEquals(R.string.auth_google_no_account, vm.uiState.value.error)

        vm.onPickerStarted()
        assertNull(vm.uiState.value.error)
        vm.onPickerResult(GoogleIdTokenResult.NotConfigured)
        assertEquals(R.string.auth_google_not_configured, vm.uiState.value.error)
    }

    @Test
    fun aSecondTapWhileSigningInDoesNotOpenAnotherPicker() = runTest {
        assertTrue(vm.onPickerStarted())
        assertFalse(vm.onPickerStarted())
    }

    @Test
    fun navigationClearsTheOneOffFlag() = runTest {
        vm.onPickerStarted()
        vm.onPickerResult(GoogleIdTokenResult.Token("t"))
        vm.onNavigated()
        assertFalse(vm.uiState.value.signedIn)
    }
}
