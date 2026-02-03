---
description: Runs frontend E2E tests using Playwright, with options for headed mode, debugging, and report viewing.
---

# E2E Test Runner

This workflow guides execution of frontend end-to-end tests using Playwright.

## Prerequisites

Ensure the full stack is running before executing E2E tests:
```bash
mise exec -- cargo run --manifest-path tools/sangita-cli/Cargo.toml -- dev --start-db
```

Or verify services are already running:
- Database: `localhost:5432`
- Backend API: `localhost:8080`
- Frontend: `localhost:5001`

## 1. Run All E2E Tests

**Trigger:** "Run E2E tests" or "Run frontend tests"

```bash
cd modules/frontend/sangita-admin-web && bun run test:e2e
```

**Expected Output:**
- Test results in terminal
- HTML report generated at `e2e/playwright-report/`

## 2. Run Specific Test File

**Trigger:** "Run E2E test for [feature]"

```bash
cd modules/frontend/sangita-admin-web && bun run test:e2e -- tests/<test-file>.spec.ts
```

**Available Test Files:**
| File | Coverage |
|:---|:---|
| `bulk-import-happy-path.spec.ts` | Full import flow success |
| `bulk-import-database.spec.ts` | Database state verification |
| `bulk-import-review.spec.ts` | Review workflow UI |
| `bulk-import-error-cases.spec.ts` | Error handling scenarios |

## 3. Debug Mode (Visual Browser)

**Trigger:** "Debug E2E test" or "Run E2E with browser visible"

### Headed Mode (see browser, no debugging)
```bash
cd modules/frontend/sangita-admin-web && bun run test:e2e:headed
```

### Full Debug Mode (step through, inspector)
```bash
cd modules/frontend/sangita-admin-web && bun run test:e2e:debug
```

## 4. View Test Report

**Trigger:** "Show E2E report" or "Open test results"

```bash
cd modules/frontend/sangita-admin-web && bun run test:e2e:report
```

This opens the Playwright HTML report in your browser.

## 5. Troubleshoot Failures

### Common Issues

**Authentication Failure:**
- Check `e2e/.auth/user.json` exists and is valid
- Re-run setup: `bun run test:e2e -- --project=setup`

**Timeout Errors:**
- Default timeout is 120s per test (configured in `playwright.config.ts`)
- For slow environments, set: `PLAYWRIGHT_TIMEOUT=180000 bun run test:e2e`

**Backend Not Responding:**
- Verify backend is running: `curl http://localhost:8080/health`
- Check logs: `mise exec -- cargo run --manifest-path tools/sangita-cli/Cargo.toml -- dev --start-db`

**Database State Issues:**
- Reset database: `mise exec -- cargo run --manifest-path tools/sangita-cli/Cargo.toml -- db reset`
- E2E tests use `e2e/fixtures/db-helpers.ts` for cleanup

### Analyzing Failures

1. Check terminal output for assertion errors
2. View screenshots in `e2e/test-results/` (captured on failure)
3. Open HTML report for detailed traces: `bun run test:e2e:report`

## 6. CI Integration

For CI environments, tests run with:
- `forbidOnly: true` (fails if `.only` is present)
- `retries: 1` (one retry on failure)
- Trace captured on first retry

Set `CI=true` environment variable to enable CI mode:
```bash
CI=true bun run test:e2e
```

## Page Objects Reference

Tests use page objects located in `e2e/pages/`:
| Page Object | Purpose |
|:---|:---|
| `login.page.ts` | Authentication flows |
| `bulk-import.page.ts` | Import submission UI |
| `import-review.page.ts` | Review/approval workflow |
| `krithi-list.page.ts` | Krithi listing and search |
