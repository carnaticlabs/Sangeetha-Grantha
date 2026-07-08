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
| **Last updated** | 2026-07-08 (initial — plan only) |

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
- [ ] **Step 0 — Plan + handover committed** (this commit)
- [ ] **Step 1 — TypeScript 6.0** — bump `typescript`; `tsc -b` green; fix new deprecations/strictness.
- [ ] **Step 2 — ESLint 10 + typescript-eslint 8.63** — bump `eslint`, `@eslint/js`,
      `typescript-eslint`; migrate flat config if needed; `bun run lint` = 0 errors.
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
Nothing upgraded yet. Start at **Step 1 (TypeScript 6)**.

## Log
- 2026-07-08: Branch created; Vitest 5 confirmed beta → deferred; plan + handover written.
