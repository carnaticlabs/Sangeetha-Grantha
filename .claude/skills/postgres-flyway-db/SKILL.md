---
name: postgres-flyway-db
description: PostgreSQL 18 and Flyway migration conventions. Use when creating or editing files in database/migrations/, changing schema or seed reference data, resetting the dev database, aligning Exposed table definitions with SQL, or debugging migration and seeding issues.
---

# PostgreSQL 18 + Flyway (database/migrations/)

Flyway Community is the **only** migration engine ([ADR-013](../../../application_documentation/02-architecture/decisions/ADR-013-db-migration-with-flyway.md)) — never Liquibase, ad-hoc DDL against a live database, or custom runners. Schema reference: [schema.md](../../../application_documentation/04-database/schema.md).

## Commands

- `make migrate` — apply pending migrations · `make migrate-status` — Flyway info
- `make db-reset` — drop → create → migrate from scratch (V__ schema + R__ reference data). Always test a new migration this way, not just incrementally.
- `make seed-dev` — dev-only sample content (reference data ships via R__ repeatables, not this)

## File naming (match the existing files exactly)

- **Versioned**: `VNN__snake_or-kebab-description.sql`, two-digit zero-padded sequence (`V01__…` … currently `V47__…`). Take the next free number. Committed versioned migrations are immutable — fix mistakes with a new migration, never by editing an applied one.
- **Repeatable**: `R__seed_NN_description.sql` (e.g. `R__seed_04_raga_reference.sql`) for reference data — ragas, talas, composer aliases, import-source authority. They re-run whenever their checksum changes, so they must be idempotent (`INSERT … ON CONFLICT DO UPDATE`). New reference data goes here, not in a V__ file.

## PostgreSQL 18 conventions

- UUID primary keys default to `uuidv7()` (time-ordered; switched from v4 in `V37__pg18_uuidv7_defaults.sql`) — use it for new tables.
- JSONB for unstructured metadata; explicit enum types are created in migrations and must be extended via `ALTER TYPE … ADD VALUE` migrations.

## After any schema or seed change

1. Update the matching Exposed `Table` objects in `modules/backend/dal/.../tables/` — schema and DAL must align exactly (integration tests migrate real containers and will catch drift).
2. Verify junction tables are populated, not just FK columns on the main entity (e.g. `krithi_ragas`) — the recurring seeding bug in this repo.
3. Confirm the data flows DB → API → UI, and remember seed/import content is subject to the lakshana rules in [Domain Model §6](../../../application_documentation/01-requirements/domain-model.md#6-musicological-correctness-rules-lakshana).
