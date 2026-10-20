package com.fundtrail.data.sync

import android.content.Context
import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import com.fundtrail.data.local.FundtrailDatabase
import com.fundtrail.data.remote.model.ChangeType
import com.fundtrail.data.remote.model.RemoteChange
import com.fundtrail.data.remote.model.RemoteSnapshot
import com.fundtrail.data.remote.dto.UserProfileDto
import com.fundtrail.data.remote.source.AccountRemoteDataSource
import com.fundtrail.data.remote.source.CategoryRemoteDataSource
import com.fundtrail.data.remote.source.GoalRemoteDataSource
import com.fundtrail.data.remote.source.TransactionRemoteDataSource
import com.fundtrail.data.remote.source.UserProfileRemoteDataSource
import com.fundtrail.data.repository.toDomain
import com.fundtrail.data.repository.toEntity
import com.fundtrail.domain.model.Account
import com.fundtrail.domain.model.AccountKind
import com.fundtrail.domain.model.Category
import com.fundtrail.domain.model.Goal
import com.fundtrail.domain.model.Result
import com.fundtrail.domain.model.SpendKind
import com.fundtrail.domain.model.Transaction
import com.fundtrail.domain.model.TransactionType
import com.fundtrail.fakes.FakeAuthRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

/** Runs the real SyncManager against an in-memory Room database and fake Firestore sources. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class SyncManagerTest {

    private class FakeRemote<T> {
        val shared = MutableSharedFlow<RemoteSnapshot<T>>()
        var source: Flow<RemoteSnapshot<T>> = shared

        suspend fun emit(snapshot: RemoteSnapshot<T>) {
            withTimeout(WAIT_MS) { shared.subscriptionCount.first { it > 0 } }
            shared.emit(snapshot)
        }
    }

    private val txRemote = FakeRemote<Transaction>()
    private val accRemote = FakeRemote<Account>()
    private val catRemote = FakeRemote<Category>()
    private val goalRemote = FakeRemote<Goal>()
    private val profileRemote = FakeRemote<UserProfileDto>()

    private val transactions = object : TransactionRemoteDataSource {
        override fun observeTransactions(uid: String) = txRemote.source
        override fun addTransaction(uid: String, transaction: Transaction): Result<String> = error("unused")
        override fun updateTransaction(uid: String, transaction: Transaction): Result<Unit> = error("unused")
        override fun updateTransaction(uid: String, transactionId: String, updates: Map<String, Any?>): Result<Unit> = error("unused")
        override fun deleteTransaction(uid: String, transactionId: String): Result<Unit> = error("unused")
    }
    private val accounts = object : AccountRemoteDataSource {
        override fun observeAccounts(uid: String) = accRemote.source
        override fun addAccount(uid: String, account: Account): Result<String> = error("unused")
        override fun updateAccount(uid: String, account: Account): Result<Unit> = error("unused")
        override fun updateAccount(uid: String, accountId: String, updates: Map<String, Any?>): Result<Unit> = error("unused")
        override fun deleteAccount(uid: String, accountId: String): Result<Unit> = error("unused")
    }
    private val categories = object : CategoryRemoteDataSource {
        override fun observeCategories(uid: String) = catRemote.source
        override fun addCategory(uid: String, category: Category): Result<String> = error("unused")
        override fun updateCategory(uid: String, category: Category): Result<Unit> = error("unused")
        override fun updateCategory(uid: String, categoryId: String, updates: Map<String, Any?>): Result<Unit> = error("unused")
        override fun deleteCategory(uid: String, categoryId: String): Result<Unit> = error("unused")
    }
    private val goals = object : GoalRemoteDataSource {
        override fun observeGoals(uid: String) = goalRemote.source
        override fun saveGoal(uid: String, goal: Goal, isUpdate: Boolean, archivedId: String?): Result<String> = error("unused")
        override fun archiveGoal(uid: String, goalId: String): Result<Unit> = error("unused")
    }
    private val profile = object : UserProfileRemoteDataSource {
        override fun observeProfile(uid: String) = profileRemote.source
        override fun writeProfile(uid: String, updates: Map<String, Any?>): Result<Unit> = error("unused")
    }

    private lateinit var db: FundtrailDatabase
    private lateinit var scope: CoroutineScope
    private lateinit var manager: SyncManager
    private val auth = FakeAuthRepository()

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            FundtrailDatabase::class.java,
        ).allowMainThreadQueries().build()
        // Load Room and Robolectric classes up front so the timed waits below only measure sync.
        runBlocking {
            db.withTransaction { db.transactionDao().deleteAll() }
            db.transactionDao().observeBetween(0, 1).first()
        }
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        manager = SyncManager(auth, db, transactions, accounts, categories, goals, profile, scope, Dispatchers.IO)
        manager.start()
    }

    @After
    fun tearDown() {
        scope.cancel()
        db.close()
    }

    private fun tx(id: String, note: String? = null, deletedAt: Long? = null) = Transaction(
        id = id, type = TransactionType.EXPENSE, amountLkrMinor = 1_000, accountId = "a1",
        categoryId = "c1", spendKind = SpendKind.DISCRETIONARY, occurredAt = 1_000, note = note,
        deletedAt = deletedAt,
    )

    private fun acc(id: String) = Account(id = id, name = id, kind = AccountKind.BANK)
    private fun cat(id: String) = Category(id = id, name = id, defaultSpendKind = SpendKind.COMMITTED, icon = "i")
    private fun goal(id: String, active: Boolean = true) = Goal(
        id = id, name = id, targetLkrMinor = 1_000L, deadline = LocalDate.now().plusDays(10),
        baselineSavedMinor = 0L, isActive = active,
    )
    private fun profileDto(displayName: String) = UserProfileDto(displayName = displayName)

    private fun <T> snap(vararg changes: Pair<ChangeType, T>, fromCache: Boolean = false) =
        RemoteSnapshot(changes.map { RemoteChange(it.first, it.second) }, fromCache)

    private suspend fun roomTx() = db.transactionDao().observeBetween(0, Long.MAX_VALUE).first().map { it.toDomain() }
    private suspend fun roomAcc() = db.accountDao().observeAll().first().map { it.toDomain() }
    private suspend fun roomCat() = db.categoryDao().observeAll().first().map { it.toDomain() }
    private suspend fun roomGoal() = db.goalDao().observeActive().first()?.toDomain()
    private suspend fun roomProfileName() = db.userSettingsDao().observe().first()?.displayName

    private fun initialSyncDone(): Boolean =
        db.openHelper.readableDatabase.query("SELECT initialSyncDone FROM sync_state").use { it.moveToFirst() && it.getInt(0) == 1 }

    private suspend fun <T> eventually(read: suspend () -> T, expected: T) {
        var last: T? = null
        try {
            withTimeout(WAIT_MS) {
                while (true) {
                    last = read()
                    if (last == expected) return@withTimeout
                    delay(20)
                }
            }
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            assertEquals(expected, last)
        }
    }

    private fun signIn(uid: String = "u1") {
        auth.user.value = uid
    }

    @Test
    fun roomMatchesRemoteAfterAddModifyAndRemove() = runBlocking {
        signIn()
        txRemote.emit(snap(ChangeType.ADDED to tx("t1"), ChangeType.ADDED to tx("t2")))
        accRemote.emit(snap(ChangeType.ADDED to acc("a1")))
        catRemote.emit(snap(ChangeType.ADDED to cat("c1")))
        eventually({ roomTx().map { it.id }.sorted() }, listOf("t1", "t2"))
        eventually({ roomAcc().map { it.id } }, listOf("a1"))
        eventually({ roomCat().map { it.id } }, listOf("c1"))

        txRemote.emit(snap(ChangeType.MODIFIED to tx("t1", note = "edited"), ChangeType.REMOVED to tx("t2"), ChangeType.ADDED to tx("t3")))
        accRemote.emit(snap(ChangeType.ADDED to acc("a2")))
        catRemote.emit(snap(ChangeType.REMOVED to cat("c1")))

        eventually({ roomTx().map { it.id to it.note }.sortedBy { it.first } }, listOf("t1" to "edited", "t3" to null))
        eventually({ roomAcc().map { it.id }.sorted() }, listOf("a1", "a2"))
        eventually({ roomCat() }, emptyList())
    }

    @Test
    fun softDeletedDocumentIsRemovedEvenWhenReportedAsModified() = runBlocking {
        signIn()
        txRemote.emit(snap(ChangeType.ADDED to tx("t1")))
        eventually({ roomTx().map { it.id } }, listOf("t1"))

        txRemote.emit(snap(ChangeType.MODIFIED to tx("t1", deletedAt = 5_000)))
        eventually({ roomTx() }, emptyList())
    }

    @Test
    fun firstSnapshotReplacesStaleRows() = runBlocking {
        db.transactionDao().upsertAll(listOf(tx("stale").toEntity()))
        signIn()
        txRemote.emit(snap(ChangeType.ADDED to tx("t1")))
        eventually({ roomTx().map { it.id } }, listOf("t1"))
    }

    @Test
    fun initialSyncDoneOnlyAfterServerSnapshotFromEveryCollection() = runBlocking {
        signIn()
        txRemote.emit(snap(ChangeType.ADDED to tx("t1"), fromCache = true))
        accRemote.emit(snap(ChangeType.ADDED to acc("a1"), fromCache = true))
        catRemote.emit(snap(ChangeType.ADDED to cat("c1"), fromCache = true))
        goalRemote.emit(snap(ChangeType.ADDED to goal("g1"), fromCache = true))
        profileRemote.emit(snap(ChangeType.MODIFIED to profileDto("Kavindu"), fromCache = true))
        eventually({ roomCat().size }, 1)
        assertFalse(initialSyncDone())

        txRemote.emit(snap<Transaction>(fromCache = false))
        accRemote.emit(snap<Account>(fromCache = false))
        goalRemote.emit(snap<Goal>(fromCache = false))
        eventually({ roomTx().size }, 1)
        assertFalse(initialSyncDone())

        catRemote.emit(snap<Category>(fromCache = false))
        profileRemote.emit(snap<UserProfileDto>(fromCache = false))
        eventually({ initialSyncDone() }, true)
    }

    @Test
    fun goalMirrorsIncludingArchiveAndOnlyOneActiveAtATime() = runBlocking {
        signIn()
        goalRemote.emit(snap(ChangeType.ADDED to goal("g1")))
        eventually({ roomGoal()?.id }, "g1")

        goalRemote.emit(snap(ChangeType.MODIFIED to goal("g1", active = false), ChangeType.ADDED to goal("g2")))
        eventually({ roomGoal()?.id }, "g2")
    }

    @Test
    fun profileMirrorsTheSingleDocument() = runBlocking {
        signIn()
        profileRemote.emit(snap(ChangeType.MODIFIED to profileDto("Kavindu")))
        eventually({ roomProfileName() }, "Kavindu")

        profileRemote.emit(snap(ChangeType.MODIFIED to profileDto("Kavindu Perera")))
        eventually({ roomProfileName() }, "Kavindu Perera")
    }

    @Test
    fun pauseStopsListenersAndResumeRestartsThemWithAFullReplace() = runBlocking {
        signIn()
        txRemote.emit(snap(ChangeType.ADDED to tx("t1")))
        eventually({ roomTx().size }, 1)

        manager.pauseSync()
        assertEquals(0, txRemote.shared.subscriptionCount.value)
        assertEquals(0, accRemote.shared.subscriptionCount.value)
        assertEquals(0, catRemote.shared.subscriptionCount.value)
        assertEquals(0, goalRemote.shared.subscriptionCount.value)
        assertEquals(0, profileRemote.shared.subscriptionCount.value)

        manager.resumeSync()
        txRemote.emit(snap(ChangeType.ADDED to tx("t9")))
        eventually({ roomTx().map { it.id } }, listOf("t9"))
    }

    @Test
    fun signOutStopsListenersAndNextSignInSyncsAgainEvenAfterPause() = runBlocking {
        signIn("u1")
        txRemote.emit(snap(ChangeType.ADDED to tx("t1")))
        eventually({ roomTx().size }, 1)

        manager.pauseSync()
        auth.user.value = null
        eventually({ txRemote.shared.subscriptionCount.value }, 0)

        signIn("u2")
        txRemote.emit(snap(ChangeType.ADDED to tx("u2t")))
        eventually({ roomTx().map { it.id } }, listOf("u2t"))
    }

    /** The sign-out null can be conflated away by `currentUserId`; a pause must not outlive the user it was requested for. */
    @Test
    fun pausedSyncRestartsForAnotherUserEvenWhenTheSignOutIsNeverObserved() = runBlocking {
        signIn("u1")
        txRemote.emit(snap(ChangeType.ADDED to tx("t1")))
        eventually({ roomTx().size }, 1)

        manager.pauseSync()
        signIn("u2") // straight to the next user: no null in between for anything to react to
        txRemote.emit(snap(ChangeType.ADDED to tx("u2t")))
        eventually({ roomTx().map { it.id } }, listOf("u2t"))
    }

    @Test
    fun signingBackInAsTheSameUserAfterAPauseSyncsAgain() = runBlocking {
        signIn("u1")
        txRemote.emit(snap(ChangeType.ADDED to tx("t1")))
        eventually({ roomTx().size }, 1)

        manager.pauseSync()
        auth.user.value = null
        eventually({ txRemote.shared.subscriptionCount.value }, 0)

        signIn("u1")
        txRemote.emit(snap(ChangeType.ADDED to tx("again")))
        eventually({ roomTx().map { it.id } }, listOf("again"))
    }

    @Test
    fun failingListenerDoesNotStopTheOthers() = runBlocking {
        accRemote.source = flow { throw RuntimeException("listener boom") }
        signIn()
        txRemote.emit(snap(ChangeType.ADDED to tx("t1")))
        catRemote.emit(snap(ChangeType.ADDED to cat("c1")))
        eventually({ roomTx().size }, 1)
        eventually({ roomCat().size }, 1)
        assertTrue(roomAcc().isEmpty())
    }

    private companion object {
        const val WAIT_MS = 20_000L // generous: slow CI/disks; only costs time when a wait fails
    }
}
