| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 3.0.1 |
| **Last Updated** | 2026-07-09 |
| **Author** | Sangeetha Grantha Team |

# Database Migrations (Sangita Grantha)

- [Config](../08-operations/config.md)
- [ADR-013 — Migrations with Flyway](../02-architecture/decisions/ADR-013-db-migration-with-flyway.md)

# Database Migrations

Sangita Grantha uses **Flyway Community Edition** (`12.9.0`) as its single migration engine, orchestrated via **Makefile** commands (`make migrate` / `make db-reset`) — see [ADR-013](../02-architecture/decisions/ADR-013-db-migration-with-flyway.md) for the decision and rationale. Flyway replaces the Python `db-migrate` tool (ADR-010 era) and the Kotlin test-side `MigrationRunner`, which had diverged into two incompatible implementations. One engine now serves dev, prod, Kotlin Testcontainers suites, Python worker tests, and CI, with a single `flyway_schema_history` tracking table.

> The cutover (TRACK-110) renamed every `NN__description.sql` to `VNN__description.sql`, removed the unused `-- migrate:up/down` markers (Flyway runs the whole file; Community has no undo), and moved reference seed data into `R__` repeatable migrations.

---

## 1. Conventions

### File naming

All migrations live in `database/migrations/` (the single Flyway `locations` path):

- **Versioned** — `VNN__description.sql` (e.g. `V01__baseline-schema-and-types.sql`). Applied once, in version order, recorded in `flyway_schema_history` with a checksum. Never edited after being applied.
- **Repeatable** — `R__seed_<NN>_<name>.sql` (e.g. `R__seed_01_reference.sql`). Re-applied automatically whenever the file's checksum changes, **after** all versioned migrations. Flyway orders repeatables alphabetically by description, so the numeric infix (`01`, `02`, …) encodes FK dependency order.

There are no `-- migrate:up` / `-- migrate:down` markers. Flyway executes the entire file; Flyway Community has no `undo`, so down-sections are not used (see Rollback, §5).

### Migration structure

```sql
SET search_path TO public;

-- Migration SQL here. Prefer IF NOT EXISTS / ON CONFLICT for idempotence.
CREATE TABLE IF NOT EXISTS new_table (...);
```

---

## 2. Migration files

43 versioned migrations (`V01`–`V43`) plus 4 repeatable seed migrations. Foundational set:

| File | Purpose | Key Entities |
|------|---------|--------------|
| [`V01__baseline-schema-and-types.sql`](../../database/migrations/V01__baseline-schema-and-types.sql) | Extensions, enum types, foundational tables | `roles`, `audit_log`, enums (workflow_state, language_code, script_code, raga_section, import_status, musical_form) |
| [`V02__domain-tables.sql`](../../database/migrations/V02__domain-tables.sql) | Primary domain tables | `users`, `composers`, `ragas`, `talas`, `deities`, `temples`, `krithis`, `krithi_ragas`, `krithi_lyric_variants` |
| [`V03__constraints-and-indexes.sql`](../../database/migrations/V03__constraints-and-indexes.sql) | Constraints, indexes, search optimization | Search/trigram indexes, foreign-key constraints |
| [`V04__import-pipeline.sql`](../../database/migrations/V04__import-pipeline.sql) | Data ingestion tables | `import_sources`, `imported_krithis` |
| [`V05__sections-tags-sampradaya-temple-names.sql`](../../database/migrations/V05__sections-tags-sampradaya-temple-names.sql) | Sections, tags, sampradaya, temple names | `krithi_sections`, `krithi_lyric_sections`, `tags`, `krithi_tags`, `sampradayas`, `temple_names` |
| [`V06__notation-tables.sql`](../../database/migrations/V06__notation-tables.sql) | Notation support for Varnams/Swarajathis | `krithi_notation_variants`, `krithi_notation_rows` |
| `V37__pg18_uuidv7_defaults.sql` | Switch UUID PK defaults to `uuidv7()` (PG18, [ADR-011](../02-architecture/decisions/ADR-011-postgresql-18-uuid-v7.md)) | all UUID-keyed tables |

### Repeatable seed migrations (reference data)

| File | Seeds |
|------|-------|
| `R__seed_01_reference.sql` | roles, base composers / ragas / talas / deities, the unmatched-PDF import source |
| `R__seed_02_composer_aliases.sql` | composer name aliases (FK → composers) |
| `R__seed_03_import_sources_authority.sql` | import-source authority tiers & metadata |
| `R__seed_04_raga_reference.sql` | comprehensive raga reference (~972 ragas) |

### Enum types

Defined in `V01__baseline-schema-and-types.sql`:

