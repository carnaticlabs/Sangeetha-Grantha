| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.1.1 |
| **Last Updated** | 2026-02-19 |
| **Author** | Sangeetha Grantha Team |

# Sangita Grantha Backend Architecture


# Backend Architecture (Sangita Grantha)

## 1. Module Layout

The backend is organized into two modules:

- **`modules/backend/api`**: Ktor HTTP server, routes, services, and request/response models
- **`modules/backend/dal`**: Data access layer using Exposed ORM, repositories, and database tables

Shared domain models live in `modules/shared/domain` (Kotlin Multiplatform).

---

## 2. Runtime Stack

For detailed versions and dependencies, see **[Tech Stack](./tech-stack.md)**.

- **Ktor Server**: Netty engine
- **Exposed ORM**: Type-safe SQL
- **PostgreSQL**: Primary data store
- **Kotlin Multiplatform**: Shared domain logic

---

## 3. Application Structure

### 3.1 Entry Point

`modules/backend/api/src/main/kotlin/com/sangita/grantha/backend/api/App.kt`

- Initializes `DatabaseFactory` from external config
- Creates `SangitaDal` instance
- Instantiates services (KrithiService, KrithiNotationService, etc.)
- Configures Ktor plugins (serialization, security, routing, etc.)
- Starts embedded Netty server

### 3.2 Package Layout

```text
modules/backend/api/
├── App.kt                    # Entry point
├── config/                   # Environment configuration
├── models/                   # Request/response DTOs
│   ├── KrithiRequests.kt
│   ├── NotationRequests.kt
│   └── ImportRequests.kt
├── routes/                   # Ktor route handlers
│   ├── HealthRoutes.kt
│   ├── PublicKrithiRoutes.kt
│   ├── AdminKrithiRoutes.kt
│   ├── AdminNotationRoutes.kt
│   ├── ImportRoutes.kt
│   ├── ReferenceDataRoutes.kt
│   ├── AuditRoutes.kt
│   └── DashboardRoutes.kt
├── services/                 # Business logic layer
│   ├── KrithiService.kt
│   ├── KrithiNotationService.kt
│   ├── ReferenceDataService.kt
│   ├── ImportService.kt
│   ├── AuditLogService.kt
│   └── AdminDashboardService.kt
├── plugins/                  # Ktor plugin configuration
│   ├── Routing.kt
│   ├── Security.kt
│   ├── Serialization.kt
│   ├── StatusPages.kt
│   ├── Cors.kt
│   └── RequestLogging.kt
└── security/                 # Auth utilities

modules/backend/dal/
├── DatabaseFactory.kt        # Connection management
├── SangitaDal.kt             # Main DAL interface
├── repositories/             # Data access repositories
│   ├── KrithiRepository.kt
│   ├── KrithiNotationRepository.kt
│   ├── ComposerRepository.kt
│   ├── RagaRepository.kt
│   ├── TalaRepository.kt
│   ├── DeityRepository.kt
│   ├── TempleRepository.kt
│   ├── TagRepository.kt
│   ├── SampradayaRepository.kt
│   ├── ImportRepository.kt
│   ├── UserRepository.kt
│   └── AuditLogRepository.kt
├── tables/                   # Exposed table definitions
│   └── CoreTables.kt
├── models/                   # DTO mappers
│   └── DtoMappers.kt
└── enums/                    # Database enum mappings
    └── DbEnums.kt
```

---

## 4. Database Access

### 4.1 DatabaseFactory

Centralized database connection management:

- Loads configuration from TOML file (via `config/application.local.toml`)
- Uses HikariCP for connection pooling
- Provides `dbQuery { }` suspend function for all database operations
- Handles transaction boundaries

```text
**Pattern:**
DatabaseFactory.dbQuery {
    // Exposed DSL operations
}
```

### 4.2 DAL Pattern

**SangitaDal** aggregates all repositories:

```kotlin
class SangitaDal {
    val krithis: KrithiRepository
    val notation: KrithiNotationRepository
    val composers: ComposerRepository
    val ragas: RagaRepository
    // ... other repositories
}
```

