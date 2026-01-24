| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-01-24 |
| **Author** | Sangeetha Grantha Team |

# CSV Import Strategy Implementation - Critical Review

| Metadata | Value |
|:---|:---|
| **Status** | Active Review |
| **Version** | 1.1.0 |
| **Last Updated** | 2026-01-23 |
| **Author** | Goose AI Critical Review |
| **Related Documents** | 
| - [CSV Import Strategy](../01-requirements/features/bulk-import/01-strategy/csv-import-strategy.md) |
| - [Technical Implementation Guide](../01-requirements/features/bulk-import/02-implementation/technical-implementation-guide.md) |
| - [Conductor Tracks](../../conductor/tracks.md) |
| **Review Update** | Updated per Clarified Requirements (2026-01) from Technical Implementation Guide |

---

## Executive Summary

This document provides a comprehensive critical review of the CSV import strategy implementation for bulk importing Krithis. The implementation spans multiple tracks (TRACK-001 through TRACK-009) and represents a significant architectural evolution from initial MVP to a production-grade system.

### Overall Assessment

**Strengths:**
- ✅ Well-architected unified dispatcher pattern (TRACK-007) reduces database load
- ✅ Comprehensive orchestration with batch/job/task hierarchy
- ✅ Robust entity resolution with normalization and caching
- ✅ Production-ready error handling and retry mechanisms
- ✅ Good separation of concerns across service layers

**Critical Issues:**
- ⚠️ **Missing Quality Scoring System**: The strategy document specifies quality tiers (EXCELLENT, GOOD, FAIR, POOR) but implementation lacks this scoring mechanism
- ⚠️ **Incomplete Auto-Approval Logic**: Auto-approval exists but doesn't fully align with strategy document's confidence thresholds
- ⚠️ **Limited Deduplication Coverage**: Deduplication only checks canonical krithis and staging, missing intra-batch deduplication during processing
- ⚠️ **Manifest Ingest Failure Handling**: Clarified requirements specify batch must be marked FAILED even if zero tasks created, but current implementation may not handle this edge case

**Areas for Improvement:**
- 🔄 Performance optimization opportunities in entity resolution cache invalidation
- 🔄 Missing comprehensive integration tests
- 🔄 Limited observability/metrics beyond basic logging
- 🔄 Frontend lacks batch-level filtering in review queue

---

## 1. Architecture Review

### 1.1 Unified Dispatcher Pattern (TRACK-007)

**Assessment: ✅ Excellent**

The evolution from multiple polling loops to a unified dispatcher with channels is a significant architectural improvement.

**Strengths:**
- **Single Polling Point**: Reduces database load from 6+ concurrent queries to 1
- **Event-Driven Wakeup**: `wakeUpChannel` with CONFLATED channel prevents unnecessary polling delays
- **Adaptive Backoff**: Exponential backoff (750ms → 15s max) efficiently handles idle periods
- **Channel-Based Decoupling**: Workers consume from channels, enabling better flow control

**Code Quality:**
```kotlin
// BulkImportWorkerService.kt:175-234
private suspend fun runDispatcherLoop(...) {
    var currentDelay = config.pollIntervalMs
    while (scope?.isActive == true) {
        // Polls for tasks across all job types
        // Sends to appropriate channels
        // Adaptive backoff with signal interrupt
    }
}
```

**Recommendations:**
1. Consider adding metrics for dispatcher polling frequency and channel utilization
2. Add circuit breaker pattern if dispatcher consistently finds no tasks (indicates system issue)

### 1.2 Batch/Job/Task Hierarchy

**Assessment: ✅ Good**

The three-level hierarchy (Batch → Job → Task) provides excellent granularity for tracking and management.

**Strengths:**
- Clear separation: Batch (CSV file), Job (work type), Task (individual URL)
- Comprehensive status tracking at each level
- Event logging for audit trail

**Concerns:**
- **Job Type Enum**: The `job_type_enum` includes `'enrich'` and `'review_prep'` which are not implemented
- **Task Status Complexity**: 7 status values (pending, running, succeeded, failed, retryable, blocked, cancelled) may be over-engineered for current needs

