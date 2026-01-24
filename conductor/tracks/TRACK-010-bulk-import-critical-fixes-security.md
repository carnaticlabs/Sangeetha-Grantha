# TRACK-010: Bulk Import Critical Fixes & Security Hardening

| Metadata | Value |
|:---|:---|
| **Status** | Completed |
| **Owner** | Backend Team |
| **Priority** | CRITICAL |
| **Created** | 2026-01-23 |
| **Completed** | 2026-01-23 |
| **Related Tracks** | TRACK-001 (Bulk Import), TRACK-011 (Quality Scoring) |
| **Implementation Plan** | [bulk-import-fixes-implementation-plan.md](../../application_documentation/07-quality/bulk-import-fixes-implementation-plan.md) |

## 1. Goal

Fix critical correctness issues and security vulnerabilities identified in code reviews (Claude, Goose, Codex) that block production deployment. These fixes ensure compliance with clarified requirements and prevent security exploits.

## 2. Problem Statement

Four critical issues prevent safe production deployment:

1. **Manifest Ingest Failure Handling:** Batch not marked FAILED when manifest ingest fails with zero tasks, violating clarified requirements (2026-01).
2. **Task Stuck Detection Race Condition:** Tasks marked RUNNING at claim time, but watchdog may mark as RETRYABLE before execution, risking double-processing.
3. **File Upload Security Vulnerabilities:** Path traversal risk, no file size limits (OOM risk), null filename handling.
4. **CSV Parsing Issues:** Platform default charset (diacritic handling risk), file readers not closed (file descriptor leaks).

## 3. Implementation Plan

### 3.1 Fix Manifest Ingest Failure Handling

**Issue:** `failManifestTask()` updates task/job status but never updates batch status to FAILED.

**Files:**
- `modules/backend/api/src/main/kotlin/com/sangita/grantha/backend/api/services/BulkImportWorkerService.kt`

**Changes:**
```kotlin
private suspend fun failManifestTask(task: ImportTaskRunDto, job: ImportJobDto, startedAt: OffsetDateTime, errorJson: String) {
    val now = OffsetDateTime.now(ZoneOffset.UTC)
    dal.bulkImport.updateTaskStatus(
        id = task.id,
        status = TaskStatus.FAILED,
        error = errorJson,
        durationMs = elapsedMsSince(startedAt),
        completedAt = now
    )
    dal.bulkImport.updateJobStatus(id = job.id, status = TaskStatus.FAILED, result = errorJson, completedAt = now)
    dal.bulkImport.createEvent(refType = "batch", refId = job.batchId, eventType = "MANIFEST_INGEST_FAILED", data = errorJson)
    
    // ✅ NEW: Mark batch as FAILED (per clarified requirements 2026-01)
    dal.bulkImport.updateBatchStatus(id = job.batchId, status = BatchStatus.FAILED, completedAt = now)
}
```

**Testing:**
- Unit test: Manifest ingest failure with zero tasks → batch marked FAILED
- Integration test: Invalid CSV upload → batch fails immediately

---

### 3.2 Fix Task Stuck Detection Race Condition

**Issue:** Tasks are marked RUNNING with `startedAt` at claim time (line 365 in BulkImportRepository.kt), but workers may not begin immediately when channels are full. Watchdog may mark these as RETRYABLE before execution starts.

**Options:**
- **Option A:** Add QUEUED state (tasks claimed but not yet executing)
- **Option B:** Only set `startedAt` when worker begins execution (not at claim time)

**Recommendation:** Option B (simpler, no schema change)

**Files:**
- `modules/backend/dal/src/main/kotlin/com/sangita/grantha/backend/dal/repositories/BulkImportRepository.kt`
- `modules/backend/api/src/main/kotlin/com/sangita/grantha/backend/api/services/BulkImportWorkerService.kt`

**Changes:**

1. **Repository:** Don't set `startedAt` in `claimNextPendingTasks()`:
```kotlin
ImportTaskRunTable.update(where = { ImportTaskRunTable.id inList taskIds }) {
    it[ImportTaskRunTable.status] = TaskStatus.RUNNING
    // ❌ REMOVE: it[ImportTaskRunTable.startedAt] = now
    it[ImportTaskRunTable.updatedAt] = now
}
```

2. **Worker Service:** Set `startedAt` when worker begins execution:
```kotlin
private suspend fun processManifestTask(task: ImportTaskRunDto, config: WorkerConfig) {
    val startedAt = OffsetDateTime.now(ZoneOffset.UTC)
    
    // ✅ NEW: Set startedAt when execution begins (not at claim time)
    dal.bulkImport.updateTaskStatus(
        id = task.id,
        startedAt = startedAt
    )
    
    // ... rest of existing logic
}
```

**Testing:**
- Unit test: Task claimed but channel full → watchdog doesn't mark as RETRYABLE
- Integration test: High channel pressure → tasks only marked RUNNING when executing

---

### 3.3 Fix File Upload Security Vulnerabilities

**Issues:**
1. Path traversal: `originalFileName` used directly (no basename sanitization)
2. No file size limits (OOM risk for large files)
3. Null filename handling

**Files:**
- `modules/backend/api/src/main/kotlin/com/sangita/grantha/backend/api/routes/BulkImportRoutes.kt`

