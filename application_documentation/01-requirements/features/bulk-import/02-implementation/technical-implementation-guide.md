# Import Pipeline Technical Implementation Guide

| Metadata | Value |
|:---|:---|
| **Status** | Implementation Guide |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-01-20 |
| **Author** | Goose AI Analysis |
| **Related Documents** | 
| - [Krithi Bulk Import Capability Analysis](../archive/krithi-bulk-import-capability-analysis-goose.md) |
| - [Web Source Detailed Analysis](../03-sources/web-source-analysis.md) |
| - [Koog Evaluation](../archive/koog-evaluation-for-import-pipeline-goose.md) |

---

## 1. Overview

This document provides concrete technical implementation guidance for building the Krithi import pipeline. It includes code examples, architecture patterns, and step-by-step implementation recommendations.

---

## 2. Architecture Overview

### 2.1 Service Layer Architecture

```
┌─────────────────────────────────────────────────────┐
│              Import API Routes                        │
│  POST /v1/admin/imports/sources/{id}/scrape         │
│  POST /v1/admin/imports/batch                        │
│  GET  /v1/admin/imports/batches/{id}/status         │
└──────────────────────┬──────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────┐
│         ImportPipelineService                       │
│  (Orchestrates import workflow)                     │
└──────┬──────────┬──────────┬──────────┬───────────┘
       │          │          │          │
       ▼          ▼          ▼          ▼
┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐
│ Scraping│ │Extraction│ │  Entity  │ │Validation│
│ Service │ │ Service  │ │Resolver  │ │ Service  │
└─────────┘ └──────────┘ └──────────┘ └──────────┘
       │          │          │          │
       └──────────┴──────────┴──────────┘
                   │
       ┌───────────▼───────────┐
       │    Repositories       │
       │  (DAL Layer)          │
       └───────────────────────┘
```

### 2.2 Module Structure

```
modules/backend/api/src/main/kotlin/com/sangita/grantha/backend/api/
├── imports/
│   ├── ImportRoutes.kt              # API endpoints
│   ├── ImportRequests.kt            # Request DTOs
│   ├── ImportService.kt             # Main orchestration
│   ├── ImportPipelineService.kt     # Pipeline workflow
│   ├── scraping/
│   │   ├── SourceHandler.kt         # Interface
│   │   ├── KarnatikSourceHandler.kt
│   │   ├── BlogspotSourceHandler.kt
│   │   └── TempleNetSourceHandler.kt
│   ├── extraction/
│   │   ├── ExtractionService.kt
│   │   └── ExtractionPrompts.kt
│   ├── resolution/
│   │   ├── EntityResolutionService.kt
│   │   ├── ComposerResolver.kt
│   │   ├── RagaResolver.kt
│   │   ├── DeityResolver.kt
│   │   └── TempleResolver.kt
│   ├── cleansing/
│   │   └── DataCleansingService.kt
│   ├── deduplication/
│   │   └── DeduplicationService.kt
│   └── validation/
│       └── ValidationService.kt
```

---

## 3. Core Service Implementations

### 3.1 ImportPipelineService

**Purpose**: Orchestrate multi-stage import workflow

**Implementation:**

