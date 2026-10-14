package com.fundtrail.ui.auth

import com.fundtrail.R
import com.fundtrail.domain.model.Result
import com.fundtrail.domain.usecase.account.SeedDefaultAccountsUseCase
import com.fundtrail.domain.usecase.auth.RegisterUseCase
import com.fundtrail.fakes.FakeAccountRepository
import com.fundtrail.fakes.FakeAuthRepository
import com.fundtrail.fakes.FakeLocalDataRepository
import com.fundtrail.fakes.MainDispatcherRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SignUpViewModelTest {
    @get:Rule val mainDispatcher = MainDispatcherRule()

    private val auth = FakeAuthRepository()
    private val vm = SignUpViewModel(
        register = RegisterUseCase(auth, FakeLocalDataRepository(auth.calls)),
        seedDefaultAccounts = SeedDefaultAccountsUseCase(FakeAccountRepository())
    )

    private fun fill(email: String = "a@b.com", password: String = "secret1", confirm: String = "secret1") {
        vm.onEmailChange(email)
        vm.onPasswordChange(password)
        vm.onConfirmChange(confirm)
    }

    @Test
    fun shortPasswordIsRejected() {
        fill(password = "123", confirm = "123")
        vm.onSubmit()
        assertEquals(R.string.auth_error_password_short, vm.uiState.value.passwordError)
        assertTrue(auth.calls.isEmpty())
    }

    @Test
    fun mismatchedConfirmIsRejected() {
        fill(confirm = "different")
        vm.onSubmit()
        assertEquals(R.string.auth_error_password_mismatch, vm.uiState.value.confirmError)
        assertTrue(auth.calls.isEmpty())
    }

    @Test
    fun successSetsRegistered() {
        fill()
        vm.onSubmit()
        assertEquals(listOf("clear", "register:a@b.com"), auth.calls)
        assertTrue(vm.uiState.value.registered)
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test
    fun failureShowsMappedError() {
        auth.result = Result.Error(RuntimeException("boom"))
        fill()
        vm.onSubmit()
        assertEquals(R.string.error_generic, vm.uiState.value.error)
        assertFalse(vm.uiState.value.registered)
    }
}
