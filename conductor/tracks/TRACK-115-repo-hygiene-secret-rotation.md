| Metadata | Value |
|:---|:---|
| **Status** | Not Started |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-06-13 |
| **Author** | Sangeetha Grantha Team |
| **Priority** | P1 — do before CI activation |
| **Epic** | [TRACK-109](./TRACK-109-production-readiness-roadmap.md) (W1/W6) |
| **Decisions** | D18 ([decision log](../../application_documentation/north-star-production-readiness-decision.md)) |
| **Depends on** | none |
| **Blocks** | TRACK-111 (clean root → green first CI / secret scan) |

# TRACK-115: Repository Hygiene & Secret Rotation (N8)

## Goal

Clean the repository root so the first CI run (and its secret/dependency scans, TRACK-111) is green against a clean tree. North-star rates this "an afternoon," and doing it first lowers the CI noise floor. Estimated ~0.5 day.

## Context

North-star finding N8: stray vendored trees and build/log artifacts at root, a tracked `ADMIN_TOKEN`, and four drifting agent-instruction files raise the noise floor for every future contributor and would generate false-positive CI scan failures.

## Implementation Plan

- [ ] Remove stray vendored trees at root: `org/jetbrains/...`, `Users/seshadri/...` (accidental IDE copies).
- [ ] Remove build/log artifacts at root: `backend.log`, `out.log`, `*_logs.txt`, `sangita_extraction_logs.txt`, `worker.log`.
- [ ] **Rotate** the `ADMIN_TOKEN` in tracked `tools/sangita-cli-archived/.env`; purge the value from the tracked file (rotate on principle even though the CLI is archived).
- [ ] Harden `.gitignore` (logs, `.env`, IDE trees, build outputs) so these cannot recur.
- [ ] Consolidate the four agent-instruction files (AGENTS/CODEX/GEMINI/GOOSE.md) into pointers to `CLAUDE.md` to prevent silent drift — or confirm out of scope and defer.
- [ ] Quick `gitleaks`/grep pass to confirm no other secrets are tracked.

## Acceptance Criteria

- Clean repository root (no stray trees, no log artifacts).
- `ADMIN_TOKEN` rotated and purged from the tracked file.
- Hardened `.gitignore`.
- (Optional) single agent-instruction source.

## References

- [North-Star Evaluation N8](../../application_documentation/north-star-evaluation.md)
- [Implementation Plan — TRACK-115](../../application_documentation/north-star-production-readiness-implementation-plan.md)
