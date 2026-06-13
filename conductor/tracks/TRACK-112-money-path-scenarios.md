| Metadata | Value |
|:---|:---|
| **Status** | Not Started |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-06-13 |
| **Author** | Sangeetha Grantha Team |
| **Priority** | P1 |
| **Epic** | [TRACK-109](./TRACK-109-production-readiness-roadmap.md) (W2 / W8) |
| **Decisions** | D9 ([decision log](../../application_documentation/north-star-production-readiness-decision.md)) |
| **Depends on** | TRACK-110, TRACK-111 |
| **Supersedes** | TRACK-014 (Bulk Import Testing & QA) |

# TRACK-112: Money-Path Service & API Scenarios (Step 5)

## Goal

Cover the business-critical flows with real-DB integration tests, prioritized by risk. Estimated 1–2 weeks, incremental. Supersedes the deferred TRACK-014.

## Implementation Plan

### Phase 1 — Service scenarios (S1–S7)
- [ ] S1: ingestion → review → canon end-to-end (raw imported_krithi → quality scoring → auto-approval → canonical krithi + sections + junction rows + audit trail).
- [ ] S2: auto-approval boundary cases (score at threshold, missing raga resolution, conflicting composer).
- [ ] S3: entity resolution (same composer in 3 transliterations → one canonical entity; ambiguous → review, never auto-merge).
- [ ] S4: duplicate/variant handling (re-import creates a lyric variant, not a duplicate canonical record).
- [ ] S5: bulk import of a 50-krithi batch with 5 malformed rows — 45 succeed, 5 row-level errors, nothing partially written.
- [ ] S6: remediation/bulk-operation rollback is fully revertible.
- [ ] S7: concurrency — two curators approve the same imported krithi simultaneously → exactly one canonical record, one clear conflict response.

### Phase 2 — API scenarios (A1–A5, Ktor `testApplication` + Testcontainers)
- [ ] A1: AuthN/Z matrix over the 15 route files (anonymous/viewer/curator/admin → expected 401/403/200).
- [ ] A2: public read surface — list/detail/search return migrated-schema-true DTOs; ETag round-trip → 304.
- [ ] A3: error-envelope consistency (malformed JSON, validation failure, missing entity → consistent shape, no stack traces).
- [ ] A4: curator workflow over HTTP (review queue → approve → visible via public route → audit row exists).
- [ ] A5: OpenAPI conformance spot-checks (grows into the N6 drift gate later).

### Phase 3 — Fixtures
- [ ] Fixture builders (`aKrithi { … }`, `anImportedKrithi { … }`) with deterministic UUIDs/timestamps.
- [ ] Golden payloads from the Trinity import as `src/test/resources/payloads/`.
- [ ] Mark TRACK-014 **Superseded by TRACK-112** with a pointer.

## Acceptance Criteria

- The three-zone flow and RBAC boundary are machine-verified.
- Per-PR integration time within budget (heavy scenarios tagged for nightly if needed).

## References

- [Integration Tests Approach §4](../../application_documentation/07-quality/integration-tests-approach.md)
- [Implementation Plan — TRACK-112](../../application_documentation/north-star-production-readiness-implementation-plan.md)
