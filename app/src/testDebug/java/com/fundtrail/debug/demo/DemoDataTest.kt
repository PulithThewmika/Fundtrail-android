package com.fundtrail.debug.demo

import com.fundtrail.domain.model.APP_ZONE
import com.fundtrail.domain.model.IncomeSource
import com.fundtrail.domain.model.InvoiceStatus
import com.fundtrail.domain.model.Transaction
import com.fundtrail.domain.model.TransactionStatus
import com.fundtrail.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth

/** The #157 acceptance list, checked against what the seeder would write. */
class DemoDataTest {
    private val today = LocalDate.of(2026, 10, 22)
    private val data = DemoData.build("seed-test", today)

    private fun Transaction.date() = Instant.ofEpochMilli(occurredAt).atZone(APP_ZONE).toLocalDate()
    private fun income(source: IncomeSource) = data.transactions.filter { it.incomeSource == source }
    private fun inCategory(id: String) = data.transactions.filter { it.categoryId == id }

    @Test
    fun everyDocumentIsValidAndSeedPrefixed() {
        data.transactions.forEach { it.validate() }
        data.invoices.forEach { it.validate() }
        data.recurring.forEach { it.validate() }
        data.goal.validate(today = today, zone = APP_ZONE)
        val ids = data.transactions.map { it.id } + data.invoices.map { it.id } + data.recurring.map { it.id } + data.goal.id
        assertTrue(ids.all { it.startsWith("seed-test-") })
        assertEquals("ids are unique", ids.size, ids.toSet().size)
    }

    @Test
    fun buildIsDeterministic() {
        assertEquals(data, DemoData.build("seed-test", today))
    }

    @Test
    fun salaryOnThe25thBetween120kAnd140k_futureOneExpected() {
        val salary = income(IncomeSource.SALARY)
        assertEquals(3, salary.size)
        assertTrue(salary.all { it.date().dayOfMonth == 25 && it.amountLkrMinor in 12_000_000L..14_000_000L })
        assertEquals(listOf(TransactionStatus.ACTUAL, TransactionStatus.ACTUAL, TransactionStatus.EXPECTED), salary.map { it.status })
    }

    @Test
    fun twoInvoicesOneReceivedOneOverdue() {
        val (received, pending) = data.invoices.partition { it.status == InvoiceStatus.RECEIVED }
        assertEquals(1, received.size)
        val settling = data.transactions.single { it.id == received.single().receivedTransactionId }
        assertEquals(received.single().id, settling.invoiceId)
        assertEquals(TransactionType.INCOME, settling.type)
        assertEquals(1, pending.size)
        assertTrue("overdue", pending.single().dueDate.isBefore(today))
    }

    @Test
    fun adSenseInUsdMonthlyWithOneEstimate() {
        val adsense = income(IncomeSource.ADSENSE)
        assertEquals(3, adsense.size)
        assertTrue(adsense.all { it.original?.currency == "USD" })
        assertEquals(1, adsense.count { it.status == TransactionStatus.EXPECTED })
        // LKR value is the original amount at the locked rate: USD 48.20 x 298.40.
        assertEquals(1_438_288L, adsense.first().amountLkrMinor)
    }

    @Test
    fun aCryptoGainAndACryptoLoss() {
        val crypto = income(IncomeSource.CRYPTO)
        assertEquals(1, crypto.count { it.amountLkrMinor > 0 })
        assertEquals(1, crypto.count { it.amountLkrMinor < 0 })
    }

    @Test
    fun commitmentsRentUtilitiesGymAndTwoSubscriptions() {
        assertTrue(inCategory("cat_rent").all { it.amountLkrMinor == 3_400_000L })
        assertEquals(3, inCategory("cat_rent").size)
        assertEquals(2, inCategory("cat_utilities").map { it.amountLkrMinor }.distinct().size)
        assertEquals(3, inCategory("cat_gym").size)
        assertEquals(2, data.recurring.count { it.categoryId == "cat_subscriptions" })
        // Utilities' October bill is due and unconfirmed, for the M3 end-to-end script (#159).
        assertTrue(data.recurring.single { it.categoryId == "cat_utilities" }.isDue(today))
        assertTrue(data.recurring.filter { it.categoryId != "cat_utilities" }.none { it.isDue(today) })
    }

    @Test
    fun coffeeAndDiningTotals24000InSeptember() {
        val september = inCategory("cat_coffee_dining").filter { YearMonth.from(it.date()) == YearMonth.of(2026, 9) }
        assertEquals(2_400_000L, september.sumOf { it.amountLkrMinor })
    }

    @Test
    fun pickMeRidesAndOneTransferToCash() {
        assertTrue(inCategory("cat_ride_hailing").isNotEmpty())
        val transfer = data.transactions.single { it.type == TransactionType.TRANSFER }
        assertEquals("default-cash", transfer.toAccountId)
    }

    @Test
    fun theMacBookGoalStartsInAugust() {
        assertEquals("MacBook Pro M4", data.goal.name)
        assertEquals(LocalDate.of(2026, 8, 1), Instant.ofEpochMilli(data.goal.createdAt!!).atZone(APP_ZONE).toLocalDate())
    }

    @Test
    fun nothingAfterTodayCountsAsHappened() {
        val future = data.transactions.filter { it.date().isAfter(today) }
        assertTrue(future.all { it.type == TransactionType.INCOME && it.status == TransactionStatus.EXPECTED })
    }
}