```kotlin
package com.sangita.grantha.backend.api.imports

import com.sangita.grantha.backend.dal.DatabaseFactory
import com.sangita.grantha.backend.dal.imports.ImportRepository
import com.sangita.grantha.shared.domain.model.ImportSourceDto
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.UUID

class ImportPipelineService(
    private val importRepository: ImportRepository,
    private val scrapingService: ScrapingService,
    private val extractionService: ExtractionService,
    private val entityResolutionService: EntityResolutionService,
    private val cleansingService: DataCleansingService,
    private val deduplicationService: DeduplicationService,
    private val validationService: ValidationService
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    suspend fun importFromSource(
        sourceId: UUID,
        urls: List<String>,
        options: ImportOptions = ImportOptions.default()
    ): ImportBatchResult = DatabaseFactory.dbQuery {
        // Create batch record
        val batchId = importRepository.createImportBatch(
            sourceId = sourceId,
            totalUrls = urls.size,
            options = options
        )
        
        // Process URLs in parallel (with concurrency limit)
        val results = urls
            .asFlow()
            .flatMapMerge(concurrency = options.maxConcurrency) { url ->
                flow {
                    emit(processUrl(batchId, sourceId, url, options))
                }
            }
            .toList()
        
        // Update batch status
        val successCount = results.count { it is ImportResult.Success }
        val failureCount = results.count { it is ImportResult.Failure }
        
        importRepository.updateImportBatch(
            batchId = batchId,
            processedUrls = results.size,
            successfulImports = successCount,
            failedImports = failureCount,
            status = if (failureCount == 0) BatchStatus.COMPLETED else BatchStatus.PARTIAL
        )
        
        ImportBatchResult(
            batchId = batchId,
            totalUrls = urls.size,
            successful = successCount,
            failed = failureCount,
            results = results
        )
    }
    
    private suspend fun processUrl(
        batchId: UUID,
        sourceId: UUID,
        url: String,
        options: ImportOptions
    ): ImportResult = withContext(Dispatchers.IO) {
        try {
            // Stage 1: Scraping
            val html = scrapingService.scrape(url, retries = options.maxRetries)
            
            // Stage 2: Extraction
            val extracted = extractionService.extract(html, sourceId)
            
            // Stage 3: Entity Resolution
            val resolved = entityResolutionService.resolve(extracted)
            
            // Stage 4: Data Cleansing
            val cleaned = cleansingService.cleanse(resolved)
            
            // Stage 5: De-duplication
            val duplicates = deduplicationService.findDuplicates(cleaned)
            
            // Stage 6: Validation
            val validationResult = validationService.validate(cleaned, duplicates)
            
            // Stage 7: Staging
            val importedKrithiId = DatabaseFactory.dbQuery {
                importRepository.createImportedKrithi(
                    sourceId = sourceId,
                    sourceKey = url,
                    extracted = cleaned,
                    validationResult = validationResult,
                    duplicateCandidates = duplicates.map { it.id }
                )
            }
            
            ImportResult.Success(importedKrithiId)
        } catch (e: Exception) {
            // Log error and return failure
            importRepository.recordImportError(batchId, url, e)
            ImportResult.Failure(url, e)
        }
    }
}

data class ImportOptions(
    val maxConcurrency: Int = 5,
    val maxRetries: Int = 3,
    val retryDelayMs: Long = 1000,
    val autoApproveThreshold: Double = 0.95
) {
    companion object {
        fun default() = ImportOptions()
    }
}

sealed class ImportResult {
    data class Success(val importedKrithiId: UUID) : ImportResult()
    data class Failure(val url: String, val error: Exception) : ImportResult()
}
```

---

### 3.2 EntityResolutionService

**Purpose**: Map extracted names to canonical entities

**Implementation:**

