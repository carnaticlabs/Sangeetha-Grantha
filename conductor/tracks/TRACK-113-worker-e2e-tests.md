| Metadata | Value |
|:---|:---|
| **Status** | Completed |
| **Version** | 2.0.0 |
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

### Phase 2 — E2E money paths (D12; nightly + pre-release) ✅ 2026-07-09
- [x] TRACK-035 reactivated + re-scoped to the compose substrate: `webServer` drives `make dev`
      (`reuseExistingServer` for a locally running stack); stale Rust-CLI error message and the
      archived `bulk_import_test.csv` paths fixed in `global-setup.ts`/`test-data.ts`;
      `E2E_SKIP_SHARED_BATCH` lets the money-path suite skip legacy shared-batch creation.
- [x] Path 1 (login → review → approve): real UI login (adminToken+email), seeds an `in_review`
      import, approves through the ConfirmationModal, verifies `import_status='approved'` +
      `mapped_krithi_id` + created krithi row in the DB. Self-cleaning.
- [x] Path 2 (bulk import happy + failure row): CSV made content-unique per run (checksum
      idempotency from TRACK-062 rejects byte-identical re-uploads) with one real source URL and
      one deterministic `.invalid`-TLD failure row; upload endpoint is rate-limited (429) so the
      test retries with backoff; asserts terminal batch state, `failed_tasks ≥ 1`, the `.invalid`
      task FAILED in `import_task_run`, and the failure surfaced in the batch-detail UI.
- [x] Path 3 (krithi edit, raga change): creates a krithi via API, changes the raga through the
      editor's selection modal, anchors save on the PUT round-trip, verifies the `krithi_ragas`
      junction AND `krithis.primary_raga_id`, then reloads and asserts the detail header shows the
      new raga (old one absent). **Found + fixed a real bug**: `KrithiService.updateKrithi` replaced
      the raga junction but left `primary_raga_id` pointing at the removed raga (create derived it,
      update didn't) — so the detail view kept showing the old raga after an edit. One-line fix
      mirrors the create path; regression pinned in the spec's DB assertions.
- [x] Nightly + pre-release: `.github/workflows/e2e-nightly.yml` (cron 21:30 UTC + manual
      dispatch) — compose stack up, health-gated, `bootstrapAdmin`, Playwright chromium,
      `bun run test:e2e:money`, report artifact + stack logs on failure.
      Verified locally: two consecutive full green runs (incl. the rate-limit backoff path).

## Acceptance Criteria

- Worker ↔ backend schema contract machine-verified. ✅ golden fixture breaks either suite on drift.
- Three E2E paths green nightly. ✅ suite green twice consecutively on the local compose substrate
  AND on CI (workflow_dispatch run 29022715430, all steps green on a cold runner). Hardening en
  route: CI compose overlay for the postgres:18 volume layout (fresh runners; base compose.yaml
  fix + local volume migration flagged separately), and admin provisioning via idempotent SQL
  (the gradle bootstrap-admin service contends on the gradle-cache lock held by the running
  backend, and the token+email auth path only needs the user row).
- TRACK-035 folded in, not duplicated. ✅ scaffolding reused (auth setup, DB verifier, config);
  legacy specs retained and runnable via `test:e2e`; money paths are the nightly gate.

## References

- [TRACK-035](./TRACK-035-frontend-e2e-testing.md)
- [Integration Tests Approach §4.4–§4.5, §5.6](../../application_documentation/07-quality/integration-tests-approach.md)
- [Implementation Plan — TRACK-113](../../application_documentation/north-star-production-readiness-implementation-plan.md)
