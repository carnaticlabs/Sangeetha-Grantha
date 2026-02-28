# TRACK-070: Import Pipeline Fix Checklist

**Date:** 2026-02-25
**Status:** Superseded by TRACK-071 (most items completed via new TOC-based approach)
**Scope:** End-to-end fix of the Dikshitar krithi import pipeline — from PDF extraction through PostgreSQL seeding to frontend display
**Context:** Original pipeline had multiple issues documented below. TRACK-071 replaced the broken extraction with a TOC-based approach that resolved most issues.

---

## Pipeline Overview (for reference)

```
mdeng.pdf / mdskt.pdf
    ↓ [REPLACED] extract_eng_pymupdf.py → extract_toc_based.py
    ↓ eng_krithis.json / skt_krithis.json
    ↓ [REPLACED] import_matched_csv.py → import_dikshitar_krithis.py (standalone)
PostgreSQL (krithis, krithi_sections, krithi_lyric_variants, krithi_lyric_sections)
    ↓ [FIXED] Admin Web /krithis table with search, pagination, filters
```

---

## PHASE 1 — Fix PDF Extraction (`extract_eng_pymupdf.py`)

### 1.1 — Upgrade PyMuPDF text extraction mode
- [x] Replace `page.get_text("markdown")` with `page.get_text("dict")` to get structured text with font metadata (done in `extract_toc_based.py`)
- [x] Use font style (italic flag) to detect section headers (`pallavi`, `anupallavi`, `caranam`, `madhyamakālasāhityam`)

### 1.2 — Implement deterministic section header detection (root cause of "Squashed Content Bug")
- [x] Built font-based classifier to detect section labels via italic flag and font name matching
- [x] When a section label is detected, close the previous section's text buffer and open a new one
- [x] Handle `madhyamakālasāhityam` as a distinct section key
- [x] Handle `noṭṭusvara sāhityam` (nottusvara compositions) mapped to pallavi
- [x] Handle `samaṣṭicaraṇam` (samashti charanam) as distinct section
- [x] Validated: krithi #1 pallavi, anupallavi, charanam, madhyamakala all correctly populated

### 1.3 — Strip embedded images from extracted text
- [x] N/A — `get_text("dict")` mode does not produce base64 images; Maltese cross artifacts stripped via regex

### 1.4 — Strip PDF page numbers and entry numbers from lyric content
- [x] Page numbers detected via font size (≤12.5pt standalone digits) and skipped
- [x] Footnote references detected via small font size (<8pt) and skipped

### 1.5 — Add extraction validation step
- [x] Stats output shows per-section coverage (484/484 pallavi, 279 anupallavi, etc.)
- [x] Empty section krithis tracked and reported (reduced from 42 → 0)

---

## PHASE 2 — Fix `eng_krithis.json` and `skt_krithis.json`

### 2.1 — Regenerate `eng_krithis.json` from fixed extractor
- [x] Re-run TOC-based extraction: 484 krithis, 484/484 with raga, tala, pallavi
- [x] Spot-checked first 5 krithis — sections correctly assigned

### 2.2 — Fix raga name normalisation in `eng_krithis.json`
- [x] `_clean_raga_name()` strips melakarta numbers `(28)`, fixes double-vowel OCR artifacts (`āarabhi` → `ārabhi`)

### 2.3 — Regenerate `skt_krithis.json` with Devanagari content
- [ ] **DEFERRED** — Velthuis-dvng TeX fonts have no ToUnicode CMap; all extracted text is garbled glyphs
- [ ] Requires dedicated glyph decoder (existing `velthuis_decoder.py` maps 155 glyphs but insufficient for full extraction)

### 2.4 — Ensure JSON section key names are canonical and consistently ordered
- [x] Canonical ordering enforced in `import_krithis.py`: pallavi → anupallavi → charanam → samashti_charanam → madhyamakala → chittaswaram → swara_sahitya

### 2.5 — Capture footnote / variant readings
- [ ] **DEFERRED** — not yet implemented

---

## PHASE 3 — Fix `import_matched_csv.py`

### 3.1 — Fix hardcoded primary language (`te`)
- [x] New `import_krithis.py` sets `primary_language = 'sa'` for all Dikshitar krithis

### 3.2 — Fix section ordering (canonical musical order, not dict.keys() order)
- [x] Canonical ordering via `SECTION_ORDER` dict in `import_krithis.py`

### 3.3 — Fix `madhyamakala` section type mapping
- [x] Maps to `MADHYAMA_KALA` DB enum correctly

### 3.4 — Fix Sanskrit section mapping
- [x] N/A for now — Sanskrit sections empty due to Velthuis font issue

### 3.5 — Strip base64 image data before inserting into DB
- [x] N/A — `get_text("dict")` mode doesn't produce base64 data

### 3.6 — Fix `en_lyrics_full` / `sa_lyrics_full` concatenation
- [x] Full lyrics built from canonically-ordered sections in `import_krithis.py`