```kotlin
package com.sangita.grantha.backend.api.imports.resolution

import com.sangita.grantha.backend.dal.composers.ComposerRepository
import com.sangita.grantha.backend.dal.ragas.RagaRepository
import com.sangita.grantha.backend.dal.deities.DeityRepository
import com.sangita.grantha.backend.dal.temples.TempleRepository
import com.sangita.grantha.shared.domain.model.*
import kotlinx.coroutines.*

class EntityResolutionService(
    private val composerRepository: ComposerRepository,
    private val ragaRepository: RagaRepository,
    private val deityRepository: DeityRepository,
    private val templeRepository: TempleRepository
) {
    suspend fun resolve(
        extracted: ExtractedMetadata
    ): ResolvedMetadata = coroutineScope {
        // Resolve entities in parallel
        val composerMatch = async { resolveComposer(extracted.composer) }
        val ragaMatch = async { resolveRaga(extracted.raga) }
        val deityMatch = async { 
            extracted.deity?.let { resolveDeity(it) } 
        }
        val templeMatch = async { 
            extracted.temple?.let { 
                resolveTemple(it, extracted.deity) 
            } 
        }
        
        ResolvedMetadata(
            composer = composerMatch.await(),
            raga = ragaMatch.await(),
            deity = deityMatch.await(),
            temple = templeMatch.await(),
            // ... other fields
        )
    }
    
    suspend fun resolveComposer(name: String): EntityMatch<ComposerDto> {
        // Normalize name
        val normalized = normalizeComposerName(name)
        
        // Try exact match first
        val exact = composerRepository.findByNameNormalized(normalized)
        if (exact != null) {
            return EntityMatch.Exact(exact, confidence = 1.0)
        }
        
        // Try fuzzy match
        val candidates = composerRepository.searchByName(normalized, limit = 5)
        if (candidates.isNotEmpty()) {
            val best = candidates.maxByOrNull { 
                similarityScore(normalized, it.nameNormalized) 
            }
            if (best != null) {
                val confidence = similarityScore(normalized, best.nameNormalized)
                return if (confidence >= 0.85) {
                    EntityMatch.Fuzzy(best, confidence)
                } else {
                    EntityMatch.NotFound(name)
                }
            }
        }
        
        return EntityMatch.NotFound(name)
    }
    
    suspend fun resolveRaga(name: String): EntityMatch<RagaDto> {
        val normalized = normalizeRagaName(name)
        
        // Try exact match
        val exact = ragaRepository.findByNameNormalized(normalized)
        if (exact != null) {
            return EntityMatch.Exact(exact, confidence = 1.0)
        }
        
        // Try fuzzy match
        val candidates = ragaRepository.searchByName(normalized, limit = 5)
        if (candidates.isNotEmpty()) {
            val best = candidates.maxByOrNull { 
                similarityScore(normalized, it.nameNormalized) 
            }
            if (best != null) {
                val confidence = similarityScore(normalized, best.nameNormalized)
                return if (confidence >= 0.80) {
                    EntityMatch.Fuzzy(best, confidence)
                } else {
                    EntityMatch.NotFound(name)
                }
            }
        }
        
        return EntityMatch.NotFound(name)
    }
    
    suspend fun resolveDeity(name: String): EntityMatch<DeityDto> {
        // Similar pattern to composer/raga resolution
        // ...
    }
    
    suspend fun resolveTemple(
        name: String,
        deityContext: String?
    ): EntityMatch<TempleDto> {
        // More complex: consider deity context and location
        val normalized = normalizeTempleName(name)
        
        // Try exact match
        val exact = templeRepository.findByNameNormalized(normalized)
        if (exact != null) {
            return EntityMatch.Exact(exact, confidence = 1.0)
        }
        
        // Try fuzzy match with deity context
        val candidates = if (deityContext != null) {
            templeRepository.searchByNameAndDeity(normalized, deityContext, limit = 10)
        } else {
            templeRepository.searchByName(normalized, limit = 10)
        }
        
        if (candidates.isNotEmpty()) {
            val best = candidates.maxByOrNull { temple ->
                val nameScore = similarityScore(normalized, temple.nameNormalized)
                val deityScore = if (deityContext != null && temple.primaryDeityId != null) {
                    // Check if deity matches
                    0.5 // Simplified - would check deity match
                } else {
                    0.0
                }
                nameScore * 0.7 + deityScore * 0.3
            }
            
            if (best != null) {
                val confidence = similarityScore(normalized, best.nameNormalized)
                return if (confidence >= 0.75) {
                    EntityMatch.Fuzzy(best, confidence)
                } else {
                    EntityMatch.NotFound(name)
                }
            }
        }
        
        return EntityMatch.NotFound(name)
    }
    
    private fun normalizeComposerName(name: String): String {
        return name
            .lowercase()
            .replace(Regex("\\b(saint|sri|swami|sir)\\b"), "")
            .trim()
            .replace(Regex("\\s+"), " ")
    }
    
    private fun normalizeRagaName(name: String): String {
        return name
            .lowercase()
            .replace(Regex("\\braga\\b"), "")
            .trim()
            .replace(Regex("\\s+"), " ")
    }
    
    private fun normalizeTempleName(name: String): String {
        return name
            .lowercase()
            .replace(Regex("\\b(temple|templenet|kshetra|kshetram)\\b"), "")
            .trim()
            .replace(Regex("\\s+"), " ")
    }
    
    private fun similarityScore(str1: String, str2: String): Double {
        // Use Levenshtein distance or Jaro-Winkler
        // Simplified implementation
        val maxLen = maxOf(str1.length, str2.length)
        if (maxLen == 0) return 1.0
        
        val distance = levenshteinDistance(str1, str2)
        return 1.0 - (distance.toDouble() / maxLen)
    }
    
    private fun levenshteinDistance(s1: String, s2: String): Int {
        // Standard Levenshtein implementation
        // ...
    }
}

sealed class EntityMatch<T> {
    data class Exact<T>(val entity: T, val confidence: Double) : EntityMatch<T>()
    data class Fuzzy<T>(val entity: T, val confidence: Double) : EntityMatch<T>()
    data class NotFound<T>(val originalName: String) : EntityMatch<T>()
}
```

