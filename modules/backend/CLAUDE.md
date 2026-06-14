# Backend Module

Kotlin + Ktor backend split into `api/` (HTTP routes) and `dal/` (data access).

## Quick Reference
```bash
./gradlew :modules:backend:api:build        # Build
./gradlew :modules:backend:api:test         # Run tests
./gradlew :modules:backend:api:runDev       # Run dev server (port 8080)
```

Reference data ships via Flyway `R__` repeatable migrations (`make migrate` / `make db-reset`)
and dev sample data via `make seed-dev` (ADR-013). There is no Gradle seed task. The admin user is
provisioned out-of-band via `./gradlew :modules:backend:api:bootstrapAdmin` (`make bootstrap-admin`).

## Key Rules
- All DB calls via `DatabaseFactory.dbQuery { }` — never raw connections
- Return DTOs from repositories, never Exposed entity objects
- Every create/update/delete must write to `AUDIT_LOG` table
- JWT auth with role-based claims at route level
- Dependencies must reference `gradle/libs.versions.toml` via `alias(libs.xyz)`
- Main class: `com.sangita.grantha.backend.api.AppKt`

## Test Conventions
- Integration tests extend `IntegrationTestBase` (`api/src/test/.../support/`). It self-provisions
  a Postgres via **Testcontainers** (`SangitaPostgres`) and schema-migrates it with the **Flyway JVM
  API** (`TestDatabase`, schema-only — `R__` reference data is skipped) — no `localhost:5432`.
- Set `TEST_DATABASE_URL` to point the suite at an external Postgres (e.g. a CI service container)
  instead of starting a container.
- State resets by truncating all tables after each test (`flyway_schema_history` preserved).
- Build your own data with `TestFixtures` (deterministic builders); HTTP via `testApplication`
  (Ktor Server Test Host).
- Tagged `@Tag("integration")`: `make test` / `./gradlew check` run everything; `make test-integration`
  (`:integrationTest` task) runs only the tagged set. Requires Docker.
