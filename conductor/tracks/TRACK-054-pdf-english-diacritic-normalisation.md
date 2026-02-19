| Metadata | Value |
|:---|:---|
| **Track ID** | TRACK-054 |
| **Title** | PDF Extraction — English Diacritic Normalisation |
| **Status** | Completed |
| **Priority** | High |
| **Created** | 2026-02-09 |
| **Updated** | 2026-02-19 |
| **Depends On** | TRACK-053 |
| **Spec Ref** | application_documentation/01-requirements/krithi-data-sourcing/pdf-diacritic-extraction-analysis.md |
| **Est. Effort** | 2–3 days |

# TRACK-054: PDF Extraction — English Diacritic Normalisation

## Objective

Fix raga, tala, and section (Charanam) extraction for the guruguha.org English PDF by normalising garbled Utopia font diacritics and updating regex patterns. After TRACK-053, 480 of 481 Krithis had "Unknown" for Raga and Tala, and 433 had only 2 sections (Pallavi + Anupallavi) because Charanam labels and raga/tala text were present in the PDF but not recognised due to garbled encoding (e.g. `r¯aga ˙m`, `t¯al.a ˙m`, `caran. am`).

## Scope

- **Python extractor only.** No UI, API, or database schema changes.
- New diacritic normaliser utility and unit tests.
- Updates to MetadataParser, StructureParser, PageSegmenter, and Worker so that:
  - Raga and tala are correctly extracted from body text containing garbled forms.
  - Section type CHARANAM is detected for labels like `caran. am` / `caran.am`.
  - Output raga/tala names are title-cased, diacritics normalised, and metadata artefacts (e.g. mēḷa number, parentheses) stripped.

## Design Context

- Diacritic mappings and patterns are defined in **Section 4.1** of the spec (e.g. ¯a→ā, ˙m→ṁ, .n→ṇ).
- Verification criteria: ≤5 Krithis with Unknown raga (vs 480 before); ≥430 Krithis with 3+ sections (vs 0 before). See **Section 6.2** of the spec.

## Task List

| Task ID | Description | Acceptance Criteria | File(s) |
|:---|:---|:---|:---|
| T54.1 | Create `normalize_garbled_diacritics()` utility | All mappings from Section 4.1 (¯a→ā, ˙m→ṁ, etc.) pass unit tests; optional whitespace between diacritic and base handled. | `tools/krithi-extract-enrich-worker/src/diacritic_normalizer.py` (new) |
| T54.2 | Add unit tests for diacritic normaliser | Tests for r¯aga ˙m, t¯al.a ˙m, juj¯avanti, ¯adi, mi´sra c¯apu, n¯ıl¯ambari, caran. am. | `tools/krithi-extract-enrich-worker/tests/test_diacritic_normalizer.py` (new) |
| T54.3 | Update MetadataParser — normalise before regex; add garbled-form patterns | Raga/tala extracted from body_text containing r¯aga ˙m, t¯al.a ˙m; mēḷa number stripped. | `tools/krithi-extract-enrich-worker/src/metadata_parser.py` |
| T54.4 | Update StructureParser — add caran. am, samashti variants | Section type CHARANAM detected when label is caran. am or caran.am. | `tools/krithi-extract-enrich-worker/src/structure_parser.py` |
| T54.5 | Update PageSegmenter METADATA_LINE_PATTERN | Pattern matches r¯aga, t¯al.a, raga, tala, राग, ताल. | `tools/krithi-extract-enrich-worker/src/page_segmenter.py` |
| T54.6 | Apply name cleanup in Worker before CanonicalExtraction | Raga/tala names in output are title-cased, diacritics normalised, (28) etc. stripped. | `tools/krithi-extract-enrich-worker/src/worker.py` |
| T54.7 | Re-extract English PDF and verify | ≤5 Krithis with Unknown raga (vs 480 before); ≥430 Krithis with 3+ sections (vs 0 before). | Manual / SQL (Section 6.2) |

## Files Changed

| File | Change |
|:---|:---|
| `tools/krithi-extract-enrich-worker/src/diacritic_normalizer.py` | New — normalise garbled diacritics |
| `tools/krithi-extract-enrich-worker/tests/test_diacritic_normalizer.py` | New — unit tests |
| `tools/krithi-extract-enrich-worker/src/metadata_parser.py` | Normalise before regex; add garbled raga/tala patterns |
| `tools/krithi-extract-enrich-worker/src/structure_parser.py` | Add caran. am / caran.am section patterns |
| `tools/krithi-extract-enrich-worker/src/page_segmenter.py` | METADATA_LINE_PATTERN for garbled forms |
| `tools/krithi-extract-enrich-worker/src/worker.py` | Apply normalisation and name cleanup before CanonicalExtraction |

## Progress Log

| Date | Unit | Notes |
|:---|:---|:---|
| 2026-02-10 | T54.1–T54.7 | All tasks completed. Diacritic normalizer utility created with 31 unit tests. MetadataParser, StructureParser, PageSegmenter, and Worker updated. 44 tests passing. |
