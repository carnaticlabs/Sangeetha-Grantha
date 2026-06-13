| Metadata | Value |
|:---|:---|
| **Status** | Not Started |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-06-13 |
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
- [ ] **GATED on your verified `make db-reset` sign-off (D16):** drop legacy `schema_migrations` / `_sqlx_migrations` (neither exists in a fresh DB; this covers any retained DB). First step of Sub-part B.

### Sub-part B — Testcontainers test substrate (not on import critical path)
- [ ] Add Testcontainers BOM + `postgresql` + `junit-jupiter` to `libs.versions.toml`; wire into `dal` and `api` test classpaths.
- [ ] `SangitaPostgres` singleton: `docker.io/library/postgres:18.3-alpine` (fully-qualified name for Podman-readiness, §3.5), migrate via **Flyway JVM API** (replaces the 157-line `MigrationRunner`).
- [ ] `TestDatabase` with `TEST_DATABASE_URL` escape hatch (default Testcontainers; external URL when set).
- [ ] Repoint `IntegrationTestBase` at the new substrate; keep truncate-after-each reset (extend exclusion to `flyway_schema_history` + reference tables).
- [ ] `@Tag("integration")` (D10) + `integrationTest` Gradle task; `make test` runs all via `check`, `make test-integration` for the tagged set.
- [ ] Test-support starts duplicated across api/dal (D11); extraction to `backend/test-support` deferred to TRACK-111.
- [ ] Delete `MigrationRunner`; archive `tools/db-migrate` → `tools/db-migrate-archived/`.
- [ ] Update `modules/backend/CLAUDE.md` (remove phantom `IntegrationTestEnv` conventions; document the real ones).

## Acceptance Criteria

- `./gradlew check` green on a machine with **no** Postgres on 5432 and only Docker present.
- `make db-reset` runs entirely through Flyway; reference data arrives via `R__` repeatables.
- Legacy tracking tables dropped after one verified cycle.
- Freeze lifted (Sub-part A complete).

## Docs to Update

- `04-database/migrations.md` (finalize Flyway sections), `00-onboarding/getting-started.md`, `02-architecture/tech-stack.md`.
- `07-quality/integration-tests-approach.md` (mark Steps 1–2 done).

## References

- [ADR-013](../../application_documentation/02-architecture/decisions/ADR-013-db-migration-with-flyway.md)
- [Integration Tests Approach](../../application_documentation/07-quality/integration-tests-approach.md)
- [Implementation Plan — TRACK-110](../../application_documentation/north-star-production-readiness-implementation-plan.md)
