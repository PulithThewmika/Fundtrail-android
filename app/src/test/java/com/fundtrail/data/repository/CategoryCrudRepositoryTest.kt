package com.fundtrail.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.fundtrail.data.local.FundtrailDatabase
import com.fundtrail.data.remote.model.RemoteSnapshot
import com.fundtrail.data.remote.source.CategoryRemoteDataSource
import com.fundtrail.domain.model.Category
import com.fundtrail.domain.model.DefaultCategories
import com.fundtrail.domain.model.Result
import com.fundtrail.domain.model.SpendKind
import com.fundtrail.domain.model.Transaction
import com.fundtrail.domain.model.TransactionType
import com.fundtrail.fakes.FakeAuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Clock

/**
 * EPIC-008 S2: add with a nature, rename, toggle nature, archive. The remote fake records what would go to
 * Firestore; the tests copy it into Room the way SyncManager does.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class CategoryCrudRepositoryTest {

    private val added = mutableListOf<Category>()
    private val updates = mutableListOf<Pair<String, Map<String, Any?>>>()
    private val remote = object : CategoryRemoteDataSource {
        override fun observeCategories(uid: String): Flow<RemoteSnapshot<Category>> = emptyFlow()
        override fun addCategory(uid: String, category: Category): Result<String> {
            added += category
            return Result.Success(category.id)
        }
        override fun updateCategory(uid: String, category: Category): Result<Unit> = error("unused")
        override fun updateCategory(uid: String, categoryId: String, updates: Map<String, Any?>): Result<Unit> {
            this@CategoryCrudRepositoryTest.updates += categoryId to updates
            return Result.Success(Unit)
        }
        override fun deleteCategory(uid: String, categoryId: String): Result<Unit> = error("unused")
    }

    private lateinit var db: FundtrailDatabase
    private lateinit var repo: CategoryRepositoryImpl
    private val rent = DefaultCategories.all.first { it.id == "cat_rent" }

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            FundtrailDatabase::class.java,
        ).allowMainThreadQueries().build()
        val auth = FakeAuthRepository().apply { user.value = "uid-1" }
        repo = CategoryRepositoryImpl(auth, db.categoryDao(), db.syncStateDao(), db.transactionDao(), remote, Clock.systemUTC())
        runBlocking { db.categoryDao().upsertAll(DefaultCategories.all.map { it.toEntity() }) }
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun addsACustomCategoryWithItsNature() = runBlocking {
        val custom = Category(id = "cat_custom", name = "Pets", defaultSpendKind = SpendKind.COMMITTED, icon = "payments", sortOrder = 13)

        assertTrue(repo.add(custom) is Result.Success)

        assertEquals(listOf(custom), added)
    }

    @Test
    fun rejectsABlankNameOrIcon() = runBlocking {
        assertTrue(repo.add(rent.copy(id = "x", name = " ")) is Result.Error)
        assertTrue(repo.add(rent.copy(id = "x", icon = "")) is Result.Error)
        assertTrue(added.isEmpty())
    }

    @Test
    fun renameSendsOnlyTheName() = runBlocking {
        assertTrue(repo.update(rent.copy(name = "Housing")) is Result.Success)

        assertEquals(listOf("cat_rent" to mapOf<String, Any?>("name" to "Housing")), updates)
    }

    @Test
    fun togglingTheNatureSendsOnlyTheNature() = runBlocking {
        assertTrue(repo.update(rent.copy(defaultSpendKind = SpendKind.DISCRETIONARY)) is Result.Success)

        assertEquals(listOf("cat_rent" to mapOf<String, Any?>("defaultSpendKind" to "DISCRETIONARY")), updates)
    }

    @Test
    fun savingWithoutChangesWritesNothing() = runBlocking {
        assertTrue(repo.update(rent) is Result.Success)

        assertTrue(updates.isEmpty())
    }

    @Test
    fun archiveSetsTheArchivedFlag() = runBlocking {
        assertTrue(repo.archive("cat_rent") is Result.Success)

        assertEquals(listOf("cat_rent" to mapOf<String, Any?>("archived" to true)), updates)
    }

    @Test
    fun archivedCategoryIsHiddenFromEntryListsButStillThereForPastEntries() = runBlocking {
        db.categoryDao().upsertAll(listOf(rent.copy(archived = true).toEntity())) // what sync does after the archive

        assertFalse(repo.observeActive().first().any { it.id == "cat_rent" })
        assertFalse(repo.suggestedFor("acc_main_bank").first().any { it.id == "cat_rent" })
        val everything = repo.observeAll().first()
        assertTrue(everything.single { it.id == "cat_rent" }.archived)
        assertEquals(12, everything.size)
    }

    @Test
    fun changingACategorysNatureDoesNotTouchEntriesAlreadySaved() = runBlocking {
        val pastExpense = Transaction(
            id = "past", type = TransactionType.EXPENSE, amountLkrMinor = 34_000_00, accountId = "acc_main_bank",
            categoryId = "cat_rent", spendKind = SpendKind.COMMITTED, occurredAt = 1,
        )
        db.transactionDao().upsertAll(listOf(pastExpense.toEntity()))

        db.categoryDao().upsertAll(listOf(rent.copy(defaultSpendKind = SpendKind.DISCRETIONARY).toEntity())) // sync after the toggle

        val stored = db.transactionDao().observeById("past").first()!!.toDomain()
        assertEquals(SpendKind.COMMITTED, stored.spendKind)
        assertEquals(SpendKind.DISCRETIONARY, repo.observeAll().first().single { it.id == "cat_rent" }.defaultSpendKind)
    }
}
