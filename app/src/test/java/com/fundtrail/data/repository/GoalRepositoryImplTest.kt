package com.fundtrail.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.fundtrail.data.local.FundtrailDatabase
import com.fundtrail.data.remote.model.RemoteSnapshot
import com.fundtrail.data.remote.source.GoalRemoteDataSource
import com.fundtrail.domain.model.Goal
import com.fundtrail.domain.model.Result
import com.fundtrail.fakes.FakeAuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

/** Runs the real archive-on-save logic against an in-memory Room database and a recording Firestore source. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class GoalRepositoryImplTest {

    private data class Saved(val uid: String, val goal: Goal, val isUpdate: Boolean, val archivedId: String?)

    private val saves = mutableListOf<Saved>()
    private val archives = mutableListOf<Pair<String, String>>()
    private val remote = object : GoalRemoteDataSource {
        override fun observeGoals(uid: String): Flow<RemoteSnapshot<Goal>> = error("unused")
        override fun saveGoal(uid: String, goal: Goal, isUpdate: Boolean, archivedId: String?): Result<String> {
            saves += Saved(uid, goal, isUpdate, archivedId)
            return Result.Success(goal.id)
        }
        override fun archiveGoal(uid: String, goalId: String): Result<Unit> {
            archives += uid to goalId
            return Result.Success(Unit)
        }
    }

    private val auth = FakeAuthRepository()
    private lateinit var db: FundtrailDatabase
    private lateinit var repo: GoalRepositoryImpl

    private val newGoal = Goal(
        id = "g1",
        name = "MacBook Pro M4",
        targetLkrMinor = 49_000_000L,
        deadline = LocalDate.now().plusMonths(6),
        baselineSavedMinor = 0L,
    )

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), FundtrailDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repo = GoalRepositoryImpl(auth, db.goalDao(), remote)
        auth.user.value = "u1"
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun savingTheFirstGoalArchivesNothingAndIsNotAnUpdate() = runBlocking {
        assertTrue(repo.save(newGoal) is Result.Success)
        assertEquals(listOf(Saved("u1", newGoal, isUpdate = false, archivedId = null)), saves)
    }

    @Test
    fun savingANewActiveGoalArchivesThePreviousOne() = runBlocking {
        db.goalDao().upsertAll(listOf(newGoal.toEntity()))
        val replacement = newGoal.copy(id = "g2", name = "Emergency fund")
        assertTrue(repo.save(replacement) is Result.Success)
        assertEquals(listOf(Saved("u1", replacement, isUpdate = false, archivedId = "g1")), saves)
    }

    @Test
    fun savingAnExistingGoalIsAnUpdateAndNeverArchivesItself() = runBlocking {
        db.goalDao().upsertAll(listOf(newGoal.toEntity()))
        val update = newGoal.copy(baselineSavedMinor = 10_000L)
        assertTrue(repo.save(update) is Result.Success)
        assertEquals(listOf(Saved("u1", update, isUpdate = true, archivedId = null)), saves)
    }

    @Test
    fun savingAnInactiveGoalNeverArchivesTheCurrentActiveOne() = runBlocking {
        db.goalDao().upsertAll(listOf(newGoal.toEntity()))
        val draft = newGoal.copy(id = "g2", name = "Draft", isActive = false)
        assertTrue(repo.save(draft) is Result.Success)
        assertEquals(listOf(Saved("u1", draft, isUpdate = false, archivedId = null)), saves)
    }

    @Test
    fun invalidGoalIsRejectedBeforeAnyWrite() = runBlocking {
        val invalid = newGoal.copy(targetLkrMinor = 0L)
        assertTrue(repo.save(invalid) is Result.Error)
        assertTrue(saves.isEmpty())
    }

    @Test
    fun savingWhileSignedOutReturnsError() = runBlocking {
        auth.user.value = null
        assertTrue(repo.save(newGoal) is Result.Error)
        assertTrue(saves.isEmpty())
    }

    @Test
    fun archiveDelegatesToRemote() = runBlocking {
        assertTrue(repo.archive("g1") is Result.Success)
        assertEquals(listOf("u1" to "g1"), archives)
    }

    @Test
    fun archiveWhileSignedOutReturnsError() = runBlocking {
        auth.user.value = null
        assertTrue(repo.archive("g1") is Result.Error)
        assertTrue(archives.isEmpty())
    }

    /**
     * Without serializing `save()`, two concurrent saves could both read the same "current
     * active" Room row before either commits, archive it twice, and each activate a different new
     * goal — leaving two goals active instead of one. This pins that `save()` bodies never
     * interleave: the second save can't even start its own Room read until the first's remote call
     * returns.
     */
    @Test
    fun concurrentSavesNeverInterleaveTheirCriticalSections() = runBlocking {
        db.goalDao().upsertAll(listOf(newGoal.toEntity()))
        val firstEntered = Mutex(locked = true)
        val releaseFirst = Mutex(locked = true)
        val gatedRemote = object : GoalRemoteDataSource {
            override fun observeGoals(uid: String): Flow<RemoteSnapshot<Goal>> = error("unused")
            override fun saveGoal(uid: String, goal: Goal, isUpdate: Boolean, archivedId: String?): Result<String> {
                saves += Saved(uid, goal, isUpdate, archivedId)
                if (saves.size == 1) {
                    firstEntered.unlock()
                    runBlocking { releaseFirst.lock() } // holds the repo's save lock until released below
                }
                return Result.Success(goal.id)
            }
            override fun archiveGoal(uid: String, goalId: String): Result<Unit> = error("unused")
        }
        val gatedRepo = GoalRepositoryImpl(auth, db.goalDao(), gatedRemote)

        val first = launch(Dispatchers.Default) { gatedRepo.save(newGoal.copy(id = "g2", name = "Replacement 1")) }
        firstEntered.lock() // first is now inside saveGoal, holding the save lock

        val second = async(Dispatchers.Default) { gatedRepo.save(newGoal.copy(id = "g3", name = "Replacement 2")) }
        delay(100) // second would have entered saveGoal by now if save() weren't serialized
        assertEquals(1, saves.size)

        releaseFirst.unlock()
        first.join()
        second.await()
        assertEquals(2, saves.size)
    }

    @Test
    fun observeActiveReadsFromRoom() = runBlocking {
        assertNull(repo.observeActive().first())
        db.goalDao().upsertAll(listOf(newGoal.toEntity()))
        assertEquals(newGoal, repo.observeActive().first())
    }
}
