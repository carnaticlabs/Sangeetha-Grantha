| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-06-24 |
| **Author** | Sangeetha Grantha Team |

# Pending Implementation Register

A running log of **deliberately deferred** capabilities and cleanups — work that was
identified during a change, intentionally left out of scope to keep that change focused,
and should be picked up at an appropriate time on the path to a stable production release.

Each entry is self-contained: it records what exists today, why it was deferred, the
proposed approach, and acceptance criteria, so it can be actioned cold without re-deriving
the original context.

> **How to use this file.** When closing out a change, append anything you knowingly punted.
> When picking an item up, convert it into a `conductor/` track, implement, then move the
> entry to a `## Resolved` section (or delete it) with a `Ref:` to the implementing track/PR.

---

## Open Items

### PI-001 — Drop the deprecated `dissenting_sources` column

| Field | Value |
|:---|:---|
| **Area** | Backend / Database (Flyway) |
| **Introduced by** | Structural-voting type reconciliation (frontend⇄backend structured DTOs) — 2026-06-24 |
| **Blast radius** | Low — single column, no live readers |

**Current state.** `structural_vote_log.dissenting_sources` (`jsonb`, **NOT NULL**) is no longer
authoritative. Dissent is now **derived** from `participants` where `agrees == false`
(`VotingParticipantDto.agrees`), and the column is written `'[]'` on every insert purely to
satisfy the `NOT NULL` constraint. Nothing reads it: the DTOs (`VotingDecisionDto`,
`VotingDetailDto`) and the frontend `VotingDetailResponse` no longer expose it.

**Why deferred.** Removing it requires a Flyway migration (the column is `NOT NULL`, so it cannot
simply be ignored at insert), and the reconciliation change was scoped to type/serialization only.

**Proposed approach.**
- Add `database/migrations/VNN__drop_dissenting_sources.sql`: `ALTER TABLE structural_vote_log DROP COLUMN dissenting_sources;`
- Remove `dissentingSources` from `StructuralVoteLogTable` (`modules/backend/dal/.../tables/SourcingTables.kt`).
- Remove the `it[V.dissentingSources] = "[]"` writes in `StructuralVotingRepository.createVotingRecord` / `createOverride`.

**Acceptance criteria.**
- Migration applies cleanly via `make migrate` / `make db-reset`.
- `:modules:backend:dal:integrationTest` (incl. `StructuralVotingRoundTripTest`) and `:modules:backend:api:test` stay green.
- No reference to `dissenting_sources` / `dissentingSources` remains in backend or frontend source.

**Key references.**
- `modules/backend/dal/src/main/kotlin/com/sangita/grantha/backend/dal/tables/SourcingTables.kt` (`StructuralVoteLogTable`)
- `modules/backend/dal/src/main/kotlin/com/sangita/grantha/backend/dal/repositories/StructuralVotingRepository.kt`
- `database/migrations/V25__structural_vote_log.sql` (original column definition)

---

### PI-002 — Restore the two removed `SourceEvidenceTab` panels (backend capability gap)

| Field | Value |
|:---|:---|
| **Area** | Backend (DTOs + evidence service) + Frontend |
| **Introduced by** | Frontend type-error cleanup of the sourcing UI — 2026-06-24 |
| **Blast radius** | Medium — touches the evidence API shape and the krithi-editor UI |

**Current state.** Two panels were removed from
`modules/frontend/sangita-admin-web/src/components/krithi-editor/SourceEvidenceTab.tsx` because they
read fields the current evidence API does not return (see the explanatory comment left in that file):

1. **"Structural Analysis per Source"** — read `KrithiEvidenceSource.structure`, which does not exist
   on `KrithiEvidenceSourceDto`.
2. **"Voting Decisions"** — read `KrithiEvidenceResponse.votingDecisions`, which does not exist on
   `KrithiEvidenceResponseDto`.

The removal is correct as a stopgap (the data was never delivered, so the panels could not have
worked), but the **capability** is still wanted for production.

**Why deferred.** Re-adding the panels requires new backend capability — extending the evidence DTOs
and the producing service/repository — which is a distinct piece of work from the voting type
reconciliation that surfaced it.

