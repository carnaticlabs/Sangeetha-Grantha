| Metadata | Value |
|:---|:---|
| **Status** | In Progress |
| **Version** | 1.1.0 |
| **Last Updated** | 2026-09-10 |
| **Author** | Sangeetha Grantha Team |

# Track: Library Upgrades — September 2026
**ID:** TRACK-142
**Status:** In Progress
**Owner:** Sangeetha Grantha Team
**Created:** 2026-09-10
**Updated:** 2026-09-10

## Goal

Refresh Kotlin, React, and Python worker dependencies against Maven Central, npm, and PyPI as of 10 Sep 2026. Apply TRACK-018 isolated batches: drop-in patches, then GA majors (Flyway 13, Logback 1.6, datetime 0.8, Vitest 5, jsdom/jest-dom, JS genai 2.x). Leave only RCs, alphas, and TypeScript 7 (blocked on 7.1 programmatic API).

## Context

- **Reference:** [Current Versions](../../application_documentation/00-meta/current-versions.md)
- **Prior waves:** [TRACK-018](./TRACK-018-q1-2026-library-upgrades.md) (Jan), [TRACK-120](./TRACK-120-dependency-upgrades-safe-jun-2026.md)–[TRACK-124](./TRACK-124-google-genai-2.x-upgrade.md) (Jun–Jul), [TRACK-135](./TRACK-135-library-upgrades-aug-2026.md) (29 Aug)
- **Method:** same isolated-batch strategy as TRACK-018 / TRACK-135
- **Audit date:** 2026-09-10 (Maven `maven-metadata.xml`, npm dist-tags, PyPI JSON, Google Maven, Gradle current)

## Intent
**Status:** Accepted
**Accepted by:** User (asked to run a 10 Sep refresh with TRACK-142, then to factor in the major upgrades)
**Accepted at:** 2026-09-10

### Problem
Pins last moved in TRACK-135 (29 Aug). Twelve days later several stables have shipped: Kotlin **2.4.20** (GA 7 Sep), React **19.3.0**, AGP **9.4.0**, Vitest **5.0.0** (now GA), Flyway **13.5.0**, Logback **1.6.3**, kotlinx-datetime **0.8.0**, plus JVM/frontend/Python patch lines. PostgreSQL JDBC is already on `42.7.13` (CVE-2026-54291 patched).

### Proposed outcome
Batches 1–3 land: drop-ins, Kotlin 2.4.20 / React 19.3 / AGP 9.4, then GA majors. Docs match source files. Only RCs/alphas and TypeScript 7 stay deferred.

### Affected users and systems
Backend (`modules/backend`), shared KMP (`modules/shared`), admin web (`modules/frontend/sangita-admin-web`), extraction worker (`tools/krithi-extract-enrich-worker`), Flyway image pins (`compose.yaml`, CI, worker integration tests), Bun/mise/CI toolchain pins.

### Constraints
- Flyway remains the only migration engine (ADR-013). Jump **12.11.0 → 13.5.0** in this track (no `initSQL` / `createSchema` callbacks in-repo; JVM is Java 25).
- `DatabaseFactory.dbQuery`, DTO boundary, `AUDIT_LOG` unchanged.
- TypeScript stays `~6.0.0` (7.1 is still `next`/dev; typescript-eslint cannot follow 7.0 programmatic API).
- material3 stays on `1.9.0`; icons-extended frozen `1.7.3`; CMP 1.13 is alpha — skip.
- kotlinx-serialization `1.12.0-RC` and Gradle `9.8.0-rc-1` — skip.

### Open questions
None. User asked to include the GA majors in this refresh.

## Spec
**Status:** Accepted
**Accepted by:** User (asked to run the refresh, then include majors)
**Accepted at:** 2026-09-10

### Requirements
1. No JDBC bump (already `42.7.13`, latest).
2. Batch 1 drop-ins:
   - JVM: JWT `4.6.1`, AWS SDK `2.54.15`, Google Auth `1.52.0`.
   - Frontend: ESLint `10.10.0`, typescript-eslint `8.70.0`, Playwright `1.63.0`, user-event `14.6.7`, eslint-plugin-react-refresh `0.5.6`, globals `17.12.0`, autoprefixer `10.5.5`, postcss `8.5.28`, `@types/react`/`@types/react-dom` `19.3.0`, `pg`/`@types/pg` 8.23.x.
   - Python lock: psycopg `3.3.5`, google-genai `2.22.0`, RapidFuzz `3.14.6`, ruff `0.16.6`.
   - Toolchain: Bun `1.4.0` → `1.4.2` (mise + CI).
