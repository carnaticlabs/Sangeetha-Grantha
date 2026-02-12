# Extraction & Variant Pipeline Failure Analysis

**Date**: 2026-02-10
**Analyst**: Claude (requested by Seshadri)
**Scope**: Why TRACK-053 through TRACK-058 did not comprehensively fix the extraction pipeline

## Three Sources Under Investigation

| Source | Extraction ID | Type |
|:---|:---|:---|
| mdeng.pdf (English Dikshitar) | `bfc19406-b996-4991-8bfe-312aadcde2d9` | PDF PRIMARY |
| mdskt.pdf (Sanskrit Dikshitar) | `9cb23ecf-7c8a-414c-a672-84ae4ab2a46b` | PDF ENRICH |
| Blogspot bulk import | batch `13877ff1-7125-41f2-96cf-961aed81409a`, task `46b32f29-8af1-4acd-bed8-ccdf546c11d9` | BULK_IMPORT |

## Two Sample Krithis: akhilandesvari raksha mam

| Field | c0d71beb (from mdeng.pdf) | c152ec7a (from blogspot) |
|:---|:---|:---|
| title | `akhil¯an. d. e´svari raks.a m¯am` | `akhilANDESvari raksha mAM` |
| title_normalized | `akhilan d esvari raksa mam` | `akhilandesvari raksha mam` |
| composer_id | `daa04109...` (Dikshithar) | `09c6757a...` (Dikshitar) |
| primary_language | `en` | `te` |
| lyric variants | 1 (garbled English) | 12 (en, sa, ta, te, kn, ml) |

---

## FINDING 1: Diacritic Normalisation Did Not Run on Titles (TRACK-054)

**Severity: CRITICAL**

The English PDF title was stored as `akhil¯an. d. e´svari raks.a m¯am`. The `diacritic_normalizer.py` **does** handle these sequences (macron `¯` + vowel, dot-above `˙` + consonant, acute `´` + s), but it was **never called on title text**.

### Code path analysis

In `worker.py:_extract_pdf()`:
1. `metadata_parser.parse(segment.body_text[:500], title_hint=segment.title_text)` is called
2. Inside `MetadataParser.parse()` (line 144-145): `normalize_garbled_diacritics()` is called on `metadata_text` (the body minus the title), but the **title itself** (`title_hint` parameter) is returned **unchanged** (line 139: `title = title_hint or lines[0]`)
3. The title comes from `segment.title_text`, which comes from `PageSegmenter._find_title_positions()` using raw `TextBlock.text`
4. The `PdfExtractor._extract_page()` method never calls `normalize_garbled_diacritics()` on text blocks

**Root cause**: `normalize_garbled_diacritics()` is applied to raga/tala metadata text but **NOT to the title**. The title is passed through as-is from the raw PDF extraction.

### Why the normalizer would have worked

The diacritic normalizer rules correctly handle:
- `¯a` → `ā` (rule 1: macron + a)
- `¯ı` → `ī` (rule 2: macron + i/dotless-i)
- `˙m` → `ṁ` (rule 4: dot-above + m)
- `´s` → `ś` (rule 6: acute + s)

But the title also contains **consonant-dot patterns** like `n.`, `d.`, `s.` (for ṇ, ḍ, ṣ) which are explicitly NOT handled by the normalizer — the docstring says "Rule 8 (consonant + dot) is handled via permissive regex in the parsers, not here." These dot patterns create spurious spaces in the normalized title: `akhilan. d. e` → `akhilan d e` after stripping special chars.

### Impact on title normalization

Even if `normalize_garbled_diacritics()` were applied to titles, it would fix macrons and acute but NOT the consonant-dot patterns. The Kotlin `NameNormalizationService.basicNormalize()` then:
1. Runs `java.text.Normalizer.normalize(NFD)` — decomposes Unicode
2. `\\p{M}` strip — removes combining marks (turns ā→a, ś→s, etc.)
3. `[^a-zA-Z0-9\\s]` strip — removes dots, macrons, etc.
4. Collapses whitespace

So `akhil¯an. d. e´svari raks.a m¯am` becomes `akhilan d esvari raksa mam` (spaces from `. ` sequences).
While blogspot's `akhilANDESvari raksha mAM` becomes `akhilandesvari raksha mam`.

These **never match**: different spacing (`akhilan d esvari` vs `akhilandesvari`), different consonants (`raksa` vs `raksha`).

