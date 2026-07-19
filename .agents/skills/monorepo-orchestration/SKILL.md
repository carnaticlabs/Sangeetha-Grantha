---
name: monorepo-orchestration
description: Guidelines for monorepo development orchestration using Mise, Makefile, and Compose.
---

# Monorepo Orchestration & Toolchain Guidelines

This skill enforces standards for environment configuration, services build, and orchestration across the Sangeetha Grantha monorepo.

## 1. Toolchain Management via Mise
The local development environment is managed using **`mise`** (defined in `.mise.toml`):
- **Tool Versions**:
  - Java: `temurin-25` (JVM toolchain version 25)
  - Python: `3.14` (for `krithi-extract-enrich-worker`; must stay ≥ the worker's `requires-python = ">=3.14"`)
  - Bun: `1.3.7` (for React admin frontend)
  - Rust: `1.93.0` (archived tool legacy)
- **Setup & Usage**:
  - Run `mise install` to install pinned runtimes.
  - Run `mise doctor` to diagnose activation issues.
  - Run `mise trust` if changes to `.mise.toml` require local approval.

## 2. Docker Compose Orchestration
The monorepo development stack is defined in `compose.yaml`:
- **Active Services**:
  - `db`: PostgreSQL 18+ database.
  - `backend`: Kotlin backend service.
  - `frontend`: React admin web service.
  - `extraction`: Python extraction worker.
- **Container Hygiene**:
  - Check volume mount mappings in `compose.yaml` to ensure code changes propagate.
  - If a service fails to reflect code updates, rebuild the container explicitly via `docker compose build <service>`.

## 3. Dev Workflow Command Map (Makefile)
Always use the standardized Makefile commands for development tasks:
- `make dev`: Starts the complete development stack (DB + Backend + Frontend + Extraction).
- `make dev-down`: Stops and tears down the active dev containers.
- `make db`: Starts the PostgreSQL container only.
- `make db-reset`: Drops, recreates, and runs Flyway migrations to reset the database.
- `make migrate`: Runs any pending Flyway migrations.
- `make test`: Runs backend Gradle test suites.
- `make test-frontend`: Runs frontend Vitest and Playwright test suites.

## 4. Gradle Build System
- Backend and Mobile shared builds are managed using **Gradle**.
- Avoid hardcoding library versions in individual `build.gradle.kts` files; all versions must be declared in the central catalogs (`gradle/libs.versions.toml`).