**Recommendations:**
1. Remove unused job types or document them as future work
2. Consider consolidating `retryable` and `failed` into a single status with retry metadata

### 1.3 Service Layer Organization

**Assessment: ✅ Good**

Services are well-separated with clear responsibilities:

```
BulkImportOrchestrationService → Batch lifecycle management
BulkImportWorkerService → Background processing
EntityResolutionService → Entity matching
DeduplicationService → Duplicate detection
AutoApprovalService → Review automation
NameNormalizationService → Text normalization
```

**Strengths:**
- Single Responsibility Principle well-applied
- Dependency injection pattern used correctly
- Services are testable in isolation

**Minor Issues:**
- `NameNormalizationService` is instantiated directly in `EntityResolutionService` rather than injected (minor coupling issue)

---

## 2. Design Decisions Analysis

### 2.0 Clarified Requirements (2026-01) Compliance Summary

The Technical Implementation Guide includes clarified requirements from 2026-01 that supersede some original strategy document specifications:

1. **✅ CSV Raga Column Optional**: Implementation correctly treats Raga as optional; scraped raga values are authoritative
2. **✅ URL Validation Syntax-Only**: Implementation correctly performs syntax-only validation (no HEAD/GET required)
3. **⚠️ Manifest Ingest Failure**: Must mark batch as FAILED even if zero tasks created - **NEEDS FIX** (see Section 2.1.1)
4. **✅ Scraping Failure Handling**: CSV metadata not used in scraping stage, so requirement to discard CSV-seeded metadata is N/A

### 2.1 CSV Parsing Implementation

**Assessment: ✅ Good (Updated per Clarified Requirements)**

**Implementation:**
- Uses Apache Commons CSV for robust parsing (handles quotes, newlines)
- Header validation with case-insensitive matching
- URL syntax validation before task creation
- Deduplication by hyperlink (case-insensitive)

**Code Review:**
```kotlin
// BulkImportWorkerService.kt:739-778
private fun parseCsvManifest(path: Path): List<CsvRow> {
    // Uses CSVFormat.DEFAULT with header mapping
    // Validates required columns: "krithi", "hyperlink"
    // Basic URL validation via isValidUrl()
}
```

### 2.1.1 Manifest Ingest Failure Handling

**Assessment: ⚠️ Needs Fix**

**Clarified Requirement (2026-01):**
> "Manifest ingest failures must mark the batch as FAILED, even if zero tasks were created."

**Current Implementation:**
```kotlin
// BulkImportWorkerService.kt:371-382
private suspend fun failManifestTask(...) {
    dal.bulkImport.updateTaskStatus(id = task.id, status = TaskStatus.FAILED, ...)
    dal.bulkImport.updateJobStatus(id = job.id, status = TaskStatus.FAILED, ...)
    dal.bulkImport.createEvent(refType = "batch", refId = job.batchId, eventType = "MANIFEST_INGEST_FAILED", ...)
    // ❌ Missing: Batch status update to FAILED
}
```

**Issue:**
- When manifest ingest fails, the job and task are marked FAILED, but the batch status is not updated
- `maybeCompleteBatch()` only runs when `totalTasks > 0`, so if manifest ingest fails before creating any tasks, the batch remains in RUNNING or PENDING status
- This violates the clarified requirement

**Recommendation:**
```kotlin
private suspend fun failManifestTask(...) {
    // ... existing code ...
    dal.bulkImport.updateBatchStatus(id = job.batchId, status = BatchStatus.FAILED, completedAt = now)
}
```

**Clarified Requirements (2026-01) Compliance:**
- ✅ **URL Validation**: Per clarified requirements, syntax-only validation is correct (no HEAD/GET required)
- ✅ **Raga Column Optional**: Per clarified requirements, CSV Raga is intentionally optional; scraped raga values are authoritative
- ⚠️ **Manifest Ingest Failure**: Clarified requirements specify batch must be marked FAILED even if zero tasks created, but implementation may not handle this edge case (see Section 2.1.1)

