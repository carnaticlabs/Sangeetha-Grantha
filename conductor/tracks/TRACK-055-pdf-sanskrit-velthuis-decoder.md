| Metadata | Value |
|:---|:---|
| **Track ID** | TRACK-055 |
| **Title** | PDF Extraction — Sanskrit Velthuis Font Decoder |
| **Status** | Completed |
| **Priority** | High |
| **Created** | 2026-02-09 |
| **Updated** | 2026-02-09 |
| **Depends On** | TRACK-054 |
| **Spec Ref** | application_documentation/01-requirements/krithi-data-sourcing/pdf-diacritic-extraction-analysis.md |
| **Est. Effort** | 3–4 days |

# TRACK-055: PDF Extraction — Sanskrit Velthuis Font Decoder

## Objective

Enable extraction of Unicode Devanagari text from Sanskrit PDFs that use Velthuis-dvng Type 1 fonts (e.g. guruguha.org `mdskt.pdf`). These fonts have no `/ToUnicode` CMap; the encoding is embedded in the Type 1 font program. This track implements a decoder that extracts the encoding vector, builds a byte→glyph mapping, and decodes text spans to Unicode Devanagari with left-side mātrā reordering and vowel merging where needed.

## Scope

- **Python extractor only.** New decoder module and integration into PdfExtractor.
- VelthuisDecoder: parse Type 1 font program, build 155-entry byte→Unicode mapping, decode page text with reordering/merging.
- Integration: detect Velthuis-dvng* fonts in extractor, decode spans; other fonts unchanged.
- Worker: set language=sa, script=devanagari for Sanskrit PDF output; optional `titleLatin` (IAST) for matching if Design Decision 5 favours pre-compute in Python.

## Design Context

- Font analysis and mapping details: **Section 7** of the spec (encoding extraction, page 17/18/100 samples).
- Spot-check target: character accuracy ≥95%; no systematic misordering. Optional IAST title per **Design Decision 5** (transliteration).

## Task List

| Task ID | Description | Acceptance Criteria | File(s) |
|:---|:---|:---|:---|
| T55.1 | Create VelthuisDecoder — extract encoding from Type1 font program | Encoding vector parsed from font stream; 155 byte→glyph mappings built. | `tools/pdf-extractor/src/velthuis_decoder.py` (new) |
| T55.2 | Implement byte→Unicode decode with glyph table | Decode produces correct Unicode for page 17 title, raga, tala, section labels (Section 7.5). | `tools/pdf-extractor/src/velthuis_decoder.py` |
| T55.3 | Implement left-side mātrā reordering and vowel merging | ि after consonant in output; a + aa-mātrā → आ where applicable. | `tools/pdf-extractor/src/velthuis_decoder.py` |
| T55.4 | Add unit tests (page 17, 18, 100 samples) | Known Devanagari strings match decoded output within agreed tolerance. | `tools/pdf-extractor/tests/test_velthuis_decoder.py` (new) |
| T55.5 | Integrate decoder into PdfExtractor — detect Velthuis, decode spans | Pages using Velthuis-dvng* fonts return decoded Unicode text; other fonts unchanged. | `tools/pdf-extractor/src/extractor.py` |
| T55.6 | Set language/script in Worker for Devanagari output | CanonicalLyricVariant has language=sa, script=devanagari for Sanskrit PDFs. | `tools/pdf-extractor/src/worker.py` |
| T55.7 | Optional: add IAST title to CanonicalExtractionDto (for matching) | If Python transliteration chosen (Design Decision 5), add `titleLatin` or equivalent for Kotlin matching. | `tools/pdf-extractor/src/schema.py`, `worker.py` |
| T55.8 | Spot-check 10 Krithis vs PDF rendering | Character accuracy ≥95%; no systematic misordering. | Manual |

## Files Changed

| File | Change |
|:---|:---|
| `tools/pdf-extractor/src/velthuis_decoder.py` | New — encoding extraction, byte→Unicode decode, reordering/merging |
| `tools/pdf-extractor/tests/test_velthuis_decoder.py` | New — unit tests |
| `tools/pdf-extractor/src/extractor.py` | Detect Velthuis fonts, decode spans |
| `tools/pdf-extractor/src/worker.py` | language/script for Sanskrit; optional titleLatin |
| `tools/pdf-extractor/src/schema.py` | Optional titleLatin in CanonicalExtractionDto |

## Progress Log

| Date | Unit | Notes |
|:---|:---|:---|
| 2026-02-10 | T55.1–T55.7 | VelthuisDecoder created with 155-entry glyph map, mātrā reordering, vowel merging. 23 unit tests. Integrated into PdfExtractor (auto-detects Velthuis fonts). Worker produces IAST alternateTitle for Devanagari extractions. 67 total tests passing. |
