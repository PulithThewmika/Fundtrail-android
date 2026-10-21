package com.fundtrail.debug.demo

import com.fundtrail.domain.model.APP_ZONE
import com.fundtrail.domain.model.Result
import com.fundtrail.domain.repository.AccountRepository
import com.fundtrail.domain.repository.CategoryRepository
import com.fundtrail.domain.repository.GoalRepository
import com.fundtrail.domain.repository.InvoiceRepository
import com.fundtrail.domain.repository.RecurringRepository
import com.fundtrail.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.first
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

/** The seed-owned documents currently in the local mirror. */
private class SeededDocs(
    val transactionIds: Set<String>,
    val invoiceIds: Set<String>,
    val recurringIds: Set<String>,
    val activeGoalId: String?,
) {
    val all: Set<String> get() = transactionIds + invoiceIds + recurringIds + setOfNotNull(activeGoalId)
}

private suspend fun readSeeded(
    transactions: TransactionRepository,
    invoices: InvoiceRepository,
    recurring: RecurringRepository,
    goals: GoalRepository,
    currentMonth: YearMonth,
): SeededDocs {
    val txIds = mutableSetOf<String>()
    var month = DEMO_FIRST_MONTH
    while (!month.isAfter(currentMonth.plusMonths(1))) {
        transactions.observeMonth(month).first().mapTo(txIds) { it.id }
        month = month.plusMonths(1)
    }
    fun Iterable<String>.seeded() = filter { it.startsWith(SEED_ID_PREFIX) }.toSet()
    return SeededDocs(
        transactionIds = txIds.seeded(),
        invoiceIds = invoices.observeAll().first().map { it.id }.seeded(),
        recurringIds = recurring.observeAll().first().map { it.id }.seeded(),
        activeGoalId = goals.observeActive().first()?.id?.takeIf { it.startsWith(SEED_ID_PREFIX) },
    )
}

/**
 * Writes [DemoData] through the repositories, so it syncs to Firestore like real entries (#157). Debug builds only.
 *
 * Idempotent: a document whose id is already in the mirror is skipped, and an existing run's id prefix is
 * reused. Only when nothing seeded is left (first seed, or after Clear) does a new run start.
 */
class SeedDemoDataUseCase @Inject constructor(
    private val accounts: AccountRepository,
    private val categories: CategoryRepository,
    private val transactions: TransactionRepository,
    private val invoices: InvoiceRepository,
    private val recurring: RecurringRepository,
    private val goals: GoalRepository,
    private val clock: Clock,
) {
    /** Returns how many documents were written (0 when everything was already there). */
    suspend operator fun invoke(): Result<Int> {
        // The seeded entries use the default accounts and categories by id.
        (accounts.seedDefaultsIfEmpty() as? Result.Error)?.let { return it }
        (categories.seedDefaultsIfEmpty() as? Result.Error)?.let { return it }

        val today = LocalDate.now(clock.withZone(APP_ZONE))
        val existing = readSeeded(transactions, invoices, recurring, goals, YearMonth.from(today))
        val run = existing.all.firstNotNullOfOrNull { RUN_ID.find(it)?.value }
            ?: SEED_ID_PREFIX + clock.millis().toString(36)
        val data = DemoData.build(run, today)

        var written = 0
        suspend fun write(id: String, present: Set<String>, block: suspend () -> Result<Unit>): Result.Error? {
            if (id in present) return null
            return when (val result = block()) {
                is Result.Success -> { written++; null }
                is Result.Error -> result
            }
        }
        data.invoices.forEach { inv -> write(inv.id, existing.invoiceIds) { invoices.add(inv) }?.let { return it } }
        data.recurring.forEach { t -> write(t.id, existing.recurringIds) { recurring.add(t) }?.let { return it } }
        data.transactions.forEach { tx -> write(tx.id, existing.transactionIds) { transactions.add(tx) }?.let { return it } }
        write(data.goal.id, setOfNotNull(existing.activeGoalId)) { goals.save(data.goal) }?.let { return it }
        return Result.Success(written)
    }

    private companion object {
        val RUN_ID = Regex("^${SEED_ID_PREFIX}[a-z0-9]+")
    }
}

/** Soft-deletes every `seed-` document and archives a seeded goal; nothing else is touched (#157). */
class ClearDemoDataUseCase @Inject constructor(
    private val transactions: TransactionRepository,
    private val invoices: InvoiceRepository,
    private val recurring: RecurringRepository,
    private val goals: GoalRepository,
    private val clock: Clock,
) {
    /** Returns how many documents were removed. */
    suspend operator fun invoke(): Result<Int> {
        val seeded = readSeeded(transactions, invoices, recurring, goals, YearMonth.now(clock.withZone(APP_ZONE)))
        val removals: List<suspend () -> Result<Unit>> =
            seeded.transactionIds.map { id -> suspend { transactions.delete(id) } } +
                seeded.invoiceIds.map { id -> suspend { invoices.delete(id) } } +
                seeded.recurringIds.map { id -> suspend { recurring.delete(id) } } +
                listOfNotNull(seeded.activeGoalId).map { id -> suspend { goals.archive(id) } }
        removals.forEach { remove -> (remove() as? Result.Error)?.let { return it } }
        return Result.Success(removals.size)
    }
}
