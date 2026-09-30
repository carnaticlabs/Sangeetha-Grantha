---
name: agentic-prompt-optimizer
description: Rewrites a user's rough or informal task into a structured, agent-optimized prompt (goal, scope, file paths, verifiable steps, tooling hints, deliverables, and acceptance criteria). Use when preparing tasks for autonomous AI agents (Gemini, Antigravity, Claude, Composer) or before starting complex multi-file tasks.
---

# Agentic Prompt Optimizer

Turn an informal or unstructured request into a **single, executable prompt** that an autonomous AI agent can run end-to-end using tools (parallel file reads, search, terminal commands, and verifiable checklists).

## When to Apply

- The user provides an ambiguous or high-level request ("fix extraction", "align with the report", "clean up the models").
- The task spans multiple files, Conductor tracks, or complex verification steps.
- The user asks to optimize a prompt for Gemini / Antigravity / Claude / Cursor Composer.
- Before embarking on a large multi-step refactor or cross-layer investigation.

## Principles (Gemini & Agentic Best Practices)

1. **Executable, Not Advisory**: Prefer concrete commands and actions ("read X, compare with Y, modify Z, run tests") over conversational suggestions.
2. **Bound Scope**: Explicitly define what is **in scope**, **out of scope**, and the clear **stop conditions**.
3. **Pin Paths & Symbols**: Use exact repo-relative or absolute paths, module names, and symbol references.
4. **Decompose Sequentially**: Break work into ordered steps where each step produces a verifiable outcome.
5. **Tool Affordances**: Guide the agent on optimal tool use (e.g. codebase grep, ripgrep, AST lookups, MCP tools).
6. **Verifiable Proof**: Require specific commands (tests, lint, curl, SQL checks) that definitively prove completion.
7. **Adhere to Repo Standards**: Enforce project conventions (Conductor tracks, Flyway migrations, commit policy `Ref:` lines).

## Output Template

Generate a **ready-to-paste prompt** using this markdown skeleton:

```markdown
## Goal
[One clear sentence defining what "done" means.]

## Context
- Repository Root: [Repo path or relative context]
- Architecture / Rules: [e.g. CLAUDE.md, .agents/AGENTS.md, Conductor track, commit policy]

## Inputs & References
- Read First: [Exact file paths to read]
- Reference Docs: [Relevant specs, schemas, or fixtures]

## Out of Scope
[Explicit list of files, modules, or features not to touch]

## Steps (Execute in Order)
1. [Step 1: Specific action and expected finding/artifact]
2. [Step 2: ...]
3. [Step 3: ...]

## Deliverables
- [Files to create or update]
- [Documentation or Conductor track updates]

## Verification
- Commands: [e.g., `./gradlew test`, `bun run test:e2e`, `make migrate`]
- Success Criteria: [Exact expected output, exit code 0, or query result]
```

## Workflow

1. **Extract Intent**: Determine the core objective, affected layers (backend, frontend, DB, worker), and risk level.
2. **Identify Missing Context**: If crucial file paths or acceptance criteria are unspecified, infer sensible defaults labeled `[assumption]` or list up to 3 concise clarifying questions.
3. **Draft the Prompt**: Populate the template with imperative instructions and verified project file paths.
4. **Add Agentic Guardrails**: Include explicit commands, tests, and security constraints (e.g., no staging secrets).

## Example

**User Request (Informal):**
> "Scan EXTRACTION-INVESTIGATION-REPORT.md and compare against the code and create detailed TRACK entries for fixing the issues."

**Optimized Agentic Prompt:**

```markdown
## Goal
For each actionable defect documented in `database/for_import/EXTRACTION-INVESTIGATION-REPORT.md`, verify current behavior against the codebase and create or update a Conductor track with concrete fix scopes.

## Inputs & References
- Read: `database/for_import/EXTRACTION-INVESTIGATION-REPORT.md`
- Registry: `conductor/tracks.md`
- Extraction Worker: `tools/krithi-extract-enrich-worker/`
- Backend Processor: `modules/backend/api/src/main/kotlin/com/sangita/grantha/backend/api/services/ExtractionResultProcessor.kt`

## Steps
1. Parse `EXTRACTION-INVESTIGATION-REPORT.md` into a structured inventory of distinct defects (title, severity, suggested fix).
2. For each defect: locate the responsible module(s) using codebase search and verify if the issue still exists or was addressed.
3. For open gaps: scaffold a new track file `conductor/tracks/TRACK-<next>-<slug>.md` using the standard Conductor template with Intent, Spec, and Plan sections.
4. Register each new track in `conductor/tracks.md`.

## Deliverables
- New `conductor/tracks/TRACK-*.md` files with clear acceptance criteria.
- Updated `conductor/tracks.md` registry.

## Verification
- Validate markdown links and verify all track IDs are unique and sequentially ordered.
- Run `uv run pytest` in `tools/krithi-extract-enrich-worker/` if any tests are touched.
```
