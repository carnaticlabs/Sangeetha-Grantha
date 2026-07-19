---
name: monorepo-orchestration
description: Dev-environment orchestration via mise, Makefile, and Docker Compose. Use when starting or debugging the dev stack, running cross-layer builds/tests, diagnosing toolchain or container issues, or deciding which layer (Python worker, Kotlin backend, Curator UI) owns a change.
---

# Monorepo Orchestration (mise + Makefile + Compose)

Toolchain versions are pinned in `.mise.toml` and mirrored in [current-versions.md](../../../application_documentation/00-meta/current-versions.md) — read them there, don't assume.

## Toolchain (mise)

`.mise.toml` pins Java (Temurin, matches the Gradle JVM toolchain), Bun (frontend), Python (worker; auto-creates a repo-root `.venv`), Rust (archived CLI only), and docker-compose. It also loads `config/postgres-local.env`. Setup: `mise trust` → `mise install`; diagnose with `mise doctor`. Docker Desktop/Engine is a separate system install.

## Makefile is the interface

Prefer `make` targets over raw docker/gradle invocations — they encode the correct env and ordering:

- Stack: `make dev` (DB + backend + frontend + extraction) · `make dev-down` · `make clean` (removes containers **and volumes**)
- Database: `make db` · `make db-reset` · `make migrate` · `make migrate-status` · `make seed-dev` · `make bootstrap-admin` (needs `ADMIN_EMAIL`/`ADMIN_PASSWORD`)
- Tests: `make test` (backend unit) · `make test-integration` · `make test-frontend` · `make steel-thread` (end-to-end) · `make check-docs`
- The user typically runs the full stack via `start-sangita.sh`.

## Docker Compose (compose.yaml)

Services: `db` (postgres 18-alpine, volume `pgdata18` at `/var/lib/postgresql` — pg18 layout), `migrate` (Flyway container), `bootstrap-admin`, `backend` / `backend-prod`, `frontend`, `extraction`. Ports: DB 5432, backend 8080, frontend 5001.

When a container doesn't reflect code changes: check volume mounts in `compose.yaml`, rebuild explicitly (`docker compose build <service>`), and clear Gradle caches before concluding the code is wrong (per CLAUDE.md debugging guidance).

## Layer ownership

**Intelligence in Python** (`tools/krithi-extract-enrich-worker` — extraction/enrichment), **Ingestion in Kotlin** (`modules/backend` — canonical writes, audit), **Review in Curator UI** (`modules/frontend/sangita-admin-web`). Put new logic in the layer that owns it rather than duplicating across layers.

## Build system

Gradle drives backend + KMP; every dependency version lives in `gradle/libs.versions.toml` — no hardcoded versions in any `build.gradle.kts`. Version bumps must also sync `current-versions.md` and the docs that reference it (see CLAUDE.md rule 5).
