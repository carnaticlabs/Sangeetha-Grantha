| Metadata | Value |
|:---|:---|
| **Status** | Not Started |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-06-13 |
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
- [ ] `dal/src/test` — scenarios D1–D6 from the approach doc: migrations-from-scratch, table round-trips, UUID v7 generation/ordering, junction-table integrity (`krithi_ragas`), constraint-violation typed errors, AUDIT_LOG write paths.
- [ ] Extract shared test-support into `backend/test-support` now that two modules consume it (D11).

### Phase 2 — CI activation
- [ ] GitHub Actions (`.github/workflows/`): `unit → integrationTest → frontend typecheck+build → worker pytest`; Testcontainers everywhere, no service-container (D7).
- [ ] Migration apply-from-scratch check + Flyway `validate` in CI (also satisfies TRACK-109 Phase-0 item).
- [ ] Gating: **blocking, PR-triggered**, required status check on `main` from the first green run (D8).
- [ ] Enforce time budgets (D9): unit < 30s, integration < 3min per commit; fail loud if exceeded.

## Acceptance Criteria

- A red `integrationTest` blocks merge to `main`.
- DAL has real coverage (D1–D6 green).
- CI green end-to-end on a PR across all layers.

## Docs to Update

- `TRACK-109` (mark W2 partially closed).
- `07-quality/integration-tests-approach.md` (Steps 3–4 done).

## References

- [Integration Tests Approach](../../application_documentation/07-quality/integration-tests-approach.md)
- [Implementation Plan — TRACK-111](../../application_documentation/north-star-production-readiness-implementation-plan.md)
