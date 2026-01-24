| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-01-24 |
| **Author** | Sangeetha Grantha Team |

# CSV Bulk Import Strategy Review (Codex)

## Executive Summary
This report consolidates findings from `application_documentation/07-quality/bulk-import-implementation-review-claude.md` and `application_documentation/07-quality/csv-import-strategy-implementation-review-goose.md` with a focused pass over the current backend implementation. The architecture (unified dispatcher, staged workers, event logging) is solid, but there are several critical correctness and security risks plus strategic gaps around quality scoring, review workflow depth, and performance at scale.

## Findings (ordered by severity)

### High
- **Queued tasks can be marked stuck before they ever start.** Tasks are set to `RUNNING` with `startedAt` at claim time, but workers may not begin immediately when channels are full; the watchdog re-marks these as `RETRYABLE`, risking double-processing and duplicate side effects. Consider a `QUEUED` state or only setting `startedAt` when a worker begins execution. (`modules/backend/dal/src/main/kotlin/com/sangita/grantha/backend/dal/repositories/BulkImportRepository.kt`, `modules/backend/api/src/main/kotlin/com/sangita/grantha/backend/api/services/BulkImportWorkerService.kt`)
- **Manifest ingest failures do not fail the batch.** `failManifestTask` updates the task/job and emits an event but never updates the batch status, leaving `RUNNING` batches with zero tasks. This contradicts the clarified requirement to mark the batch `FAILED` on manifest ingest errors. (`modules/backend/api/src/main/kotlin/com/sangita/grantha/backend/api/services/BulkImportWorkerService.kt`)
- **Upload endpoint risks path traversal and OOM.** `originalFileName` is used directly (no basename sanitization), and the entire file is read into memory with no size limit; null filenames can also throw. This is a security and stability issue. (`modules/backend/api/src/main/kotlin/com/sangita/grantha/backend/api/routes/BulkImportRoutes.kt`)
- **Quality scoring and tiering are not implemented in code.** Schema support exists, but there is no calculation or persistence of `quality_score`, `quality_tier`, or confidence aggregations, and auto-approval uses hardcoded heuristics instead of strategy-defined tiers. This blocks strategy goals around review prioritization and automation. (`database/migrations/15__add_missing_import_metadata.sql`, `modules/backend/api/src/main/kotlin/com/sangita/grantha/backend/api/services/AutoApprovalService.kt`)

### Medium
- **Review workflow depth is limited vs strategy.** There is per-import review and batch-level approve/reject-all, but strategy endpoints for bulk-review and auto-approve queues are absent; this limits batch-scale moderation workflows. (`modules/backend/api/src/main/kotlin/com/sangita/grantha/backend/api/routes/ImportRoutes.kt`, `modules/backend/api/src/main/kotlin/com/sangita/grantha/backend/api/routes/BulkImportRoutes.kt`)
- **CSV validation is deferred until manifest ingest.** The upload endpoint always creates a batch and defers header/row validation to the manifest worker, so invalid CSVs can fail minutes later rather than fast-failing at upload. (`modules/backend/api/src/main/kotlin/com/sangita/grantha/backend/api/routes/BulkImportRoutes.kt`, `modules/backend/api/src/main/kotlin/com/sangita/grantha/backend/api/services/BulkImportWorkerService.kt`)
- **Entity resolution cache is in-memory only and can be stale.** A cache table exists in schema, but resolution relies solely on in-memory lists with a 15-minute TTL, so new entities created during a batch are missed until cache refresh; multi-node deployments also diverge. (`modules/backend/api/src/main/kotlin/com/sangita/grantha/backend/api/services/EntityResolutionService.kt`, `database/migrations/15__add_missing_import_metadata.sql`)
- **Deduplication scales poorly for large batches.** For every import, the service loads all pending imports and filters in memory, leading to O(N^2) behavior for 1,200+ records. (`modules/backend/api/src/main/kotlin/com/sangita/grantha/backend/api/services/DeduplicationService.kt`)
- **Stage completion checks are O(N) per task.** `checkAndTriggerNextStage` loads all tasks on every task completion; for large batches this becomes significant DB overhead. (`modules/backend/api/src/main/kotlin/com/sangita/grantha/backend/api/services/BulkImportWorkerService.kt`)
- **Rate limiting is far more conservative than strategy.** Defaults (12/min per domain, 50/min global) imply 60-100+ minutes for 1,200 URLs, while the strategy assumed 120/min. If intentional, document the throughput tradeoff. (`modules/backend/api/src/main/kotlin/com/sangita/grantha/backend/api/services/BulkImportWorkerService.kt`)
- **CSV parsing uses platform default charset and leaves readers open.** `FileReader` defaults to system charset and is not closed, risking incorrect diacritic handling and file descriptor leaks. (`modules/backend/api/src/main/kotlin/com/sangita/grantha/backend/api/services/BulkImportWorkerService.kt`)
- **Intra-batch deduplication is limited to hyperlink uniqueness.** Duplicate titles within the same CSV are not flagged at manifest ingest, which can create redundant work items until later review. (`modules/backend/api/src/main/kotlin/com/sangita/grantha/backend/api/services/BulkImportWorkerService.kt`)

### Low
- **Honorific removal never triggers due to regex escaping.** `"\b"` is a backspace in Kotlin strings, so honorifics are not removed during normalization. (`modules/backend/api/src/main/kotlin/com/sangita/grantha/backend/api/services/NameNormalizationService.kt`)
- **Normalized lookup maps drop collisions.** `associateBy` keeps only one entity per normalized key, so collisions can silently bias resolution toward the last entry. (`modules/backend/api/src/main/kotlin/com/sangita/grantha/backend/api/services/EntityResolutionService.kt`)
- **Delete-batch TODOs remain in production code.** Inline comments indicate uncertainty about delete behavior despite an implemented repository call. (`modules/backend/api/src/main/kotlin/com/sangita/grantha/backend/api/services/BulkImportOrchestrationService.kt`)

## Strategy Alignment Notes
- **CSV ingest approach diverges from strategy.** Implementation parses CSVs in Kotlin at runtime, while the strategy described a Python seed-data flow. This is acceptable but should be explicitly documented as an intentional deviation.
- **Quality-tier driven review is not yet realized.** The strategy defines EXCELLENT/GOOD/FAIR/POOR tiers and confidence thresholds; current implementation does not compute or store these values, so review prioritization and automation cannot be applied.
- **URL validation requirement updated.** The strategy mentions HEAD/GET checks, but the confirmed requirement is syntax-only validation; ensure documentation reflects this decision.

## Decisions / Clarifications (confirmed)
- The CSV `Raga` column is optional; it is used only for validation at CSV creation time and the authoritative value is scraped from the URL.
- URL validation during manifest ingest should be syntax-only (no HEAD/GET).
- If scraping fails after data is shown to the user, CSV metadata should be discarded; users can import via a new batch.
- Manifest ingest failures should explicitly mark the batch as `FAILED`, even with zero tasks.

## Testing Gaps
- No automated tests for CSV header/row validation, malformed rows, or URL validation edge cases.
- No unit tests for normalization rules, confidence thresholds, or deduplication heuristics.
- No integration tests covering manifest ingest -> scrape -> resolution -> review lifecycle or failure recovery.

## Change Summary (secondary)
- The dispatcher/worker architecture, batch/job/task model, and event logging align well with the strategy’s staged pipeline.
- CSV parsing, scraping, resolution, and auto-approval are integrated into a single worker service, but quality tiering and batch-scale review tooling remain incomplete.
