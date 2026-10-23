package com.fundtrail.domain.usecase.income

import com.fundtrail.domain.model.APP_ZONE
import com.fundtrail.domain.model.IncomeSource
import com.fundtrail.domain.model.IncomeVsEstimateComparison
import com.fundtrail.domain.model.Transaction
import com.fundtrail.domain.model.TransactionStatus
import com.fundtrail.domain.model.TransactionType
import com.fundtrail.domain.model.UserSettings
import com.fundtrail.fakes.FakeSettingsRepository
import com.fundtrail.fakes.FakeTransactionRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Clock
import java.time.YearMonth

class GetIncomeVsEstimateUseCaseTest {

    private val oct2026 = YearMonth.of(2026, 10)
    private val clock = Clock.fixed(
        oct2026.atDay(15).atStartOfDay(APP_ZONE).toInstant(),
        APP_ZONE,
    )

    private val minEstimate = 16_000_000L // LKR 160,000.00
    private val maxEstimate = 21_000_000L // LKR 210,000.00
    private val defaultSettings = UserSettings(
        incomeEstimateMinMinor = minEstimate,
        incomeEstimateMaxMinor = maxEstimate,
        createdAt = oct2026.minusMonths(6).atDay(1).atStartOfDay(APP_ZONE).toInstant().toEpochMilli(),
    )

    @Test
    fun compute_whenActualBelowEstimateRange_returnsBelow() {
        val m1 = listOf(incomeTx("t1", 14_000_000L))
        val m2 = listOf(incomeTx("t2", 15_000_000L))
        val m3 = listOf(incomeTx("t3", 15_500_000L))

        val result = GetIncomeVsEstimateUseCase.compute(
            settings = defaultSettings,
            month1Txs = m1,
            month2Txs = m2,
            month3Txs = m3,
            currentMonth = oct2026,
        )

        assertEquals(IncomeVsEstimateComparison.BELOW, result.comparison)
        assertEquals(14_833_333L, result.actualAverageMinor)
        assertEquals(3, result.monthsCount)
    }

    @Test
    fun compute_whenActualWithinEstimateRange_returnsWithin() {
        val m1 = listOf(incomeTx("t1", 18_000_000L))
        val m2 = listOf(incomeTx("t2", 17_000_000L))
        val m3 = listOf(incomeTx("t3", 19_000_000L))

        val result = GetIncomeVsEstimateUseCase.compute(
            settings = defaultSettings,
            month1Txs = m1,
            month2Txs = m2,
            month3Txs = m3,
            currentMonth = oct2026,
        )

        assertEquals(IncomeVsEstimateComparison.WITHIN, result.comparison)
        assertEquals(18_000_000L, result.actualAverageMinor)
        assertEquals(3, result.monthsCount)
    }

    @Test
    fun compute_whenActualAboveEstimateRange_returnsAbove() {
        val m1 = listOf(incomeTx("t1", 22_000_000L))
        val m2 = listOf(incomeTx("t2", 23_000_000L))
        val m3 = listOf(incomeTx("t3", 21_000_000L))

        val result = GetIncomeVsEstimateUseCase.compute(
            settings = defaultSettings,
            month1Txs = m1,
            month2Txs = m2,
            month3Txs = m3,
            currentMonth = oct2026,
        )

        assertEquals(IncomeVsEstimateComparison.ABOVE, result.comparison)
        assertEquals(22_000_000L, result.actualAverageMinor)
        assertEquals(3, result.monthsCount)
    }

    @Test
    fun compute_fewerThan3CompleteMonths_2Months() {
        val userCreatedInM2 = oct2026.minusMonths(2).atDay(1).atStartOfDay(APP_ZONE).toInstant().toEpochMilli()
        val settings = defaultSettings.copy(createdAt = userCreatedInM2)

        val m1 = listOf(incomeTx("t1", 16_000_000L))
        val m2 = listOf(incomeTx("t2", 18_000_000L))
        val m3 = listOf(incomeTx("t3", 20_000_000L)) // Before user created, ignored

        val result = GetIncomeVsEstimateUseCase.compute(
            settings = settings,
            month1Txs = m1,
            month2Txs = m2,
            month3Txs = m3,
            currentMonth = oct2026,
        )

        assertEquals(2, result.monthsCount)
        assertEquals(17_000_000L, result.actualAverageMinor)
        assertEquals(IncomeVsEstimateComparison.WITHIN, result.comparison)
    }

