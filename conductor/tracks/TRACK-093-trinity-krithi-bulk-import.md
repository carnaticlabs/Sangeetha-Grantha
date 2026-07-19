| Metadata | Value |
|:---|:---|
| **Status** | Completed |
| **Version** | 1.3.0 |
| **Last Updated** | 2026-07-19 |
| **Author** | Sangeetha Grantha Team |

> **2026-07-19 — completed.** The re-import held below has since run. Verified against the dev DB:
>
> | Check | Result |
> |:---|:---|
> | Krithis imported | **1,226** of the 1,245 target (98.5%) — Tyagaraja 675, Dikshitar 481, Syama Sastri 70 |
> | Krithis with canonical sections | 1,225 of 1,226 |
> | Krithis with a raga link | 1,226 (zero unresolved) |
> | `krithi_revisions` | 1,292 rows across 1,225 krithis (versioned canon populated from row one) |
> | `krithi_section_revisions` | 15,651 rows |
> | `parsed_payload` format | 1,238 of 1,238 canonical — **zero legacy `ScrapedKrithiMetadata`** |
>
> The last row is TRACK-096's explicit unblock condition; that track is now clear to run its
> deletion cleanup. Residual data quality is tracked separately, not here:
> **29 krithis (2.4%) with section-count mismatches** → [TRACK-133](./TRACK-133-section-mismatch-remediation.md);
> **16 duplicate raga pairs** created by an import-side normalisation gap → [TRACK-132](./TRACK-132-raga-deduplication-normalizer-fix.md).

<details>
<summary>2026-07-11 — held by decision (superseded)</summary>

> The prerequisites for a *clean* re-import are now in place:
> the versioned-canon schema + write path (TRACK-117), the auto-approval revision attribution
> (TRACK-117 — approvals now record provenance from the extraction), and the structural-voting
> migration off the legacy scraper DTO (TRACK-096). The re-import itself (`make db-reset` → three
> bulk-import batches, ~1,245 krithis re-scraped from guru-guha.blogspot.com over hours) was
> **deliberately deferred** to a supervised session — it drops the dev DB and is a large
> outward-facing scrape. When it runs, it will populate `krithi_revisions` + per-section provenance
> from row one, and unblock TRACK-096's final cleanup (deleting `ScrapedKrithiMetadata` + the
> dual-format fallback once no legacy payloads remain).

</details>

# Goal

Import 1,245 krithis for the Carnatic Trinity composers via the bulk import UI:
- **Muthuswami Dikshitar**: 482 krithis
- **Syama Sastri**: 71 krithis
- **Tyagaraja**: 692 krithis

Source CSVs are in `database/for_import/`. Each CSV has columns: Krithi, Raga, Hyperlink.

# Pre-Import Steps

## CSV Cleanup
- Remove artifact rows where Raga column = "Part 1-5" or "Raga" (repeated header)
- Files affected: primarily Thyagaraja CSV

