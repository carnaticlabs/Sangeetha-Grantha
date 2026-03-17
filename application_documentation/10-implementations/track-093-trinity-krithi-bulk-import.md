| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.2.0 |
| **Last Updated** | 2026-03-17 |
| **Author** | Sangeetha Grantha Team |

# Trinity Krithi Bulk Import

## Purpose

Import 1,245 krithis for the three Carnatic Trinity composers (Muthuswami Dikshitar: 482, Syama Sastri: 71, Tyagaraja: 692) via the bulk import UI, with supporting raga seed migrations and scraping pipeline fixes.

## Implementation Details

### CSV Cleanup
- Removed artifact rows where Raga column contained "Part 1-5" or repeated header rows
- Primarily affected the Thyagaraja CSV

### Raga Gap Seeding (Migrations 39–40)
- 962 ragas already in DB from TRACK-091/092
- 164 CSV raga names didn't exact-match after normalization; 27 LOW-confidence matches needed seeding
- Migration 39: `39__seed_malavasri_raga.sql` — Mālāvashree raga
- Migration 40: `40__seed_missing_trinity_ragas.sql` — 9 base ragas (Kalyāni, Todi, Jujāvanti, Gaula, Nāta, Bauli, Pūrvi, Gauri, Brindāvana Sāranga)

### Scraping Pipeline Fix (v1.1.0)
Kotlin-side HTML scraping produced corrupted lyrics (Devanagari pronunciation guides, Word Division duplication, multiple language versions). Fixed by delegating entirely to Python extraction worker:
- `ScrapeWorker` now passes only CSV metadata, leaving `rawLyrics`/`rawPayload` null
- Triggers `ImportService.shouldEnqueueHtmlExtraction()` → `extraction_queue` → Python worker
- `StructureParser` enhanced with Devanagari pronunciation guide boilerplate detection

### Raga Transliteration Handling (v1.2.0)
- Normalization pipeline: diacritic removal, vowel length collapse, aspirate collapse, space removal
- RapidFuzz WRatio fuzzy matching with confidence levels (HIGH 90+, MEDIUM 70-89, LOW 60-69)

## Code Changes

| File | Change |
|------|--------|
| `database/for_import/*.csv` | Cleaned CSV source files |
| `database/migrations/39__seed_malavasri_raga.sql` | Mālāvashree raga seed |
| `database/migrations/40__seed_missing_trinity_ragas.sql` | 9 base raga seeds |
| `modules/backend/api/.../bulkimport/workers/ScrapeWorker.kt` | Delegate scraping to Python worker |
| `tools/krithi-extract-enrich-worker/src/structure_parser.py` | Boilerplate detection |

## Results

- Three-batch import strategy: Syama Sastri → Dikshitar → Thyagaraja
- All transliteration variants resolve correctly via fuzzy matching
- Clean lyrics produced through Python extraction pipeline

Ref: application_documentation/10-implementations/track-093-trinity-krithi-bulk-import.md
