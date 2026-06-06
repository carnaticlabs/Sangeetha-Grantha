| Metadata | Value |
|:---|:---|
| **Status** | Completed |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-03-20 |
| **Author** | Sangeetha Grantha Team |

# Track: Handle "Script - Word Division" Compound Language Headers

**ID:** TRACK-102
**Status:** Not Started
**Owner:** Sangita Grantha Architect
**Created:** 2026-03-20
**Updated:** 2026-03-20
**Parent:** TRACK-093 (Trinity Krithi Import)

## Goal

Add support for Dikshitar blog's compound language header format `"English - Word Division"`, `"Devanagari - Word Division"`, etc. The parser currently detects `"English"` as a language header and treats the `"Word Division"` portion ambiguously, potentially misclassifying content blocks.

## Context

- **Investigation Report:** `database/for_import/EXTRACTION-INVESTIGATION-REPORT.md`
- **Primary File(s):** `tools/krithi-extract-enrich-worker/src/structure_parser.py`
- **Severity:** Secondary
- **Affected Blogs:** Dikshitar (guru-guha) only — uses "English - Word Division", "Devanagari - Word Division" format with separate blocks per script
- **Current Behavior:** `_detect_language_header()` (line 388) checks `lowered.startswith(f"{key} -")`. For `"english - word division"`, it matches `"english"` with key `"english"` and the remainder becomes `"word division"`. The block is labeled `ENGLISH` but is actually a word-division variant. The `WORD_DIVISION` semantic is lost.
- **Expected Behavior:** `"English - Word Division"` should be recognized as a word-division block for the English script, labeled distinctly (e.g., `ENGLISH_WORD_DIVISION` or tagged as word-division type) so the dual-format merge logic can correctly deduplicate continuous vs word-division versions.

## Analysis

### Verified Code Path

1. `_detect_language_header()` (line 388-393): iterates `LANGUAGE_HEADER_CANDIDATES`
2. For input `"english - word division"`:
   - Matches `("english", "ENGLISH")` via `lowered.startswith(f"{key} -")`  → `True`
   - Returns `_HeaderMatch(label="ENGLISH", remainder="word division")`
3. The `remainder` "word division" is discarded as unlabeled text within the ENGLISH block
4. Later, `_extract_language_header_variants()` treats this as a regular ENGLISH language block
5. The `_merge_dual_format()` method (line 571) handles deduplication but only within the same variant — it can't distinguish "English continuous" from "English word division" since both are labeled `ENGLISH`

### Fix Approach

Add compound entries to `LANGUAGE_HEADER_CANDIDATES` that match before the simple entries. Order matters since the method returns on first match:

```python
# Add BEFORE the simple "english" entry:
("english - word division", "ENGLISH_WORD_DIVISION"),
("devanagari - word division", "DEVANAGARI_WORD_DIVISION"),
("tamil - word division", "TAMIL_WORD_DIVISION"),
("telugu - word division", "TELUGU_WORD_DIVISION"),
("kannada - word division", "KANNADA_WORD_DIVISION"),
("malayalam - word division", "MALAYALAM_WORD_DIVISION"),
```

Then add these compound labels to `METADATA_LABELS` so they're treated as non-lyric content (word divisions are supplementary to the continuous form and should be used for merge/dedup only).

## Implementation Plan

### Phase 1: Header Detection

- [ ] Add compound `"script - word division"` entries to `LANGUAGE_HEADER_CANDIDATES` — placed before simple script entries so they match first
- [ ] Add the compound labels to `LANGUAGE_LABELS` set
- [ ] Decide treatment: either add to `METADATA_LABELS` (skip entirely) or keep as language blocks for dual-format merging

### Phase 2: Dual-Format Integration

- [ ] If kept as language blocks: modify `_extract_language_header_variants()` to pair each word-division block with its continuous counterpart
- [ ] Use existing `_merge_dual_format()` logic to deduplicate, keeping the word-division version (has explicit word boundaries useful for display)

### Phase 3: Testing & Validation

- [ ] Unit test: `"English - Word Division"` detected as compound header, not plain `ENGLISH`
- [ ] Unit test: `"Devanagari - Word Division"` similarly handled
- [ ] Integration test against Dikshitar blog HTML — verify word-division blocks are correctly paired/merged
- [ ] Regression: Syama Sastri and Tyagaraja blogs (which use simpler formats) still work

## Design Considerations

**Option A — Treat as METADATA_LABELS (skip):** Word-division blocks are redundant with continuous blocks. Simplest approach — just skip them. Risk: loses the explicit word boundary information.

**Option B — Pair and merge (recommended):** Keep word-division blocks as language variants, then use `_merge_dual_format()` to pick the better version (word-division, which has cleaner spacing). This preserves maximum information and aligns with the existing merge logic.

## Acceptance Criteria

- [ ] `"English - Word Division"` is not confused with a plain `"English"` language block
- [ ] Word-division content is either merged with or excluded from the corresponding continuous variant
- [ ] No duplicate variants for the same script
- [ ] All existing tests pass
- [ ] Validated against Dikshitar blog HTML

## Dependencies

- **Depends on:** TRACK-100 (multi-pass parsing needed to reach the Indic word-division blocks)
- **Blocks:** None

## Files to Modify

- `tools/krithi-extract-enrich-worker/src/structure_parser.py` — `LANGUAGE_HEADER_CANDIDATES`, `LANGUAGE_LABELS`, possibly `_extract_language_header_variants()`
- `tools/krithi-extract-enrich-worker/tests/test_structure_parser.py` — add compound header test cases
