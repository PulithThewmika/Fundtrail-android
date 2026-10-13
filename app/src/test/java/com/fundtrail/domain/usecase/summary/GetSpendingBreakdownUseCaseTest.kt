package com.fundtrail.domain.usecase.summary

import com.fundtrail.domain.model.CategoryShare
import com.fundtrail.domain.model.NatureShare
import com.fundtrail.domain.model.SpendKind.COMMITTED
import com.fundtrail.domain.model.SpendKind.DISCRETIONARY
import com.fundtrail.domain.model.SpendKind
import com.fundtrail.domain.model.Transaction
import com.fundtrail.domain.model.TransactionStatus
import com.fundtrail.domain.model.TransactionType
import com.fundtrail.fakes.FakeTransactionRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

class GetSpendingBreakdownUseCaseTest {

    private val repo = FakeTransactionRepository()
    private val breakdown = GetSpendingBreakdownUseCase(repo)

    private val oct = YearMonth.of(2026, 10)
    private val nov = YearMonth.of(2026, 11) // empty in the sample data
    private val novDay = LocalDate.of(2026, 11, 3).atStartOfDay(ZoneId.of("Asia/Colombo")).toInstant().toEpochMilli()

    private fun expense(id: String, amount: Long, category: String, kind: SpendKind, status: TransactionStatus = TransactionStatus.ACTUAL) =
        Transaction(id = id, type = TransactionType.EXPENSE, status = status, amountLkrMinor = amount, accountId = "acc_main_bank",
            categoryId = category, spendKind = kind, occurredAt = novDay)

    private suspend fun add(vararg txs: Transaction) = txs.forEach { assertTrue(repo.add(it).isSuccess) }

    @Test
    fun `kavindu sample - rent is committed, coffee and dining is the top discretionary category`() = runTest {
        val b = breakdown(oct).first()

        assertEquals(5_950_000L, b.totalSpentMinor)
        assertEquals(NatureShare(COMMITTED, 3_400_000L, 3_400_000 * 100.0 / 5_950_000), b.byNature[0])
        assertEquals(NatureShare(DISCRETIONARY, 2_550_000L, 2_550_000 * 100.0 / 5_950_000), b.byNature[1])

        assertEquals(listOf("cat_rent", "cat_coffee_dining", "cat_ride_hailing"), b.categories.map { it.categoryId })
        assertEquals(CategoryShare("cat_rent", COMMITTED, 3_400_000L, 3_400_000 * 100.0 / 5_950_000), b.categories[0])
        val topDiscretionary = b.categories.first { it.spendKind == DISCRETIONARY }
        assertEquals("cat_coffee_dining", topDiscretionary.categoryId)
        assertEquals(2_400_000L, topDiscretionary.amountMinor)
        assertEquals(100.0, b.categories.sumOf { it.percent }, 1e-9)
    }

    @Test
    fun `empty month has zero totals, zero percents and no categories`() = runTest {
        val b = breakdown(nov).first()
        assertEquals(0L, b.totalSpentMinor)
        assertEquals(listOf(NatureShare(COMMITTED, 0L, 0.0), NatureShare(DISCRETIONARY, 0L, 0.0)), b.byNature)
        assertTrue(b.categories.isEmpty())
    }

    @Test
    fun `nature comes from each entry's snapshot, so an override splits the category`() = runTest {
        add(
            expense("g1", 300_000L, "cat_groceries", DISCRETIONARY),
            expense("g2", 100_000L, "cat_groceries", COMMITTED), // user overrode the default on this entry
        )
        val b = breakdown(nov).first()
        assertEquals(listOf(NatureShare(COMMITTED, 100_000L, 25.0), NatureShare(DISCRETIONARY, 300_000L, 75.0)), b.byNature)
        assertEquals(
            listOf(
                CategoryShare("cat_groceries", DISCRETIONARY, 300_000L, 75.0),
                CategoryShare("cat_groceries", COMMITTED, 100_000L, 25.0),
            ),
            b.categories,
        )
    }

    @Test
    fun `equal amounts are ranked by category id so the order is stable`() = runTest {
        add(expense("b", 50_000L, "cat_b", DISCRETIONARY), expense("a", 50_000L, "cat_a", DISCRETIONARY))
        assertEquals(listOf("cat_a", "cat_b"), breakdown(nov).first().categories.map { it.categoryId })
    }

    @Test
    fun `expected expenses, income and transfers are excluded`() = runTest {
        add(
            expense("due", 3_400_000L, "cat_rent", COMMITTED, TransactionStatus.EXPECTED),
            expense("paid", 100_000L, "cat_groceries", DISCRETIONARY),
        )
        val b = breakdown(nov).first()
        assertEquals(100_000L, b.totalSpentMinor)
        assertEquals(listOf("cat_groceries"), b.categories.map { it.categoryId })
        // October also holds salary, AdSense, crypto and a transfer: none of them reach the breakdown.
        assertEquals(5_950_000L, breakdown(oct).first().totalSpentMinor)
    }
}
