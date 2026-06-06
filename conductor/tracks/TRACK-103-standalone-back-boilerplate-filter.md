| Metadata | Value |
|:---|:---|
| **Status** | Completed |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-03-20 |
| **Author** | Sangeetha Grantha Team |

# Track: Filter Standalone "Back" Link Text as Boilerplate

**ID:** TRACK-103
**Status:** Not Started
**Owner:** Sangita Grantha Architect
**Created:** 2026-03-20
**Updated:** 2026-03-20
**Parent:** TRACK-093 (Trinity Krithi Import)

## Goal

Add standalone "Back" text to the boilerplate filter in `StructureParser._is_boilerplate()`. Dikshitar blog pages include "Back" navigation links between script sections that currently leak into lyric text.

## Context

- **Investigation Report:** `database/for_import/EXTRACTION-INVESTIGATION-REPORT.md`
- **Primary File(s):** `tools/krithi-extract-enrich-worker/src/structure_parser.py`
- **Severity:** Secondary
- **Affected Blogs:** Dikshitar (guru-guha) only — has "Back" navigation links between sections
- **Current Behavior:** `_is_boilerplate()` (line 878) checks for `"back to"` in its marker list but not standalone `"Back"` or `"back"`. A line containing just `"Back"` passes the boilerplate filter and enters lyric text.
- **Expected Behavior:** Standalone `"Back"` lines should be filtered as navigation boilerplate.

## Analysis

### Verified Code Path

1. `_build_blocks()` (line 315) calls `_is_boilerplate()` for each non-empty line (line 334)
2. `_is_boilerplate()` (line 878) checks `lowered` against a list of marker strings
3. The marker list (lines 898-912) includes `"link to this post"`, `"newer post"`, `"older post"` etc.
4. No check for standalone `"back"` — the string `"back to"` would only match if followed by more text
5. Dikshitar pages have standalone `"Back"` text (from `<a>Back</a>` navigation links) between every script section

### Fix

Add to `_is_boilerplate()`:
```python
if lowered == "back":
    return True
```

This is a standalone-line-only check (exact match on `"back"`) to avoid false positives where "back" appears within legitimate lyric text.

Also add `"Meaning of Kriti"` navigation link pattern — the report notes Dikshitar pages have `"Meaning of Kriti-1"` style links:
```python
if re.match(r"^meaning of kriti", lowered):
    return True
```

## Implementation Plan

### Phase 1: Fix

- [ ] Add `if lowered == "back": return True` to `_is_boilerplate()` (early return, before the `any()` check)
- [ ] Add `if lowered.startswith("meaning of kriti"): return True` for Dikshitar navigation links

### Phase 2: Testing & Validation

- [ ] Unit test: `"Back"` → boilerplate=True
- [ ] Unit test: `"back"` → boilerplate=True
- [ ] Unit test: `"go back to the beginning"` → still passes through (contains "back" but not standalone)
- [ ] Unit test: `"Meaning of Kriti-1"` → boilerplate=True
- [ ] Regression: existing tests pass
- [ ] Integration test against Dikshitar blog HTML — no "Back" in lyric text

## Design Considerations

Using exact match (`lowered == "back"`) is safer than substring match to avoid false positives. The word "back" is unlikely to appear as a standalone lyric line in Carnatic compositions.

## Acceptance Criteria

- [ ] Standalone "Back" lines do not appear in extracted lyric text
- [ ] "Meaning of Kriti-N" navigation links are filtered
- [ ] No false positives on legitimate lyric content
- [ ] All existing tests pass
- [ ] Validated against Dikshitar blog HTML

## Dependencies

- **Depends on:** TRACK-100 (need multi-pass parsing to encounter these lines in Indic blocks)
- **Blocks:** None

## Files to Modify

- `tools/krithi-extract-enrich-worker/src/structure_parser.py` — `_is_boilerplate()` method
- `tools/krithi-extract-enrich-worker/tests/test_structure_parser.py` — add boilerplate filter test cases
