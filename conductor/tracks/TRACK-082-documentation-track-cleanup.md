| Metadata | Value |
|:---|:---|
| **Track ID** | TRACK-082 |
| **Title** | Documentation Update & Conductor Track Cleanup |
| **Status** | Completed |
| **Created** | 2026-03-10 |
| **Author** | Sangeetha Grantha Team |
| **Depends On** | TRACK-078, TRACK-079, TRACK-080, TRACK-081 |

# TRACK-082: Documentation Update & Conductor Track Cleanup

## Objective

Update onboarding and architecture documentation to reflect the current 3-language stack (Kotlin, Python, TypeScript) and Makefile-based workflows. Close remaining open conductor tracks.

## Scope

### Documentation Updates
- `getting-started.md`: Replace Rust CLI references with `make` commands, update prerequisites (Python 3.11+, JDK 25), update project structure listing
- `tech-stack.md`: Add Extraction & Enrichment (Python) section, update Tooling & Infrastructure to reflect Makefile + Docker Compose orchestration

### Track Cleanup
- TRACK-002 (Doc Header Standardizations): Planned → Deferred
- TRACK-014 (Bulk Import Testing & QA): Proposed → Deferred
- TRACK-035 (Frontend E2E Testing): In Progress → Deferred
- TRACK-042 (MCP Database Tooling): Proposed → Deferred

## Verification

- `getting-started.md` references `make dev`, `make db-reset`, `make migrate`
- `tech-stack.md` lists Python extraction worker section
- `conductor/tracks.md` has no In Progress or Proposed tracks
