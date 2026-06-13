# Codex Guide

Entrypoint for OpenAI Codex / Codex CLI in this repository.

> **Canonical source of truth: [`CLAUDE.md`](CLAUDE.md).** Architecture, build/test
> commands, the migration engine, audit-log and commit rules, ports, and key docs live
> there — read it first. This file holds only the Codex-specific delta; it deliberately
> does **not** restate the shared rules, so it cannot drift out of sync with them.

## Start Here
1. Read [`CLAUDE.md`](CLAUDE.md) — canonical rules and architecture.
2. `.chatgpt-config.md` — Codex/ChatGPT mirror of the canonical rules.
3. `.ai-quick-reference.md` — commands, stack versions, and module map.
4. `application_documentation/09-ai/vibe-coding-references.md` — specs.

## Non-Negotiables
These are summarized from CLAUDE.md (see [Critical Rules](CLAUDE.md#critical-rules) for the authoritative text):
- **Migrations:** Flyway only, via `make migrate` / `make db-reset` (ADR-013). Never Liquibase, never a custom runner, never the archived Rust CLI.
- Wrap all DB access in `DatabaseFactory.dbQuery { }`.
- Log every mutation to the `AUDIT_LOG` table.
- Track non-trivial work via Conductor (`conductor/tracks.md`).
- Every commit must include a `Ref: application_documentation/...` line.

## MCP Servers
Local MCP server definitions live in `config/mcp-servers.json` (Postgres).
