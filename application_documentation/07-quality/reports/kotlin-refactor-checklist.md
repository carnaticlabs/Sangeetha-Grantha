| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.1.0 |
| **Last Updated** | 2026-02-08 |
| **Author** | Sangeetha Grantha Team |

---

# Kotlin Codebase Refactoring Checklist

**Date:** 2026-01-27
**Scope:** `modules/backend/` and `modules/shared/`
**Related:** [kotlin-code-review.md](./kotlin-code-review.md)

---

## How to Use This Checklist

Each item includes:
- **Priority:** P0 (Critical) → P3 (Nice-to-have)
- **Effort:** S (Small, <2h) / M (Medium, 2-8h) / L (Large, 1-3d) / XL (Epic, >3d)
- **Files:** Specific files to modify
- **Before/After:** Code examples where applicable

Mark items with:
- `[ ]` Not started
- `[~]` In progress
- `[x]` Complete
- `[-]` Won't do (with reason)

---

## 1. Architecture Improvements

### 1.1 Service Layer Interfaces
**Priority:** P0 | **Effort:** L

Extract interfaces for all service classes to enable testing and flexibility.

- [x] `KrithiService` → `IKrithiService` / `KrithiServiceImpl`
- [x] `ImportService` → `IImportService` / `ImportServiceImpl`
- [x] `ReferenceDataService` → `IReferenceDataService` / `ReferenceDataServiceImpl`
- [x] `BulkImportWorkerService` → `IBulkImportWorker` / `BulkImportWorkerServiceImpl`
- [x] `EntityResolutionService` → `IEntityResolver` / `EntityResolutionServiceImpl`
- [x] `QualityScoringService` → `IQualityScorer` / `QualityScoringServiceImpl`
- [x] `TransliterationService` → `ITransliterator` / `TransliterationServiceImpl`
- [x] `WebScrapingService` → `IWebScraper` / `WebScrapingServiceImpl`

**Example:**
```kotlin
// Before (KrithiService.kt)
class KrithiService(private val dal: SangitaDal) {
    suspend fun search(request: KrithiSearchRequest, publishedOnly: Boolean = true): KrithiSearchResult
}

// After
interface IKrithiService {
    suspend fun search(request: KrithiSearchRequest, publishedOnly: Boolean = true): KrithiSearchResult
    suspend fun getKrithi(id: Uuid): KrithiDto?
    suspend fun createKrithi(request: KrithiCreateRequest): KrithiDto
    suspend fun updateKrithi(id: Uuid, request: KrithiUpdateRequest): KrithiDto
    // ...
}

class KrithiServiceImpl(private val dal: SangitaDal) : IKrithiService {
    // Implementation unchanged
}
```

**Files:**
- `modules/backend/api/src/main/kotlin/com/sangita/grantha/backend/api/services/*.kt`

---

### 1.2 Decompose BulkImportWorkerService
**Priority:** P1 | **Effort:** XL

Split the 847-line god class into focused components.

- [x] Create `ManifestParser` class for CSV parsing logic
- [x] Create `RateLimiter` class for throttling logic
- [x] Create `TaskDispatcher` class for channel management
- [x] Create `WorkerPool` class for worker lifecycle
- [x] Create `BatchCompletionHandler` for stage transitions
- [x] Extract `TaskErrorBuilder` for error payload construction

**Target Structure:**
```text
services/
├── bulkimport/
│   ├── BulkImportOrchestrator.kt     # Main coordinator
│   ├── ManifestParser.kt             # CSV parsing
│   ├── RateLimiter.kt                # Domain rate limiting
│   ├── TaskDispatcher.kt             # Channel management
│   ├── workers/
│   │   ├── ManifestWorker.kt
│   │   ├── ScrapeWorker.kt
│   │   └── ResolutionWorker.kt
│   └── handlers/
│       ├── BatchCompletionHandler.kt
│       └── TaskErrorHandler.kt
```

**Files:**
- `modules/backend/api/src/main/kotlin/com/sangita/grantha/backend/api/services/BulkImportWorkerService.kt`

---

### 1.3 Migrate to Koin Dependency Injection
**Priority:** P2 | **Effort:** L

Replace manual DI in `App.kt` with Koin modules.

