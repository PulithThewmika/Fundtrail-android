package com.fundtrail.domain.usecase.auth

import com.fundtrail.fakes.FakeAuthRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class CredentialUseCasesTest {
    private val auth = FakeAuthRepository()

    @Test
    fun signInTrimsEmail() = runTest {
        SignInUseCase(auth)("  a@b.com ", "secret1")
        assertEquals(listOf("signIn:a@b.com"), auth.calls)
    }

    @Test
    fun registerTrimsEmail() = runTest {
        RegisterUseCase(auth)(" a@b.com", "secret1")
        assertEquals(listOf("register:a@b.com"), auth.calls)
    }
}