### What was needed but not built

A **pre-normalization step** that converts PDF garbled diacritics to proper IAST Unicode **before** the title is stored or normalized:
- `n.` → `ṇ`, `d.` → `ḍ`, `s.` → `ṣ`, `t.` → `ṭ` (Rule 8, intentionally skipped)
- Then `¯a` → `ā`, `´s` → `ś`, `˙m` → `ṁ`
- Yielding: `akhilāṇḍeśvari rakṣa mām`

After Kotlin NFD normalization: `akhilandesvari raksa mam` — still won't match `akhilandesvari raksha mam` because of `ks` vs `ksh`. The `sh` in blogspot encoding is a different transliteration convention (Harvard-Kyoto uses `S` for ś, and `sh` appears in the normalized form).

**Conclusion**: Even with a perfect diacritic fix, cross-source matching requires **transliteration-aware normalization** (IAST ṣ ≈ HK sh) which neither the Python normalizer nor the Kotlin `NameNormalizationService` supports.

---

## FINDING 2: Duplicate Composer Records — NameNormalizationService Gap

**Severity: HIGH**

Two separate composer records exist for the same person:
- `daa04109...`: "Muthuswami Dikshithar" (normalized: `muthuswami dikshithar`)
- `09c6757a...`: "Muthuswami Dikshitar" (normalized: `muthuswami dikshitar`)

### Code analysis

`NameNormalizationService.normalizeComposer()` has explicit mappings for:
- `"dikshitar"` → `"muthuswami dikshitar"`
- `"muthuswami dikshitar"` → `"muthuswami dikshitar"`
- `"muthuswamy dikshitar"` → `"muthuswami dikshitar"`

But it has **no mapping for `"muthuswami dikshithar"`** (with `th`). The PDF's composer field is "Muthuswami Dikshithar" — after `basicNormalize()` this becomes `muthuswami dikshithar`, which falls through to the `else -> normalized` branch.

**Root cause**: Missing alias in the canonical mapping table. The `th` vs `t` transliteration variant for Dikshitar was not covered.

**Impact**: All 480 mdeng.pdf krithis point to a duplicate composer, and even if titles matched, `findDuplicateCandidates` uses `title_normalized` only (no `composer_id` in the call from `ExtractionResultProcessor`), so this didn't cause the deduplication failure directly — but it creates data quality issues.

---

## FINDING 3: Sanskrit PDF Extraction Produced Only 1 Result

**Severity: CRITICAL**

The mdskt.pdf extraction shows `result_count: 1` and `status: INGESTED`.

### Code analysis of PageSegmenter

`PageSegmenter._find_title_positions()` requires:
1. `block.font_size >= min_title_size` (at least 1.3× body font)
2. `block.is_bold` — **must be True**
3. `_has_metadata_nearby()` — must find raga/tala metadata below the title

For Devanagari PDFs (mdskt.pdf):
- The `is_bold` detection in `PdfExtractor._extract_page()` line 149: `is_bold = "Bold" in font_name or "bold" in font_name`. If the Sanskrit PDF uses a Devanagari font that doesn't have "Bold" in its name (e.g., `Sanskrit2003`, `Chandas`, `Uttara`), **no blocks will be marked as bold**, and `_find_title_positions()` returns empty.
- When `_find_title_positions()` returns empty, `segment()` falls back to `_single_segment()` (line 108-109), which treats the **entire ~280-page PDF as one composition**.
- This explains `result_count: 1`.

Additionally, `_has_metadata_nearby()` checks for raga/tala patterns that include Devanagari (`राग`, `ताल`), but the METADATA_LINE_PATTERN at line 62 would need to match within 100px below the title — this secondary check is moot if `is_bold` fails first.

### Verification

The `METADATA_LINE_PATTERN` does include `राग` and `ताल` (line 63-64), so Devanagari metadata detection would work if titles were found. But the **bold detection is the gatekeeper** that fails.

**Root cause**: `is_bold` detection relies on font name containing "Bold", which doesn't work for Devanagari fonts. The segmenter should also check font weight, or use font flags from PyMuPDF (`span["flags"] & 2^4` for bold).

---

## FINDING 4: Zero Variant Matches — Cascading Failure

**Severity: CRITICAL**

The `variant_match` table is completely empty (0 rows).

### Code path

