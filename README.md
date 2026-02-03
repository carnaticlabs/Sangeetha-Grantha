# 🎶 Sangeetha Grantha

| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.1.0 |
| **Last Updated** | 2026-02-03 |
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

## 🧩 Tech Stack & Versions

For a complete and specific list of versions and dependencies, please see **[Current Versions](./application_documentation/00-meta/current-versions.md)**.

### Core Technologies

| Layer | Technology |
|-------|------------|
| **Mobile** | Kotlin Multiplatform (KMM) + Compose Multiplatform |
| **Backend** | Kotlin + Ktor + Exposed |
| **Database** | PostgreSQL 15+ |
| **Migrations** | Rust CLI (`tools/sangita-cli`) |
| **Admin Web** | React + TypeScript + Tailwind + Vite |
| **Build** | Gradle (Backend/Mobile), Bun (Frontend) |
| **Toolchain** | Managed via [mise](https://mise.jdx.dev/) |

---

## 📂 Repository Structure

```text
├── modules/
│   ├── shared/                  # Shared domain models & UI (KMM)
│   ├── backend/
│   │   ├── api/                 # Ktor REST APIs
│   │   └── dal/                 # Data access layer (Exposed)
│   └── frontend/
│       └── sangita-admin-web/   # Admin web (React + TS)
├── database/
│   ├── migrations/              # SQL migration files
│   └── seed_data/               # Seed SQL files
├── tools/
│   └── sangita-cli/             # Rust CLI for DB management, dev workflow, testing
├── openapi/                     # OpenAPI specifications
├── application_documentation/   # PRDs, ERDs, architecture docs
├── config/                      # Environment configuration (TOML)
└── gradle/libs.versions.toml    # Centralized dependency management
```

---

## 🚀 Getting Started

> **💡 Quick Setup**: For complete setup instructions, see [Getting Started](./application_documentation/00-onboarding/getting-started.md).

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

This will set up the required toolchain and development environment.

**Start development stack** (recommended via mise):
```bash
# Via mise (ensures correct tool versions)

# Reset Database (Drop -> Create -> Migrate -> Seed)
mise exec -- cargo run --manifest-path tools/sangita-cli/Cargo.toml -- db reset

# Start development stack with database
mise exec -- cargo run --manifest-path tools/sangita-cli/Cargo.toml -- dev --start-db
```

For detailed setup, usage guides, troubleshooting, and CLI commands, see:
- **[Getting Started](./application_documentation/00-onboarding/getting-started.md)** — Complete setup guide
- **[Sangita CLI README](./tools/sangita-cli/README.md)** — Development workflow and commands

---

## 📜 Documentation

- **Product Requirements Document**: [Sangita Grantha PRD](./application_documentation/01-requirements/product-requirements-document.md)
- **API Spec**: [API Contract](./application_documentation/03-api/api-contract.md)
- **Database Schema**: [Schema Overview](./application_documentation/04-database/schema.md)
- **Architecture**: [Backend System Design](./application_documentation/02-architecture/backend-system-design.md)

---

## 🤖 AI & Vibe Coding Usage

This repository is designed to work seamlessly with AI coding assistants.

For comprehensive references and coding patterns, see: **[AI & Vibe Coding References](./application_documentation/09-ai/vibe-coding-references.md)**

---

## 🛣️ Roadmap

- ✅ Core schema & ingestion pipeline
- ✅ Admin editorial workflow
- ✅ AI Transliteration & Web Scraping
- 🔄 Mobile app development
- 🔄 Advanced lyric search & ranking
- 🔮 Media management (audio/notation)
- 🔮 Public read-only web experience

---

## 🙏 Credits & Inspiration

This project draws inspiration from decades of Carnatic scholarship and legacy sources such as karnatik.com, shivkumar.org, and various composer-centric archives.

**Sangeetha Grantha** exists to preserve, structure, and respectfully modernize this knowledge for future generations.

---

<div align="center">

**Sangeetha Grantha** — *where tradition meets thoughtful engineering* 🎵

</div>