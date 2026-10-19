package com.fundtrail.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class IncomeDefaultsTest {

    private fun account(id: String) = Account(id = id, name = id, kind = AccountKind.BANK)

    private val mainBank = account("default-main-bank")
    private val binance = account("default-binance")
    private val cash = account("default-cash")
    private val active = listOf(mainBank, binance, cash)

    @Test
    fun salaryDefaultsToMainBank() {
        assertEquals("default-main-bank", IncomeDefaults.accountFor(IncomeSource.SALARY, active, lastUsedId = "default-cash"))
    }

    @Test
    fun cryptoDefaultsToBinance() {
        assertEquals("default-binance", IncomeDefaults.accountFor(IncomeSource.CRYPTO, active, lastUsedId = "default-cash"))
    }

    @Test
    fun freelanceAdsenseAndOtherFallBackToTheLastUsedAccount() {
        assertEquals("default-cash", IncomeDefaults.accountFor(IncomeSource.FREELANCE, active, lastUsedId = "default-cash"))
        assertEquals("default-cash", IncomeDefaults.accountFor(IncomeSource.ADSENSE, active, lastUsedId = "default-cash"))
        assertEquals("default-cash", IncomeDefaults.accountFor(IncomeSource.OTHER, active, lastUsedId = "default-cash"))
    }

    @Test
    fun anArchivedDefaultAccountFallsBackToTheLastUsedAccount() {
        val withoutBinance = listOf(mainBank, cash)

        assertEquals("default-cash", IncomeDefaults.accountFor(IncomeSource.CRYPTO, withoutBinance, lastUsedId = "default-cash"))
    }

    @Test
    fun noLastUsedAndNoFixedDefaultFallsBackToTheFirstActiveAccount() {
        assertEquals("default-main-bank", IncomeDefaults.accountFor(IncomeSource.FREELANCE, active, lastUsedId = null))
    }

    @Test
    fun anArchivedDefaultWithNoLastUsedFallsBackToTheFirstActiveAccount() {
        val withoutBinance = listOf(mainBank, cash)

        assertEquals("default-main-bank", IncomeDefaults.accountFor(IncomeSource.CRYPTO, withoutBinance, lastUsedId = null))
    }

    @Test
    fun emptyActiveAccountsGivesNull() {
        assertNull(IncomeDefaults.accountFor(IncomeSource.SALARY, emptyList(), lastUsedId = "default-cash"))
    }
}
