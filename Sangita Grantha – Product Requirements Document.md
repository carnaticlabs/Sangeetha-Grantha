# Sangita Grantha – Product Requirements Document (PRD)

---

## 1. Executive Summary

**Sangita Grantha** is a multi-platform, authoritative digital compendium of Carnatic classical music compositions (Krithis). The platform consolidates scattered, semi-structured sources into a single, searchable, multilingual system with strong editorial governance and musicological correctness.

The system consists of:
- **Public Mobile App** (Android & iOS via Kotlin Multiplatform)
- **Backend API Platform** (Ktor + PostgreSQL)
- **Restricted Admin Web Console** (React + TypeScript + Tailwind)
- **Data Ingestion & Normalization Pipeline** for legacy sources

**Primary Objective**  
Establish Sangita Grantha as the *system of record* for Carnatic Krithis, combining scholarly rigor with production-grade software engineering.

---

## 2. Problem Statement

Carnatic music knowledge currently exists across:
- Static websites (karnatik.com, shivkumar.org)
- Blogspot composer lists (Tyagaraja, Dikshitar, Syama Sastri, etc.)
- PDFs and scanned documents

Key issues:
- No unified or normalized schema
- Poor searchability (especially by lyrics)
- No consistent multilingual representation
- No handling of ragamalika, sampradaya, or lyric variants
- No editorial workflow or provenance tracking

---

## 3. Goals & Non-Goals

### 3.1 Goals
- Unified, normalized Krithi catalog
- Fast, multi-field search:
  - Krithi name / opening line
  - Lyrics substring
  - Composer
  - Raga(s)
  - Tala
  - Deity
  - Temple / kshetram
- Multilingual sahitya with structured sections
- Support for ragamalika and multiple charanams
- Sampradaya-aware lyric variants
- Strong admin editorial workflow
- Cloud-ready, scalable architecture

### 3.2 Non-Goals (v1)
- Audio streaming or notation playback
- Community edits or crowdsourcing
- Monetization

---

## 4. Personas

### 4.1 Rasika / Learner (Public User)
- Searches and browses Krithis
- Reads lyrics and metadata
- Uses mobile app only
- Read-only access

### 4.2 Editor (Admin)
- Creates and edits Krithis
- Fixes metadata, lyrics, sections, tags
- Cannot publish directly

### 4.3 Reviewer (Admin)
- Reviews drafts
- Publishes canonical versions

### 4.4 Admin (System)
- Full access
- Manages users, roles, taxonomies, and archival

---

## 5. High-Level Architecture

### Clients
- Android App (Kotlin Multiplatform)
- iOS App (Kotlin Multiplatform)
- Admin Web (React + TypeScript)

### Backend
- Ktor REST API
- JWT Authentication (RBAC)
- PostgreSQL
- Rust-based DB migrations

### Infrastructure
- AWS or GCP
- CI/CD via GitHub Actions
- Centralized logging and audit trails

---

## 6. Functional Requirements

---

### 6.1 Public Mobile App

#### Core Features
- Search Krithis by:
  - Name / opening line
  - Lyrics substring
  - Composer
  - Raga(s) (including ragamalika)
  - Tala
  - Deity
  - Temple / kshetram

- View Krithi details:
  - Metadata:
    - Composer
    - Tala
    - Primary language of composition
    - Deity and temple
  - Raga presentation:
    - Single raga OR ordered ragas (ragamalika)
  - Lyrics:
    - Structured by sections (Pallavi / Anupallavi / Charanams)
    - Original script
    - Transliteration
    - Optional meaning
  - Multiple lyric variants (read-only)

#### Non-Functional
- Read-only access
- Offline caching (favorites)
- Fast initial load
- Deterministic ordering of sections and ragas

---

### 6.2 Admin Web Console

#### Capabilities
- Secure login (JWT)
- CRUD for:
  - Krithis
  - Composers
  - Ragas
  - Talas
  - Deities
  - Temples (canonical + multilingual names)
  - Tags (controlled taxonomy)
  - Sampradaya

- Krithi structure management:
  - Pallavi / Anupallavi / multiple Charanams
  - Section ordering

- Lyric management:
  - Multiple languages & scripts
  - Transliteration and translation
  - Section-wise editing
  - Variant tracking with sampradaya attribution

- Metadata enrichment:
  - Primary language of composition
  - Themes / bhava / festival / philosophy tags

