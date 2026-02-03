import { FullConfig } from '@playwright/test';
import { Client } from 'pg';
import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const BATCH_STATE_FILE = path.join(__dirname, '.batch-state.json');

interface BatchState {
  batchId: string;
  createdAt: string;
  status: string;
}

async function globalSetup(config: FullConfig) {
  const apiBaseUrl = process.env.API_BASE_URL || 'http://localhost:8080';
  const dbConnectionString =
    process.env.DATABASE_URL || 'postgresql://postgres:postgres@localhost:5432/sangita_grantha';

  // 1. Health check - verify backend is running (with retry for rate limiting)
  console.log('Checking backend health...');
  let healthCheckPassed = false;
  for (let attempt = 1; attempt <= 5; attempt++) {
    try {
      const response = await fetch(`${apiBaseUrl}/health`);
      if (response.ok) {
        healthCheckPassed = true;
        console.log('Backend is healthy');
        break;
      } else if (response.status === 429) {
        console.log(`Rate limited, waiting ${attempt * 2} seconds...`);
        await new Promise((resolve) => setTimeout(resolve, attempt * 2000));
      } else {
        throw new Error(`Backend health check failed with status ${response.status}`);
      }
    } catch (error) {
      if (attempt === 5) {
        console.error('Backend health check failed:', error);
        throw new Error(
          'Backend not running. Start with: mise exec -- cargo run --manifest-path tools/sangita-cli/Cargo.toml -- dev --start-db'
        );
      }
      console.log(`Attempt ${attempt} failed, retrying...`);
      await new Promise((resolve) => setTimeout(resolve, 2000));
    }
  }

  if (!healthCheckPassed) {
    throw new Error('Backend health check failed after 5 attempts');
  }

  // 2. Create auth directory for storage state
  const authDir = path.join(__dirname, '.auth');
  if (!fs.existsSync(authDir)) {
    fs.mkdirSync(authDir, { recursive: true });
  }

  // 3. Check for existing shared batch FIRST (before cleanup!)
  console.log('Checking for existing shared batch...');

  let existingBatch: BatchState | null = null;
  try {
    if (fs.existsSync(BATCH_STATE_FILE)) {
      const content = fs.readFileSync(BATCH_STATE_FILE, 'utf-8');
      existingBatch = JSON.parse(content) as BatchState;
      console.log(`Found existing batch state: ${existingBatch.batchId} (${existingBatch.status})`);

      // Verify batch still exists by checking API
      const verifyResponse = await fetch(`${apiBaseUrl}/v1/admin/bulk-import/batches/${existingBatch.batchId}`, {
        headers: {
          Authorization: `Bearer ${await getAuthToken(apiBaseUrl)}`,
        },
      });

      if (verifyResponse.ok) {
        const batch = await verifyResponse.json();
        if (['SUCCEEDED', 'RUNNING', 'PAUSED'].includes(batch.status)) {
          console.log(`Reusing existing batch: ${existingBatch.batchId} (status: ${batch.status})`);
          // Update status in file
          fs.writeFileSync(
            BATCH_STATE_FILE,
            JSON.stringify({ ...existingBatch, status: batch.status }, null, 2)
          );
          console.log('Global setup complete - reusing batch');
          return;
        } else {
          console.log(`Batch status is ${batch.status}, will create new batch`);
        }
      } else {
        console.log(`Batch not found in API (status ${verifyResponse.status}), will create new batch`);
      }
    }
  } catch (error) {
    console.log('Could not verify existing batch:', error);
  }

  // 4. Database cleanup - only if we're creating a new batch
  console.log('Cleaning up test data from previous runs...');
  const client = new Client({ connectionString: dbConnectionString });

  try {
    await client.connect();

    // Clean up test-created data - be aggressive to avoid duplicates
    // Delete krithis created from e2e imports
    await client.query(`
      DELETE FROM krithi_lyric_sections WHERE lyric_variant_id IN (
        SELECT id FROM krithi_lyric_variants WHERE krithi_id IN (
          SELECT mapped_krithi_id FROM imported_krithis
          WHERE import_batch_id IN (
            SELECT id FROM import_batch WHERE source_manifest LIKE '%bulk_import_test%'
          )
        )
      )
    `);

    await client.query(`
      DELETE FROM krithi_lyric_variants WHERE krithi_id IN (
        SELECT mapped_krithi_id FROM imported_krithis
        WHERE import_batch_id IN (
          SELECT id FROM import_batch WHERE source_manifest LIKE '%bulk_import_test%'
        )
      )
    `);

    await client.query(`
      DELETE FROM krithi_ragas WHERE krithi_id IN (
        SELECT mapped_krithi_id FROM imported_krithis
        WHERE import_batch_id IN (
          SELECT id FROM import_batch WHERE source_manifest LIKE '%bulk_import_test%'
        )
      )
    `);

    await client.query(`
      DELETE FROM krithis WHERE id IN (
        SELECT mapped_krithi_id FROM imported_krithis
        WHERE import_batch_id IN (
          SELECT id FROM import_batch WHERE source_manifest LIKE '%bulk_import_test%'
        )
      )
    `);

    // Delete import-related data
    await client.query(`
      DELETE FROM imported_krithis WHERE import_batch_id IN (
        SELECT id FROM import_batch WHERE source_manifest LIKE '%bulk_import_test%'
      )
    `);

    await client.query(`
      DELETE FROM import_event WHERE ref_id IN (
        SELECT id FROM import_batch WHERE source_manifest LIKE '%bulk_import_test%'
      )
    `);

    await client.query(`
      DELETE FROM import_task_run WHERE job_id IN (
        SELECT id FROM import_job WHERE batch_id IN (
          SELECT id FROM import_batch WHERE source_manifest LIKE '%bulk_import_test%'
        )
      )
    `);

    await client.query(`
      DELETE FROM import_job WHERE batch_id IN (
        SELECT id FROM import_batch WHERE source_manifest LIKE '%bulk_import_test%'
      )
    `);

    await client.query(`DELETE FROM import_batch WHERE source_manifest LIKE '%bulk_import_test%'`);

    console.log('Test data cleanup complete');
  } catch (error) {
    console.warn('Database cleanup warning:', error);
    // Don't fail on cleanup errors - tables might not exist yet
  } finally {
    await client.end();
  }

  // 5. Create a new batch
  console.log('Creating shared batch for all E2E tests...');
  const csvPath = path.resolve(__dirname, '../../../../database/for_import/bulk_import_test.csv');

  try {
    const token = await getAuthToken(apiBaseUrl);

    // Upload CSV
    const fileBuffer = fs.readFileSync(csvPath);
    const formData = new FormData();
    formData.append('file', new Blob([fileBuffer]), 'bulk_import_test.csv');

    const uploadResponse = await fetch(`${apiBaseUrl}/v1/admin/bulk-import/upload`, {
      method: 'POST',
      headers: {
        Authorization: `Bearer ${token}`,
      },
      body: formData,
    });

    if (!uploadResponse.ok) {
      const errorText = await uploadResponse.text();
      throw new Error(`Failed to upload CSV: ${uploadResponse.status} - ${errorText}`);
    }

    const batch = await uploadResponse.json();
    console.log(`Shared batch created: ${batch.id}`);

    // Wait for batch processing (with long timeout)
    console.log('Waiting for batch processing to complete (this may take a few minutes)...');
    const endTime = Date.now() + 300000; // 5 minute timeout

    while (Date.now() < endTime) {
      await new Promise((resolve) => setTimeout(resolve, 3000));

      const statusResponse = await fetch(`${apiBaseUrl}/v1/admin/bulk-import/batches/${batch.id}`, {
        headers: {
          Authorization: `Bearer ${token}`,
        },
      });

      if (statusResponse.ok) {
        const currentBatch = await statusResponse.json();
        console.log(`Batch status: ${currentBatch.status}, progress: ${currentBatch.processedTasks}/${currentBatch.totalTasks}`);

        if (['SUCCEEDED', 'FAILED', 'CANCELLED'].includes(currentBatch.status)) {
          // Save final state
          const state: BatchState = {
            batchId: batch.id,
            createdAt: new Date().toISOString(),
            status: currentBatch.status,
          };
          fs.writeFileSync(BATCH_STATE_FILE, JSON.stringify(state, null, 2));
          console.log(`Batch processing completed with status: ${currentBatch.status}`);
          break;
        }
      }
    }

    // If we didn't complete, still save the batch so tests can use it
    if (!fs.existsSync(BATCH_STATE_FILE) || JSON.parse(fs.readFileSync(BATCH_STATE_FILE, 'utf-8')).batchId !== batch.id) {
      const state: BatchState = {
        batchId: batch.id,
        createdAt: new Date().toISOString(),
        status: 'RUNNING',
      };
      fs.writeFileSync(BATCH_STATE_FILE, JSON.stringify(state, null, 2));
      console.log(`Batch saved with status: RUNNING (still processing)`);
    }
  } catch (error) {
    console.error('Failed to create shared batch:', error);
    throw error;
  }

  console.log('Global setup complete');
}

async function getAuthToken(apiBaseUrl: string): Promise<string> {
  const response = await fetch(`${apiBaseUrl}/v1/auth/token`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      adminToken: process.env.ADMIN_TOKEN || 'dev-admin-token',
      email: process.env.ADMIN_EMAIL || 'admin@sangitagrantha.org',
    }),
  });

  if (!response.ok) {
    throw new Error(`Failed to get auth token: ${response.status}`);
  }

  const data = await response.json();
  return data.token;
}

export default globalSetup;
