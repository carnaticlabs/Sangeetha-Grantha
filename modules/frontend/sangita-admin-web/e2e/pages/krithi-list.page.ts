import { Page, Locator, expect } from '@playwright/test';
import { BasePage } from './base.page';

export class KrithiListPage extends BasePage {
  readonly heading: Locator;
  readonly searchInput: Locator;
  readonly krithiTable: Locator;
  readonly krithiTableBody: Locator;
  readonly loadingIndicator: Locator;
  readonly emptyMessage: Locator;
  readonly paginationInfo: Locator;

  constructor(page: Page) {
    super(page);
    this.heading = page.getByRole('heading', { name: /krithis/i });
    this.searchInput = page.getByPlaceholder(/search/i);
    this.krithiTable = page.getByRole('table');
    this.krithiTableBody = this.krithiTable.locator('tbody');
    this.loadingIndicator = page.getByText(/loading/i);
    this.emptyMessage = page.getByText(/no krithis found/i);
    this.paginationInfo = page.locator('.text-sm').filter({ hasText: /showing/i });
  }

  async goto(): Promise<void> {
    await this.page.goto('/krithis');
    await expect(this.heading).toBeVisible();
    await this.waitForLoadingToComplete();
  }

  async search(query: string): Promise<void> {
    await this.searchInput.fill(query);
    await this.searchInput.press('Enter');
    // Wait for the API response
    await this.page.waitForResponse(
      (resp) => resp.url().includes('/krithis') && resp.status() === 200,
      { timeout: 10000 }
    );
    await this.page.waitForTimeout(500); // Allow UI to update
  }

  async clearSearch(): Promise<void> {
    await this.searchInput.clear();
    await this.searchInput.press('Enter');
    await this.page.waitForResponse(
      (resp) => resp.url().includes('/krithis') && resp.status() === 200,
      { timeout: 10000 }
    );
  }

  async getKrithiCount(): Promise<number> {
    const rows = this.krithiTableBody.locator('tr');
    return rows.count();
  }

  async krithiExists(title: string): Promise<boolean> {
    const row = this.krithiTableBody.locator('tr').filter({ hasText: title });
    return row.isVisible();
  }

  async clickKrithi(title: string): Promise<void> {
    const row = this.krithiTableBody.locator('tr').filter({ hasText: title });
    await row.click();
  }

  async getKrithiTitles(): Promise<string[]> {
    const titleCells = this.krithiTableBody.locator('tr td:first-child');
    const count = await titleCells.count();
    const titles: string[] = [];

    for (let i = 0; i < count; i++) {
      const text = await titleCells.nth(i).textContent();
      if (text) titles.push(text.trim());
    }

    return titles;
  }

  async getKrithiDetails(title: string): Promise<{
    title: string;
    composer: string;
    raga: string;
    status: string;
  } | null> {
    const row = this.krithiTableBody.locator('tr').filter({ hasText: title }).first();
    const isVisible = await row.isVisible();
    if (!isVisible) return null;

    const cells = row.locator('td');
    const cellCount = await cells.count();
    if (cellCount < 4) return null;

    return {
      title: (await cells.nth(0).textContent())?.trim() || '',
      composer: (await cells.nth(1).textContent())?.trim() || '',
      raga: (await cells.nth(2).textContent())?.trim() || '',
      status: (await cells.nth(3).textContent())?.trim() || '',
    };
  }

  async waitForKrithiToAppear(title: string, timeout = 10000): Promise<void> {
    const row = this.krithiTableBody.locator('tr').filter({ hasText: title });
    await expect(row).toBeVisible({ timeout });
  }

  async isEmpty(): Promise<boolean> {
    return this.emptyMessage.isVisible();
  }

  async getPaginationTotal(): Promise<number> {
    const isVisible = await this.paginationInfo.isVisible();
    if (!isVisible) return 0;

    const text = await this.paginationInfo.textContent();
    const match = text?.match(/of\s*(\d+)/i);
    return match ? parseInt(match[1]) : 0;
  }
}
