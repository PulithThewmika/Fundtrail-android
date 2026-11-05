package com.fundtrail.ui.auth

import com.fundtrail.R
import com.fundtrail.domain.model.Result
import com.fundtrail.domain.usecase.auth.SendPasswordResetEmailUseCase
import com.fundtrail.fakes.FakeAuthRepository
import com.fundtrail.fakes.MainDispatcherRule
import com.google.firebase.FirebaseNetworkException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ForgotPasswordViewModelTest {
    @get:Rule val mainDispatcher = MainDispatcherRule()

    private val auth = FakeAuthRepository()
    private val vm = ForgotPasswordViewModel(SendPasswordResetEmailUseCase(auth))

    @Test
    fun emptyEmailShowsInlineErrorWithoutCallingAuth() {
        vm.onSubmit()
        assertEquals(R.string.auth_error_email_required, vm.uiState.value.emailError)
        assertTrue(auth.calls.isEmpty())
    }

    @Test
    fun malformedEmailIsRejected() {
        vm.onEmailChange("not-an-email")
        vm.onSubmit()
        assertEquals(R.string.auth_error_email_invalid, vm.uiState.value.emailError)
        assertTrue(auth.calls.isEmpty())
    }

    @Test
    fun successMarksEmailSent() {
        vm.onEmailChange("a@b.com")
        vm.onSubmit()
        val s = vm.uiState.value
        assertTrue(s.emailSent)
        assertFalse(s.isLoading)
        assertEquals(listOf("resetEmail:a@b.com"), auth.calls)
    }

    @Test
    fun failureShowsMappedErrorAndKeepsEmail() {
        auth.result = Result.Error(FirebaseNetworkException("offline"))
        vm.onEmailChange("a@b.com")
        vm.onSubmit()
        val s = vm.uiState.value
        assertEquals(R.string.error_network, s.error)
        assertEquals("a@b.com", s.email)
        assertFalse(s.emailSent)
    }
}
