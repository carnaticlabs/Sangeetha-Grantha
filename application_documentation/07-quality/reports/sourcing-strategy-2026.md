| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.0.1 |
| **Last Updated** | 2026-02-19 |
| **Author** | Sangeetha Grantha Team |

# Krithi Data Sourcing & Quality — Progress Report

> [!NOTE]
> Current phase: **Phase 0 — Foundation & Quality Baseline** (In Progress)

## Implementation Status Summary

### Phase 0: Foundation & Quality Baseline (In Progress)

| Task | Status | Artifact |
|:---|:---|:---|
| TRACK-039: Section count mismatch audit query | Done | `database/audits/audit_section_count_mismatch.sql` |
| TRACK-039: Label sequence mismatch audit query | Done | `database/audits/audit_label_sequence_mismatch.sql` |
| TRACK-039: Orphaned lyric blobs audit query | Done | `database/audits/audit_orphaned_lyric_blobs.sql` |
| TRACK-039: Run audits against production database | Pending | — |
| Canonical extraction schema (Kotlin) | Done | `CanonicalExtractionDto.kt` |
| Canonical extraction schema (Python) | Done | `tools/krithi-extract-enrich-worker/src/schema.py` |
| Migration 23: Source authority enhancement | Done | `database/migrations/23__source_authority_enhancement.sql` |
| Migration 24: Krithi source evidence | Done | `database/migrations/24__krithi_source_evidence.sql` |
| Migration 25: Structural vote log | Done | `database/migrations/25__structural_vote_log.sql` |
| Migration 26: Import task format tracking | Done | `database/migrations/26__import_task_format_tracking.sql` |
| Migration 27: Extraction queue | Done | `database/migrations/27__extraction_queue.sql` |
| Source registry seed data | Done | `database/seed_data/04_import_sources_authority.sql` |
| Run migrations against dev database | Pending | — |

### Phase 1: PDF Ingestion — Skeleton Extraction (In Progress)

| Task | Status | Artifact |
|:---|:---|:---|
| Python project structure | Done | `tools/krithi-extract-enrich-worker/` |
| pyproject.toml with dependencies | Done | `tools/krithi-extract-enrich-worker/pyproject.toml` |
| Dockerfile with Tesseract + Indic packs | Done | `tools/krithi-extract-enrich-worker/Dockerfile` |
| config.py (environment configuration) | Done | `tools/krithi-extract-enrich-worker/src/config.py` |
| db.py (extraction_queue operations) | Done | `tools/krithi-extract-enrich-worker/src/db.py` |
| extractor.py (PyMuPDF text extraction) | Done | `tools/krithi-extract-enrich-worker/src/extractor.py` |
| page_segmenter.py (Krithi boundary detection) | Done | `tools/krithi-extract-enrich-worker/src/page_segmenter.py` |
| structure_parser.py (section label detection) | Done | `tools/krithi-extract-enrich-worker/src/structure_parser.py` |
| metadata_parser.py (header field extraction) | Done | `tools/krithi-extract-enrich-worker/src/metadata_parser.py` |
| ocr_fallback.py (Tesseract integration) | Done | `tools/krithi-extract-enrich-worker/src/ocr_fallback.py` |
| transliterator.py (indic-transliteration) | Done | `tools/krithi-extract-enrich-worker/src/transliterator.py` |
| worker.py (DB-queue polling worker) | Done | `tools/krithi-extract-enrich-worker/src/worker.py` |
| cli.py (CLI entry point) | Done | `tools/krithi-extract-enrich-worker/src/cli.py` |
| Unit tests (schema, structure parser) | Done | `tools/krithi-extract-enrich-worker/tests/` |
| Docker Compose extension | Done | `compose.yaml` |
| PoC: 10 Dikshitar Krithis from mdskt.pdf | Pending | — |
| Kotlin ExtractionQueueRepository | Pending | — |
| Kotlin ExtractionQueueService | Pending | — |
| Kotlin ExtractionResultProcessor | Pending | — |
| End-to-end integration test | Pending | — |

### Track Status

| Track | Progress | Last Updated |
|:---|:---|:---|
| TRACK-039 | Audit queries written; awaiting execution against production DB | 2026-02-08 |
| TRACK-040 | Foundation laid; remediation deferred to Phase 4 | 2026-02-08 |
| TRACK-041 | Source authority model, DB schema, canonical schema, and Python extraction service foundation complete | 2026-02-08 |

## Next Steps

1. Run TRACK-039 SQL audits against production database to establish quality baseline.
2. Run database migrations 23–27 against dev database.
3. Insert source registry seed data.
4. Prototype: Extract 10 Dikshitar Krithis from guruguha.org mdskt.pdf using CLI.
5. Build Kotlin-side integration (ExtractionQueueRepository, ExtractionQueueService, ExtractionResultProcessor).
6. Validate three-container stack: postgres + krithi-extract-enrich-worker + Kotlin backend.
