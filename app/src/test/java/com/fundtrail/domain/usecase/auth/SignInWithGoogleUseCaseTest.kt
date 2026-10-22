package com.fundtrail.domain.usecase.auth

import com.fundtrail.domain.model.Result
import com.fundtrail.fakes.FakeAuthRepository
import com.fundtrail.fakes.FakeLocalDataRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SignInWithGoogleUseCaseTest {
    private val auth = FakeAuthRepository()
    private val local = FakeLocalDataRepository(auth.calls)
    private val useCase = SignInWithGoogleUseCase(auth, local)

    @Test
    fun clearsLeftoverDataThenSignsInWithTheToken() = runTest {
        assertEquals(Result.Success(Unit), useCase("token-1"))
        assertEquals(listOf("clear", "google:token-1"), auth.calls)
    }

    @Test
    fun aFailedClearBlocksSignIn() = runTest {
        local.clearResult = Result.Error(RuntimeException("disk"))
        assertTrue(useCase("token-1") is Result.Error)
        assertEquals(listOf("clear"), auth.calls)
    }
}