## Raga Gap Seeding (Migrations 39-40)
- 962 ragas mastered in TRACK-091/092
- 164 CSV raga names don't exact-match DB ragas after normalization
- 27 LOW-confidence matches (<70% Levenshtein) — need migration to seed
- ✅ **Migration 39**: `39__seed_malavasri_raga.sql` (Mālāvashree)
- ✅ **Migration 40**: `40__seed_missing_trinity_ragas.sql` (9 base ragas)
  - Kalyāni (melakarta #65)
  - Todi (janya of Hanumatodi #8)
  - Jujāvanti (janya of Harikāmbhōji #28)
  - Gaula (janya of Māyāmāḻavagouḻai #15)
  - Nāta (janya of Dhīraśankarābharaṇam #29)
  - Bauli (janya of Māyāmāḻavagouḻai #15)
  - Pūrvi (janya of Panthuvarāli #51)
  - Gauri (melakarta #23)
  - Brindāvana Sāranga (janya of Kharaharapriyā #22)

# Import Strategy

Three separate batches via `http://localhost:5001/bulk-import`:
1. Syama Sastri (smallest, validates flow)
2. Dikshitar
3. Thyagaraja

# Scraping Pipeline Fix (v1.1.0 — 2026-03-12)

## Problem
Kotlin-side HTML scraping in ScrapeWorker produced lyrics with:
- Devanagari pronunciation guide lines leaking into lyrics
- Word Division sections duplicating lyrics with word splits
- Multiple language versions instead of first-language-only

## Solution: Delegate to Python Extraction Worker
ScrapeWorker now passes only CSV metadata (rawTitle, rawRaga) and leaves rawLyrics/rawPayload null. This triggers `ImportService.shouldEnqueueHtmlExtraction()` → `extraction_queue` → Python worker.

### Files Changed
- `modules/backend/api/.../bulkimport/workers/ScrapeWorker.kt` — removed Kotlin scraping, delegate to Python
- `tools/krithi-extract-enrich-worker/src/structure_parser.py` — added Devanagari pronunciation guide boilerplate detection

### Pipeline Flow (After Fix)
```
CSV Upload → ScrapeWorker (CSV metadata only)
  → ImportService.submitImports() (no rawLyrics/rawPayload)
  → shouldEnqueueHtmlExtraction() = true
  → extraction_queue table
  → Python extraction worker
    → HtmlTextExtractor (fetch + parse HTML)
    → StructureParser._find_metadata_boundaries() (truncate at Word Division)
    → StructureParser._extract_sections() (first language only)
    → StructureParser._is_boilerplate() (filter pronunciation guides)
```

### Key Mechanism
`ImportService.shouldEnqueueHtmlExtraction()` (line ~117-122 of ImportService.kt) returns `true` when:
- `rawPayload == null` AND `rawLyrics.isNullOrBlank()` AND source format is HTML

# Post-Import

- Review resolution success rates
- Export unresolved ragas to CSV for later analysis
- Review failed scrape tasks (dead blog URLs)

# Raga Transliteration Handling (v1.2.0 — 2026-03-13)

## How It Works
The extraction worker handles transliteration variations automatically:

### Normalization Pipeline (`normalizer.py:normalize_for_matching()`)
- **Diacritic removal**: `Kalyāni` → `Kalyani`
- **Vowel length collapse**: `Kalyaani` → `Kalyani`
- **Aspirate collapse**: `Thodi` → `Todi`
- **Space removal**: `Brindāvana Sāranga` → `BrindavanaSaranga`
- **Case normalization**: All → lowercase

### Identity Discovery (`identity_candidates.py`)
- Uses **RapidFuzz WRatio** for fuzzy matching
- Scores 0-100 (100 = exact match)
- Confidence levels: HIGH (90+), MEDIUM (70-89), LOW (60-69)
- Handles minor spelling variations automatically

### Test Results
All transliteration variants now resolve correctly:
```
Kalyani, Kalyāni, Kalyaani      → kalyani   ✓
Todi, Thodi, Tōdi               → todi      ✓
Gaula, Gowla                    → both found ✓
Bauli, Bowli                    → both found ✓
Brindāvana Sāranga              → brindavanasaranga ✓
```

## Benefits
- **No manual intervention** needed for common transliteration variants
- **High confidence matches** (90-100%) for exact normalized forms
- **Graceful degradation** for missing ragas (extraction succeeds, resolution deferred)

# Remaining Steps

1. Restart backend to pick up Kotlin changes
2. Delete old Syama Sastri and Thyagaraja import batches from DB
3. Re-upload both CSVs (now flows through Python extraction)
4. Verify extracted lyrics are clean (no Word Division, no pronunciation guides)
5. Run backend tests (`make test`)
6. Optional cleanup: remove unused `webScrapingService`/`rateLimiter` from ScrapeWorker constructor

# Files Modified

- `database/for_import/*.csv` — cleaned
- `database/migrations/39__seed_malavasri_raga.sql` — Mālāvashree raga
- `database/migrations/40__seed_missing_trinity_ragas.sql` — 9 base ragas (v1.2.0)
- `database/migrations/41__unique_import_source_name.sql` — Fixed concurrent import source creation
- `modules/backend/api/.../bulkimport/workers/ScrapeWorker.kt` — delegate to Python
- `tools/krithi-extract-enrich-worker/src/structure_parser.py` — boilerplate detection
- `conductor/tracks.md` — updated
