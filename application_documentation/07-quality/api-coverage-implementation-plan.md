| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-01-26 |
| **Author** | Sangeetha Grantha Team |

# API Coverage Gap Implementation Plan


---


**Date:** 2026-01-27  
**Status:** Implementation Plan  
**Priority:** High - Blocking Critical Functionality

---

## Executive Summary

Based on comprehensive analysis of two API coverage reports (`API_Coverage_Report.md` and `frontend-backend-api-coverage-report.md`), this document provides a detailed implementation plan to address:

1. **Critical Gaps:** 3 missing lyric variant management endpoints (blocking Lyrics tab functionality)
2. **Path Standardization:** All authenticated/admin endpoints must use `/v1/admin/` prefix for consistency
3. **RBAC Implementation:** Comprehensive role-based access control for various user categories
4. **User Management:** Standardized user management routes (all authenticated users can add/remove users)
5. **Implementation Strategy:** Step-by-step guide following existing codebase patterns

**Impact:** Users cannot create, update, or manage lyric variant sections, blocking core Content Management workflow. Path inconsistencies create confusion and maintenance challenges. RBAC gaps limit fine-grained access control.

---

## 1. Critical Gaps: Missing Lyric Variant Endpoints

### 1.1 Gap Analysis

The frontend expects the following endpoints that do not exist in the backend:

| Endpoint | Method | Frontend Path | Expected Backend Path | Status |
|----------|--------|---------------|----------------------|--------|
| Create Lyric Variant | POST | `/admin/krithis/{id}/variants` | `/v1/admin/krithis/{id}/variants` | ❌ Missing |
| Update Lyric Variant | PUT | `/admin/variants/{id}` | `/v1/admin/variants/{id}` | ❌ Missing |
| Save Variant Sections | POST | `/admin/variants/{id}/sections` | `/v1/admin/variants/{id}/sections` | ❌ Missing |

**Current State:**
- ✅ `GET /v1/admin/krithis/{id}/variants` exists (read-only)
- ✅ Database tables exist: `krithi_lyric_variants`, `krithi_lyric_sections`
- ✅ Repository has `getLyricVariants()` method
- ❌ No create/update/save-sections methods in repository
- ❌ No service methods in `KrithiService`
- ❌ No route handlers in `AdminKrithiRoutes`

### 1.2 Implementation Pattern Reference

The notation variant implementation (`AdminNotationRoutes.kt`, `KrithiNotationService.kt`) provides an excellent reference pattern:

```kotlin
// Pattern: POST /v1/krithis/{id}/notation/variants
post("/krithis/{id}/notation/variants") {
    val id = parseUuidParam(call.parameters["id"], "krithiId")
    val request = call.receive<NotationVariantCreateRequest>()
    val created = notationService.createVariant(id, request)
    call.respond(HttpStatusCode.Created, created)
}

// Pattern: PUT /v1/notation/variants/{variantId}
put("/notation/variants/{variantId}") {
    val variantId = parseUuidParam(call.parameters["variantId"], "variantId")
    val request = call.receive<NotationVariantUpdateRequest>()
    val updated = notationService.updateVariant(variantId, request)
    call.respond(updated)
}
```

**Key Observations:**
- Notation routes are under `/v1/` (not `/v1/admin/`) but require admin auth
- Update/Delete routes use standalone paths (`/v1/notation/variants/{id}`)
- Create routes are nested under krithi (`/v1/krithis/{id}/notation/variants`)
- All mutations log to audit logs

### 1.3 Frontend Expected Payloads

From `KrithiEditor.tsx` analysis:

**Create Variant:**
```typescript
{
    language: LanguageCodeDto,
    script: ScriptCodeDto,
    transliterationScheme: string | null,
    sampradayaId: string | null,
    isPrimary: boolean
}
```

**Update Variant:** (same structure as create)

**Save Variant Sections:**
```typescript
{
    sections: Array<{
        sectionId: string,
        text: string
    }>
}
```

---

## 2. API Path Standardization: All Admin Routes to `/v1/admin/`

### 2.1 Path Standardization Requirement

**Policy:** All authenticated routes that require specific roles for CRUD operations MUST use the `/v1/admin/` prefix. This provides:
- Semantic clarity (clear distinction between public and admin endpoints)
- Consistent routing patterns
- Easier RBAC implementation and maintenance
- Alignment with frontend expectations

### 2.2 Routes Requiring Standardization

#### Notation Routes (Currently `/v1/`, Should be `/v1/admin/`)

| Operation | Current Backend Path | Target Backend Path | Frontend Path |
|-----------|---------------------|---------------------|---------------|
| Create Notation | `/v1/krithis/{id}/notation/variants` | `/v1/admin/krithis/{id}/notation/variants` | `/admin/krithis/{id}/notation/variants` |
| Update Notation | `/v1/notation/variants/{variantId}` | `/v1/admin/notation/variants/{variantId}` | `/admin/notation/variants/{variantId}` |
| Delete Notation | `/v1/notation/variants/{variantId}` | `/v1/admin/notation/variants/{variantId}` | `/admin/notation/variants/{variantId}` |
| Create Row | `/v1/notation/variants/{variantId}/rows` | `/v1/admin/notation/variants/{variantId}/rows` | `/admin/notation/variants/{variantId}/rows` |
| Update Row | `/v1/notation/rows/{rowId}` | `/v1/admin/notation/rows/{rowId}` | `/admin/notation/rows/{rowId}` |
| Delete Row | `/v1/notation/rows/{rowId}` | `/v1/admin/notation/rows/{rowId}` | `/admin/notation/rows/{rowId}` |