`ExtractionResultProcessor.processCompletedExtractions()`:
1. Detects `extraction_intent == "ENRICH"` for mdskt.pdf → delegates to `VariantMatchingService.matchVariants()`
2. `matchVariants()` receives `extractions` list — but since the Sanskrit PDF was segmented as 1 giant composition, this list has only 1 item
3. For that 1 item, `matchSingleExtraction()` normalizes the title and calls `findDuplicateCandidates()`
4. With a single giant concatenated Devanagari text as the "title", normalization strips all Devanagari characters (Kotlin `\\p{M}` + `[^a-zA-Z0-9\\s]`), leaving gibberish or empty string
5. No match found → logged and skipped

**Root cause**: Cascading from Finding 3. The variant matching logic is correct in principle but had no properly segmented input to work with.

---

## FINDING 5: Blogspot Bulk Import — Three Separate Issues

**Severity: HIGH**

### 5a. No Source Evidence Records

The `krithi_source_evidence` table has 0 rows where `source_url LIKE '%blogspot%'`. The bulk import path (`BulkImportOrchestrationService` → `IImportService`) is a **separate code path** from the extraction pipeline. It predates the sourcing pipeline (TRACK-041+) and was never updated to write `krithi_source_evidence` records.

### 5b. Duplicate Krithis Within Blogspot Import (32 duplicates)

The blogspot import created **3 copies** of many krithis (e.g., "rAma janArdana" created at 14:11:23, 14:11:34, and 14:12:07). The timestamps are ~11 seconds apart, matching the ExtractionWorker polling interval of 10 seconds.

**Hypothesis**: The batch was submitted/restarted 3 times, or the bulk import orchestrator created 3 jobs for the same tasks. Since `findDuplicateCandidates` in the bulk import path likely has a race condition — multiple concurrent workers processing the same batch items in parallel, each checking for duplicates before the other has committed.

The batch shows `total_tasks: 482, processed_tasks: 364, succeeded_tasks: 364` with status `running` — it never completed, suggesting it was interrupted and retried.

### 5c. Duplicate Sections (9 instead of 3)

The blogspot-imported krithi (c152ec7a) has sections at order indices 1-3 and 4-6 (two complete sets of PALLAVI/ANUPALLAVI/CHARANAM). This is consistent with the bulk import running twice against the same krithi without idempotency protection on section creation.

---

## FINDING 6: Cross-Source Deduplication is Fundamentally Broken

**Severity: CRITICAL**

Only **14 out of ~480** title_normalized values match between mdeng.pdf and blogspot. The remaining ~466 don't match due to:

### Sample of mismatches

| mdeng.pdf title (garbled) | mdeng normalized | blogspot normalized | Match? |
|:---|:---|:---|:---|
| `akhil¯an. d. e´svari raks.a m¯am` | `akhilan d esvari raksa mam` | `akhilandesvari raksha mam` | NO |
| `agast¯ı´svaram` | `agastsvaram` | (not in blogspot) | N/A |
| `anantab¯alak˙rs.n. a` | `anantabalakrsn a` | (would be `anantabalakrishna`) | NO |
| `annap¯urn. e vi´s¯al¯aks.i` | `annapurn e visalaksi` | (would be `annapurne vishalakshi`) | NO |

The 14 that DO match are titles that happen to have no diacritics in the original (e.g., `bhajare re citta`, `bharati`) or where the garbling accidentally produces the same output.

### Why `findDuplicateCandidates` fails

The query at `KrithiRepository.kt:769` does exact string match:
```kotlin
query.andWhere { KrithisTable.titleNormalized eq titleNormalized }
```

No fuzzy matching, no Levenshtein, no transliteration-aware comparison. This is by design for the ExtractionResultProcessor (to avoid false matches), but it means **any difference in normalization = no match**.

---

## FINDING 7: The 14 Accidental Matches Created Source Evidence but Not Variants

The 14 krithis where mdeng.pdf titles accidentally matched blogspot titles got `krithi_source_evidence` records (from ExtractionResultProcessor line 223-235). But these are the same-source matches (mdeng.pdf matched to an existing mdeng.pdf krithi), not cross-source matches to blogspot krithis — because the blogspot krithis have no source_evidence records and were created before the mdeng.pdf extraction ran.

