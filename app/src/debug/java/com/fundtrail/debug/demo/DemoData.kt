package com.fundtrail.debug.demo

import com.fundtrail.domain.model.APP_ZONE
import com.fundtrail.domain.model.Goal
import com.fundtrail.domain.model.IncomeSource
import com.fundtrail.domain.model.Invoice
import com.fundtrail.domain.model.InvoiceStatus
import com.fundtrail.domain.model.OriginalAmount
import com.fundtrail.domain.model.RecurringTemplate
import com.fundtrail.domain.model.SpendKind
import com.fundtrail.domain.model.Transaction
import com.fundtrail.domain.model.TransactionStatus
import com.fundtrail.domain.model.TransactionType
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

/** Every id the seeder writes starts with this, so Clear can find exactly what it seeded (#157). */
const val SEED_ID_PREFIX = "seed-"

/** First month of seeded data; Clear sweeps transactions from here to the current month. */
val DEMO_FIRST_MONTH: YearMonth = YearMonth.of(2026, 8)

/** One run of demo data, ready to write through the repositories. */
data class DemoDataSet(
    val invoices: List<Invoice>,
    val recurring: List<RecurringTemplate>,
    val transactions: List<Transaction>,
    val goal: Goal,
)

/**
 * Three months (Aug-Oct 2026) of Kavindu's money, as listed in #157. Pure: the same [run] and [today] always
 * give the same documents, which is what makes a second seed a no-op.
 *
 * [run] is the id prefix of one seeding (`seed-<token>`). Ids are fixed within a run; a new run is only started
 * after Clear, because the rules never let a soft-deleted document be created again under the same id.
 * Anything dated after [today] is either skipped (expenses) or stored as EXPECTED (income), so the demo never
 * shows money that hasn't happened yet as received or spent.
 */
object DemoData {

    private const val MAIN_BANK = "default-main-bank"
    private const val CASH = "default-cash"
    private const val BINANCE = "default-binance"
    private val DUE_MONTH: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMM")

    fun build(run: String, today: LocalDate): DemoDataSet {
        require(run.startsWith(SEED_ID_PREFIX)) { "Run id must start with $SEED_ID_PREFIX" }
        val b = Builder(run, today)

        // Salary, LKR 120k-140k on the 25th (A-01).
        b.income("salary-202608", date(8, 25), lkr(130_000), IncomeSource.SALARY, MAIN_BANK)
        b.income("salary-202609", date(9, 25), lkr(120_000), IncomeSource.SALARY, MAIN_BANK)
        b.income("salary-202610", date(10, 25), lkr(140_000), IncomeSource.SALARY, MAIN_BANK)

        // AdSense in USD, the latest month still an estimate (A-11).
        b.income("adsense-202608", date(8, 12), 0, IncomeSource.ADSENSE, MAIN_BANK, usd(4_820, "298.40"))
        b.income("adsense-202609", date(9, 12), 0, IncomeSource.ADSENSE, MAIN_BANK, usd(5_275, "299.10"))
        // TODO(M3): store as ESTIMATED once #177 adds it; until then an estimate is an EXPECTED entry.
        b.income(
            "adsense-202610", date(10, 12), 0, IncomeSource.ADSENSE, MAIN_BANK, usd(4_500, "300.00"),
            status = TransactionStatus.EXPECTED, note = "AdSense estimate",
        )

        // A crypto gain and a crypto loss (A-12).
        b.income("crypto-gain", date(9, 18), 0, IncomeSource.CRYPTO, BINANCE, usdt(6_150, "300.81"))
        b.income("crypto-loss", date(10, 8), 0, IncomeSource.CRYPTO, BINANCE, usdt(-3_050, "301.60"))

        // Freelance: one invoice received, one overdue (A-07).
        val acmeTx = b.income(
            "inv-acme-received", date(8, 22), lkr(85_000), IncomeSource.FREELANCE, MAIN_BANK,
            note = "Acme Lanka - Website redesign - Milestone 1",
        )
        b.invoices += Invoice(
            id = "$run-inv-acme", client = "Acme Lanka", project = "Website redesign", milestone = "Milestone 1",
            amountMinor = lkr(85_000), currency = "LKR", dueDate = date(8, 20), status = InvoiceStatus.RECEIVED,
            receivedTransactionId = acmeTx?.id, receivedAt = acmeTx?.occurredAt, issuedAt = millis(date(8, 5)),
        )
        b.transactions.replaceAll { if (it.id == acmeTx?.id) it.copy(invoiceId = "$run-inv-acme") else it }
        b.invoices += Invoice(
            id = "$run-inv-globex", client = "Globex", project = "Mobile app", milestone = "Milestone 2",
            amountMinor = 30_000L, currency = "USD", dueDate = date(10, 10), issuedAt = millis(date(9, 20)),
        )

        // Recurring commitments (A-17). Utilities' October bill is left due, so the demo can confirm it.
        b.recurring("rent", "Rent", "cat_rent", lkr(34_000), variable = false, day = 1, paid = listOf(8, 9, 10))
        b.recurring(
            "utilities", "Utilities", "cat_utilities", lkr(7_000), variable = true, day = 10, paid = listOf(8, 9),
            actual = mapOf(8 to lkr(6_850), 9 to lkr(7_420)),
        )
        b.recurring("gym", "Gym", "cat_gym", lkr(5_500), variable = false, day = 5, paid = listOf(8, 9, 10))
        b.recurring("netflix", "Netflix", "cat_subscriptions", lkr(2_990), variable = false, day = 15, paid = listOf(8, 9, 10))
        b.recurring("spotify", "Spotify", "cat_subscriptions", lkr(1_050), variable = false, day = 18, paid = listOf(8, 9, 10))

        // Coffee & dining: September totals LKR 24,000, the A-23 example.
        listOf(
            date(8, 3) to 2_100, date(8, 14) to 3_450, date(8, 27) to 1_900,
            date(9, 2) to 2_450, date(9, 6) to 3_800, date(9, 9) to 1_650, date(9, 13) to 4_200,
            date(9, 17) to 2_900, date(9, 21) to 3_350, date(9, 24) to 1_850, date(9, 28) to 3_800,
            date(10, 4) to 2_650, date(10, 16) to 4_100,
        ).forEachIndexed { i, (day, amount) ->
            b.expense("coffee-$i", day, lkr(amount), "cat_coffee_dining", SpendKind.DISCRETIONARY)
        }

        // PickMe rides.
        listOf(
            date(8, 7) to 650, date(8, 19) to 1_120,
            date(9, 4) to 780, date(9, 15) to 1_340, date(9, 29) to 590,
            date(10, 6) to 870, date(10, 17) to 1_050,
        ).forEachIndexed { i, (day, amount) ->
            b.expense("pickme-$i", day, lkr(amount), "cat_ride_hailing", SpendKind.DISCRETIONARY, note = "PickMe")
        }

        // One transfer to Cash: neither income nor expense.
        b.add(
            Transaction(
                id = "$run-transfer-cash", type = TransactionType.TRANSFER, amountLkrMinor = lkr(15_000),
                accountId = MAIN_BANK, toAccountId = CASH, occurredAt = millis(date(9, 26)), note = "ATM withdrawal",
            )
        )

        val goal = Goal(
            id = "$run-goal-macbook", name = "MacBook Pro M4", targetLkrMinor = lkr(490_000),
            deadline = LocalDate.of(2026, 12, 31), baselineSavedMinor = lkr(11_200),
            // Back-dated start, so August and September count as complete months of pace (ADR-0006).
            createdAt = millis(date(8, 1)),
        )

        return DemoDataSet(b.invoices.toList(), b.recurring.toList(), b.transactions.toList(), goal)
    }

