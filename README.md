# 🎶 Sangeetha Grantha

| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-01-16 |
| **Author** | Sangeetha Grantha Team |

---

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
- **Database**: PostgreSQL 15 (dev pinned via Docker Compose) / 15+ (prod)
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
| **Database** | PostgreSQL 15 (dev pinned via Docker Compose) / 15+ (prod) |
| **Migrations** | Rust CLI (`tools/sangita-cli`) |
| **Admin Web** | React + TypeScript + Tailwind + Vite |
| **Build** | Gradle (Backend/Mobile), Bun (Frontend) |
| **Toolchain** | Managed via [mise](https://mise.jdx.dev/): Java 25, Rust 1.92.0, Bun 1.3.0, Docker Compose |

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

## 🚀 Getting Started

> **💡 Quick Setup**: For complete setup instructions, see [Cross-Platform Development Environment Standardisation](./application_documentation/01-requirements/features/cross-platform-development-environment-standardisation.md).

**Prerequisites**:
- [mise](https://mise.jdx.dev/) (toolchain version manager)
- Docker Desktop (macOS/Windows) or Docker Engine (Linux)

**One-command setup** (after installing mise):
```bash
# Unix/Linux/macOS
./tools/bootstrap

# Windows
powershell -ExecutionPolicy Bypass -File .\tools\bootstrap.ps1
```

This will install all required tools via mise (Java 25, Rust 1.92.0, Bun 1.3.0, Docker Compose) and set up the development environment.

**Start development stack** (recommended via mise):
```bash
# Via mise (ensures correct tool versions)

# Initialize Database
mise exec -- cargo run --manifest-path tools/sangita-cli/Cargo.toml -- db init

# Migrate / Set up seed Database
mise exec -- cargo run --manifest-path tools/sangita-cli/Cargo.toml -- db migrate

# Start development stack with database
mise exec -- cargo run --manifest-path tools/sangita-cli/Cargo.toml -- dev --start-db

# Or without mise (requires tools to be installed manually)
cd tools/sangita-cli
cargo run -- dev --start-db
```

For detailed setup, usage guides, troubleshooting, and CLI commands, see:
- **[Cross-Platform Development Environment Standardisation](./application_documentation/01-requirements/features/cross-platform-development-environment-standardisation.md)** — Complete setup guide
- **[Sangita CLI README](./tools/sangita-cli/README.md)** — Development workflow and commands

---

## 📜 Documentation

- **Product Requirements Document**: [Sangita Grantha PRD](./application_documentation/01-requirements/product-requirements-document.md)
- **Development Environment Setup**: [Cross-Platform Development Environment Standardisation](./application_documentation/01-requirements/features/cross-platform-development-environment-standardisation.md)
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
