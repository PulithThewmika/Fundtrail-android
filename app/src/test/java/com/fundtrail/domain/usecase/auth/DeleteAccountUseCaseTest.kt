package com.fundtrail.domain.usecase.auth

import com.fundtrail.domain.model.Result
import com.fundtrail.fakes.FakeAppLockRepository
import com.fundtrail.fakes.FakeAuthRepository
import com.fundtrail.fakes.FakeLocalDataRepository
import com.fundtrail.fakes.FakeSyncRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** FR-117. */
class DeleteAccountUseCaseTest {
    private val auth = FakeAuthRepository()
    private val local = FakeLocalDataRepository(auth.calls)
    private val appLock = FakeAppLockRepository(auth.calls)
    private val useCase = DeleteAccountUseCase(auth, local, FakeSyncRepository(auth.calls), appLock)

    @Test
    fun deletesThenClearsLocalData() = runTest {
        val result = useCase()
        assertEquals(Result.Success(Unit), result)
        assertEquals(listOf("pauseSync", "deleteAccount", "clear", "appLock=false"), auth.calls)
    }

    @Test
    fun failedDeleteResumesSyncAndKeepsLocalData() = runTest {
        auth.result = Result.Error(RuntimeException("requires recent login"))
        val result = useCase()
        assertTrue(result is Result.Error)
        assertEquals(listOf("pauseSync", "deleteAccount", "resumeSync"), auth.calls)
    }
}
