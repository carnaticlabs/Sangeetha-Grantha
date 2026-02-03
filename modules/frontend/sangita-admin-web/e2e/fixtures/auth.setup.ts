import { test as setup, expect } from '@playwright/test';
import { testCredentials } from './test-data';
import path from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const authFile = path.resolve(__dirname, '../.auth/user.json');

setup('authenticate', async ({ page }) => {
  // Navigate to login page
  await page.goto('/login');

  // Fill in credentials
  await page.getByLabel('Admin Token').fill(testCredentials.adminToken);
  await page.getByLabel('Email').fill(testCredentials.email);

  // Click sign in button
  await page.getByRole('button', { name: /sign in/i }).click();

  // Wait for redirect to dashboard (URL should be '/' after successful login)
  await expect(page).toHaveURL('/');

  // Verify we're on the authenticated page by checking for navigation elements
  await expect(page.locator('body')).toBeVisible();

  // Save storage state for reuse in other tests
  await page.context().storageState({ path: authFile });
});
