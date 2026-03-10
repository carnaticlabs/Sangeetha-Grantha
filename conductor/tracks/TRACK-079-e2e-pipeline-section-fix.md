| Metadata | Value |
|:---|:---|
| **Track ID** | TRACK-079 |
| **Title** | E2E Pipeline Validation & Lyric Section Consistency Fix |
| **Status** | Completed |
| **Created** | 2026-03-08 |
| **Author** | Sangeetha Grantha Team |
| **Depends On** | TRACK-071 (TOC-Based Dikshitar Import), TRACK-064 (UEE) |

# TRACK-079: E2E Pipeline Validation & Lyric Section Consistency Fix

## Objective

Prove the CSV bulk import + PDF extraction pipeline end-to-end with real data, and fix the critical lyric section inconsistency affecting 92% of krithis.

## Problem Statement

### Phase 3: E2E Validation
The pipeline needed validation with real Dikshitar compositions. During validation, a critical bug was found: `KrithiMatcherService.processExtractionResult()` auto-created unverified krithis when no fuzzy match was found, bypassing curator review.

### Phase 4A: Section Inconsistency
435 out of 473 krithis (92%) had inconsistent `krithi_lyric_sections` counts across their 6 language variants. 40 krithis had at least one variant with zero sections. Root cause: the Python `structure_parser.py` parsed section labels differently per script and mishandled dual-format (continuous + word-division) text.

## Scope

### E2E Pipeline (Phase 3)
- Docker Compose orchestration for full dev stack (DB, backend, frontend, extraction)
- Fix auto-creation bug: route unmatched extractions to `imported_krithis` as PENDING
- Extraction worker rollback fix for cascading DB errors
- Vite proxy configuration for Docker networking

### Section Fix (Phase 4A)
- `structure_parser.py`: MKS demotion from top-level to sub-section, dual-format merge, bracket header detection, Indic anusvara/explicit-m forms
- `LyricVariantPersistenceService.kt`: type+queue matching instead of index-based, MKS filtered from canonical sections
- `KrithiLyricRepository.kt`: ORDER BY order_index via JOIN for correct section ordering
- Migration 38: remove MKS top-level sections, deduplicate, re-index
- Repair scripts: fill 34 zero-section krithis, fix 3 inconsistent

## Verification

- 474 krithis with source evidence (target: >=400)
- 0 auto-created krithis from unmatched extractions
- 172 unmatched extractions routed to pending review
- Section consistency: 0 krithis with mismatched section counts (down from 435)
- 111 backend tests pass