**Gaps vs Strategy:**
- ⚠️ **Original Strategy vs Clarified Requirements**: Original strategy document (Section 4.1.2) specified HEAD request validation, but this was clarified to be syntax-only. Implementation correctly follows clarified requirements.

**Recommendations:**
1. ✅ URL accessibility validation not needed per clarified requirements
2. ✅ Raga column optionality is correct per clarified requirements
3. Add validation for duplicate URLs within the same CSV (currently only deduplicates by hyperlink)
4. Fix manifest ingest failure handling to mark batch as FAILED even when zero tasks created (see Section 2.1.1)

### 2.2 Entity Resolution & Normalization

**Assessment: ✅ Very Good**

**Implementation Highlights:**
- **Caching Strategy**: 15-minute TTL with in-memory maps for O(1) lookups
- **Normalization Rules**: Domain-specific rules for Composers, Ragas, Talas
- **Fuzzy Matching**: Levenshtein distance with confidence scoring (HIGH ≥90%, MEDIUM ≥70%, LOW ≥50%)

**Code Quality:**
```kotlin
// EntityResolutionService.kt:47-64
private suspend fun ensureCache() {
    // Double-check locking pattern
    // Pre-normalizes all reference entities
    // Creates O(1) lookup maps
}
```

**Strengths:**
- Normalization rules align well with strategy document (Section 4.3.1-4.3.2)
- Cache invalidation strategy is reasonable (15 min TTL)
- Confidence scoring provides actionable metadata

**Concerns:**
1. **Cache Invalidation**: No mechanism to invalidate cache when new entities are created during import
2. **Normalization Edge Cases**: Some normalization rules may be too aggressive (e.g., removing all spaces from raga names)

**Example Issue:**
```kotlin
// NameNormalizationService.kt:50
normalized = normalized.replace(" ", "") // "Kedara Gaula" -> "kedaragaula"
```
This may cause false matches if canonical name is "Kedara Gaula" (with space) but imported is "Kedaragaula" (without space).

**Recommendations:**
1. Add cache invalidation hook when entities are created/updated
2. Consider preserving spaces in normalization but using fuzzy matching for comparison
3. Add unit tests for normalization edge cases

### 2.3 Deduplication Service

**Assessment: ⚠️ Incomplete**

**Current Implementation:**
- Checks against canonical `krithis` table
- Checks against staging `imported_krithis` (PENDING status)
- Uses normalized title matching with Levenshtein fallback

**Code Review:**
```kotlin
// DeduplicationService.kt:29-72
suspend fun findDuplicates(...) {
    // 1. Check canonical krithis
    // 2. Check staging imports
    // Missing: Intra-batch deduplication during processing
}
```

**Gaps vs Strategy:**
- ❌ **Missing Intra-Batch Deduplication**: Strategy (Section 4.3) specifies "Check within batch" but implementation only checks after all tasks complete
- ⚠️ **No Batch Context Parameter**: `findDuplicates` doesn't accept batch context for intra-batch comparison
- ⚠️ **Limited Matching Logic**: Only checks title + composer + raga, missing incipit-based matching mentioned in strategy

**Recommendations:**
1. Add batch context parameter to `findDuplicates` for intra-batch comparison
2. Implement incipit-based matching for stronger duplicate detection
3. Consider pre-computing duplicate candidates during batch processing (not just at resolution stage)

### 2.4 Auto-Approval Service

**Assessment: ⚠️ Partially Implemented**

**Current Implementation:**
```kotlin
// AutoApprovalService.kt:16-55
suspend fun autoApproveIfHighConfidence(imported: ImportedKrithiDto) {
    // Rules:
    // - HIGH confidence composer AND raga
    // - No HIGH confidence duplicates
    // - Has minimal metadata (title + lyrics)
}
```

