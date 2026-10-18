package com.fundtrail.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.fundtrail.data.local.FundtrailDatabase
import com.fundtrail.data.local.SyncStateEntity
import com.fundtrail.data.remote.model.RemoteSnapshot
import com.fundtrail.data.remote.source.CategoryRemoteDataSource
import com.fundtrail.domain.model.Category
import com.fundtrail.domain.model.DefaultCategories
import com.fundtrail.domain.model.Result
import com.fundtrail.domain.model.SpendKind
import com.fundtrail.fakes.FakeAuthRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Runs the real seedDefaultsIfEmpty against an in-memory Room database and a recording Firestore fake. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class CategorySeedingTest {

    private val written = mutableListOf<Category>()
    private var failWrites = false
    private val remote = object : CategoryRemoteDataSource {
        override fun observeCategories(uid: String): Flow<RemoteSnapshot<Category>> = emptyFlow()
        override fun addCategory(uid: String, category: Category): Result<String> {
            if (failWrites) return Result.Error(IllegalStateException("write rejected"))
            written += category
            return Result.Success(category.id)
        }
        override fun updateCategory(uid: String, category: Category): Result<Unit> = error("unused")
        override fun updateCategory(uid: String, categoryId: String, updates: Map<String, Any?>): Result<Unit> = error("unused")
        override fun deleteCategory(uid: String, categoryId: String): Result<Unit> = error("unused")
    }

    private val auth = FakeAuthRepository().apply { user.value = "uid-1" }
    private lateinit var db: FundtrailDatabase
    private lateinit var repo: CategoryRepositoryImpl

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            FundtrailDatabase::class.java,
        ).allowMainThreadQueries().build()
        repo = CategoryRepositoryImpl(auth, db.categoryDao(), db.syncStateDao(), db.transactionDao(), remote)
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun seedsTheTwelveDefaultsWithFixedIdsOnceTheInitialSyncIsDone() = runBlocking {
        db.syncStateDao().upsert(SyncStateEntity(initialSyncDone = true))

        assertTrue(repo.seedDefaultsIfEmpty() is Result.Success)

        assertEquals(DefaultCategories.all, written)
        assertEquals(6, written.count { it.defaultSpendKind == SpendKind.COMMITTED })
        assertEquals(6, written.count { it.defaultSpendKind == SpendKind.DISCRETIONARY })
    }

    @Test
    fun doesNotWriteBeforeTheInitialSyncSoItNeverSeedsOverDataThatIsStillArriving() = runBlocking {
        val seeding = async { repo.seedDefaultsIfEmpty() }

        delay(300) // offline and not yet synced: nothing may be written
        assertTrue(written.isEmpty())
        assertTrue(seeding.isActive)

        db.syncStateDao().upsert(SyncStateEntity(initialSyncDone = true))
        assertTrue(withTimeout(5_000) { seeding.await() } is Result.Success)
        assertEquals(12, written.size)
    }

    /**
     * The overwrite bug was a 2 s timeout: a returning user's first sync often takes longer, so the defaults were
     * written over their real categories. Wall-clock time is deliberate here, because what is guarded is a
     * wall-clock timeout. It can only fail late, never flake: with the sync still pending, nothing may be written.
     */
    @Test
    fun stillWaitsForTheInitialSyncAfterThreeSecondsSoASlowSyncIsNeverSeededOver() = runBlocking {
        val seeding = async { repo.seedDefaultsIfEmpty() }

        delay(3_000)
        assertTrue("seeding must not give up on a slow first sync", seeding.isActive)
        assertTrue("nothing may be written while the first sync is still pending", written.isEmpty())

        db.categoryDao().upsertAll(listOf(DefaultCategories.all.first().toEntity())) // the sync brings the user's data
        db.syncStateDao().upsert(SyncStateEntity(initialSyncDone = true))
        assertTrue(withTimeout(5_000) { seeding.await() } is Result.Success)
        assertTrue("a user whose data arrived must not be re-seeded", written.isEmpty())
    }

    @Test
    fun aRejectedFirestoreWriteNeverPutsARowIntoRoom() = runBlocking {
        failWrites = true
        db.syncStateDao().upsert(SyncStateEntity(initialSyncDone = true))

        assertTrue(repo.seedDefaultsIfEmpty() is Result.Success)

        assertTrue("only the sync listener writes to Room", db.categoryDao().observeAll().first().isEmpty())
    }

    @Test
    fun doesNotSeedIntoAnotherSessionIfTheUserSwitchedWhileWaiting() = runBlocking {
        val seeding = async { repo.seedDefaultsIfEmpty() }
        delay(100)

        auth.user.value = "uid-2"
        db.syncStateDao().upsert(SyncStateEntity(initialSyncDone = true))
        withTimeout(5_000) { seeding.await() }

        assertTrue(written.isEmpty())
    }

    @Test
    fun isIdempotentWhenCategoriesAlreadyExist() = runBlocking {
        db.syncStateDao().upsert(SyncStateEntity(initialSyncDone = true))
        db.categoryDao().upsertAll(listOf(DefaultCategories.all.first().toEntity()))

        assertTrue(repo.seedDefaultsIfEmpty() is Result.Success)

        assertTrue("an existing user must not be re-seeded", written.isEmpty())
    }

    @Test
    fun failsWhenNobodyIsSignedIn() = runBlocking {
        auth.user.value = null
        db.syncStateDao().upsert(SyncStateEntity(initialSyncDone = true))

        assertTrue(repo.seedDefaultsIfEmpty() is Result.Error)
        assertTrue(written.isEmpty())
    }
}