#### Krithi Mutation Routes (Currently `/v1/`, Should be `/v1/admin/`)

| Operation | Current Backend Path | Target Backend Path |
|-----------|---------------------|---------------------|
| Create Krithi | `/v1/krithis` | `/v1/admin/krithis` |
| Update Krithi | `/v1/krithis/{id}` | `/v1/admin/krithis/{id}` |

#### Import Routes (Currently `/v1/imports/`, Should be `/v1/admin/imports/`)

| Operation | Current Backend Path | Target Backend Path |
|-----------|---------------------|---------------------|
| Submit Imports | `/v1/imports/krithis` | `/v1/admin/imports/krithis` |
| Review Import | `/v1/imports/{id}/review` | `/v1/admin/imports/{id}/review` |

**Note:** List imports endpoint already uses `/v1/admin/imports`, so only the mutation routes need updating.

### 2.3 Routes Already Correctly Standardized

The following routes already use `/v1/admin/` prefix correctly:
- ✅ `/v1/admin/composers` (CRUD)
- ✅ `/v1/admin/ragas` (CRUD)
- ✅ `/v1/admin/talas` (CRUD)
- ✅ `/v1/admin/temples` (CRUD)
- ✅ `/v1/admin/tags` (CRUD)
- ✅ `/v1/admin/krithis/{id}/sections` (GET/POST)
- ✅ `/v1/admin/krithis/{id}/variants` (GET)
- ✅ `/v1/admin/krithis/{id}/tags` (GET)
- ✅ `/v1/admin/krithis/{id}/transliterate` (POST)
- ✅ `/v1/admin/krithis/{id}/validate` (POST)
- ✅ `/v1/admin/dashboard/stats` (GET)
- ✅ `/v1/admin/imports` (GET)
- ✅ `/v1/admin/imports/scrape` (POST)

---

## 3. Implementation Plan

### Phase 1: Implement Missing Lyric Variant Endpoints (HIGH PRIORITY)

#### Step 1.1: Create Request DTOs

**File:** `modules/backend/api/src/main/kotlin/com/sangita/grantha/backend/api/models/KrithiRequests.kt`

Add the following request models (append to existing file):

```kotlin
@Serializable
data class LyricVariantCreateRequest(
    val language: LanguageCodeDto,
    val script: ScriptCodeDto,
    val transliterationScheme: String? = null,
    val sampradayaId: String? = null,
    val variantLabel: String? = null,
    val sourceReference: String? = null,
    val lyrics: String = "", // Full lyrics text (can be empty initially)
    val isPrimary: Boolean = false,
)

@Serializable
data class LyricVariantUpdateRequest(
    val language: LanguageCodeDto? = null,
    val script: ScriptCodeDto? = null,
    val transliterationScheme: String? = null,
    val sampradayaId: String? = null,
    val variantLabel: String? = null,
    val sourceReference: String? = null,
    val lyrics: String? = null,
    val isPrimary: Boolean? = null,
)

@Serializable
data class LyricVariantSectionRequest(
    val sectionId: String,
    val text: String,
)

@Serializable
data class SaveLyricVariantSectionsRequest(
    val sections: List<LyricVariantSectionRequest>
)
```

**Dependencies:**
- `LanguageCodeDto` (from `shared/domain`)
- `ScriptCodeDto` (from `shared/domain`)
- `kotlinx.serialization.Serializable`

#### Step 1.2: Implement Repository Methods

**File:** `modules/backend/dal/src/main/kotlin/com/sangita/grantha/backend/dal/repositories/KrithiRepository.kt`

Add the following methods:

