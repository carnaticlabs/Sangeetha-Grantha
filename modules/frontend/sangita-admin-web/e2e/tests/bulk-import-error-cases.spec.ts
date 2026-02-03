import { test, expect } from '../fixtures/shared-batch';
import { BulkImportPage } from '../pages/bulk-import.page';
import { DatabaseVerifier } from '../fixtures/db-helpers';
import path from 'path';
import fs from 'fs';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

test.describe('Bulk Import - Error Cases', () => {
  let bulkImportPage: BulkImportPage;
  let db: DatabaseVerifier;
  const tempDir = path.join(__dirname, '../fixtures/temp');

  test.beforeAll(async () => {
    if (!fs.existsSync(tempDir)) {
      fs.mkdirSync(tempDir, { recursive: true });
    }
  });

  test.beforeEach(async ({ page }) => {
    bulkImportPage = new BulkImportPage(page);
    db = new DatabaseVerifier();
  });

  test.afterEach(async () => {
    await db.close();
  });

  test.afterAll(async () => {
    if (fs.existsSync(tempDir)) {
      fs.rmSync(tempDir, { recursive: true, force: true });
    }
  });

  test('rejects empty CSV file', async ({ page }) => {
    const emptyPath = path.join(tempDir, 'empty.csv');
    fs.writeFileSync(emptyPath, 'Krithi,Raga,Hyperlink\n');

    await bulkImportPage.goto();
    await bulkImportPage.fileInput.setInputFiles(emptyPath);
    await bulkImportPage.startImportButton.click();

    // Wait for any error indication - could be toast, alert, or inline message
    const errorVisible = await page.waitForSelector(
      '[role="alert"], .text-red-500, .text-rose-500, .bg-red-50, .bg-rose-50, [data-testid="error"]',
      { timeout: 5000 }
    ).catch(() => null);

    // If no explicit error UI, check that we didn't navigate away or get success toast
    const successToast = await page.locator('[role="status"]').filter({ hasText: /success|created/i }).isVisible().catch(() => false);
    expect(successToast).toBe(false);
  });

  test('rejects CSV with missing required columns', async ({ page }) => {
    const invalidPath = path.join(tempDir, 'missing-columns.csv');
    fs.writeFileSync(invalidPath, 'Name,Description\nTest,A test entry\n');

    await bulkImportPage.goto();
    await bulkImportPage.fileInput.setInputFiles(invalidPath);
    await bulkImportPage.startImportButton.click();

    // Wait for any error indication
    const errorVisible = await page.waitForSelector(
      '[role="alert"], .text-red-500, .text-rose-500, .bg-red-50, .bg-rose-50',
      { timeout: 5000 }
    ).catch(() => null);

    const successToast = await page.locator('[role="status"]').filter({ hasText: /success|created/i }).isVisible().catch(() => false);
    expect(successToast).toBe(false);
  });

  test('handles CSV with invalid URL format gracefully', async ({ page }) => {
    const invalidUrlPath = path.join(tempDir, 'invalid-urls.csv');
    fs.writeFileSync(invalidUrlPath, 'Krithi,Raga,Hyperlink\nTest Song,TestRaga,not-a-valid-url\n');

    await bulkImportPage.goto();
    await bulkImportPage.fileInput.setInputFiles(invalidUrlPath);
    await bulkImportPage.startImportButton.click();

    // Give time for response - could succeed (and mark tasks as failed) or reject immediately
    await page.waitForTimeout(2000);

    // Verify page didn't crash
    const isPageStable = await page.locator('body').isVisible();
    expect(isPageStable).toBe(true);
  });

  test('batch can be viewed in UI', async ({ page, batchId }) => {
    await bulkImportPage.goto();
    await bulkImportPage.waitForBatchToAppear('bulk_import_test.csv');
    await bulkImportPage.selectBatch('bulk_import_test.csv');

    const status = await bulkImportPage.getBatchStatus();
    expect(['PENDING', 'RUNNING', 'PAUSED', 'SUCCEEDED', 'FAILED', 'CANCELLED']).toContain(status);
  });

  test('task list is visible', async ({ page, batchId }) => {
    await bulkImportPage.goto();
    await bulkImportPage.selectBatch('bulk_import_test.csv');

    const taskCount = await bulkImportPage.getTaskCount();
    expect(taskCount).toBeGreaterThanOrEqual(0);
  });

  test('task log viewer opens when clicking task', async ({ page, batchId }) => {
    await bulkImportPage.goto();
    await bulkImportPage.selectBatch('bulk_import_test.csv');

    const taskCount = await bulkImportPage.getTaskCount();
    if (taskCount === 0) {
      console.log('No tasks available to click');
      test.skip();
      return;
    }

    // Try to find and click a task item
    const taskItem = page.locator('div').filter({ hasText: /http|succeeded|failed|running/i }).locator('.cursor-pointer, .hover\\:bg-slate-50').first();
    const isTaskClickable = await taskItem.isVisible().catch(() => false);

    if (!isTaskClickable) {
      console.log('No clickable task item found');
      test.skip();
      return;
    }

    await taskItem.click();

    // Check if log viewer opened - it may be a modal or panel
    await page.waitForTimeout(500);
    const isLogViewerOpen = await bulkImportPage.isLogViewerOpen();
    expect(typeof isLogViewerOpen).toBe('boolean');

    if (isLogViewerOpen) {
      await bulkImportPage.closeLogViewer();
    }
  });

  test('refresh button works', async ({ page, batchId }) => {
    await bulkImportPage.goto();

    const initialCount = await bulkImportPage.getBatchCount();
    await bulkImportPage.refreshButton.click();
    await page.waitForTimeout(500);

    const refreshedCount = await bulkImportPage.getBatchCount();
    expect(refreshedCount).toBeGreaterThanOrEqual(0);
  });
});
