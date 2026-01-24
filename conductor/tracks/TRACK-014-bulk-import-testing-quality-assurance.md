# TRACK-014: Bulk Import Testing & Quality Assurance

| Metadata | Value |
|:---|:---|
| **Status** | Proposed |
| **Owner** | Backend Team |
| **Priority** | MEDIUM |
| **Created** | 2026-01-23 |
| **Related Tracks** | TRACK-010, TRACK-011, TRACK-012, TRACK-013 |
| **Implementation Plan** | [bulk-import-fixes-implementation-plan.md](../../application_documentation/07-quality/bulk-import-fixes-implementation-plan.md) |

## 1. Goal

Establish comprehensive test coverage for bulk import functionality to ensure correctness, reliability, and performance at scale.

## 2. Problem Statement

Current implementation lacks automated tests:
- ❌ No unit tests for normalization service
- ❌ No unit tests for entity resolution logic
- ❌ No unit tests for deduplication heuristics
- ❌ No integration tests for full pipeline
- ❌ No performance tests for large batches
- ❌ No error recovery scenario tests

**Impact:**
- Low confidence in production stability
- Regression risk for future changes
- Difficult to validate fixes and optimizations

## 3. Implementation Plan

### 3.1 Unit Tests: Normalization Service

**File:** `modules/backend/api/src/test/kotlin/com/sangita/grantha/backend/api/services/NameNormalizationServiceTest.kt`

**Test Cases:**
```kotlin
class NameNormalizationServiceTest {
    
    private val service = NameNormalizationService()
    
    @Test
    fun `normalize composer - handles aliases`() {
        assertEquals("tyagaraja", service.normalize("Thyagaraja"))
        assertEquals("muthuswami dikshitar", service.normalize("Muthuswami Dikshitar"))
    }
    
    @Test
    fun `normalize composer - removes honorifics`() {
        assertEquals("tyagaraja", service.normalize("Sri Tyagaraja"))
        assertEquals("dikshitar", service.normalize("Dr. Muthuswami Dikshitar"))
    }
    
    @Test
    fun `normalize raga - handles spaces and diacritics`() {
        assertEquals("kalyani", service.normalize("Kalyani"))
        assertEquals("kalyani", service.normalize("Kalyaani"))
        assertEquals("kalyani", service.normalize("Kalyāni"))
    }
    
    @Test
    fun `normalize tala - handles suffixes`() {
        assertEquals("rupaka", service.normalize("Rupakam"))
        assertEquals("rupaka", service.normalize("Rupaka"))
    }
    
    @Test
    fun `normalize tala - handles transliteration`() {
        assertEquals("chapu", service.normalize("cApu"))
        assertEquals("chapu", service.normalize("Chapu"))
    }
    
    @Test
    fun `normalize - handles empty and null`() {
        assertEquals("", service.normalize(""))
        assertEquals("", service.normalize("   "))
    }
    
    @Test
    fun `normalize - preserves case-insensitive matching`() {
        val normalized1 = service.normalize("Tyagaraja")
        val normalized2 = service.normalize("TYAGARAJA")
        assertEquals(normalized1, normalized2)
    }
}
```

---

### 3.2 Unit Tests: Entity Resolution Service

**File:** `modules/backend/api/src/test/kotlin/com/sangita/grantha/backend/api/services/EntityResolutionServiceTest.kt`