---

### 3.3 DeduplicationService

**Purpose**: Detect duplicates within batch and against existing Krithis

**Implementation:**

```kotlin
package com.sangita.grantha.backend.api.imports.deduplication

import com.sangita.grantha.backend.dal.krithis.KrithiRepository
import com.sangita.grantha.shared.domain.model.*
import kotlinx.coroutines.*

class DeduplicationService(
    private val krithiRepository: KrithiRepository
) {
    suspend fun findDuplicates(
        resolved: ResolvedMetadata,
        batchContext: List<ResolvedMetadata> = emptyList()
    ): List<DuplicateMatch> = coroutineScope {
        val results = mutableListOf<DuplicateMatch>()
        
        // Check against existing Krithis
        val existingMatches = async {
            findExistingDuplicates(resolved)
        }
        
        // Check against batch context
        val batchMatches = async {
            findBatchDuplicates(resolved, batchContext)
        }
        
        results.addAll(existingMatches.await())
        results.addAll(batchMatches.await())
        
        results
    }
    
    private suspend fun findExistingDuplicates(
        resolved: ResolvedMetadata
    ): List<DuplicateMatch> {
        // Strong match: composer + title + incipit
        val strongMatches = if (resolved.composer is EntityMatch.Exact && 
                                resolved.title != null && 
                                resolved.incipit != null) {
            krithiRepository.search(
                composerId = (resolved.composer as EntityMatch.Exact).entity.id,
                titleQuery = resolved.title,
                limit = 10
            ).filter { krithi ->
                val incipitMatch = similarityScore(
                    resolved.incipit!!.lowercase(),
                    krithi.incipit?.lowercase() ?: ""
                )
                incipitMatch >= 0.90
            }.map { krithi ->
                DuplicateMatch.Strong(
                    krithiId = krithi.id,
                    confidence = 0.95,
                    reason = "Composer + Title + Incipit match"
                )
            }
        } else {
            emptyList()
        }
        
        // Medium match: composer + title
        val mediumMatches = if (strongMatches.isEmpty() && 
                                resolved.composer is EntityMatch.Exact && 
                                resolved.title != null) {
            krithiRepository.search(
                composerId = (resolved.composer as EntityMatch.Exact).entity.id,
                titleQuery = resolved.title,
                limit = 10
            ).map { krithi ->
                val titleScore = similarityScore(
                    resolved.title!!.lowercase(),
                    krithi.title.lowercase()
                )
                if (titleScore >= 0.85) {
                    DuplicateMatch.Medium(
                        krithiId = krithi.id,
                        confidence = titleScore * 0.8,
                        reason = "Composer + Title match"
                    )
                } else {
                    null
                }
            }.filterNotNull()
        } else {
            emptyList()
        }
        
        // Weak match: title + incipit (different composer possible)
        val weakMatches = if (strongMatches.isEmpty() && mediumMatches.isEmpty() &&
                             resolved.title != null && resolved.incipit != null) {
            krithiRepository.searchByTitle(resolved.title!!, limit = 20)
                .map { krithi ->
                    val titleScore = similarityScore(
                        resolved.title!!.lowercase(),
                        krithi.title.lowercase()
                    )
                    val incipitScore = similarityScore(
                        resolved.incipit!!.lowercase(),
                        krithi.incipit?.lowercase() ?: ""
                    )
                    val combinedScore = (titleScore + incipitScore) / 2
                    
                    if (combinedScore >= 0.80) {
                        DuplicateMatch.Weak(
                            krithiId = krithi.id,
                            confidence = combinedScore,
                            reason = "Title + Incipit match (composer may differ)"
                        )
                    } else {
                        null
                    }
                }.filterNotNull()
        } else {
            emptyList()
        }
        
        return (strongMatches + mediumMatches + weakMatches)
            .sortedByDescending { it.confidence }
            .take(5) // Top 5 matches
    }
    
    private suspend fun findBatchDuplicates(
        resolved: ResolvedMetadata,
        batchContext: List<ResolvedMetadata>
    ): List<DuplicateMatch> {
        // Similar logic but compare against batch context
        // ...
        return emptyList()
    }
    
    private fun similarityScore(str1: String, str2: String): Double {
        // Same implementation as EntityResolutionService
        // ...
    }
}

sealed class DuplicateMatch {
    abstract val krithiId: UUID
    abstract val confidence: Double
    abstract val reason: String
    
    data class Strong(
        override val krithiId: UUID,
        override val confidence: Double,
        override val reason: String
    ) : DuplicateMatch()
    
    data class Medium(
        override val krithiId: UUID,
        override val confidence: Double,
        override val reason: String
    ) : DuplicateMatch()
    
    data class Weak(
        override val krithiId: UUID,
        override val confidence: Double,
        override val reason: String
    ) : DuplicateMatch()
}
```

