# Backend Module

Kotlin + Ktor backend split into `api/` (HTTP routes), `dal/` (data access), and `test-support/`
(shared integration-test substrate, compiled as `src/main` so both test classpaths consume it).

## Quick Reference
```bash
./gradlew :modules:backend:api:build         # Build
./gradlew :modules:backend:api:test          # Run all api tests (unit + integration)
./gradlew :modules:backend:api:unitTest      # Unit slice only (no Docker, <30s)
./gradlew :modules:backend:dal:integrationTest  # DAL D1–D6 suite (Testcontainers)
./gradlew :modules:backend:api:runDev        # Run dev server (port 8080)
```

Reference data ships via Flyway `R__` repeatable migrations (`make migrate` / `make db-reset`)
and dev sample data via `make seed-dev` (ADR-013). There is no Gradle seed task. The admin user is
provisioned out-of-band via `./gradlew :modules:backend:api:bootstrapAdmin` (`make bootstrap-admin`).

## Key Rules
- All DB calls via `DatabaseFactory.dbQuery { }` — never raw connections
- **`dbQuery` nests**: a `dbQuery` inside another one joins the enclosing transaction rather than
  opening its own, so wrapping a multi-repo operation in `dbQuery` makes it atomic — all of it
  commits or none of it does. Use this for any read-then-write that must not half-apply (see
  `ImportService.reviewImport`). Two consequences worth knowing: a wrapped operation no longer sees
  other transactions' uncommitted rows, so read-then-write races need an explicit row lock
  (`forUpdate()`) rather than relying on incremental commits; and a long wrap holds a pooled
  connection for its whole duration. Contract is pinned by `DbQueryNestingTest`.
- Return DTOs from repositories, never Exposed entity objects
- Every create/update/delete must write to `AUDIT_LOG` table
- JWT auth with role-based claims at route level
- Dependencies must reference `gradle/libs.versions.toml` via `alias(libs.xyz)`
- Main class: `com.sangita.grantha.backend.api.AppKt`

## Test Conventions
- Integration tests extend `IntegrationTestBase` from **`:modules:backend:test-support`** (package
  `com.sangita.grantha.backend.testsupport`; depend via `testImplementation(project(...))`). It
  self-provisions a Postgres via **Testcontainers** (`SangitaPostgres`) and schema-migrates it with
  the **Flyway JVM API** (`TestDatabase` — full `V__`+`R__`, so tests run against the real reference
  seed) — no `localhost:5432`. Fixtures `findOrCreate` against the seed; reference tables are kept out
  reset to their seed snapshot after each test (test-created rows deleted, seeded rows untouched) —
  so the seed is read-only and the suite is idempotent even against a persistent/external DB.
- Set `TEST_DATABASE_URL` to point the suite at an external Postgres (e.g. a CI service container)
  instead of starting a container.
- State resets by truncating all tables after each test (`flyway_schema_history` preserved).
- Build your own data with `TestFixtures` (deterministic builders); HTTP via `testApplication`
  (Ktor Server Test Host).
- Tagged `@Tag("integration")`: `make test` / `./gradlew check` run everything; `make test-integration`
  (`:integrationTest` task) runs only the tagged set. Requires Docker. `unitTest` runs the non-tagged
  slice with no Docker.
- Constraint violations surface as typed `DalException`s (`DuplicateKeyException` / `ForeignKeyViolationException`,
  `dal/errors/`) — mapped centrally in `DatabaseFactory.dbQuery`, so repos need no per-call handling.
- DAL data-integrity scenarios (D1–D6: migrations-from-scratch, table round-trips, UUIDv7, junction
  cascade, typed errors, audit log) live in `dal/src/test/.../integration/`.
