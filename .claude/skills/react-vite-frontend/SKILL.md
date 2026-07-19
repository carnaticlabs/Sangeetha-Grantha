---
name: react-vite-frontend
description: React admin web conventions (React 19, TypeScript, Vite, Tailwind 4, Bun). Use when editing modules/frontend/sangita-admin-web — components, pages, hooks, API clients, routing, styling, or frontend tests — or running frontend tooling.
---

# React Admin Web (modules/frontend/sangita-admin-web/)

Versions live in [current-versions.md](../../../application_documentation/00-meta/current-versions.md); module-level notes in this directory's own `CLAUDE.md`.

## Tooling is Bun-only

This is a Bun project — never npm/yarn/pnpm/node (lockfile conflicts, and the box's Node is EOL; `bunfig.toml` sets `[run] bun = true` so scripts execute under Bun).

- `bun install` · `bun run dev` (port 5001) · `bun run build` · `bun run typecheck` · `bun run lint`
- Tests: `bun run test:unit` (Vitest run) or `bun run test` (watch). **Not `bun test`** — that invokes Bun's own runner, not Vitest.
- E2E: `bun run test:e2e` (Playwright, config in `e2e/`); money-path subset via `test:e2e:money` (runs nightly in CI).

## TypeScript standard

Strict typing throughout: no `any` (use `unknown` + narrowing at boundaries), explicit interfaces for props, state, and API contracts, utility types (`Pick`, `Omit`, `Partial`) to derive rather than duplicate shapes. Function components only, with correctly-scoped hook dependency arrays.

## Architecture

- `src/pages/` route screens, `src/components/` shared UI, `src/hooks/` reusable logic, `src/api/` the API layer.
- Routing via `react-router-dom` (v7) with explicitly declared routes and typed params.
- Server state via TanStack Query — queries/mutations in the api/hooks layer, not ad-hoc `fetch` in components; auth headers (JWT) handled centrally in the API client.
- Styling via Tailwind 4 utilities (PostCSS plugin `@tailwindcss/postcss`). Follow the existing look: tailored slate/indigo/emerald/amber palettes, subtle transitions — no raw default reds/blues.

## Debugging

For CORS/auth issues check `.env` files and `VITE_API_BASE_URL` first — frontend proxy configuration is the most common root cause (per CLAUDE.md).
