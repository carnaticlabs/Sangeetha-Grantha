| Metadata | Value |
|:---|:---|
| **Status** | Accepted |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-06-12 |
| **Author** | Sangeetha Grantha Team |
| **Supersedes** | [ADR-010](./ADR-010-migration-tool-course-correction.md) |
| **Reverses** | The "Flyway is explicitly NOT used" position of [ADR-003](./ADR-003-database-migrations.md) |
| **Analysis** | [Integration Tests Approach §5](../../07-quality/integration-tests-approach.md) |

# ADR-013: Database Migrations with Flyway — Standardizing the Migration Engine

## Context

This is the third migration-tooling decision in the project's history, and it deliberately reverses the first:

- **ADR-003 (2025-01)** chose a custom Rust CLI (`sangita-cli` + sqlx) and stated "Flyway is explicitly NOT used", citing JVM coupling and CLI-integration preferences.
- **ADR-010 (2026-03)** retired the Rust CLI (toolchain burden, compile friction) in favor of a custom Python `db-migrate` tool + Makefile, keeping the SQL files and the anti-Flyway stance (codified as Critical Rule #1 in `CLAUDE.md`).
- **This ADR (2026-06)** retires the custom code entirely in favor of **Flyway Community Edition**.

Three developments changed the calculus that held in ADR-003 and ADR-010:

1. **The custom approach forked, and the fork drifted.** Production/dev migrations run through Python `db-migrate` (tracking table `schema_migrations`, filename + checksum). Backend integration tests run through a second, independently written Kotlin `MigrationRunner` (tracking table `_sqlx_migrations` — a fossil of the Rust era — version-numbered, **no checksums**). Two implementations of migration semantics had to agree forever; they already disagreed on tracking, validation, and marker parsing (only 21 of 43 migration files carry the `-- migrate:up/down` markers both runners parse, with each runner applying its own fallback rules). Every future capability would have to be built twice.
2. **The Testcontainers decision** (see [Integration Tests Approach](../../07-quality/integration-tests-approach.md)) puts a JVM in every test context and demands programmatic, in-process migration of throwaway databases — for the Kotlin suite *and* the Python worker suite. ADR-003's "avoid JVM coupling for a DB-ops concern" argument no longer matches reality: the JVM is already there, and the engine must now serve **both** language ecosystems from one implementation.
3. **The north-star evaluation** (N2/N3) made the verification gap the project's top systemic risk. A standards-based engine with checksums, `validate`, and a from-scratch-apply CI gate closes a class of drift the custom tools never covered.

Hands-on experimentation with Testcontainers + Flyway (June 2026) confirmed the fit and produced this decision. ADR-010's instinct — fewer moving parts — was right; this ADR completes it by reducing the moving parts to zero custom code.

## Decision

Adopt **Flyway Community Edition** as the **single migration engine** for all environments and all languages:

- **Migration files**: stay in `database/migrations/` as plain SQL, renamed once from `NN__description.sql` to `VNN__description.sql` (Flyway convention); the now-unused `-- migrate:down` sections are stripped.
- **Developer interface unchanged**: `make migrate`, `make migrate-status`, `make db-reset` remain the commands — they invoke the Flyway CLI/Docker image instead of the Python tool.
- **Kotlin integration tests**: the Testcontainers singleton migrates its container via the Flyway **JVM API** (~5 lines), replacing the 157-line `MigrationRunner`.
- **Python worker tests**: testcontainers-python instances are migrated by invoking the same Flyway CLI/container — same files, same engine, same `flyway_schema_history` table.
- **Seed data** is restructured into explicit tiers:
  - *Reference data* (ragas, talas, languages, deities, composer aliases, import-source authority) → Flyway **repeatable migrations** (`R__seed_*.sql`), idempotent `ON CONFLICT` upserts, re-applied automatically when their checksum changes.
  - *Environment data* (admin user/credentials) → out of migrations entirely; provisioning bootstrap.
  - *Dev sample data* → `make seed-dev` / Gradle `seedDatabase` only; never in migrations or CI.
  - *Test fixtures* → Kotlin builders in test code; never SQL dumps.
- **Retired**: `tools/db-migrate` (archived to `tools/db-migrate-archived/`, alongside the Rust CLI) and the Kotlin test-side `MigrationRunner` (deleted).
- **`CLAUDE.md` Critical Rule #1** is amended: the rule's *intent* (one disciplined path for schema changes, via `make` targets) survives; the prohibition on Flyway is replaced by Flyway-as-standard. Liquibase and ad-hoc SQL executors remain prohibited, as does any new custom migration runner.

## Rationale

1. **One engine, four consumers.** Flyway is the only evaluated option serving Kotlin tests (in-process JVM API), Make/dev workflows (CLI/Docker image), Python worker tests (same CLI), and CI (from-scratch apply gate) from a single implementation with no shell-out compromises on the JVM side. The full options analysis — Flyway vs Liquibase vs dbmate vs Atlas vs Alembic vs "status quo unified" — lives in [Integration Tests Approach §5.3](../../07-quality/integration-tests-approach.md).
2. **The duplication ends.** ~360 lines of custom migration code (Python tool + Kotlin runner) and two incompatible tracking tables are replaced by a maintained industry standard and one `flyway_schema_history` table.
3. **Checksums everywhere.** The Python tool's checksum validation — its best feature — currently protects only the dev/prod path. Flyway extends it to every test database and adds `flyway validate` as a CI gate against edited-after-apply migrations.
4. **Repeatable migrations solve seed-data management.** Reference data becomes versioned, checksummed, and identical across dev/test/CI/prod — closing the gap where seed SQL was applied by an unversioned `psql` loop and test databases seeded reference data through separate code paths.
5. **Migration files are the asset, and they survive intact.** A one-shot rename is the entire content change. Lock-in is minimal by construction: plain SQL files remain portable to any engine if Flyway's licensing ever forces another course correction.
6. **Down-migrations lose nothing.** Flyway Community lacks undo — but `db-migrate` never exposed a down command either, and `make db-reset` is and remains the rollback story for local/dev. Versioned canon (north-star N5) is the eventual answer for *data* reversibility.

## Migration Plan

1. One-shot rename script: `NN__desc.sql` → `VNN__desc.sql`; strip `-- migrate:down` sections; commit as a single change with no SQL-content edits.
2. Fresh databases (every Testcontainer, `make db-reset`, CI): Flyway applies all 43 migrations from scratch — no transition state at all.
3. Existing dev databases: `flyway baseline -baselineVersion=43`, then migrate normally; drop `schema_migrations` and `_sqlx_migrations` after a verification window.
4. Swap `MigrationRunner` for the Flyway API in the test substrate (`IntegrationTestBase` / `SangitaPostgres`); convert Makefile targets; archive `tools/db-migrate`.
5. Documentation sync (this ADR, `CLAUDE.md`, `04-database/migrations.md`, onboarding, tech-stack, standards, current-versions) — tracked in the Follow-up checklist below.
6. Rehearse the baseline step against a Testcontainers instance restored from a dev dump before touching any long-lived database.

## Consequences

### Positive

- Zero custom migration code to maintain; one tracking table; one set of semantics.
- Checksum validation and `validate` gate on every path, including tests and CI.
- Reference-data seeding becomes versioned and environment-consistent via `R__` migrations.
- Kotlin and Python test suites provably share one schema source — the cross-language contract (worker ↔ backend) is machine-verified.
- The `make` interface is unchanged; developer muscle memory and scripts survive.

### Negative

- **Redgate licensing watch**: Flyway Community terms have tightened over time (undo, drift reports, old-engine support moved to paid tiers). Mitigation: migrations stay plain SQL; the engine is swappable; `flyway_schema_history` is exportable. Verify Community terms at each major Flyway upgrade.
- One-time rename touches all 43 files (mechanical, scripted, content-free).
- Existing long-lived databases need a supervised baseline step (rehearsed first — see Migration Plan).

### Neutral

- The JVM dependency Flyway brings is moot — the backend toolchain already requires it, and non-JVM consumers use the CLI/Docker image.
- This is the project's second migration-tooling course correction. That is not churn; it is the cost model changing as the system grew (tests, two language ecosystems, CI ambitions) — and each correction preserved the actual asset, the SQL files, untouched. We learn and course-correct over time.

## Follow-up

- ⏳ Amend `CLAUDE.md` Critical Rule #1 (this change accompanies the ADR)
- ⏳ Rename migrations to `V*__*.sql`; strip down-sections
- ⏳ Makefile targets → Flyway CLI/Docker
- ⏳ Replace `MigrationRunner` with Flyway API in test substrate (Integration Tests Approach, Step 2)
- ⏳ Baseline existing dev databases; drop legacy tracking tables after verification
- ⏳ Restructure `database/seed_data/` into the seed tiers (`R__` reference migrations carved out)
- ⏳ Archive `tools/db-migrate` → `tools/db-migrate-archived/`
- ⏳ Documentation sync per the Migration Plan; add Flyway version to `current-versions.md`

## References

- [Integration Tests Approach — §5 Migration & Seed-Data Tooling Re-evaluation](../../07-quality/integration-tests-approach.md) — the full analysis behind this decision
- [North-Star Evaluation](../../north-star-evaluation.md) — findings N2/N3 (verification gap)
- [Database Migrations Documentation](../../04-database/migrations.md)
- [ADR-010 (Superseded)](./ADR-010-migration-tool-course-correction.md) — Python db-migrate + Makefile
- [ADR-003 (Superseded by ADR-010)](./ADR-003-database-migrations.md) — original Rust choice and Flyway rejection
- [Flyway Community documentation](https://documentation.red-gate.com/flyway)
