import { Page, Locator, expect } from '@playwright/test';
import { BasePage } from './base.page';

export class BulkImportPage extends BasePage {
  readonly heading: Locator;
  readonly fileInput: Locator;
  readonly startImportButton: Locator;
  readonly batchesTable: Locator;
  readonly batchesTableBody: Locator;
  readonly refreshButton: Locator;
  readonly approveAllButton: Locator;
  readonly rejectAllButton: Locator;
  readonly retryFailedButton: Locator;
  readonly cancelButton: Locator;
  readonly deleteButton: Locator;
  readonly finalizeButton: Locator;
  readonly exportButton: Locator;
  readonly tasksList: Locator;

  constructor(page: Page) {
    super(page);
    this.heading = page.locator('h1, h2').filter({ hasText: /bulk import/i }).first();
    this.fileInput = page.locator('input[type="file"]');
    this.startImportButton = page.getByRole('button', { name: /start import/i });
    this.batchesTable = page.locator('table');
    this.batchesTableBody = this.batchesTable.locator('tbody');
    this.refreshButton = page.locator('button').filter({ has: page.locator('span:text("refresh")') });
    this.approveAllButton = page.getByRole('button', { name: /approve all/i });
    this.rejectAllButton = page.getByRole('button', { name: /reject all/i });
    this.retryFailedButton = page.getByRole('button', { name: /retry failed/i });
    this.cancelButton = page.getByRole('button', { name: /^cancel$/i });
    this.deleteButton = page.getByRole('button', { name: /^delete$/i }).last();
    this.finalizeButton = page.getByRole('button', { name: /finalize/i });
    this.exportButton = page.getByRole('button', { name: /export/i });
    this.tasksList = page.locator('.max-h-64.overflow-y-auto');
  }

  async goto(): Promise<void> {
    await this.page.goto('/bulk-import');
    await this.page.waitForLoadState('networkidle');
  }

  async uploadCSV(filePath: string): Promise<void> {
    await this.fileInput.setInputFiles(filePath);
    await this.startImportButton.click();
  }

  async waitForBatchToAppear(manifestName: string, timeout = 15000): Promise<void> {
    // Refresh the batch list first in case it's cached
    await this.refreshButton.click().catch(() => {});
    await this.page.waitForTimeout(500);
    await expect(this.page.getByText(manifestName).first()).toBeVisible({ timeout });
  }

  async selectBatch(manifestName: string): Promise<void> {
    const row = this.page.locator('tr').filter({ hasText: manifestName }).first();
    await row.click();
    // Wait for detail panel to load
    await this.page.waitForTimeout(500);
  }

  async selectBatchById(batchIdPrefix: string): Promise<void> {
    const row = this.page.locator(`tr:has-text("#${batchIdPrefix}")`);
    await row.click();
    await this.page.waitForTimeout(500);
  }

  async getBatchStatus(): Promise<string> {
    // Find the status badge in the selected row or detail panel
    const statusBadge = this.page.locator('.rounded-full').filter({ hasText: /running|succeeded|failed|pending|paused|cancelled/i }).first();
    const text = await statusBadge.textContent();
    return text?.trim().toUpperCase() || '';
  }

  async getProgress(): Promise<{ processed: number; total: number; percentage: number }> {
    // Find progress percentage
    const progressText = this.page.locator('.tabular-nums').filter({ hasText: '%' }).first();
    const text = await progressText.textContent();
    const percentage = parseInt(text?.replace('%', '') || '0');

    return {
      processed: 0,
      total: 0,
      percentage,
    };
  }

  async waitForBatchCompletion(
    timeout = 120000,
    pollInterval = 2000
  ): Promise<'SUCCEEDED' | 'FAILED' | 'CANCELLED'> {
    const endTime = Date.now() + timeout;

    while (Date.now() < endTime) {
      await this.refreshButton.click();
      await this.page.waitForTimeout(500);

      const status = await this.getBatchStatus();

      if (['SUCCEEDED', 'FAILED', 'CANCELLED'].includes(status)) {
        return status as 'SUCCEEDED' | 'FAILED' | 'CANCELLED';
      }

      await this.page.waitForTimeout(pollInterval);
    }

    throw new Error(`Batch did not complete within ${timeout}ms`);
  }

  async getTaskCount(): Promise<number> {
    const taskItems = this.tasksList.locator('.hover\\:bg-slate-50, div[class*="cursor-pointer"]');
    return taskItems.count();
  }

  async clickApproveAll(): Promise<void> {
    this.acceptConfirmDialog();
    await this.approveAllButton.click();
  }

  async clickRejectAll(): Promise<void> {
    this.acceptConfirmDialog();
    await this.rejectAllButton.click();
  }

  async clickRetryFailed(): Promise<void> {
    this.acceptConfirmDialog();
    await this.retryFailedButton.click();
  }

  async clickCancel(): Promise<void> {
    this.acceptConfirmDialog();
    await this.cancelButton.click();
  }

  async clickDelete(): Promise<void> {
    this.acceptConfirmDialog();
    await this.deleteButton.click();
  }

  async clickFinalize(): Promise<void> {
    await this.finalizeButton.click();
  }

  async clickExport(): Promise<void> {
    await this.exportButton.click();
  }

  async pauseBatch(): Promise<void> {
    const pauseButton = this.page.getByRole('button', { name: /pause/i });
    await pauseButton.click();
  }

  async resumeBatch(): Promise<void> {
    const resumeButton = this.page.getByRole('button', { name: /resume/i });
    await resumeButton.click();
  }

  async getBatchCount(): Promise<number> {
    const rows = this.batchesTableBody.locator('tr');
    return rows.count();
  }

  async getJobPhases(): Promise<string[]> {
    const jobItems = this.page.locator('div').filter({ hasText: /manifest_ingest|scrape|entity_resolution/i });
    const count = await jobItems.count();
    const phases: string[] = [];

    for (let i = 0; i < count; i++) {
      const text = await jobItems.nth(i).textContent();
      if (text) {
        const match = text.match(/manifest_ingest|scrape|entity_resolution/i);
        if (match) phases.push(match[0].toUpperCase());
      }
    }

    return [...new Set(phases)];
  }

  async clickTask(sourceUrlOrKey: string): Promise<void> {
    const taskItem = this.page.locator('div').filter({ hasText: sourceUrlOrKey }).locator('.cursor-pointer, .hover\\:bg-slate-50').first();
    await taskItem.click();
  }

  async isLogViewerOpen(): Promise<boolean> {
    return this.page.getByText(/task log|log viewer/i).isVisible();
  }

  async closeLogViewer(): Promise<void> {
    const closeButton = this.page.locator('button').filter({ has: this.page.locator('span:text("close")') });
    if (await closeButton.isVisible()) {
      await closeButton.click();
    }
  }
}