```kotlin
suspend fun createLyricVariant(
    krithiId: Uuid,
    language: LanguageCode,
    script: ScriptCode,
    transliterationScheme: String? = null,
    sampradayaId: UUID? = null,
    variantLabel: String? = null,
    sourceReference: String? = null,
    lyrics: String,
    isPrimary: Boolean = false,
    createdByUserId: UUID? = null,
    updatedByUserId: UUID? = null
): KrithiLyricVariantDto = DatabaseFactory.dbQuery {
    val now = OffsetDateTime.now(ZoneOffset.UTC)
    val variantId = UUID.randomUUID()
    
    KrithiLyricVariantsTable.insert {
        it[id] = variantId
        it[KrithiLyricVariantsTable.krithiId] = krithiId.toJavaUuid()
        it[KrithiLyricVariantsTable.language] = language
        it[KrithiLyricVariantsTable.script] = script
        it[KrithiLyricVariantsTable.transliterationScheme] = transliterationScheme
        it[KrithiLyricVariantsTable.sampradayaId] = sampradayaId
        it[KrithiLyricVariantsTable.variantLabel] = variantLabel
        it[KrithiLyricVariantsTable.sourceReference] = sourceReference
        it[KrithiLyricVariantsTable.lyrics] = lyrics
        it[KrithiLyricVariantsTable.isPrimary] = isPrimary
        it[KrithiLyricVariantsTable.createdByUserId] = createdByUserId
        it[KrithiLyricVariantsTable.updatedByUserId] = updatedByUserId
        it[KrithiLyricVariantsTable.createdAt] = now
        it[KrithiLyricVariantsTable.updatedAt] = now
    }
    
    KrithiLyricVariantsTable
        .selectAll()
        .where { KrithiLyricVariantsTable.id eq variantId }
        .map { it.toKrithiLyricVariantDto() }
        .single()
}

suspend fun updateLyricVariant(
    variantId: Uuid,
    language: LanguageCode? = null,
    script: ScriptCode? = null,
    transliterationScheme: String? = null,
    sampradayaId: UUID? = null,
    variantLabel: String? = null,
    sourceReference: String? = null,
    lyrics: String? = null,
    isPrimary: Boolean? = null,
    updatedByUserId: UUID? = null
): KrithiLyricVariantDto? = DatabaseFactory.dbQuery {
    val now = OffsetDateTime.now(ZoneOffset.UTC)
    val updated = KrithiLyricVariantsTable.update({ KrithiLyricVariantsTable.id eq variantId.toJavaUuid() }) {
        language?.let { value -> it[KrithiLyricVariantsTable.language] = value }
        script?.let { value -> it[KrithiLyricVariantsTable.script] = value }
        transliterationScheme?.let { value -> it[KrithiLyricVariantsTable.transliterationScheme] = value }
        sampradayaId?.let { value -> it[KrithiLyricVariantsTable.sampradayaId] = value }
        variantLabel?.let { value -> it[KrithiLyricVariantsTable.variantLabel] = value }
        sourceReference?.let { value -> it[KrithiLyricVariantsTable.sourceReference] = value }
        lyrics?.let { value -> it[KrithiLyricVariantsTable.lyrics] = value }
        isPrimary?.let { value -> it[KrithiLyricVariantsTable.isPrimary] = value }
        updatedByUserId?.let { value -> it[KrithiLyricVariantsTable.updatedByUserId] = value }
        it[KrithiLyricVariantsTable.updatedAt] = now
    }
    
    if (updated == 0) {
        return@dbQuery null
    }
    
    KrithiLyricVariantsTable
        .selectAll()
        .where { KrithiLyricVariantsTable.id eq variantId.toJavaUuid() }
        .map { it.toKrithiLyricVariantDto() }
        .singleOrNull()
}

suspend fun saveLyricVariantSections(
    variantId: Uuid,
    sections: List<Pair<UUID, String>> // (sectionId, text)
) = DatabaseFactory.dbQuery {
    val now = OffsetDateTime.now(ZoneOffset.UTC)
    val javaVariantId = variantId.toJavaUuid()
    
    // Delete existing sections for this variant
    KrithiLyricSectionsTable.deleteWhere { 
        KrithiLyricSectionsTable.lyricVariantId eq javaVariantId 
    }
    
    // Insert new sections
    if (sections.isNotEmpty()) {
        KrithiLyricSectionsTable.batchInsert(sections) { (sectionId, text) ->
            this[KrithiLyricSectionsTable.id] = UUID.randomUUID()
            this[KrithiLyricSectionsTable.lyricVariantId] = javaVariantId
            this[KrithiLyricSectionsTable.sectionId] = sectionId
            this[KrithiLyricSectionsTable.text] = text
            this[KrithiLyricSectionsTable.normalizedText] = null // Can be computed later if needed
            this[KrithiLyricSectionsTable.createdAt] = now
            this[KrithiLyricSectionsTable.updatedAt] = now
        }
    }
}
```

**Reference Implementation:** See `KrithiRepository.saveSections()` for similar pattern.

#### Step 1.3: Implement Service Methods

**File:** `modules/backend/api/src/main/kotlin/com/sangita/grantha/backend/api/services/KrithiService.kt`

Add the following methods:

```kotlin
suspend fun createLyricVariant(
    krithiId: Uuid,
    request: com.sangita.grantha.backend.api.models.LyricVariantCreateRequest
): KrithiLyricVariantDto {
    // Verify krithi exists
    val krithi = dal.krithis.findById(krithiId) 
        ?: throw NoSuchElementException("Krithi not found")
    
    val sampradayaId = request.sampradayaId?.let { parseUuidOrThrow(it, "sampradayaId") }
    
    val created = dal.krithis.createLyricVariant(
        krithiId = krithiId,
        language = LanguageCode.valueOf(request.language.name),
        script = ScriptCode.valueOf(request.script.name),
        transliterationScheme = request.transliterationScheme,
        sampradayaId = sampradayaId,
        variantLabel = request.variantLabel,
        sourceReference = request.sourceReference,
        lyrics = request.lyrics,
        isPrimary = request.isPrimary,
        createdByUserId = null, // TODO: Extract from auth context
        updatedByUserId = null
    )
    
    dal.auditLogs.append(
        action = "CREATE_LYRIC_VARIANT",
        entityTable = "krithi_lyric_variants",
        entityId = created.id
    )
    
    return created
}

suspend fun updateLyricVariant(
    variantId: Uuid,
    request: com.sangita.grantha.backend.api.models.LyricVariantUpdateRequest
): KrithiLyricVariantDto {
    val sampradayaId = request.sampradayaId?.let { parseUuidOrThrow(it, "sampradayaId") }
    
    val updated = dal.krithis.updateLyricVariant(
        variantId = variantId,
        language = request.language?.let { LanguageCode.valueOf(it.name) },
        script = request.script?.let { ScriptCode.valueOf(it.name) },
        transliterationScheme = request.transliterationScheme,
        sampradayaId = sampradayaId,
        variantLabel = request.variantLabel,
        sourceReference = request.sourceReference,
        lyrics = request.lyrics,
        isPrimary = request.isPrimary,
        updatedByUserId = null // TODO: Extract from auth context
    ) ?: throw NoSuchElementException("Lyric variant not found")
    
    dal.auditLogs.append(
        action = "UPDATE_LYRIC_VARIANT",
        entityTable = "krithi_lyric_variants",
        entityId = updated.id
    )
    
    return updated
}

suspend fun saveLyricVariantSections(
    variantId: Uuid,
    sections: List<com.sangita.grantha.backend.api.models.LyricVariantSectionRequest>
) {
    // Verify variant exists
    val variant = dal.krithis.findLyricVariantById(variantId)
        ?: throw NoSuchElementException("Lyric variant not found")
    
    val sectionsData = sections.map { 
        parseUuidOrThrow(it.sectionId, "sectionId") to it.text 
    }
    
    dal.krithis.saveLyricVariantSections(variantId, sectionsData)
    
    dal.auditLogs.append(
        action = "UPDATE_LYRIC_VARIANT_SECTIONS",
        entityTable = "krithi_lyric_sections",
        entityId = variantId
    )
}

// Helper method needed in repository
suspend fun findLyricVariantById(variantId: Uuid): KrithiLyricVariantDto? = DatabaseFactory.dbQuery {
    KrithiLyricVariantsTable
        .selectAll()
        .where { KrithiLyricVariantsTable.id eq variantId.toJavaUuid() }
        .map { it.toKrithiLyricVariantDto() }
        .singleOrNull()
}
```

