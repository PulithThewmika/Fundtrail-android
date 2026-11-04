package com.fundtrail.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.IOException

/** Reading the app lock setting must never fail open (#222). */
class AppLockRepositoryImplTest {

    private class FakeStore(override val data: Flow<Preferences>) : DataStore<Preferences> {
        override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences = emptyPreferences()
    }

    private fun repo(data: Flow<Preferences>) = AppLockRepositoryImpl(FakeStore(data))

    @Test
    fun nothingStoredMeansTheLockIsOff() = runTest {
        assertFalse(repo(flow { emit(emptyPreferences()) }).enabled.first())
    }

    @Test
    fun aStoredOnIsReadAsOn() = runTest {
        val on = mutablePreferencesOf(booleanPreferencesKey("app_lock_enabled") to true)

        assertTrue(repo(flow { emit(on) }).enabled.first())
    }

    @Test
    fun anUnreadableStoreIsTreatedAsOnNotOff() = runTest {
        // Reporting "off" here would resolve the lock and open the app even though the stored setting may be on.
        assertTrue(repo(flow { throw IOException("disk") }).enabled.first())
    }

    @Test
    fun aFailureThatIsNotAnIoErrorIsNotSwallowed() = runTest {
        try {
            repo(flow { throw IllegalStateException("bug") }).enabled.first()
            fail("expected the failure to propagate")
        } catch (expected: IllegalStateException) {
            assertEquals("bug", expected.message)
        }
    }
}
