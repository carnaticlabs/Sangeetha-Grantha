---
name: postgres-flyway-db
description: Guidelines for PostgreSQL 18+ database management and Flyway migrations (ADR-013).
---

# PostgreSQL 18+ and Flyway Migration Guidelines

This skill enforces database configuration and schema migration standards in the repository.

## 1. Migration Policy: Flyway Only (ADR-013)
Flyway Community is the single, canonical database migration engine for the project:
- **Never** run ad-hoc DDL queries directly on dev or production databases.
- **Never** use Liquibase, custom Python scripts, or custom Kotlin runner scripts.
- **Workflow via Makefile**:
  - `make migrate`: Runs any pending migrations.
  - `make db-reset`: Clears the database completely (drops and recreates the schema) and re-runs all Flyway migrations from scratch. Always use this to test schema changes from a clean state.

## 2. Migration File Formats & Naming
Flyway migration scripts reside in `database/migrations/`:
- **Versioned Migrations (`VNN__{description}.sql`)**:
  - Used for schema alterations (creating tables, adding columns, renaming fields).
  - Sequence is **two-digit zero-padded** — `V01__baseline-schema-and-types.sql` through the current `V47__…`. Take the next free number; match the existing files exactly (three-digit `V001__` is **not** this repo's convention).
  - Once committed, versioned migrations are **immutable**. Never edit an existing versioned migration file; write a new one to apply changes.
- **Repeatable Migrations (`R__seed_NN_{description}.sql`)**:
  - Two underscores after the `R`, then the `seed_NN_` prefix — e.g. `R__seed_04_raga_reference.sql`. Used for reference data (ragas, talas, composer aliases, import-source authority).
  - Repeatable migrations do not have version numbers and re-run whenever their file checksum changes.
  - Seed files must therefore be idempotent (`INSERT ... ON CONFLICT DO UPDATE`). New reference data goes here, not in a `V__` file.

## 3. Exposed Schema Alignment
- When adding tables/columns in PostgreSQL migrations, the corresponding `Table` classes in Kotlin (Exposed DAL) must be updated to align perfectly.
- Ensure junction tables (e.g. `krithi_ragas`) are fully populated when seeding data or running imports.
- Use PostgreSQL 18+ optimized datatypes:
  - UUID primary keys default to `uuidv7()` (time-ordered; switched from v4 in `V37__pg18_uuidv7_defaults.sql`) — use it for new tables.
  - Prefer modern JSONB types for unstructured config/metadata attributes.
  - Explicit enum types are created in migrations and must be extended via `ALTER TYPE … ADD VALUE` migrations.
  - pgvector is **not** part of this stack — don't reach for it. Search today is Postgres-native (see `V18__krithi-search-indexes.sql`).

## 4. Integrity & Deployability
- Integrity belongs in the database, not the application: declare foreign keys, `NOT NULL`, `UNIQUE`, and `CHECK` constraints in the migration rather than relying on Exposed or the service layer to enforce them.
- Prefer additive, separately-deployable changes — add a nullable column and backfill, rather than one migration that adds a `NOT NULL` column and rewrites a large table under a single lock.
- This project does **not** use soft deletes; there is no `deleted_at`/`is_deleted` convention. Auditability comes from the `AUDIT_LOG` table, not from tombstoned rows.
- After any schema or seed change, confirm the data flows DB → API → UI, and remember seed/import content is subject to the lakshana rules in [Domain Model §6](../../../application_documentation/01-requirements/domain-model.md#6-musicological-correctness-rules-lakshana).
