| Metadata | Value |
|:---|:---|
| **Track ID** | TRACK-062 |
| **Title** | Bulk Import Idempotency & Source Evidence Integration |
| **Status** | Done |
| **Priority** | High |
| **Created** | 2026-02-10 |
| **Updated** | 2026-02-11 |
| **Depends On** | TRACK-061 |
| **Spec Ref** | analysis-extraction-pipeline-failures.md (Findings 5, 7) |
| **Est. Effort** | 2–3 days |

# TRACK-062: Bulk Import Idempotency & Source Evidence Integration

## Objective

Fix the two bulk import issues discovered in the pipeline analysis:

1. **Duplicate Krithi creation on retry/re-run** — The blogspot import created 3 copies
   of many Krithis (32 duplicates total) because the import path has no idempotency
   guard. When a batch is interrupted and retried, or when the orchestrator creates
   multiple jobs for the same tasks, each run creates new Krithis without checking
   for existing ones with the same normalised title.

2. **No source evidence records** — The bulk import path predates the sourcing pipeline
   (TRACK-041+) and does not write `krithi_source_evidence` records. This means
   Krithis from blogspot have no provenance trail and cannot participate in structural
   voting or quality scoring.

### Evidence

- `krithi_source_evidence` has 0 rows where `source_url LIKE '%blogspot%'`
- "rAma janArdana" was created at 14:11:23, 14:11:34, and 14:12:07 (3 copies, ~11s apart)
- Blogspot-imported krithi c152ec7a has 9 sections instead of 3 (duplicate sections from re-run)

## Scope

- **Kotlin backend** — Bulk import orchestration service and import service.
- Add deduplication check before Krithi creation in bulk import path.
- Add `krithi_source_evidence` record creation in the bulk import finalization.
- Add idempotency guard on section creation (skip if sections already exist).
- No database migration needed (tables already exist).

## Design Decisions

| Decision | Choice | Rationale |
|:---|:---|:---|
| Dedup strategy | Check `findDuplicateCandidates(titleNormalized)` before creating Krithi | Reuses existing dedup logic from ExtractionResultProcessor |
| When dupe found | Link to existing Krithi instead of creating new one; log as "matched" | Same pattern as ExtractionResultProcessor |
| Source evidence in bulk import | Write evidence record during import task completion (not finalization) | Evidence should exist as soon as the Krithi is linked, for audit trail |
| Section idempotency | `ON CONFLICT DO NOTHING` or check-before-insert on `(krithi_id, section_type, order_index)` | Prevents duplicate sections on re-run |
| Batch-level idempotency | Use `idempotency_key` (already on `import_task_run`) to skip already-completed tasks | Column exists but may not be enforced |

## Task List

| Task ID | Description | Acceptance Criteria | File(s) |
|:---|:---|:---|:---|
| T62.1 | Add dedup check in bulk import Krithi creation | Before creating a new Krithi, call `findDuplicateCandidates(titleNormalized)`. If match found, use existing Krithi ID. Log "matched" vs "created" count. | Import service (Krithi creation path) |
| T62.2 | Write `krithi_source_evidence` in bulk import path | Each imported Krithi (new or matched) gets a source evidence record with `source_url`, `source_format=HTML`, `extraction_method=HTML_JSOUP`, `contributed_fields`. | Import service |
| T62.3 | Add section creation idempotency | Skip section insert if `(krithi_id, section_type, order_index)` already exists. | DAL — KrithiRepository or section creation logic |
| T62.4 | Enforce `idempotency_key` on `import_task_run` | Before processing a task, check if a completed task with the same `idempotency_key` exists. If so, skip. | Import orchestration service |
| T62.5 | Add lyric variant dedup guard | Before creating a lyric variant, check if one with `(krithi_id, language, script, source_reference)` already exists. | DAL — lyric variant creation |
| T62.6 | Unit tests for bulk import idempotency | Test: submit same batch twice → no duplicate Krithis, sections, or variants created. Source evidence written once per source. | Integration tests |

## Files Changed

| File | Change |
|:---|:---|
| `modules/backend/api/.../services/ImportService.kt` | T62.1: Dedup check via `findDuplicateCandidates(titleNormalized)` before Krithi creation; T62.2: Source evidence creation on import approval |
| `modules/backend/dal/.../repositories/KrithiRepository.kt` | T62.3: Section creation idempotency (skip if exists); T62.5: Lyric variant dedup guard |
| `modules/backend/dal/.../repositories/SourceEvidenceRepository.kt` | T62.2: Idempotency guard on `createEvidence()` |
| `modules/backend/dal/.../repositories/BulkImportRepository.kt` | T62.4: `idempotency_key` enforcement via `ON CONFLICT DO NOTHING` in `createTask()`/`createTasks()` |
| `modules/backend/api/.../services/ImportServiceTest.kt` | T62.6: 4 new idempotency tests (submit dedup, review dedup, evidence dedup, bulk task dedup) |
| `modules/backend/api/.../support/MigrationRunner.kt` | New: reads and executes SQL migration files for test DB schema setup |
| `modules/backend/api/.../support/IntegrationTestBase.kt` | New: base class for integration tests with migration-based schema + TRUNCATE cleanup |
| `modules/backend/api/.../support/TestDatabaseFactory.kt` | Rewritten: replaced manual enum/SchemaUtils approach with migration-based setup |
| `database/migrations/30__fix_entity_resolution_cache_schema.sql` | New: fixes missing `updated_at` column and `confidence` type on `entity_resolution_cache` |

## Progress Log

| Date | Unit | Notes |
|:---|:---|:---|
| 2026-02-10 | Planning | Track created from analysis-extraction-pipeline-failures.md |
| 2026-02-10 | T62.1–T62.5 | Implementation complete — dedup check, source evidence, section idempotency, idempotency key enforcement, lyric variant dedup |
| 2026-02-11 | T62.6 | Added 4 idempotency integration tests to ImportServiceTest |
| 2026-02-11 | Infra | Created migration-based integration test infrastructure (MigrationRunner, IntegrationTestBase) replacing SchemaUtils approach. Migrated all 5 test classes. Fixed entity_resolution_cache schema drift (migration 30). All 12 integration tests pass. |
