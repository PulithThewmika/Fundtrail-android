'use strict';

// Firestore security rules tests (EPIC-017/T2 S2). Run against the Firebase emulator:
//   npm --prefix rules-tests run test:rules
// Every "valid write" fixture mirrors the app's DTO toMap() shape (fixtures.js), so a rule that
// only passes on a hand-trimmed payload would still let a real client write fail silently.

const test = require('node:test');
const { assertFails, assertSucceeds } = require('@firebase/rules-unit-testing');
const {
  doc,
  setDoc,
  updateDoc,
  deleteDoc,
  getDoc,
  serverTimestamp,
  Timestamp
} = require('firebase/firestore');
const { getTestEnv, teardown } = require('./env');
const fixtures = require('./fixtures');

const OWNER = 'kavindu-uid';
const OTHER = 'other-uid';

async function ownerDb() {
  return (await getTestEnv()).authenticatedContext(OWNER).firestore();
}

async function otherDb() {
  return (await getTestEnv()).authenticatedContext(OTHER).firestore();
}

async function anonDb() {
  return (await getTestEnv()).unauthenticatedContext().firestore();
}

/** Writes directly to the emulator, bypassing rules, to seed a pre-existing document. */
async function seed(path, data) {
  const env = await getTestEnv();
  await env.withSecurityRulesDisabled(async (context) => {
    await setDoc(doc(context.firestore(), path), data);
  });
}

test.after(teardown);

test.beforeEach(async () => {
  const env = await getTestEnv();
  await env.clearFirestore();
});

// --- Ownership ---

test('owner can read their own profile', async () => {
  await seed(`users/${OWNER}`, fixtures.userProfile());
  const db = await ownerDb();
  await assertSucceeds(getDoc(doc(db, `users/${OWNER}`)));
});

test('another signed-in user is denied reading the profile', async () => {
  await seed(`users/${OWNER}`, fixtures.userProfile());
  const db = await otherDb();
  await assertFails(getDoc(doc(db, `users/${OWNER}`)));
});

test('unauthenticated request is denied', async () => {
  await seed(`users/${OWNER}`, fixtures.userProfile());
  const db = await anonDb();
  await assertFails(getDoc(doc(db, `users/${OWNER}`)));
});

// --- One valid create per collection (AC) ---

test('valid profile create is allowed', async () => {
  const db = await ownerDb();
  await assertSucceeds(setDoc(doc(db, `users/${OWNER}`), fixtures.userProfile()));
});

test('valid account create is allowed', async () => {
  const db = await ownerDb();
  await assertSucceeds(setDoc(doc(db, `users/${OWNER}/accounts/acc1`), fixtures.account()));
});

test('valid category create is allowed', async () => {
  const db = await ownerDb();
  await assertSucceeds(setDoc(doc(db, `users/${OWNER}/categories/cat1`), fixtures.category()));
});

test('valid expense transaction create is allowed (explicit-null optionals)', async () => {
  const db = await ownerDb();
  await assertSucceeds(
    setDoc(doc(db, `users/${OWNER}/transactions/tx1`), fixtures.expenseTransaction())
  );
});

test('valid transfer transaction create is allowed', async () => {
  const db = await ownerDb();
  await assertSucceeds(
    setDoc(doc(db, `users/${OWNER}/transactions/tx2`), fixtures.transferTransaction())
  );
});

test('valid invoice create is allowed', async () => {
  const db = await ownerDb();
  await assertSucceeds(setDoc(doc(db, `users/${OWNER}/invoices/inv1`), fixtures.invoice()));
});

test('valid recurring create is allowed', async () => {
  const db = await ownerDb();
  await assertSucceeds(setDoc(doc(db, `users/${OWNER}/recurring/rec1`), fixtures.recurring()));
});

test('valid goal create is allowed', async () => {
  const db = await ownerDb();
  await assertSucceeds(setDoc(doc(db, `users/${OWNER}/goals/goal1`), fixtures.goal()));
});

// --- Field validation ---

test('unknown field is denied', async () => {
  const db = await ownerDb();
  await assertFails(
    setDoc(doc(db, `users/${OWNER}/accounts/acc1`), fixtures.account({ notAField: 'nope' }))
  );
});

test('negative amount on a non-crypto expense is denied', async () => {
  const db = await ownerDb();
  await assertFails(
    setDoc(
      doc(db, `users/${OWNER}/transactions/tx1`),
      fixtures.expenseTransaction({ amountLkrMinor: -500 })
    )
  );
});

