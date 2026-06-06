# Prompt: Implement Multi-Pass Indic Script Extraction (TRACK-100 through TRACK-104)

> **Purpose**: Feed this prompt to Claude Opus to implement the critical multi-pass parsing fix and all dependent secondary fixes in `StructureParser`. This is a single coherent implementation session — the fixes are interdependent and should be landed together.
> **Output**: Working code changes to `structure_parser.py`, new test fixtures, and comprehensive test coverage. All existing tests must continue to pass.

---

## Role & Context

You are a Principal Python Engineer implementing fixes to the `StructureParser` in **Sangeetha Grantha**, a Carnatic classical music Krithi database. You have deep expertise in text parsing pipelines, Unicode/Indic script handling, and test-driven development. You write minimal, correct code — no over-engineering, no unnecessary abstractions.

The `StructureParser` parses blog HTML (already extracted to plain text) into structured lyric sections across multiple Indic scripts (Devanagari, Tamil, Telugu, Kannada, Malayalam). It currently has a **critical truncation bug**: it stops processing at the first metadata boundary (Gist/Meaning/Variations), discarding all Indic-script lyrics that appear after it. You will fix this and four related secondary issues.

## Tracks to Implement

These tracks form a dependency tree. Implement them in order — TRACK-100 first, then TRACK-101 through TRACK-104 (which can be applied incrementally on top).

| Track | Summary | Severity |
|:---|:---|:---|
| **TRACK-100** | Multi-pass parsing: extract Indic script variants beyond metadata boundaries | Critical |
| **TRACK-101** | Strip residual numbers from `caraNam 1` / `svara sAhitya 2` section headers | Secondary |
| **TRACK-102** | Handle `"English - Word Division"` compound language headers | Secondary |
| **TRACK-103** | Filter standalone `"Back"` navigation text as boilerplate | Secondary |
| **TRACK-104** | Verify per-script `"variations"` blocks excluded from lyric variants | Secondary |

## Mandatory Reading — Do This First

Read every file listed below before writing any code. Understand the full code path from `parse()` → `_build_blocks()` → `_extract_sections()` → `_extract_lyric_variants()`. Understand how language headers, section headers, metadata boundaries, and boilerplate filtering interact.

```
# THE BUG — investigation report with root cause and test evidence
database/for_import/EXTRACTION-INVESTIGATION-REPORT.md

# PRIMARY TARGET — the parser you are modifying
tools/krithi-extract-enrich-worker/src/structure_parser.py

# SCHEMA — data models (SectionType, CanonicalLyricVariant, etc.)
tools/krithi-extract-enrich-worker/src/schema.py

# CALLERS — understand how the parser is invoked
tools/krithi-extract-enrich-worker/src/worker.py
tools/krithi-extract-enrich-worker/src/html_extractor.py
tools/krithi-extract-enrich-worker/src/extractor.py

# EXISTING TESTS — understand current coverage and fixture patterns
tools/krithi-extract-enrich-worker/tests/test_structure_parser.py
tools/krithi-extract-enrich-worker/tests/fixtures/structure_parser/

# TRACK DETAILS — full analysis, verified code paths, design decisions
conductor/tracks/TRACK-100-multi-pass-indic-script-extraction.md
conductor/tracks/TRACK-101-numbered-section-label-stripping.md
conductor/tracks/TRACK-102-compound-word-division-headers.md
conductor/tracks/TRACK-103-standalone-back-boilerplate-filter.md
conductor/tracks/TRACK-104-per-script-variations-handling.md

# PARENT TRACK — overall import context
conductor/tracks/TRACK-093-trinity-krithi-bulk-import.md
```

## Implementation Strategy

### TRACK-100: Multi-Pass Parsing (Critical)

**The problem:** `parse()` at line 299-300 truncates text at the first metadata boundary:
```python
lyric_window_end = effective_boundaries[0].start_pos if effective_boundaries else len(source_text)
lyric_text = source_text[:lyric_window_end]
```

All Indic scripts appear AFTER this boundary. Only the English/IAST variant is ever extracted.

**The fix — Option A (full-document language-block parsing):**

1. **Scan the entire `source_text`** for language headers to build a map of language blocks. A language block starts at a language header line and ends where the next language header begins (or at document end).

2. **The first block** (before any language header, or under `ENGLISH`/`LATIN` header) is the English/IAST block. This block is still bounded by the first metadata boundary (preserving current behavior).

