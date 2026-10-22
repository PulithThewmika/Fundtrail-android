package com.fundtrail.domain.usecase.income

import com.fundtrail.domain.model.APP_ZONE
import com.fundtrail.domain.model.IncomeSource
import com.fundtrail.domain.model.Transaction
import com.fundtrail.domain.model.TransactionStatus
import com.fundtrail.domain.model.TransactionType
import com.fundtrail.domain.model.toMillisRange
import com.fundtrail.fakes.FakeTransactionRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Clock
import java.time.YearMonth

class ObserveCryptoYtdUseCaseTest {

    private val transactionRepo = FakeTransactionRepository()
    private val nov2026 = YearMonth.of(2026, 11)
    private val fixedClock = Clock.fixed(nov2026.atDay(1).atStartOfDay(APP_ZONE).toInstant(), APP_ZONE)

    private val useCase = ObserveCryptoYtdUseCase(
        transactionRepository = transactionRepo,
        clock = fixedClock
    )

    @Test
    fun `calculates net realised crypto results for the year`() = runTest {
        val feb2026 = YearMonth.of(2026, 2)
        val sep2026 = YearMonth.of(2026, 9)

        // FR-55: Entering +80,000 and -30,000 shows a net of +50,000
        transactionRepo.add(
            Transaction(
                id = "crypto_gain_feb",
                type = TransactionType.INCOME,
                status = TransactionStatus.ACTUAL,
                amountLkrMinor = 8_000_000L, // +80,000 LKR
                accountId = "acc_binance",
                incomeSource = IncomeSource.CRYPTO,
                occurredAt = feb2026.toMillisRange().first + 3600000L
            )
        )
        transactionRepo.add(
            Transaction(
                id = "crypto_loss_sep",
                type = TransactionType.INCOME,
                status = TransactionStatus.ACTUAL,
                amountLkrMinor = -3_000_000L, // -30,000 LKR
                accountId = "acc_binance",
                incomeSource = IncomeSource.CRYPTO,
                occurredAt = sep2026.toMillisRange().first + 3600000L
            )
        )

        // Kavindu sample data also contains tx_crypto_loss_001 in Oct 2026 (-372_124L)
        val result = useCase(nov2026).first()
        val expectedNet = 8_000_000L - 3_000_000L - 372_124L
        assertEquals(expectedNet, result.netLkrMinor)
        assertEquals(3, result.transactionCount)
    }

    @Test
    fun `excludes expected crypto transactions and other income sources`() = runTest {
        val nov2026 = YearMonth.of(2026, 11)
        val (start, _) = nov2026.toMillisRange()

        transactionRepo.add(
            Transaction(
                id = "crypto_expected",
                type = TransactionType.INCOME,
                status = TransactionStatus.EXPECTED,
                amountLkrMinor = 5_000_000L,
                accountId = "acc_binance",
                incomeSource = IncomeSource.CRYPTO,
                occurredAt = start + 1000L
            )
        )

        transactionRepo.add(
            Transaction(
                id = "freelance_income",
                type = TransactionType.INCOME,
                status = TransactionStatus.ACTUAL,
                amountLkrMinor = 5_000_000L,
                accountId = "acc_main_bank",
                incomeSource = IncomeSource.FREELANCE,
                occurredAt = start + 2000L
            )
        )

        val result = useCase(nov2026).first()
        // Only the Kavindu sample tx in Oct (-372_124L) is counted
        assertEquals(-372_124L, result.netLkrMinor)
    }
}
