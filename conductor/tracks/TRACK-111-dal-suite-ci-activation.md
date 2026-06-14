| Metadata | Value |
|:---|:---|
| **Status** | Completed |
| **Version** | 2.0.0 |
| **Last Updated** | 2026-06-14 |
| **Author** | Sangeetha Grantha Team |
| **Priority** | P0 — converts every test into a gate |
| **Epic** | [TRACK-109](./TRACK-109-production-readiness-roadmap.md) (W2 CI/CD) |
| **Decisions** | D7, D8, D9, D11 ([decision log](../../application_documentation/north-star-production-readiness-decision.md)) |
| **Depends on** | TRACK-110 (substrate), TRACK-115 (clean root) |
| **Blocks** | TRACK-118 |

# TRACK-111: DAL Test Suite + CI Activation (Steps 3–4)

## Goal

Stand up the missing DAL tests (north-star: 0 DAL tests today) and turn on GitHub Actions so the suite gates merges. Estimated ~3–4 days.

## Context

North-star N3: the DAL has zero tests; Exposed table definitions, repository queries, and the 43-migration schema are validated only incidentally. N2: no CI means no existing test gates anything. This track closes both, building on the TRACK-110 Testcontainers substrate.

## Implementation Plan

### Phase 1 — DAL suite
- [x] `dal/src/test` — scenarios D1–D6 (`modules/backend/dal/src/test/.../integration/`): `MigrationsFromScratchTest`, `TableRoundTripTest`, `UuidV7Test`, `KrithiRagasJunctionTest`, `ConstraintViolationTest`, `AuditLogTest`. 11 tests, green. DAL went 0 → 11.
- [x] Extract shared test-support into `:modules:backend:test-support` (D11) — `SangitaPostgres`, `TestDatabase`, `IntegrationTestBase`, `TestFixtures` now compiled as `src/main` and consumed by both api + dal test classpaths (package `com.sangita.grantha.backend.testsupport`).
- [x] D5 typed-error layer (per decision): `dal/errors/DalErrors.kt` (`DuplicateKeyException` 23505 / `ForeignKeyViolationException` 23503), mapped centrally in `DatabaseFactory.dbQuery` — zero per-repo edits.

### Phase 2 — CI activation
- [x] GitHub Actions (`.github/workflows/ci.yml`): `backend-unit → backend-integration → migrations → frontend typecheck+build → worker pytest`; Testcontainers on `ubuntu-latest`, no service-container for the JVM suite (D7).
- [x] Migration apply-from-scratch + Flyway `validate` job against an ephemeral Postgres (also satisfies TRACK-109 Phase-0 item); D1 additionally guards it in-suite.
- [~] Gating: **blocking, PR-triggered** checks defined. Marking the checks **required** on `main` is a one-time GitHub branch-protection setting (cannot be set from repo code) — see note below.
- [x] Time budgets (D9): added a Docker-free `unitTest` task (<30s, ~7s locally) and per-job `timeout-minutes`; integration capped at 15 min.

## Acceptance Criteria

- [x] A red `integrationTest` blocks merge to `main` (defined as a PR check; enable branch protection to enforce).
- [x] DAL has real coverage (D1–D6 green; 11 tests).
- [x] CI defined end-to-end on a PR across all layers (backend/migrations/frontend/worker).

## Manual follow-up (not code)

- In **GitHub → Settings → Branches → `main`**, mark `backend-unit`, `backend-integration`, `migrations`, `frontend`, `worker` as **required status checks** so the gate is enforced (D8). The workflow defines the checks; only branch protection can require them.

## Docs to Update

- [x] `TRACK-109` (mark W2 partially closed).
- [x] `07-quality/integration-tests-approach.md` (Steps 3–4 done).

## References

- [Integration Tests Approach](../../application_documentation/07-quality/integration-tests-approach.md)
- [Implementation Plan — TRACK-111](../../application_documentation/north-star-production-readiness-implementation-plan.md)
