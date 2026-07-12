# Agents Guide

> **Canonical source of truth: [`CLAUDE.md`](CLAUDE.md).**
> Build commands, architecture, the migration engine (Flyway / ADR-013), audit-log
> and commit rules, ports, and key docs all live there. **Read it first.**
> Do **not** copy those rules into this file — duplicating them is exactly what made
> the agent files drift out of sync before. This file carries only the delta that
> CLAUDE.md does not: the catalog of local agent skills and workflows shipped in the repo.

Tool-specific entrypoints that build on CLAUDE.md:
- **Codex / ChatGPT** → [`CODEX.md`](CODEX.md)
- **Gemini** → [`GEMINI.md`](GEMINI.md)
- **Goose** → [`GOOSE.md`](GOOSE.md)
- **Claude Code** → [`CLAUDE.md`](CLAUDE.md) (canonical)

## Local Skills (`.agent/skills/`)
- `change-mapper` — change scanning and categorization.
- `commit-policy` — commit rules and reference requirements.
- `data-quality-audit` — data integrity / quality checks.
- `documentation-guardian` — doc header, links, and formatting rules.
- `extraction-debugger` — diagnose the krithi extraction pipeline.
- `ingestion-pipeline-guardian` — guard the Kotlin ingestion path.

(`.cursor/skills/` additionally ships `agentic-prompt-optimizer` and `sangeetha-krithi-analyser`.)

## Workflows (`.agent/workflows/`)
- `agentic-prompt-optimizer.md` — rewrite an informal request into a structured, tool-friendly prompt.
- `bulk-import-testing.md` — end-to-end testing workflow for the bulk import pipeline.
- `conductor-track-manager.md` — manage the lifecycle of Conductor tracks (create, update, close).
- `debug-start-application.md` — start the full stack with log redirection for analysis.
- `e2e-test-runner.md` — run frontend E2E tests (Playwright), with headed/debug/report options.
- `generate-commit-prompt.md` — generate a commit message that satisfies commit-policy.
- `pre-commit-validation.md` — validate staged changes against commit-policy before committing.
- `retrospective-commit-and-push.md` — categorize uncommitted changes, create tracks/docs, commit atomically, push.
- `scaffold-service.md` — scaffold a backend service following the clean-architecture pattern.
- `start-application.md` — start the full stack (Database, Backend, Frontend) and redirect logs.
- `test-troubleshooter.md` — systematically diagnose and fix test failures.
- `verify-db-status.md` — verify the local database schema matches the migration files.

## Commands
See [`CLAUDE.md` → Essential Commands](CLAUDE.md#essential-commands). The full stack runs via
the Makefile + Docker Compose (`make dev`, `make db-reset`, `make migrate`); database work goes
through **Flyway** per ADR-013. The historical Rust CLI (`tools/sangita-cli`) is **archived** under
`archive/tools/sangita-cli/` — do not invoke it.

## Context Files
- [`CLAUDE.md`](CLAUDE.md) — canonical project rules (all assistants).
- `.chatgpt-config.md` — Codex/ChatGPT canonical rules mirror.
- `.ai-quick-reference.md` — quick commands, stack versions, module map.
- `.ai-context-guide.md` — cross-assistant index of context files.