Actually, looking at timestamps: mdeng.pdf krithis were created at ~06:49 UTC and blogspot krithis at ~14:11 UTC. So the mdeng.pdf extraction ran first and created 480 krithis. Then the blogspot import ran later and also created new krithis (because the normalized titles don't match).

---

## Summary: Root Cause Chain

```
1. PDF Diacritic Problem (TRACK-054 gap)
   ├─ diacritic_normalizer.py is never called on title text
   ├─ Even if called, Rule 8 (consonant+dot) was deliberately skipped
   ├─ Even with Rule 8, transliteration scheme differences (IAST vs HK)
   │   mean NFD-normalized titles still won't match
   └─ RESULT: Garbled titles stored in DB, broken normalized forms

2. Composer Name Normalization Gap
   ├─ "Dikshithar" not in NameNormalizationService canonical map
   └─ RESULT: Duplicate composer record created

3. Sanskrit PDF Segmentation Failure
   ├─ PageSegmenter.is_bold check fails for Devanagari fonts
   ├─ Entire 280-page PDF treated as 1 composition
   └─ RESULT: Only 1 extraction result instead of ~484

4. Variant Matching Empty (cascading from #1 + #3)
   ├─ Only 1 Sanskrit extraction to match
   ├─ Garbled English titles can't be matched anyway
   └─ RESULT: variant_match table is empty

5. Bulk Import Race Condition
   ├─ Import batch processed 3 times (interrupted+retried?)
   ├─ No idempotency guard on krithi creation per batch
   └─ RESULT: 32 duplicate krithis within blogspot import

6. Bulk Import ↔ Extraction Pipeline Disconnect
   ├─ Bulk import does not write krithi_source_evidence
   ├─ ExtractionResultProcessor doesn't look at bulk import data
   └─ RESULT: Two parallel systems creating krithis independently
```

## Current Database State

| Metric | Value |
|:---|:---|
| Total krithis | 707 |
| Unique title_normalized | 675 |
| Duplicate krithis | 32 |
| From mdeng.pdf (Dikshithar composer) | 480 |
| From blogspot (Dikshitar composer) | 226 |
| Cross-source title matches | 14 (out of ~480 expected) |
| Sanskrit variant matches | 0 |
| Source evidence records (mdeng.pdf) | 480 |
| Source evidence records (blogspot) | 0 |
| Variant match records | 0 |
| Duplicate composer records for Dikshitar | 2 |

---

## Recommended Fixes (Priority Order)

### P0: Fix PDF Title Diacritic Normalization
1. Call `normalize_garbled_diacritics()` on the title in `MetadataParser.parse()` or in `worker.py` before building the CanonicalExtraction
2. Implement Rule 8 (consonant+dot) in `diacritic_normalizer.py`: `n.` → `ṇ`, `d.` → `ḍ`, `s.` → `ṣ`, `t.` → `ṭ`, `l.` → `ḷ`
3. Add transliteration-aware normalization in Kotlin `NameNormalizationService`: `sh` ≈ `s` (after NFD strip), `ksh` ≈ `ks`, etc.

### P0: Fix Sanskrit PDF Segmentation
1. Use PyMuPDF font flags (`span["flags"] & 16`) for bold detection instead of font name substring
2. Add fallback: if no bold fonts found, use font-size-only heuristic with metadata-proximity as confirmation

### P1: Fix Composer Name Normalization
1. Add `"muthuswami dikshithar"` → `"muthuswami dikshitar"` to canonical mapping
2. Consider fuzzy matching for composer names (Levenshtein threshold)

### P1: Add Idempotency to Bulk Import
1. Before creating a krithi in the bulk import path, check `findDuplicateCandidates(titleNormalized)`
2. Add `ON CONFLICT` or check-before-insert for section creation

### P2: Bridge Bulk Import and Extraction Pipeline
1. Have the bulk import path write `krithi_source_evidence` records
2. OR: run a one-time migration to back-fill source evidence from `import_task_run`

### P2: Add Fuzzy/Transliteration-Aware Deduplication
1. `findDuplicateCandidates` should optionally use `LIKE` or trigram similarity (`pg_trgm`) instead of exact match
2. Consider a dedicated deduplication pass that uses Levenshtein distance with a configurable threshold

### P3: Data Remediation
1. Merge the two composer records
2. Deduplicate the 32 blogspot duplicates
3. Re-extract mdeng.pdf with fixed diacritics
4. Re-extract mdskt.pdf with fixed segmentation
5. Re-run variant matching