- [x] Add Koin dependency to `build.gradle.kts`
- [x] Create `di/AppModule.kt` for service bindings
- [x] Create `di/DalModule.kt` for repository bindings
- [x] Update `App.kt` to use `startKoin()`
- [x] Update route configurations to inject dependencies

**Example:**
```kotlin
// di/AppModule.kt
val appModule = module {
    single { SangitaDal() }
    single<IKrithiService> { KrithiServiceImpl(get()) }
    single<IImportService> { ImportServiceImpl(get()) }
    // ...
}

// App.kt
fun main() {
    startKoin { modules(appModule, dalModule) }
    embeddedServer(Netty, ...) {
        // Dependencies injected via Koin
    }.start(wait = true)
}
```

**Files:**
- `modules/backend/api/build.gradle.kts`
- `modules/backend/api/src/main/kotlin/com/sangita/grantha/backend/api/App.kt`
- Create: `modules/backend/api/src/main/kotlin/com/sangita/grantha/backend/api/di/`

---

## 2. Code Quality Fixes

### 2.1 Replace Magic Numbers with Constants
**Priority:** P2 | **Effort:** S

- [x] Extract page size limit: `200` → `MAX_PAGE_SIZE`
- [x] Extract cache TTL: `15 * 60 * 1000` → `CACHE_TTL_MS`
- [x] Extract rate limits: `60`, `120` → `PER_DOMAIN_RATE_LIMIT`, `GLOBAL_RATE_LIMIT`
- [x] Extract worker counts: `1`, `3`, `2` → named constants
- [x] Extract timeout values: `10_000`, `60_000` → named constants

**Example:**
```kotlin
// Before (KrithiRepository.kt:598)
val safeSize = pageSize.coerceIn(1, 200)

// After
companion object {
    const val MIN_PAGE_SIZE = 1
    const val MAX_PAGE_SIZE = 200
}
val safeSize = pageSize.coerceIn(MIN_PAGE_SIZE, MAX_PAGE_SIZE)
```

**Files:**
- `modules/backend/dal/src/main/kotlin/com/sangita/grantha/backend/dal/repositories/KrithiRepository.kt`
- `modules/backend/api/src/main/kotlin/com/sangita/grantha/backend/api/services/BulkImportWorkerService.kt`
- `modules/backend/api/src/main/kotlin/com/sangita/grantha/backend/api/services/EntityResolutionService.kt`

---

### 2.2 Use require/check for Preconditions
**Priority:** P2 | **Effort:** S

- [x] Replace `throw IllegalArgumentException` with `require()`
- [x] Replace `throw IllegalStateException` with `check()`
- [x] Add meaningful error messages

**Example:**
```kotlin
// Before (KrithiService.kt:236-241)
private fun parseUuidOrThrow(value: String, label: String): UUID =
    try {
        UUID.fromString(value)
    } catch (ex: IllegalArgumentException) {
        throw IllegalArgumentException("Invalid $label")
    }

// After
private fun parseUuidOrThrow(value: String, label: String): UUID {
    require(value.isNotBlank()) { "$label must not be blank" }
    return runCatching { UUID.fromString(value) }
        .getOrElse { throw IllegalArgumentException("Invalid $label: must be a valid UUID") }
}
```

**Files:**
- `modules/backend/api/src/main/kotlin/com/sangita/grantha/backend/api/services/KrithiService.kt`
- `modules/backend/api/src/main/kotlin/com/sangita/grantha/backend/api/routes/RouteHelpers.kt`

---

### 2.3 Extract UUID Parsing Helper
**Priority:** P3 | **Effort:** S

Create a reusable extension function for UUID parsing.

- [x] Create `UuidParsing.kt` with `toJavaUuidOrNull()` / `toJavaUuidOrThrow()` helpers
- [x] Replace duplicate parsing logic across services

**Example:**
```kotlin
// support/UuidParsing.kt
fun String?.toJavaUuidOrNull(label: String): UUID? = runCatching { this?.let(UUID::fromString) }.getOrNull()

fun String.toJavaUuidOrThrow(label: String): UUID {
    require(this.isNotBlank()) { "$label must not be blank" }
    return UUID.fromString(this)
}
```

