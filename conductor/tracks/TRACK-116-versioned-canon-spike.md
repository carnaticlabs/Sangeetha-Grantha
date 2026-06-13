| Metadata | Value |
|:---|:---|
| **Status** | Not Started |
| **Version** | 1.0.0 |
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

- [ ] Use `engineering:architecture` / `engineering:system-design` skills to evaluate the revisioning pattern: append-only `krithi_revisions` + current-state projection/view vs alternatives (temporal tables, audit-derived reconstruction). Recommend one with trade-offs.
- [ ] Design the provenance graph: `canonical_section.provenance → extraction_id → source_document_id → source_registry` as enforced FKs; define cardinality and null-handling.
- [ ] Define the revision-write contract: what creates a revision (every accepted change), how the import path populates it from row one, how the projection serves "current".
- [ ] Specify point-in-time read semantics and the one-query provenance answer.
- [ ] Migration shape for TRACK-117 to author in Flyway: table DDL sketch, FK additions, projection view, backfill-not-needed rationale (re-import covers it).
- [ ] Impact check: which routes/DTOs/repos change; whether the (deferred) public read surface is affected later.
- [ ] **Write ADR-014** (status Accepted on completion); add to `adr-index.md` + decisions `README.md`; link from `integration-tests-approach.md` N5 references and TRACK-117.

## Acceptance Criteria

- ADR-014 merged with a chosen pattern, schema design, and migration shape.
- TRACK-117 can be executed without further design decisions.

## Pre-req Coordination

Before the data-model work begins, **checkpoint-commit and pause TRACK-093 AND TRACK-096** (the D2 freeze covers both). The spike itself can proceed during the freeze.

## References

- [North-Star Evaluation N5](../../application_documentation/north-star-evaluation.md)
- [ADR-014 (stub)](../../application_documentation/02-architecture/decisions/ADR-014-versioned-canon.md)
- [Implementation Plan — TRACK-116](../../application_documentation/north-star-production-readiness-implementation-plan.md)
