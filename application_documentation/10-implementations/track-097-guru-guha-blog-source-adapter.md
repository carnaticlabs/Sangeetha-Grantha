| Metadata | Value |
|:---|:---|
| **Status** | Completed |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-03-17 |
| **Author** | Sangeetha Grantha Team |

# Guru Guha Blog Source Adapter

## Purpose

Fix the guru-guha.blogspot.com extraction pipeline end-to-end so that all 26+ krithis imported from this source have correct structural sections (PALLAVI, ANUPALLAVI, CHARANAM) and clean lyrics — no metadata pollution, no navigation cruft, no URLs in lyric text.

## Root Causes

1. **False metadata boundary**: Structure parser's "Meaning" pattern matched "Meaning of Kriti" navigation links at offset 48, truncating the entire lyric window
2. **Navigation table pollution**: Language-switch `<table>` elements extracted as lyric content
3. **URL noise in lyrics**: Fragment links rendered with full URLs
4. **Stale sections not upgraded**: Re-extraction didn't replace all-OTHER sections from bad extractions
5. **Retry counter not reset**: `retry()` reset status to PENDING but left `attempts` at max

## Implementation Details

### Python Extraction Worker Fixes
- Strip navigation `<table>` elements with language-switch links
- Skip navigation boilerplate links (Meaning of Kriti, Notation, etc.)
- Omit URLs for fragment/same-page links in lyric content
- Negative lookaheads on MEANING/NOTES boundary patterns
- Minimum-content guard (skip early boundaries < 200 chars)

### Backend Fixes
- `LyricVariantPersistenceService`: Upgrade stale all-OTHER sections on re-extraction; backfill includes MAPPED and IN_REVIEW statuses
- `ImportRoutes`: `POST /re-extract` endpoint with typed `ReExtractResponse`
- `KrithiLyricRepository`: `deleteAllVariants()` for clearing before re-extraction
- `ExtractionQueueRepository`: Reset `attempts = 0` in `retry()` and `retryAllFailed()`

## Code Changes

| File | Change |
|------|--------|
| `tools/krithi-extract-enrich-worker/src/html_extractor.py` | Navigation table stripping, link filtering |
| `tools/krithi-extract-enrich-worker/src/structure_parser.py` | Metadata boundary hardening, minimum-content guard |
| `tools/krithi-extract-enrich-worker/src/worker.py` | URL-based composer inference fallback |
| `tools/krithi-extract-enrich-worker/tests/test_html_extractor.py` | Guru-guha blog tests |
| `modules/backend/api/.../services/LyricVariantPersistenceService.kt` | Section upgrade, multi-status backfill |
| `modules/backend/api/.../routes/ImportRoutes.kt` | Re-extract endpoint, typed response DTOs |
| `modules/backend/dal/.../repositories/KrithiLyricRepository.kt` | `deleteAllVariants()` |
| `modules/backend/dal/.../repositories/ExtractionQueueRepository.kt` | Reset attempts in `retry()` |
| `database/migrations/42__reset_guru_guha_extraction_attempts.sql` | Reset extraction attempts for guru-guha entries |
| `database/migrations/43__reset_guru_guha_to_pending.sql` | Reset guru-guha queue entries to PENDING |

## Results

- All 26/26 guru-guha krithis have lyric variants and correct structural sections
- Clean lyrics with no navigation cruft or URL pollution
- Re-extraction pipeline fully functional with proper retry semantics

Ref: application_documentation/10-implementations/track-097-guru-guha-blog-source-adapter.md
