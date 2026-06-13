| Metadata | Value |
|:---|:---|
| **Status** | Not Started |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-06-13 |
| **Author** | Sangeetha Grantha Team |
| **Priority** | P1 — time-sensitive (gates TRACK-093/096 resume) |
| **Epic** | [TRACK-109](./TRACK-109-production-readiness-roadmap.md) (N5 data model) |
| **Decisions** | D1 ([decision log](../../application_documentation/north-star-production-readiness-decision.md)) |
| **Depends on** | TRACK-116 (ADR-014 + design), TRACK-110 (Flyway cutover) |
| **Interacts with** | TRACK-093 (paused), TRACK-096 (paused) |

# TRACK-117: Versioned Canon — Implementation + Re-Import (N5)

## Goal

Implement the [ADR-014](../../application_documentation/02-architecture/decisions/ADR-014-versioned-canon.md) design and re-import the Trinity corpus fresh, so history + provenance are captured from row one (no retrofit — enabled by the D1 "re-import from scratch" call). Estimated ~1 week.

> **Scope note:** Phase-2 data-model item, *outside* the tests+CI+Flyway scope (D6), but sequenced here because the paused import makes it cheap now and expensive later.

## Implementation Plan

- [ ] Author the schema as a **Flyway** migration (`V44__versioned_canon.sql` or per ADR-014): `krithi_revisions`, provenance FKs, current-state projection view. Depends on TRACK-110 so it is written in Flyway format from the start.
- [ ] Wire the import/ingestion path to populate revision + provenance rows at creation (not backfilled).
- [ ] `make db-reset` → re-import Trinity krithis fresh; verify revision + provenance rows populate from the import path.
- [ ] Verify junction tables (`krithi_ragas`) and sections populate through the full stack (DB → API → UI) post-reimport.
- [ ] Add integration coverage (reuses the TRACK-110 substrate): a krithi edit creates a revision; provenance lineage resolves in one query.
- [ ] Resume / close TRACK-093 and TRACK-096 against the new schema.

## Acceptance Criteria

- Every canonical krithi has a creation revision and queryable provenance lineage.
- "What did this krithi say on date X / which source produced this section" answerable in one query.
- Trinity import complete on the new model.

## References

- [North-Star Evaluation N5](../../application_documentation/north-star-evaluation.md)
- [ADR-014](../../application_documentation/02-architecture/decisions/ADR-014-versioned-canon.md)
- [Implementation Plan — TRACK-117](../../application_documentation/north-star-production-readiness-implementation-plan.md)
