# Codex Guide

This file is the entrypoint for OpenAI Codex / Codex CLI in this repository.

## Start Here
1. Read `.chatgpt-config.md` for canonical rules and architecture.
2. Use `.ai-quick-reference.md` for commands, stack versions, and module map.
3. Use `application_documentation/09-ai/vibe-coding-references.md` for specs.

## Non-Negotiables
- Run database migrations via `tools/sangita-cli` only (never Flyway/Liquibase).
- Wrap DB access in `DatabaseFactory.dbQuery { }`.
- Log all mutations to `audit_log`.
- Follow Conductor tracking in `conductor/` for non-trivial work.
- Commits must include `Ref: application_documentation/...` (see `.agent/workflows/commit-guardrails.md`).

## Local Skills and Workflows
Skills live in `.agent/skills/`:
- `change-mapper`
- `commit-policy`
- `documentation-guardian`

Workflow:
- `.agent/workflows/commit-guardrails.md`

## MCP Servers
Local MCP server definitions live in `config/mcp-servers.json` (Postgres).
