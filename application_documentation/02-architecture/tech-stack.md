| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.1.0 |
| **Last Updated** | 2026-02-08 |
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
- **Language**: TypeScript
- **Build Tool**: Vite
- **Styling**: Tailwind CSS
- **State Management**: TanStack Query (React Query)

### Mobile (Kotlin Multiplatform)
- **UI Framework**: Compose Multiplatform
- **Logic**: Shared Kotlin domain models and services

### Tooling & Infrastructure
- **Version Manager**: `mise` (Java, Rust, Bun)
- **CLI Tool**: `sangita-cli` (Rust) for migrations and dev orchestration
- **Containerization**: Docker Compose for local database
- **Package Manager**: `bun` for frontend, `gradle` for backend/mobile

---

## Implementation Status

### Completed ✅

- Core backend architecture (Ktor + Exposed)
- Database schema with all entities (Krithis, Composers, Ragas, Talas, etc.)
- Notation support (KrithiNotationVariant, KrithiNotationRow)
- Musical form enum (KRITHI, VARNAM, SWARAJATHI)
- Public read-only API endpoints
- Admin authentication (JWT)
- Admin Krithi CRUD operations
- Admin notation management (create/update/delete)
- Import pipeline (sources, imported Krithis, review workflow)
- Audit logging
- Reference data services
- Admin dashboard service
- Rust-based migration tool (`tools/sangita-cli`)
- AI Transliteration Service (Gemini integration)
- Web Scraping Infrastructure

### In Progress 🔄

- Frontend admin web console enhancements
- Mobile app (Compose Multiplatform)
- Advanced search features
- Bulk import workflow optimizations

### Planned 📋

- Audio file support and media management
- Public user accounts and favorites
- Thematic annotations and curated collections
- Advanced graph visualization
- API rate limiting and caching layer