**Changes:**
```kotlin
post {
    val multipart = call.receiveMultipart()
    var savedFilePath: String? = null
    val MAX_FILE_SIZE = 10 * 1024 * 1024 // 10MB

    multipart.forEachPart { part ->
        if (part is PartData.FileItem) {
            // ✅ NEW: Validate filename
            val originalFileName = part.originalFileName
                ?: throw IllegalArgumentException("File name is required")
            
            // ✅ NEW: Sanitize filename (prevent path traversal)
            val sanitizedFileName = Paths.get(originalFileName).fileName.toString()
                .replace(Regex("[^a-zA-Z0-9._-]"), "_")
            
            if (sanitizedFileName.isEmpty()) {
                throw IllegalArgumentException("Invalid file name")
            }
            
            // ✅ NEW: Validate file extension
            if (!sanitizedFileName.endsWith(".csv", ignoreCase = true)) {
                throw IllegalArgumentException("Only CSV files are allowed")
            }
            
            val fileBytes = part.provider().readRemaining().readBytes()
            
            // ✅ NEW: Enforce file size limit
            if (fileBytes.size > MAX_FILE_SIZE) {
                throw IllegalArgumentException("File size exceeds maximum allowed size (10MB)")
            }
            
            // Ensure storage directory exists
            val storageDir = Paths.get("storage/imports")
            if (!Files.exists(storageDir)) {
                Files.createDirectories(storageDir)
            }

            // Create unique file name to avoid collisions
            val timestamp = System.currentTimeMillis()
            val uniqueName = "${timestamp}_${sanitizedFileName}"
            val file = File(storageDir.toFile(), uniqueName)
            file.writeBytes(fileBytes)
            savedFilePath = file.absolutePath
        }
        part.dispose()
    }

    if (savedFilePath != null) {
        val created = service.createBatch(savedFilePath!!)
        call.respond(HttpStatusCode.Accepted, created)
    } else {
        call.respondText("No file uploaded", status = HttpStatusCode.BadRequest)
    }
}
```

**Testing:**
- Unit test: Path traversal attempt (e.g., `../../../etc/passwd`) → sanitized
- Unit test: Large file (>10MB) → rejected
- Unit test: Null filename → rejected
- Unit test: Non-CSV file → rejected

---

### 3.4 Fix CSV Parsing Issues

**Issues:**
1. Platform default charset (diacritic handling risk)
2. File readers not closed (file descriptor leaks)

**Files:**
- `modules/backend/api/src/main/kotlin/com/sangita/grantha/backend/api/services/BulkImportWorkerService.kt`

**Changes:**
```kotlin
private fun parseCsvManifest(path: Path): List<CsvRow> {
    // ✅ NEW: Use UTF-8 explicitly and ensure file is closed
    path.toFile().bufferedReader(Charsets.UTF_8).use { reader ->
        val parser = CSVFormat.DEFAULT.builder()
            .setHeader()
            .setSkipHeaderRecord(true)
            .setIgnoreHeaderCase(true)
            .setTrim(true)
            .build()
            .parse(reader)
        
        // ... rest of existing parsing logic
    }
}
```

**Testing:**
- Unit test: CSV with diacritics (e.g., "Muthuswāmi") → parsed correctly
- Integration test: Multiple CSV parses → no file descriptor leaks

---

## 4. Progress Log

### 2026-01-23: Track Created & Completed
- ✅ Analyzed code reviews (Claude, Goose, Codex)
- ✅ Identified critical issues
- ✅ Created implementation plan
- ✅ Fix manifest ingest failure handling - [BulkImportWorkerService.kt:394-396](../../modules/backend/api/src/main/kotlin/com/sangita/grantha/backend/api/services/BulkImportWorkerService.kt#L394-L396)
- ✅ Fix task stuck detection race condition - [BulkImportRepository.kt:361-364](../../modules/backend/dal/src/main/kotlin/com/sangita/grantha/backend/dal/repositories/BulkImportRepository.kt#L361-L364) + [markTaskStarted:379-393](../../modules/backend/dal/src/main/kotlin/com/sangita/grantha/backend/dal/repositories/BulkImportRepository.kt#L379-L393)
- ✅ Fix file upload security vulnerabilities - [BulkImportRoutes.kt:39-94](../../modules/backend/api/src/main/kotlin/com/sangita/grantha/backend/api/routes/BulkImportRoutes.kt#L39-L94)
- ✅ Fix CSV parsing issues - [BulkImportWorkerService.kt:792](../../modules/backend/api/src/main/kotlin/com/sangita/grantha/backend/api/services/BulkImportWorkerService.kt#L792)

---

## 5. Success Criteria

- ✅ Manifest ingest failures mark batch as FAILED (even with zero tasks)
- ✅ Tasks only marked RUNNING when worker begins execution
- ✅ File uploads sanitized, size-limited, null-safe
- ✅ CSV parsing uses UTF-8, closes file handles
- ✅ All security vulnerabilities addressed
- ✅ Unit tests pass
- ✅ Integration tests pass

---

## 6. References

- [Claude Review](../../application_documentation/07-quality/bulk-import-implementation-review-claude.md)
- [Goose Review](../../application_documentation/07-quality/csv-import-strategy-implementation-review-goose.md)
- [Codex Review](../../application_documentation/07-quality/csv-import-strategy-review-codex.md)
- [Implementation Plan](../../application_documentation/07-quality/bulk-import-fixes-implementation-plan.md)
- [Technical Implementation Guide](../../application_documentation/01-requirements/features/bulk-import/02-implementation/technical-implementation-guide.md) (Clarified Requirements 2026-01)
