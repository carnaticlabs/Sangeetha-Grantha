| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-01-26 |
| **Author** | Sangita Grantha Architect |

# Getting Started with Sangita Grantha

Welcome to the **Sangita Grantha** project. This document provides a comprehensive guide for new developers and contributors to set up their local development environment and understand our core workflows.

## 1. Project Overview

**Sangita Grantha** is the authoritative "System of Record" for Carnatic compositions. It is designed for longevity, musicological integrity, and high-performance access.

- **Backend:** Kotlin (Ktor 3.4.0) + Exposed ORM + PostgreSQL 15+.
- **Frontend (Admin):** React 19 + TypeScript 5.8 + Tailwind CSS + Shadcn UI.
- **Mobile:** Kotlin Multiplatform (Compose Multiplatform) for Android & iOS.
- **Tooling:** Rust CLI (`tools/sangita-cli`) for database operations and dev orchestration.

## 2. Prerequisites

Ensure you have the following installed on your system:

- **[mise](https://mise.jafp.info/)**: Our tool version manager (replaces `asdf`, `nvm`, etc.).
- **Docker & Docker Compose**: For running the PostgreSQL database.
- **Rust Toolchain**: To build and run the `sangita-cli`.
- **Bun**: For frontend package management and building.
- **JDK 21+**: For Kotlin and Android development.
- **Android Studio / Xcode**: For mobile development.

## 3. Local Environment Setup

Follow these steps to get your environment ready:

### 3.1 Tooling Installation
Run the following command to install the required tool versions managed by `mise`:
```bash
mise install
```

### 3.2 Initialize the CLI
The `sangita-cli` is central to our development process. Build it and ensure it's functional:
```bash
cargo build --manifest-path tools/sangita-cli/Cargo.toml
```

### 3.3 Database Setup
We use Docker Compose to run PostgreSQL. The CLI manages migrations and seeding:
```bash
# Start the database container and run migrations/seed
mise exec -- cargo run --manifest-path tools/sangita-cli/Cargo.toml -- db reset
```

### 3.4 Configuration
Copy the sample configuration (if available) or create a local overrides file:
```bash
# Ensure config/application.local.toml exists with your local settings
# (Refer to config/ for templates)
```

### 3.5 Frontend Dependencies
Install dependencies for the admin web module using `bun`:
```bash
cd modules/frontend/sangita-admin-web
bun install
```

## 4. Development Workflow

### 4.1 Running the Application

- **Backend (API):**
  ```bash
  ./gradlew :modules:backend:api:run
  ```
- **Frontend (Admin):**
  ```bash
  cd modules/frontend/sangita-admin-web
  bun dev
  ```
- **Database Dev Mode:**
  ```bash
  mise exec -- cargo run --manifest-path tools/sangita-cli/Cargo.toml -- dev --start-db
  ```

- **Database Migrations:**
  ```bash
  mise exec -- cargo run --manifest-path tools/sangita-cli/Cargo.toml -- db migrate
  ```

### 4.2 The Conductor System
All work MUST be tracked via the Conductor system located in the `conductor/` directory.
1. Check `conductor/tracks.md` for active tracks.
2. If starting a new task, create a track file in `conductor/tracks/`.
3. Follow the implementation plan defined in your track.

### 4.3 Database Migrations
**NEVER** use Flyway or standard SQL executors for schema changes.
- All migrations live in `database/migrations/`.
- Run migrations via: `mise exec -- cargo run --manifest-path tools/sangita-cli/Cargo.toml -- db migrate`
- Reset the DB (Drop + Create + Migrate + Seed) via: `mise exec -- cargo run --manifest-path tools/sangita-cli/Cargo.toml -- db reset`

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
- `tools/sangita-cli/`: Rust-based developer tool.

## 7. Useful Links
- [Product Definition](../../conductor/product.md)
- [Tech Stack](../../conductor/tech-stack.md)
- [Database Schema](../04-database/schema.md)
