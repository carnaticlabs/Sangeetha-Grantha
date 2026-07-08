| Metadata | Value |
|:---|:---|
| **Status** | In Progress (precondition met; on branch) |
| **Version** | 1.2.0 |
| **Last Updated** | 2026-07-08 |
| **Author** | Sangeetha Grantha Team |

# Precondition (must land first)
**PI-003 / TRACK-118 must be completed before starting this track** so there is a **green Vitest + lint baseline** to upgrade against.

Today the frontend baseline is red: there are no Vitest unit tests in `src/`, so `vitest run` defaults to collecting the Playwright `e2e/*.spec.ts` files and fails, and `bun run lint` reports pre-existing errors (e.g. the triple-slash reference in `vitest.config.ts`). Upgrading **ESLint 9→10** and **Vitest 4→5** on top of an already-broken baseline makes new breakage indistinguishable from existing failures, defeating the verification gates in the plan below.

- See `application_documentation/pending_implementation.md` → **PI-003**.
- See `conductor/tracks/TRACK-118-frontend-component-tests.md` (establishes the Vitest baseline + `vitest.config.ts` `include`/`exclude` so it ignores `e2e/`, and clears the standing lint errors).

Do not begin this track until `bun run test` and `bun run lint` are clean on the current toolchain.

# Goal
Batch 2 — Frontend major toolchain wave (June 2026). Vite, Vitest, TypeScript and ESLint are interlocked and should move together in one isolated change scoped to `modules/frontend/sangita-admin-web`.

# Scope (current → latest)
- **Vite** `7.3.1` → `8.x` (major) — ships **Rolldown** (Rust bundler) as default; plugin-compatible but a real bundler swap.
- **TypeScript** `~5.9` → `6.0` (stable) — transitional cleanup release with deprecations. **Do not target 7.0 yet** (Go rewrite, only RC/beta as of 2026-07).
- **ESLint** `9.39.2` → `10.x` (major) — bump `@eslint/js` + `typescript-eslint` (`^8.63`, supports ESLint 10 + TS 6) + `eslint-plugin-react-hooks` / `react-refresh` in lockstep.
- **@vitejs/plugin-react** `5.0.0` → `6.x` — v6 is the Vite-8 release (Oxc refresh, Babel no longer a dep).
- **Vitest** `4.1.9` → `4.1.10` (patch only). **Vitest 5 DEFERRED (2026-07-08):** still beta
  (`5.0.0-beta.6`); stable is 4.1.x, which already supports Vite 8. Revisit when Vitest 5 goes stable.

> **Execution:** in progress on branch `track-121-frontend-toolchain`; running state and resume
> instructions live in `handover.md` at the repo root (updated after every sub-step).

# Implementation Plan
1. Upgrade Vite + `@vitejs/plugin-react`; verify dev server (port 5001) and `bun run build` (Rolldown) produce a working prod bundle.
2. Upgrade Vitest to 5.x; run `bun run test` and fix any runner/config breakage.
3. Upgrade TypeScript to 6.0; resolve new deprecation/strictness errors; confirm `tsc -b` is green (CI typecheck gate, see commit f14fd67).
4. Upgrade ESLint to 10 + `typescript-eslint`; migrate flat config as needed; `bun run lint` clean.
5. Re-run Playwright E2E to confirm no runtime regressions from the bundler swap.
6. Sync version docs (`current-versions.md`, `tech-stack.md`, `getting-started.md`).
7. Commit per commit-policy.

# Risks
- Rolldown bundler swap is the biggest unknown — exercise the production build, not just dev/HMR.
- TypeScript 6.0 may surface new errors across `src/`; budget iteration time.
- Self-contained to the frontend module — no backend/worker blast radius.
