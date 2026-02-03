---
description: systematic workflow for diagnosing and fixing test failures, specifically addressing H2/Postgres compatibility and schema issues.
---

# Test Troubleshooter

This workflow guides the debugging of test failures, focusing on common issues like database schema mismatches, missing dependencies, or logic errors.

## 1. Run and Capture

Execute the failing test with detailed logging enabled.

```bash
./gradlew :modules:backend:api:test --tests "<TestClassName>" --info
```

**Analyze the Output:**
- Look for `ExposedSQLException`, `JdbcSQLDataException`, or `AssertionFailedError`.
- Identify the specific table, column, or enum causing the issue.

## 2. Diagnose Common Issues

### H2 Enum/Type Issues
**Symptom:** `Unknown data type: "ENUM_NAME"` or `Data conversion error`.
**Fix:**
- Open `modules/backend/api/src/test/kotlin/.../support/TestDatabaseFactory.kt`.
- Ensure the custom type is defined for H2, either as an `ENUM` or aliased to `VARCHAR` (more robust).
  ```kotlin
  exec("CREATE DOMAIN IF NOT EXISTS <enum_name> AS VARCHAR")
  ```

### Missing Table
**Symptom:** `Table "TABLE_NAME" not found`.
**Fix:**
- Check `TestDatabaseFactory.kt`'s `SchemaUtils.create(...)` call.
- Ensure the missing table object is included in the list.
- Verify the table object is defined in `CoreTables.kt` or relevant DAL file.

### Constructor/Dependency Mismatch
**Symptom:** Compilation error `No value passed for parameter`.
**Fix:**
- Open the Test class.
- Update the service instantiation to match the current constructor signature.
- Use `mockk` for external dependencies or `SangitaDalImpl` for DAL.

## 3. Apply Fix & Verify

1.  **Apply Code Change:** Modify the test, factory, or service code.
2.  **Rerun Test:**
    ```bash
    ./gradlew :modules:backend:api:test --tests "<TestClassName>"
    ```
3.  **Iterate:** If it fails again, check the new error message. If it passes, run the full suite to ensure no regressions.

## 4. Document (If Deferred)

If a fix is too complex or requires a major architectural shift (e.g., "Switch to Testcontainers"), create a remediation plan doc (e.g., `application_documentation/07-quality/broken-tests-remediation.md`) and log the issue.