**Files:**
- Create: `modules/backend/api/src/main/kotlin/com/sangita/grantha/backend/api/support/UuidParsing.kt`
- Update: All services using `parseUuid*` functions

---

### 2.4 Complete TODO Items
**Priority:** P1 | **Effort:** M

- [x] Implement user context extraction from auth token
  - Files: `KrithiService.kt:165`, `KrithiService.kt:194`
- [x] Implement Phase 4 validation endpoint
  - Files: `AdminKrithiRoutes.kt`

**Example:**
```kotlin
// Before
createdByUserId = null, // TODO: Extract from auth context

// After - create UserContextService
class UserContextService(private val call: ApplicationCall) {
    fun currentUserId(): Uuid? = call.principal<JwtPrincipal>()?.userId?.toUuid()
}

// In service
createdByUserId = userContext.currentUserId()
```

---

### 2.5 Reduce Parameter Count
**Priority:** P3 | **Effort:** M

Use builder pattern or parameter objects for functions with many parameters.

- [x] `KrithiRepository.create()` - 18 parameters → `KrithiCreateParams` data class
- [x] `KrithiRepository.update()` - 16 parameters → `KrithiUpdateParams` data class
- [x] `DatabaseFactory.connect()` - 8 parameters → `ConnectionConfig` data class

**Example:**
```kotlin
// Before
suspend fun create(
    title: String,
    titleNormalized: String,
    incipit: String?,
    // ... 15 more parameters
): KrithiDto

// After
data class KrithiCreateParams(
    val title: String,
    val titleNormalized: String,
    val incipit: String? = null,
    // ... structured with defaults
)

suspend fun create(params: KrithiCreateParams): KrithiDto
```

**Files:**
- `modules/backend/dal/src/main/kotlin/com/sangita/grantha/backend/dal/repositories/KrithiRepository.kt`
- `modules/backend/dal/src/main/kotlin/com/sangita/grantha/backend/dal/DatabaseFactory.kt`

---

## 3. Security Hardening

### 3.1 Implement JWT Authentication
**Priority:** P0 | **Effort:** L

Replace single admin token with proper JWT.

- [x] Add `io.ktor:ktor-server-auth-jwt` dependency
- [x] Create `JwtConfig` for token generation/validation
- [x] Update `Security.kt` to use JWT authentication
- [x] Add user claims extraction to routes
- [x] Implement token refresh mechanism

**Example:**
```kotlin
// config/JwtConfig.kt
object JwtConfig {
    val secret = System.getenv("JWT_SECRET") ?: error("JWT_SECRET required")
    val issuer = "sangita-grantha"
    val audience = "sangita-users"
    val realm = "Sangita Grantha API"

    fun generateToken(userId: Uuid, roles: List<String>): String {
        return JWT.create()
            .withIssuer(issuer)
            .withAudience(audience)
            .withClaim("userId", userId.toString())
            .withClaim("roles", roles)
            .withExpiresAt(Date(System.currentTimeMillis() + 3600_000))
            .sign(Algorithm.HMAC256(secret))
    }
}

// plugins/Security.kt
authentication {
    jwt("admin-auth") {
        realm = JwtConfig.realm
        verifier(JWT.require(Algorithm.HMAC256(JwtConfig.secret))
            .withIssuer(JwtConfig.issuer)
            .build())
        validate { credential ->
            if (credential.payload.audience.contains(JwtConfig.audience)) {
                JwtPrincipal(credential.payload)
            } else null
        }
    }
}
```

**Files:**
- `modules/backend/api/build.gradle.kts`
- `modules/backend/api/src/main/kotlin/com/sangita/grantha/backend/api/plugins/Security.kt`
- Create: `modules/backend/api/src/main/kotlin/com/sangita/grantha/backend/api/config/JwtConfig.kt`

---

### 3.2 Move API Key to Headers
**Priority:** P1 | **Effort:** S

Remove API key from URL query parameters.

- [x] Update GeminiApiClient to use header-based auth
- [x] Reduce logging verbosity of API key

**Example:**
```kotlin
// Before (GeminiApiClient.kt:51-52)
url("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=$apiKey")

// After
url("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent")
header("x-goog-api-key", apiKey)
```

