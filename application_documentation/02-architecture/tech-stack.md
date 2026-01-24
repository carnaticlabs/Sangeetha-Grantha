| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-01-24 |
| **Author** | Sangeetha Grantha Team |

# Tech Stack

| Metadata | Value |
|:---|:---|
| **Status** | Current |
| **Version** | 2.1 |
| **Last Updated** | 2026-01-20 |
| **Author** | Engineering Team |
| **Related Documents** | - [Architecture](backend-system-design.md)<br>- [Schema](../04-database/schema.md)<br>- [Frontend Admin Web](../05-frontend/admin-web/) |

## Current Dependencies

All versions are managed in `gradle/libs.versions.toml`.

### Backend Core Versions

- **Kotlin**: 2.3.0
- **Ktor**: 3.3.3 (server + client)
- **Exposed**: 1.0.0-rc-4 (ORM)
- **PostgreSQL Driver**: 42.7.8
- **HikariCP**: 7.0.2 (connection pooling)
- **JWT (Auth0)**: 4.5.0
- **Logback**: 1.5.20
- **Logstash Logback Encoder**: 8.0

### Kotlinx Libraries

- **Kotlinx Coroutines**: 1.9.0
- **Kotlinx DateTime**: 0.6.1
- **Kotlinx Serialization JSON**: 1.7.3

### Mobile (Kotlin Multiplatform)

- **Kotlin**: 2.3.0
- **Compose Multiplatform**: 1.9.3
- **Android Gradle Plugin**: 8.13.2
- **Ktor Client**: 3.3.3
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
