# Handover — TRACK-121 Frontend Major Toolchain Upgrade

> **Living document.** Refreshed after every sub-step so it is always current. If work stops
> (quota/context/broken step), this file alone is enough to resume cold.
> (Supersedes the old TRACK-106/107 handover, whose work is committed and complete.)

| | |
|:---|:---|
| **Track** | [TRACK-121](conductor/tracks/TRACK-121-frontend-major-toolchain-upgrade.md) |
| **Branch** | `track-121-frontend-toolchain` (branched from `main` @ `e124b44`) |
| **Scope dir** | `modules/frontend/sangita-admin-web` |
| **Started** | 2026-07-08 |
| **Status** | ✅ COMPLETE — merged to `main` via PR #5 (CI green). This handover is now historical. |
| **Last updated** | 2026-07-08 (merged) |

## Ground rules for this upgrade
- Work **one library at a time**; verify (`tsc -b`, `bun run build`, `bunx vitest run`, `bun run lint`)
  after each; commit each green step on the branch. Never leave `main` half-upgraded.
- Runtime is **Bun**. Vitest's CLI shebang defaults to Node; run under Bun with `bunx --bun vitest run`
  if the system Node (EOL 21) causes ESM issues. Do **not** pin Node. (See PI-003 runtime note.)
- Baseline before this track (on `main` @ `e124b44`): lint 0 errors / 189 warnings, `vitest run`
  6 passed (`src/utils/enums.test.ts`), `tsc -b` clean, `bun run build` clean.

## Revised scope (2026-07-08 — verified current versions)
| Lib | From | To | Notes |
|:---|:---|:---|:---|
| TypeScript | `~5.9.0` | `~6.0.x` | Stable. TS 7 (Go rewrite) is only RC — **do not** take it. |
| ESLint | `^9.39.2` | `^10.x` | `@eslint/js` bumps in lockstep. |
| typescript-eslint | `^8.54.0` | `^8.63.0` | Supports ESLint `^10` and TS 6. |
| Vite | `^7.3.1` | `^8.x` | **Rolldown** bundler swap — the biggest risk; exercise the prod build. |
| @vitejs/plugin-react | `^5.0.0` | `^6.x` | v6 is the Vite-8 release (Oxc refresh, no Babel dep). |
| Vitest | `^4.1.9` | `^4.1.10` | **Vitest 5 DEFERRED — still beta (5.0.0-beta.6).** 4.1.x already supports Vite 8. |
| jsdom | `^26.1.0` | keep | Stays 26 (runtime-agnostic); revisit only if Node moves off EOL 21. |

## Step status
- [x] **Step 0 — Plan + handover committed** (`1741ba5`)
- [x] **Step 1 — TypeScript 6.0** (`typescript@6.0.3`) — all 4 gates green. Fixed 3 new TS6 strict-null
      errors: `AutoApproveQueue.tsx` (`!== null`→`!= null`), `KrithiEditor.tsx` (guard render on
      `state.krithi.musicalForm` to narrow the `MusicalForm | undefined`), `useSourcingQueries.ts`
      (`useVotingDetail` param → `string | undefined`, key `?? ''`, queryFn `id!` — hook already had
      `enabled: !!id`). Committed next.
