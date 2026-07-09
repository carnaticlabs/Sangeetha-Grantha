| Metadata | Value |
|:---|:---|
| **Status** | In Progress (Phase 1 done) |
| **Version** | 1.1.0 |
| **Last Updated** | 2026-07-09 |
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

### Phase 1 — Worker DB tests (testcontainers-python) ✅ 2026-07-09
- [x] W1: job claim/complete cycle against the real `extraction_queue` schema (V27+V31) —
      9 tests in `tests/integration/test_w1_queue_contract.py`: claim transitions + attempt
      accounting, empty-queue, max-attempts exhaustion, created_at ordering, mark_done/mark_failed
      persistence, FOR UPDATE SKIP LOCKED two-worker concurrency, queue stats, Devanagari jsonb
      round-trip. **Found + fixed a real off-by-one**: `claim_pending_task` returned the
      pre-increment `attempts`, so failure diagnostics reported `attempt: 0` on the first try.
- [x] W2: shared golden fixture `shared/domain/model/import/fixtures/canonical-extraction-golden.json`
      verified on both sides — Python (`test_w2_canonical_contract.py`, 5 tests): jsonschema
      validation, lossless pydantic round-trip (pins camelCase aliases + the four snake_case
      `*_normalized` keys), mark_done→jsonb→read-back identity; Kotlin
      (`CanonicalExtractionGoldenFixtureTest`, 5 tests, unit slice): STRICT decode
      (`ignoreUnknownKeys = false`) into `CanonicalExtractionDto` + round-trip. Any field either
      side adds/renames now breaks one of the two suites.
- [x] W3: Gemini stubbed at HTTP via respx intercepting the google-genai SDK's httpx transport
      (`test_w3_gemini_arms_length.py`, 4 tests). **Contract deviation from the original wording**:
      malformed Gemini output does NOT fail the job — the implemented (and stricter arm's-length)
      contract degrades enrichment to `applied=false` + `gemini_error:*` warnings with the
      canonical metadata untouched; a failing SOURCE fetch is what fails the job (FAILED +
      structured diagnostics, result side empty). Both behaviors pinned, plus a healthy-source
      DONE companion through the full `_process_task` pipeline.
- [x] Worker migrations applied via the **same Flyway engine** — `tests/integration/conftest.py`
      runs `flyway/flyway:12.9.0-alpine` (CLI container) against the testcontainers-python
      PostgreSQL 18.3, full `V__`+`R__` set; `TEST_DATABASE_URL` escape hatch honored. Java +
      Python Testcontainers coexistence confirmed (both suites green on the same host, §5.6).
      CI worker job timeout 10→15 min for cold-runner image pulls.

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
