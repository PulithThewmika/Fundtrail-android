package com.fundtrail.domain.usecase.auth

import com.fundtrail.domain.model.Result
import com.fundtrail.domain.model.SignOutOutcome
import com.fundtrail.fakes.FakeAuthRepository
import com.fundtrail.fakes.FakeLocalDataRepository
import com.fundtrail.fakes.FakeSyncRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SignOutUseCaseTest {
    private val auth = FakeAuthRepository()
    private val local = FakeLocalDataRepository(auth.calls)
    private val useCase = SignOutUseCase(auth, local, FakeSyncRepository(auth.calls))

    @Test
    fun signsOutThenClearsLocalData() = runTest {
        val result = useCase()
        assertEquals(Result.Success(SignOutOutcome.SIGNED_OUT), result)
        assertEquals(listOf("pauseSync", "signOut", "clear"), auth.calls)
    }

    @Test
    fun unsyncedWritesStopsWithoutSigningOut() = runTest {
        local.pendingWritesDone = false
        val result = useCase()
        assertEquals(Result.Success(SignOutOutcome.HAS_UNSYNCED_WRITES), result)
        assertTrue(auth.calls.isEmpty())
    }

    @Test
    fun forceSignsOutDespiteUnsyncedWrites() = runTest {
        local.pendingWritesDone = false
        val result = useCase(force = true)
        assertEquals(Result.Success(SignOutOutcome.SIGNED_OUT), result)
        assertEquals(listOf("pauseSync", "signOut", "clear"), auth.calls)
    }

    @Test
    fun failedSignOutResumesSyncAndKeepsLocalData() = runTest {
        auth.result = Result.Error(RuntimeException("boom"))
        val result = useCase()
        assertTrue(result is Result.Error)
        assertEquals(listOf("pauseSync", "signOut", "resumeSync"), auth.calls)
    }
}
