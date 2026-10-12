package com.fundtrail.domain.model

/**
 * Domain model representing a financial account.
 *
 * @property id Unique account ID
 * @property name User-facing account name
 * @property kind [AccountKind] (BANK, CASH, SAVINGS, CRYPTO_EXCHANGE, OTHER)
 * @property currency Account ISO currency code (e.g. "LKR", "USDT")
 * @property openingBalanceLkrMinor Initial balance in LKR minor units
 * @property openingBalanceAt Epoch timestamp (milliseconds) when tracking started
 * @property archived Whether the account is hidden from pickers
 * @property sortOrder Display sorting priority
 * @property createdAt Server creation timestamp (milliseconds)
 * @property updatedAt Server update timestamp (milliseconds)
 */
data class Account(
    val id: String,
    val name: String,
    val kind: AccountKind,
    val currency: String = "LKR",
    val openingBalanceLkrMinor: Long = 0L,
    val openingBalanceAt: Long = 0L,
    val archived: Boolean = false,
    val sortOrder: Int = 0,
    val createdAt: Long? = null,
    val updatedAt: Long? = null
) {
    /**
     * Validates account invariants:
     * Validates account invariants:
     * - [id], [name], and [currency] must not be blank.
     */
    fun validate() {
        require(id.isNotBlank()) { "Account ID must not be blank" }
        require(name.isNotBlank()) { "Account name must not be blank" }
        require(currency.isNotBlank()) { "Account currency must not be blank" }
    }
}
