# TRACK-071: TOC-Based Dikshitar Krithi Import

| Metadata | Value |
|:---|:---|
| **Status** | Completed |
| **Created** | 2026-02-27 |
| **Updated** | 2026-02-28 |
| **Author** | Claude Code |
| **Ref** | TRACK-066, TRACK-067, TRACK-068, TRACK-069, TRACK-070 |

## Summary

Complete reimplementation of the Dikshitar Krithi extraction and import pipeline using TOC-based page-range extraction from the authoritative guruguha.org PDFs (`mdeng.pdf` and `mdskt.pdf`). Also includes search normalization, pagination, and filter panel for the admin Kritis listing page.

## Context

Previous tracks (066-070) documented multiple failed attempts at extracting krithis from the PDFs. The core issues were:
- Sarvam API markdown output had quality issues
- PyMuPDF raw text extraction produced garbled IAST diacritics
- Section detection was unreliable without font metadata
- Raga/tala parsing missed many variants

## Approach

1. **TOC-based extraction** — Parse the Table of Contents (pages 2-16) to get (number, title, page) for all 484 krithis, then use page ranges for extraction
2. **Font metadata for section detection** — Use `get_text("dict")` to identify italic spans as section headers
3. **Diacritic normalization** — Rule-based fixing of Utopia font garbling (macrons, dot-above, acute, tilde, consonant-dots)
4. **Standalone import script** (`import_dikshitar_krithis.py`) — Direct psycopg insertion without worker module dependencies
5. **Search normalization** — `strip_diacritics()` SQL function to enable ASCII search of IAST content
6. **Frontend enhancements** — Pagination, filter panel, search API improvements

## Files Created/Modified

### Extraction & Import
- `database/for_import/extract_toc_based.py` — New TOC-based extraction script
- `database/for_import/import_krithis.py` — Standalone import script
- `database/for_import/eng_krithis.json` — 484 English/IAST krithis (generated)
- `database/for_import/skt_krithis.json` — 484 Sanskrit krithis (generated, sections empty due to Velthuis font)
- `database/for_import/krithi_comparison_matched.csv` — Matched CSV (generated)

### Frontend
- `modules/frontend/sangita-admin-web/src/pages/KrithiList.tsx` — Rewritten with pagination, filter panel, updated search API
- `modules/frontend/sangita-admin-web/src/api/client.ts` — `searchKrithis()` updated to accept pagination + filter params

### Configuration
- `config/local.env` — `VITE_API_BASE_URL=/v1` (uses Vite proxy), `FRONTEND_PORT=5001` (CORS fix)
- `.claude/launch.json` — Added `frontend-external` and `backend-external` configs for preview

## Import Results

| Metric | Value |
|:---|:---|
| Total krithis imported | 484 |
| With raga | 484/484 |
| With tala | 484/484 |
| With pallavi | 484/484 |
| With anupallavi | 279/484 |
| With charanam | 279/484 |
| With madhyamakala | 419/484 |
| With samashti_charanam | 162/484 |
| Unique ragas | 216 |
| Unique talas | 25 |
| English lyric variants | 484 |
| Audit log entries | 484 |

## Search & Frontend Results

| Feature | Status |
|:---|:---|
| Text search (title, raga, composer, incipit) | Working — "sri" returns 148 results matching `śrī` |
| `title_normalized` / `incipit_normalized` | Populated with ASCII-stripped values via `strip_diacritics()` |
| Pagination | 25 items/page, Previous/Next, page number buttons with ellipsis |
| Filter: Raga dropdown | 216 ragas loaded from `/v1/ragas`, filters by `ragaId` |
| Filter: Composer dropdown | Composers loaded from `/v1/composers`, filters by `composerId` |
| Filter: Language dropdown | SA, TE, TA, KN options, filters by `primaryLanguage` |
| Filter badge | Shows active filter count on Filter button |
| Clear all filters | Link appears when any filter is active |
| Search + filter combination | Works correctly, resets to page 1 on change |

## Key Technical Decisions

1. **Nottusvara sāhityam** mapped to "pallavi" section type — these are a distinct musical form without standard pallavi/anupallavi/charanam structure
2. **Ragamalika** compositions — detected via rāgamālikā keyword, content defaults to "pallavi" when no standard section headers exist
3. **Sanskrit extraction deferred** — Velthuis-dvng TeX fonts have no ToUnicode CMap, rendering all extracted text as garbled glyphs
4. **Diacritic search normalization** — `strip_diacritics()` SQL function translates IAST chars to ASCII in `title_normalized` / `incipit_normalized`, enabling plain-text search
5. **CORS fix** — Changed `VITE_API_BASE_URL` from direct `http://localhost:8080` to `/v1` (uses Vite proxy); added `FRONTEND_PORT=5001` for backend CORS allowlist

## Known Limitations

- Sanskrit lyric variants not imported (Velthuis font decoding unsolved)
- Some IAST diacritics may have residual artifacts from space-collapse rules
- Ragamalika compositions have all content in "pallavi" rather than per-raga sections
- Raga `name_normalized` not yet ASCII-stripped (unique constraint conflicts with duplicate ragas)
- Language column shows raw ISO code (SA) not human-readable label

## Validation Queries

```sql
-- Total counts
SELECT COUNT(*) FROM krithis WHERE primary_language = 'sa';  -- 484

-- All have sections
SELECT COUNT(*) FROM krithis k
WHERE k.primary_language = 'sa'
AND NOT EXISTS (SELECT 1 FROM krithi_sections WHERE krithi_id = k.id);  -- 0

-- Section distribution
SELECT section_type, COUNT(*) FROM krithi_sections GROUP BY section_type ORDER BY COUNT(*) DESC;

-- Search works
SELECT COUNT(*) FROM krithis WHERE title_normalized LIKE '%sri%';  -- 145+
```
