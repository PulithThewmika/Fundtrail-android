package com.fundtrail.domain.usecase.auth

import com.fundtrail.domain.model.Result
import com.fundtrail.fakes.FakeAuthRepository
import com.fundtrail.fakes.FakeLocalDataRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CredentialUseCasesTest {
    private val auth = FakeAuthRepository()
    private val local = FakeLocalDataRepository(auth.calls)

    @Test
    fun signInClearsLeftoverDataThenTrimsEmail() = runTest {
        SignInUseCase(auth, local)("  a@b.com ", "secret1")
        assertEquals(listOf("clear", "signIn:a@b.com"), auth.calls)
    }

    @Test
    fun registerClearsLeftoverDataThenTrimsEmail() = runTest {
        RegisterUseCase(auth, local)(" a@b.com", "secret1")
        assertEquals(listOf("clear", "register:a@b.com"), auth.calls)
    }

    @Test
    fun signInIsBlockedWhenCleanupFails() = runTest {
        local.clearResult = Result.Error(RuntimeException("disk"))
        val result = SignInUseCase(auth, local)("a@b.com", "secret1")
        assertTrue(result is Result.Error)
        assertEquals(listOf("clear"), auth.calls)
    }

    @Test
    fun registerIsBlockedWhenCleanupFails() = runTest {
        local.clearResult = Result.Error(RuntimeException("disk"))
        val result = RegisterUseCase(auth, local)("a@b.com", "secret1")
        assertTrue(result is Result.Error)
        assertEquals(listOf("clear"), auth.calls)
    }
}
