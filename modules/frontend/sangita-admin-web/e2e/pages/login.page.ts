import { Page, Locator, expect } from '@playwright/test';
import { BasePage } from './base.page';

export class LoginPage extends BasePage {
  readonly adminTokenInput: Locator;
  readonly emailInput: Locator;
  readonly signInButton: Locator;
  readonly errorAlert: Locator;
  readonly pageTitle: Locator;

  constructor(page: Page) {
    super(page);
    this.adminTokenInput = page.getByLabel('Admin Token');
    this.emailInput = page.getByLabel('Email');
    this.signInButton = page.getByRole('button', { name: /sign in/i });
    this.errorAlert = page.locator('.bg-red-50');
    this.pageTitle = page.getByRole('heading', { name: 'Sangita Grantha' });
  }

  async goto(): Promise<void> {
    await this.page.goto('/login');
    await expect(this.pageTitle).toBeVisible();
  }

  async login(adminToken: string, email: string): Promise<void> {
    await this.adminTokenInput.fill(adminToken);
    await this.emailInput.fill(email);
    await this.signInButton.click();
  }

  async expectSuccessfulLogin(): Promise<void> {
    await expect(this.page).toHaveURL('/');
  }

  async expectLoginError(message?: string): Promise<void> {
    await expect(this.errorAlert).toBeVisible();
    if (message) {
      await expect(this.errorAlert).toContainText(message);
    }
  }

  async isLoggedIn(): Promise<boolean> {
    const url = this.page.url();
    return !url.includes('/login');
  }
}