    private class Builder(val run: String, val today: LocalDate) {
        val invoices = mutableListOf<Invoice>()
        val recurring = mutableListOf<RecurringTemplate>()
        val transactions = mutableListOf<Transaction>()

        fun add(tx: Transaction): Transaction? {
            if (java.time.Instant.ofEpochMilli(tx.occurredAt).atZone(APP_ZONE).toLocalDate().isAfter(today)) {
                if (tx.type != TransactionType.INCOME) return null
                return tx.copy(status = TransactionStatus.EXPECTED).also { transactions += it }
            }
            return tx.also { transactions += it }
        }

        fun income(
            key: String,
            date: LocalDate,
            lkrMinor: Long,
            source: IncomeSource,
            account: String,
            original: OriginalAmount? = null,
            status: TransactionStatus = TransactionStatus.ACTUAL,
            note: String? = null,
        ): Transaction? = add(
            Transaction(
                id = "$run-$key", type = TransactionType.INCOME, status = status,
                amountLkrMinor = original?.let(::lkrOf) ?: lkrMinor, accountId = account, incomeSource = source,
                occurredAt = millis(date), original = original, note = note,
            )
        )

        fun expense(key: String, date: LocalDate, lkrMinor: Long, category: String, kind: SpendKind, note: String? = null) {
            add(
                Transaction(
                    id = "$run-$key", type = TransactionType.EXPENSE, amountLkrMinor = lkrMinor, accountId = MAIN_BANK,
                    categoryId = category, spendKind = kind, occurredAt = millis(date), note = note,
                )
            )
        }

        /** A template plus the transactions already confirmed from it, with ids as confirm would write them. */
        fun recurring(
            key: String,
            name: String,
            category: String,
            expected: Long,
            variable: Boolean,
            day: Int,
            paid: List<Int>,
            actual: Map<Int, Long> = emptyMap(),
        ) {
            val id = "$run-rec-$key"
            val lastPaid = paid.maxOrNull()
            recurring += RecurringTemplate(
                id = id, name = name, categoryId = category, accountId = MAIN_BANK, expectedAmountMinor = expected,
                isVariableAmount = variable, dayOfMonth = day,
                nextDueDate = if (lastPaid == null) date(8, day) else date(lastPaid, day).plusMonths(1),
            )
            paid.forEach { month ->
                val due = date(month, day)
                add(
                    Transaction(
                        id = "${id}_${due.format(DUE_MONTH)}", type = TransactionType.EXPENSE,
                        amountLkrMinor = actual[month] ?: expected, accountId = MAIN_BANK, categoryId = category,
                        spendKind = SpendKind.COMMITTED, occurredAt = millis(due), recurringId = id,
                        expectedAmountLkrMinor = expected,
                    )
                )
            }
        }
    }

    private fun date(month: Int, day: Int): LocalDate = LocalDate.of(2026, month, day)

    private fun millis(date: LocalDate): Long = date.atTime(10, 0).atZone(APP_ZONE).toInstant().toEpochMilli()

    private fun lkr(rupees: Int): Long = rupees * 100L

    private fun usd(cents: Long, rate: String) = OriginalAmount("USD", cents, micros(rate))

    private fun usdt(cents: Long, rate: String) = OriginalAmount("USDT", cents, micros(rate))

    private fun micros(rate: String): Long = BigDecimal(rate).movePointRight(6).longValueExact()

    /** LKR value with the locked rate (ADR-0003): original minor units x rate, rounded half-up. */
    private fun lkrOf(original: OriginalAmount): Long =
        BigDecimal(original.amountMinor).multiply(BigDecimal(original.fxRateMicros))
            .movePointLeft(6).setScale(0, RoundingMode.HALF_UP).longValueExact()
}
