# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Sangeetha Grantha is a digital compendium of Carnatic classical music compositions (Krithis). It's a multi-module monorepo with:
- **Mobile**: Kotlin Multiplatform (iOS & Android)
- **Backend**: Kotlin + Ktor + Exposed ORM
- **Admin Web**: React 19 + TypeScript + Vite + Tailwind CSS
- **CLI Tooling**: Rust-based `sangita-cli` for database and development workflows

## Essential Commands

### Development Workflow (via mise - recommended)
```bash
# Full stack (DB + Backend + Frontend)
mise exec -- cargo run --manifest-path tools/sangita-cli/Cargo.toml -- dev --start-db

# Database reset (drop → create → migrate → seed)
mise exec -- cargo run --manifest-path tools/sangita-cli/Cargo.toml -- db reset

# Run migrations only
mise exec -- cargo run --manifest-path tools/sangita-cli/Cargo.toml -- db migrate

# End-to-end smoke test
mise exec -- cargo run --manifest-path tools/sangita-cli/Cargo.toml -- test steel-thread
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
./gradlew :modules:backend:api:seedDatabase  # Seed database
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
- JWT auth with role-based claims

**Frontend (React + TypeScript)**
- Function components with explicit TypeScript types
- Tailwind utility classes for styling
- `react-router-dom` for navigation

**Database**
- PostgreSQL 18+ (dev via Docker Compose)
- Migrations via Rust CLI (`tools/sangita-cli`), NOT Flyway
- Migration files in `database/migrations/`

## Critical Rules

1. **Never use Flyway or Liquibase** - always use `tools/sangita-cli` for migrations
2. **Dependency versions** - use `gradle/libs.versions.toml`, no hardcoded versions in build.gradle.kts
3. **Audit logging** - all backend mutations must log to `AUDIT_LOG` table
4. **Commit format** - every commit must include `Ref: application_documentation/...` line
5. **Version updates require documentation sync** - when updating dependency versions, run `sangita-cli docs sync-versions` and update any files that reference `current-versions.md`:
   - `application_documentation/00-meta/current-versions.md` (auto-generated)
   - `.cursorrules` (Core Technologies section)
   - `application_documentation/02-architecture/tech-stack.md`
   - `application_documentation/00-onboarding/getting-started.md`

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
- CLI reference: `tools/sangita-cli/README.md`