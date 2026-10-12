package com.fundtrail.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class AccountTest {

    @Test
    fun `valid account succeeds validation`() {
        val account = Account(
            id = "acc_01",
            name = "Commercial Bank Main",
            kind = AccountKind.BANK,
            currency = "LKR",
            openingBalanceLkrMinor = 100000_00L
        )
        account.validate()
        assertEquals(AccountKind.BANK, account.type)
        assertFalse(account.archived)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `blank id fails validation`() {
        val account = Account(
            id = "  ",
            name = "Cash Wallet",
            kind = AccountKind.CASH
        )
        account.validate()
    }

    @Test(expected = IllegalArgumentException::class)
    fun `blank name fails validation`() {
        val account = Account(
            id = "acc_02",
            name = "",
            kind = AccountKind.CASH
        )
        account.validate()
    }

    @Test(expected = IllegalArgumentException::class)
    fun `blank currency fails validation`() {
        val account = Account(
            id = "acc_03",
            name = "Binance Wallet",
            kind = AccountKind.CRYPTO_EXCHANGE,
            currency = "  "
        )
        account.validate()
    }
}