**Test Cases:**
```kotlin
class EntityResolutionServiceTest {
    
    private val mockDal = mockk<SangitaDal>()
    private val service = EntityResolutionService(mockDal)
    
    @Test
    fun `resolve - exact match returns high confidence`() {
        val composers = listOf(
            ComposerDto(id = uuid1, name = "Tyagaraja", normalizedName = "tyagaraja")
        )
        coEvery { mockDal.composers.listAll() } returns composers
        
        val imported = ImportedKrithiDto(
            id = uuid2,
            composerName = "Tyagaraja",
            // ... other fields
        )
        
        val result = service.resolve(imported)
        
        assertEquals(1, result.composerCandidates.size)
        assertTrue(result.composerCandidates.first().confidence >= 0.95)
    }
    
    @Test
    fun `resolve - fuzzy match returns medium confidence`() {
        val composers = listOf(
            ComposerDto(id = uuid1, name = "Tyagaraja", normalizedName = "tyagaraja")
        )
        coEvery { mockDal.composers.listAll() } returns composers
        
        val imported = ImportedKrithiDto(
            id = uuid2,
            composerName = "Thyagaraja",  // Typo
            // ... other fields
        )
        
        val result = service.resolve(imported)
        
        assertTrue(result.composerCandidates.isNotEmpty())
        assertTrue(result.composerCandidates.first().confidence >= 0.70)
        assertTrue(result.composerCandidates.first().confidence < 0.95)
    }
    
    @Test
    fun `resolve - no match returns empty candidates`() {
        val composers = listOf(
            ComposerDto(id = uuid1, name = "Tyagaraja", normalizedName = "tyagaraja")
        )
        coEvery { mockDal.composers.listAll() } returns composers
        
        val imported = ImportedKrithiDto(
            id = uuid2,
            composerName = "Unknown Composer",
            // ... other fields
        )
        
        val result = service.resolve(imported)
        
        assertTrue(result.composerCandidates.isEmpty() || 
                   result.composerCandidates.first().confidence < 0.50)
    }
    
    @Test
    fun `resolve - uses cache after first call`() {
        val composers = listOf(
            ComposerDto(id = uuid1, name = "Tyagaraja", normalizedName = "tyagaraja")
        )
        coEvery { mockDal.composers.listAll() } returns composers
        
        val imported = ImportedKrithiDto(
            id = uuid2,
            composerName = "Tyagaraja",
            // ... other fields
        )
        
        // First call
        service.resolve(imported)
        
        // Second call should use cache (verify only one DB call)
        service.resolve(imported)
        
        coVerify(exactly = 1) { mockDal.composers.listAll() }
    }
}
```

---

### 3.3 Unit Tests: Deduplication Service

**File:** `modules/backend/api/src/test/kotlin/com/sangita/grantha/backend/api/services/DeduplicationServiceTest.kt`

**Test Cases:**
```kotlin
class DeduplicationServiceTest {
    
    private val mockDal = mockk<SangitaDal>()
    private val service = DeduplicationService(mockDal, NameNormalizationService())
    
    @Test
    fun `findDuplicates - exact match in canonical krithis`() {
        val canonical = KrithiDto(
            id = uuid1,
            title = "Nagumomu",
            composerId = composerId1
        )
        coEvery { mockDal.krithis.findByNormalizedTitle(any()) } returns listOf(canonical)
        
        val imported = ImportedKrithiDto(
            id = uuid2,
            title = "Nagumomu",
            composerId = composerId1,
            // ... other fields
        )
        
        val duplicates = service.findDuplicates(imported)
        
        assertEquals(1, duplicates.size)
        assertEquals(uuid1, duplicates.first().id)
        assertEquals(1.0, duplicates.first().confidence, 0.01)
    }
    
    @Test
    fun `findDuplicates - fuzzy match in staging`() {
        val staging = ImportedKrithiDto(
            id = uuid1,
            title = "Nagumomu",
            status = ImportStatus.PENDING
        )
        coEvery { mockDal.imports.findByNormalizedTitle(any(), any(), any()) } returns listOf(staging)
        
        val imported = ImportedKrithiDto(
            id = uuid2,
            title = "Nagumomu Ganule",  // Similar but not exact
            // ... other fields
        )
        
        val duplicates = service.findDuplicates(imported)
        
        assertTrue(duplicates.isNotEmpty())
        assertTrue(duplicates.first().confidence >= 0.70)
    }
    
    @Test
    fun `findDuplicates - intra-batch deduplication`() {
        val batchContext = listOf(
            ImportedKrithiDto(
                id = uuid1,
                title = "Nagumomu",
                batchId = batchId1
            )
        )
        
        val imported = ImportedKrithiDto(
            id = uuid2,
            title = "Nagumomu Ganule",
            batchId = batchId1
        )
        
        val duplicates = service.findDuplicates(imported, batchContext)
        
        assertTrue(duplicates.any { it.source == "batch" })
    }
    
    @Test
    fun `findDuplicates - no duplicates returns empty`() {
        coEvery { mockDal.krithis.findByNormalizedTitle(any()) } returns emptyList()
        coEvery { mockDal.imports.findByNormalizedTitle(any(), any(), any()) } returns emptyList()
        
        val imported = ImportedKrithiDto(
            id = uuid1,
            title = "Unique Title",
            // ... other fields
        )
        
        val duplicates = service.findDuplicates(imported)
        
        assertTrue(duplicates.isEmpty())
    }
}
```

---

### 3.4 Integration Tests: Full Pipeline

**File:** `modules/backend/api/src/test/kotlin/com/sangita/grantha/backend/api/integration/BulkImportPipelineTest.kt`

