package com.fundtrail.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.fundtrail.data.local.FundtrailDatabase
import com.fundtrail.data.remote.model.RemoteSnapshot
import com.fundtrail.data.remote.source.RecurringRemoteDataSource
import com.fundtrail.domain.model.APP_ZONE
import com.fundtrail.domain.model.RecurringTemplate
import com.fundtrail.domain.model.Result
import com.fundtrail.domain.model.SpendKind
import com.fundtrail.domain.model.Transaction
import com.fundtrail.domain.model.TransactionStatus
import com.fundtrail.domain.model.TransactionType
import com.fundtrail.fakes.FakeAuthRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Clock
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class RecurringRepositoryImplTest {

    private val today = LocalDate.of(2026, 10, 25)
    private val clock = Clock.fixed(today.atTime(9, 0).atZone(APP_ZONE).toInstant(), APP_ZONE)

    private val confirms = mutableListOf<Triple<String, Transaction, LocalDate>>()
    private val skips = mutableListOf<Pair<String, LocalDate>>()

    private val remote = object : RecurringRemoteDataSource {
        override fun observeRecurring(uid: String): Flow<RemoteSnapshot<RecurringTemplate>> = error("unused")
        override fun addRecurring(uid: String, template: RecurringTemplate): Result<String> = Result.Success(template.id)
        override fun updateRecurring(uid: String, template: RecurringTemplate): Result<Unit> = Result.Success(Unit)
        override fun pauseRecurring(uid: String, templateId: String): Result<Unit> = Result.Success(Unit)
        override fun deleteRecurring(uid: String, templateId: String): Result<Unit> = Result.Success(Unit)
        override fun confirm(uid: String, templateId: String, transaction: Transaction, nextDueDate: LocalDate): Result<Unit> {
            confirms += Triple(templateId, transaction, nextDueDate)
            return Result.Success(Unit)
        }
        override fun skip(uid: String, templateId: String, nextDueDate: LocalDate): Result<Unit> {
            skips += templateId to nextDueDate
            return Result.Success(Unit)
        }
    }

    private val auth = FakeAuthRepository()
    private lateinit var db: FundtrailDatabase
    private lateinit var repo: RecurringRepositoryImpl

    private fun rent(
        nextDueDate: LocalDate = LocalDate.of(2026, 10, 1),
        isVariableAmount: Boolean = false,
        isPaused: Boolean = false,
    ) = RecurringTemplate(
        id = "rent", name = "Rent", categoryId = "cat_rent", accountId = "acc_bank",
        expectedAmountMinor = 3_400_000L, isVariableAmount = isVariableAmount, dayOfMonth = 1,
        nextDueDate = nextDueDate, isPaused = isPaused,
    )

    private fun mirror(template: RecurringTemplate) = runBlocking {
        db.recurringDao().upsertAll(listOf(template.toEntity()))
    }

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            FundtrailDatabase::class.java
        ).allowMainThreadQueries().build()
        repo = RecurringRepositoryImpl(auth, db.recurringDao(), remote, clock)
        auth.user.value = "u1"
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `confirm writes a committed expense and advances the cursor one month`() = runBlocking {
        mirror(rent())

        val result = repo.confirm("rent", 3_400_000L, 1_000L)

        assertTrue(result is Result.Success)
        val (id, tx, next) = confirms.single()
        assertEquals("rent", id)
        assertEquals(LocalDate.of(2026, 11, 1), next)
        assertEquals(TransactionType.EXPENSE, tx.type)
        assertEquals(TransactionStatus.ACTUAL, tx.status)
        assertEquals(SpendKind.COMMITTED, tx.spendKind)
        assertEquals("rent", tx.recurringId)
        assertEquals("cat_rent", tx.categoryId)
        assertEquals("acc_bank", tx.accountId)
        assertEquals(3_400_000L, tx.expectedAmountLkrMinor)
        assertEquals("rent_202610", tx.id)
    }

    @Test
    fun `confirming twice before Room catches up creates one expense`() = runBlocking {
        // Room still shows the old cursor for the second tap: the listener hasn't delivered the advance yet.
        mirror(rent())

        val first = repo.confirm("rent", 3_400_000L, 1_000L)
        val second = repo.confirm("rent", 3_400_000L, 1_000L)

        assertTrue(first is Result.Success)
        assertTrue(second is Result.Error)
        assertEquals(1, confirms.size)
    }

    @Test
    fun `concurrent confirms create one expense`() = runBlocking {
        mirror(rent())

        val results = (1..5).map { async { repo.confirm("rent", 3_400_000L, 1_000L) } }.awaitAll()

        assertEquals(1, results.count { it is Result.Success })
        assertEquals(1, confirms.size)
    }

    @Test
    fun `an overdue template confirms each missed month in turn, then stops`() = runBlocking {
        mirror(rent(nextDueDate = LocalDate.of(2026, 9, 1)))

        assertTrue(repo.confirm("rent", 3_400_000L, 1L) is Result.Success)
        assertTrue(repo.confirm("rent", 3_400_000L, 2L) is Result.Success)
        assertTrue(repo.confirm("rent", 3_400_000L, 3L) is Result.Error)

        assertEquals(listOf("rent_202609", "rent_202610"), confirms.map { it.second.id })
    }

    @Test
    fun `confirm is rejected before the due date`() = runBlocking {
        mirror(rent(nextDueDate = LocalDate.of(2026, 11, 1)))

        assertTrue(repo.confirm("rent", 3_400_000L, 1_000L) is Result.Error)
        assertTrue(confirms.isEmpty())
    }

    @Test
    fun `confirm is rejected for a paused template`() = runBlocking {
        mirror(rent(isPaused = true))

        assertTrue(repo.confirm("rent", 3_400_000L, 1_000L) is Result.Error)
        assertTrue(confirms.isEmpty())
    }

    @Test
    fun `a fixed amount can't be confirmed with a different amount, a variable one can`() = runBlocking {
        mirror(rent())
        assertTrue(repo.confirm("rent", 3_000_000L, 1_000L) is Result.Error)

        mirror(rent(isVariableAmount = true))
        assertTrue(repo.confirm("rent", 3_000_000L, 1_000L) is Result.Success)
        assertEquals(3_000_000L, confirms.single().second.amountLkrMinor)
    }

    @Test
    fun `skip advances the cursor without a transaction, and only once per due date`() = runBlocking {
        mirror(rent())

        assertTrue(repo.skip("rent") is Result.Success)
        assertTrue(repo.skip("rent") is Result.Error)

        assertEquals(listOf("rent" to LocalDate.of(2026, 11, 1)), skips)
        assertTrue(confirms.isEmpty())
    }

    @Test
    fun `confirm fails for a template not in Room and when signed out`() = runBlocking {
        assertTrue(repo.confirm("missing", 1L, 1L) is Result.Error)

        mirror(rent())
        auth.user.value = null
        assertTrue(repo.confirm("rent", 3_400_000L, 1L) is Result.Error)
        assertTrue(confirms.isEmpty())
    }

    @Test
    fun `add rejects an invalid template before reaching Firestore`() = runBlocking {
        assertTrue(repo.add(rent().copy(dayOfMonth = 31)) is Result.Error)
        assertTrue(repo.add(rent()) is Result.Success)
    }
}
