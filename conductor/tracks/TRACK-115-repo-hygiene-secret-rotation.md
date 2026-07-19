| Metadata | Value |
|:---|:---|
| **Status** | Completed |
| **Version** | 1.2.0 |
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

- [x] Removed stray vendored tree `org/jetbrains/...` (4 tracked `.kt` + `.DS_Store`, accidental IDE copy). `Users/seshadri/...` was an empty, untracked dir — removed from disk only.
- [x] Removed working-dir log clutter (`backend.log`, `out.log`, `backend_test.log`, `old_krithi_editor.log`, `*_logs.txt`). **Note:** these were already gitignored and never tracked — disk housekeeping, not a repo purge.
- [x] `tools/sangita-cli-archived/.env`: untracked via `git rm --cached` and scrubbed the literal to `<set-via-env-or-secrets-manager>`. **Note:** the value was `dev-admin-token` — the public dev default (also in ~8 docs/examples), not a live secret; purged on principle to keep secret scanners quiet.
- [x] Hardened `.gitignore`: added root-anchored guards `/org/ /com/ /net/ /io/ /Users/` (these roots are always accidental IDE copies; real source lives under `modules/`). Logs/`.env`/IDE/build were already covered. Removed the confusing self-referential `.gitignore` entry.
- [x] **Consolidated** the AGENTS/CODEX/GEMINI/GOOSE.md agent-instruction files (done as the spun-out follow-up). Each now points to `CLAUDE.md` as the single source of truth and retains **only** its tool-specific delta — Gemini/Goose persona, musicological domain rules and response style; Codex entrypoint + MCP location; the `.agents/` skill/workflow catalog in AGENTS.md. The shared technical rules (migrations, audit, commits, architecture) are no longer duplicated, which removes the surface that drifted. This also fixed an active contradiction: all four files told agents to avoid Flyway and use the now-archived Rust CLI — the inverse of current ADR-013 policy.
- [x] Secret pass (grep — `gitleaks` not installed): private-key/AWS/GCP/GitHub/Slack/OpenAI-token/JWT and generic `secret|password|token|api_key` patterns over all tracked files. **No real secrets tracked**; only `dev-admin-token` defaults and `<placeholder>` refs remain.

## Acceptance Criteria

- [x] Clean repository root (no stray trees, no log artifacts).
- [x] `ADMIN_TOKEN` purged from the (now-untracked) file; verified no non-example `.env` is tracked.
- [x] Hardened `.gitignore` with recurrence guards.
- [x] Single agent-instruction source — `CLAUDE.md` is canonical; AGENTS/CODEX/GEMINI/GOOSE.md now thin pointers carrying only tool-specific deltas.

## References

- [North-Star Evaluation N8](../../application_documentation/north-star-evaluation.md)
- [Implementation Plan — TRACK-115](../../application_documentation/north-star-production-readiness-implementation-plan.md)