**Test Cases:**
```kotlin
class BulkImportPipelineTest : KtorTestBase() {
    
    @Test
    fun `full pipeline - manifest ingest to review`() = testApplication {
        // 1. Upload CSV
        val csvContent = """
            krithi,hyperlink,raga
            Test Krithi,https://example.com/krithi1,Kalyani
        """.trimIndent()
        
        val uploadResponse = client.post("/v1/admin/bulk-import/upload") {
            // ... multipart form data
        }
        assertEquals(HttpStatusCode.Accepted, uploadResponse.status)
        val batchId = uploadResponse.body<ImportBatchDto>().id
        
        // 2. Wait for manifest ingest
        waitForBatchStage(batchId, "MANIFEST_INGEST", TaskStatus.SUCCEEDED, timeout = 30.seconds)
        
        // 3. Wait for scraping
        waitForBatchStage(batchId, "SCRAPE", TaskStatus.SUCCEEDED, timeout = 5.minutes)
        
        // 4. Wait for entity resolution
        waitForBatchStage(batchId, "RESOLUTION", TaskStatus.SUCCEEDED, timeout = 2.minutes)
        
        // 5. Verify imported krithi exists
        val imports = client.get("/v1/admin/imports?status=PENDING").body<List<ImportedKrithiDto>>()
        assertTrue(imports.isNotEmpty())
        
        // 6. Review import
        val importId = imports.first().id
        val reviewResponse = client.post("/v1/admin/imports/$importId/review") {
            contentType(ContentType.Application.Json)
            setBody(ImportReviewRequest(action = ImportReviewAction.APPROVE))
        }
        assertEquals(HttpStatusCode.OK, reviewResponse.status)
        
        // 7. Verify krithi created
        val krithi = client.get("/v1/admin/krithis/${imports.first().id}").body<KrithiDto>()
        assertNotNull(krithi)
    }
    
    @Test
    fun `pipeline - handles manifest ingest failure`() = testApplication {
        // Upload invalid CSV
        val csvContent = "invalid,header"
        
        val uploadResponse = client.post("/v1/admin/bulk-import/upload") {
            // ... multipart form data
        }
        val batchId = uploadResponse.body<ImportBatchDto>().id
        
        // Wait for failure
        waitForBatchStatus(batchId, BatchStatus.FAILED, timeout = 30.seconds)
        
        // Verify batch is marked FAILED
        val batch = client.get("/v1/admin/bulk-import/batches/$batchId").body<ImportBatchDto>()
        assertEquals(BatchStatus.FAILED, batch.status)
    }
    
    @Test
    fun `pipeline - handles scrape failure and retry`() = testApplication {
        // Upload CSV with invalid URL
        val csvContent = """
            krithi,hyperlink
            Test,invalid-url
        """.trimIndent()
        
        // ... upload and wait for scrape failure
        
        // Retry batch
        val retryResponse = client.post("/v1/admin/bulk-import/batches/$batchId/retry") {
            contentType(ContentType.Application.Json)
            setBody(BulkImportRetryRequest(includeFailed = true))
        }
        assertEquals(HttpStatusCode.OK, retryResponse.status)
        
        // Verify tasks are requeued
        val tasks = client.get("/v1/admin/bulk-import/batches/$batchId/tasks?status=PENDING")
            .body<List<ImportTaskRunDto>>()
        assertTrue(tasks.isNotEmpty())
    }
}
```

---

### 3.5 Performance Tests: Large Batches

**File:** `modules/backend/api/src/test/kotlin/com/sangita/grantha/backend/api/performance/BulkImportPerformanceTest.kt`

**Test Cases:**
```kotlin
class BulkImportPerformanceTest : KtorTestBase() {
    
    @Test
    fun `performance - 100 entry batch completes within timeout`() = testApplication {
        val csvContent = generateCsv(100)  // Generate 100 rows
        
        val startTime = System.currentTimeMillis()
        
        // Upload and process
        val batchId = uploadAndProcessBatch(csvContent)
        
        val endTime = System.currentTimeMillis()
        val duration = endTime - startTime
        
        // Verify completion
        waitForBatchStatus(batchId, BatchStatus.COMPLETED, timeout = 10.minutes)
        
        // Performance assertion: Should complete in reasonable time
        assertTrue(duration < 10.minutes.toMillis(), 
            "Batch took ${duration}ms, expected < ${10.minutes.toMillis()}ms")
    }
    
    @Test
    fun `performance - stage completion checks are O(1)`() = testApplication {
        val csvContent = generateCsv(100)
        val batchId = uploadAndProcessBatch(csvContent)
        
        // Monitor database queries during processing
        val queryCount = countDatabaseQueries("SELECT * FROM import_task_run")
        
        // Verify query count is reasonable (not O(N) per task)
        assertTrue(queryCount < 200, 
            "Too many queries: $queryCount (expected < 200 for 100 tasks)")
    }
    
    @Test
    fun `performance - entity resolution uses cache`() = testApplication {
        val csvContent = generateCsv(50)
        val batchId = uploadAndProcessBatch(csvContent)
        
        // Monitor cache hit rate
        val cacheHits = getCacheHitCount()
        val totalResolutions = 50 * 3  // 50 krithis × 3 entity types
        
        val hitRate = cacheHits.toDouble() / totalResolutions
        assertTrue(hitRate > 0.5, 
            "Cache hit rate too low: $hitRate (expected > 0.5)")
    }
}
```