    @Test
    fun compute_fewerThan3CompleteMonths_1Month() {
        val userCreatedInM1 = oct2026.minusMonths(1).atDay(1).atStartOfDay(APP_ZONE).toInstant().toEpochMilli()
        val settings = defaultSettings.copy(createdAt = userCreatedInM1)

        val m1 = listOf(incomeTx("t1", 15_000_000L))
        val m2 = listOf(incomeTx("t2", 18_000_000L))

        val result = GetIncomeVsEstimateUseCase.compute(
            settings = settings,
            month1Txs = m1,
            month2Txs = m2,
            month3Txs = emptyList(),
            currentMonth = oct2026,
        )

        assertEquals(1, result.monthsCount)
        assertEquals(15_000_000L, result.actualAverageMinor)
        assertEquals(IncomeVsEstimateComparison.BELOW, result.comparison)
    }

    @Test
    fun compute_fewerThan3CompleteMonths_0Months() {
        val userCreatedInM0 = oct2026.atDay(10).atStartOfDay(APP_ZONE).toInstant().toEpochMilli()
        val settings = defaultSettings.copy(createdAt = userCreatedInM0)

        val result = GetIncomeVsEstimateUseCase.compute(
            settings = settings,
            month1Txs = emptyList(),
            month2Txs = emptyList(),
            month3Txs = emptyList(),
            currentMonth = oct2026,
        )

        assertEquals(0, result.monthsCount)
        assertEquals(0L, result.actualAverageMinor)
    }

    @Test
    fun compute_gapTolerant_zeroIncomeMonthCounted() {
        val m1 = listOf(incomeTx("t1", 15_000_000L))
        val m2 = emptyList<Transaction>() // 0 income
        val m3 = listOf(incomeTx("t3", 15_000_000L))

        val result = GetIncomeVsEstimateUseCase.compute(
            settings = defaultSettings,
            month1Txs = m1,
            month2Txs = m2,
            month3Txs = m3,
            currentMonth = oct2026,
        )

        assertEquals(3, result.monthsCount)
        assertEquals(10_000_000L, result.actualAverageMinor) // (15m + 0 + 15m) / 3 = 10m
        assertEquals(IncomeVsEstimateComparison.BELOW, result.comparison)
    }

    @Test
    fun compute_whenNoEstimateConfigured_returnsNullComparison() {
        val settings = defaultSettings.copy(
            incomeEstimateMinMinor = null,
            incomeEstimateMaxMinor = null,
        )

        val result = GetIncomeVsEstimateUseCase.compute(
            settings = settings,
            month1Txs = listOf(incomeTx("t1", 18_000_000L)),
            month2Txs = emptyList(),
            month3Txs = emptyList(),
            currentMonth = oct2026,
        )

        assertNull(result.comparison)
        assertEquals(6_000_000L, result.actualAverageMinor) // Average still calculated: 18m / 3 months
        assertNull(result.estimateMinMinor)
        assertNull(result.estimateMaxMinor)
    }

    @Test
    fun compute_whenActualEqualsMinEstimate_returnsWithin() {
        val result = GetIncomeVsEstimateUseCase.compute(
            settings = defaultSettings,
            month1Txs = listOf(incomeTx("t1", 16_000_000L)),
            month2Txs = listOf(incomeTx("t2", 16_000_000L)),
            month3Txs = listOf(incomeTx("t3", 16_000_000L)),
            currentMonth = oct2026,
        )

        assertEquals(IncomeVsEstimateComparison.WITHIN, result.comparison)
        assertEquals(16_000_000L, result.actualAverageMinor)
    }

    @Test
    fun compute_whenActualEqualsMaxEstimate_returnsWithin() {
        val result = GetIncomeVsEstimateUseCase.compute(
            settings = defaultSettings,
            month1Txs = listOf(incomeTx("t1", 21_000_000L)),
            month2Txs = listOf(incomeTx("t2", 21_000_000L)),
            month3Txs = listOf(incomeTx("t3", 21_000_000L)),
            currentMonth = oct2026,
        )

        assertEquals(IncomeVsEstimateComparison.WITHIN, result.comparison)
        assertEquals(21_000_000L, result.actualAverageMinor)
    }

    @Test
    fun compute_oneSidedEstimate_minOnly() {
        val settings = defaultSettings.copy(incomeEstimateMinMinor = 16_000_000L, incomeEstimateMaxMinor = null)

        val belowResult = GetIncomeVsEstimateUseCase.compute(
            settings = settings,
            month1Txs = listOf(incomeTx("t1", 15_000_000L)),
            month2Txs = listOf(incomeTx("t2", 15_000_000L)),
            month3Txs = listOf(incomeTx("t3", 15_000_000L)),
            currentMonth = oct2026,
        )
        assertEquals(IncomeVsEstimateComparison.BELOW, belowResult.comparison)

        val withinResult = GetIncomeVsEstimateUseCase.compute(
            settings = settings,
            month1Txs = listOf(incomeTx("t1", 17_000_000L)),
            month2Txs = listOf(incomeTx("t2", 17_000_000L)),
            month3Txs = listOf(incomeTx("t3", 17_000_000L)),
            currentMonth = oct2026,
        )
        assertEquals(IncomeVsEstimateComparison.WITHIN, withinResult.comparison)
    }

