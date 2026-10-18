package com.fundtrail.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.fundtrail.data.local.FundtrailDatabase
import com.fundtrail.data.remote.model.RemoteSnapshot
import com.fundtrail.data.remote.source.CategoryRemoteDataSource
import com.fundtrail.domain.model.Category
import com.fundtrail.domain.model.DefaultCategories
import com.fundtrail.domain.model.IncomeSource
import com.fundtrail.domain.model.Result
import com.fundtrail.domain.model.SpendKind
import com.fundtrail.domain.model.Transaction
import com.fundtrail.domain.model.TransactionStatus
import com.fundtrail.domain.model.TransactionType
import com.fundtrail.fakes.FakeAuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

/** suggestedFor against a real in-memory Room database: the SQL, not just the ranking, must follow the rules. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class CategorySuggestionRepositoryTest {

    private val remote = object : CategoryRemoteDataSource {
        override fun observeCategories(uid: String): Flow<RemoteSnapshot<Category>> = emptyFlow()
        override fun addCategory(uid: String, category: Category): Result<String> = error("unused")
        override fun updateCategory(uid: String, category: Category): Result<Unit> = error("unused")
        override fun updateCategory(uid: String, categoryId: String, updates: Map<String, Any?>): Result<Unit> = error("unused")
        override fun deleteCategory(uid: String, categoryId: String): Result<Unit> = error("unused")
    }

    // A fixed "now" keeps the 30-day window deterministic (FR-33).
    private val now = 1_800_000_000_000L
    private val dayMillis = 24 * 60 * 60 * 1000L
    private val clock = Clock.fixed(Instant.ofEpochMilli(now), ZoneOffset.UTC)

    private lateinit var db: FundtrailDatabase
    private lateinit var repo: CategoryRepositoryImpl

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            FundtrailDatabase::class.java,
        ).allowMainThreadQueries().build()
        val auth = FakeAuthRepository().apply { user.value = "uid-1" }
        repo = CategoryRepositoryImpl(auth, db.categoryDao(), db.syncStateDao(), db.transactionDao(), remote, clock)
        runBlocking { db.categoryDao().upsertAll(DefaultCategories.all.map { it.toEntity() }) }
    }

    @After
    fun tearDown() = db.close()

    private var nextId = 0
    private fun expense(
        account: String,
        category: String,
        at: Long,
        status: TransactionStatus = TransactionStatus.ACTUAL,
    ) = Transaction(
        id = "t${nextId++}", type = TransactionType.EXPENSE, status = status, amountLkrMinor = 1_000,
        accountId = account, categoryId = category, spendKind = SpendKind.DISCRETIONARY, occurredAt = at,
    )

    private fun save(vararg tx: Transaction) = runBlocking { db.transactionDao().upsertAll(tx.map { it.toEntity() }) }

    private fun suggestion(account: String) = runBlocking { repo.suggestedFor(account).first().first().id }

    @Test
    fun level3_noExpensesAtAllSuggestsTheFirstCategoryBySortOrder() {
        assertEquals("cat_rent", suggestion("acc_main_bank"))
        assertEquals(DefaultCategories.all.map { it.id }, runBlocking { repo.suggestedFor("acc_main_bank").first() }.map { it.id })
    }

    @Test
    fun level2_anExpenseExactly30DaysOldStillCountsButOneMillisecondOlderDoesNot() {
        save(
            expense("acc_cash", "cat_shopping", now - 30 * dayMillis),
            // Used twice, so it would win if the boundary let it in.
            expense("acc_cash", "cat_coffee_dining", now - 30 * dayMillis - 1),
            expense("acc_cash", "cat_coffee_dining", now - 30 * dayMillis - 1),
        )

        assertEquals("cat_shopping", suggestion("acc_main_bank"))
    }

    @Test
    fun level2_accountWithoutHistoryGetsTheMostUsedCategoryOverall() {
        save(
            expense("acc_cash", "cat_coffee_dining", now - 3 * dayMillis),
            expense("acc_cash", "cat_coffee_dining", now - 2 * dayMillis),
            expense("acc_cash", "cat_shopping", now - dayMillis),
        )

        assertEquals("cat_coffee_dining", suggestion("acc_main_bank"))
    }

    @Test
    fun level2_countsOnlyTheLast30DaysSoAnOldHabitDoesNotStayOnTop() {
        save(
            // Used a lot last year, not at all lately.
            expense("acc_cash", "cat_coffee_dining", now - 90 * dayMillis),
            expense("acc_cash", "cat_coffee_dining", now - 91 * dayMillis),
            expense("acc_cash", "cat_coffee_dining", now - 92 * dayMillis),
            expense("acc_cash", "cat_shopping", now - 2 * dayMillis),
        )

        assertEquals("cat_shopping", suggestion("acc_main_bank"))
    }

    @Test
    fun level1_accountWithHistoryGetsItsMostRecentCategoryEvenIfRarelyUsedOverall() {
        save(
            expense("acc_cash", "cat_coffee_dining", 1),
            expense("acc_cash", "cat_coffee_dining", 2),
            expense("acc_cash", "cat_coffee_dining", 3),
            expense("acc_main_bank", "cat_ride_hailing", 10),
        )

        assertEquals("cat_ride_hailing", suggestion("acc_main_bank"))
        assertEquals("cat_coffee_dining", suggestion("acc_cash"))
    }

    @Test
    fun level1_usesTheNewestEntryByOccurredAtNotByInsertOrder() {
        save(
            expense("acc_main_bank", "cat_shopping", 500), // newest, saved first
            expense("acc_main_bank", "cat_gym", 100),
        )

        assertEquals("cat_shopping", suggestion("acc_main_bank"))
    }

    @Test
    fun archivedCategoriesFallThroughToTheNextLevel() {
        save(
            expense("acc_main_bank", "cat_ride_hailing", now - dayMillis),
            expense("acc_cash", "cat_coffee_dining", now - 2 * dayMillis),
        )
        // Sync mirrors the archived flag from Firestore into Room; do the same directly here.
        runBlocking {
            val archived = DefaultCategories.all.first { it.id == "cat_ride_hailing" }.copy(archived = true)
            db.categoryDao().upsertAll(listOf(archived.toEntity()))
        }

        val ranked = runBlocking { repo.suggestedFor("acc_main_bank").first() }

        assertEquals("cat_coffee_dining", ranked.first().id)
        assertEquals(false, ranked.any { it.id == "cat_ride_hailing" })
    }

    @Test
    fun expectedIncomeAndTransferRowsDoNotCount() {
        save(
            expense("acc_main_bank", "cat_gym", 5, status = TransactionStatus.EXPECTED),
            Transaction(
                id = "inc", type = TransactionType.INCOME, amountLkrMinor = 1_000, accountId = "acc_main_bank",
                incomeSource = IncomeSource.SALARY, occurredAt = 6,
            ),
            Transaction(
                id = "trf", type = TransactionType.TRANSFER, amountLkrMinor = 1_000, accountId = "acc_main_bank",
                toAccountId = "acc_cash", occurredAt = 7,
            ),
        )

        assertEquals("cat_rent", suggestion("acc_main_bank"))
    }

    @Test
    fun theSuggestionUpdatesWhenANewExpenseIsSaved() {
        assertEquals("cat_rent", suggestion("acc_main_bank"))

        save(expense("acc_main_bank", "cat_entertainment", 1))

        assertEquals("cat_entertainment", suggestion("acc_main_bank"))
    }
}