**Gaps vs Strategy:**
- ❌ **Missing Quality Score Check**: Strategy (Section 8.2) specifies quality tiers (EXCELLENT ≥0.90, GOOD ≥0.75, etc.) but implementation doesn't calculate or use quality scores
- ⚠️ **Confidence Thresholds**: Strategy specifies `minConfidenceScore: Double = 0.95` but code uses string-based "HIGH" confidence
- ⚠️ **No Configurable Rules**: Strategy shows `AutoApprovalRules` data class with configurable thresholds, but implementation is hardcoded

**Recommendations:**
1. Implement quality scoring system as specified in strategy (Section 8.2)
2. Make auto-approval rules configurable (via database or config file)
3. Add audit logging for auto-approvals with reasoning

---

## 3. Code Quality Assessment

### 3.1 Error Handling

**Assessment: ✅ Good**

**Strengths:**
- Comprehensive try-catch blocks in worker loops
- Structured error payloads (JSON) with codes and messages
- Retry logic with exponential backoff
- Watchdog for stuck tasks

**Example:**
```kotlin
// BulkImportWorkerService.kt:466-494
catch (e: Exception) {
    val errorJson = buildErrorPayload(
        code = "scrape_failed",
        message = "Scrape/import failed",
        url = url,
        attempt = attempt,
        cause = e.message
    )
    // Marks as RETRYABLE or FAILED based on attempt count
}
```

**Concerns:**
- Some error messages may leak internal details (e.g., stack traces in error JSON)
- No error aggregation/reporting for batch-level error summaries

**Recommendations:**
1. Sanitize error messages before storing (remove stack traces in production)
2. Implement error aggregation for batch-level reporting

### 3.2 Database Operations

**Assessment: ✅ Good**

**Strengths:**
- Uses `DatabaseFactory.dbQuery` pattern consistently
- Proper transaction handling
- Idempotency keys prevent duplicate task creation

**Concerns:**
- **N+1 Query Risk**: `DeduplicationService.findDuplicates` calls `dal.imports.listImports(ImportStatus.PENDING)` which loads all pending imports, then filters in memory
- **Batch Claiming**: Dispatcher claims up to 5 tasks per job type, but doesn't use batch inserts for task creation

**Recommendations:**
1. Optimize deduplication query to filter by normalized title in database
2. Consider batch inserts for task creation during manifest ingest

### 3.3 Concurrency & Thread Safety

**Assessment: ✅ Good**

**Strengths:**
- Uses Kotlin coroutines with proper scoping
- Mutex for rate limiter state (`rateLimiterMutex`)
- Channels provide safe concurrent communication
- SupervisorJob prevents worker failures from cascading

**Code Review:**
```kotlin
// BulkImportWorkerService.kt:78-80
private val rateLimiterMutex = Mutex()
private var globalWindow = RateWindow()
private val perDomainWindows = mutableMapOf<String, RateWindow>()
```

**Minor Issues:**
- `perDomainWindows` is a mutable map accessed from multiple coroutines, but protected by mutex (acceptable)

---

## 4. Implementation Gaps

### 4.1 Missing Features from Strategy

| Feature | Strategy Reference | Clarified Requirements | Implementation Status | Priority |
|:---|:---|:---|:---|:---|
| Quality Scoring System | Section 8.2 | N/A | ❌ Not Implemented | **HIGH** |
| URL Accessibility Validation | Section 4.1.2 | **Syntax-only (2026-01)** | ✅ Implemented Correctly | ~~MEDIUM~~ **N/A** |
| Raga Column Required | Original Strategy | **Optional (2026-01)** | ✅ Implemented Correctly | ~~MEDIUM~~ **N/A** |
| Manifest Ingest Failure Handling | N/A | **Must mark batch FAILED (2026-01)** | ⚠️ Partial | **HIGH** |
| Intra-Batch Deduplication | Section 4.3 | N/A | ⚠️ Partial | **MEDIUM** |
| Configurable Auto-Approval Rules | Section 4.4 | N/A | ⚠️ Hardcoded | **LOW** |
| Quality Tier Filtering (UI) | Section 4.4 | N/A | ❌ Not Implemented | **LOW** |
| Batch Statistics Dashboard | Section 4.4 | N/A | ⚠️ Basic Only | **LOW** |

