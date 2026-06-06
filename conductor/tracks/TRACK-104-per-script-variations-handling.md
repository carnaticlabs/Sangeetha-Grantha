| Metadata | Value |
|:---|:---|
| **Status** | Completed |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-03-20 |
| **Author** | Sangeetha Grantha Team |

# Track: Handle Per-Script "Variations" as Intra-Variant Metadata

**ID:** TRACK-104
**Status:** Not Started
**Owner:** Sangita Grantha Architect
**Created:** 2026-03-20
**Updated:** 2026-03-20
**Parent:** TRACK-093 (Trinity Krithi Import)

## Goal

Handle the Dikshitar blog's repeated "variations" blocks (one after each script section) as intra-variant metadata boundaries rather than document-level boundaries. Once TRACK-100 enables multi-pass parsing, these per-script "variations" blocks must be excluded from lyric content within each language block.

## Context

- **Investigation Report:** `database/for_import/EXTRACTION-INVESTIGATION-REPORT.md`
- **Primary File(s):** `tools/krithi-extract-enrich-worker/src/structure_parser.py`
- **Severity:** Secondary
- **Affected Blogs:** Dikshitar (guru-guha) only — includes "variations" block after EACH script section (6x per page)
- **Current Behavior:** `_find_metadata_boundaries()` detects multiple VARIATIONS boundaries (e.g., at pos 368, 1135, etc.). Currently moot because TRACK-100's truncation prevents reaching them. After TRACK-100 is fixed, these would be encountered within each language block.
- **Expected Behavior:** Within each language block's section parsing, "variations" should act as a local stop boundary — lyric text stops at the "variations" marker within that block, and the variations content is excluded from the lyric variant.

## Analysis

### Verified Code Path

1. `_find_metadata_boundaries()` (line 862) uses `METADATA_BOUNDARY_PATTERNS` which includes `VARIATIONS` (line 247)
2. Test evidence shows 3 VARIATIONS boundaries in Dikshitar content: pos 368, 756(MEANING), 1135
3. After TRACK-100 fix, per-language-block parsing via `_build_blocks()` will encounter `VARIATIONS` as a header
4. `_detect_language_header()` (line 388) already has `("variations", "VARIATIONS")` in candidates
5. `_extract_language_header_variants()` (line 665) already has `if block.label in METADATA_LABELS: flush(); break` at line 688-689
6. **This means the existing code already handles this correctly** — when a VARIATIONS block is encountered within a language block, `_extract_language_header_variants()` flushes the current variant and stops processing that block

### Re-Assessment

After tracing the code, the existing `_extract_language_header_variants()` method at lines 687-689 already breaks on `METADATA_LABELS` (which includes `VARIATIONS`). Once TRACK-100 provides full-document text to per-block parsing, the per-script variations blocks will be naturally excluded.

**However**, there's a subtle issue: if `_build_blocks()` processes a full language block that contains a "variations" section followed by more lyric content (e.g., a "Word Division" sub-block after "variations"), the `break` at line 689 would prematurely stop variant collection. This needs verification against actual Dikshitar page structure.

### Dikshitar Page Structure Per Language Block

```
[Language Header: Devanagari]
  pallavi (lyrics)
  anupallavi (lyrics)
  charanam (lyrics)
  variations (metadata)        ← break here is correct
[Language Header: Tamil]       ← next language block starts fresh
```

The `break` is correct per-block because each language section is self-contained with its own variations footer. No lyric content follows variations within the same language block.

## Implementation Plan

### Phase 1: Verification

- [ ] After TRACK-100 is implemented, run integration tests against Dikshitar blog HTML
- [ ] Verify that per-script "variations" blocks are correctly excluded from lyric variants
- [ ] Confirm the `break` at line 689 fires correctly within each language block

### Phase 2: Fix (if needed)

- [ ] If the existing `break` logic is insufficient (e.g., variations content leaks into lyrics), add explicit per-block metadata boundary detection
- [ ] If some language blocks have content after variations that should be included, change `break` to `continue`

### Phase 3: Testing & Validation

- [ ] Unit test: synthetic input with per-script variations → variants exclude variations text
- [ ] Integration test against Dikshitar blog HTML with all 6 script sections
- [ ] Verify variant section count matches expected (no extra "variations" content in sections)
- [ ] Regression: Syama Sastri and Tyagaraja (single or no variations block) still work

## Design Considerations

The existing code may already handle this correctly once TRACK-100 provides full text. This track is primarily a **verification and safety-net** track. If integration testing after TRACK-100 shows no issues, this track can be marked as "Verified — No Changes Needed."

## Acceptance Criteria

- [ ] Per-script "variations" text does not appear in any lyric variant's section text
- [ ] Each Dikshitar variant has only lyric sections (Pallavi, Anupallavi, Charanam) — no metadata contamination
- [ ] All existing tests pass
- [ ] Validated against Dikshitar blog HTML

## Dependencies

- **Depends on:** TRACK-100 (must have multi-pass parsing working first)
- **Blocks:** None

## Files to Modify

- `tools/krithi-extract-enrich-worker/src/structure_parser.py` — potentially `_extract_language_header_variants()` (only if verification reveals issues)
- `tools/krithi-extract-enrich-worker/tests/test_structure_parser.py` — add per-script variations test cases
