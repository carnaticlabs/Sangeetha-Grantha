| Metadata | Value |
|:---|:---|
| **Status** | Completed |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-03-15 |
| **Author** | Sangeetha Grantha Team |

# Goal

Fix the guru-guha.blogspot.com extraction pipeline end-to-end so that all 26+ krithis imported from this source have correct structural sections (PALLAVI, ANUPALLAVI, CHARANAM) and clean lyrics — no metadata pollution, no navigation cruft, no URLs in lyric text.

# Motivation

Krithis imported from guru-guha.blogspot.com showed "abhayAmbA jagadambA - rAgaM kalyANi - tALaM Adi" as lyrics under an UNKNOWN section type. Root causes:
1. **False metadata boundary**: The structure parser's "Meaning" pattern matched "Meaning of Kriti" navigation links at offset 48, truncating the entire lyric window
2. **Navigation table pollution**: Language-switch `<table>` elements (English | Devanagari | Tamil | …) were extracted as lyric content
3. **URL noise in lyrics**: Fragment links like `<a href="#V1">text</a>` rendered as `"text (full-url#V1)"`
4. **Stale sections not upgraded**: Once an import had all-OTHER sections from a bad extraction, re-extraction didn't replace them
5. **Retry counter not reset**: `ExtractionQueueRepository.retry()` reset status to PENDING but left `attempts` at max, so the claim query returned no rows

# Implementation Plan

## Python Extraction Worker Fixes
- [x] `html_extractor.py`: Strip navigation `<table>` elements with language-switch links
- [x] `html_extractor.py`: Skip navigation boilerplate links (Meaning of Kriti, Notation, etc.)
- [x] `html_extractor.py`: Omit URLs for fragment/same-page links in lyric content
- [x] `structure_parser.py`: Add negative lookaheads to MEANING/NOTES boundary patterns
- [x] `structure_parser.py`: Add minimum-content guard (skip early boundaries < 200 chars)
- [x] Tests: `test_guru_guha_blog_navigation_stripped`, `test_structure_parser_no_false_meaning_boundary`

## Backend Fixes
- [x] `LyricVariantPersistenceService.kt`: Upgrade stale all-OTHER sections on re-extraction
- [x] `LyricVariantPersistenceService.kt`: Backfill includes MAPPED and IN_REVIEW statuses
- [x] `ImportRoutes.kt`: Add `POST /re-extract` endpoint with typed `ReExtractResponse`
- [x] `KrithiLyricRepository.kt`: Add `deleteAllVariants()` for clearing before re-extraction
- [x] `ExtractionQueueRepository.kt`: Reset `attempts = 0` in `retry()` and `retryAllFailed()`

## Re-extraction Cycle
- [x] Rebuild backend with retry fix
- [x] Reset extraction queue entries to PENDING with attempts=0
- [x] Verify Python worker processes all 26 entries (INGESTED, attempts=1)
- [x] Call `POST /v1/admin/imports/backfill-lyrics` — 26 processed, 0 errors
- [x] Verify krithi 287051ca shows PALLAVI/ANUPALLAVI/CHARANAM with clean lyrics ✓
- [x] All 26/26 guru-guha krithis have lyric variants and sections ✓

# Files Modified

| File | Change |
|:---|:---|
| `tools/krithi-extract-enrich-worker/src/html_extractor.py` | Navigation table stripping, link filtering |
| `tools/krithi-extract-enrich-worker/src/structure_parser.py` | Metadata boundary hardening, minimum-content guard |
| `tools/krithi-extract-enrich-worker/tests/test_html_extractor.py` | Guru-guha blog tests |
| `modules/backend/api/.../services/LyricVariantPersistenceService.kt` | Section upgrade, multi-status backfill |
| `modules/backend/api/.../routes/ImportRoutes.kt` | Re-extract endpoint, typed response DTOs |
| `modules/backend/dal/.../repositories/KrithiLyricRepository.kt` | deleteAllVariants() |
| `modules/backend/dal/.../repositories/ExtractionQueueRepository.kt` | Reset attempts in retry() |

Ref: application_documentation/10-implementations/track-097-guru-guha-blog-source-adapter.md