- `workflow_state_enum`: `draft`, `in_review`, `published`, `archived`
- `language_code_enum`: `sa`, `ta`, `te`, `kn`, `ml`, `hi`, `en`
- `script_code_enum`: `devanagari`, `tamil`, `telugu`, `kannada`, `malayalam`, `latin`
- `raga_section_enum`: `pallavi`, `anupallavi`, `charanam`, `other`
- `import_status_enum`: `pending`, `in_review`, `mapped`, `rejected`, `discarded`
- `musical_form_enum`: `KRITHI`, `VARNAM`, `SWARAJATHI` *(added in `V02`)*

---

## 3. Migration workflow

### Makefile commands

The Makefile drives the `flyway/flyway:12.9.0-alpine` image (the compose `migrate` service):

```bash
make migrate         # flyway migrate — applies pending V__ + re-applies changed R__
make migrate-status  # flyway info
make db-reset        # drop → create the database, then flyway migrate (schema + reference data)
make seed-dev        # dev-only sample content (database/seed_data/02_sample_data.sql)
make bootstrap-admin # provision/update the admin user (argon2id); needs ADMIN_EMAIL / ADMIN_PASSWORD
```

`make db-reset` applies reference data automatically (via the `R__` repeatables) — it no longer needs a separate seed step. Dev sample data and the admin user are deliberately *not* migrations (see §4).

### Creating a new migration

1. Create `database/migrations/V<next>__description.sql` (next sequential version, e.g. `V44__...`).
2. Write idempotent SQL (`IF NOT EXISTS`, `ON CONFLICT`); no `-- migrate:down` section.
3. Test: `make db-reset` (full from-scratch apply) and `make migrate` (incremental).
4. Update this file and `domain-model.md` / schema docs if entities change.

For reference-data changes, edit the relevant `R__seed_*.sql` instead — Flyway re-applies it on the next `migrate` because its checksum changed.

### Best practices

- ✅ Idempotent DDL/DML (`IF NOT EXISTS`, `ON CONFLICT … DO NOTHING`).
- ✅ Add indexes after the table exists (same or later migration).
- ❌ **Never edit a versioned migration after it has been applied** — Flyway's checksum validation (`flyway validate`, a CI gate) rejects it. Write a new `V__` instead.
- ❌ **Never bypass the Makefile / Flyway** — no Liquibase, ad-hoc SQL executors, or custom runners ([ADR-013](../02-architecture/decisions/ADR-013-db-migration-with-flyway.md)).

### Ordering

Versioned migrations apply in version order; repeatables apply afterwards in description (alphabetical) order. Dependency highlights: `V01` (enums/roles) → `V02` (domain tables) → `V03/04/05/06` (constraints, import, sections, notation). Repeatable seeds depend on the schema and on each other in `R__seed_01 → 02 → 03 → 04` order.

---

## 4. Seed-data tiers (ADR-013, D15)

| Tier | Where | Applied by |
|------|-------|-----------|
| **Reference data** (ragas, composers, aliases, import-source authority, roles) | `database/migrations/R__seed_*.sql` | `flyway migrate` (checksummed, idempotent, environment-consistent) |
| **Environment data** (admin user + credentials) | `tools/BootstrapAdmin.kt` | `make bootstrap-admin` — argon2id hash via `PasswordHasher` (TRACK-114); never in SQL |
| **Dev sample data** | `database/seed_data/02_sample_data.sql` | `make seed-dev` only — never a migration, never CI |
| **Test fixtures** | Kotlin builders (`TestFixtures.kt`) | test code — never SQL dumps |

---

## 5. Rollback & history tracking

- Flyway records every applied migration in **`flyway_schema_history`** (version, description, checksum, success).
- Flyway Community has **no `undo`**. The local rollback story is `make db-reset` (drop → create → re-apply). Data reversibility is the domain of versioned canon (north-star N5, [ADR-014](../02-architecture/decisions/ADR-014-versioned-canon.md)).
- **Existing long-lived databases** (migrated by the retired tooling) are adopted with `flyway baseline -baselineVersion=43`, then migrated normally. Rehearse the baseline against a Testcontainers instance restored from a dump **before** touching any real database (ADR-013 Migration Plan §6).

---

## 6. Engine details

- Image: `flyway/flyway:12.9.0-alpine`; JVM API (`org.flywaydb:flyway-core` + `flyway-database-postgresql`) for the Kotlin Testcontainers suite (TRACK-110 Sub-part B).
- Single `locations`: `filesystem:/flyway/sql` → `database/migrations/` (both `V__` and `R__`).
- `validateMigrationNaming` on; `baselineOnMigrate` off.
- Version pinned in `gradle/libs.versions.toml` (`flyway`), `compose.yaml`, and [current-versions.md](../00-meta/current-versions.md).
