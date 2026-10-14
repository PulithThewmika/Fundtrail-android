package com.fundtrail.domain.model

val SUPPORTED_CURRENCIES = listOf("LKR", "USD", "USDT")

val defaultAccounts = listOf(
    Account(id = "default-main-bank", name = "Main bank", kind = AccountKind.BANK, currency = "LKR", sortOrder = 1),
    Account(id = "default-secondary-bank", name = "Secondary bank", kind = AccountKind.BANK, currency = "LKR", sortOrder = 2),
    Account(id = "default-cash", name = "Cash", kind = AccountKind.CASH, currency = "LKR", sortOrder = 3),
    Account(id = "default-binance", name = "Binance", kind = AccountKind.CRYPTO_EXCHANGE, currency = "USDT", sortOrder = 4),
    Account(id = "default-other", name = "Other", kind = AccountKind.OTHER, currency = "LKR", sortOrder = 5),
)
