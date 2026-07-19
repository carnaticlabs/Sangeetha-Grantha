---
name: ktor-exposed-backend
description: Backend conventions for the Kotlin + Ktor + Exposed stack. Use when editing routes, services, repositories, or Exposed tables in modules/backend/api or modules/backend/dal, adding endpoints, writing backend tests, or touching auth, audit logging, or transactions.
---

# Ktor + Exposed Backend (modules/backend/)

Shared rules are in [CLAUDE.md](../../../CLAUDE.md); versions in [current-versions.md](../../../application_documentation/00-meta/current-versions.md). This skill adds the enforcement detail.

## Layering: route → service → repository

- Routes (in `modules/backend/api/.../routes/`) stay thin: deserialize, auth-check, call the service, map the result to an HTTP status. Business rules live in services; data access in repositories behind interfaces, injected via Koin.
- Error handling goes through StatusPages / centralized interception, not per-route try/catch.

## Non-negotiables

1. **Every DB operation runs inside `DatabaseFactory.dbQuery { }`** (`modules/backend/dal/.../DatabaseFactory.kt`) — it owns the transaction and Hikari pooling; a raw `transaction { }` bypasses both.
2. **DTOs cross the repository boundary, never Exposed rows.** Map `ResultRow`s to the `@Serializable` DTOs (shared ones live in `modules/shared/domain`) *inside* the `dbQuery` block, so nothing lazy escapes the transaction.
3. **Every mutation writes to the audit log** via `AuditLogRepository` — inserts, updates, deletes, no exceptions. `AuditLogTest` in the DAL integration suite guards this.
4. **Admin routes are gated by `requireRole`** (see `api/plugins/Routing.kt`, `api/routes/RouteHelpers.kt`). Roles derive from stored `role_assignments`, never from request-supplied claims (ADR-004 v1.3). Auth is moving to OAuth/OTP — don't add new password-login plumbing.

## Kotlin style (this layer)

Interfaces for repository/service contracts, constructor injection, immutability, expression bodies, and functional collection pipelines (`map`, `filter`, `associateBy`) over mutable accumulation. Typical shape:

```kotlin
class KrithiService(private val repo: KrithiRepository, private val audit: AuditLogRepository) {
    suspend fun rename(id: UUID, title: String, actor: String): KrithiDto =
        repo.update(id) { it.copy(title = title) }
            .also { audit.record(action = "RENAME_KRITHI", target = it.id, actor = actor) }
}
```

## Build & test

- Build: `./gradlew :modules:backend:api:build` (fat JAR) · Run dev: `runDev`
- Tests: `make test` (unit) and `make test-integration`; integration tests self-provision Postgres via Testcontainers using shared infra in `:modules:backend:test-support` (`IntegrationTestBase`, `SangitaPostgres`) and migrate via the Flyway JVM API — never a custom runner.
