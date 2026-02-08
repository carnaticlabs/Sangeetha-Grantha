| Metadata | Value |
|:---|:---|
| **Status** | Auto-Generated |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-01-30 |
| **Generated** | 2026-01-30T03:54:07Z |
| **Tool** | sangita-cli docs sync-versions |

# Current Technology Versions

> **This file is auto-generated.** Do not edit manually.
>
> Run `sangita-cli docs sync-versions` to regenerate after updating dependencies.
>
> All documentation should reference this file instead of hardcoding version numbers.

---

## Development Toolchain

*Source: `.mise.toml`*

| Tool | Version | Notes |
|------|---------|-------|
| Java | `temurin-25` | Temurin distribution, JVM toolchain |
| Rust | `1.93.0` | For sangita-cli tool |
| Bun | `1.3.6` | Frontend package manager |
| Docker Compose | `latest` | Database container management |

---

## Backend Stack (Kotlin/JVM)

*Source: `gradle/libs.versions.toml`*

### Core Framework

| Library | Version | Purpose |
|---------|---------|---------|
| Kotlin | `2.3.0` | Language version |
| Ktor | `3.4.0` | HTTP server & client framework |
| Exposed | `1.0.0` | SQL ORM (DSL-based) |
| Koin | `3.5.6` | Dependency injection |

### Kotlinx Libraries

| Library | Version | Purpose |
|---------|---------|---------|
| Coroutines | `1.10.2` | Async programming |
| DateTime | `0.7.1` | Cross-platform date/time |
| Serialization JSON | `1.10.0` | JSON serialization |

### Database & Infrastructure

| Library | Version | Purpose |
|---------|---------|---------|
| PostgreSQL Driver | `42.7.9` | JDBC driver |
| HikariCP | `7.0.2` | Connection pooling |
| Logback | `1.5.25` | Logging framework |
| Logstash Encoder | `8.0` | JSON log formatting |
| Commons CSV | `1.10.0` | CSV parsing |

### Security & Auth

| Library | Version | Purpose |
|---------|---------|---------|
| JWT (Auth0) | `4.5.0` | JWT token handling |
| Google Auth | `1.23.0` | OAuth2 (future SSO) |

### Build & Packaging

| Library | Version | Purpose |
|---------|---------|---------|
| Shadow Plugin | `9.2.2` | Fat JAR packaging |
| Micrometer | `1.12.3` | Metrics & monitoring |

### Testing

| Library | Version | Purpose |
|---------|---------|---------|
| MockK | `1.14.7` | Kotlin mocking framework |

---

## Frontend Stack (React/TypeScript)

*Source: `modules/frontend/sangita-admin-web/package.json`*

### Core Framework

| Library | Version | Purpose |
|---------|---------|---------|
| React | `19.2.3` | UI framework |
| TypeScript | `5.8.2` | Type-safe JavaScript |
| Vite | `6.2.0` | Build tool & dev server |

### Styling & UI

| Library | Version | Purpose |
|---------|---------|---------|
| Tailwind CSS | `4.1.18` | Utility-first CSS |

### Routing & State

| Library | Version | Purpose |
|---------|---------|---------|
| React Router | `7.11.0` | Client-side routing |
| TanStack Query | `5.90.20` | Data fetching & caching |

### Development & Testing

| Library | Version | Purpose |
|---------|---------|---------|
| ESLint | `9.39.2` | Code linting |
| Vitest | `4.0.18` | Unit testing |

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
| AWS SDK | `2.29.0` | S3 storage (future) |
| Google Auth | `1.23.0` | SSO integration (future) |

---

## Version History

| Date | Change |
|------|--------|
| 2026-01-30 | Auto-generated from source files |

---

## How to Use This File

### In Documentation

Instead of hardcoding versions, reference this file:

```markdown
For current versions, see [Current Versions](./current-versions.md).

The backend uses Ktor (see [current version](./current-versions.md#core-framework))
with Exposed ORM for database access.
```

### Updating Versions

1. Update the source file (`gradle/libs.versions.toml`, `package.json`, or `.mise.toml`)
2. Run `sangita-cli docs sync-versions`
3. Commit both the source file and the regenerated `current-versions.md`

### CI Integration

Add to your CI pipeline to ensure versions stay in sync:

```yaml
- name: Verify version sync
  run: |
    sangita-cli docs sync-versions --check
```