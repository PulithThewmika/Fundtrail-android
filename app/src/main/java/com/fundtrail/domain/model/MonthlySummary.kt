package com.fundtrail.domain.model

/**
 * One month's actual totals in LKR minor units (schema §6.2: only ACTUAL counts, transfers never do).
 *
 * @property receivedIncomeMinor Sum of actual income; a crypto loss lowers it.
 * @property netSavedMinor `receivedIncomeMinor - spentMinor`; negative when the month overspent.
 * @property incomeBySource Actual income per source, only sources with entries.
 */
data class MonthlySummary(
    val receivedIncomeMinor: Long,
    val spentMinor: Long,
    val netSavedMinor: Long,
    val incomeBySource: Map<IncomeSource, Long>,
)

/**
 * One month's actual spending split by the nature stored on each expense.
 * Percentages are shares of [totalSpentMinor], 0–100.
 */
data class SpendingBreakdown(
    val totalSpentMinor: Long,
    val byNature: List<NatureShare>,
    val categories: List<CategoryShare>,
)

data class NatureShare(val spendKind: SpendKind, val amountMinor: Long, val percent: Double)

/** A category's spend under one nature; a category whose entries override the nature appears once per nature. */
data class CategoryShare(val categoryId: String, val spendKind: SpendKind, val amountMinor: Long, val percent: Double)
