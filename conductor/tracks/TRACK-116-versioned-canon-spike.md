| Metadata | Value |
|:---|:---|
| **Status** | Completed |
| **Version** | 1.1.0 |
| **Last Updated** | 2026-06-13 |
| **Author** | Sangeetha Grantha Team |
| **Priority** | P1 — design gate for TRACK-117 |
| **Type** | Architecture spike — produces a decision + design, authors no migration |
| **Epic** | [TRACK-109](./TRACK-109-production-readiness-roadmap.md) (N5 data model) |
| **Decisions** | D1 ([decision log](../../application_documentation/north-star-production-readiness-decision.md)) |
| **Depends on** | none (design-only; does not need Flyway done) |
| **Blocks** | TRACK-117 |

# TRACK-116: Versioned Canon — Architecture Spike + ADR-014 (N5)

## Goal

Decide and document *how* the canonical record carries history and provenance, **before** any schema is written. Output is **[ADR-014](../../application_documentation/02-architecture/decisions/ADR-014-versioned-canon.md)** plus a concrete schema design, so TRACK-117 is pure execution. Estimated ~2–3 days.

**This spike is design-only and runs during the D2 freeze in parallel with TRACK-110** — it adds no critical-path time.

## Context

North-star finding N5: AUDIT_LOG records *that* a change happened, not a re-materializable *what*. A scholarly corpus needs bitemporal answers ("what did this krithi say on 2026-01-15, and which source/extraction/curator decision produced each section?"). Decision D1: because the DB krithis can be re-imported from scratch, we pause the import, build versioned canon, and re-import fresh — capturing history from row one with **no retrofit**.

## Implementation Plan

- [x] Evaluated three patterns (append-only `krithi_revisions` + projection view vs PostgreSQL temporal/system-versioned tables vs audit-derived reconstruction) with a trade-off table. **Chose Option A** — extension-free, per-section provenance co-located with content, native re-import population.
- [x] Designed the provenance graph: introduces a `source_documents` node between `extraction_queue` and `import_sources` (the registry); attribution at the **section** grain via `krithi_section_revisions.{extraction_id, source_document_id}`. Cardinality + null-handling (manual vs import) specified, with a `CHECK` requiring every revision to carry an extraction **or** a user.
- [x] Defined the revision-write contract: every *accepted* change writes one envelope + per-section rows + projection + AUDIT_LOG, atomically; import writes revision #1 with provenance from row one.
- [x] Specified point-in-time semantics (transaction-time `valid_from`; valid-time left as a future extension) and the one-query N5 provenance answer (`krithi_sections_asof()` + 3 LEFT JOINs).
- [x] Authored the `V44__versioned_canon.sql` migration **shape** (DDL sketch, FK additions, view + as-of function, indexes, no-backfill rationale) — **no SQL file written**, per the spike's design-only scope.
- [x] Impact check: current read routes/DTOs unaffected (read the projection); history/provenance endpoints are additive; deferred public read surface gains a future lineage capability.
- [x] **Wrote ADR-014** (status **Accepted**); added to `adr-index.md` + decisions `README.md`; linked from `integration-tests-approach.md` N5 reference and TRACK-117.

## Acceptance Criteria

- [x] ADR-014 merged with a chosen pattern, schema design, and migration shape.
- [x] TRACK-117 can be executed without further design decisions (DDL sketch + write contract + action items are spelled out).

## Pre-req Coordination

Before the data-model work begins, **checkpoint-commit and pause TRACK-093 AND TRACK-096** (the D2 freeze covers both). The spike itself can proceed during the freeze.

## References

- [North-Star Evaluation N5](../../application_documentation/north-star-evaluation.md)
- [ADR-014 (stub)](../../application_documentation/02-architecture/decisions/ADR-014-versioned-canon.md)
- [Implementation Plan — TRACK-116](../../application_documentation/north-star-production-readiness-implementation-plan.md)
