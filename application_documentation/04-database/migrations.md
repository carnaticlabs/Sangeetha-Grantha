| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 3.5.0 |
| **Last Updated** | 2026-09-10 |
| **Author** | Sangeetha Grantha Team |
| **Document Type** | Current guide |

# Flyway migrations and reference data

---

**Flyway Community is the only migration engine.** Make/Compose uses the Flyway container; Kotlin integration tests use its JVM API. Both apply the same SQL directory. [ADR-013](../02-architecture/decisions/ADR-013-db-migration-with-flyway.md) supersedes the retired Rust CLI and custom Python/Kotlin runners.

## 1. File conventions

- `V<version>__description.sql` — ordered versioned migrations. Applied files are immutable; subsequent changes need a new version.
- `R__seed_<order>_<description>.sql` — repeatable reference seeds. Flyway reapplies a repeatable when its checksum changes; the numeric description prefix encodes seed dependency order.

Files live in [database/migrations](../../database/migrations). Flyway executes the whole file; old `migrate:up`/`migrate:down` marker conventions are not used. SQL examples in ADRs are design sketches unless linked to an actual migration.

## 2. Current schema landmarks

At this documentation review, the directory contains `V01`–`V60` and six repeatable reference seeds. Inspect the directory before assigning a new number.

| Migration | Responsibility |
|:---|:---|
| V01–V06 | Core enums, reference/composition tables, sections, tags, notation and initial import schema |
| V23–V27 | Source authority, evidence, structural voting, extraction integration |
| V37 | PostgreSQL UUIDv7 defaults |
| [V44](../../database/migrations/V44__versioned_canon.sql) | `source_documents`, `krithi_revisions`, `krithi_section_revisions` and provenance |
| V50–V57 | Raga cleanup, match keys, aliases, identity/resolution and standing checks |
| [V58](../../database/migrations/V58__semantic_search_pgvector.sql) | pgvector search documents, profiles and embeddings |
| [V59](../../database/migrations/V59__musical_form_unestablished.sql) | `UNESTABLISHED` musical-form enum value |
| [V60](../../database/migrations/V60__musical_form_default_unestablished.sql) | Default unclassified form for new compositions |

Earlier TRACK-133 corpus-fix files also used numbers V58–V62 and were retired in TRACK-139. They are different files from the current V58–V60 schema migrations. Identify history by full filename/description/checksum before making any recovery decision.

## 3. Migration workflow

```bash
make db
make migrate-status
make migrate
```

`make migrate` targets the local Compose `migrate` service with its configured connection. It applies pending versioned migrations and changed repeatables. Assigning a worker-style `DATABASE_URL` to the shell does not redirect that service to a different database.

For a new schema change, inspect the highest version, create the next `V__` file, and verify both a clean migration and an upgrade from a representative prior state in an isolated database. Run appropriate integration checks and update affected schema/domain/API documentation. Do not use a developer's populated database as a disposable test fixture.

For reference-data changes, update the relevant repeatable and verify that it remains safe to reapply. Corpus content corrections use parser/extraction/reingestion/curator workflows and retain revision attribution. Older grandfathered corpus cleanup migrations are historical exceptions, not templates for new work.

## 4. Seed-data tiers

| Data | Location / mechanism | Purpose |
|:---|:---|:---|
| Reference data | `R__seed_01` through `R__seed_06` | Roles, reference entities, aliases, source authority and raga reconciliation |
| Environment account | `make bootstrap-admin` | Admin identity, argon2id password hash and role assignment |
| Development samples | `make seed-dev` | Optional local sample compositions |
| Test fixtures | Test-support builders and per-layer fixtures | Deterministic test data |
| Canonical corpus | Import/reingest/curator service paths | Source-backed composition content and history |

`make db-reset` drops and recreates the local database, then applies Flyway schema and reference data. It does not restore the corpus or automatically provision the environment's admin. It is destructive and is unnecessary for routine pending migrations.

## 5. Rollback & history tracking

Flyway records description, version, checksum, and application state in `flyway_schema_history`. Community does not provide undo migrations. Prefer a compatible forward fix or an independently rehearsed restore; a local database reset is only appropriate for disposable data.

For a database from a retired migration tool or the retired corpus-fix sequence:

1. Preserve a backup and inspect its schema/history without modifying it.
2. Compare full migration identities with the current SQL directory.
3. Rehearse the exact adoption/reconciliation on an isolated restored database.
4. Validate schema, reference data, revisions, and public reads before applying the reviewed procedure to the real target.

Do not copy old examples that baseline at a fixed version or delete history rows by version number alone. In particular, deleting rows 58/59/60 today may remove legitimate semantic-search and musical-form schema history. The TRACK-139 report describes a specific historical retirement, not a general-purpose Flyway repair command.

For old dumps, generated `raga_match_key` expressions may depend on function search-path resolution. Investigate the restore error against the actual schema and rehearse any function/schema adjustment on the isolated restore. Do not disable triggers or edit migration history as a routine first response.

See [database runbook](../08-operations/runbooks/database-runbook.md) and [TRACK-139 evidence](../10-implementations/track-139-retire-corpus-data-fix-migrations.md).

## 6. Engine and validation

Compose sets the migration location, naming validation, and disables automatic baseline-on-migrate. The migration version is pinned in [Current Versions](../00-meta/current-versions.md), [Compose](../../compose.yaml), and the [Gradle catalog](../../gradle/libs.versions.toml).

CI checks a from-scratch migrate/validate and standing raga rules. Testcontainers applies the same versioned/repeatable set for database tests. Review [integration testing](../07-quality/integration-tests-approach.md) for test isolation and [schema](./schema.md) for data relationships.

---

[Section index](./README.md) · [Documentation home](./../README.md) · [Feature status](./../01-requirements/features/README.md)
