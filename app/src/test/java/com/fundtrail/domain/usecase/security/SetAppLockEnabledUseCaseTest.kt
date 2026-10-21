package com.fundtrail.domain.usecase.security

import com.fundtrail.domain.model.Result
import com.fundtrail.domain.model.ScreenLockRequiredException
import com.fundtrail.fakes.FakeAppLockRepository
import com.fundtrail.fakes.FakeDeviceSecurityRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SetAppLockEnabledUseCaseTest {
    private val appLock = FakeAppLockRepository()
    private val device = FakeDeviceSecurityRepository()
    private val setEnabled = SetAppLockEnabledUseCase(appLock, device)

    @Test
    fun enablesWhenThePhoneCanAuthenticate() = runTest {
        assertEquals(Result.Success(Unit), setEnabled(true))
        assertEquals(true, appLock.enabled.value)
    }

    @Test
    fun staysOffWithoutAScreenLock() = runTest {
        device.canAuthenticate = false

        val result = setEnabled(true)

        assertTrue((result as Result.Error).exception is ScreenLockRequiredException)
        assertEquals(false, appLock.enabled.value)
    }

    @Test
    fun turningItOffNeverNeedsAScreenLock() = runTest {
        appLock.enabled.value = true
        device.canAuthenticate = false

        assertEquals(Result.Success(Unit), setEnabled(false))
        assertEquals(false, appLock.enabled.value)
    }
}