### 4.2 Database Schema Gaps

**Missing Columns (from Strategy Section 6.2):**
- `extraction_confidence DECIMAL(3,2)` - Not implemented
- `entity_mapping_confidence DECIMAL(3,2)` - Not implemented (resolution_data exists but no aggregated confidence)
- `quality_score DECIMAL(3,2)` - Not implemented
- `quality_tier VARCHAR(20)` - Not implemented
- `processing_errors JSONB` - Not implemented (error stored in task, not imported_krithis)

**Recommendations:**
1. Add quality scoring columns to `imported_krithis` table
2. Calculate and store quality scores during entity resolution stage
3. Add quality tier filtering to review UI

---

## 5. Performance & Scalability

### 5.1 Current Performance Characteristics

**Strengths:**
- Unified dispatcher reduces DB load significantly
- Entity resolution caching prevents N+1 queries
- Batch claiming (5 tasks at a time) reduces lock contention
- Rate limiting prevents external API overload

**Bottlenecks:**
1. **Entity Resolution Cache**: 15-minute TTL means new entities created during import won't be found until cache expires
2. **Deduplication Query**: `listImports(ImportStatus.PENDING)` loads all pending imports into memory
3. **Manifest Parsing**: Single-threaded CSV parsing (acceptable for current scale)

### 5.2 Scalability Concerns

**At Scale (1,200+ entries):**
- **Memory**: Entity resolution cache holds all composers/ragas/talas in memory (acceptable, <5K entities)
- **Database**: Task table will grow large, but indices are appropriate
- **Concurrency**: 3 scrape workers + 2 resolution workers should handle load, but may need tuning

**Recommendations:**
1. Add database connection pooling metrics
2. Monitor task table growth and consider archival strategy
3. Consider horizontal scaling of workers (multiple instances)

---

## 6. Error Handling & Resilience

### 6.1 Retry Strategy

**Assessment: ✅ Good**

**Implementation:**
- Max 3 attempts per task
- Exponential backoff (implicit via retry scheduling)
- Watchdog marks stuck RUNNING tasks as RETRYABLE
- Terminal failures increment batch counters

**Code Review:**
```kotlin
// BulkImportWorkerService.kt:417-432
if (attempt > config.maxAttempts) {
    // Marks as FAILED, increments batch failed counter
    // Triggers next stage check
}
```

**Strengths:**
- Clear retry boundaries
- Prevents infinite retry loops
- Proper state transitions

### 6.2 Failure Isolation

**Assessment: ✅ Good**

**Strengths:**
- Individual task failures don't stop batch
- Job-level status tracking
- Batch can complete with partial failures

**Concerns:**
- No circuit breaker for external services (WebScrapingService)
- Rate limiter doesn't back off on repeated failures

**Recommendations:**
1. Add circuit breaker for WebScrapingService if failure rate exceeds threshold
2. Implement adaptive rate limiting (reduce rate on errors)

---

## 7. Testing & Quality Assurance

### 7.1 Test Coverage

**Assessment: ⚠️ Insufficient**

**Current State:**
- No unit tests found for services
- No integration tests for bulk import flow
- Manual testing mentioned in tracks but no automated tests

**Missing Tests:**
1. **Unit Tests:**
   - `NameNormalizationService` normalization rules
   - `EntityResolutionService` matching logic
   - `DeduplicationService` duplicate detection
   - CSV parsing edge cases

2. **Integration Tests:**
   - End-to-end batch creation → scraping → resolution → review
   - Error recovery scenarios
   - Rate limiting behavior

**Recommendations:**
1. Add unit tests for normalization service (critical for data quality)
2. Add integration test for full import pipeline
3. Add performance tests for large batches (100+ entries)

### 7.2 Manual Testing

**From Tracks:**
- TRACK-001 mentions "Load test on ~1,240 entries" as pending
- No documented test results or success criteria validation

**Recommendations:**
1. Document manual test results
2. Create test data fixtures for reproducible testing
3. Add smoke tests that run on CI/CD

