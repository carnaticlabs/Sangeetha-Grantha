---
name: verify-db-status
description: Verifies that the local PostgreSQL database schema is in sync with Flyway migration files (ADR-013), checks migration history in flyway_schema_history, and executes migrations or resets. Use after pulling changes, editing migrations, or when debugging schema discrepancies.
---

# Verify Database Status

This skill validates that the local PostgreSQL database schema is aligned with Flyway migration files in `database/migrations/` per ADR-013.

> [!IMPORTANT]
> Sangita Grantha uses **Flyway** exclusively for migrations via `make migrate` and `make db-reset`. The legacy Rust CLI (`sangita-cli`) is archived and the Python migration runner is superseded.

---

## 1. Inspect Migration Files

Check the latest migration files in the repository:

```bash
ls -1 database/migrations/V*.sql | tail -n 5
```

---

## 2. Apply Pending Migrations

Run Flyway migrations via the Makefile target:

```bash
make migrate
```

---

## 3. Query Database Migration History

Confirm that the latest migration recorded in PostgreSQL matches the latest migration script on disk:

```sql
SELECT installed_rank, version, description, type, installed_on, execution_time, success
FROM flyway_schema_history
ORDER BY installed_rank DESC
LIMIT 5;
```

---

## 4. Clean Database Reset (If Required)

If local schema state is corrupted or out of sync with migration history, perform a clean reset (drops, recreates, runs all Flyway migrations from scratch, and seeds foundational entities):

```bash
make db-reset
```
