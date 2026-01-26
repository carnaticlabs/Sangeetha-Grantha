| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-01-26 |
| **Author** | Sangeetha Grantha Team |

# Tech Stack


## Current Dependencies

All versions are managed in `gradle/libs.versions.toml`.

### Backend Core Versions

- **Kotlin**: 2.3.0
- **Ktor**: 3.4.0 (server + client)
- **Exposed**: 1.0.0 (ORM)
- **PostgreSQL Driver**: 42.7.9
- **HikariCP**: 7.0.2 (connection pooling)
- **JWT (Auth0)**: 4.5.0
- **Logback**: 1.5.25
- **Logstash Logback Encoder**: 8.0
- **Commons CSV**: 1.10.0

### Kotlinx Libraries

- **Kotlinx Coroutines**: 1.10.2
- **Kotlinx DateTime**: 0.6.1
- **Kotlinx Serialization JSON**: 1.10.0

### Mobile (Kotlin Multiplatform)

- **Kotlin**: 2.3.0
- **Compose Multiplatform**: 1.10.0
- **Android Gradle Plugin**: 9.0.0
- **Ktor Client**: 3.4.0
- **Kotlinx Libraries**: Same as backend

### Frontend (Admin Web)

- **React**: 19.2.0
- **TypeScript**: 5.8.3
- **Vite**: 7.1.7
- **Tailwind CSS**: 3.4.13
- **React Router**: Latest (via npm)

### Database & Migrations

- **PostgreSQL**: 15+ (local development)
- **Rust Migration Tool**: `tools/sangita-cli` (custom Rust CLI)
- **❌ NOT Flyway**: Migrations are managed via Rust tool, not Flyway

### Build & Tooling

- **Gradle**: Wrapper included
- **Shadow Plugin**: 9.2.2 (for JAR packaging)
- **JDK**: 25+ (JVM Toolchain)
- **Rust**: Stable (for migration CLI)
- **Bun**: For frontend package management

### Cloud & Authentication

- **AWS SDK**: 2.29.0 (for S3, future use)
- **Google Auth Library**: 1.23.0 (for SSO, future use)

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

### In Progress 🔄

- Frontend admin web console
- Mobile app (KMM)
- Advanced search features
- Bulk import workflows

### Planned 📋

- Audio file support
- Public user accounts
- Comments and annotations
- Advanced graph visualization
- API rate limiting
- Caching layer
