| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.3.0 |
| **Last Updated** | 2026-07-08 |
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
| Bun | `1.3.7` | Frontend package manager & runtime |
| Python | `3.11+` | Migration tool & extraction worker (runtime: 3.14) |
| Docker Compose | `latest` | Container orchestration |

---

## Backend Stack (Kotlin/JVM)

*Source: `gradle/libs.versions.toml`*

### Core Framework

| Library | Version | Purpose |
|---------|---------|---------|
| Kotlin | `2.3.0` | Language version |
| Ktor | `3.5.0` | HTTP server & client framework |
| Exposed | `1.0.0` | SQL ORM (DSL-based) |
| Koin | `4.2.1` | Dependency injection |

### Kotlinx Libraries

| Library | Version | Purpose |
|---------|---------|---------|
| Coroutines | `1.10.2` | Async programming |
| DateTime | `0.7.1` | Cross-platform date/time |
| Serialization JSON | `1.10.0` | JSON serialization |

### Database & Infrastructure

| Library | Version | Purpose |
|---------|---------|---------|
| PostgreSQL Driver | `42.7.11` | JDBC driver (CVE-2026-42198 fix) |
| HikariCP | `7.0.2` | Connection pooling |
| Logback | `1.5.34` | Logging framework |
| Logstash Encoder | `8.0` | JSON log formatting |
| Commons CSV | `1.14.1` | CSV parsing |
| Jsoup | `1.22.1` | HTML parsing |
| Caffeine | `3.2.3` | In-memory caching |
| dotenv-kotlin | `6.5.1` | Environment variable loading |

### Security & Auth

| Library | Version | Purpose |
|---------|---------|---------|
| JWT (Auth0) | `4.5.0` | JWT token handling |
| Google Auth | `1.41.0` | OAuth2 (future SSO) |
| password4j | `1.8.2` | argon2id password hashing (TRACK-114) |

### Build & Packaging

| Library | Version | Purpose |
|---------|---------|---------|
| Shadow Plugin | `9.3.1` | Fat JAR packaging |
| Micrometer | `1.15.2` | Metrics & monitoring |

### Testing

| Library | Version | Purpose |
|---------|---------|---------|
| MockK | `1.14.9` | Kotlin mocking framework |

---

## Frontend Stack (React/TypeScript)

*Source: `modules/frontend/sangita-admin-web/package.json`*

### Core Framework

| Library | Version | Purpose |
|---------|---------|---------|
| React | `19.2.7` | UI framework |
| TypeScript | `6.0.x` | Type-safe JavaScript |
| Vite | `8.1.3` | Build tool & dev server (Rolldown bundler) |

### Styling & UI

| Library | Version | Purpose |
|---------|---------|---------|
| Tailwind CSS | `4.3.1` | Utility-first CSS |

### Routing & State

| Library | Version | Purpose |
|---------|---------|---------|
| React Router | `7.18.0` | Client-side routing |
| TanStack Query | `5.101.1` | Data fetching & caching |
| Google GenAI | `1.34.0` | AI integration |

### Development & Testing

| Library | Version | Purpose |
|---------|---------|---------|
| ESLint | `10.6.0` | Code linting (via `bun run`; needs Bun runtime) |
| Vitest | `4.1.10` | Unit testing (Vitest 5 deferred — still beta) |
| Playwright | `1.61.1` | E2E testing |

---

## Python Tools

### Extraction Worker (`tools/krithi-extract-enrich-worker`)

*Source: `tools/krithi-extract-enrich-worker/pyproject.toml` · Lock: `uv.lock`*

#### Core Libraries

| Library | Version | Purpose |
|---------|---------|---------|
| PyMuPDF | `1.27.2.3` | Primary PDF text extraction |
| pdfplumber | `0.11.9` | Tabular PDF extraction fallback |
| pytesseract | `0.3.13` | Tesseract OCR wrapper |
| indic-transliteration | `2.3.76` | Script conversion (Devanagari ↔ Tamil/Telugu/etc.) |
| Pydantic | `2.13.4` | Schema validation |
| psycopg | `3.3.4` | PostgreSQL driver (async-capable) |
| google-genai | `>=1.0.0` | Unified Gemini SDK (replaced deprecated google-generativeai) |
| RapidFuzz | `3.14.3` | Fast fuzzy matching |
| HTTPX | `0.28.1` | Async HTTP client |
| BeautifulSoup4 | `4.14.3` | HTML parsing |
| structlog | `25.5.0` | Structured JSON logging |
| Click | `8.3.1` | CLI framework |

#### Development & Testing

| Library | Version | Purpose |
|---------|---------|---------|
| pytest | `9.0.2` | Test framework |
| Ruff | `0.15.0` | Linter & formatter |
| mypy | `1.19.1` | Static type checker |

### Migration Tool

| Component | Version | Notes |
|---------|---------|---------|
| Flyway Community | `12.9.0` | Single migration engine ([ADR-013](../02-architecture/decisions/ADR-013-db-migration-with-flyway.md)). Make/dev/CI via `flyway/flyway:12.9.0-alpine`; Kotlin Testcontainers via the Flyway JVM API. Pinned in `gradle/libs.versions.toml` (`flyway`) and `compose.yaml`. |

Migrations are standardized on **Flyway Community** ([ADR-013](../02-architecture/decisions/ADR-013-db-migration-with-flyway.md)). The previous Python tool (`tools/db-migrate`, psycopg `>=3.1`) is superseded and archived (`tools/db-migrate-archived/`, TRACK-110).

### Test & CI Substrate

| Component | Version | Notes |
|---------|---------|---------|
| Testcontainers | `1.21.4` | `org.testcontainers:postgresql`; integration tests self-provision `postgres:18.3-alpine`. Pinned in `gradle/libs.versions.toml` (`testcontainers`). |
| GitHub Actions CI | — | `.github/workflows/ci.yml` (TRACK-111): backend unit/integration, Flyway migrate+validate, frontend typecheck+build, worker pytest. Blocking, PR-triggered (D7/D8). |

Shared integration-test infrastructure (`IntegrationTestBase`, `SangitaPostgres`, `TestDatabase`, `TestFixtures`) lives in the **`:modules:backend:test-support`** module (TRACK-111, D11), consumed by both the `api` and `dal` test classpaths.

---

## Mobile Stack (Kotlin Multiplatform)

*Source: `gradle/libs.versions.toml`*

| Library | Version | Purpose |
|---------|---------|---------|
| Kotlin | `2.3.0` | Shared with backend |
| Compose Multiplatform | `1.10.0` | Cross-platform UI |
| Android Gradle Plugin | `9.0.0` | Android build |
| Ktor Client | `3.5.0` | HTTP client |

---

## Cloud & External Services

*Source: `gradle/libs.versions.toml`*

| Library | Version | Purpose |
|---------|---------|---------|
| AWS SDK | `2.41.22` | S3 storage (future) |
| Google Auth | `1.41.0` | SSO integration (future) |

---

## Database

| Component | Version | Notes |
|-----------|---------|-------|
| PostgreSQL | `18.3` | Docker image: `postgres:18.3-alpine` |

---

## Version History

| Date | Change |
|------|--------|
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
