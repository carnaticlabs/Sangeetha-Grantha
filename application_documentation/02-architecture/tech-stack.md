| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.2.0 |
| **Last Updated** | 2026-07-12 |
| **Author** | Sangeetha Grantha Team |

# Tech Stack

The Sangeetha Grantha platform uses a modern, type-safe stack spanning mobile, backend, and web.

For current toolchain and library versions, see **[Current Versions](../00-meta/current-versions.md)**.

## Core Technologies

### Backend & Mobile (Kotlin/JVM)
- **Language**: Kotlin
- **HTTP Server**: Ktor (Netty)
- **ORM**: Exposed (DSL-based)
- **Database**: PostgreSQL 18+
- **Security**: JWT (Auth0)
- **Dependency Injection**: Koin

### Admin Web (React/TypeScript)
- **Framework**: React 19
- **Language**: TypeScript 6.0
- **Build Tool**: Vite 8.1 (Rolldown bundler)
- **Styling**: Tailwind CSS 4.3
- **State Management**: TanStack Query (React Query)
- **Testing**: Vitest (55 component tests), Playwright (E2E nightly)

### Mobile (Kotlin Multiplatform)
- **UI Framework**: Compose Multiplatform
- **Logic**: Shared Kotlin domain models and services

### Extraction & Enrichment (Python)
- **Worker**: krithi-extract-enrich-worker (Python 3.11+)
- **PDF Extraction**: PyMuPDF (fitz)
- **HTTP Client**: httpx
- **OCR Fallback**: Tesseract (via pytesseract)

### Testing Infrastructure
- **Integration Tests**: Testcontainers 2.0.5 (JVM) + testcontainers-python
- **CI**: GitHub Actions — backend unit/integration, Flyway validate, frontend typecheck+build+test, worker pytest
- **E2E**: Playwright against compose stack, nightly schedule

### Tooling & Infrastructure
- **Version Manager**: `mise` (Java, Bun)
- **Migration Tool**: Flyway Community 12.9.0 for database migrations ([ADR-013](./decisions/ADR-013-db-migration-with-flyway.md); replaces the Python `db-migrate` tool, archived)
- **Dev Orchestration**: Makefile + Docker Compose (DB, backend, frontend, extraction)
- **Containerization**: Docker Compose for full dev stack
- **Package Manager**: `bun` for frontend, `gradle` for backend/mobile

---

## Implementation Status

### Completed ✅

- Core backend architecture (Ktor + Exposed)
- Database schema with all entities (47 migrations, versioned canon)
- Notation support (KrithiNotationVariant, KrithiNotationRow)
- Musical form enum (KRITHI, VARNAM, SWARAJATHI)
- Public read-only API endpoints
- Admin authentication (JWT + argon2id password hashing)
- Admin Krithi CRUD operations
- Admin notation management (create/update/delete)
- Import pipeline (sources, imported Krithis, review workflow)
- Bulk import with chunked approval and serialization
- Curator review UI (approve/reject/merge with keyboard shortcuts)
- Audit logging
- Reference data services
- Admin dashboard service
- Database migration tooling (Flyway per ADR-013)
- AI Transliteration Service (Gemini integration)
- Web Scraping Infrastructure
- Testcontainers integration test substrate (TRACK-110)
- DAL test suite + CI activation (TRACK-111)
- Worker + E2E tests (TRACK-113)
- Frontend component tests — 55 Vitest tests (TRACK-118)
- Versioned canon & provenance graph (ADR-014, TRACK-116/117)
- Full CI/CD pipeline (GitHub Actions)

### In Progress 🔄

- Mobile app (Compose Multiplatform)
- Advanced search features
- Versioned canon re-import (TRACK-117)

### Planned 📋

- Semantic search (pgvector + FTS, TRACK-108)
- OAuth / OTP authentication (TRACK-119)
- Audio file support and media management
- Public user accounts and favorites
- Thematic annotations and curated collections
- Advanced graph visualization
- API rate limiting and caching layer