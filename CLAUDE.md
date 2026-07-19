# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Sangeetha Grantha is a digital compendium of Carnatic classical music compositions (Krithis). It's a multi-module monorepo with:
- **Mobile**: Kotlin Multiplatform (iOS & Android)
- **Backend**: Kotlin + Ktor + Exposed ORM
- **Admin Web**: React 19 + TypeScript + Vite + Tailwind CSS
- **CLI Tooling**: Flyway for migrations (ADR-013) + Makefile for dev workflows

This is a Carnatic Music Krithi Analyser project using: Kotlin backend (Exposed ORM), React/TypeScript frontend, Python data pipeline scripts, PostgreSQL database. Always verify changes compile/build across all three layers before committing.

## Essential Commands

### Development Workflow (via Makefile)
```bash
make dev            # Full stack via Docker Compose (DB + Backend + Frontend + Extraction)
make dev-down       # Stop dev stack
make db             # Start database only
make db-reset       # Drop → create → Flyway migrate (schema V__ + reference data R__)
make seed-dev       # Dev-only sample content (reference data now ships via R__ repeatables)
make migrate        # Run pending migrations (Flyway)
make migrate-status # Show Flyway migration status
make bootstrap-admin # Provision the admin user (argon2id); needs ADMIN_EMAIL / ADMIN_PASSWORD
make test           # Backend unit tests
make test-integration # Backend integration tests (Testcontainers)
make test-frontend  # Frontend tests
make steel-thread   # End-to-end steel thread test
make check-docs     # Validate documentation links
make clean          # Remove all containers and volumes
```

### Build & Test
```bash
# Backend build (produces fat JAR)
./gradlew :modules:backend:api:build

# Backend tests
./gradlew :modules:backend:api:test

# Frontend (from modules/frontend/sangita-admin-web)
bun install
bun run dev      # development server on port 5001
bun run build    # production build
```

### Gradle Tasks
```bash
./gradlew :modules:backend:api:run        # Run backend
./gradlew :modules:backend:api:runDev     # Run backend in dev mode
```

## Architecture

### Module Structure
```
modules/
├── shared/
│   ├── domain/          # KMP domain models (@Serializable DTOs)
│   └── presentation/    # Shared UI components
├── backend/
│   ├── api/             # Ktor routes (main class: AppKt)
│   └── dal/             # Data access layer (Exposed ORM)
└── frontend/
    └── sangita-admin-web/  # React admin console
```

### Key Patterns

**Backend (Kotlin + Ktor)**
- Routes delegate to services/repositories
- Use `DatabaseFactory.dbQuery { }` for all database operations
- Return explicit DTOs, not Exposed entity objects
- All mutations must write to `AUDIT_LOG` table
- JWT auth with role-based claims; admin routes are gated by `requireRole` (ADR-004 v1.3) — roles are
  derived from stored `role_assignments`, never from the request

**Frontend (React + TypeScript)**
- Function components with explicit TypeScript types
- Tailwind utility classes for styling
- `react-router-dom` for navigation

**Database**
- PostgreSQL 18+ (dev via Docker Compose)
- Migrations via **Flyway Community** (`make migrate` / `make db-reset`) per ADR-013; the custom Python `db-migrate` and the test-side Kotlin `MigrationRunner` are retired
- Migration files in `database/migrations/` (`VNN__description.sql`)

## Critical Rules

1. **Flyway is the only migration engine** (ADR-013) - always go through `make migrate` or `make db-reset`; never Liquibase, never ad-hoc SQL executors, never custom migration runners. *Rationale for the 2026-06 switch: the previous custom tooling had forked into two diverging implementations (Python `db-migrate` for dev/prod, Kotlin `MigrationRunner` for tests) with incompatible tracking tables and no checksum validation on the test path; Flyway gives one standards-based engine for Kotlin (JVM API in Testcontainers), Python (CLI), Make, and CI, plus repeatable migrations for reference seed data. See `application_documentation/02-architecture/decisions/ADR-013-db-migration-with-flyway.md`.*
2. **Dependency versions** - use `gradle/libs.versions.toml`, no hardcoded versions in build.gradle.kts
3. **Audit logging** - all backend mutations must log to `AUDIT_LOG` table
4. **Commit format** - every commit must include `Ref: application_documentation/...` line
5. **Version updates require documentation sync** - update any files that reference `current-versions.md`:
   - `application_documentation/00-meta/current-versions.md`
   - `application_documentation/02-architecture/tech-stack.md`
   - `application_documentation/00-onboarding/getting-started.md`

