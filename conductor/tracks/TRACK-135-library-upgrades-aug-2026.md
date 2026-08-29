| Metadata | Value |
|:---|:---|
| **Status** | Done |
| **Version** | 1.1.0 |
| **Last Updated** | 2026-08-29 |
| **Author** | Sangeetha Grantha Team |

# Track: Library Upgrades — August 2026
**ID:** TRACK-135
**Status:** Done
**Owner:** Sangeetha Grantha Team
**Created:** 2026-08-29
**Updated:** 2026-08-29

## Goal

Refresh Kotlin, React, and Python worker dependencies to the current stable lines (29 Aug 2026). Apply security patches first, then isolated batches through AGP 9.3 / Gradle 9.7. Defer TypeScript 7, Vitest 5, Flyway 13, Logback 1.6, and kotlinx-datetime 0.8.

## Context

- **Reference:** [Current Versions](../../application_documentation/00-meta/current-versions.md)
- **Prior waves:** [TRACK-018](./TRACK-018-q1-2026-library-upgrades.md) (Jan), [TRACK-120](./TRACK-120-dependency-upgrades-safe-jun-2026.md)–[TRACK-124](./TRACK-124-google-genai-2.x-upgrade.md) (Jun–Jul)
- **Method:** same isolated-batch strategy as TRACK-018 / TRACK-120

## Intent
**Status:** Accepted
**Accepted by:** User (asked to create the track and start Batch 1–2c)
**Accepted at:** 2026-08-29

### Problem
Pins last moved in June–July 2026. PostgreSQL JDBC `42.7.11` is in the CVE-2026-54291 window. Ktor, Exposed, CMP, AGP, Flyway 12.x, React toolchain, and the Python lock have published stables behind what we ship. `current-versions.md` Python rows are also stale versus `uv.lock`.

### Proposed outcome
Security patch plus Batches 1–2c land: drop-in minors, Exposed 1.x, Kotlin 2.4.10 + CMP 1.12, AGP 9.3.2 + Gradle 9.7.1. Docs match source files. Deferred majors stay listed on this track.

### Affected users and systems
Backend (`modules/backend`), shared KMP (`modules/shared`), admin web (`modules/frontend/sangita-admin-web`), extraction worker (`tools/krithi-extract-enrich-worker`), Flyway image pins (`compose.yaml`, CI, worker integration tests).

### Constraints
- Flyway remains the only migration engine (ADR-013). Stay on **12.x** this track (`12.11.0`); do not jump to 13.
- `DatabaseFactory.dbQuery`, DTO boundary, `AUDIT_LOG` unchanged.
- Do not bump TypeScript 7 (no programmatic API until 7.1 — typescript-eslint cannot follow).
- Vitest 5 still RC — stay on 4.1.x.
- kotlinx-datetime stays `0.7.1` (TRACK-018 already reverted a datetime bump).
- material3 stays on its own `1.9.0` train; icons-extended frozen `1.7.3`.
- Frontend `@google/genai` 1.x → 2.x is **not** this track (Python worker already on 2.x via TRACK-124).

### Open questions
None for Batches 1–2c. Deferred items stay on this track for a later session.

## Spec
**Status:** Accepted
**Accepted by:** User (asked to start Batch 1–2c)
**Accepted at:** 2026-08-29

### Requirements
1. PostgreSQL JDBC `42.7.11` → `42.7.13` (CVE-2026-54291).
2. Batch 1 drop-ins: Ktor 3.5.2, coroutines 1.11.0, serialization 1.11.0, Koin 4.2.2, HikariCP 7.1.0, Logback 1.5.38, JWT 4.6.0, MockK 1.14.11, Jsoup 1.23.2, Caffeine 3.2.4, password4j 1.8.4, Shadow 9.6.1, Flyway **12.11.0** (catalog + `compose.yaml` + CI + worker `FLYWAY_IMAGE`), Micrometer 1.17.1, Google Auth 1.51.0, AWS SDK 2.54.7.
3. Frontend Batch 1: React 19.2.8, React Router 7.18.3, TanStack Query 5.102.8, Tailwind 4.3.3, Vite 8.2.2, plugin-react 6.1.1, Vitest 4.1.11, ESLint 10.9.1, typescript-eslint 8.68.0, Playwright 1.62.1, plus the listed patch/minor devDeps. TypeScript stays `~6.0.0`.
4. Python: `uv lock --upgrade` of named packages (PyMuPDF, pydantic, pydantic-settings, google-genai, click, ruff, mypy). Floors in `pyproject.toml` stay ranges; lock is source of truth.
5. Batch 2a: Exposed `1.0.0` → `1.5.0` (Maven latest; if 1.5.0 is unusable, land `1.4.0` and record why).
6. Batch 2b: Kotlin `2.4.0` → `2.4.10`, CMP `1.11.1` → `1.12.0`; AndroidX activity-compose `1.13.0`, core-ktx `1.19.0`, material `1.14.0`. material3 remains `1.9.0`.
7. Batch 2c: AGP `9.0.0` → `9.3.2`, Gradle wrapper `9.1.0` → `9.7.1`.
8. Sync [current-versions.md](../../application_documentation/00-meta/current-versions.md), [tech-stack.md](../../application_documentation/02-architecture/tech-stack.md), [migrations.md](../../application_documentation/04-database/migrations.md), `README.md`.