**Files:**
- `modules/backend/api/src/main/kotlin/com/sangita/grantha/backend/api/clients/GeminiApiClient.kt`

---

### 3.3 Add Request Validation Middleware
**Priority:** P1 | **Effort:** M

- [x] Create `RequestValidation` Ktor plugin
- [x] Add body size limits
- [x] Add request rate limiting (per IP)
- [x] Sanitize path parameters

**Example:**
```kotlin
// plugins/RequestValidation.kt
fun Application.configureRequestValidation() {
    install(RequestValidation) {
        validate<KrithiCreateRequest> { request ->
            if (request.title.isBlank()) {
                ValidationResult.Invalid("Title must not be blank")
            } else {
                ValidationResult.Valid
            }
        }
    }

    install(RateLimit) {
        global {
            rateLimiter(limit = 100, refillPeriod = 1.minutes)
        }
    }
}
```

**Files:**
- Create: `modules/backend/api/src/main/kotlin/com/sangita/grantha/backend/api/plugins/RequestValidation.kt`
- Update: `modules/backend/api/src/main/kotlin/com/sangita/grantha/backend/api/App.kt`

---

## 4. Performance Optimizations

### 4.1 Optimize Search Query (N+1 Fix)
**Priority:** P1 | **Effort:** M

Reduce search query from 3 queries to 1 using proper JOINs.

- [x] Refactor `KrithiRepository.search()` to use single JOIN query
- [x] Add database indexes for search columns
- [-] Consider materialized view for complex searches (deferred; JOINs + indexes sufficient for current load)

**Example:**
```kotlin
// Before: 3 separate queries
val krithiDtos = baseQuery.map { it.toKrithiDto() }
val composersMap = ComposersTable.selectAll()...
val ragasMap = KrithiRagasTable.join(RagasTable)...

// After: Single query with JOINs
val query = KrithisTable
    .join(ComposersTable, JoinType.INNER, KrithisTable.composerId, ComposersTable.id)
    .leftJoin(KrithiRagasTable, { KrithisTable.id }, { KrithiRagasTable.krithiId })
    .leftJoin(RagasTable, { KrithiRagasTable.ragaId }, { RagasTable.id })
    .select(
        KrithisTable.columns + ComposersTable.name + RagasTable.id + RagasTable.name + KrithiRagasTable.orderIndex
    )
```

**Files:**
- `modules/backend/dal/src/main/kotlin/com/sangita/grantha/backend/dal/repositories/KrithiRepository.kt`

---

### 4.2 Add Response Caching
**Priority:** P2 | **Effort:** M

Cache frequently accessed reference data.

- [x] Add `io.ktor:ktor-server-caching-headers` plugin
- [x] Configure cache for `/composers`, `/ragas`, `/talas`, etc.
- [x] Add ETag support for conditional requests

**Example:**
```kotlin
// plugins/Caching.kt
fun Application.configureCaching() {
    install(CachingHeaders) {
        options { call, outgoingContent ->
            when (call.request.uri) {
                in listOf("/v1/composers", "/v1/ragas", "/v1/talas") ->
                    CachingOptions(CacheControl.MaxAge(maxAgeSeconds = 3600))
                else -> null
            }
        }
    }
}
```

**Files:**
- `modules/backend/api/build.gradle.kts`
- Create: `modules/backend/api/src/main/kotlin/com/sangita/grantha/backend/api/plugins/Caching.kt`

---

### 4.3 Add Database Query Logging
**Priority:** P3 | **Effort:** S

Enable query logging for performance analysis.

- [x] Configure Exposed query logging
- [x] Add slow query detection (>100ms)
- [x] Add query count per request

**Example:**
```kotlin
// DatabaseFactory.kt
fun enableQueryLogging() {
    TransactionManager.current().warnLongQueriesDuration = 100 // ms
    addLogger(StdOutSqlLogger)
}
```

**Files:**
- `modules/backend/dal/src/main/kotlin/com/sangita/grantha/backend/dal/DatabaseFactory.kt`

---

## 5. Testing Infrastructure

### 5.1 Create Test Database Factory
**Priority:** P1 | **Effort:** M

