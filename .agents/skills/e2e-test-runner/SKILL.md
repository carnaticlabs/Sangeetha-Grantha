---
name: e2e-test-runner
description: Runs frontend end-to-end tests using Playwright in modules/frontend/sangita-admin-web with options for headless, headed, UI debug mode, and HTML report generation. Use when validating UI flows, checking bulk import web journeys, or troubleshooting frontend test failures.
---

# E2E Test Runner

This skill guides execution of frontend end-to-end (E2E) journeys using Playwright in `modules/frontend/sangita-admin-web`.

> [!NOTE]
> E2E test suites run against the live full stack (PostgreSQL + Backend + Admin Web). Ensure `make dev` is running before running Playwright tests.

---

## Prerequisites

Verify required services are accessible:
- Database: `localhost:5432`
- Backend API: `http://localhost:8080/health`
- Frontend: `http://localhost:5001`

If not running, launch the stack:
```bash
make dev
```

---

## 1. Run All E2E Tests

Execute the default headless test run:

```bash
cd modules/frontend/sangita-admin-web && bun run test:e2e
```

To run money/smoke journeys without the full shared batch:
```bash
cd modules/frontend/sangita-admin-web && bun run test:e2e:money
```

---

## 2. Run Specific Test Journeys

Execute a targeted test file:

```bash
cd modules/frontend/sangita-admin-web && bun run test:e2e -- tests/<test-file>.spec.ts
```

### Available Test Suites:
| Test File | Focus Area |
|:---|:---|
| `bulk-import-happy-path.spec.ts` | Complete import submission to approval flow |
| `bulk-import-database.spec.ts` | Verification of database side-effects |
| `bulk-import-review.spec.ts` | Curator review and section resolution UI |
| `bulk-import-error-cases.spec.ts` | Malformed inputs and validation error handling |

---

## 3. Visual & Debugging Modes

### Headed Mode (Watch tests run in browser)
```bash
cd modules/frontend/sangita-admin-web && bun run test:e2e:headed
```

### Interactive Debugger (Playwright Inspector)
Step through individual actions and inspect DOM selectors:
```bash
cd modules/frontend/sangita-admin-web && bun run test:e2e:debug
```

---

## 4. View Test Reports & Traces

After a test run, launch the interactive HTML report:

```bash
cd modules/frontend/sangita-admin-web && bun run test:e2e:report
```

---

## 5. Troubleshooting Common Failures

### 1. Authentication Failure
- Verify auth storage state exists at `e2e/.auth/user.json`.
- Regenerate auth state: `bun run test:e2e -- --project=setup`

### 2. Timeouts
- Default timeout is 120s per test (set in `playwright.config.ts`).
- Override for slower environments: `PLAYWRIGHT_TIMEOUT=180000 bun run test:e2e`

### 3. Stale Database Fixtures
- Reset test state to clean migration fixtures:
```bash
make db-reset
```