### Design
All JVM versions live in `gradle/libs.versions.toml`. Flyway Community image tags must match the catalog (`compose.yaml`, `.github/workflows/ci.yml`, `tools/krithi-extract-enrich-worker/tests/integration/conftest.py`). Frontend caret ranges in `package.json` then `bun install`. Worker lock via `uv lock --upgrade-package`.

### Flagged concerns
- **Flyway:** 12.11.0 only. Major 13 (`initSQL` removed, callback rename) is deferred.
- **Exposed 1.0→1.5:** five minors since TRACK-016. Compile + `make test` + `make test-integration` are the gate; no schema/SQL changes expected.
- **CMP 1.12:** language/API 2.2 minimum — we are on 2.4. Watch for another accessor/target fallout like TRACK-122.
- **AGP 9.3 + Gradle 9.7:** build-system risk; verify `:modules:shared:presentation:assemble` and backend.

### Open questions carried forward
None for this slice.

## Plan
**Status:** Accepted
**Accepted by:** User (asked to start Batch 1–2c)
**Accepted at:** 2026-08-29

### Files that change
- `gradle/libs.versions.toml`, `gradle/wrapper/gradle-wrapper.properties`
- `modules/frontend/sangita-admin-web/package.json`, `bun.lock`
- `tools/krithi-extract-enrich-worker/uv.lock` (and `pyproject.toml` only if a floor must rise)
- `compose.yaml`, `.github/workflows/ci.yml`, `tools/krithi-extract-enrich-worker/tests/integration/conftest.py`
- Version docs: `application_documentation/00-meta/current-versions.md`, `02-architecture/tech-stack.md`, `04-database/migrations.md`, `README.md`
- This track + `conductor/tracks.md`

### Order of work
1. Create this track and branch `track-135-library-upgrades-aug-2026`.
2. Batch 1 version pins (including JDBC 42.7.13 and Flyway 12.11.0) + frontend `bun install` + worker `uv lock`.
3. Batch 2a Exposed 1.5.0; fix compile if APIs moved.
4. Batch 2b Kotlin 2.4.10 + CMP 1.12.0 + AndroidX.
5. Batch 2c AGP 9.3.2 + Gradle 9.7.1 wrapper.
6. Sync version docs.
7. Proof commands below.

### Risks
- Exposed 1.5 may require import or UUID API tweaks (TRACK-016 pattern).
- CMP 1.12 may deprecate more plugin accessors or drop a target.
- AGP 9.3 may require Gradle 9.3+ settings we do not have yet — wrapper bump is in the same batch.
- google-genai 2.14→2.20 is still the generate_content surface; re-run worker tests.

### Proof
- `./gradlew :modules:backend:api:build :modules:shared:domain:assemble :modules:shared:presentation:assemble`
- `make test`
- `make test-integration` (if Docker is available)
- `make test-frontend`
- Worker: `cd tools/krithi-extract-enrich-worker && uv run pytest` (or the Makefile target if present)
- `make check-docs` after version-doc edits

## Implementation Plan
- [x] Create TRACK-135 and registry row
- [x] Batch 1: catalog + frontend + Python lock + Flyway 12.11 pins
- [x] Batch 2a: Exposed 1.5.0 (no API churn)
- [x] Batch 2b: Kotlin 2.4.10 + CMP 1.12.0 + AndroidX (`compileSdk` 36 → 37; CMP 1.12 AARs require it)
- [x] Batch 2c: AGP 9.3.2 + Gradle 9.7.1
- [x] Sync version docs
- [x] Verify compile/tests (Kotlin + frontend green; worker unit + integration 18 passed with Flyway 12.11.0-alpine)

## Deferred (not this session)
- TypeScript 7.0.2 (wait for 7.1 API)
- Vitest 5 (still RC)
- Flyway 13.4.0
- Logback 1.6.3 + logstash-encoder 9.0
- kotlinx-datetime 0.8.0
- Kotlin 2.4.20 (RC)
- CMP material3 1.12.0-alpha03
- Frontend `@google/genai` 1.34.0 → 2.19.0
- jsdom 26 → 30, `@testing-library/jest-dom` 6 → 7, `@types/node` 22 → 26
- `eslint-plugin-react-hooks` 7.1.1 (`react-hooks/immutability` is a large UI refactor; pinned at **7.0.1**)

## Progress Log
- **2026-08-29**: Track created. Intent/Spec/Plan accepted via implementation request. Starting Batches 1–2c.
- **2026-08-29**: Worker integration: pulled `flyway/flyway:12.11.0-alpine`; `uv run pytest tests/integration` **18 passed**. conftest now `_ensure_image` before migrate so a cold Hub pull cannot eat the 600s `docker run` timeout.
- **2026-08-29**: Full pre-commit validation green — Gradle build (backend + shared, Gradle 9.7.1/AGP 9.3.2), backend integration (dal + api), frontend 56/56 (Vitest 4.1.11), worker unit 294 + integration 18, check-docs, registry sync. Exposed 1.5 and CMP 1.12 landed with no API/accessor churn. Batches 1–2c ready to commit.
- **2026-08-29**: PR #12 merged to `main` (`01305cc`); all 8 CI checks green (backend unit + integration, Flyway validate, frontend, worker). Status → Done.
