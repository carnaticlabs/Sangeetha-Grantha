# Agents Guide

This repo ships local agent skills and workflows for consistent changes.

## Skills
- `.agent/skills/change-mapper/SKILL.md` - change scanning and categorization.
- `.agent/skills/commit-policy/SKILL.md` - commit rules and reference requirements.
- `.agent/skills/documentation-guardian/SKILL.md` - doc header, links, and formatting rules.

## Workflows
- `.agent/workflows/bulk-import-testing.md` - comprehensive testing workflow for the bulk import pipeline covering backend unit tests, scraping services, and E2E flows.
- `.agent/workflows/commit-guardrails.md` - standard workflow for committing changes with documentation guardrails enforcement.
- `.agent/workflows/conductor-track-manager.md` - manages the lifecycle of Conductor tracks (create, update, close) to streamline project management and documentation.
- `.agent/workflows/debug-start-application.md` - start the full stack application (Database, Backend, Frontend) with log redirection for analysis.
- `.agent/workflows/e2e-test-runner.md` - runs frontend E2E tests using Playwright, with options for headed mode, debugging, and report viewing.
- `.agent/workflows/pre-commit-validation.md` - validates staged changes against commit-policy before committing, ensuring documentation references and no secrets.
- `.agent/workflows/retrospective-commit-and-push.md` - categorize uncommitted changes, create track files and implementation summary docs, then commit in atomic chunks and push.
- `.agent/workflows/scaffold-service.md` - scaffolds a new backend service following the clean architecture pattern (Interface, Implementation, DI, Test).
- `.agent/workflows/start-application.md` - start the full stack application (Database, Backend, Frontend) and redirect logs.
- `.agent/workflows/test-troubleshooter.md` - systematic workflow for diagnosing and fixing test failures, addressing H2/Postgres compatibility and schema issues.
- `.agent/workflows/verify-db-status.md` - verify that the local database schema is in sync with the migration files.

## Commands
- `cargo run -- dev --start-db` (from `tools/sangita-cli`) - start full stack dev environment.
- `cargo run -- db reset` (from `tools/sangita-cli`) - reset local database.
- `cargo run -- test steel-thread` - run steel-thread tests.

## Context Files
- `.chatgpt-config.md` - canonical project rules (Codex/ChatGPT).
- `.ai-quick-reference.md` - quick commands, stack versions, module map.
- `.ai-context-guide.md` - cross-assistant index of context files.
- `CODEX.md` - Codex entrypoint for this repo.
