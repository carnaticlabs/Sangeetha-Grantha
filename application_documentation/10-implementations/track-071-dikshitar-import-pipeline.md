| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-02-28 |
| **Author** | Sangeetha Grantha Team |

# Dikshitar Krithi Import Pipeline (TRACK-071)

---

## 1. Overview

A standalone pipeline for extracting and importing 484 Muthuswami Dikshitar krithis from the guruguha.org authoritative PDFs (`mdeng.pdf` and `mdskt.pdf`) into the Sangeetha Grantha database.

This pipeline replaces the earlier Sarvam API-based extraction (TRACK-066/067) and broken PyMuPDF markdown extraction (TRACK-068/069) with a TOC-based page-range approach.

**Tracks:** TRACK-066 (deprecated), TRACK-067 (deprecated), TRACK-068 (deprecated), TRACK-069 (deprecated), [TRACK-070](../../../conductor/tracks/TRACK-070-import-pipeline-fix-checklist.md) (superseded), [TRACK-071](../../../conductor/tracks/TRACK-071-toc-based-krithi-import.md) (primary)

---

## 2. Pipeline Architecture

```text
mdeng.pdf / mdskt.pdf (guruguha.org PDFs)
    |
    v
[1] extract_toc_based.py
    - Parse TOC (pages 2-16) for 484 krithi entries
    - Use page ranges for per-krithi extraction
    - Font metadata (italic flag) for section detection
    - Diacritic normalisation for Utopia font garbling
    |
    v
eng_krithis.json / skt_krithis.json / krithi_comparison_matched.csv
    |
    v
[2] import_dikshitar_krithis.py
    - Standalone psycopg script (no worker module deps)
    - Inserts into: krithis, krithi_sections, krithi_ragas,
      krithi_lyric_variants, krithi_lyric_sections, audit_log
    |
    v
PostgreSQL (484 krithis, ~1623 sections, 216 ragas, 25 talas)
    |
    v
[3] Admin Web KrithiList
    - Search with strip_diacritics() normalisation
    - Filter panel (Raga, Composer, Language)
    - Server-side pagination (25/page)
```

---

## 3. Extraction Script: `extract_toc_based.py`

**Location:** `database/for_import/extract_toc_based.py`

**Approach:**
1. Parse the Table of Contents from the PDF using `fitz.open()` (`PyMuPDF`)
2. Extract (number, title, start_page) tuples for all 484 krithis
3. For each krithi, extract text from its page range using `get_text("dict")` for font-aware extraction
4. Detect section headers via italic font flag and font name matching
5. Apply diacritic normalisation rules for Utopia font garbling (macrons, dot-above, acute, tilde, consonant-dots)

**Outputs:**
- `eng_krithis.json` — 484 English/IAST krithis with sections
- `skt_krithis.json` — 484 Sanskrit krithis (sections partially empty due to Velthuis font limitation)
- `krithi_comparison_matched.csv` — Cross-referenced CSV with metadata

**Known Limitation:** Sanskrit PDF (`mdskt.pdf`) uses Velthuis-encoded TeX fonts that PyMuPDF cannot fully decode. Sanskrit lyric sections are partially empty as a result.

---

## 4. Import Script: `import_dikshitar_krithis.py`

**Location:** `tools/krithi-extract-enrich-worker/src/import_dikshitar_krithis.py`

**Data quality fixes applied (TRACK-070 Phase 3):**

| Fix | Description |
|:---|:---|
| 3.1 | `primary_language` detection — `sa` for Dikshitar (was hardcoded `te`) |
| 3.2 | Canonical section ordering (Pallavi → Anupallavi → Charanam → ...) |
| 3.4 | Sanskrit section fuzzy key matching via normalised lookup |
| 3.5 | `clean_lyric_text()` removes base64 images, Devanagari numbers, blank lines |
| 3.6 | `en_lyrics_full` / `sa_lyrics_full` built in canonical order |
| 3.7 | Raga name normalisation: strip melakarta suffixes, fix double-vowel OCR artefacts |
| 3.8 | Tala name normalised to lowercase |
| 3.9 | `AUDIT_LOG` INSERT per krithi (project requirement) |
| 3.10 | Logger call moved outside inner section loop |

**Database writes per krithi:**
1. `krithis` — main record with `title_normalized`
2. `krithi_ragas` — junction table entry for search API
3. `krithi_sections` — one row per section in canonical order
4. `krithi_lyric_variants` — English (primary) + Sanskrit variants
5. `krithi_lyric_sections` — per-section lyric text for each variant
6. `audit_log` — IMPORT action with metadata JSON

---

## 5. Search Normalisation

### `strip_diacritics()` SQL Function

Enables ASCII search of IAST content. Populates `title_normalized` and `incipit_normalized` columns.

Example: searching "akhilandesvari" matches "akhilāṇḍeśvaryai".

See [Schema Overview — SQL Functions](../04-database/schema.md#12-sql-functions) for mapping details.

---

## 6. Frontend Changes

### KrithiList Rewrite

- **Debounced search** (300ms) with `strip_diacritics()`-backed normalisation
- **Filter panel**: Raga, Composer, Language dropdowns populated from reference data APIs
- **Server-side pagination**: 25 items/page with ellipsis-style navigator
- **Raga display**: Array of chip badges (supports ragamalika)

### API Client Update

`searchKrithis()` now accepts: `query`, `ragaId`, `composerId`, `language`, `page`, `pageSize`.

See [Frontend UI Specs — KrithiList](../05-frontend/admin-web/ui-specs.md#81-krithi-list-search-filter-pagination) for full specification.

---

## 7. Import Results

| Metric | Value |
|:---|:---|
| Total krithis imported | 484 |
| Total sections created | ~1,623 |
| Ragas created/resolved | 216 |
| Talas created/resolved | 25 |
| Audit log entries | 484 |
| Base64 image rows | 0 (cleaned) |
| Composer | Muthuswami Dikshitar |
| Primary language | `sa` (Sanskrit) |

---

## 8. File Inventory

| File | Purpose |
|:---|:---|
| `database/for_import/extract_toc_based.py` | TOC-based PDF extraction script |
| `tools/krithi-extract-enrich-worker/src/import_dikshitar_krithis.py` | Database import script |
| `database/for_import/eng_krithis.json` | Generated English/IAST krithi data |
| `database/for_import/skt_krithis.json` | Generated Sanskrit krithi data |
| `database/for_import/krithi_comparison_matched.csv` | Cross-referenced metadata CSV |
| `modules/frontend/sangita-admin-web/src/pages/KrithiList.tsx` | Rewritten listing page |
| `modules/frontend/sangita-admin-web/src/api/client.ts` | Updated API client |

---

## 9. Related Documentation

- [TRACK-071 Conductor Track](../../../conductor/tracks/TRACK-071-toc-based-krithi-import.md)
- [Database Schema Overview](../04-database/schema.md)
- [Frontend UI Specs](../05-frontend/admin-web/ui-specs.md)
- [Krithi Data Sourcing Quality Strategy](../01-requirements/krithi-data-sourcing/quality-strategy.md)