**Note:** Add `findLyricVariantById` to `KrithiRepository` if it doesn't exist.

#### Step 1.4: Add Route Handlers

**File:** `modules/backend/api/src/main/kotlin/com/sangita/grantha/backend/api/routes/AdminKrithiRoutes.kt`

Add routes within the existing `/v1/admin/krithis` route block:

```kotlin
// Add after the existing get("/{id}/variants") route (around line 83)

post("/{id}/variants") {
    val id = parseUuidParam(call.parameters["id"], "krithiId")
        ?: return@post call.respondText("Missing krithi ID", status = HttpStatusCode.BadRequest)
    val request = call.receive<com.sangita.grantha.backend.api.models.LyricVariantCreateRequest>()
    val created = krithiService.createLyricVariant(id, request)
    call.respond(HttpStatusCode.Created, created)
}

// Add new route block for variant-level operations
route("/v1/admin/variants") {
    put("/{id}") {
        val id = parseUuidParam(call.parameters["id"], "variantId")
            ?: return@put call.respondText("Missing variant ID", status = HttpStatusCode.BadRequest)
        val request = call.receive<com.sangita.grantha.backend.api.models.LyricVariantUpdateRequest>()
        val updated = krithiService.updateLyricVariant(id, request)
        call.respond(updated)
    }
    
    post("/{id}/sections") {
        val id = parseUuidParam(call.parameters["id"], "variantId")
            ?: return@post call.respondText("Missing variant ID", status = HttpStatusCode.BadRequest)
        val request = call.receive<com.sangita.grantha.backend.api.models.SaveLyricVariantSectionsRequest>()
        krithiService.saveLyricVariantSections(id, request.sections)
        call.respond(HttpStatusCode.NoContent)
    }
}
```

**Route Structure Decision:**
- Create: `/v1/admin/krithis/{id}/variants` (nested under krithi, matches GET pattern)
- Update/Sections: `/v1/admin/variants/{id}` (standalone, matches notation pattern)

**Alternative:** Could nest all under `/v1/admin/krithis/{krithiId}/variants/{variantId}`, but the current pattern matches notation routes.

### Phase 2: Standardize All Admin Routes to `/v1/admin/` (HIGH PRIORITY)

**Policy Enforcement:** All authenticated routes performing CRUD operations MUST use `/v1/admin/` prefix.

#### Step 2.1: Update Notation Routes

**File:** `modules/backend/api/src/main/kotlin/com/sangita/grantha/backend/api/routes/AdminNotationRoutes.kt`

Change route prefix from `/v1` to `/v1/admin`:

```kotlin
fun Route.adminNotationRoutes(notationService: KrithiNotationService) {
    route("/v1/admin") {  // Changed from "/v1"
        post("/krithis/{id}/notation/variants") {
            // ... existing code ...
        }
        
        put("/notation/variants/{variantId}") {
            // ... existing code ...
        }
        
        delete("/notation/variants/{variantId}") {
            // ... existing code ...
        }
        
        post("/notation/variants/{variantId}/rows") {
            // ... existing code ...
        }
        
        put("/notation/rows/{rowId}") {
            // ... existing code ...
        }
        
        delete("/notation/rows/{rowId}") {
            // ... existing code ...
        }
    }
}
```

**Impact:** Backend paths become:
- `/v1/admin/krithis/{id}/notation/variants`
- `/v1/admin/notation/variants/{variantId}`
- `/v1/admin/notation/variants/{variantId}/rows`
- `/v1/admin/notation/rows/{rowId}`

#### Step 2.2: Update Krithi Mutation Routes

**File:** `modules/backend/api/src/main/kotlin/com/sangita/grantha/backend/api/routes/AdminKrithiRoutes.kt`

Move POST and PUT routes from `/v1/krithis` to `/v1/admin/krithis`:

```kotlin
fun Route.adminKrithiRoutes(
    krithiService: KrithiService,
    transliterationService: TransliterationService
) {
    route("/v1") {
        // Remove post("/krithis") and put("/krithis/{id}") from here
        // Move them to route("/v1/admin/krithis") block below
    }
    
    route("/v1/admin/krithis") {
        post {
            val request = call.receive<KrithiCreateRequest>()
            val created = krithiService.createKrithi(request)
            call.respond(HttpStatusCode.Created, created)
        }

        put("/{id}") {
            val id = parseUuidParam(call.parameters["id"], "krithiId")
                ?: return@put call.respondText("Missing krithi ID", status = HttpStatusCode.BadRequest)
            val request = call.receive<KrithiUpdateRequest>()
            val updated = krithiService.updateKrithi(id, request)
            call.respond(updated)
        }
        
        // ... existing routes (sections, variants, tags, etc.) ...
    }
}
```

#### Step 2.3: Update Import Routes

