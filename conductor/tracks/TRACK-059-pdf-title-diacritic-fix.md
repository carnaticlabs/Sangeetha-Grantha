| Metadata | Value |
|:---|:---|
| **Track ID** | TRACK-059 |
| **Title** | PDF Title Diacritic Normalisation & Consonant-Dot Decoding |
| **Status** | Completed |
| **Priority** | Critical |
| **Created** | 2026-02-10 |
| **Updated** | 2026-02-19 |
| **Depends On** | TRACK-054 |
| **Spec Ref** | analysis-extraction-pipeline-failures.md (Finding 1) |
| **Est. Effort** | 2 days |

# TRACK-059: PDF Title Diacritic Normalisation & Consonant-Dot Decoding

## Objective

Fix the critical gap where `normalize_garbled_diacritics()` is applied to raga/tala
metadata but **never to Krithi titles**. Additionally implement "Rule 8" — consonant-dot
patterns (`n.` → ṇ, `d.` → ḍ, `s.` → ṣ, `t.` → ṭ, `l.` → ḷ) that were deliberately
skipped in TRACK-054. Without this fix, all ~480 English PDF titles are stored with
raw garbled diacritics (e.g. `akhil¯an. d. e´svari raks.a m¯am` instead of
`akhilāṇḍeśvari rakṣa mām`), and Kotlin-side normalisation produces broken
`title_normalized` values with spurious spaces and wrong consonants.

### Root Cause (from analysis)

1. `MetadataParser.parse()` receives the title via `title_hint` and returns it
   unchanged — `normalize_garbled_diacritics()` is only called on the body metadata
   text (lines 144–145 of `metadata_parser.py`).
2. `worker.py:_extract_pdf()` passes `segment.title_text` (raw from
   `PageSegmenter`) into `MetadataParser.parse(title_hint=...)` and directly uses
   `metadata.title` in the `CanonicalExtraction`.
3. Rule 8 (consonant + dot) was intentionally omitted from the normaliser because
   it risked false positives in body text. However it is **required** for titles
   where patterns like `n. d.` are unambiguous parts of the garbled encoding.

### Impact if not fixed

- All English PDF titles remain garbled in the DB.
- `title_normalized` contains spurious spaces and wrong consonants.
- Cross-source deduplication fails (only 14/480 match blogspot).
- Variant matching has no usable titles to match against.

## Scope

- **Python extractor only.** No database schema changes.
- Extend `diacritic_normalizer.py` with Rule 8 (consonant-dot patterns).
- Apply full normalisation to title text in `MetadataParser` and/or `worker.py`.
- Update unit tests.

## Design Decisions

| Decision | Choice | Rationale |
|:---|:---|:---|
| Where to normalise titles | In `MetadataParser.parse()`, apply to title before returning | Single responsibility — MetadataParser owns all field cleanup |
| Rule 8 scope | Apply to all text (title + body), not just titles | With the patterns scoped to `consonant + dot + optional space + vowel/consonant`, false positives are minimal |
| Consonant-dot safety | Use anchored patterns: only match `n.` / `d.` / `t.` / `s.` / `l.` when followed by a vowel or space | Prevents matching decimal points, abbreviations, etc. |
| Dot-space handling | Strip the dot and any intervening space: `n. d` → `ṇd` | The space is an artefact of PDF glyph extraction, not intentional |

## Task List

| Task ID | Description | Acceptance Criteria | File(s) |
|:---|:---|:---|:---|
| T59.1 | Implement Rule 8 in `diacritic_normalizer.py` | `n. ` → ṇ, `d. ` → ḍ, `s. ` → ṣ, `t. ` → ṭ, `l. ` → ḷ; handles optional whitespace between consonant-dot and next char; all existing tests still pass. | `tools/krithi-extract-enrich-worker/src/diacritic_normalizer.py` |
| T59.2 | Add Rule 8 unit tests | Tests for: `raks.a` → `rakṣa`, `n. d. e` → `ṇḍe`, `pat.t.` → `paṭṭ`, `t¯al.a` → `tāḷa`; edge cases (decimal numbers, abbreviations not affected). | `tools/krithi-extract-enrich-worker/tests/test_diacritic_normalizer.py` |
| T59.3 | Apply `normalize_garbled_diacritics()` to title in `MetadataParser.parse()` | `title` field in returned `KrithiMetadata` has diacritics normalised; `akhil¯an. d. e´svari raks.a m¯am` → `akhilāṇḍeśvari rakṣa mām`. | `tools/krithi-extract-enrich-worker/src/metadata_parser.py` |
| T59.4 | Verify title normalisation flows through worker | `CanonicalExtraction.title` in `result_payload` contains clean IAST titles for all ~484 compositions. | `tools/krithi-extract-enrich-worker/src/worker.py` (verify, may not need changes) |
| T59.5 | Integration: re-extract mdeng.pdf sample page and verify | Sample page 1 title is `akhilāṇḍeśvari rakṣa mām` (not garbled). Raga/tala still correct. | Manual / test script |

## Files Changed

| File | Change |
|:---|:---|
| `tools/krithi-extract-enrich-worker/src/diacritic_normalizer.py` | Add Rule 8 consonant-dot patterns |
| `tools/krithi-extract-enrich-worker/tests/test_diacritic_normalizer.py` | Rule 8 unit tests |
| `tools/krithi-extract-enrich-worker/src/metadata_parser.py` | Apply normalisation to title (not just body) |

## Progress Log

| Date | Unit | Notes |
|:---|:---|:---|
| 2026-02-10 | Planning | Track created from analysis-extraction-pipeline-failures.md |
| 2026-02-10 | T59.1–T59.4 | Implemented Rule 8 consonant-dot patterns with negative lookbehind `(?<![A-Z])` to prevent false positives on abbreviations. Applied `normalize_garbled_diacritics()` to title in `MetadataParser.parse()`. All 43 diacritic normalizer tests pass (12 new Rule 8 tests). Full suite 79/79 pass. |
