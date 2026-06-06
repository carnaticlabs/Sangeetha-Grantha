| Metadata | Value |
|:---|:---|
| **Status** | Completed |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-03-20 |
| **Author** | Sangeetha Grantha Team |

# Track: Strip Residual Numbers from Numbered Section Labels

**ID:** TRACK-101
**Status:** Not Started
**Owner:** Sangita Grantha Architect
**Created:** 2026-03-20
**Updated:** 2026-03-20
**Parent:** TRACK-093 (Trinity Krithi Import)

## Goal

Fix the `StructureParser` so that when numbered section headers like `caraNam 1`, `svara sAhitya 2` are matched, the trailing number is stripped from the section body text. Currently the number bleeds into the lyric content as a leading character.

## Context

- **Investigation Report:** `database/for_import/EXTRACTION-INVESTIGATION-REPORT.md`
- **Primary File(s):** `tools/krithi-extract-enrich-worker/src/structure_parser.py`
- **Severity:** Secondary
- **Affected Blogs:** Primarily Syama Sastri (numbered caraNam 1/2/3) and any blog with numbered sections
- **Current Behavior:** When `_detect_section_header()` matches `caraNam 1`, the regex `pattern.sub("", line, count=1)` at line 399 removes the matched section keyword but leaves the trailing number. The `remainder` becomes `"1 \n mANikya mayamaiyunna..."`.
- **Expected Behavior:** The `remainder` should be `"mANikya mayamaiyunna..."` with no leading number.

## Analysis

### Verified Code Path

1. `_detect_section_header()` (line 396) iterates `SECTION_HEADER_PATTERNS`
2. The CHARANAM pattern (line 146-149): `r"^\s*[\-–—•*()=\[\]]*\s*(?:(?:ch|c)ara?n(?:\.\s*am|am)|caraṇam)(?:\b|:|\.|\-|\)|]|=|$)"`
3. This pattern matches `caraNam` but NOT the trailing ` 1` — the `(?:\b|...|$)` anchor group matches at the word boundary after "caraNam"
4. Line 399: `remainder = pattern.sub("", line, count=1).strip()` removes `caraNam` but `" 1"` stays
5. Line 400: `remainder = re.sub(r"^[:\-)\]\.\s]+", "", remainder).strip()` strips leading punctuation/whitespace but NOT digits
6. The `remainder` `"1"` is then prepended to the section text via `_HeaderMatch.remainder`

### Fix Location

The fix belongs in `_detect_section_header()` at line 400. After stripping punctuation/whitespace, also strip a leading number (with optional whitespace) that represents the section occurrence counter.

```python
# Current (line 400):
remainder = re.sub(r"^[:\-)\]\.\s]+", "", remainder).strip()

# Fixed:
remainder = re.sub(r"^[:\-)\]\.\s]+", "", remainder).strip()
remainder = re.sub(r"^\d+\s*", "", remainder).strip()
```

### Edge Case Consideration

Need to ensure this doesn't strip leading digits that are actually part of lyric text. In Carnatic compositions, lyric lines don't start with bare numbers — numbered section markers are the only source of leading digits after a header match. Safe to strip.

## Implementation Plan

### Phase 1: Fix

- [ ] In `_detect_section_header()` (line 400), add `re.sub(r"^\d+\s*", "", remainder)` after the existing punctuation strip
- [ ] Alternatively, extend the existing regex at line 400 to include digits: `r"^[:\-)\]\.\s\d]+"`

### Phase 2: Testing & Validation

- [ ] Unit test: input `"caraNam 1\nmANikya mayamaiyunna"` → section text should be `"mANikya mayamaiyunna"` with no leading `"1"`
- [ ] Unit test: input `"svara sAhitya 2\nkunda radanA"` → section text `"kunda radanA"`
- [ ] Unit test: input `"caraNam\nmANikya"` (no number) → still works correctly
- [ ] Regression: verify existing passing extractions still work
- [ ] Integration test against Syama Sastri blog HTML (has numbered caraNam 1/2/3)

## Design Considerations

Simple one-line fix. The trailing-number strip should happen after the existing punctuation strip to handle cases like `caraNam - 1` or `caraNam: 2`.

## Acceptance Criteria

- [ ] No leading digits in section body text when section header has a number suffix
- [ ] Sections without numbers are unaffected
- [ ] All existing tests pass
- [ ] Validated against real HTML from all 3 blog sources

## Dependencies

- **Depends on:** TRACK-100 (multi-pass parsing must work first so we can validate across all variants)
- **Blocks:** None

## Files to Modify

- `tools/krithi-extract-enrich-worker/src/structure_parser.py` — `_detect_section_header()` method, line ~400
- `tools/krithi-extract-enrich-worker/tests/test_structure_parser.py` — add numbered-label test cases