---

### 3.6 Error Recovery Tests

**File:** `modules/backend/api/src/test/kotlin/com/sangita/grantha/backend/api/integration/BulkImportErrorRecoveryTest.kt`

**Test Cases:**
```kotlin
class BulkImportErrorRecoveryTest : KtorTestBase() {
    
    @Test
    fun `error recovery - retry failed tasks`() = testApplication {
        // Create batch with some failing tasks
        val batchId = createBatchWithFailures()
        
        // Retry failed tasks
        val retryResponse = client.post("/v1/admin/bulk-import/batches/$batchId/retry") {
            contentType(ContentType.Application.Json)
            setBody(BulkImportRetryRequest(includeFailed = true))
        }
        
        // Verify tasks are requeued
        val tasks = client.get("/v1/admin/bulk-import/batches/$batchId/tasks?status=PENDING")
            .body<List<ImportTaskRunDto>>()
        assertTrue(tasks.isNotEmpty())
    }
    
    @Test
    fun `error recovery - watchdog marks stuck tasks as retryable`() = testApplication {
        // Create batch and simulate stuck task
        val batchId = createBatchWithStuckTask()
        
        // Wait for watchdog to run
        delay(11.minutes)  // Watchdog timeout is 10 minutes
        
        // Verify stuck task is marked RETRYABLE
        val tasks = client.get("/v1/admin/bulk-import/batches/$batchId/tasks?status=RETRYABLE")
            .body<List<ImportTaskRunDto>>()
        assertTrue(tasks.isNotEmpty())
    }
    
    @Test
    fun `error recovery - batch can be cancelled`() = testApplication {
        val batchId = createLargeBatch()
        
        // Cancel batch
        val cancelResponse = client.post("/v1/admin/bulk-import/batches/$batchId/cancel")
        assertEquals(HttpStatusCode.OK, cancelResponse.status)
        
        // Verify batch is cancelled
        val batch = client.get("/v1/admin/bulk-import/batches/$batchId").body<ImportBatchDto>()
        assertEquals(BatchStatus.CANCELLED, batch.status)
    }
}
```

---

## 4. Progress Log

### 2026-01-23: Track Created
- ✅ Analyzed testing gaps
- ✅ Designed test suite structure
- ✅ Created implementation plan

### Pending
- [ ] Unit tests for normalization service
- [ ] Unit tests for entity resolution service
- [ ] Unit tests for deduplication service
- [ ] Integration tests for full pipeline
- [ ] Performance tests for large batches
- [ ] Error recovery scenario tests
- [ ] Test coverage >80% for critical services

---

## 5. Success Criteria

- ✅ Unit test coverage >80% for normalization, resolution, deduplication
- ✅ Integration test for full pipeline (manifest → scrape → resolution → review)
- ✅ Performance test for 100+ entry batches
- ✅ Error recovery tests pass
- ✅ All tests run in CI/CD
- ✅ Test execution time < 10 minutes

---

## 6. Dependencies

- **TRACK-010:** Critical fixes must be complete before testing
- **TRACK-011:** Quality scoring tests depend on quality scoring implementation
- **TRACK-012:** Review workflow tests depend on review workflow completion
- **TRACK-013:** Performance tests depend on performance optimizations

---

## 7. References

- [Implementation Plan](../../application_documentation/07-quality/bulk-import-fixes-implementation-plan.md)
- [Claude Review](../../application_documentation/07-quality/bulk-import-implementation-review-claude.md)
- [Goose Review](../../application_documentation/07-quality/csv-import-strategy-implementation-review-goose.md)
- [Codex Review](../../application_documentation/07-quality/csv-import-strategy-review-codex.md)