- Workflow states:
  - `DRAFT`
  - `IN_REVIEW`
  - `PUBLISHED`
  - `ARCHIVED`

- Import review queue
- Audit log viewer

#### UX Expectations
- Explicit form-driven editing
- Clear save / submit / publish actions
- No implicit auto-publish
- Destructive actions require confirmation

---

### 6.3 Backend API

#### Core REST Endpoints (v1)
```
GET  /health

GET  /v1/krithis/search
GET  /v1/krithis/{id}

POST /v1/krithis              (admin)
PUT  /v1/krithis/{id}         (admin)

GET  /v1/composers
GET  /v1/ragas
GET  /v1/talas
GET  /v1/deities
GET  /v1/temples
GET  /v1/tags
GET  /v1/sampradayas

POST /v1/imports/krithis      (admin)
POST /v1/imports/{id}/review  (admin)

GET  /v1/audit/logs           (admin)
```

#### API Rules
- Public endpoints are strictly read-only
- All mutations write to `AUDIT_LOG`
- DTOs only; ORM entities never exposed
- Authorization enforced via role claims

---

### 6.4 Data Ingestion

#### Sources
- karnatik.com
- shivkumar.org
- Blogspot composer lists
- PDFs (Papanasam Shivan, others)

#### Pipeline
1. Scrape → raw structured rows
2. Normalize:
   - Raga names
   - Tala names
   - Temple names via aliases
3. Store in staging tables (`ImportedKrithi`, `ImportedLyric`)
4. Admin reviews & maps:
   - Primary language
   - Raga(s)
   - Sections
   - Tags
   - Sampradaya
5. Promote to canonical entities

#### Rules
- Never auto-publish
- Always retain source reference
- Imported tags may carry confidence scores

---

## 7. Data Model (Conceptual)

### Core Entities
- Composer
- Raga
- Tala
- Deity
- Temple
- TempleName (multilingual & aliases)
- Tag (controlled taxonomy)
- Sampradaya
- Krithi  
  - Includes `primary_language` (e.g. Sanskrit, Telugu, Tamil)
- KrithiSection
- KrithiLyricVariant
- KrithiLyricSection
- ImportSource
- AuditLog
- User / Role

### Relationships
- Krithi → Composer (1–1)
- Krithi → Raga (1–many) **(Ragamalika supported)**
  - Single-raga krithis: exactly 1 raga
  - Ragamalika: ordered list of ragas
- Krithi → Tala (1–1)
- Krithi → Deity (0–1)
- Krithi → Temple (0–1)
- Temple → TempleName (1–many)
- Krithi → KrithiSection (1–many)
- KrithiSection → KrithiLyricSection (1–many, via lyric variants)
- Krithi → KrithiLyricVariant (1–many)
- KrithiLyricVariant → Sampradaya (0–1)
- Krithi → Tag (many–many)

---

## 8. Non-Functional Requirements

### Performance
- Search p95 < 300 ms
- API p95 < 500 ms

### Security
- JWT with role claims
- HTTPS only
- Admin endpoints authenticated

### Observability
- Request logging
- Error tracking
- Audit logs for all writes

### Maintainability
- Strict typing everywhere
- Versioned migrations
- Clear module boundaries
- Deterministic schemas

---

## 9. Tech Stack (Locked)

- **Mobile:** Kotlin Multiplatform + Compose
- **Backend:** Ktor + Exposed
- **Admin Web:** React + TypeScript + Tailwind
- **Database:** PostgreSQL 15+
- **Migrations:** Rust CLI (SQL-based)
- **CI/CD:** GitHub Actions
- **Cloud:** AWS or GCP

---

## 10. Milestones (Suggested)

### Phase 0 – Foundation
- Repo + module scaffolding
- CI/CD + migrations

### Phase 1 – Core Data & API
- Krithi, sections, tags, sampradaya schema
- Read APIs

### Phase 2 – Admin Console
- CRUD + workflow
- Import review + audit logs

### Phase 3 – Mobile App
- Search + browse
- Krithi detail screens

---

## 11. Codex / Copilot Instructions (Authoritative)

When generating code:
- Follow **Sangita Grantha Blueprint** strictly
- Use KMM shared domain models
- Use Rust migrations (**never Flyway**)
- Use strict TypeScript (no `any`)
- Keep Ktor routes thin and services explicit
- Treat this PRD as the **source of truth**