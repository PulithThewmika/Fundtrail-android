'use strict';

const fs = require('node:fs');
const path = require('node:path');
const { initializeTestEnvironment } = require('@firebase/rules-unit-testing');

const RULES_PATH = path.join(__dirname, '..', '..', 'firestore.rules');

let testEnv;

/** One shared emulator environment for the whole test run. */
async function getTestEnv() {
  if (!testEnv) {
    testEnv = await initializeTestEnvironment({
      projectId: 'fundtrail-5632e-rules-test',
      firestore: {
        rules: fs.readFileSync(RULES_PATH, 'utf8'),
        host: '127.0.0.1',
        port: 8080
      }
    });
  }
  return testEnv;
}

async function teardown() {
  if (testEnv) {
    await testEnv.cleanup();
  }
}

module.exports = { getTestEnv, teardown };