- [x] Create `TestDatabaseFactory` with H2 in-memory database
- [x] Add schema migration for tests
- [x] Add test data fixtures

**Example:**
```kotlin
// test/support/TestDatabaseFactory.kt
object TestDatabaseFactory {
    fun connectTestDb() {
        DatabaseFactory.connect(
            databaseUrl = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
            username = "sa",
            password = "",
            driverClassName = "org.h2.Driver"
        )
        // Run migrations
        transaction {
            SchemaUtils.create(
                ComposersTable, RagasTable, TalasTable, KrithisTable, // ...
            )
        }
    }
}
```

**Files:**
- Create: `modules/backend/api/src/test/kotlin/com/sangita/grantha/backend/api/support/TestDatabaseFactory.kt`
- Create: `modules/backend/api/src/test/kotlin/com/sangita/grantha/backend/api/support/TestFixtures.kt`

---

### 5.2 Add Service Unit Tests
**Priority:** P1 | **Effort:** L

- [x] Add tests for `KrithiService`
- [x] Add tests for `ImportService`
- [x] Add tests for `EntityResolutionService`
- [x] Add tests for `QualityScoringService`

**Test Structure:**
```kotlin
class KrithiServiceTest {
    private lateinit var dal: SangitaDal
    private lateinit var service: KrithiService

    @BeforeEach
    fun setup() {
        TestDatabaseFactory.connectTestDb()
        dal = SangitaDal()
        service = KrithiService(dal)
    }

    @Test
    fun `search returns paginated results`() = runTest {
        // Given
        val request = KrithiSearchRequest(query = "test", page = 0, pageSize = 10)

        // When
        val result = service.search(request)

        // Then
        assertThat(result.page).isEqualTo(0)
        assertThat(result.pageSize).isEqualTo(10)
    }
}
```

**Files:**
- Create: `modules/backend/api/src/test/kotlin/com/sangita/grantha/backend/api/services/KrithiServiceTest.kt`
- Create: `modules/backend/api/src/test/kotlin/com/sangita/grantha/backend/api/services/ImportServiceTest.kt`

---

## 6. Documentation

### 6.1 Add KDoc Comments
**Priority:** P2 | **Effort:** M

- [x] Document all public service methods
- [x] Document all repository methods
- [x] Document complex algorithms (entity resolution, quality scoring)
- [x] Add package-level documentation

**Example:**
```kotlin
/**
 * Searches for krithis matching the given criteria.
 *
 * @param request Search parameters including filters and pagination
 * @param publishedOnly If true, only returns PUBLISHED krithis (default for public API)
 * @return Paginated search results with krithi summaries
 * @throws IllegalArgumentException if page or pageSize are invalid
 */
suspend fun search(request: KrithiSearchRequest, publishedOnly: Boolean = true): KrithiSearchResult
```

**Files:**
- All files in `modules/backend/api/src/main/kotlin/com/sangita/grantha/backend/api/services/`
- All files in `modules/backend/dal/src/main/kotlin/com/sangita/grantha/backend/dal/repositories/`

---

### 6.2 Add Architecture Decision Records
**Priority:** P3 | **Effort:** S

- [x] Document why Exposed ORM was chosen
- [x] Document why manual DI (vs Koin/Dagger)
- [x] Document authentication strategy decisions
- [x] Document bulk import architecture

**Files:**
- Create: `application_documentation/02-architecture/adr/`

---

## 7. Observability

### 7.1 Add Structured Logging
**Priority:** P2 | **Effort:** M

- [x] Configure Logback for JSON output in production
- [x] Add MDC context (request ID, user ID)
- [x] Add log correlation across services

**Example:**
```kotlin
// config/LogbackConfig.kt
fun configure(env: ApiEnvironment) {
    if (env.environment == "PROD") {
        val encoder = LogstashEncoder()
        // Configure JSON output
    }
}

// MDC in routes
withContext(MDCContext("requestId" to call.callId.toString())) {
    // Request handling
}
```

**Files:**
- `modules/backend/api/src/main/kotlin/com/sangita/grantha/backend/api/config/LogbackConfig.kt` (programmatic configuration)

---

### 7.2 Add Metrics Collection
**Priority:** P3 | **Effort:** M

