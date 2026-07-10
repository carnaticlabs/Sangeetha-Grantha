| Metadata | Value |
|:---|:---|
| **Status** | Completed |
| **Version** | 2.0.0 |
| **Last Updated** | 2026-07-10 |
| **Author** | Sangeetha Grantha Team |

# Goal
Batch 3b — Upgrade Testcontainers `1.21.4` → `2.0.5` (major). Test-only blast radius, scoped to the integration-test substrate established under ADR-013 / TRACK-110.

# Scope (`gradle/libs.versions.toml`)
- **Testcontainers** `1.21.4` → `2.0.5` — `testcontainers-postgresql` (`org.testcontainers:postgresql`).

# Affected code
- `:modules:backend:test-support` module — `IntegrationTestBase`, `SangitaPostgres`, `TestDatabase`, `TestFixtures` (consumed by `api` and `dal` test classpaths).
- Integration tests that self-provision `postgres:18.3-alpine`.

# Implementation Plan
1. Bump `testcontainers` in `libs.versions.toml`.
2. Review the Testcontainers 2.0 migration notes for API changes (container lifecycle, network, JUnit integration).
3. Update `test-support` helpers for any renamed/removed APIs.
4. Run `make test` + DAL integration suite; confirm containers provision and tests pass.
5. Sync `current-versions.md` (Test & CI Substrate section).
6. Commit per commit-policy.

# Risks
- Major version — expect API surface changes; confined to test classpaths (no production runtime impact).
- Verify Flyway JVM API (`flyway-core`) still integrates with the new Testcontainers lifecycle.

# Completion (2026-07-10)
Actual 2.x delta was small for this codebase:
- Artifact renamed: `org.testcontainers:postgresql` → `org.testcontainers:testcontainers-postgresql`
  (the old coordinate has no 2.x releases — Maven metadata still says latest 1.21.4).
- `PostgreSQLContainer` moved to package `org.testcontainers.postgresql` and dropped the
  `<SELF>` self-type generic (`PostgreSQLContainer<*>` → `PostgreSQLContainer`).
- No other API changes hit `SangitaPostgres`/`TestDatabase`/`IntegrationTestBase`; Flyway JVM API
  integration unchanged. Verified: `:dal:integrationTest`, `:api:integrationTest`, and the full
  `:dal:test` + `:api:test` suites green on 2.0.5.
