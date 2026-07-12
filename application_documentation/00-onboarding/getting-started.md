| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.2.0 |
| **Last Updated** | 2026-07-10 |
| **Author** | Sangeetha Grantha Team |

# Getting Started with Sangita Grantha

Welcome to the **Sangita Grantha** project. This document provides a comprehensive guide for new developers and contributors to set up their local development environment and understand our core workflows.

## 1. Project Overview

**Sangita Grantha** is the authoritative "System of Record" for Carnatic compositions. It is designed for longevity, musicological integrity, and high-performance access.

For current toolchain and library versions, see **[Current Versions](../00-meta/current-versions.md)**.

## 2. Prerequisites

Ensure you have the following installed on your system:

- **[mise](https://mise.jafp.info/)**: Our tool version manager (replaces `asdf`, `nvm`, etc.).
- **Docker & Docker Compose**: For running the full dev stack (DB, backend, frontend, extraction).
- **Python 3.11+**: For the extraction worker.
- **Bun**: For frontend package management and building.
- **JDK 25 (Temurin)**: For Kotlin and Android development. (See [Current Versions](../00-meta/current-versions.md))
- **Android Studio / Xcode**: For mobile development.

## 3. Local Environment Setup

Follow these steps to get your environment ready:

### 3.1 Tooling Installation
Run the following command to install the required tool versions managed by `mise`:
```bash
mise install
```

### 3.2 Database Setup
We use Docker Compose to run PostgreSQL. Migrations are managed by **Flyway** (see [ADR-013](../02-architecture/decisions/ADR-013-db-migration-with-flyway.md)):
```bash
# Full stack start (DB + Backend + Frontend + Extraction)
make dev

# Or database only
make db

# Reset DB (drop → create → migrate → seed)
make db-reset
```

> **PostgreSQL 18 volume layout.** The `db` service mounts its volume at
> `/var/lib/postgresql` (the `postgres:18+` image layout, volume `pgdata18`);
> fresh checkouts need nothing special. If your machine still has the pre-18
> `pgdata` volume (mounted at `.../data`, retired 2026-07-10), migrate once:
>
> ```bash
> # 1. With the OLD compose.yaml still checked out, back up:
> docker compose --profile dev up -d db
> docker exec sangeetha-grantha-db-1 pg_dump -U postgres -Fc sangita_grantha > sangita.dump
> # 2. Pull the new compose.yaml, then re-create the DB on the new volume:
> docker compose --profile dev down && docker compose --profile dev up -d --wait db
> docker exec -i sangeetha-grantha-db-1 pg_restore -U postgres -d sangita_grantha --no-owner < sangita.dump
> # 3. Verify, then remove the retired volume:
> docker volume rm sangeetha-grantha_pgdata
> ```

### 3.3 Frontend Dependencies
Install dependencies for the admin web module using `bun`:
```bash
cd modules/frontend/sangita-admin-web
bun install
```

## 4. Development Workflow

### 4.1 Running the Application

- **Full Dev Stack (recommended):**
```bash
  make dev          # Docker Compose: DB + Backend + Frontend + Extraction
  make dev-down     # Stop all services
```
- **Backend only:**
```bash
  ./gradlew :modules:backend:api:run
```
- **Frontend only:**
```bash
  cd modules/frontend/sangita-admin-web
  bun run dev
```
- **Database Migrations:**
```bash
  make migrate      # Run pending migrations
  make db-reset     # Drop → create → Flyway migrate (schema V__ + reference data R__)
```

### 4.2 The Conductor System
All work MUST be tracked via the Conductor system located in the `conductor/` directory.
1. Check `conductor/tracks.md` for active tracks.
2. If starting a new task, create a track file in `conductor/tracks/`.
3. Follow the implementation plan defined in your track.

### 4.3 Database Migrations
**Flyway is the only migration engine** ([ADR-013](../02-architecture/decisions/ADR-013-db-migration-with-flyway.md)). Never use Liquibase, ad-hoc SQL executors, or custom migration runners.
- All migrations live in `database/migrations/` (`VNN__description.sql`).
- Run migrations via: `make migrate`
- Reset the DB (Drop → Create → Flyway migrate, which applies schema + `R__` reference data) via: `make db-reset`
- Reference seed data ships as Flyway repeatable migrations (`R__seed_*.sql`); dev sample data via `make seed-dev`.
- History: Rust CLI (ADR-003) → Python `db-migrate` (ADR-010) → Flyway (ADR-013) — the SQL files survived every transition.

## 5. Coding Standards & Mandates

### 5.1 Backend (Kotlin)
- **Result Pattern:** Always use `Result<T, E>` for service layer returns. No exceptions for domain logic.
- **DTO Separation:** Never leak Exposed DAO entities to the API layer. Map to `@Serializable` DTOs in `modules/shared/domain`.
- **Database Access:** Use `DatabaseFactory.dbQuery { ... }`.

### 5.2 Frontend (React/TS)
- **Strict TypeScript:** No `any`. Use strict interfaces.
- **State Management:** Use `tanstack-query` for data fetching.

### 5.3 Commit Policy
Every commit message **MUST** include a reference to the relevant specification:
```text
feat: implement bulk import parser

Ref: application_documentation/01-requirements/features/bulk-import/01-strategy/csv-import-strategy.md
```

## 6. Project Structure

- `application_documentation/`: High-level requirements, architecture, and API specs.
- `conductor/`: Active development tracks and plans.
- `database/migrations/`: SQL migration files.
- `modules/backend/`: Ktor API and services.
- `modules/frontend/sangita-admin-web/`: React admin interface.
- `modules/shared/domain/`: KMP module with shared DTOs and logic.
- `archive/tools/db-migrate/`: Python migration tool (superseded by Flyway — [ADR-013](../02-architecture/decisions/ADR-013-db-migration-with-flyway.md), archived TRACK-110).
- `tools/krithi-extract-enrich-worker/`: Python extraction & enrichment worker.

## 7. Useful Links
- [Product Definition](../../conductor/product.md)
- [Tech Stack](../02-architecture/tech-stack.md)
- [Database Schema](../04-database/schema.md)