package com.fundtrail.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.fundtrail.data.local.FundtrailDatabase
import com.fundtrail.data.local.SyncStateEntity
import com.fundtrail.data.local.entity.UserSettingsEntity
import com.fundtrail.data.remote.source.UserProfileRemoteDataSource
import com.fundtrail.domain.model.Result
import com.fundtrail.domain.model.UserSettings
import com.fundtrail.fakes.FakeAuthRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Runs the real create-vs-merge logic against an in-memory Room database and a recording Firestore source. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class SettingsRepositoryImplTest {

    private val writes = mutableListOf<Pair<String, Map<String, Any?>>>()
    private val remote = object : UserProfileRemoteDataSource {
        override fun observeProfile(uid: String) = error("unused")
        override fun writeProfile(uid: String, updates: Map<String, Any?>): Result<Unit> {
            writes += uid to updates
            return Result.Success(Unit)
        }
    }

    private val auth = FakeAuthRepository()
    private lateinit var db: FundtrailDatabase
    private lateinit var repo: SettingsRepositoryImpl

    private val settings = UserSettings(incomeEstimateMinMinor = 40_000_00L, incomeEstimateMaxMinor = 60_000_00L, expectedSalaryMinor = 50_000_00L, salaryPayday = 25)

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), FundtrailDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repo = SettingsRepositoryImpl(auth, db.userSettingsDao(), db.syncStateDao(), remote)
        auth.user.value = "u1"
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun mergesWithoutDisplayNameWhenTheRowAlreadyExists() = runBlocking {
        db.userSettingsDao().upsert(existingRow())
        assertTrue(repo.update(settings) is Result.Success)
        val (uid, updates) = writes.single()
        assertEquals("u1", uid)
        assertTrue("displayName" !in updates)
        assertTrue("createdAt" !in updates)
        assertEquals(settings.incomeEstimateMinMinor, updates["incomeEstimateMinMinor"])
    }

    @Test
    fun createsWithDisplayNameWhenNoRowAndAlreadySynced() = runBlocking {
        auth.currentUserDisplayName = null
        finishSync()
        assertTrue(repo.update(settings) is Result.Success)
        val (_, updates) = writes.single()
        assertEquals("FundTrail user", updates["displayName"])
        assertEquals(1, updates["schemaVersion"])
        assertTrue("createdAt" in updates)
    }

    @Test
    fun waitsForTheFirstSyncBeforeCreating() = runBlocking {
        assertTrue(repo.update(settings) is Result.Error)
        assertTrue(writes.isEmpty())
    }

    @Test
    fun invalidSettingsAreRejectedBeforeAnyWrite() = runBlocking {
        val invalid = settings.copy(incomeEstimateMinMinor = 100L, incomeEstimateMaxMinor = 50L)
        assertTrue(repo.update(invalid) is Result.Error)
        assertTrue(writes.isEmpty())
    }

    @Test
    fun updatingWhileSignedOutReturnsError() = runBlocking {
        auth.user.value = null
        assertTrue(repo.update(settings) is Result.Error)
        assertTrue(writes.isEmpty())
    }

    @Test
    fun observeDefaultsWhenNoRowExists() = runBlocking {
        assertEquals(UserSettings(), repo.observe().first())
    }

    @Test
    fun observeReadsFromRoom() = runBlocking {
        db.userSettingsDao().upsert(existingRow())
        assertEquals(settings.copy(createdAt = 1L), repo.observe().first())
    }

    private suspend fun finishSync() = db.syncStateDao().upsert(SyncStateEntity(initialSyncDone = true))

    private fun existingRow() = UserSettingsEntity(
        displayName = "Kavindu",
        incomeEstimateMinMinor = settings.incomeEstimateMinMinor,
        incomeEstimateMaxMinor = settings.incomeEstimateMaxMinor,
        expectedSalaryMinor = settings.expectedSalaryMinor,
        salaryPayday = settings.salaryPayday,
        schemaVersion = 1,
        createdAt = 1L,
        updatedAt = 1L,
    )
}
