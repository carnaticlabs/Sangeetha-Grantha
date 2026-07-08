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
| **Last updated** | 2026-07-08 (Step 2 ESLint 10 done) |

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
- [ ] **Step 3 — Vite 8 + @vitejs/plugin-react 6 (+ Vitest 4.1.10)** — bump; `bun run build`
      (Rolldown) produces a working prod bundle; dev server (5001) boots; `bunx vitest run` green.
- [ ] **Step 4 — Docs sync + finalize** — `current-versions.md`, tech-stack, getting-started;
      flip TRACK-121 status; open PR / merge to `main`.

## Verification commands (run from `modules/frontend/sangita-admin-web`)
```bash
bun install
bunx tsc -b            # typecheck (CI gate)
bun run build         # prod build (Rolldown once on Vite 8)
bunx vitest run       # unit tests (add --bun if Node-shebang ESM issues)
bun run lint          # must stay 0 errors
```

## Current state / where to resume
TS 6 + ESLint 10 done + committed; `bunfig.toml` makes `bun run` use Bun's runtime.
**Resume at Step 3 (Vite 8 + @vitejs/plugin-react 6 + Vitest 4.1.10).** This is the riskiest step —
Vite 8 swaps in the Rolldown bundler. Bump `vite ^7.3.1→^8.x`, `@vitejs/plugin-react ^5.0.0→^6.x`,
`vitest ^4.1.9→^4.1.10`; `bun install`; then verify **in this order**:
`bunx tsc -b` → `bun run build` (Rolldown prod bundle must succeed) → `bunx vitest run` (jsdom;
Vitest 4.1 uses the installed Vite 8) → `bun run lint`. Boot `bun run dev` (port 5001) to confirm the
dev server + HMR. Watch for Rolldown plugin-compat and any `vite.config.ts` API changes.
**CI note:** confirm `.github/workflows/ci.yml` frontend job still passes — it runs under setup-bun, so
`bunfig.toml` applies; `bunx tsc -b` + `bun run build` should be unaffected.

## Log
- 2026-07-08: Branch created; Vitest 5 confirmed beta → deferred; plan + handover written (`1741ba5`).
- 2026-07-08: Step 1 TypeScript 6.0.3 — 3 strict-null fixes; tsc/build/vitest/lint all green.
- 2026-07-08: Step 2 ESLint 10.6.0 stack — 2 e2e rule fixes + `bunfig.toml` (Bun runtime for
  `bun run`, needed because ESLint 10 wants `util.styleText` absent in EOL Node 21). All green.
