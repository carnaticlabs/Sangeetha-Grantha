| Metadata | Value |
|:---|:---|
| **Track ID** | TRACK-077 |
| **Title** | Devanagari-Aware Normalizer for Variant Matching |
| **Status** | Superseded |
| **Created** | 2026-03-07 |
| **Author** | Sangeetha Grantha Team |
| **Depends On** | TRACK-059 (holistic enrichment fix) |

# TRACK-077: Devanagari-Aware Normalizer for Variant Matching

## Objective

Improve Sanskrit variant matching from **285/541 (53%)** to **420+/541 (78%+)** by adding Devanagari-to-Latin normalisation that produces matching keys compatible with the existing IAST-based normalisation pipeline.

## Problem Statement

After the TRACK-059 holistic enrichment fix, the ENRICH pipeline correctly processes Sanskrit PDFs but only matches 285 of 541 extracted compositions. Analysis of the 252 missed entries (`database/analysis/missed_sanskrit_variants.csv`) reveals:

| Category | Count | Addressable? |
|:---|---:|:---|
| **normalization_gap** | 135 | Yes — this track |
| garbled_ocr | 45 | Partially — extraction-side cleanup |
| genuinely_absent | 38 | No — not in English corpus |
| pending_review | 23 | Already matched, needs manual review |
| section_label | 5 | Yes — extraction-side filter |
| prefix_mismatch | 5 | Yes — prefix stripping |
| index_entry | 1 | Yes — extraction-side filter |

The **135 normalization_gap** entries are the primary target. Sub-pattern analysis shows:

| Sub-pattern | Count | Example (Sanskrit → English) |
|:---|---:|:---|
| Word boundary / spacing | 103 | `kamalamba baja re` vs `kamalambam baja re` |
| Vowel differences | 15 | `mahalaksimi` vs `mahalaksmi` |
| Other (transpositions, conjuncts) | 17 | `ramacandarena` vs `ramacandrena` |

### Root Cause

When the Python extraction worker extracts Devanagari text from `mdskt.pdf`, it transliterates to Latin via Unicode NFD decomposition + diacritic stripping. This produces different output from the IAST-encoded `mdeng.pdf` for three reasons:

1. **Anusvāra (ṁ) handling**: Devanagari `कमलाम्बा` → `kamalamba` (no trailing m), but IAST `kamalāmbāṁ` → `kamalambam` (trailing m preserved).
2. **Conjunct splitting**: Devanagari conjuncts like `न्द्र` (ndra) sometimes decompose as `ndar` instead of `ndr` depending on font encoding.
3. **Virama-dependent vowel insertion**: Devanagari `लक्ष्मी` → `laksimi` (epenthetic i) vs IAST `lakṣmī` → `laksmi`.

## Scope

### Phase 1: Enhance TransliterationCollapse (Kotlin)

**File:** `modules/shared/domain/src/commonMain/kotlin/.../TransliterationCollapse.kt`

Add rules for Devanagari-derived Latin quirks:

```kotlin
// Phase 1 additions to RULES:
"ndar" to "ndr",    // ramacandarena → ramacandrena
"ndra" to "ndr",    // (already covered by above in most cases)

// Post-collapse normalisation (new function):
fun collapseForMatching(value: String): String {
    var result = collapse(value)
    // Strip trailing anusvāra 'm' from words (kamalambam → kamalamba)
    result = result.replace(Regex("m\\b"), "")
    // Collapse epenthetic vowels in consonant clusters (laksimi → laksmi)
    result = result.replace(Regex("([kgcjtdpb])i([kgcjtdpbnmrlvs])"), "$1$2")
    return result
}
```

**Impact:** Update `NameNormalizationService.basicNormalize()` to use `collapseForMatching()` for title normalisation.

### Phase 2: Devanagari Pre-Normalisation (Python extraction side)

**File:** `tools/krithi-extract-enrich-worker/src/diacritic_normalizer.py`

Add a `normalize_devanagari_to_iast()` function that runs before NFD decomposition:

1. Explicit virama-aware transliteration using Unicode codepoint mapping
2. Handle common Devanagari ligatures (क्ष → kṣa, ज्ञ → jña, श्र → śra)
3. Preserve anusvāra (ं → ṁ) before NFD strips it
4. Strip Devanagari digit prefixes (e.g., `३.` at start of entries)

### Phase 3: OCR Noise Prefix Stripping

**File:** `VariantMatchingService.kt` → `matchSingleExtraction()`

Before candidate search, strip known OCR noise prefixes:
- `ori ` → `` (garbled śrī from Devanagari fonts)
- `kra ` → `` (garbled prefix)
- Single-word all-caps prefix followed by Devanagari → strip prefix

**Impact:** Recovers ~5 prefix_mismatch entries.

### Phase 4: Section Label Filtering (Python extraction side)

**File:** `tools/krithi-extract-enrich-worker/src/page_segmenter.py`

Add post-segmentation filter to discard entries whose title normalises to known section labels:
- `caranam`, `pallavi`, `anupallavi`, `citaswaram`, `madyamakalasahityam`
- `anukramanika` (index/table of contents)
- Single-word entries matching raga or tala names

**Impact:** Removes ~6 false-positive extractions (5 section labels + 1 index entry).

### Phase 5: Compressed-Key Matching Enhancement

**File:** `KrithiSearchRepository.kt` → `findDuplicateCandidates()`

Currently uses exact `REPLACE(title_normalized, ' ', '')` comparison. Enhance to also try matching with trailing `m` stripped and epenthetic vowels removed:

```sql
-- Additional match: strip trailing 'm' from both sides
REGEXP_REPLACE(REPLACE(title_normalized, ' ', ''), 'm$', '')
  = REGEXP_REPLACE(:compressed, 'm$', '')
```

This gives the DB-level search a better chance of finding candidates for the fuzzy scorer.

## Expected Outcome

| Metric | Before | After |
|:---|---:|---:|
| Sanskrit variant matches | 308 | ~440 |
| Auto-approved (HIGH) | 285 | ~400 |
| Pending review (MEDIUM) | 23 | ~40 |
| Unmatched (addressable gap) | 135 | ~30 |
| Garbled OCR (extraction noise) | 45 | ~40 |

## Verification

1. `./gradlew :modules:backend:api:test` — 0 failures
2. Reset database, re-run PRIMARY + ENRICH pipeline
3. Compare `variant_match` counts against baseline (308 total, 285 auto-approved)
4. Target: ≥400 auto-approved Sanskrit variant matches
5. Regenerate `database/analysis/missed_sanskrit_variants.csv` and verify `normalization_gap` count drops from 135 to <30

## Data References

- Baseline analysis: `database/analysis/missed_sanskrit_variants.csv` (252 entries)
- English baseline: `database/analysis/missing_english_variants.csv` (1 entry)
- Source PDFs: `database/for_import/mdeng.pdf` (English/IAST), `database/for_import/mdskt.pdf` (Devanagari)
- Current normaliser: `modules/shared/domain/.../TransliterationCollapse.kt` (9 rules)
- Current matching: `VariantMatchingService.kt` → `matchSingleExtraction()`

## Risk

- **False positives**: Overly aggressive normalisation could match different compositions. Mitigated by the multi-signal scorer (title 50%, raga/tala 30%, page position 20%) and the AUTO_APPROVED threshold of 0.85.
- **Regression on English matching**: Changes to `TransliterationCollapse` affect all normalisation. Mitigated by running the full PRIMARY extraction pipeline in verification.

---

Superseded by curator-assist approach. Normalization consolidation into Python covers Phase 1 rules. Remaining gaps handled via manual curator review UI.
