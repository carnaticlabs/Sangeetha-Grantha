| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.5.0 |
| **Last Updated** | 2026-08-29 |
| **Author** | Sangeetha Grantha Team |

# Current Technology Versions

> **Single source of truth** for all dependency versions across the project.
>
> All documentation should reference this file instead of hardcoding version numbers.
>
> When updating dependencies, update the source file first, then sync this document.

---

## Development Toolchain

*Source: `.mise.toml`*

| Tool | Version | Notes |
|------|---------|-------|
| Java | `temurin-25` | Temurin distribution, JVM toolchain |
| Bun | `1.4.0` | Frontend package manager & runtime |
| Python | `3.11+` | Migration tool & extraction worker (runtime: 3.14) |
| Docker Compose | `latest` | Container orchestration |

---

## Backend Stack (Kotlin/JVM)

*Source: `gradle/libs.versions.toml`*

### Core Framework

| Library | Version | Purpose |
|---------|---------|---------|
| Kotlin | `2.4.10` | Language version |
| Ktor | `3.5.2` | HTTP server & client framework |
| Exposed | `1.5.0` | SQL ORM (DSL-based) |
| Koin | `4.2.2` | Dependency injection |

### Kotlinx Libraries

| Library | Version | Purpose |
|---------|---------|---------|
| Coroutines | `1.11.0` | Async programming |
| DateTime | `0.7.1` | Cross-platform date/time (0.8 deferred — TRACK-018 history) |
| Serialization JSON | `1.11.0` | JSON serialization |

### Database & Infrastructure

| Library | Version | Purpose |
|---------|---------|---------|
| PostgreSQL Driver | `42.7.13` | JDBC driver (CVE-2026-54291 fix) |
| HikariCP | `7.1.0` | Connection pooling |
| Logback | `1.5.38` | Logging framework (1.6 deferred) |
| Logstash Encoder | `8.0` | JSON log formatting |
| Commons CSV | `1.14.1` | CSV parsing |
| Jsoup | `1.23.2` | HTML parsing |
| Caffeine | `3.2.4` | In-memory caching |
| dotenv-kotlin | `6.5.1` | Environment variable loading |

### Security & Auth

| Library | Version | Purpose |
|---------|---------|---------|
| JWT (Auth0) | `4.6.0` | JWT token handling |
| Google Auth | `1.51.0` | OAuth2 (future SSO) |
| password4j | `1.8.4` | argon2id password hashing (TRACK-114) |

### Build & Packaging

| Library | Version | Purpose |
|---------|---------|---------|
| Shadow Plugin | `9.6.1` | Fat JAR packaging |
| Micrometer | `1.17.1` | Metrics & monitoring |

### Testing

| Library | Version | Purpose |
|---------|---------|---------|
| MockK | `1.14.11` | Kotlin mocking framework |

---

## Frontend Stack (React/TypeScript)

*Source: `modules/frontend/sangita-admin-web/package.json`*

### Core Framework

| Library | Version | Purpose |
|---------|---------|---------|
| React | `19.2.8` | UI framework |
| TypeScript | `6.0.x` | Type-safe JavaScript (TypeScript 7 deferred — no programmatic API until 7.1) |
| Vite | `8.2.2` | Build tool & dev server (Rolldown bundler) |

### Styling & UI

| Library | Version | Purpose |
|---------|---------|---------|
| Tailwind CSS | `4.3.3` | Utility-first CSS |

### Routing & State

| Library | Version | Purpose |
|---------|---------|---------|
| React Router | `7.18.3` | Client-side routing |
| TanStack Query | `5.102.8` | Data fetching & caching |
| Google GenAI | `1.34.0` | AI integration (JS 2.x deferred) |

### Development & Testing

| Library | Version | Purpose |
|---------|---------|---------|
| ESLint | `10.9.1` | Code linting (via `bun run`; needs Bun runtime). `eslint-plugin-react-hooks` pinned **7.0.1** — 7.1.1's `react-hooks/immutability` is a large UI refactor, deferred. |
| Vitest | `4.1.11` | Unit testing (Vitest 5 deferred — still RC) |
| Playwright | `1.62.1` | E2E testing |

---

## Python Tools

### Extraction Worker (`tools/krithi-extract-enrich-worker`)

*Source: `tools/krithi-extract-enrich-worker/pyproject.toml` · Lock: `uv.lock`*

#### Core Libraries