3. **Each subsequent language block** (DEVANAGARI, TAMIL, TELUGU, KANNADA, MALAYALAM) is parsed independently:
   - Run `_build_blocks()` on just that block's text
   - Use metadata boundaries local to that block to exclude meanings/notes/variations
   - Run section detection and map results to the canonical skeleton from the English block

4. **Key constraint:** The existing `_extract_language_header_variants()` method already handles per-block section parsing and canonical mapping. The main change is in `parse()` — feed it the full document text instead of the truncated window.

**Approach:** Modify `parse()` to:
- Still extract the English/IAST variant from the pre-boundary window (backward compatible)
- Then scan the full `source_text` for language header blocks that appear AFTER the metadata boundary
- For each post-boundary language block, extract sections and build a `DetectedLyricVariant`
- Combine all variants into the result

**Do NOT:**
- Rewrite `_build_blocks()`, `_extract_sections()`, or `_extract_lyric_variants()` from scratch — they work correctly, they just receive truncated input
- Change the return type of `parse()` or any public API
- Break the existing single-variant path (for PDFs and documents without language headers)

### TRACK-101: Strip Numbered Labels

In `_detect_section_header()` around line 400, after the existing punctuation strip:
```python
remainder = re.sub(r"^[:\-)\]\.\s]+", "", remainder).strip()
```
Add:
```python
remainder = re.sub(r"^\d+\s*", "", remainder).strip()
```

This removes leading numbers (e.g., the `1` from `caraNam 1`) from section body text. Safe because Carnatic lyric lines never start with bare numbers after a section header match.

### TRACK-102: Compound Word Division Headers

Add compound entries to `LANGUAGE_HEADER_CANDIDATES` **before** the simple script entries (order matters — first match wins):
```python
("english - word division", "WORD_DIVISION"),
("devanagari - word division", "WORD_DIVISION"),
("tamil - word division", "WORD_DIVISION"),
# ... etc for all scripts
```

These should map to `WORD_DIVISION` (already in `METADATA_LABELS`) so the word-division blocks are treated as metadata and excluded from lyric variants. The continuous-form block is the canonical one.

### TRACK-103: Standalone "Back" Boilerplate

In `_is_boilerplate()`, add early returns:
```python
if lowered == "back":
    return True
if lowered.startswith("meaning of kriti"):
    return True
```

### TRACK-104: Per-Script Variations

After TRACK-100 is implemented, verify this works automatically. The existing `_extract_language_header_variants()` already breaks on `METADATA_LABELS` (line 688-689). If integration tests pass, no code changes needed — just add a test case confirming the behavior.

## Testing Requirements

### Test Fixtures to Create

Create realistic test fixtures in `tests/fixtures/structure_parser/` that simulate actual blog content:

1. **`dikshitar_multi_variant.txt`** — Simulates guru-guha blog structure:
   ```
   English/IAST Pallavi/Anupallavi/Charanam
   variations (metadata boundary)
   Meaning section
   Devanagari header + sections
   Devanagari - Word Division (compound header)
   Back (navigation)
   Tamil header + sections
   variations (per-script, repeated)
   Telugu header + sections
   Kannada header + sections
   Malayalam header + sections
   ```

2. **`tyagaraja_multi_variant.txt`** — Simulates thyagaraja blog with P/A/C abbreviations:
   ```
   P (Pallavi text)
   A (Anupallavi text)
   C (Charanam text)
   Gist (metadata boundary)
   Notes
   Devanagari sections (प./अ./च. headers)
   Tamil sections
   Telugu sections
   Kannada sections
   Malayalam sections
   ```

3. **`syama_sastri_numbered.txt`** — Simulates syamakrishna blog with numbered charanams:
   ```
   pallavi (text)
   anupallavi (text)
   caraNam 1 (text — verify no leading "1" in body)
   caraNam 2 (text)
   caraNam 3 (text)
   Gist (metadata boundary)
   Devanagari sections
   Tamil sections
   Telugu sections
   ```

Each fixture needs a corresponding `.expected.json` with:
```json
{
  "sections": ["PALLAVI", "ANUPALLAVI", "CHARANAM", ...],
  "variantScripts": ["latin", "devanagari", "tamil", "telugu", "kannada", "malayalam"],
  "variantLanguages": ["en", "sa", "ta", "te", "kn", "ml"],
  "metadataBoundaryLabels": ["VARIATIONS", "MEANING", ...]
}
```

### Test Cases to Add

Add these tests to `test_structure_parser.py`:

