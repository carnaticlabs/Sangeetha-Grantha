| Metadata | Value |
|:---|:---|
| **Status** | Ready for review |
| **Version** | 1.1.0 |
| **Last Updated** | 2026-08-29 |
| **Author** | Sangeetha Grantha Team |

# Track: AI-native SDLC artifacts
**ID:** TRACK-134
**Status:** Ready for review
**Owner:** Sangeetha Grantha Team
**Created:** 2026-08-24
**Updated:** 2026-08-29

## Goal

Close the AI-native SDLC gaps around Build: committed Intent / Spec / Plan on tracks, review policy, session verification, edit-time hooks, and deterministic agent-config evals. Conductor stays the system of record — no parallel `intent/` tree.

## Context
- **Reference:** [AI-native SDLC artifacts](../../application_documentation/10-implementations/track-134-ai-native-sdlc-artifacts.md)
- **Playbook:** [The AI-Native SDLC playbook](https://claude.com/blog/the-ai-native-sdlc-playbook) (21 Aug 2026)
- **Depends on:** TRACK-083 (agent skills/hooks), TRACK-111 (CI)

## Intent
**Status:** Accepted
**Accepted by:** User (implementation request)
**Accepted at:** 2026-08-24

### Problem
Agents already write code faster than Plan, Review, and Maintain can absorb it. Tracks mix intent, design, and a task list. There is no committed plan for later review to check, no REVIEW.md, no required verification output, and hooks only fire at commit.

### Proposed outcome
Every new track has Intent → Spec → Plan sections with a human accept gate. Slash commands write Spec and Plan. PRs are reviewed against REVIEW.md. Sessions paste test output before “done.” Edit-time hooks block `.env` reads and illegal migration edits. CI fails if agent-config evals regress.

### Affected users and systems
Agents (Claude Code / Cursor), humans reviewing PRs, CI, `conductor/` tracks, `.claude/` hooks and commands.

### Constraints
- Conductor remains source of truth; do not add a parallel `intent/` directory.
- Do not automate production deploys (TRACK-109).
- Evals must be deterministic (no LLM API key in CI).
- Keep CLAUDE.md short; put layer rules in skills.

### Open questions
None for this slice. Full LLM-in-CI evals and production control bands stay deferred.

## Spec
**Status:** Accepted
**Accepted by:** User (chose the six concrete artifacts)
**Accepted at:** 2026-08-24

### Requirements
1. Track template and conductor-track-manager include Intent / Spec / Plan with status gates.
2. `/spec-from-track` and `/plan-from-spec` write those sections; they do not implement code.
3. `REVIEW.md` at repo root: bugs, security, compliance (including lakshana).
4. CLAUDE.md has a Verifying your work block.
5. Edit-time hooks: deny `.env` reads; protect committed `V__` migrations.
6. `evals/` + `make agent-evals` + CI job on agent-config paths.

### Design
Markdown artifacts live on the track file. Commands are `.claude/commands/` with Cursor symlinks. Hooks are scripts under `.claude/hooks/`. Evals are JSON cases checked by `evals/check.py`.

### Flagged concerns
- **Flyway:** hooks must allow new `V__` / `R__` files and block edits to committed versioned migrations (checksums).
- **Lakshana:** spec command must call carnatic-musicologist for import/seed/lyric-structure work.
- **Secrets:** hook + `permissions.deny` both needed; Bash `cat .env` can still bypass Read tools.

### Open questions carried forward
None.

## Plan
**Status:** Accepted
**Accepted by:** User (asked to implement the listed artifacts)
**Accepted at:** 2026-08-24

### Files that change
- `conductor/tracks.md`, `conductor/tracks/TRACK-134-ai-native-sdlc-artifacts.md`
- `.agents/workflows/conductor-track-manager.md`
- `.claude/commands/spec-from-track.md`, `.claude/commands/plan-from-spec.md` + Cursor symlinks
- `REVIEW.md`, `CLAUDE.md`
- `.claude/settings.json`, `.claude/hooks/*.py`
- `evals/`, `Makefile`, `.github/workflows/ci.yml`
- `application_documentation/10-implementations/track-134-ai-native-sdlc-artifacts.md`

### Order of work
1. Track registry + template + commands.
2. REVIEW.md + CLAUDE.md verification.
3. Hooks.
4. Evals + CI + Makefile.
5. Implementation doc.

### Risks
Existing commit hooks must keep working. Hook JSON shapes differ between Claude Code and Cursor — scripts accept both.

### Proof
`python3 evals/check.py` exits 0. `python3 conductor/check-registry-sync.py` exits 0. New commands are symlinked from `.cursor/commands/`.

## Implementation Plan
- [x] Intent / Spec / Plan sections on the track template
- [x] `/spec-from-track` and `/plan-from-spec`
- [x] `REVIEW.md`
- [x] Verifying your work in CLAUDE.md
- [x] Edit-time hooks for `.env` and migrations (Claude Code + Cursor)
- [x] `evals/` + CI + `make agent-evals`

## Progress Log
- **2026-08-24**: Track created; implementation started from the six concrete artifacts.
- **2026-08-24**: Six artifacts landed (template, commands, REVIEW.md, verification block, hooks, evals).
- **2026-08-29**: Review I1–I4: Cursor `.cursor/hooks.json` + deny JSON; narrower `protect-tests`; Shell/StrReplace/Delete matchers and `.env` dump bypasses; CLAUDE.md `.env` policy vs `.env.example`.
- **2026-08-29**: I1–I4 fixes validated (evals 38/38, 13 hook probes). Status → Ready for review.
- **2026-08-29**: Review comment: allow tracked `.env.*.example` templates (e.g. `config/.env.auto-approval.example`) through `deny-secrets`; still deny `config/.env.auto-approval`.