---

### 3.4 Source Handler Interface

**Purpose**: Abstract different source types

**Implementation:**

```kotlin
package com.sangita.grantha.backend.api.imports.scraping

import kotlinx.coroutines.*

interface SourceHandler {
    suspend fun discoverUrls(): List<String>
    suspend fun scrape(url: String): RawContent
    fun getSourceId(): UUID
    fun getSourceName(): String
}

data class RawContent(
    val url: String,
    val html: String,
    val metadata: Map<String, String> = emptyMap()
)

class KarnatikSourceHandler(
    private val baseUrl: String = "https://karnatik.com"
) : SourceHandler {
    override suspend fun discoverUrls(): List<String> {
        // Parse main lyrics page
        // Extract composition URLs
        // Handle pagination
        // ...
    }
    
    override suspend fun scrape(url: String): RawContent {
        // Fetch HTML
        // Clean content (remove nav, ads)
        // Extract metadata
        // ...
    }
    
    override fun getSourceId(): UUID {
        // Return known Karnatik source ID
    }
    
    override fun getSourceName(): String = "Karnatik.com"
}

class BlogspotSourceHandler(
    private val blogUrl: String,
    private val listPageUrl: String? = null
) : SourceHandler {
    override suspend fun discoverUrls(): List<String> {
        // Parse blog archive or list page
        // Extract post URLs
        // ...
    }
    
    override suspend fun scrape(url: String): RawContent {
        // Fetch blog post
        // Extract main content area
        // Remove blogspot navigation
        // ...
    }
    
    // ...
}
```

---

## 4. Database Schema Enhancements

### 4.1 Import Batch Table

```sql
-- Add to migration file
CREATE TABLE import_batches (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  import_source_id UUID NOT NULL REFERENCES import_sources(id),
  status TEXT NOT NULL DEFAULT 'pending', -- pending, processing, completed, failed, partial
  total_urls INT NOT NULL,
  processed_urls INT NOT NULL DEFAULT 0,
  successful_imports INT NOT NULL DEFAULT 0,
  failed_imports INT NOT NULL DEFAULT 0,
  options JSONB, -- ImportOptions as JSON
  error_summary JSONB, -- Summary of errors
  started_at TIMESTAMPTZ,
  completed_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('UTC', now())
);

CREATE INDEX idx_import_batches_source_status 
  ON import_batches(import_source_id, status);
```

### 4.2 Enhanced Imported Krithis