### 3.7 — Fix raga name lookup to handle IAST diacritics
- [x] Raga names stripped and normalized before upsert

### 3.8 — Add tala name normalisation
- [x] Tala names cleaned and upserted in `import_krithis.py`

### 3.9 — Add AUDIT_LOG entries per krithi (project requirement)
- [x] Each krithi import writes to `audit_log` table with action='IMPORT'

### 3.10 — Fix logger.info placement
- [x] Progress logged every 50 krithis, not per-section

---

## PHASE 4 — Database Migration & Re-import

### 4.1 — Reset and clean the import
- [x] `mise exec -- cargo run --manifest-path tools/sangita-cli/Cargo.toml -- db reset` completed
- [x] Single seed krithi present

### 4.2 — Re-run fixed import script
- [x] 484 krithis imported, 0 skipped
- [x] Actual yields: 485 krithis (484 + 1 seed), 484 lyric variants, 1623 krithi sections, 216 ragas, 25 talas

### 4.3 — Post-import DB validation queries
- [x] `SELECT COUNT(*) FROM krithis WHERE primary_language = 'sa';` → 484 ✓
- [x] `SELECT COUNT(*) FROM krithis WHERE primary_language = 'te';` → 1 (seed) ✓
- [x] `SELECT COUNT(*) FROM krithi_sections WHERE section_type = 'PALLAVI';` → 484 ✓
- [x] All krithis have at least 1 section (0 with empty sections) ✓
- [x] `SELECT COUNT(*) FROM krithi_lyric_sections WHERE text LIKE '%data:image%';` → 0 ✓
- [x] 484 audit_log entries with action='IMPORT' ✓

### 4.4 — Spot-check 5 krithis manually
- [x] `akhilāṇḍeśvari rakṣa mām` — pallavi, anupallavi, charanam, madhyamakala all populated ✓
- [x] Ragamalika compositions (#205, #261, #407, #455) — all have content ✓
- [x] Nottusvara sāhityam compositions (#21, #45, etc.) — content mapped to pallavi ✓

---

## PHASE 5 — Frontend Bug Fixes

### 5.1 — Fix Raga column in Krithis listing table
- [x] Raga column now displays correctly (e.g., `jujāvanti`, `ārabhi`) via backend join on `krithi_ragas` + `ragas`

### 5.2 — Fix Language display in Krithis listing
- [x] Shows `SA` for all Dikshitar krithis (raw ISO code display; human-readable labels deferred)

### 5.3 — Verify Structure tab section order
- [ ] **NOT YET VERIFIED** — requires manual navigation to individual krithi pages

### 5.4 — Verify Lyrics tab content
- [ ] **NOT YET VERIFIED** — requires manual navigation to individual krithi pages

### 5.5 — Verify Lyric Variants tab (Devanagari)
- [ ] **DEFERRED** — Sanskrit lyric variants not imported

### 5.6 — Fix "Modi" badge on Raga field
- [ ] **NOT YET INVESTIGATED**

---

## PHASE 6 — Structural Schema Improvements

### 6.1 — Add `MADHYAMA_KALA` as a first-class section type
- [x] `MADHYAMA_KALA` enum value exists in DB schema ✓
- [x] 419 madhyamakala sections imported ✓

### 6.2 — Model Lyric Variant Readings
- [ ] **DEFERRED**

### 6.3 — Add Incipit field population
- [x] Incipit auto-populated from first ~120 chars of pallavi during import ✓

### 6.4 — Add Deity and Temple canonical links
- [ ] **DEFERRED**

---

## PHASE 7 — Pipeline Hardening & Tests

### 7.1–7.4 — Unit/integration tests
- [ ] **DEFERRED** — extraction and import scripts work correctly but lack automated test coverage

---

## Additional Work Done (not in original checklist)

### Search Functionality Fix
- [x] `title_normalized` and `incipit_normalized` populated with ASCII-stripped values using `strip_diacritics()` SQL function
- [x] Searching "sri" now matches `śrī` titles (148 results) ✓

### Pagination
- [x] Frontend `searchKrithis()` API client updated to pass `page` and `pageSize` params
- [x] Backend already supported pagination (default 50/page, max 200)
- [x] UI shows 25 items/page with Previous/Next controls and page number buttons

### Filter Panel
- [x] Filter button toggles dropdown panel with Raga, Composer, Language dropdowns
- [x] Reference data loaded from `/v1/ragas` and `/v1/composers` endpoints
- [x] Active filter count shown as badge on Filter button
- [x] "Clear all filters" link when filters active
- [x] Filters combine with text search and reset pagination

### CORS / Auth Fix
- [x] `VITE_API_BASE_URL` changed from `http://localhost:8080` to `/v1` (uses Vite proxy)
- [x] `FRONTEND_PORT=5001` added to `config/local.env` for correct CORS origin

---

*Ref: application_documentation/04-database/schema.md · database/for_import/ · TRACK-071*
