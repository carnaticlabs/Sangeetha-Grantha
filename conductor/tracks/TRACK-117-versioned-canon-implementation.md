| Metadata | Value |
|:---|:---|
| **Status** | In Progress (implementation complete; Trinity re-import pending) |
| **Version** | 1.1.0 |
| **Last Updated** | 2026-07-10 |
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

- [x] Author the schema as a **Flyway** migration — `V44__versioned_canon.sql` (2026-07-10): `source_documents`,
      `krithi_revisions`, `krithi_section_revisions`, `extraction_queue.source_document_id`,
      `v_krithi_current_revision` view, `krithi_sections_asof()` function, per the ADR-014 DDL sketch
      (uuidv7 defaults, attribution + doc-requires-extraction CHECKs, pure DDL, no backfill).
- [x] Wire the import/ingestion path (2026-07-10): DAL `RevisionRepository` (append-only `appendRevision`,
      `ensureSourceDocument[ForSource]` with registry resolution shared with `krithi_source_evidence`,
      `snapshotCurrentState`, `latestRevision`/`listRevisions`/`sectionProvenance`).
      Revision-writers wired: `KrithiCreationFromExtractionService` (IMPORT rev #1 with per-section
      extraction + source-document attribution), `ImportService.reviewImport` curator approvals
      (IMPORT snapshot attributed to the JWT reviewer — `reviewerUserId` threaded from the routes),
      `KrithiService.updateKrithi` (CURATOR_EDIT snapshot).
- [x] **Auto-approval gap closed (2026-07-11).** `ImportService.reviewImport` now resolves the
      producing extraction by source URL (`ExtractionQueueRepository.findLatestCompletedIdBySourceUrl`)
      and the source-document node from the imported row's canonical `parsed_payload`, then writes the
      revision attributed to `(extraction, source_document)` and/or the curator. Because the
      extraction alone satisfies the ADR-014 attribution floor, **system auto-approvals**
      (`AutoApprovalService`, no reviewer) now write revisions too — previously skipped. Only imports
      with neither a resolvable extraction nor a reviewer are skipped (logged). No schema change was
      needed — the linkage rides the existing `source_key` ↔ `extraction_queue.source_url` correlation
      and the canonical payload already on the row. Verified: new DAL test for the lookup, full
      backend suites green, E2E money paths green on the reworked approval path.
- [ ] `make db-reset` → re-import Trinity krithis fresh; verify revision + provenance rows populate from
      the import path. **Deliberately left for a supervised run** — drops the dev DB and re-scrapes the
      corpus over the network (hours); resume together with TRACK-093.
- [x] Verify through the full stack (2026-07-10, sans re-import): live API probe — krithi edit via
      PUT /admin/krithis writes an attributed CURATOR_EDIT revision; `krithi_sections_asof()` executes
      against live data; all three E2E money paths green on the revision-writing backend.
- [x] Integration coverage on the TRACK-110 substrate (2026-07-10): `VersionedCanonTest` (6 tests) —
      monotonic revision numbers + latest-wins, attribution floor rejected, source-document dedup,
      one-query provenance lineage (URL/checksum/registry), as-of point-in-time reads (v1 at t,
      v2 now, empty before creation), `snapshotCurrentState` capturing serving-layer sections+lyrics.
- [ ] Resume / close TRACK-093 and TRACK-096 against the new schema (after the supervised re-import).

## Acceptance Criteria

- Every canonical krithi has a creation revision and queryable provenance lineage.
- "What did this krithi say on date X / which source produced this section" answerable in one query.
- Trinity import complete on the new model.

## References

- [North-Star Evaluation N5](../../application_documentation/north-star-evaluation.md)
- [ADR-014](../../application_documentation/02-architecture/decisions/ADR-014-versioned-canon.md)
- [Implementation Plan — TRACK-117](../../application_documentation/north-star-production-readiness-implementation-plan.md)
