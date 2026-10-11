package com.fundtrail.ui.auth

import com.fundtrail.R
import com.fundtrail.domain.model.Result
import com.fundtrail.domain.usecase.auth.SignInUseCase
import com.fundtrail.fakes.FakeAuthRepository
import com.fundtrail.fakes.FakeLocalDataRepository
import com.fundtrail.fakes.MainDispatcherRule
import com.google.firebase.FirebaseNetworkException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SignInViewModelTest {
    @get:Rule val mainDispatcher = MainDispatcherRule()

    private val auth = FakeAuthRepository()
    private val vm = SignInViewModel(SignInUseCase(auth, FakeLocalDataRepository(auth.calls)))

    @Test
    fun emptyFieldsShowInlineErrorsWithoutCallingAuth() {
        vm.onSubmit()
        val s = vm.uiState.value
        assertEquals(R.string.auth_error_email_required, s.emailError)
        assertEquals(R.string.auth_error_password_required, s.passwordError)
        assertTrue(auth.calls.isEmpty())
    }

    @Test
    fun malformedEmailIsRejected() {
        vm.onEmailChange("not-an-email")
        vm.onPasswordChange("secret1")
        vm.onSubmit()
        assertEquals(R.string.auth_error_email_invalid, vm.uiState.value.emailError)
        assertTrue(auth.calls.isEmpty())
    }

    @Test
    fun successSetsSignedInOnce() {
        vm.onEmailChange("a@b.com")
        vm.onPasswordChange("secret1")
        vm.onSubmit()
        assertTrue(vm.uiState.value.signedIn)
        assertFalse(vm.uiState.value.isLoading)
        vm.onNavigated()
        assertFalse(vm.uiState.value.signedIn)
    }

    @Test
    fun failureShowsMappedErrorAndKeepsEmail() {
        auth.result = Result.Error(FirebaseNetworkException("offline"))
        vm.onEmailChange("a@b.com")
        vm.onPasswordChange("wrong")
        vm.onSubmit()
        val s = vm.uiState.value
        assertEquals(R.string.error_network, s.error)
        assertEquals("a@b.com", s.email)
        assertFalse(s.isLoading)
        assertFalse(s.signedIn)
    }

    @Test
    fun editingClearsError() {
        auth.result = Result.Error(RuntimeException())
        vm.onEmailChange("a@b.com")
        vm.onPasswordChange("x")
        vm.onSubmit()
        vm.onPasswordChange("y")
        assertNull(vm.uiState.value.error)
    }
}
