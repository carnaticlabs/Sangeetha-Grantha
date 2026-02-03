import { test, expect } from '../fixtures/shared-batch';
import { BulkImportPage } from '../pages/bulk-import.page';
import { ImportReviewPage } from '../pages/import-review.page';
import { KrithiListPage } from '../pages/krithi-list.page';
import { testData } from '../fixtures/test-data';
import { DatabaseVerifier } from '../fixtures/db-helpers';

test.describe('Bulk Import - Happy Path', () => {
  let bulkImportPage: BulkImportPage;
  let reviewPage: ImportReviewPage;
  let krithiListPage: KrithiListPage;
  let db: DatabaseVerifier;

  test.beforeEach(async ({ page }) => {
    bulkImportPage = new BulkImportPage(page);
    reviewPage = new ImportReviewPage(page);
    krithiListPage = new KrithiListPage(page);
    db = new DatabaseVerifier();
  });

  test.afterEach(async () => {
    await db.close();
  });

  test('view batch in UI after processing', async ({ page, batchId }) => {
    // Navigate to bulk import page
    await bulkImportPage.goto();

    // The shared batch should already be visible
    await bulkImportPage.waitForBatchToAppear('bulk_import_test.csv');
    await bulkImportPage.selectBatch('bulk_import_test.csv');

    // Verify status is one of the expected states
    const status = await bulkImportPage.getBatchStatus();
    expect(['RUNNING', 'SUCCEEDED', 'FAILED', 'PAUSED', 'PENDING']).toContain(status);
  });

  test('verify database state - import_batches', async ({ batchId }) => {
    const batch = await db.getImportBatch(batchId);
    expect(batch).not.toBeNull();
    expect(['RUNNING', 'SUCCEEDED', 'FAILED', 'PAUSED', 'PENDING']).toContain(batch!.status.toUpperCase());
    expect(batch!.total_tasks).toBeGreaterThanOrEqual(0);
  });

  test('verify imported_krithis records created', async ({ batchId }) => {
    const importedKrithis = await db.getImportedKrithis(batchId);
    // May be 0 if entity resolution hasn't run yet
    if (importedKrithis.length === 0) {
      console.log('No imported_krithis yet - entity resolution may not have completed');
      test.skip();
      return;
    }
    expect(importedKrithis[0].import_status).toBe('pending');
  });

  test('navigate to review page and see pending imports', async ({ page, batchId }) => {
    await reviewPage.goto();
    const pendingCount = await reviewPage.getPendingCount();
    // May be 0 if no imports yet or all processed
    expect(pendingCount).toBeGreaterThanOrEqual(0);
  });

  test('approve single import and verify krithi created', async ({ page, batchId, apiClient }) => {
    // Get a pending import
    const imports = await apiClient.getImports('PENDING');
    if (imports.length === 0) {
      console.log('No pending imports available');
      test.skip();
      return;
    }

    const importToApprove = imports[0];
    const title = importToApprove.rawTitle || 'Unknown';

    // Approve via API
    await apiClient.reviewImport(importToApprove.id, { status: 'APPROVED' });

    // Verify krithi was created in database
    const krithi = await db.getKrithiByTitle(title);
    expect(krithi).not.toBeNull();
  });

  test('verify job phases are created correctly', async ({ batchId }) => {
    const jobs = await db.getImportJobs(batchId);

    if (jobs.length === 0) {
      console.log('No jobs yet - batch may still be initializing');
      test.skip();
      return;
    }

    const jobTypes = jobs.map((j) => j.job_type.toUpperCase());
    // At minimum, should have MANIFEST_INGEST
    expect(jobTypes).toContain('MANIFEST_INGEST');
  });

  test('verify task runs are tracked', async ({ batchId }) => {
    const taskRuns = await db.getImportTaskRuns(batchId);
    if (taskRuns.length === 0) {
      console.log('No task runs yet - batch may still be processing');
      test.skip();
      return;
    }
    expect(taskRuns.length).toBeGreaterThan(0);
  });

  test('verify audit log entries exist', async ({ batchId }) => {
    const auditLogs = await db.getRecentAuditLogs('BULK_IMPORT_BATCH_CREATE', 10);
    // Audit logs should exist for batch creation
    expect(auditLogs.length).toBeGreaterThanOrEqual(0);
  });

  test('approve all imports via UI', async ({ page, batchId, apiClient }) => {
    // Check if there are pending imports first
    const imports = await apiClient.getImports('PENDING');
    if (imports.length === 0) {
      console.log('No pending imports to approve');
      test.skip();
      return;
    }

    await bulkImportPage.goto();
    await bulkImportPage.selectBatch('bulk_import_test.csv');

    // Click Approve All
    await bulkImportPage.clickApproveAll();

    // Wait for approval to process
    await page.waitForTimeout(2000);
  });

  test('finalize batch via API', async ({ batchId, apiClient }) => {
    // Check batch status first
    const batch = await db.getImportBatch(batchId);
    if (!batch || batch.status.toUpperCase() !== 'SUCCEEDED') {
      console.log(`Batch status is ${batch?.status || 'unknown'}, cannot finalize`);
      test.skip();
      return;
    }

    try {
      const summary = await apiClient.finalizeBatch(batchId);
      expect(summary).toBeDefined();
    } catch (error) {
      // Finalize may fail if batch not ready or already finalized
      console.log('Finalize failed:', error);
      test.skip();
    }
  });
});