test('crypto realised loss (negative amount) is allowed', async () => {
  const db = await ownerDb();
  await assertSucceeds(
    setDoc(doc(db, `users/${OWNER}/transactions/tx1`), fixtures.cryptoLossTransaction())
  );
});

test('transfer to the same account is denied', async () => {
  const db = await ownerDb();
  await assertFails(
    setDoc(
      doc(db, `users/${OWNER}/transactions/tx1`),
      fixtures.transferTransaction({ toAccountId: 'acc1' })
    )
  );
});

test('expense without a category is denied', async () => {
  const db = await ownerDb();
  await assertFails(
    setDoc(
      doc(db, `users/${OWNER}/transactions/tx1`),
      fixtures.expenseTransaction({ categoryId: null, spendKind: null })
    )
  );
});

test('recurring dayOfMonth 31 is denied', async () => {
  const db = await ownerDb();
  await assertFails(
    setDoc(doc(db, `users/${OWNER}/recurring/rec1`), fixtures.recurring({ dayOfMonth: 31 }))
  );
});

test('profile with min estimate above max is denied', async () => {
  const db = await ownerDb();
  await assertFails(
    setDoc(
      doc(db, `users/${OWNER}`),
      fixtures.userProfile({ incomeEstimateMinMinor: 500000, incomeEstimateMaxMinor: 100000 })
    )
  );
});

// --- Mutation rules ---

test('hard delete is denied', async () => {
  await seed(`users/${OWNER}/accounts/acc1`, fixtures.account());
  const db = await ownerDb();
  await assertFails(deleteDoc(doc(db, `users/${OWNER}/accounts/acc1`)));
});

test('soft delete (deletedAt update) is allowed', async () => {
  await seed(`users/${OWNER}/invoices/inv1`, fixtures.invoice());
  const db = await ownerDb();
  await assertSucceeds(
    updateDoc(doc(db, `users/${OWNER}/invoices/inv1`), {
      deletedAt: serverTimestamp(),
      updatedAt: serverTimestamp()
    })
  );
});

test('changing createdAt on update is denied', async () => {
  await seed(`users/${OWNER}/accounts/acc1`, {
    ...fixtures.account(),
    createdAt: Timestamp.fromDate(new Date('2026-01-01T00:00:00Z'))
  });
  const db = await ownerDb();
  await assertFails(
    updateDoc(doc(db, `users/${OWNER}/accounts/acc1`), {
      name: 'Renamed bank',
      createdAt: Timestamp.fromDate(new Date('2099-01-01T00:00:00Z'))
    })
  );
});

test('owner can settle a PENDING invoice', async () => {
  await seed(`users/${OWNER}/invoices/inv1`, fixtures.invoice({ status: 'PENDING' }));
  const db = await ownerDb();
  await assertSucceeds(
    updateDoc(doc(db, `users/${OWNER}/invoices/inv1`), {
      status: 'RECEIVED',
      receivedTxId: 'tx1',
      receivedAt: Timestamp.now()
    })
  );
});

test('settling an already-RECEIVED invoice a second time is denied', async () => {
  await seed(
    `users/${OWNER}/invoices/inv1`,
    fixtures.invoice({
      status: 'RECEIVED',
      receivedTxId: 'tx1',
      receivedAt: Timestamp.now()
    })
  );
  const db = await ownerDb();
  await assertFails(
    updateDoc(doc(db, `users/${OWNER}/invoices/inv1`), {
      status: 'RECEIVED',
      receivedTxId: 'tx2',
      receivedAt: Timestamp.now()
    })
  );
});

test('reverting a RECEIVED invoice back to PENDING is denied', async () => {
  await seed(
    `users/${OWNER}/invoices/inv1`,
    fixtures.invoice({
      status: 'RECEIVED',
      receivedTxId: 'tx1',
      receivedAt: Timestamp.now()
    })
  );
  const db = await ownerDb();
  await assertFails(
    updateDoc(doc(db, `users/${OWNER}/invoices/inv1`), { status: 'PENDING' })
  );
});

test('soft-deleting a RECEIVED invoice is still allowed', async () => {
  await seed(
    `users/${OWNER}/invoices/inv1`,
    fixtures.invoice({
      status: 'RECEIVED',
      receivedTxId: 'tx1',
      receivedAt: Timestamp.now()
    })
  );
  const db = await ownerDb();
  await assertSucceeds(
    updateDoc(doc(db, `users/${OWNER}/invoices/inv1`), {
      deletedAt: serverTimestamp(),
      updatedAt: serverTimestamp()
    })
  );
});

