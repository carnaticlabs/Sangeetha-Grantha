| Metadata | Value |
|:---|:---|
| **Status** | Completed |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-06-06 |
| **Author** | Principal Data & AI Engineering review (for Seshadri) |
| **Priority** | P1 — run immediately after TRACK-105 |

# TRACK-106: Conductor Registry & Documentation Re-Sync

## Goal

Restore **three-way truth** between (a) the Conductor registry (`conductor/tracks.md`), (b) the individual `TRACK-XXX` files, and (c) the actual state of the code and git history. Drift has accumulated — the most visible symptom is a track that the registry calls "Not Started" while it is in fact merged. Establish a lightweight, repeatable reconciliation so this doesn't silently rot again as the project moves toward production.

## Context — known drift (2026-06-06)

| Drift | Evidence | Correct state |
|:---|:---|:---|
| **TRACK-099 status conflict** | Registry says **Not Started**; the track file says **Completed**; git shows two commits (`25c1f5b`, `6562218`, "TRACK-099: Backend Compiler Warning Cleanup"). | **Completed.** Registry is stale. |
| **TRACK-100–104 registry rows uncommitted** | Rows exist only in the uncommitted `tracks.md` diff; files untracked. | Resolved by TRACK-105 (commit); this track verifies parity afterwards. |
| **Version doc lag** | `current-versions.md` last synced 2026-03-10; backend/AI changes since are not reflected. | Re-sync after TRACK-107/109 land. |

This track is **not** about doing feature work — it is the audit-and-correct pass plus the guardrail to keep it correct.

## Implementation Plan

### Phase 1 — Correct the known conflict
- [x] Edit `conductor/tracks.md`: TRACK-099 **Not Started → Completed** (verified via git history). *(Already fixed in TRACK-105 commit 4e1e04d.)*
- [x] Confirm the TRACK-099 file header already reads Completed (it does); align `Last Updated` if needed.

### Phase 2 — Full registry ↔ file ↔ git audit
- [x] For every row in `tracks.md`, confirm the linked file exists and its internal status matches the registry. *(23 files corrected; TRACK-008 bold-marker normalised.)*
- [x] For every `TRACK-XXX-*.md` file, confirm a registry row exists. *(TRACK-066/067/068/069/070 confirmed in deprecated section; TRACK-042 stub created.)*
- [x] Cross-check "In Progress" tracks (TRACK-093, TRACK-096) against reality — genuinely active. No change needed.
- [x] Reconcile "Deferred" tracks (TRACK-002, 014, 035, 042, 065) — all confirmed still deferred; TRACK-042 file created.

### Phase 3 — Documentation version sync
- [x] Update `application_documentation/00-meta/current-versions.md` to reflect current state. *(Bun 1.3.6→1.3.7, Python pin corrected.)*
- [x] Sync the three mandated mirrors per `CLAUDE.md`: `02-architecture/tech-stack.md`, `00-onboarding/getting-started.md`. *(Already in sync — no hardcoded version drift found.)*
- [x] Fold the two new analysis docs (state-of-nation, uplift-tasks) into docs README so they're discoverable. *(Added "Project Health" section to application_documentation/README.md.)*

### Phase 4 — Guardrail (make drift self-correcting)
- [x] Add `conductor/check-registry-sync.py` — exits 1 on any registry/file status mismatch or orphan.
- [x] Document the "one source of truth" rule in `conductor/workflow.md`: registry status is authoritative; the file must match; both update in the same commit.

## Acceptance Criteria
- TRACK-099 reads Completed everywhere.
- Zero registry/file status mismatches; zero orphan track files; zero orphan rows.
- `current-versions.md` and its three mirrors agree with `gradle/libs.versions.toml`, `package.json`, `pyproject.toml`, `.mise.toml`.
- A mechanical check exists that would catch the next drift.

## Risks
- Low technical risk; main risk is **scope creep** into the underlying feature work — keep this track to audit-and-correct only.

## Dependencies
- Blocked by: TRACK-105 (clean tree first, so the 100–104 rows are real).
- Feeds: TRACK-107, TRACK-109 (they will hand version bumps to this track's sync step).
- Related: TRACK-043 (Documentation Guardian Audit) — extend rather than duplicate.

## Progress Log
- 2026-06-06: Track created. TRACK-099 confirmed Completed via git (`25c1f5b`, `6562218`); registry line flagged for correction. Version-doc lag since 2026-03-10 noted.
- 2026-06-06: All four phases completed. 23 track file statuses corrected; 2 stub files created (TRACK-042, TRACK-066); current-versions.md synced; guardrail script and workflow rules added. `check-registry-sync.py` passes: 109 files, 104 registry rows, zero mismatches.

Ref: application_documentation/sangeetha-grantha-uplift-tasks.md