**File:** `modules/backend/api/src/main/kotlin/com/sangita/grantha/backend/api/routes/ImportRoutes.kt`

Change route prefix from `/v1/imports` to `/v1/admin/imports`:

```kotlin
fun Route.importRoutes(
    importService: ImportService,
    webScrapingService: WebScrapingService
) {
    route("/v1/admin/imports") {  // Changed from "/v1/imports"
        post("/krithis") {
            val requests = call.receive<List<ImportKrithiRequest>>()
            val created = importService.submitImports(requests)
            call.respond(HttpStatusCode.Accepted, created)
        }
        
        // ... existing routes ...
    }
    
    route("/v1/admin/imports") {
        // Move review route here as well
        post("/{id}/review") {
            // ... existing code ...
        }
    }
}
```

**Note:** The GET `/v1/admin/imports` route already exists, so only mutation routes need updating.

#### Step 2.4: Update Frontend Client (if needed)

**File:** `modules/frontend/sangita-admin-web/src/api/client.ts`

Verify frontend paths match the new backend paths. The frontend already uses `/admin/` prefix, so minimal changes should be needed. However, ensure all paths are consistent.

**Breaking Changes:** These changes are breaking changes for any external API consumers. Consider versioning or migration strategy if there are external clients.

### Phase 5: Testing & Validation

#### Step 5.1: Unit Tests

