---
name: agentic-prompt-optimizer
description: >-
  Rewrites a user's rough task into a structured, agent-optimized prompt: clear
  goal, scope, file paths, verifiable steps, tooling, outputs, and constraints.
  Use when the user asks to improve a prompt, make a task clearer for an AI
  agent, "agentic" or "Composer/Claude/GPT" prompt prep, or before large
  multi-file tasks (tracks, refactors, investigations).
---

# Agentic Prompt Optimizer

Turn an informal request into a **single message** an agent can execute with tools: parallel reads, search, terminal commands, and checklists.

## When to apply

- The user pastes a vague goal ("fix extraction", "align with the report").
- The task spans many files, Conductor tracks, or verification steps.
- The user explicitly wants a better prompt for Cursor / Composer / Claude / GPT.

## Principles (model-agnostic)

1. **Executable, not advisory** — Prefer "read X, compare to Y, edit Z, run tests" over "think about".
2. **Bound scope** — State **in scope**, **out of scope**, and **stop conditions**.
3. **Pin locations** — Absolute or repo-relative paths, module names, symbols to search.
4. **Decompose** — Ordered steps; each step has a checkable outcome.
5. **Tool affordances** — Suggest parallel exploration when steps are independent; name likely `grep`/`semantic search` targets.
6. **Verification** — Tests, commands, or file diffs that prove done-ness.
7. **Project rules** — Mention repo-specific requirements (e.g. `conductor/tracks.md`, `Ref:` lines) when relevant.

## Output template

Produce a **ready-to-paste prompt** using this skeleton (omit sections that do not apply):

```markdown
## Goal
[One sentence: what "done" means.]

## Context
- Repo: [path or assume workspace root]
- Must follow: [e.g. Conductor tracks, commit policy, docs under application_documentation/]

## Inputs
- Read first: [files]
- Optional: [reports, fixtures]

## Out of scope
[What not to change]

## Steps (execute in order)
1. [Step — expected artifact or finding]
2. ...

## Parallel work (if applicable)
- Track A: ...
- Track B: ...

## Deliverables
- [Files to create/update and format]
- [Registry updates, e.g. conductor/tracks.md]

## Verification
- Commands: [...]
- Success: [...]
```

## Workflow

1. **Extract intent** — Goal, urgency, audience (implementer vs reviewer).
2. **Inventory missing facts** — If paths or success criteria are missing, list **up to 3** concise questions *or* assume defaults and label them `[assumption]`.
3. **Rewrite** — Fill the template; use imperative verbs; reference real paths from the user message.
4. **Add agentic hints** — e.g. "Use codebase search for …", "Run tests in … after edits".

## Example (condensed)

**User (rough):**  
"Scan EXTRACTION-INVESTIGATION-REPORT.md and compare against the code and create detailed TRACK entries for fixing the issues."

**Optimized (snippet):**

```markdown
## Goal
For each actionable issue in `database/for_import/EXTRACTION-INVESTIGATION-REPORT.md`, either map it to existing behavior in the repo or define a new Conductor track with a concrete fix scope.

## Inputs
- Read: the report above; `conductor/tracks.md`; relevant code under `tools/krithi-extract-enrich-worker/` (and paths cited in the report).

## Steps
1. Parse the report into a numbered list of issues (title, severity, suggested fix).
2. For each issue: locate the responsible module(s) with search and confirm current behavior with tests or a minimal repro fixture.
3. For gaps: add `conductor/tracks/TRACK-<next>-<slug>.md` per issue cluster; update `conductor/tracks.md`. Each track: Goal, Context (link report section), Implementation plan, Progress log.

## Deliverables
- New/updated TRACK files + registry rows; cross-links from track to report sections.

## Verification
- `pytest` (or project-standard tests) for touched Python; markdown links relative and valid.
```

## Anti-patterns

- A single blob of prose with no headings or acceptance criteria.
- "Be thorough" without **what** to verify or **where** to look.
- Duplicating entire repo guidelines — **point** to `.cursorrules` / `AGENTS.md` instead of pasting them.

## See also

- Project workflows: `AGENTS.md`
- Conductor: `conductor/tracks.md`, `.agent/workflows/conductor-track-manager.md`
