| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.2.0 |
| **Last Updated** | 2026-09-10 |
| **Author** | Sangeetha Grantha Team |
| **Document Type** | Current guide |

# Browser end-to-end tests

---

The Curator Console's Playwright suite exercises the configured application stack. Use the existing configuration and fixtures rather than creating a second Playwright setup from an old documentation example.

## Setup and commands

From the frontend module:

```bash
cd modules/frontend/sangita-admin-web
bun install --frozen-lockfile
bunx playwright install chromium
bun run test:e2e:money
```

Other scripts:

| Command | Purpose |
|:---|:---|
| `bun run test:e2e` | Full configured suite |
| `bun run test:e2e:headed` | Visible browser |
| `bun run test:e2e:debug` | Interactive debugging |
| `bun run test:e2e:report` | Open the HTML report |

## Environment and fixtures

[playwright.config.ts](../../../modules/frontend/sangita-admin-web/e2e/playwright.config.ts) uses Chromium, one worker, sequential stateful execution, auth setup, and a default frontend at `http://localhost:5001` (overridable with `FRONTEND_URL`). It reuses an available frontend or starts the Compose development stack through its `webServer` configuration.

[global setup](../../../modules/frontend/sangita-admin-web/e2e/global-setup.ts) and [fixtures](../../../modules/frontend/sangita-admin-web/e2e/fixtures) own user/session and data preparation. Inspect those before targeting an environment: E2E flows can create and mutate records. Source-driven suites may depend on external sites/provider configuration. Use an intended test environment, not a valuable unreviewed corpus.

The `test:e2e:money` script selects the money-path scenarios with `E2E_SKIP_SHARED_BATCH=1`. Full import suites have broader state/source prerequisites.

## What to inspect

A browser journey should follow the actual change through search, detail/edit/review, save, reload, and resulting state. For ingestion work, verify backing API/database evidence as well as a success toast. Include empty results, unavailable data, failed requests, and retry behavior when relevant.

Traces are retained on first retry; screenshots are captured on failure; HTML reports go to the configured Playwright report directory. Keep generated reports and auth state out of committed source.

[CI nightly workflow](../../../.github/workflows/e2e-nightly.yml) · [Integration testing](../integration-tests-approach.md) · [Post-import checks](./test-plan.md) · [Curator Console](../../05-frontend/admin-web/ui-specs.md)

---

[Section index](./README.md) · [Documentation home](./../../README.md) · [Feature status](./../../01-requirements/features/README.md)