**Repository Pattern:**
- Each repository handles CRUD for one entity type
- Returns DTOs (from `modules/shared/domain`), not Exposed entities
- All operations use `DatabaseFactory.dbQuery { }`
- Repositories are injected into services

**Optimization Patterns:**
- **Create Operations**: Use `insert { ... }.resultedValues` to return inserted row in single query
- **Update Operations**: Use `Table.updateReturning()` to return updated row in single query
- **Collection Updates**: Use smart diffing algorithms to preserve metadata and minimize writes
- See [Database Layer Optimization](../01-requirements/features/database-layer-optimization.md) for details

---

## 5. Service Layer

Services contain business logic and orchestrate repository calls.

### 5.1 KrithiService

- Search and filtering (public and admin)
- Krithi CRUD operations
- Lyric variant management
- Section management
- Tag assignment
- Workflow state transitions (draft → in_review → published → archived)

### 5.2 KrithiNotationService

- Notation variant CRUD (for VARNAM/SWARAJATHI compositions)
- Notation row management
- Supports SWARA and JATHI notation types
- Handles tala, kalai, and eduppu metadata
- Validates `musicalForm` before allowing notation operations

### 5.3 ReferenceDataService

- Lists composers, ragas, talas, deities, temples, tags, sampradayas
- Provides reference data statistics

### 5.4 ImportService

- Import source management with authority hierarchy (Tier 1–5)
- Imported Krithi review workflow
- Mapping imported entries to canonical Krithis
- `ComposerSourcePriority` map — routes each composer to their most authoritative source (e.g., Dikshitar → guruguha.org)
- Multi-source structural voting integration via `StructuralVotingEngine`
- Enhanced deity/temple resolution with priority ordering (overrides → resolution → scraped metadata)

### 5.4.1 StructuralVotingEngine

Compares section structures from multiple sources for the same Krithi and selects the most accurate canonical structure.