- [x] **Step 2 — ESLint 10** — `eslint@10.6.0`, `@eslint/js@10.0.1` (note: @eslint/js lags the CLI —
      10.0.1 is its latest), `typescript-eslint@8.63.0`, `eslint-plugin-react-refresh@0.5.3` (our
      `^0.4.26` capped below the ESLint-10-compatible 0.5.x). Fixed 2 new ESLint-10 recommended-rule
      errors in `e2e/global-setup.ts`: `preserve-caught-error` (added `{ cause: error }`),
      `no-useless-assignment` (dropped dead `= null` initializer). `bun run lint` = 0 errors.
      **Runtime:** ESLint 10 needs `util.styleText` (absent in the box's EOL Node 21.4.0), so added
      `modules/frontend/sangita-admin-web/bunfig.toml` with `[run] bun = true` — `bun run` scripts now
      execute under Bun's runtime instead of the node shebang. Verified `bun run lint`/`build` + vitest
      all green under Bun. Direct calls: use `bunx --bun eslint .` / `bunx --bun vitest run`.
- [x] **Step 3 — Vite 8 + @vitejs/plugin-react 6 + Vitest 4.1.10** — `vite@8.1.3` (Rolldown),
      `@vitejs/plugin-react@6.0.3`, `vitest@4.1.10`. Installed clean; no `vite.config.ts` changes needed.
      Gates: `bunx tsc -b` ✓; `bun run build` (Rolldown) ✓ built in ~0.9s; `bun run lint` 0 errors ✓;
      dev server `VITE v8.1.3 ready in 262ms`, HTTP 200 on :5001 ✓.
      **Vitest under Vite 8 needs Bun:** `bunx vitest run` now fails with `styleText` (Vite 8/Vitest
      4.1.10 import it from `node:util`, absent in EOL Node 21). Use **`bunx --bun vitest run`** (6
      passed) or `bun run test` (bunfig → Bun). This is a local-box artifact of EOL Node 21; CI's
      modern Node likely isn't affected, and `bunfig.toml` covers `bun run` either way.
- [x] **Step 4 — Docs sync + finalize** — DONE: `current-versions.md` v1.3.0; TRACK-121 → Completed
      (track file + registry). CI green on PR #5 (frontend typecheck+build, backend unit+integration,
      Flyway, worker); rebase-merged to `main`; branch deleted.

## Verification commands (run from `modules/frontend/sangita-admin-web`)
```bash
bun install
bunx tsc -b            # typecheck (CI gate)
bun run build         # prod build (Rolldown once on Vite 8)
bunx vitest run       # unit tests (add --bun if Node-shebang ESM issues)
bun run lint          # must stay 0 errors
```

## Current state / where to resume
**All three major upgrades (TS 6, ESLint 10, Vite 8) DONE, green, committed on branch; docs synced.**
Only finalization remains (outward actions — left for the user):
1. **Push the branch** and confirm `.github/workflows/ci.yml` frontend job (setup-bun) passes with
   Vite 8 — `bunx tsc -b` + `bun run build`. NB: CI's own Node (modern) likely doesn't hit the
   `styleText` issue; `bunfig.toml` covers `bun run` regardless.
2. **Open a PR** `track-121-frontend-toolchain` → `main`; merge after CI green. Do NOT fast-merge a
   Rolldown bundler swap without CI.
3. On merge: flip TRACK-121 → Completed in `conductor/tracks.md` + the track file; update the registry
   row (currently `Blocked (needs TRACK-118)` on `main`).
4. Optional next: wire `bunx --bun vitest run` into CI (that's a TRACK-118 item), and revisit Vitest 5
   once it ships stable.

## Local verification snapshot (branch @ latest commit)
| Gate | Command | Result |
|:---|:---|:---|
| Typecheck | `bunx tsc -b` | ✓ exit 0 |
| Build (Rolldown) | `bun run build` | ✓ ~0.9s |
| Unit tests | `bunx --bun vitest run` | ✓ 6 passed |
| Lint | `bun run lint` | ✓ 0 errors / 189 warnings |
| Dev server | `bun run dev` | ✓ VITE 8.1.3, HTTP 200 :5001 |

## Log
- 2026-07-08: Branch created; Vitest 5 confirmed beta → deferred; plan + handover written (`1741ba5`).
- 2026-07-08: Step 1 TypeScript 6.0.3 — 3 strict-null fixes; tsc/build/vitest/lint all green.
- 2026-07-08: Step 2 ESLint 10.6.0 stack — 2 e2e rule fixes + `bunfig.toml` (Bun runtime for
  `bun run`, needed because ESLint 10 wants `util.styleText` absent in EOL Node 21). All green.
- 2026-07-08: Step 3 Vite 8.1.3 (Rolldown) + plugin-react 6.0.3 + vitest 4.1.10 — no config changes;
  build/tsc/lint/dev green; vitest green under `--bun`. Only Step 4 (docs + finalize) remains.