**Proposed approach.**
- Add `structure: List<SectionSummaryDto>` (per-source parsed sections) to `KrithiEvidenceSourceDto`,
  populated where each source's `rawExtraction` is parsed.
- Add an evidence-level `votingDecisions: List<VotingDecisionDto>` (or a slim summary DTO) to
  `KrithiEvidenceResponseDto`, populated by joining `structural_vote_log` for the krithi.
- Re-mirror both on the frontend `KrithiEvidenceSource` / `KrithiEvidenceResponse` types and restore
  the two panels in `SourceEvidenceTab.tsx` (reuse `StructureVisualiser` + the existing voting links).

**Acceptance criteria.**
- `GET /admin/sourcing/evidence/krithi/{id}` returns per-source `structure` arrays and a
  `votingDecisions` list (real JSON arrays, end-to-end).
- Both panels render in the krithi editor against live data; `bun run build` / `tsc --noEmit` clean.
- Reuses the structured `SectionSummaryDto` contract from the voting reconciliation (no string JSONB
  fields reintroduced).

**Key references.**
- `modules/frontend/sangita-admin-web/src/components/krithi-editor/SourceEvidenceTab.tsx` (removal + comment)
- `modules/shared/domain/.../model/EvidenceVotingDtos.kt` (`KrithiEvidenceSourceDto`, `KrithiEvidenceResponseDto`, `SectionSummaryDto`)
- `modules/frontend/sangita-admin-web/src/types/sourcing.ts` (`KrithiEvidenceSource`, `KrithiEvidenceResponse`)

---

### PI-003 — Carried-forward cleanups from TRACK-120 (Batch 1 dependency upgrades)

| Field | Value |
|:---|:---|
| **Area** | Backend (Ktor) + Frontend (test/lint config) |
| **Introduced by** | TRACK-120 — Batch 1 safe dependency upgrades — 2026-06-24 |
| **Blast radius** | Low — warnings + pre-existing test-config gaps, no runtime impact |

**Current state.** Two non-blocking notes were carried forward from TRACK-120 (neither caused by the
upgrades themselves):

1. **Ktor 3.5 `dispose → release` deprecation warnings** in
   `modules/backend/api/src/main/kotlin/com/sangita/grantha/backend/api/routes/BulkImportRoutes.kt`
   (6 call sites, lines ~46–118). Ktor 3.5 deprecates `val dispose: () -> Unit` in favour of `release`.
   Compilation succeeds; this is warning-level only, but the project targets zero warnings (TRACK-099).
2. ~~**Pre-existing Vitest/lint issues tied to TRACK-118 not being started.**~~ **RESOLVED 2026-06-24**
   under the TRACK-118 baseline: `vitest run` now scopes to `src/` (excludes `e2e/`) with a first
   passing test, and `bun run lint` is at 0 errors. jsdom pinned to `^26.1.0` to work under EOL Node 21.
   See TRACK-118 "Baseline delivered". Only item (1) below remains open.

**Why deferred.** TRACK-120 was scoped to safe version bumps only; touching source (Ktor API migration)
or standing up the frontend test/lint baseline (TRACK-118) was intentionally left out to keep the
upgrade focused and independently revertable.

**Proposed approach.**
- Replace the 6 deprecated `dispose` usages with `release` in `BulkImportRoutes.kt`; rebuild to confirm
  zero new warnings.
- Address the Vitest/lint gaps under **TRACK-118** (Frontend Component Tests): add a `test.include` /
  `exclude` to `vitest.config.ts` so it ignores `e2e/`, and clear the standing lint errors.

**Acceptance criteria.**
- `./gradlew :modules:backend:api:assemble` emits no `dispose`-related deprecation warnings.
- `:modules:backend:api:test` stays green.
- (Under TRACK-118) `vitest run` no longer collects Playwright specs; `bun run lint` is clean.

**Key references.**
- `modules/backend/api/src/main/kotlin/com/sangita/grantha/backend/api/routes/BulkImportRoutes.kt`
- `modules/frontend/sangita-admin-web/vitest.config.ts`
- `conductor/tracks/TRACK-120-dependency-upgrades-safe-jun-2026.md`, `conductor/tracks/TRACK-118-frontend-component-tests.md`
