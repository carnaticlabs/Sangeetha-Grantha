| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.1.0 |
| **Last Updated** | 2026-02-08 |
| **Author** | Sangeetha Grantha Team |

# End-to-End Testing Guide

This document describes the E2E testing strategy, setup, and test scenarios for Sangita Grantha.

---

## 1. Overview

### 1.1 E2E Testing Philosophy

- **User-centric**: Tests simulate real user workflows
- **Critical paths first**: Focus on high-value journeys
- **Stable and reliable**: Tests should not flake
- **Fast feedback**: Run in CI within reasonable time

### 1.2 Testing Stack

| Component | Tool | Purpose |
|-----------|------|---------|
| **Browser automation** | Playwright | Cross-browser testing |
| **API testing** | REST Client / cURL | Backend verification |
| **Test runner** | Playwright Test | Test execution |
| **Assertions** | Playwright Expect | Verification |

---

## 2. Environment Setup

### 2.1 Prerequisites

```bash
# Install Playwright
cd modules/frontend/sangita-admin-web
bun add -D @playwright/test
bunx playwright install
```

### 2.2 Configuration

Create `playwright.config.ts`:

```typescript
import { defineConfig, devices } from '@playwright/test';

export default defineConfig({
  testDir: './e2e',
  timeout: 30000,
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 0,
  workers: process.env.CI ? 1 : undefined,
  reporter: [
    ['html'],
    ['json', { outputFile: 'test-results.json' }]
  ],
  use: {
    baseURL: 'http://localhost:5001',
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
  },
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
    {
      name: 'firefox',
      use: { ...devices['Desktop Firefox'] },
    },
  ],
  webServer: {
    command: 'bun run dev',
    url: 'http://localhost:5001',
    reuseExistingServer: !process.env.CI,
  },
});
```

### 2.3 Test Data Setup

Create `e2e/fixtures/test-data.ts`:

```typescript
export const testAdmin = {
  token: 'dev-admin-token',
  userId: process.env.TEST_ADMIN_USER_ID || 'get-from-db',
};

export const testKrithi = {
  title: 'E2E Test Krithi',
  composer: 'Tyagaraja',
  raga: 'Mohanam',
  tala: 'Adi',
};
```

---

## 3. Test Structure

### 3.1 Directory Structure

```text
modules/frontend/sangita-admin-web/
├── e2e/
│   ├── fixtures/
│   │   ├── test-data.ts
│   │   └── auth.setup.ts
│   ├── pages/
│   │   ├── login.page.ts
│   │   ├── dashboard.page.ts
│   │   ├── krithi-list.page.ts
│   │   └── krithi-editor.page.ts
│   ├── tests/
│   │   ├── auth.spec.ts
│   │   ├── krithi-crud.spec.ts
│   │   ├── search.spec.ts
│   │   └── import.spec.ts
│   └── utils/
│       └── api-helpers.ts
├── playwright.config.ts
└── package.json
```

### 3.2 Page Object Pattern

Create `e2e/pages/login.page.ts`:

```typescript
import { Page, Locator } from '@playwright/test';

export class LoginPage {
  readonly page: Page;
  readonly adminTokenInput: Locator;
  readonly userIdInput: Locator;
  readonly loginButton: Locator;
  readonly errorMessage: Locator;

  constructor(page: Page) {
    this.page = page;
    this.adminTokenInput = page.getByLabel('Admin Token');
    this.userIdInput = page.getByLabel('User ID');
    this.loginButton = page.getByRole('button', { name: 'Login' });
    this.errorMessage = page.getByRole('alert');
  }

  async goto() {
    await this.page.goto('/login');
  }

  async login(token: string, userId: string) {
    await this.adminTokenInput.fill(token);
    await this.userIdInput.fill(userId);
    await this.loginButton.click();
  }
}
```

Create `e2e/pages/krithi-list.page.ts`:

```typescript
import { Page, Locator } from '@playwright/test';

export class KrithiListPage {
  readonly page: Page;
  readonly searchInput: Locator;
  readonly createButton: Locator;
  readonly krithiTable: Locator;

  constructor(page: Page) {
    this.page = page;
    this.searchInput = page.getByPlaceholder('Search krithis...');
    this.createButton = page.getByRole('button', { name: 'Create Krithi' });
    this.krithiTable = page.getByRole('table');
  }

  async goto() {
    await this.page.goto('/krithis');
  }

  async search(query: string) {
    await this.searchInput.fill(query);
    await this.searchInput.press('Enter');
  }

  async getKrithiCount(): Promise<number> {
    const rows = await this.krithiTable.locator('tbody tr').count();
    return rows;
  }
}
```

