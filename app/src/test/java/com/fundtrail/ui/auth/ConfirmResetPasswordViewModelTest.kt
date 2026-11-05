package com.fundtrail.ui.auth

import com.fundtrail.R
import com.fundtrail.domain.model.Result
import com.fundtrail.domain.usecase.auth.ConfirmPasswordResetUseCase
import com.fundtrail.fakes.FakeAuthRepository
import com.fundtrail.fakes.MainDispatcherRule
import com.google.firebase.auth.FirebaseAuthActionCodeException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ConfirmResetPasswordViewModelTest {
    @get:Rule val mainDispatcher = MainDispatcherRule()

    private val auth = FakeAuthRepository()
    private val vm = ConfirmResetPasswordViewModel(ConfirmPasswordResetUseCase(auth))

    @Test
    fun shortPasswordIsRejectedWithoutCallingAuth() {
        vm.onPasswordChange("short")
        vm.onConfirmChange("short")
        vm.onSubmit("code-1")
        assertEquals(R.string.auth_error_password_short, vm.uiState.value.passwordError)
        assertTrue(auth.calls.isEmpty())
    }

    @Test
    fun mismatchedConfirmationIsRejected() {
        vm.onPasswordChange("secret1")
        vm.onConfirmChange("secret2")
        vm.onSubmit("code-1")
        assertEquals(R.string.auth_error_password_mismatch, vm.uiState.value.confirmError)
        assertTrue(auth.calls.isEmpty())
    }

    @Test
    fun successMarksDone() {
        vm.onPasswordChange("secret1")
        vm.onConfirmChange("secret1")
        vm.onSubmit("code-1")
        val s = vm.uiState.value
        assertTrue(s.done)
        assertFalse(s.isLoading)
        assertEquals(listOf("confirmReset:code-1"), auth.calls)
    }

    @Test
    fun invalidCodeShowsMappedError() {
        auth.result = Result.Error(FirebaseAuthActionCodeException("ERROR_EXPIRED_ACTION_CODE", "expired"))
        vm.onPasswordChange("secret1")
        vm.onConfirmChange("secret1")
        vm.onSubmit("code-1")
        val s = vm.uiState.value
        assertEquals(R.string.error_auth_reset_code_invalid, s.error)
        assertFalse(s.done)
    }
}
