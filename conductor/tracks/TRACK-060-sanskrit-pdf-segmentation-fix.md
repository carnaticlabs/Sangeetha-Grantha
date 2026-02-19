| Metadata | Value |
|:---|:---|
| **Track ID** | TRACK-060 |
| **Title** | Sanskrit PDF Segmentation — Bold Detection & Devanagari Support |
| **Status** | Completed |
| **Priority** | Critical |
| **Created** | 2026-02-10 |
| **Updated** | 2026-02-19 |
| **Depends On** | TRACK-054, TRACK-055 |
| **Spec Ref** | analysis-extraction-pipeline-failures.md (Finding 3) |
| **Est. Effort** | 2–3 days |

# TRACK-060: Sanskrit PDF Segmentation — Bold Detection & Devanagari Support

## Objective

Fix the page segmenter so it can correctly segment the Sanskrit Devanagari PDF
(mdskt.pdf, ~484 Krithis) into individual compositions. Currently the entire
280-page PDF is treated as a single composition (`result_count: 1`) because the
bold-font detection fails for Devanagari fonts.

### Root Cause (from analysis)

`PageSegmenter._find_title_positions()` requires `block.is_bold == True`.
In `PdfExtractor._extract_page()`, bold detection is:
```python
is_bold = "Bold" in font_name or "bold" in font_name
```

Devanagari fonts (e.g. `Sanskrit2003`, `Chandas`, `Uttara`, `Siddhanta`) rarely
include "Bold" in the font name string. PyMuPDF provides font flags in
`span["flags"]` where bit 4 (value 16) indicates bold — this is not used.

When no title positions are found, `segment()` falls back to `_single_segment()`
which wraps the entire document as one composition.

### Impact if not fixed

- Sanskrit PDF produces 1 extraction result instead of ~484.
- Variant matching has nothing to work with (0 variant matches).
- The entire ENRICH pipeline is inoperative for Devanagari sources.

## Scope

- **Python extractor only.** No database or backend changes.
- Fix bold detection to use PyMuPDF font flags.
- Add Devanagari-specific title detection heuristics.
- Add fallback segmentation when no bold fonts exist.
- Unit tests for the new detection logic.

## Design Decisions

| Decision | Choice | Rationale |
|:---|:---|:---|
| Bold detection method | Use PyMuPDF `span["flags"] & 16` (bit 4) as primary; font name as fallback | Font flags are reliable across all scripts; font name is a heuristic |
| Fallback when no bold | Font-size-only heuristic with metadata proximity confirmation | Some PDFs use size (not weight) to distinguish titles; requiring metadata nearby prevents false positives |
| Devanagari title confirmation | Check for Devanagari Unicode range (U+0900–U+097F) in title text + metadata patterns | Ensures we don't segment on random large text |
| Script-specific metadata patterns | Already in place: `राग` and `ताल` in `METADATA_LINE_PATTERN` | No changes needed to metadata detection |

## Task List

| Task ID | Description | Acceptance Criteria | File(s) |
|:---|:---|:---|:---|
| T60.1 | Fix bold detection: use `span["flags"]` in PdfExtractor | `TextBlock.is_bold` is True for PyMuPDF-flagged bold spans regardless of font name. Existing Latin bold detection still works. | `tools/krithi-extract-enrich-worker/src/extractor.py` |
| T60.2 | Add fallback title detection in PageSegmenter | When `_find_title_positions()` finds no bold candidates at any threshold, try font-size-only candidates with `_has_metadata_nearby()` as gatekeeper. Return these as title positions. | `tools/krithi-extract-enrich-worker/src/page_segmenter.py` |
| T60.3 | Unit tests for bold detection with font flags | Test that `is_bold=True` when `flags & 16` even if font name has no "Bold". Test backward compat for Latin fonts. | `tools/krithi-extract-enrich-worker/tests/test_extractor.py` (new or extend) |
| T60.4 | Unit tests for fallback segmentation | Test that a document with no bold fonts but clear font-size titles + metadata nearby produces correct segments. | `tools/krithi-extract-enrich-worker/tests/test_page_segmenter.py` (new or extend) |
| T60.5 | Integration: extract mdskt.pdf sample pages | Extract pages 1–10 of mdskt.pdf; verify >= 3 segments detected with Devanagari titles. | Manual / test script |
| T60.6 | Full extraction: mdskt.pdf end-to-end | Full PDF segmented into ~484 compositions (tolerance ±10). Title text is Devanagari. Body text correctly split. | Manual / SQL verification |

## Files Changed

| File | Change |
|:---|:---|
| `tools/krithi-extract-enrich-worker/src/extractor.py` | Use `span["flags"] & 16` for bold detection alongside font name |
| `tools/krithi-extract-enrich-worker/src/page_segmenter.py` | Fallback title detection when no bold candidates found |
| `tools/krithi-extract-enrich-worker/tests/test_extractor.py` | Bold detection unit tests |
| `tools/krithi-extract-enrich-worker/tests/test_page_segmenter.py` | Fallback segmentation unit tests |

## Progress Log

| Date | Unit | Notes |
|:---|:---|:---|
| 2026-02-10 | Planning | Track created from analysis-extraction-pipeline-failures.md |
| 2026-02-10 | T60.1–T60.2 | Fixed bold detection in `PdfExtractor._extract_page()` to use `span["flags"] & 16` (PyMuPDF font flags). Added `_find_title_positions_by_size_only()` fallback in `PageSegmenter` for PDFs where no bold candidates found at any threshold — uses font size with metadata proximity as gatekeeper. Full suite 79/79 pass. |