3. Batch 2a: Kotlin `2.4.10` → `2.4.20` (GA 7 Sep; Gradle 9.7.1 already supported).
4. Batch 2b: React / react-dom `19.2.8` → `19.3.0`.
5. Batch 2c: AGP `9.3.2` → `9.4.0` (requires Gradle ≥9.6.0; wrapper stays `9.7.1`). compileSdk 37 already set.
6. Batch 3a: kotlinx-datetime `0.7.1` → `0.8.0`; Logback `1.5.38` → `1.6.3` + logstash-encoder `9.0`.
7. Batch 3b: Flyway `12.11.0` → `13.5.0` (catalog + `compose.yaml` + CI + worker `FLYWAY_IMAGE`).
8. Batch 3c: Vitest `4.1.11` → `5.0.0`, jsdom `26.1.0` → `30.0.1`, `@testing-library/jest-dom` `6.9.1` → `7.0.1`.
9. Batch 3d: `@types/node` `22.14.0` → `26.5.1` (`ts6.0` tag). **Do not ship frontend `@google/genai`.** The admin web has no TS import of the SDK; LLM calls go through the Kotlin API (`GeminiApiClient` / transliterate) and the Python worker. Remove the unused package, the importmap entry, and the Vite `define` that would bake `GEMINI_API_KEY` into the browser bundle. `eslint-plugin-react-hooks` stays **7.0.1** (7.1.1 `immutability` still errors on BulkImport/CuratorReview — TRACK-135 loading-loop).
10. Sync [current-versions.md](../../application_documentation/00-meta/current-versions.md).

### Design
All JVM versions live in `gradle/libs.versions.toml`. Flyway Community image tags must match the catalog. Frontend caret ranges in `package.json` then `bun install`. Worker lock via `uv lock --upgrade-package`. Bun pin in `.mise.toml` plus CI `oven-sh/setup-bun` and fallback installer.

### Flagged concerns
- **Kotlin 2.4.20:** Wasm companion-init and `@JsFun` `require()` compile error do not apply (no JS/Wasm targets).
- **AGP 9.4:** max API 37 — already on compileSdk 37.
- **datetime 0.8:** Instant already lives in `kotlin.time`; no `import kotlinx.datetime` in-tree. 0.8 deprecates `TimeZone` serialization only.
- **Flyway 13:** `initSQL` removed / `createSchema` → `beforeCreateSchema`. Neither is used. Java 21 floor; we are on 25. Image `flyway/flyway:13.5.0-alpine`.
- **Logback 1.6:** drop-in from 1.5 except Janino conditionals (config is programmatic, no Janino). Encoder 9.0 pulls Jackson 3 — no Jackson in this repo today.
- **Vitest 5:** removed deprecated entry points; `clearMocks` defaults true. Config uses `vitest/config` only. Requires Node ≥22.12 / Vite ≥6.4; scripts run under Bun.
- **google-genai JS:** unused. Admin web has no `from '@google/genai'` call site. Transliteration already hits `POST /admin/krithis/{id}/transliterate`. Extraction/enrichment is the Python worker. Frontend Vite previously `define`d `process.env.GEMINI_API_KEY` from `config/` — that would embed the key in the client bundle. Remove the SDK rather than bump it.
- **eslint-plugin-react-hooks 7.1.1:** TRACK-135 reverted this after a CuratorReviewPage loading loop. Re-attempt; keep 7.0.1 only if the immutability rule forces a large UI rewrite.

### Open questions carried forward
None for this slice.

## Plan
**Status:** Accepted
**Accepted by:** User (asked to run the refresh and include majors)
**Accepted at:** 2026-09-10

### Files that change
- `gradle/libs.versions.toml`
- `modules/shared/presentation/src/commonMain/kotlin/com/sangita/grantha/shared/presentation/RasikaApp.kt` (BackHandler → NavigationEventHandler)
- `modules/shared/presentation/build.gradle.kts`
- `modules/frontend/sangita-admin-web/package.json`, `bun.lock`, `src/index.html`, possibly Vitest setup / hook-lint fallout
- `tools/krithi-extract-enrich-worker/uv.lock`, `tests/integration/conftest.py`
- `compose.yaml`, `.github/workflows/ci.yml`, `.github/workflows/e2e-nightly.yml`
- `.mise.toml`
- `application_documentation/00-meta/current-versions.md`
- This track + `conductor/tracks.md`
- `.agents/skills/monorepo-orchestration/SKILL.md` (hardcoded Bun pin)

