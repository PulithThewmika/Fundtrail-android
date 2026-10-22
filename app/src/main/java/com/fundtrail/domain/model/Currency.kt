package com.fundtrail.domain.model

/** The reporting currency: every stored LKR amount is the truth (ADR-0003). */
const val LKR = "LKR"

/** The one currency code table (schema.md §4.2). */
val SUPPORTED_CURRENCIES = listOf(LKR, "USD", "USDT")