    @Test
    fun compute_oneSidedEstimate_maxOnly() {
        val settings = defaultSettings.copy(incomeEstimateMinMinor = null, incomeEstimateMaxMinor = 21_000_000L)

        val aboveResult = GetIncomeVsEstimateUseCase.compute(
            settings = settings,
            month1Txs = listOf(incomeTx("t1", 22_000_000L)),
            month2Txs = listOf(incomeTx("t2", 22_000_000L)),
            month3Txs = listOf(incomeTx("t3", 22_000_000L)),
            currentMonth = oct2026,
        )
        assertEquals(IncomeVsEstimateComparison.ABOVE, aboveResult.comparison)

        val withinResult = GetIncomeVsEstimateUseCase.compute(
            settings = settings,
            month1Txs = listOf(incomeTx("t1", 20_000_000L)),
            month2Txs = listOf(incomeTx("t2", 20_000_000L)),
            month3Txs = listOf(incomeTx("t3", 20_000_000L)),
            currentMonth = oct2026,
        )
        assertEquals(IncomeVsEstimateComparison.WITHIN, withinResult.comparison)
    }

    @Test
    fun compute_createdAtNullFallback_detectsAvailableMonths() {
        val settings = defaultSettings.copy(createdAt = null)

        val result3 = GetIncomeVsEstimateUseCase.compute(
            settings = settings,
            month1Txs = emptyList(),
            month2Txs = emptyList(),
            month3Txs = listOf(incomeTx("t3", 15_000_000L)),
            currentMonth = oct2026,
        )
        assertEquals(3, result3.monthsCount)

        val result2 = GetIncomeVsEstimateUseCase.compute(
            settings = settings,
            month1Txs = emptyList(),
            month2Txs = listOf(incomeTx("t2", 15_000_000L)),
            month3Txs = emptyList(),
            currentMonth = oct2026,
        )
        assertEquals(2, result2.monthsCount)

        val result1 = GetIncomeVsEstimateUseCase.compute(
            settings = settings,
            month1Txs = listOf(incomeTx("t1", 15_000_000L)),
            month2Txs = emptyList(),
            month3Txs = emptyList(),
            currentMonth = oct2026,
        )
        assertEquals(1, result1.monthsCount)
    }

    @Test
    fun compute_excludesExpectedTransferAndDeletedTransactions() {
        val expectedIncome = incomeTx("t1", 18_000_000L).copy(status = TransactionStatus.EXPECTED)
        val transferTx = Transaction(
            id = "t2",
            type = TransactionType.TRANSFER,
            status = TransactionStatus.ACTUAL,
            amountLkrMinor = 18_000_000L,
            accountId = "acc1",
            toAccountId = "acc2",
            occurredAt = oct2026.minusMonths(1).atDay(10).atStartOfDay(APP_ZONE).toInstant().toEpochMilli(),
        )
        val deletedIncome = incomeTx("t3", 18_000_000L).copy(deletedAt = 1000L)
        val actualIncome = incomeTx("t4", 18_000_000L)

        val result = GetIncomeVsEstimateUseCase.compute(
            settings = defaultSettings,
            month1Txs = listOf(expectedIncome, transferTx, deletedIncome, actualIncome),
            month2Txs = emptyList(),
            month3Txs = emptyList(),
            currentMonth = oct2026,
        )

        assertEquals(6_000_000L, result.actualAverageMinor) // Only actualIncome (18m / 3)
    }

    @Test
    fun invoke_combinesFlowsFromRepositories() = runTest {
        val settingsRepo = FakeSettingsRepository()
        settingsRepo.update(defaultSettings)
        val txRepo = FakeTransactionRepository()

        val m1Tx = incomeTx("tx1", 18_000_000L, occurredAt = oct2026.minusMonths(1))
        txRepo.add(m1Tx)

        val useCase = GetIncomeVsEstimateUseCase(settingsRepo, txRepo, clock)
        val result = useCase(oct2026).first()

        assertEquals(minEstimate, result.estimateMinMinor)
        assertEquals(maxEstimate, result.estimateMaxMinor)
    }

    private fun incomeTx(id: String, amountMinor: Long, occurredAt: YearMonth = oct2026.minusMonths(1)) = Transaction(
        id = id,
        type = TransactionType.INCOME,
        status = TransactionStatus.ACTUAL,
        amountLkrMinor = amountMinor,
        accountId = "acc1",
        incomeSource = IncomeSource.FREELANCE,
        occurredAt = occurredAt.atDay(10).atStartOfDay(APP_ZONE).toInstant().toEpochMilli(),
    )
}
