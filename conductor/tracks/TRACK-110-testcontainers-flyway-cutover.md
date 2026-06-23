| Metadata | Value |
|:---|:---|
| **Status** | Completed |
| **Version** | 2.0.0 |
| **Last Updated** | 2026-06-14 |
| **Author** | Sangeetha Grantha Team |
| **Priority** | P0 — foundation for the whole initiative |
| **Epic** | [TRACK-109](./TRACK-109-production-readiness-roadmap.md) (W2 CI/CD) |
| **Decisions** | D2, D7, D10, D11, D14, D15, D16 ([decision log](../../application_documentation/north-star-production-readiness-decision.md)) |
| **Depends on** | TRACK-114 (admin bootstrap hash) |
| **Blocks** | TRACK-111, TRACK-112, TRACK-113, TRACK-117 |

# TRACK-110: Testcontainers Substrate + Flyway Cutover (Steps 1–2)

## Goal

Make integration tests self-provisioning via **Testcontainers**, and consolidate the two diverged migration runners (Python `db-migrate` + Kotlin `MigrationRunner`) onto **Flyway** per [ADR-013](../../application_documentation/02-architecture/decisions/ADR-013-db-migration-with-flyway.md). Estimated ~3–4 days.

Two sub-parts: **Sub-part A (Flyway cutover) is on the import critical path** — the D2 freeze cannot lift until it lands. **Sub-part B (test substrate) is not** and runs after the import resumes.

## Context

See [Integration Tests Approach](../../application_documentation/07-quality/integration-tests-approach.md) §1–§5. The current harness hard-codes `localhost:5432`, pins no Postgres version, shares one mutable DB, and runs two incompatible migration implementations (`schema_migrations` vs `_sqlx_migrations`). Testcontainers + Flyway fixes all of this and makes CI (TRACK-111) a one-day task.

## Implementation Plan

### Sub-part A — Flyway cutover (critical path, during the D2 freeze)
- [x] Scripted rename `NN__desc.sql` → `VNN__desc.sql` for all 43 files; strip `-- migrate:down` sections. **Split into two commits** (per review): a content-free rename + comment-only-down strip, then a behavioural fix removing *live* down-SQL from 5 files (V14/V15/V33/V36/V37) — these silently reverted their up-migrations under the old whole-file Python executor (most critically V37, which reset all `uuidv7()` defaults to `gen_random_uuid()`). Verified: post-reset id defaults are `uuidv7()`.
- [x] Wire Flyway **Docker image** (`flyway/flyway:12.8.1-alpine`, pinned in `compose.yaml` + `gradle/libs.versions.toml`) into `make migrate`, `make migrate-status` (→ `flyway info`), `make db-reset` (D14).
- [x] Seed tiering (D15): admin carved out of `R__seed_01_reference.sql`; `01`(−admin)/`03`/`04`/`05` → `R__seed_0N_*.sql` repeatables (alphabetical = FK order); `02_sample_data.sql` → `make seed-dev`. Verified: reference data populates via `flyway migrate` (ragas=972, aliases=5, …), `users=0` pre-bootstrap.
- [x] Admin-user bootstrap: `tools/BootstrapAdmin.kt` + `bootstrapAdmin` Gradle task + `make bootstrap-admin` + `bootstrap` compose service; argon2id via `PasswordHasher` (TRACK-114). Verified: idempotent, hash is `$argon2id$…` (len 161), role bound, secret never logged.
- [x] Baseline rehearsal: **no populated dev DB exists** (both local pgdata volumes held only an empty default cluster — audited), so there is nothing to baseline/reconcile locally; the from-scratch `flyway migrate` is the real path. Baseline procedure (`flyway baseline -baselineVersion=43`, rehearse on a dump-restored Testcontainers instance) documented in `04-database/migrations.md` §5 for any real long-lived DB.
- [x] *(bridge)* `MigrationRunner` patched to tolerate `VNN__` / skip `R__` so the existing integration suite stays green during the A→B window (deleted outright in Sub-part B). Verified: `ImportRoutesTest` passes.
- [x] **D16 (verified cycle signed off):** legacy `schema_migrations` / `_sqlx_migrations` confirmed absent from the verified Flyway DB — nothing to drop; creators removed (MigrationRunner deleted, db-migrate archived). Done in Sub-part B.

### Sub-part B — Testcontainers test substrate (not on import critical path)
- [x] Added `testcontainers` (1.21.4) to `libs.versions.toml` with `org.testcontainers:postgresql` + Flyway JVM API libs (`flyway-core`/`flyway-database-postgresql` 12.8.1); wired into `api` test classpath. (No BOM/`junit-jupiter` module needed — the singleton needs no annotations; JUnit 5 comes via kotlin-test-junit5. `dal` has no tests yet, so deps land there when it gains integration coverage — folds into TRACK-111.)
- [x] `SangitaPostgres` singleton: `docker.io/library/postgres:18.3-alpine` (fully-qualified for Podman-readiness), migrated via **Flyway JVM API** (replaces the 156-line `MigrationRunner`).
- [x] `TestDatabase` with `TEST_DATABASE_URL` escape hatch (default Testcontainers; external URL when set). **Full `V__`+`R__`** migrate — tests run against the real reference seed; `TestFixtures` consume it via `findOrCreate`.
- [x] Repointed `IntegrationTestBase` at the new substrate; truncate-after-each retained, exclusion = **`flyway_schema_history` + reference tables** (`PRESERVED_TABLES`: roles/composers/ragas/talas/deities/composer_aliases/import_sources) so the seed stays stable. *(Upgraded from the interim schema-only approach.)*
- [x] `@Tag("integration")` (D10) + `integrationTest` Gradle task; `make test` / `./gradlew check` run all, `make test-integration` runs the tagged set.
- [x] Test-support stayed in `api` for TRACK-110 (D11); extraction to `:modules:backend:test-support` + `dal` integration coverage **delivered in [TRACK-111](./TRACK-111-dal-suite-ci-activation.md)**.
- [x] Deleted `MigrationRunner` (+ dead `TestDatabaseFactory`); archived `tools/db-migrate` → `tools/db-migrate-archived/`.
- [x] Updated `modules/backend/CLAUDE.md` (real Testcontainers/Flyway conventions; removed phantom `IntegrationTestEnv`).
- [x] B1 (D16): legacy `schema_migrations` / `_sqlx_migrations` confirmed absent from the verified Flyway DB (nothing to drop); creators removed.

## Acceptance Criteria

- [x] `./gradlew check` green on a machine with **no** Postgres on 5432 and only Docker present — integration tests self-provision via Testcontainers (random host port; the old `localhost:5432` dependency is gone).
- [x] `make db-reset` runs entirely through Flyway; reference data arrives via `R__` repeatables.
- [x] Legacy tracking tables confirmed absent after the verified cycle (none to drop).
- [x] Freeze lifted (Sub-part A complete and signed off).

## Docs to Update

- `04-database/migrations.md` (finalize Flyway sections), `00-onboarding/getting-started.md`, `02-architecture/tech-stack.md`.
- `07-quality/integration-tests-approach.md` (mark Steps 1–2 done).

## References

- [ADR-013](../../application_documentation/02-architecture/decisions/ADR-013-db-migration-with-flyway.md)
- [Integration Tests Approach](../../application_documentation/07-quality/integration-tests-approach.md)
- [Implementation Plan — TRACK-110](../../application_documentation/north-star-production-readiness-implementation-plan.md)
