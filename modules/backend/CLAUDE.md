# Backend Module

Kotlin + Ktor backend split into `api/` (HTTP routes) and `dal/` (data access).

## Quick Reference
```bash
./gradlew :modules:backend:api:build        # Build
./gradlew :modules:backend:api:test         # Run tests
./gradlew :modules:backend:api:runDev       # Run dev server (port 8080)
./gradlew :modules:backend:api:seedDatabase # Seed DB
```

## Key Rules
- All DB calls via `DatabaseFactory.dbQuery { }` — never raw connections
- Return DTOs from repositories, never Exposed entity objects
- Every create/update/delete must write to `AUDIT_LOG` table
- JWT auth with role-based claims at route level
- Dependencies must reference `gradle/libs.versions.toml` via `alias(libs.xyz)`
- Main class: `com.sangita.grantha.backend.api.AppKt`

## Test Conventions
- Integration tests in `api/src/test/kotlin/.../integration/`
- Use `testApplication` from Ktor Server Test Host
- Seed via `IntegrationTestEnv` and `IntegrationSeedData`
- Deterministic fixtures: fixed UUIDs, known timestamps