| Library | Version | Purpose |
|---------|---------|---------|
| PyMuPDF | `1.28.2` | Primary PDF text extraction |
| pdfplumber | `0.11.10` | Tabular PDF extraction fallback |
| pytesseract | `0.3.13` | Tesseract OCR wrapper |
| indic-transliteration | `2.3.82` | Script conversion (Devanagari ↔ Tamil/Telugu/etc.) |
| Pydantic | `2.13.5` | Schema validation |
| Pydantic Settings | `2.15.0` | Environment-based config validation |
| psycopg | `3.3.4` | PostgreSQL driver (async-capable) |
| google-genai | `>=2.0.0` (resolved `2.20.0`) | Unified Gemini SDK; 2.0 breaking changes are Interactions-API-only — the worker's generate_content/batches surface is unaffected (TRACK-124) |
| RapidFuzz | `3.14.5` | Fast fuzzy matching |
| HTTPX | `0.28.1` | Async HTTP client |
| BeautifulSoup4 | `4.15.0` | HTML parsing |
| structlog | `26.1.0` | Structured JSON logging |
| Click | `8.5.0` | CLI framework |

#### Development & Testing

| Library | Version | Purpose |
|---------|---------|---------|
| pytest | `9.1.1` | Test framework |
| Ruff | `0.16.5` | Linter & formatter |
| mypy | `2.3.1` | Static type checker |

### Migration Tool

| Component | Version | Notes |
|---------|---------|---------|
| Flyway Community | `12.11.0` | Single migration engine ([ADR-013](../02-architecture/decisions/ADR-013-db-migration-with-flyway.md)). Make/dev/CI via `flyway/flyway:12.11.0-alpine`; Kotlin Testcontainers via the Flyway JVM API. Pinned in `gradle/libs.versions.toml` (`flyway`) and `compose.yaml`. Flyway 13 deferred. |

Migrations are standardized on **Flyway Community** ([ADR-013](../02-architecture/decisions/ADR-013-db-migration-with-flyway.md)). The previous Python tool (`tools/db-migrate`, psycopg `>=3.1`) is superseded and archived (`archive/tools/db-migrate/`, TRACK-110).

### Test & CI Substrate

| Component | Version | Notes |
|---------|---------|---------|
| Testcontainers | `2.0.5` | `org.testcontainers:testcontainers-postgresql` (artifact renamed in 2.x; `PostgreSQLContainer` now in `org.testcontainers.postgresql`, no self-type generic). Integration tests self-provision `postgres:18.3-alpine`. Pinned in `gradle/libs.versions.toml` (`testcontainers`). TRACK-123. |
| GitHub Actions CI | — | `.github/workflows/ci.yml` (TRACK-111): backend unit/integration, Flyway migrate+validate, frontend typecheck+build, worker pytest. Blocking, PR-triggered (D7/D8). |

Shared integration-test infrastructure (`IntegrationTestBase`, `SangitaPostgres`, `TestDatabase`, `TestFixtures`) lives in the **`:modules:backend:test-support`** module (TRACK-111, D11), consumed by both the `api` and `dal` test classpaths.

---

## Mobile Stack (Kotlin Multiplatform)

*Source: `gradle/libs.versions.toml`*

| Library | Version | Purpose |
|---------|---------|---------|
| Kotlin | `2.4.10` | Shared with backend |
| Compose Multiplatform | `1.12.0` | Cross-platform UI (material3 `1.9.0` own train; icons-extended frozen `1.7.3`; iosX64 dropped by CMP 1.11+; **compileSdk 37** required by AndroidX Compose 1.12) |
| Android Gradle Plugin | `9.3.2` | Android build (Gradle wrapper `9.7.1`) |
| Ktor Client | `3.5.2` | HTTP client |

---

## Cloud & External Services

*Source: `gradle/libs.versions.toml`*

| Library | Version | Purpose |
|---------|---------|---------|
| AWS SDK | `2.54.7` | S3 storage (future) |
| Google Auth | `1.51.0` | SSO integration (future) |

---

## Database

| Component | Version | Notes |
|-----------|---------|-------|
| PostgreSQL | `18.3` | Docker image: `postgres:18.3-alpine` |

---

## Version History

