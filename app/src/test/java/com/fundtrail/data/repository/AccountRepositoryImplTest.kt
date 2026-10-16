package com.fundtrail.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.fundtrail.data.local.FundtrailDatabase
import com.fundtrail.data.local.SyncStateEntity
import com.fundtrail.data.remote.model.RemoteSnapshot
import com.fundtrail.data.remote.source.AccountRemoteDataSource
import com.fundtrail.domain.model.Account
import com.fundtrail.domain.model.AccountKind
import com.fundtrail.domain.model.Result
import com.fundtrail.domain.model.defaultAccounts
import com.fundtrail.fakes.FakeAuthRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.joinAll
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
import java.util.Collections

/** Runs the real seeding logic against an in-memory Room database and a recording Firestore source. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class AccountRepositoryImplTest {

    private val written: MutableList<Pair<String, String>> = Collections.synchronizedList(mutableListOf())
    private val remote = object : AccountRemoteDataSource {
        override fun observeAccounts(uid: String): Flow<RemoteSnapshot<Account>> = error("unused")
        override fun addAccount(uid: String, account: Account): Result<String> {
            written += uid to account.id
            return Result.Success(account.id)
        }
        override fun updateAccount(uid: String, account: Account): Result<Unit> = error("unused")
        override fun updateAccount(uid: String, accountId: String, updates: Map<String, Any?>): Result<Unit> = error("unused")
        override fun deleteAccount(uid: String, accountId: String): Result<Unit> = error("unused")
    }

    private val auth = FakeAuthRepository()
    private lateinit var db: FundtrailDatabase
    private lateinit var scope: CoroutineScope
    private lateinit var repo: AccountRepositoryImpl

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), FundtrailDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        repo = AccountRepositoryImpl(auth, db.accountDao(), db.syncStateDao(), remote, scope)
        auth.user.value = "u1"
    }

    @After
    fun tearDown() {
        scope.cancel()
        db.close()
    }

    private suspend fun finishSync() = db.syncStateDao().upsert(SyncStateEntity(initialSyncDone = true))

    /** Waits until every background seeding job has finished. */
    private suspend fun awaitSeeding() = withTimeout(5_000) { scope.coroutineContext[Job]!!.children.toList().joinAll() }

    @Test
    fun returnsImmediatelyAndWritesNothingBeforeTheFirstSync() = runBlocking {
        assertTrue(repo.seedDefaultsIfEmpty() is Result.Success)
        delay(200)
        assertEquals(emptyList<Pair<String, String>>(), written.toList())
    }

    @Test
    fun seedsAllDefaultAccountsOnceSyncFindsNoAccounts() = runBlocking {
        repo.seedDefaultsIfEmpty()
        finishSync()
        awaitSeeding()
        assertEquals(defaultAccounts.map { "u1" to it.id }, written.toList())
    }

    @Test
    fun existingUserIsNeverSeeded() = runBlocking {
        db.accountDao().upsertAll(listOf(Account(id = "a1", name = "BOC", kind = AccountKind.BANK).toEntity()))
        finishSync()
        repo.seedDefaultsIfEmpty()
        awaitSeeding()
        assertEquals(emptyList<Pair<String, String>>(), written.toList())
    }

    @Test
    fun userWhoSignedOutWhileWaitingIsNotSeeded() = runBlocking {
        repo.seedDefaultsIfEmpty()
        auth.user.value = "u2"
        finishSync()
        awaitSeeding()
        assertEquals(emptyList<Pair<String, String>>(), written.toList())
    }

    @Test
    fun notSignedInReturnsError() = runBlocking {
        auth.user.value = null
        assertTrue(repo.seedDefaultsIfEmpty() is Result.Error)
    }
}
