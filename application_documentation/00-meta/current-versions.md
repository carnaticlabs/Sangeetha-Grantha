| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.1.0 |
| **Last Updated** | 2026-03-10 |
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
| Bun | `1.3.6` | Frontend package manager & runtime |
| Python | `3.14+` | Migration tool & extraction worker |
| Docker Compose | `latest` | Container orchestration |

---

## Backend Stack (Kotlin/JVM)

*Source: `gradle/libs.versions.toml`*

### Core Framework

| Library | Version | Purpose |
|---------|---------|---------|
| Kotlin | `2.3.0` | Language version |
| Ktor | `3.4.0` | HTTP server & client framework |
| Exposed | `1.0.0` | SQL ORM (DSL-based) |
| Koin | `4.1.1` | Dependency injection |

### Kotlinx Libraries

| Library | Version | Purpose |
|---------|---------|---------|
| Coroutines | `1.10.2` | Async programming |
| DateTime | `0.7.1` | Cross-platform date/time |
| Serialization JSON | `1.10.0` | JSON serialization |

### Database & Infrastructure

| Library | Version | Purpose |
|---------|---------|---------|
| PostgreSQL Driver | `42.7.10` | JDBC driver |
| HikariCP | `7.0.2` | Connection pooling |
| Logback | `1.5.32` | Logging framework |
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
| React | `19.2.4` | UI framework |
| TypeScript | `5.9.x` | Type-safe JavaScript |
| Vite | `7.3.1` | Build tool & dev server |

### Styling & UI

| Library | Version | Purpose |
|---------|---------|---------|
| Tailwind CSS | `4.2.1` | Utility-first CSS |

### Routing & State

| Library | Version | Purpose |
|---------|---------|---------|
| React Router | `7.13.1` | Client-side routing |
| TanStack Query | `5.90.21` | Data fetching & caching |
| Google GenAI | `1.34.0` | AI integration |

### Development & Testing

| Library | Version | Purpose |
|---------|---------|---------|
| ESLint | `9.39.2` | Code linting |
| Vitest | `4.0.18` | Unit testing |
| Playwright | `1.40.0` | E2E testing |

---

## Python Tools

### Extraction Worker (`tools/krithi-extract-enrich-worker`)

*Source: `tools/krithi-extract-enrich-worker/pyproject.toml` · Lock: `uv.lock`*

#### Core Libraries

| Library | Version | Purpose |
|---------|---------|---------|
| PyMuPDF | `1.27.1` | Primary PDF text extraction |
| pdfplumber | `0.11.9` | Tabular PDF extraction fallback |
| pytesseract | `0.3.13` | Tesseract OCR wrapper |
| indic-transliteration | `2.3.76` | Script conversion (Devanagari ↔ Tamil/Telugu/etc.) |
| Pydantic | `2.12.5` | Schema validation |
| psycopg | `3.3.2` | PostgreSQL driver (async-capable) |
| google-generativeai | `0.8.6` | Gemini API client |
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

### Migration Tool (`tools/db-migrate`)

*Source: `tools/db-migrate/pyproject.toml`*

| Library | Version | Purpose |
|---------|---------|---------|
| psycopg | `>=3.1` | PostgreSQL driver |

---

## Mobile Stack (Kotlin Multiplatform)

*Source: `gradle/libs.versions.toml`*

| Library | Version | Purpose |
|---------|---------|---------|
| Kotlin | `2.3.0` | Shared with backend |
| Compose Multiplatform | `1.10.0` | Cross-platform UI |
| Android Gradle Plugin | `9.0.0` | Android build |
| Ktor Client | `3.4.0` | HTTP client |

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
