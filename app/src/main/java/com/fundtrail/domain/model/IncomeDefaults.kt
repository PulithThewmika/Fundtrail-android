package com.fundtrail.domain.model

/**
 * Picks the account a new income entry should pre-select (FR-23 pattern applied to income, #103 AC 2).
 * Salary and Crypto income nearly always land in the same place, so they get a fixed default account;
 * every other source falls back to the last-used account. Pure function, same shape as [CategorySuggestions].
 */
object IncomeDefaults {
    private const val SALARY_ACCOUNT_ID = "default-main-bank"
    private const val CRYPTO_ACCOUNT_ID = "default-binance"

    /**
     * @param lastUsedId id of the account the newest expense was booked to (FR-31), or null if there is none
     * @return an active account id, or null if [activeAccounts] is empty
     */
    fun accountFor(source: IncomeSource, activeAccounts: List<Account>, lastUsedId: String?): String? {
        val fixed = when (source) {
            IncomeSource.SALARY -> SALARY_ACCOUNT_ID
            IncomeSource.CRYPTO -> CRYPTO_ACCOUNT_ID
            else -> null
        }
        val candidates = listOf(fixed, lastUsedId)
        return candidates.firstOrNull { id -> id != null && activeAccounts.any { it.id == id } }
            ?: activeAccounts.firstOrNull()?.id
    }

    /**
     * The currency a new entry for [source] starts in (#174 AC 1): AdSense is paid in USD and Binance
     * results are in USDT, everything else is LKR. The user can still change it.
     */
    fun currencyFor(source: IncomeSource): String = when (source) {
        IncomeSource.ADSENSE -> "USD"
        IncomeSource.CRYPTO -> "USDT"
        else -> LKR
    }
}
