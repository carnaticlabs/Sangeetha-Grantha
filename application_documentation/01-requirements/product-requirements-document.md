| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-01-26 |
| **Author** | Sangeetha Grantha Team |

# Sangita Grantha – Product Requirements Document (PRD)


---

## 1. Executive Summary

**Sangita Grantha** is a multi-platform, authoritative digital compendium of Carnatic classical music compositions. It consolidates scattered, semi-structured sources into a single, normalized, searchable, and multilingual system with strong editorial governance and musicological correctness.

The platform supports **multiple Carnatic musical forms**, including:

- **Krithis** – primarily sahitya-centric  
- **Varnams** – notation-centric pedagogical compositions  
- **Swarajathis** – jathi + swara driven compositional forms  

While Krithis emphasize lyrical structure, **Varnams and Swarajathis require detailed, section-wise swara notation aligned with tala**. Sangita Grantha models both lyrics and notation as first-class, structured entities.

The system consists of:
- Public Mobile App (Android & iOS via Kotlin Multiplatform)
- Backend API Platform (Ktor + PostgreSQL)
- Restricted Admin Web Console (React + TypeScript + Tailwind)
- Data Ingestion & Normalization Pipeline for legacy sources

**Primary Objective**

Establish Sangita Grantha as the *system of record* for Carnatic compositions, combining scholarly rigor with production-grade software engineering.

---

## 2. Problem Statement

Carnatic music knowledge currently exists across:
- Static websites (karnatik.com, shivkumar.org, etc.)
- Blogspot composer lists
- PDFs and scanned books

### Key Challenges

- No unified or normalized schema
- Poor searchability (especially by lyrics)
- Inconsistent multilingual representation
- No structured handling of notation-centric forms
- No editorial workflow or provenance tracking

---

## 3. Goals & Non-Goals

### 3.1 Goals

- Unified, normalized catalog of Carnatic compositions
- Musical-form-aware data modeling
- Fast, multi-field search:
  - Composition name / opening line
  - Lyrics substring
  - Composer
  - Raga(s)
  - Tala
  - Deity
  - Temple / kṣetram
- Multilingual sahitya with structured sections
- Detailed swara notation for Varnams and Swarajathis
- Support for ragamalika compositions
- Sampradaya-aware variants
- Strong admin editorial workflows

### 3.2 Non-Goals (v1)

- Audio playback or notation animation
- Cursor-synced tala visualization
- Community edits or crowdsourcing
- Monetization

---

## 4. Personas

### 4.1 Rasika / Learner (Public User)

- Searches and browses compositions
- Reads lyrics and notation
- Uses mobile app only
- Read-only access

### 4.2 Editor (Admin)

- Creates and edits compositions
- Manages sections, lyrics, and notation
- Cannot publish directly

### 4.3 Reviewer (Admin)

- Reviews drafts
- Publishes canonical versions

### 4.4 Admin (System)

- Full access
- Manages users, taxonomies, and archival

---

## 5. Musical Form Awareness (Core Principle)

Each composition in Sangita Grantha **MUST** be classified by a `musical_form`.

### Supported Musical Forms (v1)

- `KRITHI`
- `VARNAM`
- `SWARAJATHI`

### Musical Form Determines

- Required section types
- Presence and structure of notation
- Admin validation rules
- Mobile rendering behavior

---

## 6. Functional Requirements

---

### 6.1 Public Mobile App (Read-Only)

#### Core Features

- Search compositions by:
  - Title / incipit
  - Lyrics substring
  - Composer
  - Raga(s)
  - Tala
  - Deity
  - Temple / kṣetram
- Browse by:
  - Composer
  - Raga
  - Deity / temple
  - Tags (festival, bhava, etc.)

#### Composition Detail View

**Metadata**
- Title and incipit
- Composer
- Musical form
- Raga(s) (ordered for ragamalika)
- Tala
- Deity and temple
- Primary language
- Sahitya summary (optional)

**Lyrics & Notation**
- Lyrics organized by sections
- Multiple lyric variants (language/script)
- For Varnams & Swarajathis:
  - Swara notation displayed line-by-line
  - Avartanam boundaries visually indicated
  - Sahitya aligned where available

