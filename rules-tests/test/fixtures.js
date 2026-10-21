'use strict';

const { serverTimestamp, Timestamp } = require('firebase/firestore');

// Payload shapes mirror the app's DTO `toMap()` output exactly (data/remote/dto/*.kt): optional
// fields are sent as explicit nulls, never omitted, and createdAt/updatedAt are always
// FieldValue.serverTimestamp(). A rule that only passes on hand-trimmed payloads would let a
// stricter-than-intended rule reject a real, queued offline write.

function userProfile(overrides = {}) {
  return {
    displayName: 'Kavindu',
    incomeEstimateMinMinor: null,
    incomeEstimateMaxMinor: null,
    expectedSalaryMinor: null,
    salaryPayday: 25,
    schemaVersion: 1,
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
    ...overrides
  };
}

function account(overrides = {}) {
  return {
    name: 'Main bank',
    kind: 'BANK',
    currency: 'LKR',
    openingBalanceLkrMinor: 1120000,
    openingBalanceAt: Timestamp.fromDate(new Date('2026-01-01T00:00:00Z')),
    archived: false,
    sortOrder: 0,
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
    ...overrides
  };
}

function category(overrides = {}) {
  return {
    name: 'Food delivery',
    defaultSpendKind: 'DISCRETIONARY',
    icon: 'food',
    archived: false,
    sortOrder: 0,
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
    ...overrides
  };
}

function expenseTransaction(overrides = {}) {
  return {
    type: 'EXPENSE',
    status: 'ACTUAL',
    amountLkrMinor: 85000,
    accountId: 'acc1',
    toAccountId: null,
    categoryId: 'cat1',
    spendKind: 'DISCRETIONARY',
    incomeSource: null,
    occurredAt: Timestamp.now(),
    original: null,
    note: null,
    recurringId: null,
    expectedAmountLkrMinor: null,
    invoiceId: null,
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
    ...overrides
  };
}

function transferTransaction(overrides = {}) {
  return {
    type: 'TRANSFER',
    status: 'ACTUAL',
    amountLkrMinor: 500000,
    accountId: 'acc1',
    toAccountId: 'acc2',
    categoryId: null,
    spendKind: null,
    incomeSource: null,
    occurredAt: Timestamp.now(),
    original: null,
    note: null,
    recurringId: null,
    expectedAmountLkrMinor: null,
    invoiceId: null,
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
    ...overrides
  };
}

function cryptoLossTransaction(overrides = {}) {
  return {
    type: 'INCOME',
    status: 'ACTUAL',
    amountLkrMinor: -372124,
    accountId: 'default-binance',
    toAccountId: null,
    categoryId: null,
    spendKind: null,
    incomeSource: 'CRYPTO',
    occurredAt: Timestamp.now(),
    original: { currency: 'USDT', amountMinor: -1240, fxRateMicros: 300100000 },
    note: null,
    recurringId: null,
    expectedAmountLkrMinor: null,
    invoiceId: null,
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
    ...overrides
  };
}

function invoice(overrides = {}) {
  return {
    clientName: 'Acme Co',
    projectName: 'Website revamp',
    milestone: null,
    amountMinor: 500000,
    currency: 'USD',
    issuedAt: Timestamp.now(),
    dueAt: Timestamp.now(),
    status: 'PENDING',
    receivedTxId: null,
    receivedAt: null,
    lastNotifiedAt: null,
    note: null,
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
    ...overrides
  };
}

function recurring(overrides = {}) {
  return {
    name: 'Rent',
    categoryId: 'cat_rent',
    accountId: 'acc1',
    expectedAmountMinor: 5000000,
    isVariableAmount: false,
    dayOfMonth: 5,
    nextDueDate: Timestamp.now(),
    isPaused: false,
    lastReviewedAt: null,
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
    ...overrides
  };
}

function goal(overrides = {}) {
  return {
    name: 'MacBook Pro M4',
    targetLkrMinor: 49000000,
    baselineSavedMinor: 0,
    startedAt: Timestamp.now(),
    deadline: Timestamp.now(),
    isActive: true,
    createdAt: serverTimestamp(),
    updatedAt: serverTimestamp(),
    ...overrides
  };
}

module.exports = {
  userProfile,
  account,
  category,
  expenseTransaction,
  transferTransaction,
  cryptoLossTransaction,
  invoice,
  recurring,
  goal
};
