| Metadata | Value |
|:---|:---|
| **Status** | Not Started |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-06-13 |
| **Author** | Sangeetha Grantha Team |
| **Priority** | P1 |
| **Epic** | [TRACK-109](./TRACK-109-production-readiness-roadmap.md) (W2 / W7) |
| **Decisions** | D5, D12 ([decision log](../../application_documentation/north-star-production-readiness-decision.md)) |
| **Depends on** | TRACK-110, TRACK-111 |
| **Revives** | TRACK-035 (Playwright scaffolding) |

# TRACK-113: Worker + E2E Layer (Step 6)

## Goal

Cross-language worker DB tests + the three E2E money paths, reusing the existing Playwright scaffolding but fitted to the Testcontainers/Flyway substrate. Estimated ~1 week. Revives TRACK-035.

## Implementation Plan

### Phase 1 — Worker DB tests (testcontainers-python)
- [ ] W1: job claim/complete cycle against the real `extraction_jobs` schema.
- [ ] W2: payload written by worker validates against `canonical-extraction-schema.json` **and** ingests via the Kotlin path (shared golden fixture — the cross-language seam where TRACK-096 bugs lived).
- [ ] W3: Gemini stubbed at HTTP (respx/WireMock container) — malformed output → job marked failed with diagnostics, no partial writes.
- [ ] Worker migrations applied via the **same Flyway engine** (CLI/container); confirm Java + Python Testcontainers coexist (approach §5.6).

### Phase 2 — E2E money paths (D12; nightly + pre-release)
- [ ] Reactivate TRACK-035: status Deferred → active; **re-scope its config to the new substrate** (DB verification points at the Testcontainers/Flyway DB, not a hand-started one).
- [ ] Path 1: login → review → approve.
- [ ] Path 2: bulk import (happy path + one failure row).
- [ ] Path 3: krithi edit with raga change reflected in the detail view.
- [ ] Playwright `webServer` drives the compose stack (`make dev` / `start-sangita.sh`).

## Acceptance Criteria

- Worker ↔ backend schema contract machine-verified.
- Three E2E paths green nightly.
- TRACK-035 folded in, not duplicated.

## References

- [TRACK-035](./TRACK-035-frontend-e2e-testing.md)
- [Integration Tests Approach §4.4–§4.5, §5.6](../../application_documentation/07-quality/integration-tests-approach.md)
- [Implementation Plan — TRACK-113](../../application_documentation/north-star-production-readiness-implementation-plan.md)
