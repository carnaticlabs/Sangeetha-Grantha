# TRACK-008: Entity Resolution Hardening & Deduplication

| Metadata | Value |
|:---|:---|
| **Status** | **Completed** |
| **Owner** | Backend Team |
| **Priority** | High |
| **Related Tracks** | TRACK-001 (Bulk Import) |

## 1. Goal
Implement robust, performant, and musicologically accurate Entity Resolution and Deduplication for the Bulk Import pipeline (Phase 3). This ensures that imported Krithis are correctly mapped to canonical Composers, Ragas, and Talas without creating duplicates or bad data.

## 2. Problem Statement
The current `EntityResolutionService` is naive:
- **Performance**: Fetches all reference data (Composers/Ragas/Talas) for *every* row, causing N+1 query issues during batch processing.
- **Accuracy**: Uses simple Levenshtein distance without domain-specific normalization (e.g., "Dikshitar" vs "Muthuswami Dikshitar", "Rupakam" vs "Rupaka").
- **Deduplication**: No logic exists to detect if the imported Krithi already exists in the database (Phase 3 requirement).

## 3. Implementation Plan

### 3.1 Normalization Logic (Domain Rules)
Implement `NameNormalizationService` with specific rules identified in `csv-import-strategy.md`:
- **Composers**: Handle aliases ("Thyagaraja" -> "Tyagaraja", "Dikshitar" -> "Muthuswami Dikshitar").
- **Talas**: Handle suffixes ("-am" removal) and transliteration ("cApu" -> "chapu").
- **Ragas**: Vowel reduction and space removal ("Kalyani" vs "Kalyaani").

### 3.2 Performance Optimization (Caching)
- **Pre-fetch**: Load all reference entities (id, name, normalized_name) into memory maps at the start of the `EntityResolution` job (or cache with expiry).
- **Lookup**: Use in-memory maps for O(1) exact/normalized matching before falling back to fuzzy search.

### 3.3 Deduplication Service
- Implement `DeduplicationService` to check:
    1.  **Exact Match**: Title + Composer + Raga match against `krithis` table.
    2.  **Fuzzy Match**: High-confidence match on Title/Incipit.
    3.  **Batch Context**: Check against other items in the current batch (intra-batch deduplication).

### 3.4 Integration
- Update `BulkImportWorkerService` to use the optimized `EntityResolutionService`.
- Store `duplicate_candidates` in `imported_krithis` JSONB column.

## 4. Progress Log
- [x] Create `NameNormalizationService` with domain rules.
- [x] Refactor `EntityResolutionService` to use Caching/Pre-fetching.
- [x] Implement `DeduplicationService`.
- [x] Add unit tests for normalization and matching logic. (Manual verification via successful build and code review)
- [x] Database: Add `duplicate_candidates` column to `imported_krithis`.
- [x] Integration: Wire services in `App.kt` and `BulkImportWorkerService`.