- [x] Add Micrometer/Prometheus metrics
- [x] Track request latency, error rates
- [x] Track database connection pool stats
- [x] Track import worker throughput

**Example:**
```kotlin
// plugins/Metrics.kt
fun Application.configureMetrics() {
    install(MicrometerMetrics) {
        registry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
        meterBinders = listOf(
            JvmMemoryMetrics(),
            JvmGcMetrics(),
            ProcessorMetrics()
        )
    }
}
```

**Files:**
- `modules/backend/api/build.gradle.kts`
- Create: `modules/backend/api/src/main/kotlin/com/sangita/grantha/backend/api/plugins/Metrics.kt`

---

## 8. Shared Module Improvements

### 8.1 Add Validation Annotations
**Priority:** P2 | **Effort:** S

- [-] Add `@Contextual` for types requiring custom serialization (current DTOs use explicit serializers)
- [-] Consider adding JSR-380 (Bean Validation) annotations (deferred; validation enforced via Ktor RequestValidation)
- [x] Add value range constraints

**Example:**
```kotlin
@Serializable
data class KrithiSearchRequest(
    val query: String? = null,
    @SerialName("page")
    val page: Int = 0,  // Add: @Min(0)
    @SerialName("pageSize")
    val pageSize: Int = 50,  // Add: @Min(1) @Max(200)
)
```

**Files:**
- `modules/shared/domain/src/commonMain/kotlin/com/sangita/grantha/shared/domain/model/*.kt`

---

### 8.2 Split Large DTO Files
**Priority:** P3 | **Effort:** S

- [x] Split `KrithiDtos.kt` (195 lines) into logical groups
- [x] Split `ImportDtos.kt` (151 lines) into workflow and orchestration

**Target Structure:**
```text
domain/model/
├── krithi/
│   ├── KrithiDto.kt
│   ├── KrithiSearchDto.kt
│   ├── KrithiNotationDto.kt
│   └── KrithiEnums.kt
└── import/
    ├── ImportDto.kt
    ├── BatchDto.kt
    └── ImportEnums.kt
```

**Files:**
- `modules/shared/domain/src/commonMain/kotlin/com/sangita/grantha/shared/domain/model/`

---

## Progress Tracking

### Sprint 1 (Security & Critical)
| Item | Status | Assignee | Notes |
|------|--------|----------|-------|
| 3.1 JWT Authentication | [x] | Sangeetha Grantha Architect | JWT + refresh + claims wired |
| 3.2 API Key Headers | [x] | Sangeetha Grantha Architect | Header-based auth + redacted logging |
| 1.1 Service Interfaces | [x] | Sangeetha Grantha Architect | Interfaces + impls + DI wiring |
| 2.4 Complete TODOs | [x] | Sangeetha Grantha Architect | Auth user context + validation endpoint |

### Sprint 2 (Performance & Quality)
| Item | Status | Assignee | Notes |
|------|--------|----------|-------|
| 4.1 Optimize Search | [x] | Sangeetha Grantha Architect | JOIN rewrite + indexes (materialized view deferred) |
| 2.1 Magic Numbers | [x] | Sangeetha Grantha Architect | Constants centralized |
| 2.2 Preconditions | [x] | Sangeetha Grantha Architect | Require/check helpers applied |
| 5.1 Test Database | [x] | Sangeetha Grantha Architect | H2 factory + fixtures |

### Sprint 3 (Architecture & Maintenance)
| Item | Status | Assignee | Notes |
|------|--------|----------|-------|
| 1.2 Decompose Workers | [x] | Sangeetha Grantha Architect | Bulk import split into components |
| 1.3 Koin DI | [x] | Sangeetha Grantha Architect | Koin modules + App.kt wiring |
| 6.1 KDoc Comments | [x] | Sangeetha Grantha Architect | Services + repositories + packages |
| 4.2 Response Caching | [x] | Sangeetha Grantha Architect | Cache headers + ETag |

---

## Completion Criteria

A refactoring item is considered complete when:

1. Code changes are implemented
2. Existing tests pass
3. New tests are added (if applicable)
4. Code review approved
5. Documentation updated
6. No regression in functionality

---

*Checklist generated from automated code review. Update status as items are completed.*