- Scores candidates by primary section count, technical marker detection (Madhyama Kala, Chittaswaram), and source authority tier
- Voting rules: Unanimous agreement (HIGH confidence), Majority agreement (MEDIUM), Authority override (Tier 1 prevails), Manual review (LOW)
- Results logged to `structural_vote_log` table for audit transparency
- See [Strategy §5.3](../01-requirements/krithi-data-sourcing/quality-strategy.md#53-phase-2--structural-validation-track-041-integration)

### 5.4.2 KrithiStructureParser (Enhanced)

The `KrithiStructureParser` has been enhanced with:

- Multi-script section header detection (Devanagari, Tamil, Telugu, Kannada, Malayalam)
- Parenthesized Madhyama Kala recognition (e.g., `(m.k)`, `(madhyama kāla)`)
- Tamil subscript normalisation (removes Unicode subscripts `₁₂₃₄`)
- Improved Ragamalika sub-section detection with raga labels
- Enhanced boilerplate filtering (pronunciation guides, blog footers, metadata noise)

### 5.5 AuditLogService

- Query audit logs by entity, actor, or time range
- Supports filtering and pagination

### 5.6 AdminDashboardService

- Aggregates statistics for admin dashboard
- Counts by workflow state, musical form, etc.

### 5.7 AI Services

**TransliterationService:**
- Uses `GeminiApiClient` for multi-script transliteration.
- Preserves alignment markers and notation symbols.

**WebScrapingService:**
- Fetches HTML from external sources (e.g., shivkumar.org).
- Uses Gemini to parse structured metadata from unstructured HTML.

### 5.8 Containerised Extraction Architecture

The import pipeline now supports multi-format ingestion (PDF, DOCX, OCR) via a Python extraction service running as a Docker container alongside the Kotlin backend.

**Integration Pattern**: Database Queue Table (`extraction_queue`)
- Kotlin writes extraction requests → Python polls and processes → writes results back
- No HTTP coupling, no subprocess management, no shared filesystem
- Uses `SELECT ... FOR UPDATE SKIP LOCKED` for exactly-once processing
- See [Strategy §8.3](../01-requirements/krithi-data-sourcing/quality-strategy.md#83-integration-via-database-queue-table)

**Python Extraction Service** (`tools/krithi-extract-enrich-worker/`):
- PyMuPDF for text extraction with positional data
- Tesseract OCR with Indic language packs (Sanskrit, Tamil, Telugu, Kannada, Malayalam)
- `indic-transliteration` for script conversion
- Page segmentation for anthology PDFs (e.g., 484 Dikshitar compositions)
- Section label detection and metadata header extraction
- All output conforms to the `CanonicalExtractionDto` schema

**Deployment**:
- Docker Compose (local): `krithi-extract-enrich-worker` service, profile-gated under `extraction`
- Kubernetes (production): Deployment with Cloud SQL Proxy sidecar, HPA based on queue depth
- See `compose.yaml` and [Strategy §8.2](../01-requirements/krithi-data-sourcing/quality-strategy.md#82-docker-containerised-architecture)

---

## 6. HTTP API Design

## 6. HTTP API Design

The comprehensive API specification, including all request/response models and route definitions, is maintained in:

**[API Contract](../03-api/api-contract.md)**

### 6.1 Strategy

**Public Routes** (`/v1/`):
- Read-only access to published content.
- No authentication required.
- Optimized for caching and fast retrieval.

**Admin Routes** (`/v1/admin/`):
- Full CRUD capabilities.
- Protected by JWT authentication and RBAC.
- Includes workflow management, audit logs, and import pipelines.

All admin routes require JWT authentication with appropriate roles.

---

## 7. Error Handling

- **StatusPages plugin**: Centralized error handling
- **Structured errors**: JSON error responses with `code`, `message`, `fields`
- **HTTP status codes**: 400 (validation), 401 (unauthorized), 403 (forbidden), 404 (not found), 500 (internal)
- **No stack traces**: Production errors return generic messages; details logged server-side

---

## 8. Security & Auth

- **JWT-based authentication** for admin endpoints
- **Role-based access control** (RBAC) via `roles` and `role_assignments` tables
- **Public endpoints**: Read-only, no authentication required
- **Admin endpoints**: Require JWT with role claims (`editor`, `reviewer`, `admin`)
- See `../06-backend/security-requirements.md` for detailed security patterns

---

## 9. Observability & Audit

- **Request logging**: All HTTP requests logged via Ktor CallLogging plugin
- **Audit trail**: All mutations write to `audit_log` table
- **Audit fields**: `actor_user_id`, `action`, `entity_table`, `entity_id`, `diff` (JSONB), `metadata` (JSONB)
- **Query audit logs**: `GET /v1/admin/audit` endpoint for audit log queries

---

## 10. Testing

- **Integration tests**: Use Ktor `testApplication` with test database
- **Test data**: Deterministic fixtures with fixed UUIDs
- **Service tests**: Mock repositories or use in-memory database
- **Route tests**: Test full HTTP request/response cycle

---

## 11. Best Practices

### Database Operations
- ✅ Always use `DatabaseFactory.dbQuery { }` for database access
- ✅ Return DTOs, never Exposed entity objects
- ✅ Use transactions for multi-step operations
- ✅ **Use `insert().resultedValues` for create operations** - Eliminates post-insert SELECT queries
- ✅ **Use `updateReturning()` for update operations** - Eliminates post-update SELECT queries
- ✅ **Use smart diffing for collection updates** - Preserves metadata and minimizes writes

### Service Layer
- ✅ Keep routes thin; delegate to services
- ✅ Services orchestrate repository calls
- ✅ Validate business rules in services, not repositories

### Error Handling
- ✅ Use sealed results or nullable returns (avoid exceptions unless necessary)
- ✅ Map database errors to structured API errors
- ✅ Log errors with context, return generic messages to clients

### Mutations
- ✅ All mutations must write to `audit_log` table
- ✅ Use transactions for atomic operations
- ✅ Validate `musicalForm` before allowing notation operations

### Code Organization
- ✅ Keep DTOs in `modules/shared/domain` for cross-platform use
- ✅ Use `kotlinx.serialization` for all DTOs
- ✅ Use `kotlinx.datetime.Instant` (not Java date/time classes)
- ✅ Use `kotlin.uuid.Uuid` for IDs