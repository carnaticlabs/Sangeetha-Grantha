| Metadata | Value |
|:---|:---|
| **Status** | Completed (with findings — see §Findings) |
| **Version** | 2.1.0 |
| **Last Updated** | 2026-07-18 |
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

### Phase 1 — Service scenarios (S1–S7) ✅ `MoneyPathServiceTest` (22 tests)
- [x] S1: ingestion → review → canon end-to-end — canonical krithi + sections + lyric variants + raga junction rows + source evidence + audit trail.
- [x] S2: auto-approval boundary cases — score exactly at threshold vs just below, missing raga, low-confidence raga, ambiguous composer, high-confidence duplicate, missing metadata, ineligible tier, already-reviewed.
- [x] S3: entity resolution — seeded aliases collapse to one composer entity.
- [x] S4: duplicate handling — re-import maps to the existing canonical record and records both source URLs as evidence.
- [x] S5: 50-row batch with 5 malformed rows — 45 reach canon, 5 fail individually with a named cause, failed rows stay PENDING.
- [x] S6: revertibility — a curator edit appends a CURATOR_EDIT revision; existing revisions are never rewritten. **Re-scoped**: the original wording assumed a rollback feature; none exists (no `rollback`/`revert`/`undo` in services or routes). Since versioned canon (TRACK-117) landed, revertibility now means recoverable-from-revision-history, which is what is asserted. S6b covers rejection safety, S6c the fix below.
- [x] S7: concurrency — two simultaneous approvals of one import yield exactly one canonical krithi and one agreed mapping. S7b covers submission idempotency.

### Phase 2 — API scenarios (A1–A5) ✅ `MoneyPathApiTest` (18 tests)
- [x] A1: auth boundary — anonymous → 401, unsigned/foreign-secret token → 401, valid token → 200. **The 401/403/200 matrix could not be written as specified: there is no 403 tier.** See F3.
- [x] A2: public read surface — anonymous search returns the paged envelope; ETag round-trip on `/v1/ragas` → 304 with an empty body; unknown id → 404; malformed id → 4xx.
- [x] A3: error envelopes — malformed JSON is a client error; no stack frames or exception class names in any error body.
- [x] A4: curator workflow over HTTP — queue → approve → publicly readable → audit row; rejected imports surface nothing publicly.
- [x] A5: conformance spot-checks — `AuthTokenResponse` shape, pagination echo, error envelope.

### Phase 3 — Fixtures ✅ `MoneyPathFixtures`
- [x] Fixture builders — `anImportRequest`, `anImportBatch(count, malformed)`, `aPendingImport`, `anApprovedKrithi`, `aCurator`, plus deterministic `testUuid(seed)` / `testSourceKey(seed)`. Placed in the **api** test source set, not `:modules:backend:test-support`, because they build api-module request models and test-support depends only on `:dal` — putting them there would invert the module dependency.
- [x] Golden payloads — **deliberately not copied** into `api/src/test/resources/payloads/`. `shared/domain/model/import/fixtures/canonical-extraction-golden.json` already serves this role and is asserted by both the Python worker suite and `CanonicalExtractionGoldenFixtureTest`, so it pins the Kotlin↔Python contract (TRACK-113 W2). Copying it would fork that contract; `MoneyPathFixtures.goldenExtraction()` reuses it instead.
- [x] Mark TRACK-014 **Superseded by TRACK-112** with a pointer.

## Findings

Writing the coverage surfaced five defects. **F1 is fixed**; the rest are pinned by characterisation
tests whose comments state the condition that should invert them, so a fix surfaces visibly.