**Repository Tests:**
- `KrithiRepositoryTest.kt` (create if doesn't exist)
  - Test `createLyricVariant()` with valid inputs
  - Test `updateLyricVariant()` updates correct fields
  - Test `saveLyricVariantSections()` replaces existing sections
  - Test error cases (invalid krithi ID, invalid variant ID)

**Service Tests:**
- `KrithiServiceTest.kt`
  - Test service methods delegate to repository correctly
  - Test audit logging occurs
  - Test error handling

#### Step 5.2: Integration Tests

**File:** `modules/backend/api/src/test/kotlin/com/sangita/grantha/backend/api/integration/`

Create or extend integration tests:

```kotlin
class LyricVariantRoutesTest {
    @Test
    fun `POST admin krithis variants creates variant`() {
        // Test create endpoint
    }
    
    @Test
    fun `PUT admin variants updates variant`() {
        // Test update endpoint
    }
    
    @Test
    fun `POST admin variants sections saves sections`() {
        // Test save sections endpoint
    }
}
```

#### Step 5.3: Manual Testing Checklist

- [ ] Create new lyric variant via UI
- [ ] Update existing lyric variant via UI
- [ ] Save variant sections via UI
- [ ] Verify audit logs are created
- [ ] Test error cases (invalid IDs, missing fields)
- [ ] Test notation routes work with standardized `/v1/admin/` paths
- [ ] Test user management routes (create, update, delete users)
- [ ] Test role assignment routes
- [ ] Test RBAC permissions (verify users can only access allowed resources)
- [ ] Test all authenticated users can manage users (policy verification)

### Phase 3: Implement User Management Routes (MEDIUM PRIORITY)

**Policy:** All authenticated users have privilege to add and remove users (universal user management access).

#### Step 3.1: Create User Management Routes

**File:** `modules/backend/api/src/main/kotlin/com/sangita/grantha/backend/api/routes/UserManagementRoutes.kt` (create new file)

```kotlin
package com.sangita.grantha.backend.api.routes

import com.sangita.grantha.backend.api.models.*
import com.sangita.grantha.backend.api.services.UserManagementService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route

fun Route.userManagementRoutes(userService: UserManagementService) {
    route("/v1/admin/users") {
        get {
            val users = userService.listUsers()
            call.respond(users)
        }
        
        get("/{id}") {
            val id = parseUuidParam(call.parameters["id"], "userId")
                ?: return@get call.respondText("Missing user ID", status = HttpStatusCode.BadRequest)
            val user = userService.getUser(id)
            if (user == null) {
                call.respondText("User not found", status = HttpStatusCode.NotFound)
            } else {
                call.respond(user)
            }
        }
        
        post {
            val request = call.receive<UserCreateRequest>()
            val created = userService.createUser(request)
            call.respond(HttpStatusCode.Created, created)
        }
        
        put("/{id}") {
            val id = parseUuidParam(call.parameters["id"], "userId")
                ?: return@put call.respondText("Missing user ID", status = HttpStatusCode.BadRequest)
            val request = call.receive<UserUpdateRequest>()
            val updated = userService.updateUser(id, request)
            if (updated == null) {
                call.respondText("User not found", status = HttpStatusCode.NotFound)
            } else {
                call.respond(updated)
            }
        }
        
        delete("/{id}") {
            val id = parseUuidParam(call.parameters["id"], "userId")
                ?: return@delete call.respondText("Missing user ID", status = HttpStatusCode.BadRequest)
            val deleted = userService.deleteUser(id)
            if (deleted) {
                call.respond(HttpStatusCode.NoContent)
            } else {
                call.respondText("User not found", status = HttpStatusCode.NotFound)
            }
        }
        
        // Role assignment routes
        post("/{id}/roles") {
            val id = parseUuidParam(call.parameters["id"], "userId")
                ?: return@post call.respondText("Missing user ID", status = HttpStatusCode.BadRequest)
            val request = call.receive<AssignRoleRequest>()
            userService.assignRole(id, request.roleCode)
            call.respond(HttpStatusCode.NoContent)
        }
        
        delete("/{id}/roles/{roleCode}") {
            val id = parseUuidParam(call.parameters["id"], "userId")
                ?: return@delete call.respondText("Missing user ID", status = HttpStatusCode.BadRequest)
            val roleCode = call.parameters["roleCode"]
                ?: return@delete call.respondText("Missing role code", status = HttpStatusCode.BadRequest)
            val removed = userService.removeRole(id, roleCode)
            if (removed) {
                call.respond(HttpStatusCode.NoContent)
            } else {
                call.respondText("Role assignment not found", status = HttpStatusCode.NotFound)
            }
        }
        
        get("/{id}/roles") {
            val id = parseUuidParam(call.parameters["id"], "userId")
                ?: return@get call.respondText("Missing user ID", status = HttpStatusCode.BadRequest)
            val roles = userService.getUserRoles(id)
            call.respond(roles)
        }
    }
}
```

**Request DTOs** (add to `modules/backend/api/src/main/kotlin/com/sangita/grantha/backend/api/models/UserRequests.kt`):

```kotlin
@Serializable
data class UserCreateRequest(
    val email: String? = null,
    val fullName: String,
    val displayName: String? = null,
    val password: String? = null, // Optional if using external auth
    val isActive: Boolean = true,
    val roleCodes: List<String> = emptyList(), // Initial roles
)

@Serializable
data class UserUpdateRequest(
    val email: String? = null,
    val fullName: String? = null,
    val displayName: String? = null,
    val password: String? = null,
    val isActive: Boolean? = null,
)

@Serializable
data class AssignRoleRequest(
    val roleCode: String,
)
```

**Note:** User management routes are accessible to all authenticated users (no additional RBAC checks beyond authentication).

### Phase 4: Implement Comprehensive RBAC System (HIGH PRIORITY)

**Policy:** Implement fine-grained role-based access control for various categories of users performing CRUD operations on Sangita Grantha content.

#### Step 4.1: RBAC Architecture Overview

The system already has foundational RBAC infrastructure:
- `roles` table (code, name, capabilities JSONB)
- `role_assignments` table (user_id, role_code)
- Authentication system in place

**Required Enhancements:**

1. **Capability-Based Authorization:** Use JSONB capabilities in roles table to define fine-grained permissions
2. **Authorization Middleware:** Create authorization interceptors for route-level permission checks
3. **Role Definitions:** Define standard roles with capabilities (e.g., `admin`, `editor`, `reviewer`, `viewer`)
4. **Permission Model:** Implement capability-based permission checking

#### Step 4.2: Define Standard Roles and Capabilities

**Capability Structure (JSONB):**

```json
{
  "krithis": {
    "create": true,
    "read": true,
    "update": true,
    "delete": true,
    "publish": true
  },
  "composers": {
    "create": true,
    "read": true,
    "update": true,
    "delete": false
  },
  "notation": {
    "create": true,
    "read": true,
    "update": true,
    "delete": false
  },
  "users": {
    "manage": true  // All authenticated users have this
  }
}
```

**Standard Roles:**

| Role Code | Description | Key Capabilities |
|-----------|-------------|------------------|
| `super_admin` | Full system access | All capabilities: true |
| `admin` | Content management | CRUD on all content, no user management restrictions |
| `editor` | Content editing | Create/Update on krithis, composers, notation; no delete/publish |
| `reviewer` | Content review | Read all, update workflow state, no delete |
| `viewer` | Read-only access | Read capabilities only |

#### Step 4.3: Implement Authorization Service

**File:** `modules/backend/api/src/main/kotlin/com/sangita/grantha/backend/api/services/AuthorizationService.kt` (create new file)

```kotlin
package com.sangita.grantha.backend.api.services

import com.sangita.grantha.backend.dal.SangitaDal
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.uuid.Uuid

data class Permission(
    val resource: String, // e.g., "krithis", "composers"
    val action: String    // e.g., "create", "read", "update", "delete"
)

class AuthorizationService(private val dal: SangitaDal) {
    suspend fun hasPermission(userId: Uuid, permission: Permission): Boolean {
        // Get user roles
        val roles = dal.users.getUserRoles(userId)
        
        // Check if any role has the required capability
        return roles.any { role ->
            val capabilities = role.capabilities as? JsonObject ?: return@any false
            val resourceCap = capabilities[permission.resource] as? JsonObject ?: return@any false
            val actionValue = resourceCap[permission.action]?.jsonPrimitive?.content
            actionValue == "true" || actionValue == true.toString()
        }
    }
    
    suspend fun requirePermission(userId: Uuid, permission: Permission) {
        if (!hasPermission(userId, permission)) {
            throw SecurityException("User does not have permission: ${permission.resource}.${permission.action}")
        }
    }
}
```

#### Step 4.4: Create Authorization Interceptor

**File:** `modules/backend/api/src/main/kotlin/com/sangita/grantha/backend/api/middleware/AuthorizationInterceptor.kt` (create new file)

Create route interceptors that check permissions before allowing access. This can be integrated into route handlers or as a Ktor feature.

#### Step 4.5: Apply RBAC to Routes

Update route handlers to check permissions:

```kotlin
// Example in AdminKrithiRoutes.kt
post("/v1/admin/krithis") {
    val userId = getCurrentUserId(call) // Extract from auth context
    authorizationService.requirePermission(userId, Permission("krithis", "create"))
    
    val request = call.receive<KrithiCreateRequest>()
    val created = krithiService.createKrithi(request)
    call.respond(HttpStatusCode.Created, created)
}
```

#### Step 4.6: Role Management Routes

Add role management routes (admin-only):

**File:** `modules/backend/api/src/main/kotlin/com/sangita/grantha/backend/api/routes/RoleManagementRoutes.kt` (create new file)

```kotlin
route("/v1/admin/roles") {
    get {
        // List all roles (admin only)
    }
    
    get("/{code}") {
        // Get role details
    }
    
    post {
        // Create role (super_admin only)
    }
    
    put("/{code}") {
        // Update role capabilities (super_admin only)
    }
    
    get("/{code}/users") {
        // List users with this role
    }
}
```

**Note:** User management (add/remove users) remains accessible to all authenticated users as per policy. Role assignment and role definition management require appropriate permissions.

---

## 4. Implementation Order & Dependencies

### Recommended Implementation Sequence

**Critical Path (Blocking Features):**
1. **Phase 1.1-1.4:** Implement Lyric Variant Endpoints (blocking Lyrics tab functionality)
2. **Phase 2:** Standardize All Admin Routes to `/v1/admin/` (required for consistency and RBAC)
3. **Phase 4:** Implement RBAC System (required for fine-grained access control)
4. **Phase 3:** User Management Routes (can be implemented in parallel with Phase 4)
5. **Phase 5:** Testing (depends on all implementation phases)

**Parallel Work:**
- Phase 1 and Phase 2 can be done in parallel (different files)
- Phase 3 and Phase 4 can be done in parallel (different concerns)
- Testing should follow each phase

### Dependencies

- **Phase 1** → No dependencies (new functionality)
- **Phase 2** → Should be done before Phase 4 (RBAC needs standardized paths)
- **Phase 3** → Can be done independently (user management is universal)
- **Phase 4** → Depends on Phase 2 (needs standardized `/v1/admin/` paths)
- **Phase 5** → Depends on all phases

### Critical Path Summary

1. **Phase 1:** Lyric variant endpoints (blocking feature)
2. **Phase 2:** Path standardization (foundation for RBAC)
3. **Phase 4:** RBAC implementation (access control)
4. **Phase 3:** User management (independent, can be parallel)
5. **Phase 5:** Testing and validation

---

## 5. Risk Assessment & Mitigation

### Risks

1. **Database Schema Mismatch:** Ensure `sampradayaId` column exists in `krithi_lyric_variants` table
   - **Mitigation:** Verify schema in `database/migrations/02__domain-tables.sql`
   
2. **Enum Mismatch:** Frontend sends DTOs, backend needs domain enums
   - **Mitigation:** Follow existing pattern from `KrithiService.createKrithi()`
   
3. **Audit Log Context:** User ID extraction from auth context
   - **Mitigation:** Follow pattern from existing routes (currently null, can enhance later)

4. **Route Conflicts:** Ensure new routes don't conflict with existing ones
   - **Mitigation:** Review routing configuration in `Routing.kt`

### Dependencies to Verify

- [ ] `LanguageCodeDto` and `ScriptCodeDto` exist in `shared/domain`
- [ ] Database migration includes `sampradayaId` in `krithi_lyric_variants`
- [ ] `KrithiLyricSectionsTable` schema matches expected structure
- [ ] `toKrithiLyricVariantDto()` mapper exists in DAL

---

## 6. Code Quality Considerations

### Follow Existing Patterns

1. **Error Handling:** Use `parseUuidParam()` for UUID validation
2. **HTTP Status Codes:** 
   - `201 Created` for POST (create)
   - `200 OK` for PUT (update)
   - `204 No Content` for POST (save sections)
   - `400 Bad Request` for validation errors
   - `404 Not Found` for missing entities
3. **Audit Logging:** All mutations must log to `AUDIT_LOG` table
4. **Transaction Safety:** Repository methods use `DatabaseFactory.dbQuery {}`
5. **Null Safety:** Use Kotlin nullable types appropriately

### Code Organization

- Request DTOs: `modules/backend/api/src/main/kotlin/.../models/`
- Repository Methods: `modules/backend/dal/src/main/kotlin/.../repositories/KrithiRepository.kt`
- Service Methods: `modules/backend/api/src/main/kotlin/.../services/KrithiService.kt`
- Routes: `modules/backend/api/src/main/kotlin/.../routes/AdminKrithiRoutes.kt`

---

## 7. Additional Observations from Coverage Reports

### Medium Priority (Not Blocking)

1. **Pagination:** Frontend shows pagination UI but doesn't call backend pagination API
   - Backend supports `page` and `pageSize` parameters
   - **Recommendation:** Implement frontend pagination in future iteration

2. **Server-Side Filtering:** Frontend uses client-side filtering
   - Backend supports multiple filter parameters
   - **Recommendation:** Move filtering to backend for better performance

3. **Validation Endpoint:** `POST /v1/admin/krithis/{id}/validate` exists but is a stub
   - **Recommendation:** Implement validation logic or remove endpoint

### Low Priority

4. **Error Handling:** Frontend should gracefully handle 404s for missing endpoints
   - **Recommendation:** Add error boundaries and user-friendly error messages

---

## 8. Success Criteria

### Phase 1 Complete When:
- [ ] All 3 lyric variant endpoints return expected responses
- [ ] Frontend can successfully create/update/save variant sections
- [ ] Audit logs are created for all mutations
- [ ] Integration tests pass
- [ ] Manual testing checklist completed

### Phase 2 Complete When:
- [ ] All admin routes use `/v1/admin/` prefix consistently
- [ ] Notation routes standardized to `/v1/admin/`
- [ ] Krithi mutation routes standardized to `/v1/admin/krithis`
- [ ] Import mutation routes standardized to `/v1/admin/imports`
- [ ] Frontend paths match backend paths
- [ ] Integration tests updated and passing
- [ ] Breaking changes documented (if any external API consumers)

### Phase 3 Complete When:
- [ ] User management routes implemented at `/v1/admin/users`
- [ ] All authenticated users can create/update/delete users
- [ ] Role assignment routes functional
- [ ] User list and detail endpoints working
- [ ] Integration tests passing

### Phase 4 Complete When:
- [ ] AuthorizationService implemented
- [ ] Standard roles defined with capabilities
- [ ] Route-level permission checks implemented
- [ ] Role management routes functional (admin-only)
- [ ] Permission system tested with various user roles
- [ ] Documentation updated with role capabilities

---

## 9. References

### Related Documents
- `application_documentation/API_Coverage_Report.md`
- `application_documentation/07-quality/frontend-backend-api-coverage-report.md`
- `application_documentation/06-backend/mutation-handlers.md` (audit logging patterns)

### Reference Implementations
- `modules/backend/api/src/main/kotlin/com/sangita/grantha/backend/api/routes/AdminNotationRoutes.kt`
- `modules/backend/api/src/main/kotlin/com/sangita/grantha/backend/api/services/KrithiNotationService.kt`
- `modules/backend/dal/src/main/kotlin/com/sangita/grantha/backend/dal/repositories/KrithiRepository.kt` (see `saveSections()`)

### Database Schema
- `database/migrations/02__domain-tables.sql` (tables: `krithi_lyric_variants`, `krithi_lyric_sections`)

---

## Appendix A: Quick Reference - Endpoint Summary

### New Endpoints to Implement (Phase 1)

```
POST   /v1/admin/krithis/{id}/variants
PUT    /v1/admin/variants/{id}
POST   /v1/admin/variants/{id}/sections
```

### Endpoints to Standardize (Phase 2 - Path Changes)

**Notation Routes:**
```
POST   /v1/krithis/{id}/notation/variants          → /v1/admin/krithis/{id}/notation/variants
PUT    /v1/notation/variants/{variantId}           → /v1/admin/notation/variants/{variantId}
DELETE /v1/notation/variants/{variantId}           → /v1/admin/notation/variants/{variantId}
POST   /v1/notation/variants/{variantId}/rows      → /v1/admin/notation/variants/{variantId}/rows
PUT    /v1/notation/rows/{rowId}                   → /v1/admin/notation/rows/{rowId}
DELETE /v1/notation/rows/{rowId}                   → /v1/admin/notation/rows/{rowId}
```

**Krithi Mutation Routes:**
```
POST   /v1/krithis                                 → /v1/admin/krithis
PUT    /v1/krithis/{id}                            → /v1/admin/krithis/{id}
```

**Import Routes:**
```
POST   /v1/imports/krithis                         → /v1/admin/imports/krithis
POST   /v1/imports/{id}/review                     → /v1/admin/imports/{id}/review
```

### New Endpoints to Implement (Phase 3 - User Management)

```
GET    /v1/admin/users
GET    /v1/admin/users/{id}
POST   /v1/admin/users
PUT    /v1/admin/users/{id}
DELETE /v1/admin/users/{id}
POST   /v1/admin/users/{id}/roles
DELETE /v1/admin/users/{id}/roles/{roleCode}
GET    /v1/admin/users/{id}/roles
```

### New Endpoints to Implement (Phase 4 - RBAC/Role Management)

```
GET    /v1/admin/roles
GET    /v1/admin/roles/{code}
POST   /v1/admin/roles                    (super_admin only)
PUT    /v1/admin/roles/{code}             (super_admin only)
GET    /v1/admin/roles/{code}/users
```

### Path Standardization Policy

**All authenticated/admin routes MUST follow this pattern:**
- **Public routes:** `/v1/{resource}` (e.g., `/v1/krithis/search`, `/v1/krithis/{id}`)
- **Admin/authenticated routes:** `/v1/admin/{resource}` (e.g., `/v1/admin/krithis`, `/v1/admin/composers`)
- **Admin nested resources:** `/v1/admin/{resource}/{id}/{subresource}` (e.g., `/v1/admin/krithis/{id}/variants`)

**Exceptions:**
- Health checks: `/health`, `/v1/health` (no auth required)
- Public read endpoints: `/v1/krithis/search`, `/v1/krithis/{id}` (optional auth for enhanced features)

---

---

## 10. RBAC Policy Summary

### User Management Policy
- **All authenticated users** have privilege to add, update, and remove users
- User management routes: `/v1/admin/users`
- No additional role checks required (beyond authentication)

### Role Assignment Policy
- Role assignment is a separate concern from user management
- Role assignment routes: `/v1/admin/users/{id}/roles`
- Role definitions management: `/v1/admin/roles` (super_admin only)

### Content Management Policy
- Fine-grained RBAC based on resource and action capabilities
- Resources: `krithis`, `composers`, `ragas`, `talas`, `temples`, `tags`, `notation`, `imports`
- Actions: `create`, `read`, `update`, `delete`, `publish` (resource-specific)
- Permission checking: Route-level authorization interceptors

### Standard Roles

| Role | User Management | Content CRUD | Publishing | Role Management |
|------|----------------|--------------|------------|-----------------|
| super_admin | ✅ | ✅ All | ✅ | ✅ |
| admin | ✅ | ✅ All | ✅ | ❌ |
| editor | ✅ | ✅ Create/Update | ❌ | ❌ |
| reviewer | ✅ | ✅ Read/Update State | ✅ | ❌ |
| viewer | ✅ | ✅ Read Only | ❌ | ❌ |

**Note:** All roles have user management privileges (add/remove users) as per policy.

---

**Document Status:** Ready for Implementation  
**Next Steps:** 
1. Begin Phase 1.1 (Create Request DTOs for lyric variants)
2. Begin Phase 2.1 (Standardize notation routes)
3. Plan Phase 4 (RBAC architecture design)