```sql
-- Add columns to imported_krithis table
ALTER TABLE imported_krithis
  ADD COLUMN IF NOT EXISTS import_batch_id UUID REFERENCES import_batches(id),
  ADD COLUMN IF NOT EXISTS extraction_confidence DECIMAL(3,2),
  ADD COLUMN IF NOT EXISTS entity_mapping_confidence DECIMAL(3,2),
  ADD COLUMN IF NOT EXISTS duplicate_candidates JSONB,
  ADD COLUMN IF NOT EXISTS quality_score DECIMAL(3,2),
  ADD COLUMN IF NOT EXISTS processing_errors JSONB;

CREATE INDEX idx_imported_krithis_batch 
  ON imported_krithis(import_batch_id);
```

---

## 5. API Endpoints

### 5.1 Import Routes

```kotlin
package com.sangita.grantha.backend.api.imports

import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.UUID

fun Route.importRoutes(
    importService: ImportService,
    pipelineService: ImportPipelineService
) {
    route("/v1/admin/imports") {
        // Start import from source
        post("/sources/{sourceId}/scrape") {
            val sourceId = call.parameters["sourceId"]?.let { UUID.fromString(it) }
                ?: return@post call.respond(HttpStatusCode.BadRequest)
            
            val request = call.receive<ScrapeImportRequest>()
            
            val result = pipelineService.importFromSource(
                sourceId = sourceId,
                urls = request.urls,
                options = request.options ?: ImportOptions.default()
            )
            
            call.respond(result)
        }
        
        // Get batch status
        get("/batches/{batchId}/status") {
            val batchId = call.parameters["batchId"]?.let { UUID.fromString(it) }
                ?: return@get call.respond(HttpStatusCode.BadRequest)
            
            val status = importService.getBatchStatus(batchId)
            call.respond(status)
        }
        
        // List imported Krithis (existing, enhanced)
        get("/krithis") {
            val status = call.request.queryParameters["status"]
            val sourceId = call.request.queryParameters["sourceId"]?.let { UUID.fromString(it) }
            val batchId = call.request.queryParameters["batchId"]?.let { UUID.fromString(it) }
            
            val imports = importService.listImportedKrithis(
                status = status,
                sourceId = sourceId,
                batchId = batchId
            )
            
            call.respond(imports)
        }
    }
}

data class ScrapeImportRequest(
    val urls: List<String>,
    val options: ImportOptions? = null
)
```

---

## 6. Implementation Checklist

### Phase 1: Foundation (Weeks 1-4)

- [ ] Create `ImportPipelineService` with coroutine-based workflow
- [ ] Implement `EntityResolutionService` with fuzzy matching
- [ ] Build `SourceHandler` interface and Karnatik implementation
- [ ] Enhance `WebScrapingService` for multi-source support
- [ ] Create `DeduplicationService` with duplicate detection
- [ ] Add import batch tracking to database
- [ ] Create import batch API endpoints
- [ ] Enhance import review UI with confidence scores
- [ ] Add basic error handling and retry logic
- [ ] Implement logging and error tracking

### Phase 2: Data Quality (Weeks 5-8)

- [ ] Implement `DataCleansingService` for normalization
- [ ] Enhance `ValidationService` with quality checks
- [ ] Add quality scoring system
- [ ] Implement batch operations in review UI
- [ ] Add cross-source duplicate detection
- [ ] Create data quality dashboard
- [ ] Implement auto-approval for high-confidence imports
- [ ] Add manual correction workflow

### Phase 3: TempleNet Integration & Trinity Sources (Weeks 9-10)

- [ ] Create `TempleNetSourceHandler` for temple data
- [ ] Implement temple matching in `EntityResolutionService`
- [ ] Pre-fetch and cache major temples
- [ ] Add temple entity management enhancements
- [ ] Integrate temple matching into import pipeline
- [ ] Create temple disambiguation UI
- [ ] Add Dikshitar Kritis List handler (Trinity composer coverage)

### Phase 4: Advanced Features (Weeks 11-12)

