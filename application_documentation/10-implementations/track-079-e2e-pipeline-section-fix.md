| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-03-10 |
| **Author** | Sangeetha Grantha Team |

# E2E Pipeline Validation & Lyric Section Consistency Fix

## Purpose

Prove the CSV bulk import + PDF extraction pipeline end-to-end with real Dikshitar compositions, and fix the critical lyric section inconsistency affecting 92% of krithis.

## Implementation Details

### E2E Pipeline (Phase 3)
- Docker Compose orchestration for full dev stack
- Fixed auto-creation bug: unmatched extractions now route to `imported_krithis` as PENDING for curator review
- Extraction worker rollback fix for cascading DB errors
- Vite proxy configuration for Docker networking

### Section Fix (Phase 4A)
- Python `structure_parser.py`: MKS demotion, dual-format merge, bracket headers, Indic anusvara/explicit-m forms
- Kotlin `LyricVariantPersistenceService`: type+queue matching, MKS filtered from canonical sections
- `KrithiLyricRepository`: ORDER BY order_index via JOIN
- Migration 38: remove MKS top-level sections, deduplicate, re-index

## Code Changes

| File | Change |
|------|--------|
| `compose.yaml` | Dev backend source mount, extraction DATABASE_URL, frontend proxy |
| `KrithiMatcherService.kt` | Route unmatched to `imported_krithis` instead of auto-creating |
| `structure_parser.py` | MKS demotion, dual-format merge, bracket headers |
| `LyricVariantPersistenceService.kt` | Type+queue section matching |
| `KrithiLyricRepository.kt` | ORDER BY via JOIN for section order |
| `38__fix_inconsistent_lyric_sections.sql` | Section cleanup migration |
| `worker.py` | DB rollback after errors |

## Results

- 474 krithis with source evidence
- 0 auto-created krithis (was 141)
- 172 unmatched → pending review
- 0 section inconsistencies (was 435)

Ref: application_documentation/10-implementations/track-079-e2e-pipeline-section-fix.md
