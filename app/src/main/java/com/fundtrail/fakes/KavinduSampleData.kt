package com.fundtrail.fakes

import com.fundtrail.domain.model.Account
import com.fundtrail.domain.model.AccountKind
import com.fundtrail.domain.model.Category
import com.fundtrail.domain.model.Goal
import com.fundtrail.domain.model.IncomeSource
import com.fundtrail.domain.model.Invoice
import com.fundtrail.domain.model.InvoiceStatus
import com.fundtrail.domain.model.OriginalAmount
import com.fundtrail.domain.model.RecurringTemplate
import com.fundtrail.domain.model.SpendKind
import com.fundtrail.domain.model.Transaction
import com.fundtrail.domain.model.TransactionStatus
import com.fundtrail.domain.model.TransactionType
import com.fundtrail.domain.model.UserSettings
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
            openingBalanceLkrMinor = 0L,
            sortOrder = 3
        ),
        Account(
            id = "acc_savings",
            name = "Savings Account",
            kind = AccountKind.SAVINGS,
            currency = "LKR",
            openingBalanceLkrMinor = 1_120_000L, // LKR 11,200.00
            sortOrder = 4
        ),
        Account(
            id = "acc_binance",
            name = "Binance",
            kind = AccountKind.CRYPTO_EXCHANGE,
            currency = "USDT",
            openingBalanceLkrMinor = 0L,
            sortOrder = 5
        )
    )

    val defaultCategories: List<Category> = listOf(
        Category(
            id = "cat_rent",
            name = "Rent",
            defaultSpendKind = SpendKind.COMMITTED,
            icon = "home",
            sortOrder = 1
        ),
        Category(
            id = "cat_coffee_dining",
            name = "Coffee & Dining",
            defaultSpendKind = SpendKind.DISCRETIONARY,
            icon = "coffee",
            sortOrder = 2
        ),
        Category(
            id = "cat_ride_hailing",
            name = "Ride Hailing",
            defaultSpendKind = SpendKind.DISCRETIONARY,
            icon = "directions_car",
            sortOrder = 3
        ),
        Category(
            id = "cat_groceries",
            name = "Groceries",
            defaultSpendKind = SpendKind.DISCRETIONARY,
            icon = "shopping_cart",
            sortOrder = 4
        ),
        Category(
            id = "cat_subscriptions",
            name = "Subscriptions",
            defaultSpendKind = SpendKind.COMMITTED,
            icon = "subscriptions",
            sortOrder = 5
        ),
        Category(
            id = "cat_food_delivery",
            name = "Food Delivery",
            defaultSpendKind = SpendKind.DISCRETIONARY,
            icon = "fastfood",
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
            // AdSense estimate: USD 50.00 (14,922.50 LKR) EXPECTED
            Transaction(
                id = "tx_adsense_001",
                type = TransactionType.INCOME,
                status = TransactionStatus.EXPECTED,
                amountLkrMinor = 1_492_250L,
                accountId = "acc_main_bank",
                incomeSource = IncomeSource.ADSENSE,
                occurredAt = date5,
                original = OriginalAmount(
                    currency = "USD",
                    amountMinor = 50_00L,
                    fxRateMicros = 298_450_000L
                ),
                note = "AdSense estimate"
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
            // Crypto realized loss: USDT -12.40 (-3,721.24 LKR) ACTUAL
            Transaction(
                id = "tx_crypto_loss_001",
                type = TransactionType.INCOME,
                status = TransactionStatus.ACTUAL,
                amountLkrMinor = -372_124L,
                accountId = "acc_binance",
                incomeSource = IncomeSource.CRYPTO,
                occurredAt = date8,
                original = OriginalAmount(
                    currency = "USDT",
                    amountMinor = -12_40L,
                    fxRateMicros = 300_100_000L
                ),
                note = "Crypto realized loss"
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
            ),
            // Transfer: 10,000 LKR from Main Bank to Secondary Bank
            Transaction(
                id = "tx_transfer_001",
                type = TransactionType.TRANSFER,
                status = TransactionStatus.ACTUAL,
                amountLkrMinor = 1_000_000L,
                accountId = "acc_main_bank",
                toAccountId = "acc_secondary_bank",
                occurredAt = date10,
                note = "Transfer to Secondary Bank"
            )
        )
    }

    val sampleTransactions: List<Transaction> = createSampleTransactionsFor(2026, 10)

    /** MacBook Pro goal, saved against the Savings Account, due end of 2026. */
    val defaultGoal: Goal = Goal(
        id = "goal_macbook",
        name = "MacBook Pro M4",
        targetLkrMinor = 49_000_000L, // LKR 490,000.00
        deadline = LocalDate.of(2026, 12, 31),
        baselineSavedMinor = 1_120_000L, // matches acc_savings opening balance
        isActive = true
    )

    val defaultSettings: UserSettings = UserSettings(
        incomeEstimateMinMinor = 14_000_000L, // LKR 140,000.00
        incomeEstimateMaxMinor = 18_000_000L, // LKR 180,000.00
        expectedSalaryMinor = 13_000_000L, // LKR 130,000.00
        salaryPayday = 25
    )

    val defaultInvoices: List<Invoice> = listOf(
        Invoice(
            id = "inv_acme_001",
            client = "Acme Co",
            project = "Landing page",
            milestone = "Final delivery",
            amountMinor = 5_000_000L, // LKR 50,000.00
            currency = "LKR",
            dueDate = LocalDate.of(2026, 9, 15), // before the sample month: overdue
            status = InvoiceStatus.PENDING,
            note = "Awaiting client sign-off"
        ),
        Invoice(
            id = "inv_beta_001",
            client = "Beta Studio",
            project = "Mobile app",
            milestone = "Milestone 2",
            amountMinor = 30_000L, // USD 300.00
            currency = "USD",
            dueDate = LocalDate.of(2026, 10, 20),
            status = InvoiceStatus.PENDING
        ),
        Invoice(
            id = "inv_gamma_001",
            client = "Gamma Ltd",
            project = "API integration",
            amountMinor = 2_000_000L, // LKR 20,000.00
            currency = "LKR",
            dueDate = LocalDate.of(2026, 9, 1),
            status = InvoiceStatus.RECEIVED,
            receivedTransactionId = "tx_gamma_received_001",
            receivedAt = LocalDate.of(2026, 9, 3).atStartOfDay(zoneId).toInstant().toEpochMilli()
        )
    )

    val defaultRecurringTemplates: List<RecurringTemplate> = listOf(
        RecurringTemplate(
            id = "rec_rent",
            name = "Rent",
            categoryId = "cat_rent",
            accountId = "acc_main_bank",
            expectedAmountMinor = 3_400_000L, // LKR 34,000.00
            dayOfMonth = 2,
            nextDueDate = LocalDate.of(2026, 11, 2)
        ),
        RecurringTemplate(
            id = "rec_subscriptions",
            name = "Subscriptions",
            categoryId = "cat_subscriptions",
            accountId = "acc_main_bank",
            expectedAmountMinor = 250_000L, // LKR 2,500.00
            isVariableAmount = true,
            dayOfMonth = 15,
            nextDueDate = LocalDate.of(2026, 10, 15)
        ),
        RecurringTemplate(
            id = "rec_gym",
            name = "Gym",
            categoryId = "cat_subscriptions",
            accountId = "acc_main_bank",
            expectedAmountMinor = 500_000L, // LKR 5,000.00
            dayOfMonth = 5,
            nextDueDate = LocalDate.of(2026, 9, 5),
            isPaused = true
        )
    )
}
