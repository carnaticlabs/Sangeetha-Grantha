| Metadata | Value |
|:---|:---|
| **Status** | Draft — to be authored by [TRACK-116](../../../conductor/tracks/TRACK-116-versioned-canon-spike.md) |
| **Version** | 0.1.0 |
| **Last Updated** | 2026-06-13 |
| **Author** | Sangeetha Grantha Team |

# ADR-014: Versioned Canon & Provenance Graph (N5)

> **Stub.** This ADR is scaffolded ahead of the architecture spike (TRACK-116). It will be filled in and moved to **Accepted** as the spike's deliverable, then implemented by TRACK-117. Do not treat the choices below as decided — they are the questions the spike must answer.

## Context

North-star finding N5: the canonical krithi tables hold only current state; AUDIT_LOG records *that* a change happened, not a re-materializable *what*. A scholarly corpus needs bitemporal answers — "what did the canonical text of this krithi say on 2026-01-15, and which source/extraction/curator decision produced each section?" Decision D1 (see [decision log](../../north-star-production-readiness-decision.md)) chose to pause the Trinity import, build versioned canon, and re-import fresh so history is captured from row one with no retrofit.

## Decision

*To be determined by TRACK-116.* Candidate patterns to evaluate:

- **Append-only `krithi_revisions` + current-state projection view** (working hypothesis).
- PostgreSQL temporal/system-versioned tables.
- Audit-derived reconstruction (rejected baseline — N5 is precisely its failure).

## Open questions for the spike

- Revision granularity: whole-krithi vs per-section revisions.
- Provenance graph FKs: `canonical_section.provenance → extraction_id → source_document_id → source_registry` — cardinality and null-handling.
- Revision-write contract: what creates a revision; how the import path populates it at creation.
- Point-in-time read semantics and the one-query provenance answer.
- Migration shape (authored in Flyway by TRACK-117): DDL, FK additions, projection view.
- Impact on routes/DTOs/repos and the (deferred) public read surface.

## Consequences

*To be completed by the spike.*

## References

- [North-Star Evaluation N5](../../north-star-evaluation.md)
- [TRACK-116 (spike)](../../../conductor/tracks/TRACK-116-versioned-canon-spike.md) · [TRACK-117 (implementation)](../../../conductor/tracks/TRACK-117-versioned-canon-implementation.md)
- [Implementation Plan](../../north-star-production-readiness-implementation-plan.md)
