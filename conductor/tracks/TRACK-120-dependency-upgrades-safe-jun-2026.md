| Metadata | Value |
|:---|:---|
| **Status** | In Progress |
| **Version** | 1.1.0 |
| **Last Updated** | 2026-06-24 |
| **Author** | Sangeetha Grantha Team |

# Goal
Batch 1 — Security patch + low-risk drop-in dependency upgrades (June 2026). These are patch/minor bumps with no expected API breakage. Verify compile/build/test across all three layers before committing.

# Scope (current → latest)

## Security (non-optional)
- **PostgreSQL JDBC** `42.7.10` → `42.7.11` — fixes **CVE-2026-42198** (SCRAM PBKDF2 iteration DoS). Drop-in.

## Backend (`gradle/libs.versions.toml`)
- Ktor `3.4.0` → `3.5.0` (minor — digest auth, session cookies)
- Koin `4.1.1` → `4.2.1` (minor — Ktor 3.4 DI bridge)
- Flyway `12.8.1` → `12.9.0` (minor — also sync `compose.yaml` image tag)
- Logback `1.5.32` → `1.5.34` (patch)

## Frontend (`modules/frontend/sangita-admin-web/package.json`)
- React / react-dom `19.2.4` → `19.2.7` / `19.2.6` (patch)
- TanStack Query `5.90.21` → `5.101.1` (minor)
- Tailwind CSS `4.2.1` → `4.3.1` (minor)
- React Router `7.13.1` → latest 7.x (stay on 7 line)
- Vitest `4.0.18` → `4.1.9` (minor — adds Vite 8 support)
- **Playwright** `1.40.0` → `1.61.0` (21 minors behind — run full E2E suite after)

## Python worker (`tools/krithi-extract-enrich-worker/pyproject.toml` + `uv.lock`)
- pydantic `2.12.5` → `2.13.4` (minor)
- psycopg `3.3.2` → `3.3.4` (patch)
- PyMuPDF `1.27.1` → `1.27.2.3` (patch)

# Implementation Plan
1. Bump backend versions in `gradle/libs.versions.toml`; sync Flyway tag in `compose.yaml`. Run `./gradlew :modules:backend:api:build` + `make test`.
2. Bump frontend versions in `package.json`; `bun install`; `bun run build` + `bun run test`; run Playwright E2E suite.
3. Bump Python worker floors in `pyproject.toml`; `uv lock`; run worker pytest.
4. Sync version docs: `current-versions.md`, `tech-stack.md`, `getting-started.md` (per CLAUDE.md Critical Rule 5).
5. Commit per commit-policy (include `Ref:` line + this track).

# Notes
- Exposed `1.0.0` and MockK `1.14.9` are already current — no action.
- Lowest-risk batch; safe to complete in a single session.