---

## 8. Security Considerations

### 8.1 File Upload Security

**Assessment: ⚠️ Needs Improvement**

**Current Implementation:**
```kotlin
// BulkImportRoutes.kt:32-63
post {
    val multipart = call.receiveMultipart()
    // Saves file to storage/imports/
    // No file size limit
    // No file type validation beyond .csv extension
}
```

**Security Concerns:**
1. **No File Size Limit**: Large CSV files could cause memory issues
2. **Path Traversal Risk**: File name not sanitized before saving
3. **No Content Validation**: Only checks file extension, not actual CSV content

**Recommendations:**
1. Add file size limit (e.g., 10MB)
2. Sanitize file names to prevent path traversal
3. Validate CSV content before processing (not just extension)

### 8.2 URL Validation

**Assessment: ✅ Correct per Clarified Requirements**

**Current:**
- Syntax validation only (scheme, host)
- No accessibility check (per clarified requirements 2026-01)

**Clarified Requirements (2026-01):**
> "URL validation during manifest ingest is syntax-only (no HEAD/GET requirement)."

**Status:** ✅ Implementation correctly follows clarified requirements. No changes needed.

---

## 9. Frontend Implementation Review

### 9.1 Bulk Import Dashboard

**Assessment: ✅ Good**

**Strengths:**
- Real-time polling (2s interval) for active batches
- Clear status visualization with progress bars
- Task filtering by status
- Batch actions (pause, resume, cancel, retry, delete)

**Code Review:**
```typescript
// BulkImport.tsx:89-101
useEffect(() => {
    let interval: NodeJS.Timeout;
    const isRunning = selectedBatch?.status === 'RUNNING' || selectedBatch?.status === 'PENDING';
    if (isRunning && selectedBatchId) {
        interval = setInterval(() => {
            void loadBatchDetail(selectedBatchId);
            void refreshBatches();
        }, 2000);
    }
    return () => clearInterval(interval);
}, [selectedBatch?.status, selectedBatchId]);
```

**Concerns:**
- Polling continues even if user navigates away (should cleanup)
- No error handling for failed API calls in polling

**Recommendations:**
1. Add cleanup for polling on component unmount
2. Add error handling with retry logic for failed API calls

### 9.2 Review Workflow Integration

**Assessment: ⚠️ Partial**

**Current:**
- Batch-level approve/reject all buttons exist
- No batch filter in main review queue (mentioned in TRACK-009 but not implemented)
- No quality tier filtering

**Gaps:**
- Strategy (Section 4.4) specifies "Filter by composer, raga, confidence score" in review UI
- No batch context shown in review queue

**Recommendations:**
1. Add batch filter dropdown to review queue
2. Add quality tier filtering
3. Show batch context in review queue items

---

## 10. Recommendations Summary

### 10.1 Critical (Must Fix)

1. **Fix Manifest Ingest Failure Handling**
   - Update `failManifestTask()` to mark batch as FAILED when manifest ingest fails
   - Ensure batch is marked FAILED even if zero tasks were created (per clarified requirements 2026-01)
   - This is a compliance issue with clarified requirements

2. **Implement Quality Scoring System**
   - Add `quality_score` and `quality_tier` columns to `imported_krithis`
   - Calculate scores during entity resolution (completeness + confidence + validation)
   - Use quality tiers for auto-approval and UI filtering

3. **Fix Entity Resolution Cache Invalidation**
   - Invalidate cache when new entities are created
   - Or reduce TTL to 1-2 minutes during active imports

### 10.2 High Priority (Should Fix)

4. **Complete Deduplication Service**
   - Add intra-batch deduplication during processing
   - Implement incipit-based matching
   - Optimize database queries (filter in DB, not memory)

5. **Add Comprehensive Testing**
   - Unit tests for normalization and resolution logic
   - Integration tests for full pipeline
   - Performance tests for large batches

6. **Improve Error Handling**
   - Sanitize error messages (remove stack traces)
   - Add error aggregation for batch summaries
   - Implement circuit breaker for external services

