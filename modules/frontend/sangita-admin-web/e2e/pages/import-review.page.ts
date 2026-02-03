import { Page, Locator, expect } from '@playwright/test';
import { BasePage } from './base.page';

export class ImportReviewPage extends BasePage {
  readonly heading: Locator;
  readonly pendingCountLabel: Locator;
  readonly importList: Locator;
  readonly detailPanel: Locator;
  readonly selectAllCheckbox: Locator;
  readonly refreshButton: Locator;
  readonly approveButton: Locator;
  readonly rejectButton: Locator;
  readonly bulkApproveButton: Locator;
  readonly bulkRejectButton: Locator;
  readonly selectedCountLabel: Locator;

  // Override form fields - use locators that find input next to label text
  readonly titleInput: Locator;
  readonly composerInput: Locator;
  readonly ragaInput: Locator;
  readonly talaInput: Locator;
  readonly deityInput: Locator;
  readonly templeInput: Locator;
  readonly languageInput: Locator;
  readonly lyricsTextarea: Locator;

  // Resolution panel
  readonly resolutionPanel: Locator;

  constructor(page: Page) {
    super(page);
    this.heading = page.locator('h1, h2').filter({ hasText: /review/i }).first();
    this.pendingCountLabel = page.getByText(/pending \(\d+\)/i);
    this.importList = page.locator('.overflow-y-auto').first();
    this.detailPanel = page.locator('div').filter({ hasText: 'Primary Metadata' }).first();
    this.selectAllCheckbox = page.locator('input[type="checkbox"]').first();
    this.refreshButton = page.locator('button').filter({ has: page.locator('span:text("refresh")') });
    this.approveButton = page.getByRole('button', { name: /approve/i }).filter({ hasNotText: /selected/i });
    this.rejectButton = page.getByRole('button', { name: /^reject$/i });
    this.bulkApproveButton = page.getByRole('button', { name: /approve selected/i });
    this.bulkRejectButton = page.getByRole('button', { name: /reject selected/i });
    this.selectedCountLabel = page.locator('.bg-slate-100').filter({ hasText: /selected/i });

    // Override form fields - find input elements within label containers
    // The structure is: <div><label>Title</label><input/></div>
    this.titleInput = page.locator('label:has-text("Title")').locator('xpath=following-sibling::input | following-sibling::*//input').first();
    this.composerInput = page.locator('label:has-text("Composer")').locator('xpath=following-sibling::input | following-sibling::*//input').first();
    this.ragaInput = page.locator('label:has-text("Raga")').locator('xpath=following-sibling::input | following-sibling::*//input').first();
    this.talaInput = page.locator('label:has-text("Tala")').locator('xpath=following-sibling::input | following-sibling::*//input').first();
    this.deityInput = page.locator('label:has-text("Deity")').locator('xpath=following-sibling::input | following-sibling::*//input').first();
    this.templeInput = page.locator('label:has-text("Temple")').locator('xpath=following-sibling::input | following-sibling::*//input').first();
    this.languageInput = page.locator('label:has-text("Language")').locator('xpath=following-sibling::input | following-sibling::*//input').first();
    this.lyricsTextarea = page.locator('label:has-text("Lyrics")').locator('xpath=following-sibling::textarea | following-sibling::*//textarea').first();

    // Resolution panel
    this.resolutionPanel = page.locator('.bg-indigo-50').filter({ hasText: /AI Resolution|Candidates/i });
  }

  async goto(): Promise<void> {
    await this.page.goto('/bulk-import/review');
    // Wait for page to load
    await this.page.waitForLoadState('networkidle');
  }

  async getPendingCount(): Promise<number> {
    const text = await this.pendingCountLabel.textContent();
    const match = text?.match(/\((\d+)\)/);
    return match ? parseInt(match[1]) : 0;
  }

  async hasImports(): Promise<boolean> {
    // Check if there are any import items in the list
    const importItems = this.page.locator('.font-semibold.text-sm.text-ink-900');
    const count = await importItems.count();
    return count > 0;
  }

  async selectImportByTitle(title: string): Promise<void> {
    const importItem = this.page.locator('div').filter({ hasText: title }).locator('.cursor-pointer').first();
    await importItem.click();
    // Wait for detail panel to populate
    await this.page.waitForTimeout(500);
  }

