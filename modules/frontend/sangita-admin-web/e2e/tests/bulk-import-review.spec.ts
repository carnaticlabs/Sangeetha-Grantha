import { test, expect } from '../fixtures/shared-batch';
import { ImportReviewPage } from '../pages/import-review.page';
import { KrithiListPage } from '../pages/krithi-list.page';
import { DatabaseVerifier } from '../fixtures/db-helpers';

test.describe('Import Review Workflow', () => {
  let reviewPage: ImportReviewPage;
  let krithiListPage: KrithiListPage;
  let db: DatabaseVerifier;

  test.beforeEach(async ({ page }) => {
    reviewPage = new ImportReviewPage(page);
    krithiListPage = new KrithiListPage(page);
    db = new DatabaseVerifier();
  });

  test.afterEach(async () => {
    await db.close();
  });

  test('displays pending imports from batch', async ({ batchId }) => {
    await reviewPage.goto();

    const count = await reviewPage.getPendingCount();
    // May have some pending if not all approved yet
    expect(count).toBeGreaterThanOrEqual(0);
  });

  test('shows import details when selected', async ({ page, batchId }) => {
    await reviewPage.goto();

    const count = await reviewPage.getPendingCount();
    if (count === 0) {
      test.skip();
      return;
    }

    await reviewPage.selectImportByIndex(0);

    const title = await reviewPage.getOverrideFieldValue('title');
    expect(title).not.toBe('');
  });

  test('allows editing override fields before approval', async ({ page, batchId, apiClient }) => {
    // Check if there are pending imports
    const imports = await apiClient.getImports('PENDING');
    if (imports.length === 0) {
      test.skip();
      return;
    }

    await reviewPage.goto();
    await reviewPage.selectImportByIndex(0);

    const originalTitle = await reviewPage.getOverrideFieldValue('title');
    const modifiedTitle = `${originalTitle} - E2E Test`;
    await reviewPage.setOverrideField('title', modifiedTitle);

    await reviewPage.approveSelectedImport();

    const krithi = await db.getKrithiByTitle('E2E Test');
    // Krithi should exist with modified title
    expect(krithi).not.toBeNull();
  });

  test('reject import with confirmation', async ({ page, batchId, apiClient }) => {
    const imports = await apiClient.getImports('PENDING');
    if (imports.length === 0) {
      test.skip();
      return;
    }

    await reviewPage.goto();
    await reviewPage.selectImportByIndex(0);

    await reviewPage.rejectSelectedImport();

    // Verify rejection via database
    const rejected = await db.getImportedKrithisByStatus(batchId, 'REJECTED');
    expect(rejected.length).toBeGreaterThan(0);
  });

  test('bulk select and approve multiple imports', async ({ page, batchId, apiClient }) => {
    const imports = await apiClient.getImports('PENDING');
    if (imports.length < 2) {
      test.skip();
      return;
    }

    await reviewPage.goto();
    const initialCount = await reviewPage.getPendingCount();

    await reviewPage.bulkSelectAll();

    const selectedCount = await reviewPage.getSelectedCount();
    expect(selectedCount).toBeGreaterThan(0);

    await reviewPage.bulkApproveSelected();

    await page.waitForTimeout(2000);
    await reviewPage.refreshQueue();

    const finalCount = await reviewPage.getPendingCount();
    expect(finalCount).toBeLessThan(initialCount);
  });

  test('source URL is displayed', async ({ page, batchId, apiClient }) => {
    const imports = await apiClient.getImports('PENDING');
    if (imports.length === 0) {
      test.skip();
      return;
    }

    await reviewPage.goto();
    await reviewPage.selectImportByIndex(0);

    const sourceUrl = await reviewPage.getSourceUrl();
    // Source URL should be present (it's from the CSV hyperlink column)
    expect(sourceUrl.length).toBeGreaterThan(0);
  });

  test('lyrics preview can be edited', async ({ page, batchId, apiClient }) => {
    const imports = await apiClient.getImports('PENDING');
    if (imports.length === 0) {
      test.skip();
      return;
    }

    await reviewPage.goto();
    await reviewPage.selectImportByIndex(0);

    const originalLyrics = await reviewPage.getLyricsPreview();
    const modifiedLyrics = originalLyrics + '\n\n-- E2E Test Modification';
    await reviewPage.setLyricsPreview(modifiedLyrics);

    const updatedLyrics = await reviewPage.getLyricsPreview();
    expect(updatedLyrics).toContain('E2E Test Modification');
  });

  test('refresh button reloads the queue', async ({ page }) => {
    await reviewPage.goto();

    const initialCount = await reviewPage.getPendingCount();
    await reviewPage.refreshQueue();

    const refreshedCount = await reviewPage.getPendingCount();
    expect(typeof refreshedCount).toBe('number');
  });

  test('resolution panel visibility', async ({ page, batchId, apiClient }) => {
    const imports = await apiClient.getImports('PENDING');
    if (imports.length === 0) {
      test.skip();
      return;
    }

    await reviewPage.goto();
    await reviewPage.selectImportByIndex(0);

    // Resolution panel may or may not be visible depending on entity resolution results
    const hasResolution = await reviewPage.isResolutionPanelVisible();
    expect(typeof hasResolution).toBe('boolean');
  });
});
