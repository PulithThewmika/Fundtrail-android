package com.fundtrail.data.repository

import com.fundtrail.data.local.entity.AccountEntity
import com.fundtrail.data.local.entity.CategoryEntity
import com.fundtrail.data.local.entity.InvoiceEntity
import com.fundtrail.data.local.entity.TransactionEntity
import com.fundtrail.domain.model.Account
import com.fundtrail.domain.model.Category
import com.fundtrail.domain.model.Invoice
import com.fundtrail.domain.model.Transaction
import java.time.LocalDate

fun TransactionEntity.toDomain() = Transaction(
    id = id,
    type = type,
    status = status,
    amountLkrMinor = amountLkrMinor,
    accountId = accountId,
    toAccountId = toAccountId,
    categoryId = categoryId,
    spendKind = spendKind,
    incomeSource = incomeSource,
    occurredAt = occurredAt,
    original = original,
    note = note,
    recurringId = recurringId,
    expectedAmountLkrMinor = expectedAmountLkrMinor,
    invoiceId = invoiceId,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

/** `deletedAt` is dropped: soft-deleted rows are deleted from Room, never stored. */
fun Transaction.toEntity() = TransactionEntity(
    id = id,
    type = type,
    status = status,
    amountLkrMinor = amountLkrMinor,
    accountId = accountId,
    toAccountId = toAccountId,
    categoryId = categoryId,
    spendKind = spendKind,
    incomeSource = incomeSource,
    occurredAt = occurredAt,
    original = original,
    note = note,
    recurringId = recurringId,
    expectedAmountLkrMinor = expectedAmountLkrMinor,
    invoiceId = invoiceId,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun AccountEntity.toDomain() = Account(
    id = id,
    name = name,
    kind = kind,
    currency = currency,
    openingBalanceLkrMinor = openingBalanceLkrMinor,
    openingBalanceAt = openingBalanceAt,
    archived = archived,
    sortOrder = sortOrder,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun Account.toEntity() = AccountEntity(
    id = id,
    name = name,
    kind = kind,
    currency = currency,
    openingBalanceLkrMinor = openingBalanceLkrMinor,
    openingBalanceAt = openingBalanceAt,
    archived = archived,
    sortOrder = sortOrder,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun CategoryEntity.toDomain() = Category(
    id = id,
    name = name,
    defaultSpendKind = defaultSpendKind,
    icon = icon,
    archived = archived,
    sortOrder = sortOrder,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun Category.toEntity() = CategoryEntity(
    id = id,
    name = name,
    defaultSpendKind = defaultSpendKind,
    icon = icon,
    archived = archived,
    sortOrder = sortOrder,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun InvoiceEntity.toDomain() = Invoice(
    id = id,
    client = client,
    project = project,
    milestone = milestone,
    amountMinor = amountMinor,
    currency = currency,
    dueDate = LocalDate.parse(dueDate),
    status = status,
    receivedTransactionId = receivedTransactionId,
    receivedAt = receivedAt,
    lastNotifiedAt = lastNotifiedAt,
    note = note,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun Invoice.toEntity() = InvoiceEntity(
    id = id,
    client = client,
    project = project,
    milestone = milestone,
    amountMinor = amountMinor,
    currency = currency,
    dueDate = dueDate.toString(),
    status = status,
    receivedTransactionId = receivedTransactionId,
    receivedAt = receivedAt,
    lastNotifiedAt = lastNotifiedAt,
    note = note,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