  async selectImportByIndex(index: number): Promise<void> {
    const importItems = this.page.locator('.font-semibold.text-sm.text-ink-900');
    await importItems.nth(index).click();
    await this.page.waitForTimeout(500);
  }

  async approveSelectedImport(): Promise<void> {
    await this.approveButton.click();
    await this.waitForToast('success');
  }

  async rejectSelectedImport(): Promise<void> {
    this.acceptConfirmDialog();
    await this.rejectButton.click();
    await this.waitForToast('success');
  }

  async toggleCheckboxForImport(title: string): Promise<void> {
    const importRow = this.page.locator('div').filter({ hasText: title }).first();
    const checkbox = importRow.locator('input[type="checkbox"]').first();
    await checkbox.click();
  }

  async bulkSelectAll(): Promise<void> {
    await this.selectAllCheckbox.check();
  }

  async bulkDeselectAll(): Promise<void> {
    await this.selectAllCheckbox.uncheck();
  }

  async getSelectedCount(): Promise<number> {
    const isVisible = await this.selectedCountLabel.isVisible();
    if (!isVisible) return 0;

    const text = await this.selectedCountLabel.textContent();
    const match = text?.match(/(\d+)\s*selected/i);
    return match ? parseInt(match[1]) : 0;
  }

  async bulkApproveSelected(): Promise<void> {
    this.acceptConfirmDialog();
    await this.bulkApproveButton.click();
    await this.waitForToast('success');
  }

  async bulkRejectSelected(): Promise<void> {
    this.acceptConfirmDialog();
    await this.bulkRejectButton.click();
    await this.waitForToast('success');
  }

  async getOverrideFieldValue(field: 'title' | 'composer' | 'raga' | 'tala' | 'deity' | 'temple' | 'language'): Promise<string> {
    const inputMap: Record<string, Locator> = {
      title: this.titleInput,
      composer: this.composerInput,
      raga: this.ragaInput,
      tala: this.talaInput,
      deity: this.deityInput,
      temple: this.templeInput,
      language: this.languageInput,
    };
    return inputMap[field].inputValue();
  }

  async setOverrideField(field: 'title' | 'composer' | 'raga' | 'tala' | 'deity' | 'temple' | 'language', value: string): Promise<void> {
    const inputMap: Record<string, Locator> = {
      title: this.titleInput,
      composer: this.composerInput,
      raga: this.ragaInput,
      tala: this.talaInput,
      deity: this.deityInput,
      temple: this.templeInput,
      language: this.languageInput,
    };
    await inputMap[field].fill(value);
  }

  async getLyricsPreview(): Promise<string> {
    return this.lyricsTextarea.inputValue();
  }

  async setLyricsPreview(lyrics: string): Promise<void> {
    await this.lyricsTextarea.fill(lyrics);
  }

  async isResolutionPanelVisible(): Promise<boolean> {
    return this.resolutionPanel.isVisible();
  }

  async clickResolutionCandidate(entityType: 'COMPOSER' | 'RAGA' | 'DEITY' | 'TEMPLE', candidateName: string): Promise<void> {
    const entitySection = this.resolutionPanel.locator(`div:has-text("${entityType}")`).first();
    const candidateButton = entitySection.locator('button').filter({ hasText: candidateName });
    await candidateButton.click();
  }

  async getSourceUrl(): Promise<string> {
    const sourceLink = this.page.locator('a[href*="http"]').first();
    const href = await sourceLink.getAttribute('href');
    return href || '';
  }

  async refreshQueue(): Promise<void> {
    await this.refreshButton.click();
    await this.page.waitForTimeout(500);
  }

  async isQueueEmpty(): Promise<boolean> {
    const emptyMessage = this.page.getByText(/queue is empty|no pending/i);
    return emptyMessage.isVisible();
  }

  async getImportTitles(): Promise<string[]> {
    const importItems = this.page.locator('.font-semibold.text-sm.text-ink-900');
    const count = await importItems.count();
    const titles: string[] = [];

    for (let i = 0; i < count; i++) {
      const text = await importItems.nth(i).textContent();
      if (text) titles.push(text.trim());
    }

    return titles;
  }
}