### 10.3 Medium Priority (Nice to Have)

7. **Make Auto-Approval Configurable**
   - Store rules in database or config file
   - Allow per-batch configuration
   - Add audit logging for auto-approvals

8. **Enhance Frontend**
   - Add batch filter to review queue
   - Add quality tier filtering
   - Improve error handling in polling

9. **Add Observability**
   - Metrics for dispatcher polling frequency
   - Channel utilization metrics
   - Batch processing time histograms
   - Error rate tracking

### 10.4 Low Priority (Future Work)

10. **Performance Optimizations**
    - Batch inserts for task creation
    - Database query optimization for deduplication
    - Consider horizontal scaling of workers

11. **Security Hardening**
    - File size limits
    - Path traversal prevention
    - Content validation for CSV files

---

## 11. Conclusion

The CSV import strategy implementation represents a **solid foundation** with excellent architectural decisions (unified dispatcher, entity resolution caching, comprehensive orchestration). However, several **critical gaps** exist between the strategy document and implementation, particularly around quality scoring, URL validation, and comprehensive deduplication.

**Overall Grade: B+ (Good, with room for improvement)**

**Key Strengths:**
- Well-architected worker system with efficient polling
- Robust error handling and retry mechanisms
- Good separation of concerns

**Key Weaknesses:**
- Missing quality scoring system (specified in strategy)
- Incomplete deduplication (missing intra-batch)
- Insufficient test coverage
- Some security concerns with file uploads

**Next Steps:**
1. **URGENT**: Fix manifest ingest failure handling to mark batch as FAILED (clarified requirements compliance)
2. Prioritize implementing quality scoring system
3. Complete deduplication service with intra-batch support
4. Add comprehensive test suite
5. Address security concerns in file upload

**Note on Clarified Requirements (2026-01):**
The implementation correctly follows most clarified requirements:
- ✅ URL validation is syntax-only (no HEAD/GET required)
- ✅ CSV Raga column is optional (scraped values are authoritative)
- ⚠️ Manifest ingest failure handling needs fix to mark batch as FAILED
- ✅ Scraping failure handling: CSV metadata is not used in scraping stage (only URL is used), so requirement to "discard CSV-seeded metadata" is N/A

**Clarified Requirement Analysis:**
> "If scraping fails after data is presented to the user, discard CSV-seeded metadata and require a new batch."

**Current Implementation:**
- CSV metadata (krithi name, raga) is only used to create task keys during manifest ingest
- Scraping stage only uses the URL; scraped data is authoritative
- No CSV-seeded metadata is stored or used in the import process
- **Status**: Requirement is N/A for current architecture (CSV metadata not used in scraping/import stages)

The implementation is **production-ready for current scale** but needs the above improvements before handling the full 1,200+ entry target with confidence.

---

## 12. Appendix: Code Metrics

### 12.1 Service Complexity

| Service | Lines of Code | Cyclomatic Complexity (Est.) | Dependencies |
|:---|:---|:---|:---|
| `BulkImportWorkerService` | ~790 | High (multiple loops, conditionals) | 6 services |
| `BulkImportOrchestrationService` | ~145 | Low | 1 DAL, 1 service |
| `EntityResolutionService` | ~152 | Medium | 1 DAL, 1 service |
| `DeduplicationService` | ~98 | Low | 1 DAL, 1 service |
| `AutoApprovalService` | ~56 | Low | 2 services |
| `NameNormalizationService` | ~102 | Low | 0 |

### 12.2 Database Schema

- **Tables Added**: 4 (`import_batch`, `import_job`, `import_task_run`, `import_event`)
- **Columns Added to Existing**: 2 (`duplicate_candidates`, `import_batch_id` in `imported_krithis`)
- **Indices**: 12 (appropriate for query patterns)

### 12.3 API Endpoints

- **Bulk Import Routes**: 12 endpoints
- **Review Integration**: 2 endpoints (approve-all, reject-all)
- **Total**: 14 endpoints for bulk import functionality

---

*End of Review*