| # | Finding | Status |
|:---|:---|:---|
| F1 | Approving an import whose payload has no COMPLETED extraction derived a source document with no extraction run, violating `ksr_doc_requires_extraction_ck` (V44) and aborting the approval **after** the krithi was committed — orphaned krithi, import left PENDING, retry could duplicate it. | **Fixed** — the document node is only derived when an extraction is resolvable; otherwise the revision is still written, curator-attributed. Guarded by S6c. |
| F2 | `saveKrithiSections` mutates exactly the state revisions capture but writes no revision, so section edits leave no recovery point. `KrithiRevisionDto` also carries no krithi-level metadata, so `updateKrithi` snapshots cannot restore a title. ADR-014 coverage gap. | Open — pinned by S6 `GAP` test. Belongs to TRACK-117. |
| F3 | No route checks the `roles` claim; `authenticate("admin-auth")` validates only signature, audience and a `userId` claim. Any validly-signed token — including one with no roles — has full admin access. Compounded by `POST /v1/auth/token` minting caller-supplied roles behind the shared `ADMIN_TOKEN`. | Open — pinned by two A1 `GAP` tests. This is the TRACK-119 deployment blocker. |
| F4 | `reviewImport`'s broad `catch (e: Exception)` re-wraps `NoSuchElementException` as `RuntimeException("Failed to create krithi: …")`, so a missing import returns **500** instead of 404 and leaks internal phrasing to the caller. | **Fixed** — the catch now lets `NoSuchElementException` (404) and `IllegalArgumentException` (400) propagate with their own identity; only genuinely unexpected failures are wrapped. Guarded by two A4 tests. |
| F6 | `DatabaseFactory.dbQuery` did not join an enclosing transaction — every nested call opened its own and committed independently. A service could not make a multi-repo operation atomic by wrapping it: the wrap compiled, read as a transaction boundary, and did nothing. `reviewImport` relied on exactly that and could leave a committed krithi behind when a later step failed. | **Fixed** — see below. Guarded by `DbQueryNestingTest` (4 tests) and S6d. |
| F5 | `AutoApprovalService` treats an unparseable `duplicateCandidates` payload as "no duplicates" and auto-approves — fail-open on a deduplication guard. | Open — pinned by S2 `GAP` test. |

### The transaction fix (F6) — what it took

Wrapping `reviewImport` in `dbQuery` was **not** sufficient, and would have been a silent no-op. An
empirical probe against Exposed 1.0 established that a nested `newSuspendedTransaction` opens its own
transaction and commits independently, while the ambient transaction *is* visible to nested suspend
calls. So the fix had to go in `DatabaseFactory.dbQuery` itself: if `TransactionManager.currentOrNull()`
returns a transaction, join it instead of opening a new one.

Blast radius was checked before changing shared infrastructure: no `dbQuery` block anywhere in the
api module currently nests a `dal.*` repo call, so joining is a **no-op for all existing code** and
only affects code that deliberately nests.

**Making the promotion atomic then exposed a second problem.** With one transaction per approval,
two concurrent approvals of the same import no longer see each other's uncommitted krithi under READ
COMMITTED, so both created one — S7 started failing with 2 krithis. The old code had only avoided
this by accident: incremental commits narrowed the window, they did not close it. The fix is a proper
one — `ImportRepository.findByIdForUpdate` takes a row lock on the import for the duration of the
promotion, so the second approval blocks, then observes the import already APPROVED and adopts the
krithi the first one produced.

**Test-quality note.** The first atomicity test written for this was vacuous: it used a composer-less
import as the failure trigger, which throws *before* anything is written, so it passed with the fix
disabled. It was replaced with a trigger that fails late — an unresolvable reviewer id, which fails
the ADR-014 revision write after the krithi, sections, junction rows and source evidence are all
written. Both S6d and `DbQueryNestingTest` were verified to fail with the fix reverted.

## Acceptance Criteria

- [x] The three-zone flow is machine-verified (S1, A4).
- [~] The RBAC boundary is machine-verified **as it exists** — authentication is covered; authorisation is not enforced anywhere, and that fact is now pinned rather than assumed (F3).
- [x] Per-PR integration time within budget — the 40 money-path tests add ~10s to `:api:integrationTest`; no scenario needed deferring to nightly.

## References

- [Integration Tests Approach §4](../../application_documentation/07-quality/integration-tests-approach.md)
- [Implementation Plan — TRACK-112](../../application_documentation/north-star-production-readiness-implementation-plan.md)