- [ ] Add remaining Blogspot source handlers (Guru Guha main blog, Syama Krishna, Thyagaraja)
- [ ] Implement Koog POC for extraction stage
- [ ] Performance optimization
- [ ] Enhanced observability (OpenTelemetry)
- [ ] Monitoring and alerting
- [ ] Documentation and runbooks

---

## 7. Testing Strategy

### 7.1 Unit Tests

```kotlin
class EntityResolutionServiceTest {
    @Test
    fun `resolveComposer with exact match`() = runTest {
        val service = EntityResolutionService(...)
        val match = service.resolveComposer("Tyagaraja")
        assertTrue(match is EntityMatch.Exact)
        assertEquals(1.0, match.confidence)
    }
    
    @Test
    fun `resolveComposer with fuzzy match`() = runTest {
        val service = EntityResolutionService(...)
        val match = service.resolveComposer("Tyagayya")
        assertTrue(match is EntityMatch.Fuzzy)
        assertTrue(match.confidence >= 0.85)
    }
}
```

### 7.2 Integration Tests

```kotlin
class ImportPipelineIntegrationTest {
    @Test
    fun `full import pipeline with test data`() = runTest {
        val pipeline = ImportPipelineService(...)
        val result = pipeline.importFromSource(
            sourceId = testSourceId,
            urls = listOf("http://test.com/krithi1")
        )
        
        assertEquals(1, result.successful)
        assertEquals(0, result.failed)
    }
}
```

---

## 8. Performance Considerations

### 8.1 Concurrency

- **Default**: 5 concurrent URL processing
- **Configurable**: Via `ImportOptions.maxConcurrency`
- **Database**: Use connection pooling
- **Rate Limiting**: Respect source rate limits

### 8.2 Caching

- **Entity Resolution**: Cache composer/raga/deity lookups
- **HTML Content**: Cache scraped content for retries
- **Temple Data**: Pre-fetch and cache TempleNet data

### 8.3 Batch Processing

- **Batch Size**: Process 100 URLs per batch
- **Progress Tracking**: Update batch status incrementally
- **Error Isolation**: Individual URL failures don't stop batch

---

## 9. Error Handling

### 9.1 Retry Strategy

```kotlin
suspend fun <T> withRetry(
    maxRetries: Int = 3,
    delayMs: Long = 1000,
    block: suspend () -> T
): T {
    var lastException: Exception? = null
    repeat(maxRetries) { attempt ->
        try {
            return block()
        } catch (e: Exception) {
            lastException = e
            if (attempt < maxRetries - 1) {
                delay(delayMs * (1 shl attempt)) // Exponential backoff
            }
        }
    }
    throw lastException ?: Exception("Retry failed")
}
```

### 9.2 Error Logging

- Log all errors with context
- Store errors in `import_batches.error_summary`
- Track error rates by source
- Alert on high error rates

---

## 10. Monitoring & Observability

### 10.1 Metrics

- Import batch success/failure rates
- Processing time per URL
- Entity resolution accuracy
- Duplicate detection rates
- Human review queue size

### 10.2 Logging

- Structured logging with correlation IDs
- Log each pipeline stage
- Track performance metrics
- Error details with stack traces

---

## 11. Conclusion

This implementation guide provides a concrete foundation for building the import pipeline. Key principles:

1. **Start Simple**: Custom coroutine-based workflow
2. **Modular Design**: Separate services for each concern
3. **Error Resilience**: Comprehensive error handling
4. **Observability**: Logging and metrics from day one
5. **Incremental Enhancement**: Add features as needed

**Next Steps:**
1. Review and refine this guide with team
2. Start with Phase 1 implementation
3. Iterate based on learnings
4. Evaluate Koog after Phase 1 is stable

---

## 12. References

- [Krithi Bulk Import Capability Analysis](../archive/krithi-bulk-import-capability-analysis-goose.md)
- [Web Source Detailed Analysis](../03-sources/web-source-analysis.md)
- [Koog Evaluation](../archive/koog-evaluation-for-import-pipeline-goose.md)
- [Database Schema](../../../../04-database/schema.md)
- [API Contract](../../../../03-api/api-contract.md)