## Musicological Domain Rules

Carnatic correctness (*lakshana*) — musical forms (KRITHI/VARNAM/SWARAJATHI section requirements), Ragamalika, notation-vs-lyrics, and raga/tala/terminology rules — is documented in [Domain Model §6](application_documentation/01-requirements/domain-model.md#6-musicological-correctness-rules-lakshana). Treat it as a correctness contract for data entry, extraction, validation, and any generated SQL/seed data.

## Claude Code Assets

This file stays canonical for cross-cutting rules; layer-specific conventions live in project skills so they load only when relevant. Don't duplicate content between the two — link.

### Layer Skills (`.claude/skills/`)

Load the matching skill before working in a layer instead of re-deriving its conventions:

- `kmp-compose-mobile` — `modules/shared/` (KMP targets, expect/actual, Compose rules)
- `ktor-exposed-backend` — `modules/backend/` (layering, dbQuery, DTO, audit, auth non-negotiables)
- `postgres-flyway-db` — `database/migrations/` (V__/R__ naming, PG18 conventions, seeding checks)
- `react-vite-frontend` — `modules/frontend/sangita-admin-web/` (Bun-only tooling, strict TS, test commands)
- `python-extraction-worker` — `tools/krithi-extract-enrich-worker/` (Pydantic paradigm, module map, uv)
- `monorepo-orchestration` — mise/Makefile/Compose workflows and layer ownership
- `verify-import` — post-import data verification checklist

### Slash Commands (`.claude/commands/`)

`/dev-start`, `/db-reset`, `/test-all`, `/steel-thread`, `/new-migration`, `/commit` (follows this repo's commit conventions), `/Sangeetha-Krithi-Analyser` (krithi section analysis).

### Specialist Subagents (`.claude/agents/`)

Delegated to on demand (not loaded every turn): `kotlin-backend-engineer`, `postgres-engineer`, `python-engineer`, and `carnatic-musicologist` (reviews krithi *data* for musicological correctness — useful for bulk-import batches). They defer to this file and the domain model for shared rules and add domain judgment on top.

### Dev Servers & Preview (`.claude/launch.json`)

Named launch configs exist for `frontend` (port 5001), `backend` (8080), and `full-stack` (`make dev`). Start dev servers through the browser-preview tooling with these names — never as raw background Bash — then verify changes in the preview (console/network/page checks) rather than asking the user to check manually.

## Conductor Workflow

Work is tracked in `conductor/tracks/TRACK-<ID>-<slug>.md` files. Check `conductor/tracks.md` for active tracks before starting work.

## Toolchain & Versions

For current toolchain and library versions, see [Current Versions](application_documentation/00-meta/current-versions.md).

## Default Ports

- Database: 5432
- Backend API: 8080
- Frontend: 5001

## Key Documentation

- Onboarding: `application_documentation/00-onboarding/getting-started.md`
- Architecture: `application_documentation/02-architecture/`
- Database schema: `application_documentation/04-database/schema.md`
- API spec: `openapi/sangita-grantha.openapi.yaml`
- Migration approach (Flyway): `application_documentation/02-architecture/decisions/ADR-013-db-migration-with-flyway.md`
- Integration testing strategy: `application_documentation/07-quality/integration-tests-approach.md`
- Previous migration tools (archived): `archive/tools/db-migrate/` (Python, superseded by ADR-013), `archive/tools/sangita-cli/` (Rust)

## Debugging Guidelines 
For CORS/auth issues, always check .env files and VITE_API_BASE_URL first, not TOML config files. Frontend proxy configuration is the most common root cause.

 ## Data & Migrations 
 Always verify seed data populates junction tables (e.g., krithi_ragas), not just foreign key columns on the main entity. After any seed/migration, confirm data appears correctly through the full stack (DB → API → UI).
## Git & Commits 
Follow commit-policy skill conventions strictly: include TRACK references, use proper formatting. Never create commits without checking the project's commit message conventions first.

## Infrastructure & Docker 
When debugging Docker/infrastructure issues: check Dockerfile base image versions match project requirements, verify volume mount paths for the current DB version, and ensure Gradle caches are cleared before assuming code changes aren't taking effect.