### Order of work
1. Create this track and branch `track-142-library-upgrades-sep-2026`.
2. Batch 1 version pins + frontend `bun install` + worker `uv lock` + Bun 1.4.2.
3. Batch 2a–2c: Kotlin 2.4.20, React 19.3, AGP 9.4.0.
4. Batch 3a–3d: datetime/logback, Flyway 13, Vitest 5 + jsdom/jest-dom, genai 2.x / types/node 26 / react-hooks 7.1.1.
5. Sync version docs.
6. Proof commands below.

### Risks
- Flyway 13 CLI/JVM behavioural drift despite unused `initSQL`.
- Encoder 9.0 / Jackson 3 on the logging classpath.
- Vitest 5 `clearMocks` default flipping mock-using tests.
- react-hooks 7.1.1 `immutability` rule (TRACK-135 regression).
- First pull of `flyway/flyway:13.5.0-alpine` on Docker Hub.

### Proof
- `./gradlew :modules:backend:api:build :modules:shared:domain:assemble :modules:shared:presentation:assemble`
- `make test`
- `make test-frontend`
- Worker: `cd tools/krithi-extract-enrich-worker && uv run pytest`
- `make check-docs` after version-doc edits

## Implementation Plan
- [x] Create TRACK-142 and registry row
- [x] Batch 1: JVM/frontend/Python patches + Bun 1.4.2
- [x] Batch 2a: Kotlin 2.4.20
- [x] Batch 2b: React 19.3.0
- [x] Batch 2c: AGP 9.4.0
- [x] Batch 3a: datetime 0.8 + Logback 1.6.3 + encoder 9.0
- [x] Batch 3b: Flyway 13.5.0
- [x] Batch 3c: Vitest 5 + jsdom 30 + jest-dom 7
- [x] Batch 3d: `@types/node` 26; **remove** unused frontend `@google/genai` (do not bump 1→2)
- [x] Sync version docs
- [x] Migrate Rasika `BackHandler` → `NavigationEventHandler`
- [x] Verify compile/tests

## Deferred (not this session)
- TypeScript 7.0.2 / 7.1-dev (no stable programmatic API for typescript-eslint)
- kotlinx-serialization `1.12.0-RC`
- CMP `1.13.0-alpha01` / material3 alpha
- Gradle `9.8.0-rc-1` (wrapper stays 9.7.1 current)

## Progress Log
- **2026-09-10**: Track created from Maven/npm/PyPI audit. Intent/Spec/Plan accepted via refresh request.
- **2026-09-10**: User asked to factor in GA majors. Spec/Plan expanded to Batches 3a–3d (Flyway 13, Logback 1.6, datetime 0.8, Vitest 5, jsdom/jest-dom, @types/node 26). TypeScript 7 / CMP alpha / serialization RC / Gradle RC remain deferred. `eslint-plugin-react-hooks` stays 7.0.1.
- **2026-09-10**: Validated frontend `@google/genai` is unused; LLM already routes via backend `GeminiApiClient` and the Python worker. Dropped Batch 3d genai bump. Removed the package, importmap, and Vite `define` of `GEMINI_API_KEY` so the key cannot land in the browser bundle.
- **2026-09-10**: Replaced deprecated Compose `BackHandler` in `RasikaApp.kt` with `NavigationEventHandler` (`org.jetbrains.androidx.navigationevent:navigationevent-compose:1.1.0`, CMP 1.12 companion). Proof: presentation JVM/Android/metadata compile (no BackHandler deprecation warning); `make test` 291; `make test-integration` 148; `make test-mobile` 57; Vitest 66; worker unit 386 + integration 22 (Flyway `13.5.0-alpine`); `vite build` green. `make check-docs` still reports TRACK-142 until that file is git-tracked.
- **2026-09-10**: Cleared AGP 9.4 / Gradle 9.6 configuration-warning flood: `androidLibrary` → `android` + `withHostTest {}`; `by getting` → named source-set `dependencies {}`; drop Jetifier; drop domain `iosX64`; `kotlin.native.ignoreDisabledTargets=true`; assets `directories`; KrithiSearchRepository `!!`. Parser-test constructor deprecation and Compose `createEmptyComposeRule` v2 left (behavior-sensitive).
- **2026-09-10**: Retired unused Kotlin lyric-scrape path. Python `structure_parser.py` is canonical. Removed `KrithiStructureParser`, `SectionHeaderDetector`, `HtmlTextExtractor`, `ScrapeJsonSanitizer`, `ScrapeCache`, unused `TempleScrapingService`/`GeocodingService`, and Jsoup/Caffeine catalog entries. Kept `StructuralVotingEngine` (live ingestion).
