package com.fundtrail.fakes

import com.fundtrail.domain.model.Account
import com.fundtrail.domain.model.AccountKind
import com.fundtrail.domain.model.Category
import com.fundtrail.domain.model.IncomeSource
import com.fundtrail.domain.model.SpendKind
import com.fundtrail.domain.model.Transaction
import com.fundtrail.domain.model.TransactionStatus
import com.fundtrail.domain.model.TransactionType
import java.time.LocalDate
import java.time.ZoneId

/**
 * Pre-seeded Kavindu sample month data used across fakes, previews, and unit tests.
 */
object KavinduSampleData {

    val defaultAccounts: List<Account> = listOf(
        Account(
            id = "acc_main_bank",
            name = "Main Bank",
            kind = AccountKind.BANK,
            currency = "LKR",
            openingBalanceLkrMinor = 10_000_000L, // LKR 100,000.00
            sortOrder = 1
        ),
        Account(
            id = "acc_secondary_bank",
            name = "Secondary Bank",
            kind = AccountKind.BANK,
            currency = "LKR",
            openingBalanceLkrMinor = 2_500_000L, // LKR 25,000.00
            sortOrder = 2
        ),
        Account(
            id = "acc_cash",
            name = "Cash Wallet",
            kind = AccountKind.CASH,
            currency = "LKR",
            openingBalanceLkrMinor = 1_120_000L, // LKR 11,200.00
            sortOrder = 3
        ),
        Account(
            id = "acc_binance",
            name = "Binance",
            kind = AccountKind.CRYPTO_EXCHANGE,
            currency = "USDT",
            openingBalanceLkrMinor = 0L,
            sortOrder = 4
        )
    )

    val defaultCategories: List<Category> = listOf(
        Category(
            id = "cat_rent",
            name = "Rent",
            defaultSpendKind = SpendKind.COMMITTED,
            iconKey = "home",
            isDefault = true,
            sortOrder = 1
        ),
        Category(
            id = "cat_coffee_dining",
            name = "Coffee & Dining",
            defaultSpendKind = SpendKind.DISCRETIONARY,
            iconKey = "coffee",
            isDefault = true,
            sortOrder = 2
        ),
        Category(
            id = "cat_ride_hailing",
            name = "Ride Hailing",
            defaultSpendKind = SpendKind.DISCRETIONARY,
            iconKey = "directions_car",
            isDefault = true,
            sortOrder = 3
        ),
        Category(
            id = "cat_groceries",
            name = "Groceries",
            defaultSpendKind = SpendKind.DISCRETIONARY,
            iconKey = "shopping_cart",
            isDefault = true,
            sortOrder = 4
        ),
        Category(
            id = "cat_subscriptions",
            name = "Subscriptions",
            defaultSpendKind = SpendKind.COMMITTED,
            iconKey = "subscriptions",
            isDefault = true,
            sortOrder = 5
        ),
        Category(
            id = "cat_food_delivery",
            name = "Food Delivery",
            defaultSpendKind = SpendKind.DISCRETIONARY,
            iconKey = "fastfood",
            isDefault = true,
            sortOrder = 6
        )
    )

    private val zoneId = ZoneId.of("Asia/Colombo")

    /**
     * Sample transactions for a baseline month (default: October 2026).
     */
    fun createSampleTransactionsFor(year: Int = 2026, month: Int = 10): List<Transaction> {
        val date1 = LocalDate.of(year, month, 1).atStartOfDay(zoneId).toInstant().toEpochMilli()
        val date2 = LocalDate.of(year, month, 2).atStartOfDay(zoneId).toInstant().toEpochMilli()
        val date5 = LocalDate.of(year, month, 5).atStartOfDay(zoneId).toInstant().toEpochMilli()
        val date8 = LocalDate.of(year, month, 8).atStartOfDay(zoneId).toInstant().toEpochMilli()
        val date10 = LocalDate.of(year, month, 10).atStartOfDay(zoneId).toInstant().toEpochMilli()

        return listOf(
            // Salary: 130,000 LKR
            Transaction(
                id = "tx_salary_001",
                type = TransactionType.INCOME,
                status = TransactionStatus.ACTUAL,
                amountLkrMinor = 13_000_000L,
                accountId = "acc_main_bank",
                incomeSource = IncomeSource.SALARY,
                occurredAt = date1,
                note = "Monthly Salary"
            ),
            // Rent: 34,000 LKR
            Transaction(
                id = "tx_rent_001",
                type = TransactionType.EXPENSE,
                status = TransactionStatus.ACTUAL,
                amountLkrMinor = 3_400_000L,
                accountId = "acc_main_bank",
                categoryId = "cat_rent",
                spendKind = SpendKind.COMMITTED,
                occurredAt = date2,
                note = "Apartment Rent"
            ),
            // Coffee & Dining entry 1: 14,000 LKR
            Transaction(
                id = "tx_coffee_001",
                type = TransactionType.EXPENSE,
                status = TransactionStatus.ACTUAL,
                amountLkrMinor = 1_400_000L,
                accountId = "acc_main_bank",
                categoryId = "cat_coffee_dining",
                spendKind = SpendKind.DISCRETIONARY,
                occurredAt = date5,
                note = "Dinner with team"
            ),
            // Coffee & Dining entry 2: 10,000 LKR (Total coffee & dining = 24,000 LKR)
            Transaction(
                id = "tx_coffee_002",
                type = TransactionType.EXPENSE,
                status = TransactionStatus.ACTUAL,
                amountLkrMinor = 1_000_000L,
                accountId = "acc_main_bank",
                categoryId = "cat_coffee_dining",
                spendKind = SpendKind.DISCRETIONARY,
                occurredAt = date8,
                note = "Café coffee & snacks"
            ),
            // PickMe ride: 1,500 LKR
            Transaction(
                id = "tx_ride_001",
                type = TransactionType.EXPENSE,
                status = TransactionStatus.ACTUAL,
                amountLkrMinor = 150_000L,
                accountId = "acc_main_bank",
                categoryId = "cat_ride_hailing",
                spendKind = SpendKind.DISCRETIONARY,
                occurredAt = date10,
                note = "PickMe ride to client meeting"
            )
        )
    }

    val sampleTransactions: List<Transaction> = createSampleTransactionsFor(2026, 10)
}
