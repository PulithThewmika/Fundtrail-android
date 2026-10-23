package com.fundtrail.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.fundtrail.domain.model.Result
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FxRateRepositoryImplTest {

    private lateinit var file: File
    private lateinit var scope: TestScope
    private lateinit var store: DataStore<Preferences>
    private lateinit var repo: FxRateRepositoryImpl

    @Before
    fun setUp() {
        file = File.createTempFile("fx_rate_test", ".preferences_pb").also { it.delete() }
        scope = TestScope(StandardTestDispatcher())
        store = PreferenceDataStoreFactory.create(scope = scope) { file }
        repo = FxRateRepositoryImpl(store)
    }

    @After
    fun tearDown() {
        file.delete()
    }

    @Test
    fun remembersARatePerCurrency() = runTest(scope.testScheduler) {
        assertTrue(repo.observeLastUsedRates().first().isEmpty())

        repo.setLastUsedRate("USD", 297_450_000L)
        repo.setLastUsedRate("USDT", 300_000_000L)

        val rates = repo.observeLastUsedRates().first()
        assertEquals(297_450_000L, rates["USD"])
        assertEquals(300_000_000L, rates["USDT"])
    }

    @Test
    fun overwritesTheSameCurrencyOnTheNextSave() = runTest(scope.testScheduler) {
        repo.setLastUsedRate("USD", 297_450_000L)
        repo.setLastUsedRate("USD", 310_000_000L)

        assertEquals(310_000_000L, repo.observeLastUsedRates().first()["USD"])
    }

    @Test
    fun rejectsLkrAndNonPositiveRates() = runTest(scope.testScheduler) {
        assertTrue(repo.setLastUsedRate("LKR", 1_000_000L) is Result.Error)
        assertTrue(repo.setLastUsedRate("USD", 0L) is Result.Error)
        assertNull(repo.observeLastUsedRates().first()["USD"])
    }
}
