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

## Cursor Cloud specific instructions

### Services overview

| Service | Port | How to run |
|---|---|---|
| PostgreSQL 18.3 | 5432 | `docker compose up -d db` (from repo root) |
| Ktor Backend API | 8080 | `./gradlew :modules:backend:api:run --no-daemon` |
| React Admin Frontend | 5001 | `bun run dev` (from `modules/frontend/sangita-admin-web`) |

### Gotchas

- **`gradlew` is gitignored.** The repo does not track the Gradle wrapper script. You must generate it before running any `./gradlew` command: `gradle wrapper --gradle-version 9.1.0` (requires Gradle 9.1.0 on PATH, installed at `/opt/gradle-9.1.0/bin`).
- **Java 25 (Temurin) is required.** The `build.gradle.kts` enforces `jvmToolchain(25)`. Set `JAVA_HOME=/usr/lib/jvm/temurin-25-jdk-amd64`.
- **DB defaults work out of the box.** The backend reads `DB_HOST=localhost`, `DB_USER=postgres`, `DB_PASSWORD=postgres`, `DB_NAME=sangita_grantha` from env or falls back to these defaults. No `.env` file is needed for local dev.
- **Migrations use the Python `db-migrate` tool**, not Flyway. Run via `PYTHONPATH=tools/db-migrate python3 -m db_migrate migrate` (or `make migrate`).
- **Dashboard 401 error is expected** when no JWT token is present — the dashboard endpoint requires auth. Reference data pages (Ragas, Composers, etc.) load without auth via the frontend proxy.
- **Frontend lint has pre-existing warnings/errors** — mainly in `e2e/` test files (unused vars, empty patterns). These are not blocking.
- **`bun test` picks up Playwright E2E files erroneously.** There are no Vitest unit tests in `src/`. E2E tests must be run separately with `bun run test:e2e` (requires full stack + Playwright browsers).
- **Docker socket permissions:** After Docker install, run `sudo chmod 666 /var/run/docker.sock` if you get permission errors.

### Standard commands

See `CLAUDE.md` and `Makefile` for the full command reference. Key commands:
- `make dev` — full Docker Compose stack
- `make db-reset` — drop + create + migrate
- `make seed` — seed reference data
- `make test` — backend tests
- `./gradlew :modules:backend:api:test` — backend tests (direct)
