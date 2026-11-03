package com.fundtrail.debug.demo

import com.fundtrail.domain.model.APP_ZONE
import com.fundtrail.domain.model.Goal
import com.fundtrail.domain.model.Result
import com.fundtrail.domain.repository.GoalRepository
import com.fundtrail.fakes.FakeAccountRepository
import com.fundtrail.fakes.FakeCategoryRepository
import com.fundtrail.fakes.FakeInvoiceRepository
import com.fundtrail.fakes.FakeRecurringRepository
import com.fundtrail.fakes.FakeTransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth

class DemoDataUseCasesTest {
    private var clock = Clock.fixed(LocalDate.of(2026, 10, 22).atTime(9, 0).atZone(APP_ZONE).toInstant(), APP_ZONE)

    // The fakes reject a second create under an existing id (also a soft-deleted one), like the Firestore rules.
    private val transactions = FakeTransactionRepository()
    private val invoices = FakeInvoiceRepository()
    private val recurring = FakeRecurringRepository()

    /** Like the real repository: only the active goal is observed, and archiving hides it. */
    private val goals = object : GoalRepository {
        val all = MutableStateFlow<List<Goal>>(emptyList())
        override fun observeActive(): Flow<Goal?> = all.map { list -> list.firstOrNull { it.isActive } }
        override suspend fun save(goal: Goal): Result<Unit> {
            all.value = all.value.map { it.copy(isActive = false) }.filterNot { it.id == goal.id } + goal
            return Result.Success(Unit)
        }
        override suspend fun archive(id: String): Result<Unit> {
            all.value = all.value.map { if (it.id == id) it.copy(isActive = false) else it }
            return Result.Success(Unit)
        }
    }

    private fun seed() = SeedDemoDataUseCase(
        FakeAccountRepository(), FakeCategoryRepository(), transactions, invoices, recurring, goals, clock,
    )

    private fun clear() = ClearDemoDataUseCase(transactions, invoices, recurring, goals, clock)

    private suspend fun allTransactionIds(): List<String> =
        generateSequence(YearMonth.of(2026, 1)) { it.plusMonths(1) }.take(12).toList()
            .flatMap { month -> transactions.observeMonth(month).first().map { it.id } }

    private suspend fun seededIds(): List<String> =
        allTransactionIds().filter { it.startsWith(SEED_ID_PREFIX) } +
            invoices.observeAll().first().map { it.id }.filter { it.startsWith(SEED_ID_PREFIX) } +
            recurring.observeAll().first().map { it.id }.filter { it.startsWith(SEED_ID_PREFIX) } +
            listOfNotNull(goals.observeActive().first()?.id?.takeIf { it.startsWith(SEED_ID_PREFIX) })

    private suspend fun runPrefix(): String = seededIds().first().split("-").take(2).joinToString("-")

    @Test
    fun seedWritesTheWholeDataSet() = runTest {
        val written = (seed()() as Result.Success).data

        val ids = seededIds()
        assertEquals(ids.size, written)
        assertTrue(ids.size > 40)
        assertEquals("MacBook Pro M4", goals.observeActive().first()?.name)
    }

    @Test
    fun seedingTwiceCreatesNoDuplicates() = runTest {
        seed()()
        val first = seededIds().sorted()

        val second = seed()()

        assertEquals(Result.Success(0), second)
        assertEquals(first, seededIds().sorted())
    }

    @Test
    fun clearRemovesOnlySeedDocuments() = runTest {
        val before = allTransactionIds().filterNot { it.startsWith(SEED_ID_PREFIX) }
        val invoicesBefore = invoices.observeAll().first().map { it.id }
        seed()()

        val removed = (clear()() as Result.Success).data

        assertTrue(removed > 40)
        assertEquals(emptyList<String>(), seededIds())
        assertEquals(before, allTransactionIds())
        assertEquals(invoicesBefore, invoices.observeAll().first().map { it.id })
        assertNull(goals.observeActive().first())
    }

    @Test
    fun seedingAfterClearStartsANewRunInsteadOfReusingDeletedIds() = runTest {
        seed()()
        val firstRun = runPrefix()
        clear()()
        clock = Clock.offset(clock, java.time.Duration.ofMinutes(1))

        val reseed = seed()()

        assertTrue(reseed is Result.Success && reseed.data > 40)
        assertNotEquals(firstRun, runPrefix())
    }
}
