---
name: test-troubleshooter
description: Systematic diagnostic workflow for diagnosing and resolving backend test failures in Kotlin, Ktor, Exposed, and H2/PostgreSQL. Use when tests fail with SQL exceptions, missing tables, DI/constructor mismatches, or schema errors.
---

# Test Troubleshooter

This skill guides the systematic triage, diagnosis, and remediation of test failures in `modules/backend/api`, particularly when dealing with database schema discrepancies, H2/PostgreSQL compatibility issues, or dependency wiring errors.

## 1. Run & Capture Detailed Diagnostics

Execute the failing test class or method with verbose info logging enabled:

```bash
./gradlew :modules:backend:api:test --tests "<TestClassName>" --info
```

Look for key error signatures:
- `ExposedSQLException` / `JdbcSQLDataException` (SQL / Schema issue)
- `UninitializedPropertyAccessException` / `NoClassDefFoundError` (Test fixture setup)
- `AssertionFailedError` / `AssertionError` (Business logic discrepancy)

---

## 2. Common Failure Archetypes & Solutions

### A. H2 Enum / Custom Postgres Domain Mismatch
**Symptom**: `Unknown data type: "ENUM_NAME"` or `Data conversion error converting "..."`
**Cause**: PostgreSQL uses custom `CREATE TYPE ... AS ENUM (...)`, whereas in-memory H2 may not understand custom domain types automatically.
**Fix**:
1. Open `modules/backend/api/src/test/kotlin/com/sangita/grantha/backend/api/support/TestDatabaseFactory.kt`.
2. Add an H2-compatible domain fallback before creating tables:
```kotlin
exec("CREATE DOMAIN IF NOT EXISTS <enum_name> AS VARCHAR")
```

### B. Missing Table in Test Setup
**Symptom**: `Table "TABLE_NAME" not found` during test execution.
**Cause**: The table was added to production Flyway migrations or DAL, but not added to `SchemaUtils.create(...)` in test fixtures.
**Fix**:
1. Open `TestDatabaseFactory.kt`.
2. Verify that the table object from `CoreTables.kt` or `DalTables.kt` is included in the `SchemaUtils.create(...)` call.

### C. Constructor / Dependency Injection Mismatch
**Symptom**: Kotlin compile error: `No value passed for parameter '<param>'`.
**Cause**: Service constructor signature changed, but test instantiation was not updated.
**Fix**:
1. Inspect the service constructor in `modules/backend/api/src/main/kotlin/.../services/`.
2. Update the test's `@BeforeEach setup()` method, providing a mock (via `mockk<T>()`) or real DAL instance.

---

## 3. Iterative Verification

After applying changes, re-run only the target test:

```bash
./gradlew :modules:backend:api:test --tests "<TestClassName>"
```

Once passing, run the full backend test suite to prevent regressions:

```bash
make test
```
