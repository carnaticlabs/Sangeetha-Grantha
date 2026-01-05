# 🎶 Sangeetha Grantha

*A Digital Compendium of Carnatic Classical Music*

---

## 📖 Overview

**Sangeetha Grantha** is an open, authoritative, multi-platform digital compendium of **Carnatic classical music compositions (Krithis)**.

The project aims to unify scattered, semi-structured sources into a **single, searchable, multilingual, and musicologically correct system**, with strong editorial governance and production-grade engineering.

It is designed to become the **system of record** for Carnatic Krithis — supporting composers, ragas, talas, sahitya, sampradaya, temples, and themes in a structured and extensible manner.

---

## 🎯 Key Objectives

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
  - Sampradaya (pāṭhāntaram / bani)
  - Primary language of composition
- Provide a clean **editorial workflow** for curation and review
- Deliver a modern, scalable, cloud-ready platform

---

## 🧱 System Architecture

### 📱 Clients

- **Mobile App**: Android & iOS using **Kotlin Multiplatform (KMM)**
- **Admin Web Console**: React + TypeScript + Tailwind CSS

### ⚙️ Backend

- **API**: Kotlin + Ktor (REST)
- **Database**: PostgreSQL 15+
- **Migrations**: Rust-based CLI tool (`tools/sangita-cli`) for database management (no Flyway)

### ☁️ Infrastructure

- AWS or Google Cloud
- CI/CD via GitHub Actions
- Centralized logging and audit trails

---

## ✨ Core Features

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
  - `DRAFT → IN_REVIEW → PUBLISHED → ARCHIVED`
- Data ingestion & normalization pipeline
- Full audit trail for all mutations

---

## 🗂️ Data Model
 
For the authoritative schema definition and detailed relationship models, please refer to:
- **[Schema Overview](./application_documentation/04-database/schema.md)**

---

## 🧩 Tech Stack

For a complete and specific list of versions and dependencies, please see **[Tech Stack Documentation](./application_documentation/02-architecture/tech-stack.md)**.

### Core Technologies

| Layer | Technology |
|-------|------------|
| **Mobile** | Kotlin Multiplatform (KMM) + Compose Multiplatform |
| **Backend** | Kotlin + Ktor + Exposed |
| **Database** | PostgreSQL 15+ |
| **Migrations** | Rust CLI (`tools/sangita-cli`) |
| **Admin Web** | React + TypeScript + Tailwind + Vite |
| **Build** | Gradle (Backend/Mobile), Bun (Frontend) |

---

## 📂 Repository Structure

```text
├── androidApp/                  # Android client (KMM)
├── iosApp/                      # iOS client (KMM)
├── modules/
│   ├── shared/                  # Shared domain models & UI (KMM)
│   ├── backend/
│   │   ├── api/                 # Ktor REST APIs
│   │   └── dal/                 # Data access layer (Exposed)
│   └── frontend/
│       └── sangita-admin-web/   # Admin web (React + TS)
├── database/
│   └── migrations/              # SQL migration files
├── tools/
│   └── sangita-cli/             # Rust CLI for DB management, dev workflow, testing
├── openapi/                     # OpenAPI specifications
├── application_documentation/   # PRDs, ERDs, architecture docs
├── config/                      # Environment configuration (TOML)
└── gradle/libs.versions.toml    # Centralized dependency management
```

---

## 🚀 Getting Started (Local Development)

### Prerequisites

- JDK 25+ (JVM Toolchain)
- Gradle (wrapper included)
- Bun
- Rust (stable)
- PostgreSQL 15+

### Setup

```bash
# Clone repository
git clone https://github.com/carnaticlabs/Sangeetha-Grantha.git
cd Sangeetha-Grantha

# Run database migrations using Sangita CLI
cd tools/sangita-cli
cargo run -- db migrate

# Or reset database (drop, create, migrate, seed)
cargo run -- db reset

# Start full development stack (DB + Backend + Frontend)
cargo run -- dev --start-db
```

### Development CLI

The Sangita CLI (`tools/sangita-cli`) provides unified commands for development:

```bash
cd tools/sangita-cli

# Database management
cargo run -- db migrate          # Run migrations only
cargo run -- db reset            # Reset database (drop → create → migrate → seed)
cargo run -- db health           # Check database health

# Development workflow
cargo run -- dev                 # Start Backend + Frontend
cargo run -- dev --start-db      # Start DB + Backend + Frontend

# Testing
cargo run -- test steel-thread   # Run end-to-end smoke tests

# Setup & verification
cargo run -- setup               # Check environment and dependencies
cargo run -- net info            # Show local network info

# Commit guardrails (workflow enforcement)
cargo run -- commit install-hooks    # Install Git hooks for commit validation
cargo run -- commit check             # Validate commit message format
cargo run -- commit scan-sensitive    # Scan staged files for sensitive data
```

**Commit Guardrails:** All commits must reference a documentation file in `application_documentation/`. See [`tools/sangita-cli/README.md`](tools/sangita-cli/README.md#commit-guardrails) for details.

For more details, see [`tools/sangita-cli/README.md`](tools/sangita-cli/README.md).

Mobile apps are built via Android Studio (Android) and Xcode (iOS).

---

## 📜 Documentation

- **Product Requirements Document**: [Sangita Grantha PRD](./application_documentation/01-requirements/product-requirements-document.md)
- **OpenAPI Spec**: [`openapi/sangita-grantha.openapi.yaml`](openapi/sangita-grantha.openapi.yaml)
- **Database Schema & ERDs**: [`application_documentation/04-database/`](application_documentation/04-database/)
- **Architecture & Blueprints**: [`application_documentation/02-architecture/`](application_documentation/02-architecture/)

---

## 🤖 AI & Vibe Coding Usage

This repository is designed to work seamlessly with VS Code Copilot / Codex / Cursor / Google Antigravity.

For comprehensive references and coding patterns, see: **[AI & Vibe Coding References](./application_documentation/09-ai/vibe-coding-references.md)**

This document includes:
- Product requirements and domain model references
- Architecture patterns and design guidelines
- API contracts and integration specs
- Database schema and migration strategies
- Frontend architecture and patterns
- Development workflow and commands
- Key coding patterns and constraints

---

## 🛣️ Roadmap

- ✅ Core schema & ingestion pipeline
- ✅ Admin editorial workflow
- 🔄 Mobile app UX refinement
- 🔄 Advanced lyric search & ranking
- 🔮 Notation & audio references (future)
- 🔮 Public read-only web experience

---

## 🙏 Credits & Inspiration

This project draws inspiration from decades of Carnatic scholarship and legacy sources such as:

- [karnatik.com](https://www.karnatik.com)
- [shivkumar.org/music](https://www.shivkumar.org/music/)
- Composer-centric archives and PDFs

**Sangeetha Grantha** exists to preserve, structure, and respectfully modernize this knowledge for future generations.

---

## 📄 License

TBD (to be finalized — likely a permissive open-source license).

---

<div align="center">

**Sangeetha Grantha** — *where tradition meets thoughtful engineering* 🎵

</div>
