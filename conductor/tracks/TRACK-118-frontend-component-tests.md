| Metadata | Value |
|:---|:---|
| **Status** | Completed |
| **Version** | 2.0.0 |
| **Last Updated** | 2026-07-09 |
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
- [x] Wire `vitest run` into CI (frontend job) — done 2026-07-09 (execution plan Step 1): `bun run test:unit` between Typecheck and Build.
- [x] Tests for `CuratorReviewPage.tsx` — the curation workflow (2026-07-09, execution plan Step 3).
- [x] Tests for `BulkImport.tsx` — the highest-volume write path UI (2026-07-09, execution plan Step 4).
- [x] Decompose the 600–850-line page components into testable units as coverage is added
      (2026-07-09): CuratorReviewPage → `components/curator-review/` (4 components);
      BulkImport → `components/bulk-import/` (`BatchList`, `TaskLogDrawer`, `UploadPanel`) +
      `utils/bulk-import-format.ts`. Remaining inline: BulkImport's detail panel (stepper/tasks/
      events) — covered by page tests; extract further only if it grows.

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

## Preparation survey (2026-07-09)

Codebase facts that shape the work:

- **Vitest baseline confirmed green**: `bunx vitest run` → 1 file / 6 tests in ~2s.
- **Mocking strategy: `vi.mock('../api/client')`.** Both pages consume *named* functions from
  `src/api/client.ts` (Curator: `getCuratorStats`, `getCuratorSectionIssues`, `getImports`,
  `reviewImport`, `searchKrithis`; BulkImport: `listBulkImportBatches`, `getBulkImportBatch`,
  `getBulkImportJobs/Tasks/Events`, `uploadBulkImportFile`; `useBatchActions` wraps 9 more batch
  mutations). One `fetch` wrapper (`request<T>`) sits behind all of them — module-mocking the client
  is sufficient; **no MSW dependency needed**.
- **Test infra gaps**: `@testing-library/jest-dom` is installed but not wired (`setupFiles: []`);
  `@testing-library/user-event` is missing; no shared render wrapper. Curator needs
  `QueryClientProvider` (2 `useQuery` calls + `useSourcingQueries`); BulkImport needs `MemoryRouter`
  (imports `Link`).
- **Decomposition seams already visible**:
  - `CuratorReviewPage.tsx` (688 lines): `StatCard`/`TabButton`/`FormField`/`SectionIssuesTab` are
    already separate components at the bottom of the file (lines 604–687) — extraction is mechanical.
    The 8 `overrideXxx` useState fields (lines 48–55) want a single reducer/object → `OverrideForm`.
  - `BulkImport.tsx` (857 lines): lines 22–104 are pure presentational maps + formatters
    (`statusChip`, `taskChip`, label maps, `formatDuration`, `formatDate`, `basename`, `parseError`)
    — extractable and directly unit-testable with zero rendering.
  - `useBatchActions` is a self-contained hook (switch over 9 client mutations + toasts) — good
    first hook-level test target.

### Execution plan

- [x] **Step 0 — infra (2026-07-09)**: added `@testing-library/user-event@14.6.1`; created
      `src/test/setup.ts` (`import '@testing-library/jest-dom/vitest'`) wired into
      `vitest.config.ts` `setupFiles`; created `src/test/test-utils.tsx` (custom `render` wrapping
      fresh `QueryClient` with `retry: false` + `MemoryRouter`, re-exporting RTL + `userEvent`),
      proven by a harness smoke test (`test-utils.test.tsx`, 2 tests). ESLint override added for
      `src/test/**` + `*.test.*` (react-refresh rule N/A to non-hot-reloaded files). Gates: 8/8
      vitest, `tsc -b` clean, lint at the 189-warning baseline (zero added).
- [x] **Step 1 — CI wiring (2026-07-09)**: added `"test:unit": "vitest run"` script; inserted
      `bun run test:unit` between Typecheck and Build in the `frontend` job of
      `.github/workflows/ci.yml` (job renamed "Frontend typecheck + unit tests + build"). (Runner
      Node is current LTS, so the jsdom-26/Node-21 local constraint doesn't bite in CI; keep
      jsdom 26 pinned regardless.) Also closes the TRACK-118 plan item "Wire `vitest run` into CI".
