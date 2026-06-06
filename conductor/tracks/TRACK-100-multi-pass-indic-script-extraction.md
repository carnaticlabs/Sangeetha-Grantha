| Metadata | Value |
|:---|:---|
| **Status** | Completed |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-03-20 |
| **Author** | Sangeetha Grantha Team |

# Track: Multi-Pass Parsing Architecture for Indic Script Extraction

**ID:** TRACK-100
**Status:** Not Started
**Owner:** Sangita Grantha Architect
**Created:** 2026-03-20
**Updated:** 2026-03-20
**Parent:** TRACK-093 (Trinity Krithi Import)

## Goal

Redesign the `StructureParser.parse()` method to process content beyond the first metadata boundary, enabling extraction of Devanagari, Tamil, Telugu, Kannada, and Malayalam lyric variants. Currently only the English/IAST variant is extracted because the parser truncates all text at the first metadata boundary (Gist/Meaning/Variations), and all Indic-script lyrics appear after these boundaries on all three Trinity blogs.

## Context

- **Investigation Report:** `database/for_import/EXTRACTION-INVESTIGATION-REPORT.md`
- **Primary File(s):** `tools/krithi-extract-enrich-worker/src/structure_parser.py`
- **Severity:** Critical
- **Affected Blogs:** All three — Dikshitar (guru-guha), Syama Sastri (syamakrishna), Tyagaraja (thyagaraja)
- **Current Behavior:** `parse()` at line 299 sets `lyric_window_end = effective_boundaries[0].start_pos`, truncating all text after the first metadata boundary. On Dikshitar blogs this is `variations` (~pos 370), on Syama Sastri/Tyagaraja it's `Gist` (~pos 229-285). All Indic scripts appear after these positions and are never parsed.
- **Expected Behavior:** Parser should extract 4-6 lyric variants per krithi (en + sa + ta + te + kn + ml), each with properly mapped sections (Pallavi, Anupallavi, Charanam, etc.).

## Analysis

### Verified Code Path

1. `parse()` (line 274) receives full extracted text
2. `_find_metadata_boundaries()` (line 862) scans entire text for MEANING, GIST, NOTES, WORD_DIVISION, VARIATIONS patterns
3. Lines 286-297: Early-boundary guard skips boundaries in first 200 chars (TRACK-097 fix)
4. **Line 299-300** — the critical truncation:
   ```python
   lyric_window_end = effective_boundaries[0].start_pos if effective_boundaries else len(source_text)
   lyric_text = source_text[:lyric_window_end]
   ```
5. `_build_blocks()` (line 315) only receives `lyric_text` — the truncated window
6. `_extract_sections()` and `_extract_lyric_variants()` therefore only see English/IAST content

### Blog Content Layout (verified against test evidence)

```
English/IAST sections (Pallavi, Anupallavi, Charanam)
─── metadata boundary (Gist/Variations) ─── ← parser stops here
Word-by-word Meaning
Notes/Comments
Devanagari sections
Tamil sections
Telugu sections
Kannada sections
Malayalam sections
```

### Report Accuracy

The report's characterization is accurate. The truncation at line 299-300 is the root cause. The `_extract_language_header_variants()` method (line 665) already has logic to handle language-labeled blocks, but it never sees them because `_build_blocks()` is fed truncated text.

## Implementation Plan

### Phase 1: Full-Document Language Block Detection

- [ ] In `parse()`, scan the **entire** `source_text` (not just `lyric_text`) for language headers using `_detect_language_header()` to identify top-level language block boundaries
- [ ] Build a map of language blocks: `{language_label: (start_pos, end_pos)}` covering the full document
- [ ] The English/IAST block remains bounded by the first metadata boundary (current behavior preserved)
- [ ] Each Indic-script block is bounded by its language header start through the next language header (or document end)

### Phase 2: Per-Block Section Parsing

- [ ] For each language block, run `_build_blocks()` on the block's text slice independently
- [ ] Within each block, use metadata boundaries **local to that block** to exclude meanings/notes/variations
- [ ] Run `_extract_sections()` per block to get section structure
- [ ] Map Indic-script sections to the canonical skeleton from the English/IAST block (existing `_sections_from_variant_blocks()` logic)

### Phase 3: Integration

- [ ] Modify `parse()` to assemble `lyric_variants` from all language blocks instead of just the truncated window
- [ ] Ensure the English/IAST variant is always the first variant (canonical)
- [ ] Handle the case where a language block contains its own metadata sub-sections (e.g., Dikshitar's per-script "variations" — see TRACK-104)
- [ ] Preserve backward compatibility: documents without language headers should continue using the existing `_extract_script_split_variants()` path

### Phase 4: Testing & Validation

- [ ] Unit tests: multi-variant extraction from synthetic text with all 6 scripts
- [ ] Integration test against actual blog HTML from all 3 sources (Dikshitar, Syama Sastri, Tyagaraja)
- [ ] Verify each source produces expected variant count: Dikshitar (6), Syama Sastri (6+), Tyagaraja (6+)
- [ ] Verify section mapping: each variant's sections align with the English canonical skeleton
- [ ] Regression: existing single-variant extractions (PDFs, etc.) still work correctly
- [ ] Validate on 60-krithi test CSV before full 1,240-krithi run

## Design Considerations

**Option A — Full-document language-block parsing** (recommended): Scan the entire document for language headers, parse each block independently. This is cleaner because it treats each language variant as a first-class entity and naturally handles per-script metadata sections.

**Option B — Two-phase approach**: Extract English skeleton first, then scan remaining document for Indic blocks. This preserves more of the current code path but creates tighter coupling between the two phases.

**Recommendation:** Option A. The existing `_extract_language_header_variants()` method already implements most of the per-block logic — it just needs to operate on the full document. The change is primarily in `parse()` where the text window is constructed, not in the downstream extraction methods.

**Risk:** Language headers (e.g., "Tamil", "Devanagari") might appear within lyric text as words. Mitigation: language headers must be standalone lines (the existing `_detect_language_header()` already checks for this via `lowered == key` or `lowered.startswith(f"{key}:")` patterns).

## Acceptance Criteria

- [ ] Parser extracts 4-6 lyric variants from Dikshitar blog HTML (en + sa + ta + te + kn + ml)
- [ ] Parser extracts 4-6 lyric variants from Syama Sastri blog HTML
- [ ] Parser extracts 4-6 lyric variants from Tyagaraja blog HTML
- [ ] Each variant has correctly mapped sections (Pallavi, Anupallavi, Charanam at minimum)
- [ ] English/IAST variant sections are identical to current output (no regression)
- [ ] All existing tests pass
- [ ] Validated against real HTML from all 3 blog sources

## Dependencies

- **Depends on:** None (this is the foundational fix)
- **Blocks:** TRACK-101, TRACK-102, TRACK-103, TRACK-104 (all secondary fixes build on top of working multi-pass extraction)

## Files to Modify

- `tools/krithi-extract-enrich-worker/src/structure_parser.py` — rewrite `parse()` to scan full document for language blocks; adjust `_build_blocks()` call sites
- `tools/krithi-extract-enrich-worker/tests/test_structure_parser.py` — add multi-variant test cases for all 3 blog sources