---

## 4. Test Scenarios

### 4.1 Authentication Tests

Create `e2e/tests/auth.spec.ts`:

```typescript
import { test, expect } from '@playwright/test';
import { LoginPage } from '../pages/login.page';
import { testAdmin } from '../fixtures/test-data';

test.describe('Authentication', () => {
  test('should login with valid credentials', async ({ page }) => {
    const loginPage = new LoginPage(page);
    await loginPage.goto();

    await loginPage.login(testAdmin.token, testAdmin.userId);

    await expect(page).toHaveURL('/dashboard');
    await expect(page.getByText('Dashboard')).toBeVisible();
  });

  test('should show error with invalid token', async ({ page }) => {
    const loginPage = new LoginPage(page);
    await loginPage.goto();

    await loginPage.login('invalid-token', testAdmin.userId);

    await expect(loginPage.errorMessage).toBeVisible();
    await expect(loginPage.errorMessage).toContainText('Invalid credentials');
  });

  test('should redirect to login when not authenticated', async ({ page }) => {
    await page.goto('/krithis');
    await expect(page).toHaveURL('/login');
  });

  test('should logout successfully', async ({ page }) => {
    // Setup: login first
    const loginPage = new LoginPage(page);
    await loginPage.goto();
    await loginPage.login(testAdmin.token, testAdmin.userId);

    // Logout
    await page.getByRole('button', { name: 'Logout' }).click();

    await expect(page).toHaveURL('/login');
  });
});
```

### 4.2 Krithi CRUD Tests

Create `e2e/tests/krithi-crud.spec.ts`:

```typescript
import { test, expect } from '@playwright/test';
import { LoginPage } from '../pages/login.page';
import { KrithiListPage } from '../pages/krithi-list.page';
import { testAdmin, testKrithi } from '../fixtures/test-data';

test.describe('Krithi CRUD', () => {
  test.beforeEach(async ({ page }) => {
    // Login before each test
    const loginPage = new LoginPage(page);
    await loginPage.goto();
    await loginPage.login(testAdmin.token, testAdmin.userId);
    await expect(page).toHaveURL('/dashboard');
  });

  test('should create a new krithi', async ({ page }) => {
    // Navigate to krithi list
    await page.goto('/krithis');

    // Click create button
    await page.getByRole('button', { name: 'Create Krithi' }).click();

    // Fill form
    await page.getByLabel('Title').fill(testKrithi.title);
    await page.getByLabel('Composer').selectOption({ label: testKrithi.composer });
    await page.getByLabel('Raga').selectOption({ label: testKrithi.raga });
    await page.getByLabel('Tala').selectOption({ label: testKrithi.tala });

    // Submit
    await page.getByRole('button', { name: 'Create' }).click();

    // Verify success
    await expect(page.getByText('Krithi created successfully')).toBeVisible();
    await expect(page).toHaveURL(/\/krithis\/[a-f0-9-]+/);
  });

  test('should search for krithi', async ({ page }) => {
    const krithiList = new KrithiListPage(page);
    await krithiList.goto();

    // Search for a known krithi
    await krithiList.search('Vatapi');

    // Verify results
    await expect(page.getByText('Vatapi Ganapatim')).toBeVisible();
  });

  test('should edit krithi title', async ({ page }) => {
    // Navigate to an existing krithi
    await page.goto('/krithis');
    await page.getByText('Test Krithi').first().click();

    // Click edit
    await page.getByRole('button', { name: 'Edit' }).click();

    // Update title
    const titleInput = page.getByLabel('Title');
    await titleInput.clear();
    await titleInput.fill('Updated Krithi Title');

    // Save
    await page.getByRole('button', { name: 'Save' }).click();

    // Verify
    await expect(page.getByText('Krithi updated successfully')).toBeVisible();
    await expect(page.getByText('Updated Krithi Title')).toBeVisible();
  });

  test('should add lyric variant', async ({ page }) => {
    // Navigate to krithi detail
    await page.goto('/krithis');
    await page.getByText('Test Krithi').first().click();

    // Add variant
    await page.getByRole('button', { name: 'Add Variant' }).click();

    // Fill variant form
    await page.getByLabel('Language').selectOption('Tamil');
    await page.getByLabel('Script').selectOption('Tamil');
    await page.getByLabel('Pallavi').fill('Test pallavi lyrics in Tamil');

    // Save
    await page.getByRole('button', { name: 'Save Variant' }).click();

    // Verify
    await expect(page.getByText('Variant added successfully')).toBeVisible();
    await expect(page.getByText('Tamil (Tamil script)')).toBeVisible();
  });
});
```

