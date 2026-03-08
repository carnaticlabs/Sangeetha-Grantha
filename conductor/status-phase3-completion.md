# Phase 3 Status Report — E2E Pipeline Validation

| Metadata | Value |
|:---|:---|
| **Date** | 2026-03-08 |
| **Phase** | 3 — Prove pipeline E2E with real data |
| **Handoff** | `conductor/handoff-simplify-and-ship.md` |
| **Status** | **Substantially complete — critical bug fixed, data quality issues identified** |

---

## Executive Summary

Phase 3 E2E validation is functionally complete. The CSV bulk import + PDF extraction pipeline runs end-to-end. A critical auto-creation bug was discovered and fixed: unmatched extractions now route to `imported_krithis` as PENDING for curator review instead of silently creating unverified krithis. Two systemic data quality issues were discovered that must be addressed before Phase 5 curation.

---

## Pipeline Results

### Krithis & Imports

| Metric | Count | Notes |
|:---|---:|:---|
| Krithis in DB | **474** | All from CSV bulk import |
| CSV imports approved | 477 | 474 krithis after deduplication |
| CSV imports stuck (PENDING) | 6 | Entity resolution phase never ran (see Known Issues #2) |
| CSV imports rejected | 1 | — |
| Unmatched extractions (PENDING) | 172 | From PDF extraction — awaiting curator review |

### Source Evidence

| Metric | Count |
|:---|---:|
| Total evidence records | 782 |
| Krithis with evidence | 473 (99.8%) |
| Krithis with 1 source | 166 |
| Krithis with 2 sources | 305 |
| Krithis with 3+ sources | 2 |

### Extraction Pipeline

| Metric | Count | Notes |
|:---|---:|:---|
| PDF extractions (mdeng.pdf) | 485 | All ingested successfully |
| Matched to existing krithis | ~313 | Created source evidence records |
| Unmatched → pending review | 172 | Routed to `imported_krithis` (bug fix) |
| Auto-created krithis | **0** | Was 141 before fix |
| Extraction errors | 0 | — |

### Lyric Variants

| Language | Script | Variants | Notes |
|:---|:---|---:|:---|
| English | Latin | 473 | Primary IAST transliteration |
| Sanskrit | Devanagari | 473 | — |
| Tamil | Tamil | 473 | — |
| Telugu | Telugu | 473 | — |
| Kannada | Kannada | 473 | — |
| Malayalam | Malayalam | 473 | — |

Each krithi has exactly 6 lyric variants (one per language/script). All variants have the `lyrics` column populated with full text.

---

## Critical Bug Fixed: Auto-Creation of Unmatched Krithis

### Problem

`KrithiMatcherService.processExtractionResult()` auto-created a new `krithis` record whenever no fuzzy match was found. In the mdeng.pdf run, this created 141 unverified krithis with no curator oversight — titles, ragas, and talas were accepted as-is from OCR/extraction output without human review.

### Fix

Replaced `createNewKrithi()` with `createPendingImport()` which routes unmatched extractions to the `imported_krithis` table with status `PENDING` under a dedicated import source ("PDF Extraction (Unmatched)"). These await manual curator review before becoming krithis.

### Files Changed

| File | Change |
|:---|:---|
| `KrithiMatcherService.kt` | Replaced `createNewKrithi()` with `createPendingImport()`, removed `krithiCreationService` dependency |
| `AppModule.kt` | Removed `krithiCreationService` from DI for `KrithiMatcherService` |
| `SourceRegistryRepository.kt` | Added `findByName()` method |
| `01_reference_data.sql` | Added "PDF Extraction (Unmatched)" import source |
| `ExtractionResultProcessorUnitTest.kt` | Removed `krithiCreationService` from constructor |
| `ExtractionResultProcessorTest.kt` | Updated to verify unmatched → pending import flow |

### Verification

- All 111 backend tests pass
- E2E re-run: `make db-reset && make seed` → CSV import (484 rows) → approve all → mdeng.pdf extraction → **0 auto-created krithis**, 172 routed to pending review

---

## Known Issues Discovered

### Issue 1: Lyric Section Inconsistency Across Variants (CRITICAL for Phase 5)

**435 out of 473 krithis** (92%) have inconsistent `krithi_lyric_sections` counts across their 6 language variants for the same krithi.

**Example — `abhayAmbA nAyaka vara dAyaka`:**

The [source page](http://guru-guha.blogspot.com/2007/07/dikshitar-kriti-abhayaambaa-naayaka.html) has 3 structural sections (Pallavi, Anupallavi, Charanam) with Madhyama Kala Sahitya embedded within sections.

| Language | Sections Stored | Issue |
|:---|---:|:---|
| English (en) | 3 | Pallavi + Anupallavi + Madhyama Kala |
| Sanskrit (sa) | 3 | Pallavi + Anupallavi + Madhyama Kala (Charanam text merged into MK) |
| Tamil (ta) | 3 | Same as Sanskrit |
| Kannada (kn) | 4 | Pallavi + Anupallavi + Madhyama Kala + Charanam |
| Telugu (te) | 2 | Pallavi + Anupallavi (Charanam text merged into Anupallavi) |
| Malayalam (ml) | 2 | Same as Telugu |

**Worst case — `kAyArOhaNESam`:** ranges from 2 sections (te, ml) to 7 sections (kn). The Kannada variant stores both the continuous-text and word-division presentations as separate sections, duplicating the Pallavi and Anupallavi.

**Root Cause:** The blogspot source provides two presentation formats per script — continuous text and word-division variant. The `DeterministicWebScraper` (stub mode) parses section labels differently per script, and some scripts merge the Charanam text into the Anupallavi or Madhyama Kala section when the `[CHARANAM]` label is embedded inline. The dual-format (continuous + word-division) is sometimes treated as separate variants (Kannada) and sometimes merged (other scripts).

**Impact:** Curator review in Phase 5 will find structurally inconsistent data. Users browsing by section (e.g. "show me the Charanam") will get different results depending on the language variant selected.

**40 krithis** have at least one language variant with **zero sections** — the full lyrics exist in the `lyrics` column but the section parser failed completely.

### Issue 2: Bulk Import Entity Resolution Phase Never Ran

The batch pipeline has 3 phases: manifest_ingest → scrape → entity_resolution. The entity_resolution job was created but has **0 task runs**. This left 6 CSV imports stuck in PENDING with `raw_title = URL` and no metadata (composer, raga, tala are null).

**Root Cause:** `BulkImportTaskRepository.createTasks()` uses an idempotency key of `batchId::sourceUrl` that is **not scoped to job type**. When the resolution phase tries to create task runs with the same URLs that the scrape phase already used, the dedup filter removes all 482 as "already existing." The resolution job is created empty and the dispatcher polls forever finding nothing.

**Impact:** 6 krithis not imported. Batch stuck in `RUNNING` state indefinitely.

**Affected krithis:**
- `nAga lingaM namAmi` (naagalingam)
- `madhurAmbA jayati` (madhuramba jayati)
- `madhrAmbhA samrakshatu` (madhrambha samrakshatu)
- `mAdhavO mAmpATu` (madhavo mampatu)
- `madhurAmbAyAstava` (madhurambayastava)
- `SrI ganESAtparaM` (sri ganesatparam)

### Issue 3: PDF Extraction via Docker Requires HTTP-Accessible Files

The extraction worker uses `httpx` to download source files — `file://` and bare filesystem paths fail with "UnsupportedProtocol." PDFs must be served over HTTP for the Docker-based worker to access them. This was worked around by running a local `python3 -m http.server` but needs a proper solution (e.g. mount a volume and add a file-path handler in the worker, or use a shared storage volume with an nginx sidecar).

---

## Phase 3 Checklist (from handoff)

| Task | Status | Notes |
|:---|:---|:---|
| Run `db reset`, seed reference data | Done | `make db-reset && make seed` |
| Import 484 Dikshitar krithis via CSV | Done | 477 approved, 474 krithis after dedup |
| Submit `mdeng.pdf` to extraction queue | Done | 485 extractions, all ingested |
| Verify Kotlin processes results | Done | 313 matched, 172 pending review, 0 errors |
| Submit `mdskt.pdf`, verify variant matching | **Not done** | Blocked by Issue #3 (file protocol); also requires Phase 1 normalizer for Sanskrit matching |
| Debug and fix pipeline issues | Done | Auto-creation bug fixed; two more issues identified |
| **Checkpoint: >=400 krithis with evidence** | **Met** | **473 krithis with source evidence** |

---

## Recommendations for Next Phases

### Before Phase 4 (Curator Review UI)

1. **Fix idempotency key scoping** in `BulkImportTaskRepository.createTasks()` — include `jobId` in the key to unblock entity resolution. This will fix the 6 stuck imports and the permanently-running batch.

2. The curator review UI should surface the 172 unmatched PDF extractions from `imported_krithis` where `import_source = 'PDF Extraction (Unmatched)'` alongside the normal import review queue.

### Before Phase 5 (Curate to 500+)

3. **Fix lyric section parsing** — this is the highest-impact data quality issue. The `DeterministicWebScraper` / `LyricVariantPersistenceService` must produce consistent section counts across all 6 language variants for the same krithi. The source blogspot pages have a clear structure (Pallavi → Anupallavi → Charanam with embedded Madhyama Kala Sahitya). The parser should:
   - Treat Madhyama Kala Sahitya as a sub-section within its parent, not a top-level section
   - Not duplicate sections when both continuous-text and word-division formats are present
   - Produce identical section types and counts across all script variants of the same krithi

4. **Submit `mdskt.pdf`** for Sanskrit variant matching — requires serving the PDF over HTTP and may require the consolidated Python normalizer from Phase 1.

---

## Infrastructure Changes (from this session)

| File | Change |
|:---|:---|
| `compose.yaml` | Dev backend uses source mount + `gradlew run` instead of fat JAR build; extraction uses `DATABASE_URL`; added `extraction-cache` volume; frontend uses `API_PROXY_TARGET` |
| `vite.config.ts` | Proxy target from env var for Docker networking |
| `worker.py` | Added rollback after DB errors to prevent cascading "current transaction is aborted" |
| `.claude/launch.json` | Updated to use `make dev` |