```python
# TRACK-100: Multi-variant extraction
def test_fixture_dikshitar_multi_variant():
    """Dikshitar blog produces 6 lyric variants (en + 5 Indic scripts)."""

def test_fixture_tyagaraja_multi_variant():
    """Tyagaraja blog with P/A/C abbreviations produces multi-script variants."""

def test_fixture_syama_sastri_numbered():
    """Syama Sastri numbered charanams produce clean section text."""

def test_multi_variant_canonical_mapping():
    """Each Indic variant's sections map to the English canonical skeleton."""

# TRACK-101: Numbered label stripping
def test_numbered_charanam_text_has_no_leading_number():
    """'caraNam 1\\nlyric text' → section text is 'lyric text', not '1\\nlyric text'."""

def test_numbered_swara_sahitya_text_clean():
    """'svara sAhitya 2\\nswara text' → section text is 'swara text'."""

# TRACK-102: Compound headers
def test_compound_word_division_header_detected():
    """'English - Word Division' is detected as WORD_DIVISION, not ENGLISH."""

def test_compound_devanagari_word_division():
    """'Devanagari - Word Division' is WORD_DIVISION metadata."""

# TRACK-103: Boilerplate
def test_standalone_back_is_boilerplate():
    """Standalone 'Back' line is filtered as boilerplate."""

def test_meaning_of_kriti_is_boilerplate():
    """'Meaning of Kriti-1' navigation link is filtered."""

# TRACK-104: Per-script variations
def test_per_script_variations_excluded_from_lyrics():
    """'variations' block within a language section doesn't appear in variant text."""
```

### Running Tests

```bash
cd tools/krithi-extract-enrich-worker
python -m pytest tests/test_structure_parser.py -v
```

All existing tests must pass. Run the full test suite after all changes:
```bash
python -m pytest tests/ -v
```

## Quality Constraints

1. **Minimal diff** — change only what's necessary. Don't refactor working code, don't add docstrings to unchanged methods, don't reorganize imports.

2. **Backward compatibility** — documents without language headers (PDFs, single-language text) must continue to use the existing `_extract_script_split_variants()` path. The multi-pass logic only activates when language headers are detected in the post-boundary region.

3. **No new dependencies** — use only stdlib (`re`, `dataclasses`, `typing`, `logging`).

4. **Preserve the parse contract** — `StructureParseResult` fields (`sections`, `lyric_variants`, `metadata_boundaries`) keep their semantics. `sections` is the canonical skeleton from the English/IAST block. `lyric_variants` now contains multiple entries instead of one.

5. **Edge cases to handle:**
   - Document with no language headers at all → existing behavior (single variant from script detection)
   - Document with language headers but no metadata boundary → parse entire document as language blocks
   - Language header appears in first 200 chars → not affected by the early-boundary guard (that's for metadata boundaries only)
   - Empty language block (header with no content) → skip, don't create an empty variant

## Verification Checklist

Before declaring done:

- [ ] `python -m pytest tests/test_structure_parser.py -v` — all pass (existing + new)
- [ ] `python -m pytest tests/ -v` — full suite passes
- [ ] Dikshitar fixture: 6 variants extracted (en, sa, ta, te, kn, ml)
- [ ] Tyagaraja fixture: multi-variant extraction with P/A/C abbreviation sections
- [ ] Syama Sastri fixture: numbered charanams have clean text (no leading digits)
- [ ] No "Back" navigation text in any variant's section text
- [ ] No "Word Division" blocks creating duplicate language variants
- [ ] Per-script "variations" content excluded from lyric sections
- [ ] Single-variant documents (PDFs) still work — test `test_fixture_kotlin_parity_multiscript` passes

## Output Deliverables

1. **Modified file:** `tools/krithi-extract-enrich-worker/src/structure_parser.py`
2. **New fixtures:** `tests/fixtures/structure_parser/dikshitar_multi_variant.txt` + `.expected.json`
3. **New fixtures:** `tests/fixtures/structure_parser/tyagaraja_multi_variant.txt` + `.expected.json`
4. **New fixtures:** `tests/fixtures/structure_parser/syama_sastri_numbered.txt` + `.expected.json`
5. **Updated tests:** `tests/test_structure_parser.py` with all new test cases
6. **Updated TRACKs:** Mark TRACK-100 through TRACK-104 status as `Completed` in each track file and in `conductor/tracks.md`

## Commit Convention

When complete, commit with:
```
TRACK-100: Multi-pass Indic script extraction + secondary parser fixes (TRACK-101–104)

Ref: application_documentation/04-database/schema.md
```
