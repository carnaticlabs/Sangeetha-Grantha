| Metadata | Value |
|:---|:---|
| **Status** | In Progress (baseline done; coverage pending) |
| **Version** | 1.1.0 |
| **Last Updated** | 2026-06-24 |
| **Author** | Sangeetha Grantha Team |
| **Priority** | P2 |
| **Epic** | [TRACK-109](./TRACK-109-production-readiness-roadmap.md) (W2 Quality) |
| **Decisions** | D13 ([decision log](../../application_documentation/north-star-production-readiness-decision.md)) |
| **Depends on** | TRACK-111 (CI must exist first) |

# TRACK-118: Frontend Component Tests (Vitest)

## Goal

Component tests on the most business-critical UI, kept off the backend critical path. Estimated ~1 week. Deliberately scheduled **after** CI exists (TRACK-111) so it does not dilute the backend foundation.

## Context

North-star N3: zero frontend tests across 113 TS/TSX files, including the 688-line `CuratorReviewPage.tsx` and 857-line `BulkImport.tsx` — the most business-critical UI in the system.

## Implementation Plan

- [x] **Green Vitest + lint baseline** (2026-06-24) — precondition for [TRACK-121](./TRACK-121-frontend-major-toolchain-upgrade.md). See "Baseline delivered" below.
- [ ] Wire `vitest run` into CI (frontend job) — currently CI runs only typecheck + build under Bun (`oven-sh/setup-bun`).
- [ ] Tests for `CuratorReviewPage.tsx` — the curation workflow.
- [ ] Tests for `BulkImport.tsx` — the highest-volume write path UI.
- [ ] Decompose the 600–850-line page components into testable units as coverage is added.

## Baseline delivered (2026-06-24)

Purpose: give TRACK-121 a green `vitest run` + `bun run lint` to upgrade against (was red on both).

- **Lint 19 errors → 0** (`bun run lint` exits 0; 189 warnings remain, pre-existing/tolerated):
  - Fixed properly: `String`→`string` (`src/api/client.ts`), `let`→`const` (`LyricsTab.tsx`),
    case-block braces + removed useless try/catch (`src/hooks/useBatchActions.ts`), removed the
    triple-slash reference (`vitest.config.ts`).
  - Config decisions in `eslint.config.js`: `react-hooks/set-state-in-effect` → `warn` (advisory
    react-hooks v7 perf rule; the 5 sites are intentional prop→state sync); added an `e2e/**`
    override turning off `no-empty-pattern` + `no-empty-object-type` (idiomatic Playwright fixtures).
- **Vitest green**: `vitest.config.ts` now scopes collection to `src/**/*.{test,spec}.{ts,tsx}` and
  excludes `e2e/` (Playwright has its own runner). Added `src/utils/enums.test.ts` (6 tests) as the
  first real unit coverage.
- **jsdom pinned `^27.4.0` → `^26.1.0`.** jsdom 27 pulls `html-encoding-sniffer@6 → @exodus/bytes`
  (pure ESM). The project runs on **Bun**, but Vitest's CLI has a `#!/usr/bin/env node` shebang, so
  `bunx vitest` shells out to the system's **EOL Node 21**, which can't `require()` that ESM dep
  (`ERR_REQUIRE_ESM`). jsdom 26 uses the CJS `html-encoding-sniffer@4` and is runtime-agnostic —
  verified green under **both** `bunx --bun vitest run` (Bun runtime) and the default Node path.
  - **This is a Bun project — do _not_ pin Node.** jsdom 26 is current-enough and needs no runtime
    pinning. If jsdom 27 is wanted later, run Vitest under Bun's runtime (`bunx --bun vitest run`, or
    `[run] bun = true` in `bunfig.toml`) so Node is never involved — rather than pinning a Node LTS.

## Remaining (the substantive N3 work)
Component coverage for `CuratorReviewPage.tsx` / `BulkImport.tsx` + decomposition, and CI wiring — the
~1-week effort this track was originally scoped for — is still open.

## Acceptance Criteria

- The two critical pages have meaningful component coverage.
- Runs in CI within the frontend time budget.

## References

- [North-Star Evaluation N3](../../application_documentation/north-star-evaluation.md)
- [Implementation Plan — TRACK-118](../../application_documentation/north-star-production-readiness-implementation-plan.md)