### 4.3 Search Tests

Create `e2e/tests/search.spec.ts`:

```typescript
import { test, expect } from '@playwright/test';
import { LoginPage } from '../pages/login.page';
import { testAdmin } from '../fixtures/test-data';

test.describe('Search Functionality', () => {
  test.beforeEach(async ({ page }) => {
    const loginPage = new LoginPage(page);
    await loginPage.goto();
    await loginPage.login(testAdmin.token, testAdmin.userId);
  });

  test('should filter by composer', async ({ page }) => {
    await page.goto('/krithis');

    // Select composer filter
    await page.getByLabel('Composer').selectOption({ label: 'Tyagaraja' });

    // Wait for results
    await page.waitForResponse(resp =>
      resp.url().includes('/v1/admin/krithis') && resp.status() === 200
    );

    // Verify all results are by Tyagaraja
    const composerCells = await page.locator('td:nth-child(2)').allTextContents();
    for (const cell of composerCells) {
      expect(cell).toBe('Tyagaraja');
    }
  });

  test('should filter by workflow state', async ({ page }) => {
    await page.goto('/krithis');

    // Select draft filter
    await page.getByLabel('Status').selectOption('draft');

    // Verify results show draft status
    await expect(page.locator('.badge:has-text("Draft")')).toHaveCount(await page.locator('tbody tr').count());
  });

  test('should combine multiple filters', async ({ page }) => {
    await page.goto('/krithis');

    // Apply multiple filters
    await page.getByLabel('Composer').selectOption({ label: 'Tyagaraja' });
    await page.getByLabel('Raga').selectOption({ label: 'Mohanam' });
    await page.getByPlaceholder('Search').fill('endaro');

    // Verify filtered results
    await expect(page.getByText('Endaro Mahanubhavulu')).toBeVisible();
  });

  test('should clear filters', async ({ page }) => {
    await page.goto('/krithis');

    // Apply filter
    await page.getByLabel('Composer').selectOption({ label: 'Tyagaraja' });

    // Get filtered count
    const filteredCount = await page.locator('tbody tr').count();

    // Clear filters
    await page.getByRole('button', { name: 'Clear Filters' }).click();

    // Verify more results now
    const unfilteredCount = await page.locator('tbody tr').count();
    expect(unfilteredCount).toBeGreaterThanOrEqual(filteredCount);
  });
});
```

### 4.4 Import Pipeline Tests

Create `e2e/tests/import.spec.ts`:

```typescript
import { test, expect } from '@playwright/test';
import { LoginPage } from '../pages/login.page';
import { testAdmin } from '../fixtures/test-data';
import path from 'path';

test.describe('Import Pipeline', () => {
  test.beforeEach(async ({ page }) => {
    const loginPage = new LoginPage(page);
    await loginPage.goto();
    await loginPage.login(testAdmin.token, testAdmin.userId);
  });

  test('should upload CSV file', async ({ page }) => {
    await page.goto('/imports');

    // Upload file
    const fileInput = page.locator('input[type="file"]');
    await fileInput.setInputFiles(path.join(__dirname, '../fixtures/test-import.csv'));

    // Submit
    await page.getByRole('button', { name: 'Upload' }).click();

    // Verify success
    await expect(page.getByText('Import completed')).toBeVisible();
    await expect(page.getByText('5 records imported')).toBeVisible();
  });

  test('should review imported krithi', async ({ page }) => {
    await page.goto('/imports');

    // Click on pending import
    await page.getByText('Pending').first().click();

    // Verify review page
    await expect(page.getByText('Review Import')).toBeVisible();
    await expect(page.getByText('Raw Data')).toBeVisible();
    await expect(page.getByText('Suggested Matches')).toBeVisible();
  });

  test('should map import to existing krithi', async ({ page }) => {
    await page.goto('/imports');
    await page.getByText('Pending').first().click();

    // Select suggested match
    await page.getByRole('radio', { name: /Vatapi Ganapatim/i }).click();

    // Confirm mapping
    await page.getByRole('button', { name: 'Map to Existing' }).click();

    // Verify
    await expect(page.getByText('Import mapped successfully')).toBeVisible();
  });

  test('should reject import with reason', async ({ page }) => {
    await page.goto('/imports');
    await page.getByText('Pending').first().click();

    // Click reject
    await page.getByRole('button', { name: 'Reject' }).click();

    // Enter reason
    await page.getByLabel('Rejection Reason').fill('Duplicate entry - already exists');

    // Confirm
    await page.getByRole('button', { name: 'Confirm Reject' }).click();

    // Verify
    await expect(page.getByText('Import rejected')).toBeVisible();
  });
});
```

