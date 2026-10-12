package com.fundtrail.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class TransactionTest {

    @Test
    fun `valid expense succeeds validation`() {
        val tx = Transaction(
            id = "tx_01",
            type = TransactionType.EXPENSE,
            amountLkrMinor = 1500_00L,
            accountId = "acc_main",
            categoryId = "cat_food",
            spendKind = SpendKind.DISCRETIONARY,
            occurredAt = System.currentTimeMillis()
        )
        tx.validate()
        assertEquals(1500_00L, tx.amountLkrMinor)
        assertEquals(SpendKind.DISCRETIONARY, tx.spendKind)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `expense missing categoryId fails validation`() {
        val tx = Transaction(
            id = "tx_02",
            type = TransactionType.EXPENSE,
            amountLkrMinor = 1000_00L,
            accountId = "acc_main",
            categoryId = null,
            spendKind = SpendKind.COMMITTED,
            occurredAt = System.currentTimeMillis()
        )
        tx.validate()
    }

    @Test(expected = IllegalArgumentException::class)
    fun `expense missing spendKind fails validation`() {
        val tx = Transaction(
            id = "tx_02b",
            type = TransactionType.EXPENSE,
            amountLkrMinor = 1000_00L,
            accountId = "acc_main",
            categoryId = "cat_food",
            spendKind = null,
            occurredAt = System.currentTimeMillis()
        )
        tx.validate()
    }

    @Test(expected = IllegalArgumentException::class)
    fun `expense with toAccountId fails validation`() {
        val tx = Transaction(
            id = "tx_03",
            type = TransactionType.EXPENSE,
            amountLkrMinor = 1000_00L,
            accountId = "acc_main",
            toAccountId = "acc_sec",
            categoryId = "cat_food",
            spendKind = SpendKind.COMMITTED,
            occurredAt = System.currentTimeMillis()
        )
        tx.validate()
    }

    @Test(expected = IllegalArgumentException::class)
    fun `expense with incomeSource fails validation`() {
        val tx = Transaction(
            id = "tx_04",
            type = TransactionType.EXPENSE,
            amountLkrMinor = 1000_00L,
            accountId = "acc_main",
            categoryId = "cat_food",
            spendKind = SpendKind.COMMITTED,
            incomeSource = IncomeSource.SALARY,
            occurredAt = System.currentTimeMillis()
        )
        tx.validate()
    }

    @Test(expected = IllegalArgumentException::class)
    fun `expense with non-positive amount fails validation`() {
        val tx = Transaction(
            id = "tx_05",
            type = TransactionType.EXPENSE,
            amountLkrMinor = 0L,
            accountId = "acc_main",
            categoryId = "cat_food",
            spendKind = SpendKind.COMMITTED,
            occurredAt = System.currentTimeMillis()
        )
        tx.validate()
    }

    @Test
    fun `valid transfer succeeds validation`() {
        val tx = Transaction(
            id = "tx_06",
            type = TransactionType.TRANSFER,
            status = TransactionStatus.ACTUAL,
            amountLkrMinor = 5000_00L,
            accountId = "acc_main",
            toAccountId = "acc_sec",
            occurredAt = System.currentTimeMillis()
        )
        tx.validate()
        assertNull(tx.categoryId)
        assertNull(tx.incomeSource)
        assertNull(tx.spendKind)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `transfer with expected status fails validation`() {
        val tx = Transaction(
            id = "tx_06b",
            type = TransactionType.TRANSFER,
            status = TransactionStatus.EXPECTED,
            amountLkrMinor = 5000_00L,
            accountId = "acc_main",
            toAccountId = "acc_sec",
            occurredAt = System.currentTimeMillis()
        )
        tx.validate()
    }

    @Test(expected = IllegalArgumentException::class)
    fun `transfer with spendKind fails validation`() {
        val tx = Transaction(
            id = "tx_06c",
            type = TransactionType.TRANSFER,
            amountLkrMinor = 5000_00L,
            accountId = "acc_main",
            toAccountId = "acc_sec",
            spendKind = SpendKind.COMMITTED,
            occurredAt = System.currentTimeMillis()
        )
        tx.validate()
    }

    @Test(expected = IllegalArgumentException::class)
    fun `transfer with same source and destination fails validation`() {
        val tx = Transaction(
            id = "tx_07",
            type = TransactionType.TRANSFER,
            amountLkrMinor = 5000_00L,
            accountId = "acc_main",
            toAccountId = "acc_main",
            occurredAt = System.currentTimeMillis()
        )
        tx.validate()
    }

    @Test(expected = IllegalArgumentException::class)
    fun `transfer missing toAccountId fails validation`() {
        val tx = Transaction(
            id = "tx_08",
            type = TransactionType.TRANSFER,
            amountLkrMinor = 5000_00L,
            accountId = "acc_main",
            toAccountId = null,
            occurredAt = System.currentTimeMillis()
        )
        tx.validate()
    }

    @Test(expected = IllegalArgumentException::class)
    fun `transfer with categoryId fails validation`() {
        val tx = Transaction(
            id = "tx_09",
            type = TransactionType.TRANSFER,
            amountLkrMinor = 5000_00L,
            accountId = "acc_main",
            toAccountId = "acc_sec",
            categoryId = "cat_food",
            occurredAt = System.currentTimeMillis()
        )
        tx.validate()
    }

    @Test
    fun `valid income succeeds validation`() {
        val tx = Transaction(
            id = "tx_10",
            type = TransactionType.INCOME,
            amountLkrMinor = 130000_00L,
            accountId = "acc_main",
            incomeSource = IncomeSource.SALARY,
            occurredAt = System.currentTimeMillis()
        )
        tx.validate()
    }

    @Test(expected = IllegalArgumentException::class)
    fun `income missing incomeSource fails validation`() {
        val tx = Transaction(
            id = "tx_11",
            type = TransactionType.INCOME,
            amountLkrMinor = 10000_00L,
            accountId = "acc_main",
            incomeSource = null,
            occurredAt = System.currentTimeMillis()
        )
        tx.validate()
    }

    @Test(expected = IllegalArgumentException::class)
    fun `income with spendKind fails validation`() {
        val tx = Transaction(
            id = "tx_11b",
            type = TransactionType.INCOME,
            amountLkrMinor = 10000_00L,
            accountId = "acc_main",
            incomeSource = IncomeSource.SALARY,
            spendKind = SpendKind.COMMITTED,
            occurredAt = System.currentTimeMillis()
        )
        tx.validate()
    }

    @Test(expected = IllegalArgumentException::class)
    fun `income with categoryId fails validation`() {
        val tx = Transaction(
            id = "tx_12",
            type = TransactionType.INCOME,
            amountLkrMinor = 10000_00L,
            accountId = "acc_main",
            incomeSource = IncomeSource.SALARY,
            categoryId = "cat_food",
            occurredAt = System.currentTimeMillis()
        )
        tx.validate()
    }

    @Test
    fun `crypto income allows negative amount for realized loss`() {
        val cryptoLossTx = Transaction(
            id = "tx_13",
            type = TransactionType.INCOME,
            amountLkrMinor = -3721_24L,
            accountId = "acc_binance",
            incomeSource = IncomeSource.CRYPTO,
            occurredAt = System.currentTimeMillis(),
            original = OriginalAmount(
                currency = "USDT",
                amountMinor = -12_40L,
                fxRateMicros = 300_100_000L
            )
        )
        cryptoLossTx.validate()
        assertNotNull(cryptoLossTx.original)
        assertEquals(-12_40L, cryptoLossTx.original?.amountMinor)
        assertEquals("USDT", cryptoLossTx.original?.currency)
        assertEquals(300_100_000L, cryptoLossTx.original?.fxRateMicros)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `crypto income zero amount fails validation`() {
        val tx = Transaction(
            id = "tx_14",
            type = TransactionType.INCOME,
            amountLkrMinor = 0L,
            accountId = "acc_binance",
            incomeSource = IncomeSource.CRYPTO,
            occurredAt = System.currentTimeMillis()
        )
        tx.validate()
    }

    @Test(expected = IllegalArgumentException::class)
    fun `foreign currency entry with blank currency fails validation`() {
        val tx = Transaction(
            id = "tx_15",
            type = TransactionType.INCOME,
            status = TransactionStatus.EXPECTED,
            amountLkrMinor = 14922_50L,
            accountId = "acc_main",
            incomeSource = IncomeSource.ADSENSE,
            occurredAt = System.currentTimeMillis(),
            original = OriginalAmount(
                currency = "   ",
                amountMinor = 50_00L,
                fxRateMicros = 298_450_000L
            )
        )
        tx.validate()
    }
}
