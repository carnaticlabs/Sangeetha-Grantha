import { Page, Locator, expect } from '@playwright/test';

export abstract class BasePage {
  readonly page: Page;
  readonly toastSuccess: Locator;
  readonly toastError: Locator;

  constructor(page: Page) {
    this.page = page;
    // Toast selectors based on ToastContainer component
    this.toastSuccess = page.locator('[role="status"]').filter({ hasText: /success|created|approved/i });
    this.toastError = page.locator('[role="alert"]').filter({ hasText: /error|failed/i });
  }

  abstract goto(): Promise<void>;

  async waitForToast(type: 'success' | 'error', timeout = 10000): Promise<string> {
    const toast = type === 'success' ? this.toastSuccess : this.toastError;
    await toast.first().waitFor({ timeout });
    return (await toast.first().textContent()) || '';
  }

  async expectToastMessage(type: 'success' | 'error', expectedText: string | RegExp): Promise<void> {
    const toast = type === 'success' ? this.toastSuccess : this.toastError;
    await expect(toast.first()).toBeVisible({ timeout: 10000 });
    if (typeof expectedText === 'string') {
      await expect(toast.first()).toContainText(expectedText);
    } else {
      await expect(toast.first()).toHaveText(expectedText);
    }
  }

  async waitForLoadingToComplete(): Promise<void> {
    // Wait for any loading indicators to disappear
    const loadingIndicator = this.page.getByText(/loading/i);
    if (await loadingIndicator.isVisible()) {
      await loadingIndicator.waitFor({ state: 'hidden', timeout: 30000 });
    }
  }

  async acceptConfirmDialog(): Promise<void> {
    this.page.once('dialog', (dialog) => dialog.accept());
  }

  async dismissConfirmDialog(): Promise<void> {
    this.page.once('dialog', (dialog) => dialog.dismiss());
  }
}