---

## 5. Running Tests

### 5.1 Local Execution

```bash
# Run all tests
bunx playwright test

# Run specific test file
bunx playwright test auth.spec.ts

# Run with UI mode
bunx playwright test --ui

# Run in headed mode (see browser)
bunx playwright test --headed

# Run specific browser
bunx playwright test --project=chromium
```

### 5.2 CI Execution

```yaml
# .github/workflows/e2e.yml
name: E2E Tests

on:
  push:
    branches: [main]
  pull_request:

jobs:
  e2e:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Setup Bun
        uses: oven-sh/setup-bun@v1

      - name: Install dependencies
        run: |
          cd modules/frontend/sangita-admin-web
          bun install

      - name: Install Playwright
        run: bunx playwright install --with-deps

      - name: Start backend
        run: ./gradlew :modules:backend:api:run &

      - name: Wait for backend
        run: |
          timeout 60 bash -c 'until curl -s http://localhost:8080/health; do sleep 1; done'

      - name: Run E2E tests
        run: |
          cd modules/frontend/sangita-admin-web
          bunx playwright test

      - name: Upload report
        uses: actions/upload-artifact@v4
        if: always()
        with:
          name: playwright-report
          path: modules/frontend/sangita-admin-web/playwright-report
```

### 5.3 Debugging Failures

```bash
# View HTML report
bunx playwright show-report

# Run with trace viewer
bunx playwright test --trace on

# Debug specific test
bunx playwright test auth.spec.ts --debug
```

---

## 6. Best Practices

### 6.1 Test Isolation

- Each test should be independent
- Use `beforeEach` for setup, not shared state
- Clean up test data after tests

### 6.2 Selectors

**Prefer (in order):**
1. `getByRole` - most accessible
2. `getByLabel` - form elements
3. `getByText` - visible text
4. `getByTestId` - last resort

**Avoid:**
- CSS selectors (`.class`, `#id`)
- XPath
- Index-based selectors

### 6.3 Assertions

```typescript
// Good - specific assertion
await expect(page.getByText('Krithi created')).toBeVisible();

// Avoid - vague assertion
await expect(page).toHaveURL('/krithis');

// Good - wait for network
await page.waitForResponse(resp => resp.url().includes('/krithis'));

// Avoid - arbitrary wait
await page.waitForTimeout(2000);
```

### 6.4 Test Data

- Use factories for test data generation
- Clean up after tests
- Use unique identifiers to avoid conflicts

---

## 7. API Testing with cURL

For API-only E2E testing without browser:

```bash
#!/bin/bash
# e2e/api-tests.sh

BASE_URL="http://localhost:8080"
ADMIN_TOKEN="dev-admin-token"
USER_ID="<admin-user-id>"

# Get JWT
JWT=$(curl -s -X POST "$BASE_URL/auth/token" \
  -H "Content-Type: application/json" \
  -d "{\"adminToken\": \"$ADMIN_TOKEN\", \"userId\": \"$USER_ID\"}" \
  | jq -r '.token')

echo "JWT obtained: ${JWT:0:20}..."

# Test search
echo "Testing search..."
SEARCH_RESULT=$(curl -s "$BASE_URL/v1/krithis/search")
SEARCH_COUNT=$(echo $SEARCH_RESULT | jq '.total')
echo "Search returned $SEARCH_COUNT results"

# Test create
echo "Testing create..."
CREATE_RESULT=$(curl -s -X POST "$BASE_URL/v1/admin/krithis" \
  -H "Authorization: Bearer $JWT" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "E2E Test Krithi",
    "composerId": "<composer-id>",
    "musicalForm": "KRITHI",
    "primaryLanguage": "sa"
  }')

KRITHI_ID=$(echo $CREATE_RESULT | jq -r '.id')
echo "Created krithi: $KRITHI_ID"

# Cleanup
curl -s -X DELETE "$BASE_URL/v1/admin/krithis/$KRITHI_ID" \
  -H "Authorization: Bearer $JWT"

echo "E2E API tests completed"
```

---

## 8. Related Documents

- [Test Plan](./test-plan.md)
- [Steel Thread Implementation](../../06-backend/steel-thread-implementation.md)
- [API Contract](../../03-api/api-contract.md)
- [Troubleshooting](../../00-onboarding/troubleshooting.md)