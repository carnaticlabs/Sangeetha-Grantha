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
- **Versioned Migrations (`V{Version}__{Description}.sql`)**:
  - Used for schema alterations (creating tables, adding columns, renaming fields).
  - Version numbers must use an incrementing sequence (e.g., `V001__init.sql`, `V002__add_audit_log.sql`).
  - Once committed, versioned migrations are **immutable**. Never edit an existing versioned migration file; write a new one to apply changes.
- **Repeatable Migrations (`R___{Description}.sql`)**:
  - Used for seed data, static reference tables (e.g., Ragas, Talas), and stored procedures.
  - Repeatable migrations do not have version numbers and run whenever their file checksum changes.
  - Seed files must be idempotent (use `INSERT ... ON CONFLICT DO UPDATE` or similar).

## 3. Exposed Schema Alignment
- When adding tables/columns in PostgreSQL migrations, the corresponding `Table` classes in Kotlin (Exposed DAL) must be updated to align perfectly.
- Ensure junction tables (e.g. `krithi_ragas`) are fully populated when seeding data or running imports.
- Use PostgreSQL 18+ optimized datatypes:
  - Always use standard UUID columns (`UUID` type) for primary keys.
  - Prefer modern JSONB types for unstructured config/metadata attributes.
  - Leverage pgvector extensions for semantic search embeddings when applicable.