- [x] **Step 2 — pure-unit wins (2026-07-09)**: extracted BulkImport's presentation maps +
      formatters (verbatim move) to `src/utils/bulk-import-format.ts` with 13 tests
      (`formatDuration`/`formatDate`/`basename`/`parseError` edge cases, status-map completeness);
      `useBatchActions` covered by 8 renderHook tests via `vi.mock` of the API client + Toast
      (per-action client dispatch + toast, in-flight `actionLoading`, confirm-declined no-op,
      finalize error branch, export blob download, failure path skips refresh). Suite now 29 tests
      across 4 files. Follow-up for Step 4: three more private `formatDuration` variants exist in
      `TimelineCard.tsx`, `ExtractionMonitorPage.tsx`, `SourcesAndProcessingPage.tsx` — consolidate
      during decomposition.
- [x] **Step 3 — CuratorReviewPage coverage (2026-07-09)**: extracted `StatCard`/`TabButton`/
      `FormField`/`SectionIssuesTab` to `src/components/curator-review/` (verbatim, barrel export;
      page 688 → ~600 lines). `FormField` gained `useId`-based label association (a11y fix, also
      makes fields queryable by label); same for the page's Lyrics Preview textarea. 5 unit tests
      for `SectionIssuesTab` (empty state, rows, pagination boundaries). 10 page-level tests with
      `vi.mock` of the client + `useSourcingQueries`: queue render + auto-select into form,
      empty-queue state, status-filter page-0 refetch, approve modal → APPROVED payload with
      overrides (incl. edited-field case), reject requires notes → REJECTED payload, approve
      failure toast, keyboard shortcut `a`, bulk select-all → per-id reviewImport, Section Issues
      tab lazy query + render. Also fixed the `useBatchActions` export test clicking a real anchor
      (jsdom navigation warning) by spying on `HTMLAnchorElement.prototype.click`.
- [x] **Step 4 — BulkImport coverage (2026-07-09)**: extracted `BatchList`/`TaskLogDrawer`/
      `UploadPanel` to `src/components/bulk-import/` (page 785 → ~460 lines); `UploadPanel` gained
      label/input association (a11y, mirrors the FormField fix). Removed dead code: `executeDelete`
      + its stray design-notes comment blob, `deleteConfirmBatchId`, and the never-true
      `loadingDetail` branches. Consolidated the two byte-identical private `formatDuration` copies
      (`ExtractionMonitorPage`, `SourcesAndProcessingPage`) onto the shared util; `TimelineCard`'s
      variant kept intentionally (richer m/h tiers). 4 `TaskLogDrawer` unit tests (overview,
      error panel, clipboard copy, close) + 7 page tests (list render + auto-select + detail load,
      empty state, task-status filter refetch, task drawer open with error detail, upload happy
      path + created-batch selection, upload failure toast, Approve All dispatch + list refresh).
      Suite: 55 tests / 8 files; lint 186 warnings (−2 from dead code).

Guardrails: extract-then-test each unit so page tests stay thin integration tests; new code adds
zero lint warnings (189 tolerated pre-existing); keep Vitest collection scoped to `src/**`.

## Acceptance Criteria

- The two critical pages have meaningful component coverage. ✅ 10 CuratorReviewPage + 7 BulkImport
  page tests over the review/upload/action workflows, plus unit tests on their extracted pieces.
- Runs in CI within the frontend time budget. ✅ `bun run test:unit` is a blocking step in the
  frontend job; full suite (55 tests) runs in ~1.5s.

## Close-out (2026-07-09)

All four execution-plan steps delivered (infra, CI wiring, pure units, both pages + decomposition).
Suite grew 0 → 55 tests / 8 files; `vitest run` gates every PR. Frontend lint went 189 → 186
warnings via dead-code removal along the way. Follow-on candidates (out of scope, not blocking):
component coverage for the remaining sourcing pages, and E2E revival under TRACK-113.

## References

- [North-Star Evaluation N3](../../application_documentation/north-star-evaluation.md)
- [Implementation Plan — TRACK-118](../../application_documentation/north-star-production-readiness-implementation-plan.md)
