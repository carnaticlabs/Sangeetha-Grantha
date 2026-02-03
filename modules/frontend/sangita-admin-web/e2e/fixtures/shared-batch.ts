import { test as base } from '@playwright/test';
import { DirectApiClient } from '../utils/api-client';
import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

// File to persist batch ID across all test runs (created by global-setup.ts)
const BATCH_STATE_FILE = path.join(__dirname, '../.batch-state.json');

interface BatchState {
  batchId: string;
  createdAt: string;
  status: string;
}

function readBatchState(): BatchState | null {
  try {
    if (fs.existsSync(BATCH_STATE_FILE)) {
      const content = fs.readFileSync(BATCH_STATE_FILE, 'utf-8');
      return JSON.parse(content) as BatchState;
    }
  } catch {
    // Ignore errors
  }
  return null;
}

export interface SharedBatchFixture {
  batchId: string;
  apiClient: DirectApiClient;
}

// Create a custom test fixture that provides the shared batch
export const test = base.extend<{}, SharedBatchFixture>({
  // Worker-scoped fixtures (shared across all tests in a worker)
  apiClient: [
    async ({}, use) => {
      const client = new DirectApiClient();
      await client.login();
      await use(client);
    },
    { scope: 'worker' },
  ],

  batchId: [
    async ({}, use) => {
      // Read batch ID from state file (created by global-setup.ts)
      const state = readBatchState();

      if (!state) {
        throw new Error(
          'No batch state found. Global setup should have created a batch. ' +
            'Run the tests again or check global-setup.ts for errors.'
        );
      }

      console.log(`Using shared batch: ${state.batchId} (status: ${state.status})`);
      await use(state.batchId);
    },
    { scope: 'worker' },
  ],
});

export { expect } from '@playwright/test';
