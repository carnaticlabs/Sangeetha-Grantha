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
- **Migrations**: Rust-based SQL migration CLI (no Flyway)

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

## 🗂️ Data Model Highlights

- **Krithi**
  - Primary language (e.g. Sanskrit, Telugu, Tamil)
  - One or more ragas (Ragamalika)
  - Tala, Deity, Temple
- **Krithi Sections**
  - Pallavi, Anupallavi, Charanams, etc.
- **Lyric Variants**
  - Language & script
  - Sampradaya attribution
- **Tags / Themes**
  - Bhava, Festival, Philosophy, Kshetra, etc.
- **Temple Names**
  - Multilingual names and aliases for ingestion & search

---

## 🧩 Tech Stack

| Layer | Technology |
|-------|------------|
| Mobile | Kotlin Multiplatform + Compose |
| Backend | Kotlin + Ktor |
| ORM | Exposed |
| Database | PostgreSQL 15+ |
| Migrations | Rust CLI (SQL-based) |
| Admin Web | React + TypeScript + Tailwind |
| CI/CD | GitHub Actions |
| Cloud | AWS / GCP |

### Tech Stack Versions

#### Core Languages & Frameworks
- **Kotlin**: `2.3.0`
- **Compose Multiplatform**: `1.9.3`
- **Android Gradle Plugin (AGP)**: `8.13.2`

#### Backend Stack
- **Ktor**: `3.3.3`
- **Exposed**: `1.0.0-rc-4`
- **PostgreSQL**: `42.7.8`
- **HikariCP**: `7.0.2`
- **Logback**: `1.5.20`
- **Logstash Logback Encoder**: `8.0`
- **JWT (Auth0)**: `4.5.0`
- **Shadow Plugin**: `9.2.2`

#### Kotlinx Libraries
- **Kotlinx Coroutines**: `1.9.0`
- **Kotlinx DateTime**: `0.6.1`
- **Kotlinx Serialization JSON**: `1.7.3`

#### AndroidX & Material
- **Activity Compose**: `1.10.0`
- **AndroidX Core KTX**: `1.15.0`
- **Material**: `1.12.0`

#### Cloud & Authentication
- **AWS SDK**: `2.29.0`
- **Google Auth Library**: `1.23.0`

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
│   └── rust/                    # Rust CLI for DB migrations & seeds
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

# Run database migrations
cd database/rust
cargo run migrate

# Run backend API
./gradlew :modules:backend:api:runDev

# Run Admin Web
cd modules/frontend/sangita-admin-web
bun install
bun run dev
```

### Development CLI

```bash
# Start development environment
cd tools/sangita-cli && cargo run dev

# Run tests
cd tools/sangita-cli && cargo run test
```

Mobile apps are built via Android Studio (Android) and Xcode (iOS).

---

## 📜 Documentation

- **Product Requirements Document**: [`application_documentation/requirements/Sangita_Grantha_PRD.md`](application_documentation/requirements/Sangita_Grantha_PRD.md)
- **OpenAPI Spec**: [`openapi/sangita-grantha.openapi.yaml`](openapi/sangita-grantha.openapi.yaml)
- **Database Schema & ERDs**: [`application_documentation/database/`](application_documentation/database/)
- **Architecture & Blueprints**: [`application_documentation/backend/`](application_documentation/backend/)

---

## 🤖 AI & Copilot Usage

This repository is designed to work seamlessly with VS Code Copilot / Codex / Cursor / Google Antigravity.

**Key reference files:**

- `PROJECT_BLUEPRINT_SANGITA_GRANTHA.md`
- `Sangita Grantha – Product Requirements Document.md`
- `openapi/sangita-grantha.openapi.yaml`

These documents act as the source of truth for code generation.

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
- [shivkumar.org](http://shivkumar.org)
- Composer-centric archives and PDFs

**Sangeetha Grantha** exists to preserve, structure, and respectfully modernize this knowledge for future generations.

---

## 📄 License

TBD (to be finalized — likely a permissive open-source license).

---

<div align="center">

**Sangeetha Grantha** — *where tradition meets thoughtful engineering* 🎵

</div>