#### Non-Goals (Mobile v1)

- Tala animation
- Playback cursor
- Audio–notation synchronization

---

### 6.2 Admin Web Console

---

#### 6.2.1 Reference Data Management

CRUD for:
- Composers
- Ragas
- Talas
- Deities
- Temples (with multilingual names)
- Tags (controlled taxonomy)
- Sampradayas

---

#### 6.2.2 Composition (Krithi) Management

**Fields**
- Title, incipit
- Composer
- Musical form
- Raga(s)
- Tala
- Deity
- Temple
- Primary language
- Sahitya summary
- Notes
- Workflow state

**Workflow**
- Draft → In Review → Published → Archived
- Explicit actions only (no auto-publish)
- Full audit logging

---

#### 6.2.3 Section Structure Management

**Supported Section Types**
- Pallavi
- Anupallavi
- Charanam(s)
- Muktaayi Swaram (Varnam only)
- Chittaswaram(s) (Varnam only)
- Jathi (Swarajathi only)
- Swara–Sahitya composite sections

##### Musical Form Constraints

**VARNAM**
- Pallavi and Anupallavi are mandatory
- Exactly one Muktaayi Swaram is mandatory
- At least one Chittaswaram is mandatory

**SWARAJATHI**
- Sections must alternate between Jathi and Sahitya
- Tala alignment is mandatory

---

#### 6.2.4 Lyric Management

- Multiple lyric variants per composition
- Language, script, transliteration scheme
- Optional sampradaya and variant label
- Section-wise lyric entry

> **IMPORTANT RULE**  
> Swara notation **SHALL NOT** be stored in lyric sections.

---

#### 6.2.5 Notation Management (Varnams & Swarajathis)

Notation is modeled **independently of lyrics**.

Editors can:
- Create multiple notation variants
- Attribute notation to bani / school / source
- Define tala, kalai, and eduppu offset
- Enter ordered notation rows per section

Notation supports:
- Swara-only passages
- Swara + sahitya alignment
- Multiple cycles per section
- Tala-aware grouping

---

#### 6.2.6 Tags & Themes

- Controlled taxonomy (festival, bhava, philosophy, etc.)
- Assign confidence and source
- Used for discovery and filtering

---

#### 6.2.7 Import Review & Canonicalization

- Review imported data
- Map to canonical entities
- Preserve source references
- Never auto-publish imported data

---

## 7. Data Model (Conceptual)

### Core Entities

**Composition**
- title
- incipit
- composer
- musical_form
- primary_language
- raga(s)
- tala
- deity
- temple
- workflow_state

**Section**
- section_type
- order_index

**Lyric Variant**
- language
- script
- sampradaya
- variant_label

**Lyric Section**
- section
- text

**Notation Variant**
- notation_type (`SWARA` | `JATHI`)
- tala
- kalai
- eduppu_offset
- source_reference
- variant_label

**Notation Row**
- section
- order_index
- swara_text
- sahitya_text (optional)
- tala_markers

---

## 8. Backend API Requirements

### Public APIs
```
GET /v1/compositions/search
GET /v1/compositions/{id}
GET /v1/compositions/{id}/notation
```

### Admin APIs
```
POST /v1/admin/compositions
PUT  /v1/admin/compositions/{id}
POST /v1/admin/compositions/{id}/notation
```

### API Rules

- Public APIs are read-only
- All mutations are audited
- DTO-only exposure (no ORM leakage)

---

## 9. Non-Functional Requirements

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
- Immutable audit logs

---

## 10. Technology Stack (Locked)

- **Mobile:** Kotlin Multiplatform + Compose
- **Backend:** Ktor + PostgreSQL
- **Admin Web:** React + TypeScript + Tailwind
- **Migrations:** Rust CLI (SQL-based)
- **Cloud:** AWS or GCP
- **CI/CD:** GitHub Actions

---



---

## 12. Guiding Principle

> Sangita Grantha models Carnatic compositions **as musicians learn and perform them**, not merely as blocks of text.

This PRD is the **single source of truth** for implementation.
