# Sangeetha Grantha

| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.2.0 |
| **Last Updated** | 2026-03-10 |
| **Author** | Sangeetha Grantha Team |

---

*A Digital Compendium of Carnatic Classical Music*

---

## Overview

**Sangeetha Grantha** is an open, authoritative, multi-platform digital compendium of **Carnatic classical music compositions (Krithis)**.

The project unifies scattered, semi-structured sources into a **single, searchable, multilingual, and musicologically correct system**, with strong editorial governance and production-grade engineering.

It is designed to become the **system of record** for Carnatic Krithis — supporting composers, ragas, talas, sahitya, sampradaya, temples, and themes in a structured and extensible manner.

---

## Key Objectives

- Consolidate Carnatic Krithi data from multiple legacy sources
- Enable fast, accurate search across:
  - Krithi name / opening line
  - Lyrics (substring search)
  - Composer
  - Raga(s), including **Ragamalika**
  - Tala
  - Deity
  - Temple / Kshetram
- Preserve **musicological correctness**:
  - Pallavi / Anupallavi / multiple Charanams
  - Sampradaya (pathantaram / bani)
  - Primary language of composition
- Provide a clean **editorial workflow** for curation and review
- Deliver a modern, scalable, cloud-ready platform

---

## System Architecture

### Clients

- **Mobile App**: Android & iOS using **Kotlin Multiplatform (KMM)**
- **Admin Web Console**: React + TypeScript + Tailwind CSS

### Backend

- **API**: Kotlin + Ktor (REST)
- **Database**: PostgreSQL 18+
- **Migrations**: Python `db-migrate` tool (`tools/db-migrate`) — no Flyway

### Infrastructure

- AWS or Google Cloud
- CI/CD via GitHub Actions
- Centralized logging and audit trails

---

## Core Features

### Public (Read-only)

- Browse and search Krithis
- Structured lyrics:
  - Pallavi / Anupallavi / Charanams
  - Original script
  - Transliteration
  - Optional meaning
- Ragamalika support (ordered ragas)
- Multilingual sahitya
- Sampradaya-aware variants

### Admin (Restricted)

- CRUD for:
  - Krithis
  - Composers
  - Ragas
  - Talas
  - Deities
  - Temples (with multilingual names & aliases)
  - Tags / themes
  - Sampradaya
- Editorial workflow:
  - `DRAFT -> IN_REVIEW -> PUBLISHED -> ARCHIVED`
- Data ingestion & normalization pipeline
- Full audit trail for all mutations

---

## Data Model

For the authoritative schema definition and detailed relationship models, see:
- **[Schema Overview](./application_documentation/04-database/schema.md)**

---

## Tech Stack

For a complete and specific list of versions and dependencies, see **[Current Versions](./application_documentation/00-meta/current-versions.md)**.

### Core Technologies

| Layer | Technology |
|-------|------------|
| **Mobile** | Kotlin Multiplatform (KMM) + Compose Multiplatform |
| **Backend** | Kotlin + Ktor + Exposed |
| **Database** | PostgreSQL 18+ |
| **Migrations** | Python `db-migrate` (`tools/db-migrate`) |
| **Extraction** | Python worker (`tools/krithi-extract-enrich-worker`) |
| **Admin Web** | React + TypeScript + Tailwind + Vite |
| **Build** | Gradle (Backend/Mobile), Bun (Frontend) |
| **Orchestration** | Docker Compose (`compose.yaml`) |
| **Toolchain** | Managed via [mise](https://mise.jdx.dev/) |

---

## Repository Structure

```text
├── modules/
│   ├── shared/                          # Shared domain models & UI (KMM)
│   │   ├── domain/                      # @Serializable DTOs
│   │   └── presentation/               # Shared UI components
│   ├── backend/
│   │   ├── api/                         # Ktor REST APIs
│   │   └── dal/                         # Data access layer (Exposed)
│   └── frontend/
│       └── sangita-admin-web/           # Admin web (React + TS)
├── database/
│   ├── migrations/                      # SQL migration files
│   ├── seed_data/                       # Seed SQL files
│   ├── audits/                          # Data audit queries
│   └── for_import/                      # Import data & scripts
├── tools/
│   ├── db-migrate/                      # Python DB migration tool
│   ├── krithi-extract-enrich-worker/    # Python extraction pipeline
│   └── sangita-cli-archived/            # Archived Rust CLI (legacy)
├── openapi/                             # OpenAPI specifications
├── application_documentation/           # PRDs, ERDs, architecture docs
├── conductor/                           # Project tracking (tracks & phases)
├── config/                              # Environment configuration
└── gradle/libs.versions.toml            # Centralized dependency management
```

---

## Getting Started

> For complete setup instructions, see [Getting Started](./application_documentation/00-onboarding/getting-started.md).

**Prerequisites**:
- [mise](https://mise.jdx.dev/) (toolchain version manager)
- Docker Desktop (macOS/Windows) or Docker Engine (Linux)

**Development workflow (via Makefile)**:
```bash
# Start full dev stack (DB + Backend + Frontend + Extraction)
make dev

# Stop dev stack
make dev-down

# Database operations
make db              # Start database only
make db-reset        # Drop + create + migrate
make seed            # Seed reference data
make migrate         # Run pending migrations

# Testing
make test            # Backend tests
make test-frontend   # Frontend tests
make steel-thread    # E2E steel thread test

# Cleanup
make clean           # Remove all containers and volumes
```

**Manual frontend development**:
```bash
cd modules/frontend/sangita-admin-web
bun install
bun run dev          # Dev server on port 5001
```

For detailed setup, usage guides, and troubleshooting, see:
- **[Getting Started](./application_documentation/00-onboarding/getting-started.md)** — Complete setup guide
- **[Migration Tool](./tools/db-migrate/README.md)** — Database migration commands

---

## Default Ports

| Service | Port |
|---------|------|
| Database (PostgreSQL) | 5432 |
| Backend API | 8080 |
| Frontend Dev Server | 5001 |

---

## Documentation

- **Product Requirements**: [Sangita Grantha PRD](./application_documentation/01-requirements/product-requirements-document.md)
- **API Spec**: [API Contract](./application_documentation/03-api/api-contract.md)
- **Database Schema**: [Schema Overview](./application_documentation/04-database/schema.md)
- **Architecture**: [Backend System Design](./application_documentation/02-architecture/backend-system-design.md)
- **Current Versions**: [Tech Versions](./application_documentation/00-meta/current-versions.md)

---

## AI & Vibe Coding Usage

This repository is designed to work seamlessly with AI coding assistants.

For comprehensive references and coding patterns, see: **[AI & Vibe Coding References](./application_documentation/09-ai/vibe-coding-references.md)**

---

## Roadmap

- Done: Core schema & ingestion pipeline
- Done: Admin editorial workflow
- Done: AI Transliteration & Web Scraping
- Done: PostgreSQL 18 upgrade
- Done: Python migration & extraction tooling
- In Progress: Mobile app development
- In Progress: Advanced lyric search & ranking
- Planned: Media management (audio/notation)
- Planned: Public read-only web experience

---

## Credits & Inspiration

This project draws inspiration from decades of Carnatic scholarship and legacy sources such as karnatik.com, shivkumar.org, and various composer-centric archives.

**Sangeetha Grantha** exists to preserve, structure, and respectfully modernize this knowledge for future generations.