| Date | Change |
|------|--------|
| 2026-08-29 | TRACK-135 (Batch 1–2c): PostgreSQL JDBC 42.7.11→42.7.13 (CVE-2026-54291), Ktor 3.5.0→3.5.2, Exposed 1.0.0→1.5.0, Kotlin 2.4.0→2.4.10, CMP 1.11.1→1.12.0, AGP 9.0.0→9.3.2, Gradle 9.1.0→9.7.1, Flyway 12.9.0→12.11.0, React 19.2.7→19.2.8, Vite 8.1.3→8.2.2, plus catalog/frontend/Python lock drop-ins. TypeScript 7 / Vitest 5 / Flyway 13 / Logback 1.6 / datetime 0.8 deferred. |
| 2026-08-29 | Bun 1.3.7→1.4.0 (mise pin, CI `oven-sh/setup-bun` + fallback installer, monorepo-orchestration skill). Frontend `bun install` + typecheck + build green on 1.4.0. |
| 2026-07-10 | TRACK-122 (Batch 3a): Kotlin 2.3.0→2.4.0, Compose Multiplatform 1.10.0→1.11.1. CMP 1.11 fallout: `compose.*` plugin accessors deprecated → explicit catalog deps (material3 on its own `1.9.0` train, icons-extended frozen `1.7.3`); iosX64 no longer published by CMP → target dropped from `:shared:presentation`; `-Xexplicit-backing-fields` now in-language (flag removed); Kotlin/Native `sourceInfoType=none`→`noop`. Backend + KMP builds and full test suites green. |
| 2026-07-10 | TRACK-124 (Batch 3c): google-genai floor >=1.0.0→>=2.0.0 (resolved 1.34.0→2.9.0); 2.0 breaking changes confined to the Interactions API, worker call sites untouched; 144 worker tests green incl. HTTP-stubbed SDK round-trips. |
| 2026-07-10 | TRACK-123 (Batch 3b): Testcontainers 1.21.4→2.0.5 — artifact renamed to `org.testcontainers:testcontainers-postgresql`, `PostgreSQLContainer` moved to `org.testcontainers.postgresql` and lost its self-type generic; full backend + DAL integration suites green. |
| 2026-07-08 | TRACK-121 (frontend major toolchain): TypeScript 5.9→6.0, ESLint 9.39.2→10.6.0 (+ typescript-eslint 8.63.0, @eslint/js 10.0.1, eslint-plugin-react-refresh 0.5.3), Vite 7.3.1→8.1.3 (Rolldown), @vitejs/plugin-react 5→6.0.3, Vitest 4.1.9→4.1.10. Vitest 5 deferred (beta). Added `bunfig.toml` (`[run] bun = true`) — ESLint 10 / Vite 8 need `util.styleText`, absent in the box's EOL Node 21, so `bun run` scripts execute under Bun; direct calls use `bunx --bun`. |
| 2026-06-24 | TRACK-120 (Batch 1 safe upgrades): PostgreSQL JDBC 42.7.10→42.7.11 (CVE-2026-42198), Ktor 3.4.0→3.5.0, Koin 4.1.1→4.2.1, Logback 1.5.32→1.5.34, Flyway 12.8.1→12.9.0, React 19.2.4→19.2.7, TanStack Query 5.90.21→5.101.1, Tailwind 4.2.1→4.3.1, React Router 7.13.1→7.18.0, Vitest 4.0.18→4.1.9, Playwright 1.40.0→1.61.1, pydantic 2.12.5→2.13.4, psycopg 3.3.2→3.3.4, PyMuPDF 1.27.1→1.27.2.3 |
| 2026-06-06 | TRACK-106 re-sync: Bun 1.3.6→1.3.7, Python version pin corrected to 3.11+ (mise.toml), Last Updated synced |
| 2026-03-10 | Added Python tools section (extraction worker + db-migrate) with resolved versions from uv.lock |
| 2026-03-10 | Dependency updates: PostgreSQL 42.7.10, Logback 1.5.32, Jsoup 1.22.1, Caffeine 3.2.3, Tailwind 4.2.1, React Router 7.13.1, TanStack Query 5.90.21 |
| 2026-03-10 | Synced all versions from source files (gradle/libs.versions.toml, package.json, compose.yaml) |
| 2026-01-30 | Initial auto-generated from source files |

---

## How to Use This File

### In Documentation

Instead of hardcoding versions, reference this file:

```markdown
For current versions, see [Current Versions](./current-versions.md).
```

### Updating Versions

1. Update the source file (`gradle/libs.versions.toml`, `package.json`, or `.mise.toml`)
2. Update this file to match
3. Commit both the source file and the updated `current-versions.md`
