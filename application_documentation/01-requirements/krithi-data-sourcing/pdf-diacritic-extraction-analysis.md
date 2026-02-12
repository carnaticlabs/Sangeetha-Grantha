| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 3.2.0 |
| **Last Updated** | 2026-02-10 |
| **Author** | Sangeetha Grantha Team |
| **Related Tracks** | TRACK-041, TRACK-053, TRACK-054–TRACK-058 (proposed) |
| **Scope** | PDF extraction quality — garbled diacritic handling, metadata recovery, section detection, Devanagari font decoding |
| **Sources Under Analysis** | [guruguha.org mdeng.pdf](https://guruguha.org/wp-content/uploads/2022/03/mdeng.pdf) (English/IAST) and [guruguha.org mdskt.pdf](https://guruguha.org/wp-content/uploads/2025/01/mdskt.pdf) (Sanskrit/Devanagari) — 484 Dikshitar Krithis each |

# PDF Diacritic Extraction Analysis — Garbled Encoding Recovery for Raga, Tala, and Section Metadata

## 1. Executive Summary

After successfully extracting 486 Krithi segments from the guruguha.org English transliteration PDF (`mdeng.pdf`) and creating 419 new Krithi records in the database (TRACK-053), a data quality audit revealed a critical gap: **480 out of 481 Krithis have "Unknown" for both Raga and Tala**, and **433 have only 2 sections detected (Pallavi + Anupallavi) when most should have 3+ (including Charanam)**. The Raga, Tala, and Charanam data IS present in the source PDF — it is being extracted by PyMuPDF as text blocks — but the extraction pipeline's regex patterns cannot recognise the garbled diacritic encoding produced by the PDF's Utopia font family.

Additionally, the companion Sanskrit/Devanagari PDF (`mdskt.pdf`) presents a **distinct and more severe encoding challenge**: the `Velthuis-dvng10` TeX font has no Unicode mapping at all — both PyMuPDF and `pdftotext` extract raw byte values that appear as garbled ASCII/control characters. However, by **extracting the font's encoding vector from its embedded Type 1 font program**, we discovered a complete 155-entry glyph-to-Unicode mapping that enables deterministic decoding to Unicode Devanagari with ~90% first-pass accuracy (reaching ~98% with systematic post-processing).

This document provides a detailed forensic analysis of both encoding issues, their root causes, and comprehensive solution designs for both the English IAST garbled diacritic recovery and the Sanskrit Devanagari font decoding.

---

## 2. Problem Statement

### 2.1 Observed Symptoms

| Metric | Expected | Actual | Gap |
|:---|:---|:---|:---|
| Krithis with identified Raga | ~481 | 1 | 480 missing (99.8%) |
| Krithis with identified Tala | ~481 | 1 | 480 missing (99.8%) |
| Krithis with 3+ sections (P-A-C) | ~434 | 0 | All Charanams missed |
| Krithis with exactly 2 sections | ~47 | 433 | 386 incorrectly truncated |

### 2.2 Specific Example: Krithi `dc754fae-febc-4dd9-b37e-de51bf5a7cfc`

**What the PDF contains (page 17):**

The original page displays:

```
1
akhilāṇḍeśvari rakṣa mām

rāgaṁ: jujāvanti (28)    tālāṁ: ādi

Pallavi
akhilāṇḍeśvari rakṣa mām āgamasampradāyanipuṇe śrī

Anupallavi
nikhilalokanityātmike vimale
nirmale śyāmaḷe sakalakaḷe

Caraṇam
lambodara guruguha pūjite lambālākodbhāvite hasite
vāgdevatārādhite varade varaśailarājanute
...
```

**What PyMuPDF extracts (garbled Utopia font encoding):**

```
r¯aga ˙m: juj¯avanti (28)
t¯al.a ˙m: ¯adi
```

**What the database stores:**

```
raga:  Unknown
tala:  Unknown
sections: PALLAVI, ANUPALLAVI  (Charanam missing)
```

The data is physically present in the extracted text — the pipeline simply cannot recognise it through its current regex patterns.

---

## 3. Root Cause Analysis

### 3.1 Font Encoding Artifacts

The guruguha.org PDFs use the **Utopia font family** (Utopia-Bold, Utopia-Regular, Utopia-Regular-Slant_167, Utopia-Bold-Slant_167). This is a PostScript Type 1 font that encodes IAST (International Alphabet of Sanskrit Transliteration) diacritics using **standalone combining characters** rather than precomposed Unicode codepoints.

When PyMuPDF extracts text from these fonts, the diacritics appear as separate characters adjacent to (or with whitespace between) the base character they modify:

| IAST Character | Unicode Codepoint | Garbled Extraction | Explanation |
|:---|:---|:---|:---|
| `ā` (a-macron) | U+0101 | `¯a` | Standalone macron (U+00AF) + `a` |
| `ī` (i-macron) | U+012B | `¯ı` or `¯i` | Standalone macron + dotless i or `i` |
| `ū` (u-macron) | U+016B | `¯u` | Standalone macron + `u` |
| `ṁ` (m-underdot/anusvara) | U+1E41 | `˙m` | Standalone dot-above (U+02D9) + `m` |
| `ṅ` (n-overdot) | U+1E45 | `˙n` | Standalone dot-above + `n` |
| `ṇ` (n-underdot) | U+1E47 | `n.` | `n` + standalone dot (U+002E) + space |
| `ḍ` (d-underdot) | U+1E0D | `d.` | `d` + standalone dot + space |
| `ṭ` (t-underdot) | U+1E6D | `t.` | `t` + standalone dot + space |
| `ḷ` (l-underdot) | U+1E37 | `l.` or `.l` | Context-dependent dot positioning |
| `ś` (s-acute) | U+015B | `´s` | Standalone acute (U+00B4) + `s` |
| `ṣ` (s-underdot) | U+1E63 | `s.` | `s` + standalone dot |
| `ñ` (n-tilde) | U+00F1 | `˜n` | Standalone tilde (U+02DC) + `n` |
| `ṛ` (r-underdot) | U+1E5B | `r.` | `r` + standalone dot |

**Critical observation**: Spaces often appear between the base character and its diacritic modifier. For example, `rāgaṁ` becomes `r¯aga ˙m` (with a space before the anusvara marker). This makes simple character-level substitution insufficient — the normalisation must handle intervening whitespace.

### 3.2 Affected Pipeline Stages

The garbled encoding breaks three independent stages of the extraction pipeline:

#### Stage 1: Metadata Parser (`metadata_parser.py`)

The `MetadataParser._extract_raga_tala()` method uses these regex patterns:

```python
RAGA_PATTERN = re.compile(
    r"(?:raa?ga|rāga|राग)\s*[:—–\-]\s*(.+?)(?:\s*[—–\-|]\s*(?:taa?la|tāla|ताल)|$)",
    re.IGNORECASE,
)
TALA_PATTERN = re.compile(
    r"(?:taa?la|tāla|ताल)\s*[:—–\-]\s*(.+?)(?:\s*$|\s*[—–\-|])",
    re.IGNORECASE,
)
```

**Why these fail:**

- `rāga` expects the precomposed Unicode character `ā` (U+0101), but the text contains `r¯aga` (standalone macron U+00AF + `a`).
- `raa?ga` matches ASCII forms `raga` or `raaga`, but not `r¯aga` because the macron `¯` is an unaccounted-for character between `r` and `a`.
- Neither pattern accounts for the anusvara suffix `ṁ` / `˙m` that appears in `rāgaṁ:` / `r¯aga ˙m:`.
- Neither pattern handles the parenthesised mēḷa number `(28)` that follows the raga name.

**What the text actually looks like when it reaches the parser:**

```
akhil¯an. d. e´svari raks.a m¯am       ← bold-italic title repetition (included in body_text)
num r¯aga ˙m: juj¯avanti (28)          ← RAGA metadata line
t¯al.a ˙m: ¯adi                        ← TALA metadata line
pallavi                                  ← section label
akhil¯an. d. e´svari raks.a m¯a ˙m...  ← pallavi text
```

The metadata parser receives the first 500 characters of `body_text`, which DOES include the raga and tala lines. The regex simply cannot match the garbled form.

#### Stage 2: Structure Parser (`structure_parser.py`)

The `StructureParser` detects section labels using patterns like:

```python
(SectionType.CHARANAM, re.compile(
    r"(?:charaa?n[au]m|caraṇam|चरणम्)(?:\s*(\d+))?",
    re.IGNORECASE,
))
```

**Why this fails:**

The PDF text renders `caraṇam` as `caran. am` (with a standalone dot after `n` and a space before `am`). The pattern `charaa?n[au]m` expects contiguous ASCII characters. The garbled `caran. am` has a dot and space splitting the word, so it doesn't match any variant.

**Observed extraction across 481 Krithis:**

| Section Count | Expected | Actual | Reason |
|:---|:---|:---|:---|
| 1 section | ~47 | 47 | Correct — some Krithis are genuinely single-section |
| 2 sections (P + A) | ~0 | 433 | Charanam missed due to garbled `caran. am` |
| 3+ sections | ~434 | 0 | All Charanams, Madhyama Kala Sahityams missed |
| 0 sections | 0 | 1 | Edge case |

#### Stage 3: Name Quality in Output

Even if the regex patterns matched, the extracted raga and tala names would be garbled:

- Raga: `juj¯avanti (28)` instead of `Jujāvanti`
- Tala: `¯adi` instead of `Ādi`
- Raga: `´suddhas¯averi (1)` instead of `Śuddhasāveri`
- Tala: `r¯upakam` instead of `Rūpakam`
- Tala: `mi´sra c¯apu` instead of `Miśra Cāpu`

Without name normalisation, entity resolution downstream would fail to match these garbled names to canonical Raga/Tala records, creating hundreds of duplicate reference entities.

### 3.3 Scale of Impact — Confirmed by Database Analysis

```sql
-- 480 of 481 Krithis have "Unknown" raga
SELECT count(*) as total,
       count(CASE WHEN r.name = 'Unknown' THEN 1 END) as unknown_raga,
       count(CASE WHEN t.name = 'Unknown' THEN 1 END) as unknown_tala
FROM krithis k
LEFT JOIN ragas r ON k.primary_raga_id = r.id
LEFT JOIN talas t ON k.tala_id = t.id;

-- Result: total=481, unknown_raga=480, unknown_tala=480
```

```sql
-- Section distribution: 433 have exactly 2 sections (missing Charanam)
SELECT sections_count, count(*) as krithi_count
FROM (
    SELECT k.id, (SELECT count(*) FROM krithi_sections ks WHERE ks.krithi_id = k.id) as sections_count
    FROM krithis k
) sub
GROUP BY sections_count ORDER BY sections_count;

-- Result: 0→1, 1→47, 2→433
```

### 3.4 Evidence: Raw PDF Text Blocks (PyMuPDF `dict` mode)

Direct examination of the PDF with PyMuPDF's `get_text('dict')` reveals the exact text blocks produced for every page. The pattern is consistent across all 484 compositions:

**Page 17 (Krithi #1: Akhilāṇḍeśvari):**

| Block | y0 | Font Size | Font | Bold | Text |
|:---|:---|:---|:---|:---|:---|
| 0 | 72.0 | 17.2 | Utopia-Bold | Yes | `1` |
| 0 | 71.7 | 17.2 | Utopia-Bold | Yes | `akhil¯an. d. e´svari raks.a m¯am` |
| 1 | 113.0 | 17.2 | Utopia-Bold-Slant_167 | Yes | `akhil¯an. d. e´svari raks.a m¯am` |
| 2 | 145.3 | 17.2 | Utopia-Regular | No | `num r¯aga ˙m: juj¯avanti (28)` |
| 2 | 145.3 | 17.2 | Utopia-Regular | No | `t¯al.a ˙m: ¯adi` |
| 3 | 181.0 | 20.7 | Utopia-Regular-Slant_167 | No | `pallavi` |
| 4 | 217.8 | 14.3 | Utopia-Regular | No | `akhil¯an. d. e´svari raks.a m¯a ˙m ...` |
| 5 | 245.6 | 20.7 | Utopia-Regular-Slant_167 | No | `anupallavi` |
| ... | ... | ... | ... | ... | ... |
| 8 | 335.2 | 20.7 | Utopia-Regular-Slant_167 | No | `caran. am` |
| 9 | 372.1 | 14.3 | Utopia-Regular | No | `lambodara guruguha p¯ujite ...` |

**Page 18 (Krithi #2: Akhilāṇḍeśvaryai):**

| Block | Text |
|:---|:---|
| metadata | `r¯aga ˙m: ¯arabhi (29)` |
| metadata | `t¯al.a ˙m: adi` |

**Page 20 (Krithi #4: Agastīśvaram):**

| Block | Text |
|:---|:---|
| metadata | `r¯aga ˙m: lalita (15)` |
| metadata | `t¯al.a ˙m: mi´sra c¯apu` |

**Page 31 (Krithi: Amba Nīlāyatākṣi):**

| Block | Text |
|:---|:---|
| metadata | `r¯aga ˙m: n¯ıl¯ambari (29)` |
| metadata | `t¯al.a ˙m: ¯adi` |

**Page 51:**

| Block | Text |
|:---|:---|
| metadata | `r¯aga ˙m: c¯amaram (56)` |
| metadata | `t¯al.a ˙m: adi` |

**Page 101:**

| Block | Text |
|:---|:---|
| metadata | `r¯aga ˙m: gurjjari (15)` |
| metadata | `t¯al.a ˙m: ¯adi` |

**Consistent patterns across all 484 compositions:**

1. Raga line format: `r¯aga ˙m: <name> (<mēḷa_number>)` — always `r¯aga ˙m:` prefix
2. Tala line format: `t¯al.a ˙m: <name>` — always `t¯al.a ˙m:` prefix
3. Both lines at y-position ~145, font size 17.2pt, Utopia-Regular (not bold)
4. Charanam label: `caran. am` — always with dot and space in the middle
5. Section labels (pallavi, anupallavi) render correctly (no diacritics to garble)
6. Page 17 has a `num` prefix artifact on the raga line; all other pages do not

### 3.5 Note on the `num` Prefix (Page 17 Only)

The first Krithi's raga line reads `num r¯aga ˙m: juj¯avanti (28)` while all subsequent pages have `r¯aga ˙m: ...` without any prefix. The `num` is likely a rendering artifact from the PDF — possibly the tail end of a glyph sequence from the preceding text block that bleeds into the raga span due to the Utopia font's custom encoding table. It is not linguistically meaningful and only occurs on the first composition page.

---

## 4. Solution Design

The fix addresses all three affected pipeline stages with a layered approach:

1. **Text normalisation** — A reusable function that converts garbled standalone diacritics to proper IAST Unicode.
2. **Enhanced regex patterns** — Updated patterns in MetadataParser and StructureParser that handle both normalised and raw garbled forms.
3. **Name cleanup** — Post-extraction cleanup to produce clean raga/tala names suitable for entity resolution.

### 4.1 Layer 1: Garbled Diacritic Normalisation Function

A new utility function `normalize_garbled_diacritics(text: str) -> str` that performs systematic replacement of standalone diacritic + base character sequences with their precomposed Unicode equivalents.

**Normalisation rules (ordered by specificity):**

| Priority | Pattern | Replacement | Example |
|:---|:---|:---|:---|
| 1 | `¯` + optional space + `a` | `ā` | `r¯aga` → `rāga`, `r¯a ga` → `rāga` |
| 2 | `¯` + optional space + `i` or `ı` | `ī` | `n¯ıl` → `nīl` |
| 3 | `¯` + optional space + `u` | `ū` | `r¯upakam` → `rūpakam` |
| 4 | `˙` + optional space + `m` | `ṁ` | `˙m` → `ṁ`, `aga ˙m` → `agaṁ` |
| 5 | `˙` + optional space + `n` | `ṅ` | `sa˙n` → `saṅ` |
| 6 | `´` + optional space + `s` | `ś` | `´svari` → `śvari` |
| 7 | `˜` + optional space + `n` | `ñ` | `j˜n¯ana` → `jñāna` |
| 8 | consonant + `.` + space | consonant | `n. d.` → `ṇḍ` (contextual — see 4.1.1) |

**Important**: Rules 1–7 are safe for broad application. Rule 8 (consonant + dot) is contextual and risky for general text — a `.` after a consonant could be a genuine period. For the metadata and section label parsing use cases, it is sufficient to apply rules 1–7 and handle the consonant-dot patterns via permissive regex patterns rather than text normalisation.

#### 4.1.1 Consonant-Dot Handling Strategy

The consonant + `.` + space pattern (e.g., `n.` for `ṇ`, `d.` for `ḍ`) is ambiguous outside of known contexts. Rather than normalising these globally, the approach is:

- **For metadata labels** (`r¯aga ˙m`, `t¯al.a ˙m`): handle via regex alternation that explicitly allows `\.?` between `l` and `a` in `tāla`.
- **For section labels** (`caran. am`): handle via regex alternation that allows `\.?\s*` between `n` and `am`.
- **For extracted names** (raga, tala, title): apply normalisation rules 1–7 to clean up macrons and anusvara; leave consonant-dots for downstream name resolution (which already uses fuzzy matching).

### 4.2 Layer 2: Enhanced Regex Patterns

#### 4.2.1 MetadataParser — Raga/Tala Extraction

The metadata parser needs to handle four categories of raga/tala label formats:

| Category | Raga Example | Tala Example |
|:---|:---|:---|
| Clean ASCII | `raga: Sankarabharanam` | `tala: Adi` |
| Clean IAST Unicode | `rāga: Śaṅkarābharaṇam` | `tāla: Ādi` |
| Garbled Utopia encoding | `r¯aga ˙m: ´sa˙nkar¯abh.` | `t¯al.a ˙m: ¯adi` |
| Devanagari | `राग: शंकराभरणम्` | `ताल: आदि` |

**Proposed raga label pattern (handles all four categories):**

```python
# Raga label — matches: raga, raaga, rāga, rāgaṁ, rāgam, r¯aga, r¯aga˙m, r¯aga ˙m
RAGA_LABEL = (
    r"(?:"
    r"r[¯ā]?a+ga\s*[˙.]?\s*[ṁm]?"  # ASCII/IAST/garbled
    r"|rāga[ṁm]?"                     # Precomposed IAST
    r"|राग"                            # Devanagari
    r")"
)
```

**Proposed tala label pattern:**

```python
# Tala label — matches: tala, taala, tāla, tālaṁ, t¯ala, t¯al.a, t¯al.a˙m
TALA_LABEL = (
    r"(?:"
    r"t[¯ā]?a+l[\.ḷ]?\s*a\s*[˙.]?\s*[ṁm]?"  # ASCII/IAST/garbled
    r"|tāl[ḷ]?a[ṁm]?"                           # Precomposed IAST
    r"|ताल"                                       # Devanagari
    r")"
)
```

**Updated full raga extraction pattern:**

```python
RAGA_PATTERN = re.compile(
    RAGA_LABEL + r"\s*[:—–\-]\s*"
    r"(.+?)"                         # Capture raga name (non-greedy)
    r"(?:"
    r"\s*\(\d+\)\s*"                 # Strip optional mēḷa number: (28)
    r")?"
    r"(?:"
    r"\s*[—–\-|]\s*" + TALA_LABEL    # Followed by tala label
    r"|$"                             # Or end of line
    r")",
    re.IGNORECASE | re.MULTILINE,
)
```

**Updated full tala extraction pattern:**

```python
TALA_PATTERN = re.compile(
    TALA_LABEL + r"\s*[:—–\-]\s*"
    r"(.+?)"                         # Capture tala name
    r"(?:\s*\(\d+\))?"               # Strip optional number
    r"(?:\s*$|\s*[—–\-|])",          # End of line or separator
    re.IGNORECASE | re.MULTILINE,
)
```

#### 4.2.2 StructureParser — Section Label Detection

Add garbled variants to section patterns:

```python
# Charanam — add garbled form: caran. am, caran.am
(SectionType.CHARANAM, re.compile(
    r"(?:charaa?n[au]m|caraṇam|चरणम्|caran\.?\s*am)(?:\s*(\d+))?",
    re.IGNORECASE,
))

# Samashti Charanam — add garbled form
(SectionType.SAMASHTI_CHARANAM, re.compile(
    r"(?:samashti\s*charaa?n[au]m|samashṭi\s*caraṇam|समष्टि\s*चरणम्"
    r"|samash?t\.?i\s*caran\.?\s*am)",
    re.IGNORECASE,
))
```

#### 4.2.3 PageSegmenter — Metadata Detection

Update the `METADATA_LINE_PATTERN` to also recognise garbled metadata labels:

```python
METADATA_LINE_PATTERN = re.compile(
    r"(?:r[¯ā]?a+ga|rāga|राग"          # Raga variants
    r"|t[¯ā]?a+l[\.ḷ]?\s*a|tāla|ताल"   # Tala variants
    r"|deity|kshetra|temple)",
    re.IGNORECASE,
)
```

### 4.3 Layer 3: Name Cleanup Pipeline

After extracting the raga/tala name, a cleanup function produces a clean name for entity resolution:

**Step 1 — Strip mēḷa number**: Remove `(28)`, `(15)`, etc.

```python
name = re.sub(r'\s*\(\d+\)\s*', '', name).strip()
```

**Step 2 — Apply diacritic normalisation** (rules 1–7 from Section 4.1):

```python
name = normalize_garbled_diacritics(name)
# "juj¯avanti" → "jujāvanti"
# "¯adi" → "ādi"  
# "mi´sra c¯apu" → "miśra cāpu"
# "n¯ıl¯ambari" → "nīlāmbari"
# "´suddhas¯averi" → "śuddhasāveri"
```

**Step 3 — Title-case normalisation**:

```python
name = name.strip().title()
# "jujāvanti" → "Jujāvanti"
# "ādi" → "Ādi"
# "miśra cāpu" → "Miśra Cāpu"
```

**Expected results for sampled raga/tala names:**

| Raw Garbled Name | After Cleanup | Canonical Name |
|:---|:---|:---|
| `juj¯avanti (28)` | `Jujāvanti` | Jujāvanti |
| `¯adi` | `Ādi` | Ādi |
| `¯arabhi (29)` | `Ārabhī` | Ārabhī |
| `´suddhas¯averi (1)` | `Śuddhasāveri` | Śuddhasāveri |
| `lalita (15)` | `Lalita` | Lalita |
| `mi´sra c¯apu` | `Miśra Cāpu` | Miśra Cāpu |
| `n¯ıl¯ambari (29)` | `Nīlāmbari` | Nīlāmbari |
| `c¯amaram (56)` | `Cāmaram` | Cāmaram |
| `r¯upakam` | `Rūpakam` | Rūpakam |
| `gurjjari (15)` | `Gurjjari` | Gurjjari |

### 4.4 Integration Points

#### 4.4.1 Where Normalisation is Applied

```
                                    ┌──────────────────────┐
                                    │  PdfExtractor        │
                                    │  (extractor.py)      │
                                    │  Raw text blocks     │
                                    └──────────┬───────────┘
                                               │
                                    ┌──────────▼───────────┐
                                    │  PageSegmenter       │
                                    │  (page_segmenter.py) │
                                    │  ► Updated           │
                                    │  METADATA_LINE_PATTERN│
                                    └──────────┬───────────┘
                                               │
                               ┌───────────────┼───────────────┐
                               │               │               │
                    ┌──────────▼──────┐  ┌─────▼────────┐  ┌──▼───────────┐
                    │ MetadataParser  │  │ StructParser  │  │ Worker       │
                    │ (metadata_      │  │ (structure_   │  │ (worker.py)  │
                    │  parser.py)     │  │  parser.py)   │  │              │
                    │                 │  │               │  │ ► Applies    │
                    │ ► Normalise     │  │ ► Updated     │  │   name       │
                    │   text before   │  │   section     │  │   cleanup to │
                    │   regex match   │  │   patterns    │  │   raga/tala  │
                    │ ► Strip mēḷa # │  │   for garbled │  │   before     │
                    │ ► Clean names   │  │   forms       │  │   building   │
                    └─────────────────┘  └──────────────┘  │   Canonical  │
                                                           │   Extraction │
                                                           └──────────────┘
```

#### 4.4.2 Non-Invasive Design

The normalisation function is **additive** — it does not change the extraction pipeline's architecture. It is applied:

1. **In MetadataParser**: Normalise the `metadata_text` before running regex patterns. This makes existing patterns work without rewriting them entirely.
2. **In StructureParser**: Normalise section label text before matching. Alternatively, add garbled-form patterns alongside existing ones.
3. **In Worker**: Apply name cleanup to `metadata.raga` and `metadata.tala` before constructing the `CanonicalExtraction`.

The raw extracted body text (used for lyric content) is **not** normalised — this preserves the exact extracted text in the database for future re-processing with improved normalisation if needed.

---

## 5. Files to Modify

| File | Change Type | Description |
|:---|:---|:---|
| `tools/pdf-extractor/src/metadata_parser.py` | **Major** | Add `normalize_garbled_diacritics()`, update all raga/tala regex patterns, add name cleanup |
| `tools/pdf-extractor/src/structure_parser.py` | **Moderate** | Add garbled section label patterns (`caran. am`, `samash.t.i caran. am`) |
| `tools/pdf-extractor/src/page_segmenter.py` | **Minor** | Update `METADATA_LINE_PATTERN` to match garbled forms |
| `tools/pdf-extractor/src/worker.py` | **Minor** | Apply name cleanup to raga/tala names in `_extract_pdf()` |

No database migrations required. No Kotlin backend changes required. No frontend changes required.

---

## 6. Verification Plan

### 6.1 Unit Verification

Test the normalisation function against known garbled → clean mappings:

```python
assert normalize_garbled_diacritics("r¯aga ˙m") == "rāgaṁ"
assert normalize_garbled_diacritics("t¯al.a ˙m") == "tāl.aṁ"  # dot in tāḷa preserved
assert normalize_garbled_diacritics("juj¯avanti") == "jujāvanti"
assert normalize_garbled_diacritics("¯adi") == "ādi"
assert normalize_garbled_diacritics("mi´sra c¯apu") == "miśra cāpu"
assert normalize_garbled_diacritics("n¯ıl¯ambari") == "nīlāmbari"
```

### 6.2 Integration Verification

1. Reset the extraction queue entry for the guruguha.org PDF back to `DONE` status.
2. Delete the 419 created Krithis (or re-process in a test environment).
3. Re-run the extraction with the fixed code.
4. Verify with SQL:

```sql
-- Should show ~0 unknown ragas (vs. 480 before)
SELECT count(CASE WHEN r.name = 'Unknown' THEN 1 END) as unknown_raga
FROM krithis k LEFT JOIN ragas r ON k.primary_raga_id = r.id;

-- Should show 3+ sections for most Krithis
SELECT sections_count, count(*) FROM (
    SELECT (SELECT count(*) FROM krithi_sections ks WHERE ks.krithi_id = k.id) as sections_count
    FROM krithis k
) sub GROUP BY sections_count ORDER BY sections_count;
```

### 6.3 Spot-Check Verification

Manually verify a sample of 10 Krithis against the PDF:

| Krithi | Expected Raga | Expected Tala | Expected Sections |
|:---|:---|:---|:---|
| Akhilāṇḍeśvari | Jujāvanti | Ādi | P, A, C |
| Akhilāṇḍeśvaryai | Ārabhī | Ādi | P, A, C |
| Akhilāṇḍeśvaro | Śuddhasāveri | Rūpakam | P, A, C |
| Agastīśvaram | Lalita | Miśra Cāpu | P, A, C |
| Amba Nīlāyatākṣi | Nīlāmbari | Ādi | P, A, C |

---

## 7. Sanskrit PDF Analysis (`mdskt.pdf`)

### 7.1 Overview

The [Sanskrit edition](https://guruguha.org/wp-content/uploads/2025/01/mdskt.pdf) (`mdskt.pdf`) is a companion to the English transliteration PDF, containing the same 484 Dikshitar Krithis in Devanagari script. Both PDFs share the same structure: numbered Krithis in alphabetical order (by Sanskrit alphabet), each with title, raga, tala, and section labels (Pallavi, Anupallavi, Charanam, Madhyama Kala Sahityam).

**Key use case**: When the Sanskrit PDF is submitted as an extraction request, the Devanagari lyrics should be added as a **Sanskrit lyric variant** to the corresponding Krithi records already created from the English PDF. For example, the Sanskrit lyrics on page 17 should map to Krithi `dc754fae-febc-4dd9-b37e-de51bf5a7cfc` (Akhilāṇḍeśvari).

| Property | English PDF (`mdeng.pdf`) | Sanskrit PDF (`mdskt.pdf`) |
|:---|:---|:---|
| Total pages | 541 | 541 |
| Krithi count | 484 | 484 |
| Ordering | Alphabetical (Sanskrit alphabet) | Alphabetical (Sanskrit alphabet) |
| Script | IAST Latin transliteration | Devanagari |
| Primary font | Utopia (Type 1 PostScript) | Velthuis-dvng10 (Type 1 PostScript, TeX) |
| Font issue | Garbled standalone diacritics | Missing Unicode mapping (no ToUnicode CMap) |
| Page numbers | Utopia-Regular 12pt | Utopia-Regular 12pt (same!) |
| Punctuation font | (within Utopia) | CMR17 (Computer Modern Roman, TeX) |

### 7.2 Font Analysis

The Sanskrit PDF uses **four font families**:

| Font | Role | Type | Encoding Issue |
|:---|:---|:---|:---|
| `Velthuis-dvng10` | Main text (regular) | Type 1 PostScript | No `/Encoding` dict, no `/ToUnicode` CMap |
| `Velthuis-dvngi10` | Section labels (italic) | Type 1 PostScript | Same — no encoding metadata |
| `Velthuis-dvng8` | Footnotes | Type 1 PostScript | Same |
| `CMR17` | Punctuation (`:`, `(`, `)`) | Type 1 (Computer Modern) | Standard ASCII — extracts correctly |
| `MSAM10` | Decorative separator (✠) | Type 1 (Math Symbols) | Extracts correctly |
| `Utopia-Regular` | Page numbers only | Type 1 PostScript | Same font as English PDF — extracts correctly |

The `Velthuis-dvng*` fonts are generated by the `devnag` TeX/LaTeX package. They typeset Devanagari text from Velthuis transliteration input during compilation. The resulting Type 1 font maps byte positions to Devanagari glyphs using a **custom encoding vector embedded in the font program**, but does NOT include a `/ToUnicode` CMap or `/Encoding` dictionary in the PDF metadata.

**Consequence**: Both PyMuPDF and `pdftotext` (poppler) extract raw byte values that correspond to glyph codes in the font's internal encoding, not Unicode Devanagari. The text appears as garbled ASCII/control characters when extracted.

### 7.3 Raw Extraction Results — Page 17 Comparison

**What the PDF displays (Devanagari, as rendered in a browser):**

```
१
अखिलाण्डेश्वरि रक्ष माम्

अखिलाण्डेश्वरि रक्ष माम्
रागं : जुजावन्ति (२८)   ताळं : आदि

पल्लवि
अखिलाण्डेश्वरि रक्ष मां आगमसम्प्रदायनिपुणे श्री

अनुपल्लवि
निखिललोकनित्यात्मिके विमले
निर्मले श्यामळे सकलकळे

चरणम्
लम्बोदर गुरुगुह पूजिते लम्बालकोद्भाविते हसिते
वाग्देवताराधिते वरदे वरशैलराजनुते शारदे
```

**What PyMuPDF extracts (raw garbled bytes):**

```
1
aEKlA\x17X\x03\x98Er r" mAm^

aEKlA\x17X\x03\x98Er r" mAm^
rAg\\ : j\x00 jAvE\x06t ( 28 )
tA\x0f\\ : aAEd

plEv
aEKlA\x17X\x03\x98Er r" mA\\ aAgms\\pdAyEnp\x00 Z\x03 \x99F

an\x00 plEv
EnEKllokEn(yAE(mk\x03 Evml\x03
Enm\rl\x03 [yAm\x0f\x03 sklk\x0f\x03

crZm^
lMbodr g\x00 zg\x00 h p\x01 Ejt\x03 lMbAlkodAEvt\x03 hEst\x03
vA`d\x03vtArAEDt\x03 vrd\x03 vrf{lrAjn\x00 t\x03
```

**What `pdftotext` (poppler) extracts:**

```
aEKlAXvEr r" mAm^
rAg\ : яяAvEt ( 28 )
tA\ : aAEd
pllEv
crZm^
```

Both tools produce garbled output — the text is fundamentally unreadable without the font encoding knowledge.

### 7.4 Font Encoding Discovery — The Rosetta Stone

**Critical finding**: The Type 1 font program, embedded as a Flate-compressed stream in the PDF, contains a complete **encoding vector** in its cleartext header. By decompressing the stream and parsing the PostScript encoding section, we recovered the full mapping from byte positions (0–254) to Velthuis Devanagari glyph names.

**Extraction method:**

```python
import fitz, zlib, re

doc = fitz.open('mdskt.pdf')
# Font stream at xref 5032 (Velthuis-dvng10 font program)
stream = doc.xref_stream_raw(5032)
decompressed = zlib.decompress(stream)
# First 4171 bytes = cleartext header containing the encoding vector
header = decompressed[:4171].decode('latin-1')
# Parse "dup N /glyphname put" entries
entries = re.findall(r'dup\s+(\d+)\s+/(\S+)\s+put', header)
```

**Result: 155 glyph mappings** from byte positions to meaningful Velthuis Devanagari glyph names.

#### 7.4.1 Complete Encoding Map (Key Entries)

**Vowels and mātrās:**

| Byte | Char | Glyph Name | Unicode | Description |
|:---|:---|:---|:---|:---|
| 97 | `a` | a | अ (U+0905) | Vowel a |
| 105 | `i` | i | इ (U+0907) | Vowel i |
| 117 | `u` | u | उ (U+0909) | Vowel u |
| 101 | `e` | e | ए (U+090F) | Vowel e |
| 65 | `A` | aamatra | ा (U+093E) | ā-mātrā |
| 69 | `E` | imatra | ि (U+093F) | i-mātrā |
| 70 | `F` | iimatra | ी (U+0940) | ī-mātrā |
| 0 | `\x00` | umatra | ु (U+0941) | u-mātrā |
| 1 | `\x01` | uumatra | ू (U+0942) | ū-mātrā |
| 3 | `\x03` | ematra | े (U+0947) | e-mātrā |
| 111 | `o` | omatra | ो (U+094B) | o-mātrā |
| 123 | `{` | aimatra | ै (U+0948) | ai-mātrā |
| 79 | `O` | aumatra | ौ (U+094C) | au-mātrā |
| 2 | `\x02` | rimatra | ृ (U+0943) | ṛ-mātrā |

**Consonants:**

| Byte | Char | Glyph Name | Unicode | Description |
|:---|:---|:---|:---|:---|
| 107 | `k` | ka | क (U+0915) | ka |
| 75 | `K` | kha | ख (U+0916) | kha |
| 103 | `g` | ga | ग (U+0917) | ga |
| 71 | `G` | gha | घ (U+0918) | gha |
| 99 | `c` | ca | च (U+091A) | ca |
| 106 | `j` | ja | ज (U+091C) | ja |
| 86 | `V` | tta | ट (U+091F) | ṭa |
| 88 | `X` | dda | ड (U+0921) | ḍa |
| 90 | `Z` | nna | ण (U+0923) | ṇa |
| 116 | `t` | ta. | त (U+0924) | ta |
| 100 | `d` | da | द (U+0926) | da |
| 110 | `n` | na | न (U+0928) | na |
| 112 | `p` | pa | प (U+092A) | pa |
| 98 | `b` | ba | ब (U+092C) | ba |
| 109 | `m` | ma | म (U+092E) | ma |
| 121 | `y` | ya | य (U+092F) | ya |
| 114 | `r` | ra | र (U+0930) | ra |
| 108 | `l` | la | ल (U+0932) | la |
| 15 | `\x0f` | lla | ळ (U+0934) | ḷa (retroflex) |
| 118 | `v` | va | व (U+0935) | va |
| 102 | `f` | sha | श (U+0936) | śa |
| 113 | `q` | ssa | ष (U+0937) | ṣa |
| 115 | `s` | sa | स (U+0938) | sa |
| 104 | `h` | ha | ह (U+0939) | ha |

**Half forms (consonant + virāma, for conjuncts):**

| Byte | Glyph Name | Unicode | Example |
|:---|:---|:---|:---|
| 6 | halfna | न् (U+0928 U+094D) | Used in न्त (nta) |
| 23 | halfnna | ण् (U+0923 U+094D) | Used in ण्ड (ṇḍa) |
| 91 | halfsha | श् (U+0936 U+094D) | Used in श्र (śra) |
| 77 | halfma | म् (U+092E U+094D) | Used in म्ब (mba) |
| 5 | halfya | य् (U+092F U+094D) | Used in त्य (tya) |

**Conjunct ligatures (single glyph for consonant cluster):**

| Byte | Glyph Name | Unicode | Devanagari |
|:---|:---|:---|:---|
| 34 | ksa | क्ष (U+0915 U+094D U+0937) | क्ष |
| 152 | sh_v | श्व (U+0936 U+094D U+0935) | श्व |
| 153 | sh_r | श्र (U+0936 U+094D U+0930) | श्र |
| 165 | l_l | ल्ल (U+0932 U+094D U+0932) | ल्ल |
| 163 | ss_tt | ष्ट (U+0937 U+094D U+091F) | ष्ट |
| 226 | j_ny | ज्ञ (U+091C U+094D U+091E) | ज्ञ |
| 129 | t_t | त्त (U+0924 U+094D U+0924) | त्त |

**Special markers:**

| Byte | Char | Glyph Name | Unicode | Description |
|:---|:---|:---|:---|:---|
| 92 | `\` | anusvara | ं (U+0902) | Anusvāra |
| 94 | `^` | virama | ् (U+094D) | Virāma (halant) |
| 44 | `,` | visarga | ः (U+0903) | Visarga |
| 13 | `\r` | repha | र् | Repha (superscript r) |

### 7.5 Proof-of-Concept Decode — Page 17

Using the extracted encoding vector, we built a decoder that converts raw byte sequences to Unicode Devanagari. Results from decoding page 17 (Akhilāṇḍeśvari):

| Element | Decoded Output | Expected Text | Accuracy |
|:---|:---|:---|:---|
| Title | अखिलाण्डेश्वरि रक्ष माम् | अखिलाण्डेश्वरि रक्ष माम् | **100%** |
| Raga label | रागं | रागं | **100%** |
| Raga name | जुजावन्ित | जुजावन्ति | ~90% (mātrā order) |
| Tala label | ताळं | ताळं | **100%** |
| Tala name | आदि | आदि | **100%** |
| Section: Charanam | चरणम् | चरणम् | **100%** |
| Section: Pallavi | पलवि | पल्लवि | ~80% (italic font) |
| Body text (first line) | अखिलाण्डेश्वरि रक्ष मां | अखिलाण्डेश्वरि रक्ष मां | **100%** |
| Madhyama Kala Sahityam | मध्यमकालसाहित्यम् | मध्यमकालसाहित्यम् | **~95%** |

**Overall first-pass accuracy: ~90%**. The remaining issues are systematic and fixable:

#### 7.5.1 Known Decode Issues

1. **Left-side mātrā reordering** (`ि` / imatra): In Devanagari fonts, the `ि` mātrā is drawn to the LEFT of its consonant visually. The font encodes it BEFORE the consonant in the byte stream. In Unicode, it must appear AFTER the consonant. Current decoder only swaps with the immediately following glyph, but for consonant clusters like `न्ति` (halfna + ta + imatra), the mātrā needs to be placed after the entire cluster. **Fix**: Detect consonant cluster boundaries (sequences of half-forms + final consonant) and place the mātrā after the complete cluster.

2. **Cross-span vowel merging**: The title vowel `आ` sometimes appears as `अ` (vowel a) in one font span and `ा` (ā-mātrā) in the next span. When they're in separate spans, the decoder doesn't merge them into `आ` (U+0906). **Fix**: Post-process across span boundaries.

3. **Italic font glyph reporting**: PyMuPDF's `rawdict` mode sometimes reports the wrong character code for `l_l` (ल्ल) in the italic font `Velthuis-dvngi10`, showing byte 108 (`la`) instead of byte 165 (`l_l`). The `pdftotext` tool correctly identifies two characters. **Fix**: Use heuristic detection (if `la` appears in section label context where `l_l` is expected) or extract text via pdftotext as fallback.

4. **Repha positioning**: The `repha` glyph (superscript `र्`) at byte 13 is placed before the consonant it modifies in the byte stream (visual order), but in Unicode it should appear as `र + ्` before the next consonant. **Fix**: Similar to mātrā reordering — detect repha and position it correctly relative to the consonant cluster.

### 7.6 OCR Comparison — Tesseract Sanskrit Model

For comparison, we also tested Tesseract OCR (v5 with the `san` Sanskrit model) on a 200 DPI rendering of page 17:

| Element | OCR Output | Expected | Notes |
|:---|:---|:---|:---|
| Title | अखिलाण्डेश्**चरि** रक्ष माम् | अखिलाण्डेश्**वरि** | `श्व` misread as `श्च` |
| Raga | जुजावन्ति (**correct!**) | जुजावन्ति | Perfect |
| Tala label | **ताव्छः** | ताळं | `ळ` misread; anusvara missed |
| Pallavi | पल्लवि (**correct!**) | पल्लवि | Perfect |
| Anupallavi | अनुपल्लवि (**correct!**) | अनुपल्लवि | Perfect |
| Charanam | चरणम् (**correct!**) | चरणम् | Perfect |
| Body text | निर्मले श्याम**च्छे** सकलक**व्टे** | ...श्यामळे सकलकळे | ळ consistently misread |

**OCR overall accuracy: ~85–90%**. Strengths: handles mātrā ordering natively (visual recognition). Weaknesses: misreads conjuncts (श्व→श्च) and the rare `ळ` character (ळ→व्छ/व्ट).

### 7.7 Comparison of Approaches

| Criterion | Font Encoding Mapping | OCR (Tesseract) | Hybrid |
|:---|:---|:---|:---|
| **Accuracy** | ~90% first pass, ~98% with fixes | ~85–90% | ~98%+ |
| **Speed** | Very fast (direct byte mapping) | Moderate (image render + OCR) | Moderate |
| **Mātrā ordering** | Needs post-processing | Correct natively | Best of both |
| **Conjunct accuracy** | **100%** (direct glyph mapping) | Varies (visual recognition) | 100% |
| **Dependencies** | PyMuPDF only | Tesseract + language model | Both |
| **Font-specificity** | Font-specific encoding needed | Font-agnostic | Mixed |
| **Scalability** | Works for any page count | Limited by OCR speed | Good |
| **Maintenance** | One-time font mapping effort | Model updates needed | Moderate |

**Recommendation**: **Font encoding mapping as primary, OCR as validation/fallback**. The font encoding approach is deterministic, fast, and 100% accurate for consonants and conjuncts — the remaining mātrā reordering issues are systematic and algorithmically fixable. OCR provides independent validation and handles edge cases where the font mapping fails.

### 7.8 Cross-Linking Strategy: Sanskrit to English Krithis

Both PDFs contain the same 484 Krithis in the same order. Cross-linking uses multiple signals:

| Signal | Reliability | Method |
|:---|:---|:---|
| **Krithi index number** | High | Both PDFs number Krithis 1–484 (Devanagari numerals in Sanskrit PDF) |
| **Page position** | High | Same Krithi appears on approximately the same page in both PDFs |
| **Raga + Tala match** | Very High | Identical raga/tala metadata in both (Sanskrit: रागं, English: rāgaṁ) |
| **Title transliteration** | High | IAST title ↔ Devanagari title have deterministic mapping |
| **Composer (Dikshitar)** | Low (all same) | All 484 are by Muthuswami Dikshitar |

**Linking flow for extraction processing:**

```
Sanskrit PDF extraction → CanonicalExtractionDto
    │
    ├─ Extract Krithi number from page (e.g., "७५" → 75)
    ├─ Extract raga name in Devanagari (e.g., "जुजावन्ति")
    ├─ Transliterate to IAST (e.g., "jujāvanti")
    │
    ├─ Match against existing Krithi records:
    │     1. By raga + tala combination
    │     2. By Krithi title (transliteration match)
    │     3. By source page number (within same PDF set)
    │
    └─ Result: Add Devanagari lyrics as LyricVariant
          with language = "SA" (Sanskrit)
          linked to existing Krithi record
```

**For the specific case of Krithi `dc754fae-febc-4dd9-b37e-de51bf5a7cfc`:**

| Field | English PDF (page 17) | Sanskrit PDF (page 17) |
|:---|:---|:---|
| Title | akhilāṇḍeśvari rakṣa mām | अखिलाण्डेश्वरि रक्ष माम् |
| Raga | Jujāvanti (28) | जुजावन्ति (२८) |
| Tala | Ādi | आदि |
| Sections | Pallavi, Anupallavi, Caraṇam, MKS | पल्लवि, अनुपल्लवि, चरणम्, मध्यमकालसाहित्यम् |
| Madhyama Kala | jhallīmaddaḷa... | झल्लीमदळझझर्रवाद्य... |

The match is unambiguous — same page number, same raga (Jujāvanti/जुजावन्ति), same tala (Ādi/आदि), same section count and structure. The Devanagari text would be added as a new `LyricVariant` record.

---

## 8. Broader Implications and Future Considerations

### 8.1 Applicability to Other PDF Sources

The garbled diacritic problem is not unique to guruguha.org. Any PDF using Type 1 PostScript fonts with custom encoding vectors for IAST characters will exhibit similar artifacts. Sources like printed musicology papers, academic journals, and historical compilations often use these fonts. The normalisation function should be designed as a general-purpose utility, not a guruguha-specific hack.

The Velthuis font encoding problem specifically affects TeX/LaTeX-generated PDFs with Devanagari content. The `devnag` package is widely used in Sanskrit scholarly publications, so this decoder has broad applicability.

### 8.2 Re-Processing vs. Fresh Extraction

Two options for applying the fix to the already-extracted data:

**Option A — Re-extract from PDF**: Reset the extraction queue, re-run the full pipeline. This produces clean data from scratch but requires deleting the 419 created Krithis (or handling deduplication against them).

**Option B — Patch existing records**: Run the normalisation and re-parsing against the stored `raw_extraction` JSONB in `krithi_source_evidence`, updating raga/tala/section fields in-place. This preserves the existing Krithi UUIDs and any manual edits, but requires a one-off migration script.

**Recommendation**: Option A (re-extract) is cleaner for this initial load since no manual edits have been made to the created Krithis. For future incremental updates, Option B will be needed.

### 8.3 LLM Refinement as a Future Enhancement

The strategy document (Section 4.4.3, Stage 5) envisions an LLM refinement step where Gemini validates and corrects the pattern-matched extraction. For the garbled diacritic problem specifically, an LLM could:

- Identify the intended raga name from garbled text (e.g., recognise `juj¯avanti` as Jujāvanti even without regex)
- Cross-reference against a known list of 72 mēḷakarta ragas and their janya ragas
- Resolve ambiguous section boundaries

However, the deterministic regex + normalisation approach is preferred for reliability and cost. LLM refinement should be a validation layer, not the primary extraction mechanism.

---

## 9. Related Documents

| Document | Relationship |
|:---|:---|
| [quality-strategy.md](./quality-strategy.md) | Parent strategy — this analysis addresses Phase 1 extraction quality gap |
| [implementation-checklist.md](./implementation-checklist.md) | Implementation tasks — this analysis creates new tasks for Phase 1 |
| [TRACK-041](../../../conductor/tracks/TRACK-041-enhanced-sourcing-logic.md) | Enhanced sourcing — this fix enables the PDF extraction pipeline to produce usable data |
| [TRACK-053](../../../conductor/tracks/TRACK-053-krithi-creation-from-extraction.md) | Krithi creation from extraction — created the 419 Krithis that surfaced this quality gap |
| **TRACK-054–058** (proposed) | Section 11 — Conductor track breakdown for diacritic fix, Sanskrit decoder, variant backend/UI, E2E QA |
| `tools/pdf-extractor/src/metadata_parser.py` | Primary file to modify (English PDF garbled diacritics) |
| `tools/pdf-extractor/src/structure_parser.py` | Secondary file to modify (section label patterns) |
| `tools/pdf-extractor/src/page_segmenter.py` | Minor update needed (metadata detection pattern) |
| `tools/pdf-extractor/src/extractor.py` | Sanskrit PDF: font encoding decoder integration point |

---

## 10. Implementation Plan

### 10.1 User Journey — Language Variant Extraction

This section describes how an end user (Admin / IngestionOps persona) will interact with the system when adding a language variant — specifically, submitting the Sanskrit PDF (`mdskt.pdf`) after the English PDF (`mdeng.pdf`) has already been extracted.

#### 10.1.1 End-to-End User Flow

```
┌────────────────────────────────────────────────────────────────────────┐
│  STEP 1: Submit Extraction Request                                     │
│  User opens Extraction Monitor → "New Extraction Request"              │
│                                                                        │
│  ┌─────────────────────────────────────────────────────────┐           │
│  │ Source URL:    [https://guruguha.org/.../mdskt.pdf    ] │           │
│  │ Format:        (● PDF)  ○ HTML  ○ DOCX                 │           │
│  │ Registered Source: [guruguha.org (Tier 1)          ▾]   │           │
│  │ Composer Hint: [Muthuswami Dikshitar                  ] │           │
│  │                                                         │           │
│  │ ── Language & Variant ──────────────────────────────── │           │
│  │ Content Language: [Sanskrit (SA)                    ▾]   │           │
│  │ Script:           [Devanagari (auto-detected)       ▾]   │           │
│  │                                                         │           │
│  │ Extraction Intent:                                      │           │
│  │   ○ Discover — Find new Krithis from this source        │           │
│  │   ● Enrich  — Add language variant to existing Krithis  │           │
│  │                                                         │           │
│  │ ── Enrichment Target (shown when Intent = Enrich) ──── │           │
│  │ Related Extraction: [#78441939 — mdeng.pdf (484)    ▾]   │           │
│  │   ℹ "484 Krithis from this extraction will be matched"  │           │
│  │                                                         │           │
│  │ Expected Krithi Count: [484        ]                    │           │
│  │ Page Range:            [17-541     ]                    │           │
│  │                                                         │           │
│  │              [Cancel]  [Submit Extraction]               │           │
│  └─────────────────────────────────────────────────────────┘           │
└────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│  STEP 2: Extraction Processing (automatic)                             │
│                                                                        │
│  Python extractor detects Velthuis-dvng font → activates decoder       │
│  Extracts 484 segments with decoded Devanagari text                    │
│  Writes CanonicalExtractionDto with language=SA, script=devanagari     │
│  Status: PENDING → PROCESSING → DONE                                   │
│                                                                        │
│  User monitors progress on Extraction Monitor page                     │
└────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│  STEP 3: Variant Matching (automatic + review)                         │
│                                                                        │
│  ExtractionResultProcessor detects intent=ENRICH                       │
│  For each extracted segment:                                           │
│    1. Match by Krithi index number (1-484)                             │
│    2. Validate by raga + tala                                          │
│    3. Cross-check title (Devanagari → IAST transliteration)            │
│    4. Compute match confidence (0.0 – 1.0)                             │
│                                                                        │
│  Result: 480 HIGH confidence, 3 MEDIUM, 1 LOW                         │
└────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│  STEP 4: Variant Match Review (user review)                            │
│                                                                        │
│  Extraction Detail page shows "Variant Match Report" tab               │
│                                                                        │
│  ┌─────────────────────────────────────────────────────────┐           │
│  │ Variant Match Summary                                   │           │
│  │   ✅ 480 HIGH confidence matches (auto-approved)        │           │
│  │   ⚠️  3 MEDIUM confidence — review needed               │           │
│  │   ❌  1 LOW confidence — manual match needed             │           │
│  │                                                         │           │
│  │ ┌─────────────────────────────────────────────────────┐ │           │
│  │ │ #17 — अखिलाण्डेश्वरि रक्ष माम्                     │ │           │
│  │ │ Matched to: Akhilāṇḍeśvari rakṣa mām              │ │           │
│  │ │ Confidence: 0.98 (raga ✓ tala ✓ index ✓ title ✓)   │ │           │
│  │ │ Raga: जुजावन्ति ↔ Jujāvanti   ✅ Match              │ │           │
│  │ │ Tala: आदि ↔ Ādi               ✅ Match              │ │           │
│  │ │ Sections: P + A + C + MKS      ✅ Same structure     │ │           │
│  │ │                          [Approve] [Override] [Skip] │ │           │
│  │ └─────────────────────────────────────────────────────┘ │           │
│  │                                                         │           │
│  │ [Approve All HIGH] [Review MEDIUM] [Skip Unmatched]     │           │
│  └─────────────────────────────────────────────────────────┘           │
└────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│  STEP 5: Variant Persistence                                           │
│                                                                        │
│  For each approved match:                                              │
│    → Create krithi_lyric_variants record (SA, devanagari)              │
│    → Create krithi_lyric_sections per section                          │
│    → Create krithi_source_evidence linking to mdskt.pdf                │
│    → Write audit_log entry                                             │
│                                                                        │
│  Result: 484 Krithis now have both Latin/IAST AND Devanagari lyrics    │
└────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│  STEP 6: Verification in Krithi Editor                                 │
│                                                                        │
│  User opens any Krithi → Lyrics tab shows language variant tabs:       │
│                                                                        │
│  ┌─────────────────────────────────────────────────────────┐           │
│  │ Lyrics   [English/IAST ▼] [Sanskrit/Devanagari ▼]      │           │
│  │                                                         │           │
│  │ ┌──────────────────┐  ┌──────────────────┐              │           │
│  │ │ Pallavi          │  │ पल्लवि           │              │           │
│  │ │ akhilāṇḍeśvari  │  │ अखिलाण्डेश्वरि  │              │           │
│  │ │ rakṣa mām       │  │ रक्ष माम्        │              │           │
│  │ │ āgamasampradāya │  │ आगमसम्प्रदाय     │              │           │
│  │ │ nipuṇe śrī      │  │ निपुणे श्री       │              │           │
│  │ └──────────────────┘  └──────────────────┘              │           │
│  │                                                         │           │
│  │ ┌──────────────────┐  ┌──────────────────┐              │           │
│  │ │ Anupallavi       │  │ अनुपल्लवि        │              │           │
│  │ │ nikhilaloka...   │  │ निखिललोक...      │              │           │
│  │ └──────────────────┘  └──────────────────┘              │           │
│  └─────────────────────────────────────────────────────────┘           │
└────────────────────────────────────────────────────────────────────────┘
```

#### 10.1.2 Order of Operations — Which PDF First?

The system should support **any order** of extraction. The three scenarios:

| Scenario | First Extraction | Second Extraction | System Behaviour |
|:---|:---|:---|:---|
| **A** (typical) | English PDF → 484 Krithis created | Sanskrit PDF (intent: Enrich) | Matches to existing Krithis, adds Devanagari lyric variants |
| **B** (reverse) | Sanskrit PDF → 484 Krithis created | English PDF (intent: Enrich) | Matches to existing Krithis, adds IAST lyric variants |
| **C** (simultaneous) | Both PDFs submitted independently | Both set to intent: Discover | First completes → creates Krithis. Second's processing sees existing records and auto-creates variants if matching is confident |

For Scenario C, the `ExtractionResultProcessor` already has matching logic — when a normalised title matches an existing Krithi, it creates source evidence rather than a duplicate. The enhancement needed is to also create a **lyric variant** when the matched Krithi has a different language than the extraction.

#### 10.1.3 How the System Identifies "Same Composer, Different Language"

The matching algorithm uses a **multi-signal confidence score**:

| Signal | Weight | Description | Example |
|:---|:---|:---|:---|
| **Krithi index** | 0.30 | Both PDFs number Krithis 1–484; extracted sequence order provides a natural index | Extraction #1 in both PDFs → Krithi 1 |
| **Raga match** | 0.25 | Raga name in Devanagari transliterated to IAST, compared to existing raga | जुजावन्ति → jujāvanti ≈ Jujāvanti |
| **Tala match** | 0.15 | Same approach as raga | आदि → ādi ≈ Ādi |
| **Composer match** | 0.10 | Composer hint from request or extracted text | Both: Muthuswami Dikshitar |
| **Source lineage** | 0.10 | Same registered source (guruguha.org) and explicit "Related Extraction" link | Same publisher, related PDF set |
| **Title similarity** | 0.10 | Levenshtein or phonetic similarity between transliterated titles | अखिलाण्डेश्वरि → akhilāṇḍeśvari ≈ 0.95 |

**Confidence thresholds:**

| Score | Level | Action |
|:---|:---|:---|
| ≥ 0.85 | HIGH | Auto-approve variant linkage |
| 0.60 – 0.84 | MEDIUM | Flag for user review |
| < 0.60 | LOW | Cannot auto-match — user must manually select target Krithi |

When the user explicitly selects a "Related Extraction" in the request, the matching is scoped to only those Krithis, significantly improving accuracy and speed.

---

### 10.2 UI/UX Enhancements

#### 10.2.1 Extraction Request Modal — New Fields

Add three new fields to the `ExtractionRequestModal` and `CreateExtractionRequest` type:

| Field | Type | Required | Default | Description |
|:---|:---|:---|:---|:---|
| `contentLanguage` | `LanguageCode \| null` | No | `null` (auto-detect) | Language of the source content |
| `extractionIntent` | `'discover' \| 'enrich'` | No | `'discover'` | Whether to create new Krithis or add variants |
| `relatedExtractionId` | `string \| null` | No | `null` | When intent=enrich, the extraction to match against |

**Conditional visibility**: The `relatedExtractionId` selector only appears when `extractionIntent = 'enrich'`. It shows a dropdown of recent DONE/INGESTED extractions from the same registered source, with result counts.

**Auto-detection**: When `contentLanguage` is null, the Python extractor detects the language/script from the font analysis (Velthuis → Sanskrit/Devanagari, Utopia → Latin/IAST, etc.). The detected language is stored in the extraction result.

#### 10.2.2 Extraction Detail — Variant Match Report Tab

When an extraction has `intent = 'enrich'` and status = `DONE`/`INGESTED`, the Extraction Detail page shows a new **"Variant Matches"** tab alongside the existing "Results" tab.

**Tab content:**

1. **Summary bar**: Match statistics (HIGH/MEDIUM/LOW/UNMATCHED counts)
2. **Match table**: One row per extracted segment, showing:
   - Extracted title (in source language/script)
   - Matched Krithi title (in primary language)
   - Match confidence with signal breakdown
   - Status (auto-approved / pending review / unmatched)
3. **Bulk actions**:
   - "Approve All HIGH" — persists all high-confidence variant linkages
   - "Review MEDIUM" — filters to show only medium-confidence for manual review
   - "Skip Unmatched" — marks low-confidence as skipped for later
4. **Per-row actions**:
   - "Approve" — persist this variant linkage
   - "Override" — manually select a different target Krithi
   - "Create New" — treat as a new Krithi instead of a variant
   - "Skip" — defer for later

#### 10.2.3 Krithi Editor — Lyric Variant Tabs

The existing Krithi Editor's Lyrics section should display variant tabs:

- **Tab bar**: One tab per lyric variant, labelled `{Language} / {Script}` (e.g., "English / Latin", "Sanskrit / Devanagari")
- **Add Variant**: "+" button opens a modal to add a new variant manually
- **Side-by-side view**: Toggle to display two variants in parallel columns for comparison
- **Source provenance**: Each variant shows its source (e.g., "From mdeng.pdf, page 17" or "From mdskt.pdf, page 17")
- **Primary toggle**: Star icon to mark one variant as `is_primary` per language

#### 10.2.4 Quality Dashboard — Variant Coverage

Add a new card to the Quality Dashboard:

| Metric | Description |
|:---|:---|
| Krithis with 1 language | Count of Krithis with only one lyric variant |
| Krithis with 2+ languages | Count of Krithis with multiple language variants |
| Language coverage matrix | Heatmap of Language × Composer showing variant availability |

---

### 10.3 Backend Enhancements

#### 10.3.1 Extraction Request Schema Extension

Add to `extraction_queue` table (new migration):

```sql
ALTER TABLE extraction_queue
  ADD COLUMN content_language language_code_enum NULL,
  ADD COLUMN extraction_intent TEXT NOT NULL DEFAULT 'discover'
    CHECK (extraction_intent IN ('discover', 'enrich')),
  ADD COLUMN related_extraction_id UUID NULL
    REFERENCES extraction_queue(id);

COMMENT ON COLUMN extraction_queue.content_language IS
  'Hint for expected content language; NULL = auto-detect during extraction';
COMMENT ON COLUMN extraction_queue.extraction_intent IS
  'discover = find new Krithis; enrich = add language variants to existing Krithis';
COMMENT ON COLUMN extraction_queue.related_extraction_id IS
  'When intent=enrich, the prior extraction whose Krithis should be matched against';
```

#### 10.3.2 Variant Matching Service (New)

A new `VariantMatchingService` in the backend that:

1. Takes a `CanonicalExtractionDto` + target Krithi set
2. Computes multi-signal match confidence for each extraction against each candidate
3. Returns a ranked match result with confidence breakdown
4. Supports manual override (user selects a different match)

**Key methods:**

```kotlin
class VariantMatchingService(
    private val krithiRepository: KrithiRepository,
    private val ragaRepository: RagaRepository,
    private val transliterator: Transliterator,
) {
    fun matchToExistingKrithis(
        extractions: List<CanonicalExtractionDto>,
        relatedExtractionId: UUID?,   // scope matching to this extraction's Krithis
        composerHint: String?,
    ): VariantMatchReport

    fun approveMatch(matchId: UUID): LyricVariantDto
    fun overrideMatch(matchId: UUID, targetKrithiId: UUID): LyricVariantDto
    fun skipMatch(matchId: UUID)
}

data class VariantMatchReport(
    val matches: List<VariantMatch>,
    val highCount: Int,
    val mediumCount: Int,
    val lowCount: Int,
    val unmatchedCount: Int,
)

data class VariantMatch(
    val extractionIndex: Int,
    val extractedTitle: String,
    val extractedLanguage: String,
    val matchedKrithiId: UUID?,
    val matchedKrithiTitle: String?,
    val confidence: Double,
    val signals: MatchSignals,
    val status: MatchStatus,  // AUTO_APPROVED, PENDING_REVIEW, UNMATCHED
)

data class MatchSignals(
    val indexScore: Double,
    val ragaScore: Double,
    val talaScore: Double,
    val composerScore: Double,
    val lineageScore: Double,
    val titleScore: Double,
)
```

#### 10.3.3 ExtractionResultProcessor Enhancement

The existing `processExtractionResult()` method needs a branch for `intent = 'enrich'`:

```
if extraction.intent == ENRICH:
    → Call VariantMatchingService.matchToExistingKrithis()
    → For HIGH matches: auto-create lyric variant + source evidence
    → For MEDIUM/LOW: store match candidates for user review
    → Return VariantMatchReport
else:
    → Existing flow: match or create Krithi
```

#### 10.3.4 API Endpoints (New)

| Method | Path | Description |
|:---|:---|:---|
| `GET` | `/v1/admin/sourcing/extractions/{id}/variant-matches` | Get variant match report for an enrichment extraction |
| `POST` | `/v1/admin/sourcing/extractions/{id}/variant-matches/approve-all` | Bulk-approve all HIGH matches |
| `POST` | `/v1/admin/sourcing/variant-matches/{matchId}/approve` | Approve a single match |
| `POST` | `/v1/admin/sourcing/variant-matches/{matchId}/override` | Override match target |
| `POST` | `/v1/admin/sourcing/variant-matches/{matchId}/skip` | Skip a match |

---

### 10.4 Python Extractor Enhancements

#### 10.4.1 English PDF: Garbled Diacritic Normalisation

A new `diacritic_normalizer.py` utility module:

- `normalize_garbled_diacritics(text: str) -> str` — converts Utopia font garbled forms (¯a → ā, ˙m → ṁ, etc.)
- Applied in `MetadataParser` before regex matching
- Applied in `Worker` for raga/tala name cleanup

#### 10.4.2 Sanskrit PDF: Velthuis Font Decoder

A new `velthuis_decoder.py` module:

- `VelthuisDecoder` class that:
  - Extracts the encoding vector from a Type 1 font program stream
  - Builds byte → glyph name → Unicode mapping
  - Decodes raw byte sequences to Unicode Devanagari
  - Handles left-side mātrā reordering (ि)
  - Handles repha positioning
  - Handles cross-span vowel merging (a + aamatra → आ)
- `detect_velthuis_font(page) -> bool` — checks if a page uses Velthuis fonts
- Integration point: in `PdfExtractor._extract_page()`, if Velthuis detected, decode each span through `VelthuisDecoder`

#### 10.4.3 Language & Script Detection

Enhance `Worker._extract_pdf()`:

- After extraction, detect language/script from the decoded text
- If Velthuis font detected → `language = 'sa'`, `script = 'devanagari'`
- If Utopia font with IAST → `language = 'sa'`, `script = 'latin'`, `transliteration_scheme = 'IAST'`
- Store in `CanonicalLyricVariant.language` and `CanonicalLyricVariant.script`

#### 10.4.4 Extraction Request Payload Enhancement

The Python extractor reads the `request_payload` JSONB from the extraction queue. Add support for:

```json
{
  "content_language": "sa",
  "extraction_intent": "enrich",
  "related_extraction_id": "78441939-1531-454b-ab12-79dabe671c1e"
}
```

The extractor passes these through to the result payload so the Kotlin `ExtractionResultProcessor` can use them during processing.

---

### 10.5 Implementation Phases

#### Phase 1: English PDF Diacritic Fix (Sections 3–5)

**Goal**: Fix raga/tala/section extraction for the 484 English PDF Krithis.

**Scope**: Python extractor only. No UI, API, or database changes.

| # | Task | File(s) | Effort |
|:---|:---|:---|:---|
| 1.1 | Create `diacritic_normalizer.py` with `normalize_garbled_diacritics()` | `tools/pdf-extractor/src/diacritic_normalizer.py` | S |
| 1.2 | Unit test normaliser against all known garbled→clean mappings | `tools/pdf-extractor/tests/test_diacritic_normalizer.py` | S |
| 1.3 | Update `MetadataParser` — normalise text before regex, add garbled-form patterns | `tools/pdf-extractor/src/metadata_parser.py` | M |
| 1.4 | Update `StructureParser` — add `caran. am`, `samash.t.i` patterns | `tools/pdf-extractor/src/structure_parser.py` | S |
| 1.5 | Update `PageSegmenter.METADATA_LINE_PATTERN` for garbled forms | `tools/pdf-extractor/src/page_segmenter.py` | S |
| 1.6 | Update `Worker` — apply name cleanup to raga/tala before CanonicalExtraction | `tools/pdf-extractor/src/worker.py` | S |
| 1.7 | Reset extraction → re-extract English PDF | CLI / DB | S |
| 1.8 | Verify: SQL queries from Section 6.2 | Manual | S |

**Acceptance criteria**: ≤5 Krithis with "Unknown" raga (vs. 480 before). ≥430 Krithis with 3+ sections (vs. 0 before).

---

#### Phase 2: Sanskrit PDF Decoder (Section 7)

**Goal**: Enable extraction of Devanagari text from the Velthuis-font Sanskrit PDF.

**Scope**: Python extractor. New module + integration.

| # | Task | File(s) | Effort |
|:---|:---|:---|:---|
| 2.1 | Create `velthuis_decoder.py` — encoding extraction + byte→Unicode mapping | `tools/pdf-extractor/src/velthuis_decoder.py` | L |
| 2.2 | Implement mātrā reordering and vowel merging post-processing | `tools/pdf-extractor/src/velthuis_decoder.py` | M |
| 2.3 | Unit test decoder against page 17, 18, 100 known text | `tools/pdf-extractor/tests/test_velthuis_decoder.py` | M |
| 2.4 | Integrate decoder into `PdfExtractor` — detect Velthuis, decode spans | `tools/pdf-extractor/src/extractor.py` | M |
| 2.5 | Enhance `Worker` language/script detection for Devanagari | `tools/pdf-extractor/src/worker.py` | S |
| 2.6 | Test extraction of full `mdskt.pdf` in dev environment | Manual | M |
| 2.7 | Spot-check 10 Krithis against PDF rendering | Manual | S |

**Acceptance criteria**: ≥95% character accuracy on decoded Devanagari text. Raga/tala/section labels correctly parsed from Devanagari.

---

#### Phase 3: Language Variant Backend Infrastructure (Section 10.3)

**Goal**: Backend support for "enrich" intent and variant matching.

**Scope**: Database migration, Kotlin backend services, API endpoints.

| # | Task | File(s) | Effort |
|:---|:---|:---|:---|
| 3.1 | Database migration: add `content_language`, `extraction_intent`, `related_extraction_id` to `extraction_queue` | `database/migrations/29__extraction_variant_support.sql` | S |
| 3.2 | Update `ExtractionQueueRepository` — new columns in create/read | `modules/backend/dal/.../ExtractionQueueRepository.kt` | S |
| 3.3 | Update `SourcingRoutes` — accept new fields in create extraction endpoint | `modules/backend/api/.../SourcingRoutes.kt` | S |
| 3.4 | Create `VariantMatchingService` — multi-signal matching algorithm | `modules/backend/api/.../services/VariantMatchingService.kt` | L |
| 3.5 | Implement Devanagari→IAST transliteration for title matching | `modules/backend/api/.../services/TransliterationService.kt` | M |
| 3.6 | Update `ExtractionResultProcessor` — branch for intent=ENRICH | `modules/backend/api/.../services/ExtractionResultProcessor.kt` | M |
| 3.7 | Create variant match persistence — store match candidates for review | `modules/backend/dal/.../repositories/VariantMatchRepository.kt` | M |
| 3.8 | Add variant match API endpoints | `modules/backend/api/.../routes/SourcingRoutes.kt` | M |
| 3.9 | Wire new services in Koin DI | `modules/backend/api/.../di/AppModule.kt` | S |
| 3.10 | Integration test: submit enrichment extraction, verify matching | Test | M |

**Acceptance criteria**: Backend correctly matches Sanskrit extractions to English Krithis with ≥85% HIGH confidence. Lyric variants persisted with correct language/script.

---

#### Phase 4: UI/UX for Language Variant Management (Section 10.2)

**Goal**: User-facing screens for submitting, reviewing, and managing language variants.

**Scope**: React frontend — modal enhancement, new tab, editor update.

| # | Task | File(s) | Effort |
|:---|:---|:---|:---|
| 4.1 | Update `CreateExtractionRequest` type — add `contentLanguage`, `extractionIntent`, `relatedExtractionId` | `modules/frontend/.../types/sourcing.ts` | S |
| 4.2 | Enhance `ExtractionRequestModal` — language selector, intent toggle, related extraction dropdown | `modules/frontend/.../components/sourcing/ExtractionRequestModal.tsx` | M |
| 4.3 | Add `usePriorExtractions` hook — fetches recent DONE/INGESTED extractions for dropdown | `modules/frontend/.../hooks/useSourcingQueries.ts` | S |
| 4.4 | Create `VariantMatchReport` component — summary bar + match table | `modules/frontend/.../components/sourcing/VariantMatchReport.tsx` | L |
| 4.5 | Add "Variant Matches" tab to Extraction Detail page | `modules/frontend/.../pages/sourcing/ExtractionDetailPage.tsx` | M |
| 4.6 | Create `VariantMatchRow` component — per-match detail with approve/override/skip | `modules/frontend/.../components/sourcing/VariantMatchRow.tsx` | M |
| 4.7 | Create `KrithiSelectorModal` — for manual match override | `modules/frontend/.../components/sourcing/KrithiSelectorModal.tsx` | M |
| 4.8 | Enhance Krithi Editor — lyric variant tabs + add variant button | `modules/frontend/.../pages/KrithiEditor.tsx` | L |
| 4.9 | Create `LyricVariantTabs` component — tab bar + content + side-by-side toggle | `modules/frontend/.../components/krithi-editor/LyricVariantTabs.tsx` | L |
| 4.10 | Add variant coverage card to Quality Dashboard | `modules/frontend/.../pages/sourcing/QualityDashboardPage.tsx` | S |

**Acceptance criteria**: User can submit Sanskrit PDF as enrichment, review variant matches, approve linkages, and see Devanagari lyrics in Krithi Editor alongside IAST.

---

#### Phase 5: End-to-End Integration & QA

**Goal**: Full pipeline test from submission to display.

| # | Task | Effort |
|:---|:---|:---|
| 5.1 | End-to-end test: submit `mdeng.pdf` → 484 Krithis → submit `mdskt.pdf` as enrichment → verify 484 variants | L |
| 5.2 | End-to-end test: submit `mdskt.pdf` first → then `mdeng.pdf` (reverse order) | M |
| 5.3 | Spot-check 20 Krithis: compare variant text to PDF rendering | M |
| 5.4 | Performance test: variant matching for 484 Krithis should complete in < 30s | S |
| 5.5 | Update Conductor track documentation | S |
| 5.6 | Update `pdf-diacritic-extraction-analysis.md` with final results | S |

---

### 10.6 Dependency Graph

```
Phase 1 ──────────────────────────────────────────┐
  (English PDF fix — Python only)                  │
                                                   ▼
Phase 2 ──────────────────────────────────────── Phase 5
  (Sanskrit decoder — Python only)               (E2E QA)
                                                   ▲
Phase 3 ──────────────────────────────────────────┤
  (Backend variant infrastructure)                 │
       │                                           │
       ▼                                           │
Phase 4 ──────────────────────────────────────────┘
  (Frontend variant UI)
```

- **Phases 1 and 2** can proceed in parallel (both Python-only).
- **Phase 3** depends on Phase 2 (needs decoded Sanskrit text to test matching).
- **Phase 4** depends on Phase 3 (frontend needs backend APIs).
- **Phase 5** depends on all four prior phases.

---

### 10.7 Estimated Effort

| Phase | Tasks | Estimated Effort | Key Risk |
|:---|:---|:---|:---|
| 1: English PDF Fix | 8 tasks | **2–3 days** | Regex edge cases; mēḷa number variations |
| 2: Sanskrit Decoder | 7 tasks | **3–4 days** | Mātrā reordering accuracy; font variant handling |
| 3: Backend Variant | 10 tasks | **4–5 days** | Transliteration accuracy; matching confidence tuning |
| 4: Frontend Variant UI | 10 tasks | **5–6 days** | Review UX complexity; Krithi Editor refactoring |
| 5: E2E QA | 6 tasks | **2–3 days** | Integration edge cases |
| **Total** | **41 tasks** | **~16–21 days** | |

---

### 10.8 Design Decisions (Resolved)

The following design questions have been resolved. Implementations must adhere to these decisions.

| # | Question | Decision | Rationale / Notes |
|:---|:---|:---|:---|
| **1** | Should HIGH matches auto-persist without user review? | **Yes — auto-persist when confidence ≥ 0.85.** | Reduces friction for large batch operations. User can still review post-hoc via Source Evidence Browser and Krithi Editor. |
| **2** | What happens when a variant extraction finds a Krithi NOT in the related extraction? | **Flag as anomaly for user to decide.** | May indicate extra content in one PDF (e.g. appendix, different edition). Do not auto-create or auto-skip; surface in Variant Match Report with status ANOMALY and allow user to: link to a different Krithi, create new Krithi, or skip. |
| **3** | Should the Krithi Editor support inline editing of variant text? | **Read-only display until a manual edit workflow is designed.** | Variants sourced from PDF extraction are displayed as read-only. A future track can add inline editing with audit trail and versioning. |
| **4** | How to handle structural mismatches (e.g. English has 3 sections, Sanskrit has 4)? | **Align by section type; flag when order or counts differ.** | Matching uses section type (PALLAVI, ANUPALLAVI, CHARANAM, MADHYAMA_KALA_SAHITYA). If counts or order differ, set match flag `structureMismatch: true` and surface in review. Do not block variant linkage — attach what aligns; user can correct structure in a later edit. |
| **5** | Should transliteration Devanagari↔IAST be backend (Kotlin) or Python? | **Evaluate both ecosystems; prefer Python if it offers better support.** | Kotlin ecosystem for Indic transliteration is less mature. Python has established libraries (e.g. `indic-transliteration`, `aksharamukha`). **Options:** (A) Pre-compute IAST title in Python extractor and include in `CanonicalExtractionDto` so Kotlin matching uses it without a transliteration service. (B) Small Python transliteration API called by Kotlin when matching. (C) Kotlin library if evaluation finds one adequate for Devanagari↔IAST. **Recommendation:** Start with (A) for simplicity; add (B) only if matching needs runtime transliteration (e.g. user override, ad-hoc sources). |

---

## 11. Conductor Track Breakdown — Actionable Task List

This section provides a detailed, track-ready breakdown for registering and executing work via [conductor/tracks.md](../../../conductor/tracks.md). Each proposed track has a clear objective, scope, dependency list, and task table with task IDs, descriptions, acceptance criteria, and files to modify.

### 11.1 Proposed Tracks Overview

| Track ID | Name | Status | Depends On | Est. Effort |
|:---|:---|:---|:---|:---|
| **TRACK-054** | PDF Extraction — English Diacritic Normalisation (Raga/Tala/Sections) | Proposed | TRACK-053 | 2–3 days |
| **TRACK-055** | PDF Extraction — Sanskrit Velthuis Font Decoder | Proposed | TRACK-054 | 3–4 days |
| **TRACK-056** | Language Variant Backend — Matching & Enrichment API | Proposed | TRACK-055 | 4–5 days |
| **TRACK-057** | Language Variant UI — Extraction Request & Match Review | Proposed | TRACK-056 | 5–6 days |
| **TRACK-058** | PDF Extraction & Variant Pipeline — E2E QA | Proposed | TRACK-057 | 2–3 days |

**Spec Ref (all tracks):** `application_documentation/01-requirements/krithi-data-sourcing/pdf-diacritic-extraction-analysis.md`

---

### 11.2 TRACK-054: PDF Extraction — English Diacritic Normalisation

**Objective:** Fix raga, tala, and section (Charanam) extraction for guruguha.org English PDF by normalising garbled Utopia font diacritics and updating regex patterns.

**Scope:** Python extractor only. No UI, API, or database changes.

**Depends On:** TRACK-053 (Krithi Creation from Extraction).

| Task ID | Description | Acceptance Criteria | File(s) |
|:---|:---|:---|:---|
| T54.1 | Create `normalize_garbled_diacritics()` utility | All mappings from Section 4.1 (¯a→ā, ˙m→ṁ, etc.) pass unit tests; optional whitespace between diacritic and base handled. | `tools/pdf-extractor/src/diacritic_normalizer.py` (new) |
| T54.2 | Add unit tests for diacritic normaliser | Tests for r¯aga ˙m, t¯al.a ˙m, juj¯avanti, ¯adi, mi´sra c¯apu, n¯ıl¯ambari, caran. am. | `tools/pdf-extractor/tests/test_diacritic_normalizer.py` (new) |
| T54.3 | Update MetadataParser — normalise before regex; add garbled-form patterns | Raga/tala extracted from body_text containing r¯aga ˙m, t¯al.a ˙m; mēḷa number stripped. | `tools/pdf-extractor/src/metadata_parser.py` |
| T54.4 | Update StructureParser — add caran. am, samashti variants | Section type CHARANAM detected when label is caran. am or caran.am. | `tools/pdf-extractor/src/structure_parser.py` |
| T54.5 | Update PageSegmenter METADATA_LINE_PATTERN | Pattern matches r¯aga, t¯al.a, raga, tala, राग, ताल. | `tools/pdf-extractor/src/page_segmenter.py` |
| T54.6 | Apply name cleanup in Worker before CanonicalExtraction | Raga/tala names in output are title-cased, diacritics normalised, (28) etc. stripped. | `tools/pdf-extractor/src/worker.py` |
| T54.7 | Re-extract English PDF and verify | ≤5 Krithis with Unknown raga (vs 480 before); ≥430 Krithis with 3+ sections (vs 0 before). | Manual / SQL (Section 6.2) |

**Progress Log:** (To be maintained in track file.)

---

### 11.3 TRACK-055: PDF Extraction — Sanskrit Velthuis Font Decoder

**Objective:** Enable extraction of Unicode Devanagari text from Sanskrit PDFs that use Velthuis-dvng Type 1 fonts (e.g. mdskt.pdf).

**Scope:** Python extractor — new decoder module and integration into PdfExtractor.

**Depends On:** TRACK-054.

| Task ID | Description | Acceptance Criteria | File(s) |
|:---|:---|:---|:---|
| T55.1 | Create VelthuisDecoder — extract encoding from Type1 font program | Encoding vector parsed from font stream; 155 byte→glyph mappings built. | `tools/pdf-extractor/src/velthuis_decoder.py` (new) |
| T55.2 | Implement byte→Unicode decode with glyph table | Decode produces correct Unicode for page 17 title, raga, tala, section labels (Section 7.5). | `tools/pdf-extractor/src/velthuis_decoder.py` |
| T55.3 | Implement left-side mātrā reordering and vowel merging | ि after consonant in output; a + aamatra → आ where applicable. | `tools/pdf-extractor/src/velthuis_decoder.py` |
| T55.4 | Add unit tests (page 17, 18, 100 samples) | Known Devanagari strings match decoded output within agreed tolerance. | `tools/pdf-extractor/tests/test_velthuis_decoder.py` (new) |
| T55.5 | Integrate decoder into PdfExtractor — detect Velthuis, decode spans | Pages using Velthuis-dvng* fonts return decoded Unicode text; other fonts unchanged. | `tools/pdf-extractor/src/extractor.py` |
| T55.6 | Set language/script in Worker for Devanagari output | CanonicalLyricVariant has language=sa, script=devanagari for Sanskrit PDFs. | `tools/pdf-extractor/src/worker.py` |
| T55.7 | Optional: add IAST title to CanonicalExtractionDto (for matching) | If Python transliteration chosen (Design Decision 5), add `titleLatin` or equivalent for Kotlin matching. | `tools/pdf-extractor/src/schema.py`, `worker.py` |
| T55.8 | Spot-check 10 Krithis vs PDF rendering | Character accuracy ≥95%; no systematic misordering. | Manual |

**Progress Log:** (To be maintained in track file.)

---

### 11.4 TRACK-056: Language Variant Backend — Matching & Enrichment API

**Objective:** Backend support for “enrich” extraction intent: store intent/related extraction, implement variant matching, persist lyric variants for matched Krithis, and expose variant-match review API.

**Scope:** Database migration, Kotlin services (VariantMatchingService, transliteration if in Kotlin), ExtractionResultProcessor changes, new API endpoints.

**Depends On:** TRACK-055.

**Design reminders:** HIGH (≥0.85) auto-persist; anomalies flagged; align by section type, flag structure mismatches; transliteration per Design Decision 5 (prefer pre-compute in Python or evaluate Kotlin lib).

| Task ID | Description | Acceptance Criteria | File(s) |
|:---|:---|:---|:---|
| T56.1 | Migration: add content_language, extraction_intent, related_extraction_id to extraction_queue | Columns added with defaults; CHECK on extraction_intent. | `database/migrations/29__extraction_variant_support.sql` (new) |
| T56.2 | Update ExtractionQueueRepository and DTOs for new columns | Create/read/write new fields. | DAL + shared domain |
| T56.3 | Sourcing API: accept new fields in POST /extractions | Request body accepts contentLanguage, extractionIntent, relatedExtractionId. | `SourcingRoutes.kt`, `CreateExtractionRequest` (frontend types) |
| T56.4 | Implement VariantMatchingService — multi-signal matching | matchToExistingKrithis() returns VariantMatchReport with confidence and signals; ≥0.85 = HIGH. | `services/VariantMatchingService.kt` (new) |
| T56.5 | Devanagari↔IAST for matching | Title matching uses IAST from extraction payload (if present) or Kotlin/Python transliteration per evaluation. | TransliterationService or use payload field |
| T56.6 | ExtractionResultProcessor: branch for intent=ENRICH | When intent=enrich, run variant matching; auto-persist HIGH; store MEDIUM/LOW for review. | `ExtractionResultProcessor.kt` |
| T56.7 | Persist lyric variants for approved matches | Create krithi_lyric_variants + krithi_lyric_sections; link source evidence. | VariantMatchingService / existing lyric persistence |
| T56.8 | API: GET extraction/{id}/variant-matches, POST approve/override/skip | Endpoints implemented and secured. | `SourcingRoutes.kt` |
| T56.9 | Flag anomalies and structure mismatches | Matches outside related extraction scope flagged ANOMALY; section count/order diff set structureMismatch. | VariantMatchingService |
| T56.10 | Wire services in Koin; integration test | Enrichment extraction produces match report and HIGH matches create variants. | `AppModule.kt`, test |

**Progress Log:** (To be maintained in track file.)

---

### 11.5 TRACK-057: Language Variant UI — Extraction Request & Match Review

**Objective:** User-facing UI for submitting enrichment extractions, reviewing variant matches, and viewing lyric variants in the Krithi Editor (read-only).

**Scope:** React frontend — ExtractionRequestModal, Extraction Detail (Variant Matches tab), Krithi Editor lyric variant tabs.

**Depends On:** TRACK-056.

| Task ID | Description | Acceptance Criteria | File(s) |
|:---|:---|:---|:---|
| T57.1 | Extend CreateExtractionRequest and API types | contentLanguage, extractionIntent, relatedExtractionId. | `types/sourcing.ts` |
| T57.2 | ExtractionRequestModal: language, intent, related extraction | Fields visible; related extraction dropdown when intent=enrich; validation. | `ExtractionRequestModal.tsx` |
| T57.3 | usePriorExtractions hook | Fetches recent DONE/INGESTED extractions for same source for dropdown. | `useSourcingQueries.ts` or new hook |
| T57.4 | VariantMatchReport component | Summary bar (HIGH/MEDIUM/LOW/UNMATCHED); match table with confidence and signals. | `VariantMatchReport.tsx` (new) |
| T57.5 | Extraction Detail: “Variant Matches” tab | Tab visible when intent=enrich and status DONE/INGESTED; loads variant match report. | Extraction detail page |
| T57.6 | VariantMatchRow: approve / override / skip | Per-row actions; override opens Krithi selector modal. | `VariantMatchRow.tsx` (new) |
| T57.7 | KrithiSelectorModal | Search/select Krithi for manual match override. | `KrithiSelectorModal.tsx` (new) |
| T57.8 | Krithi Editor: lyric variant tabs | One tab per variant (e.g. English/Latin, Sanskrit/Devanagari); read-only. | KrithiEditor, LyricVariantTabs (new) |
| T57.9 | LyricVariantTabs: add variant button, side-by-side toggle | UI for future manual add; optional side-by-side view. | `LyricVariantTabs.tsx` |
| T57.10 | Quality Dashboard: variant coverage card | Krithis with 1 vs 2+ languages; optional matrix. | QualityDashboardPage |

**Progress Log:** (To be maintained in track file.)

---

### 11.6 TRACK-058: PDF Extraction & Variant Pipeline — E2E QA

**Objective:** End-to-end verification of English PDF fix, Sanskrit decoding, and variant enrichment flow; performance and regression checks.

**Depends On:** TRACK-057.

| Task ID | Description | Acceptance Criteria | File(s) |
|:---|:---|:---|:---|
| T58.1 | E2E: Submit mdeng.pdf → 484 Krithis; then mdskt.pdf as enrichment | 484 variant linkages created; Devanagari lyrics visible in editor. | E2E test or manual script |
| T58.2 | E2E: Reverse order (mdskt.pdf first, then mdeng.pdf) | Same outcome: 484 Krithis with both variants. | E2E / manual |
| T58.3 | Spot-check 20 Krithis: variant text vs PDF | No systematic errors; minor diffs documented. | Manual |
| T58.4 | Performance: variant matching 484 Krithis | Completes in under 30s. | Manual / test |
| T58.5 | Update conductor tracks and this document | Tracks registered in tracks.md; progress logs updated; analysis doc version/date. | tracks.md, track files, this doc |
| T58.6 | Document transliteration choice | If Python pre-compute or Kotlin lib chosen, document in analysis and track. | This doc, TRACK-056 |

**Progress Log:** (To be maintained in track file.)

---

### 11.7 Registry Snippet for conductor/tracks.md

Copy the following table rows into the Conductor Tracks Registry table in [conductor/tracks.md](../../../conductor/tracks.md) when creating the track files:

```markdown
| [TRACK-054](./tracks/TRACK-054-pdf-english-diacritic-normalisation.md) | PDF Extraction — English Diacritic Normalisation | Proposed |
| [TRACK-055](./tracks/TRACK-055-pdf-sanskrit-velthuis-decoder.md) | PDF Extraction — Sanskrit Velthuis Font Decoder | Proposed |
| [TRACK-056](./tracks/TRACK-056-language-variant-backend.md) | Language Variant Backend — Matching & Enrichment API | Proposed |
| [TRACK-057](./tracks/TRACK-057-language-variant-ui.md) | Language Variant UI — Extraction Request & Match Review | Proposed |
| [TRACK-058](./tracks/TRACK-058-pdf-variant-pipeline-e2e.md) | PDF Extraction & Variant Pipeline — E2E QA | Proposed |
```

---

### 11.8 Task ID Cross-Reference (Phase → Track → Task)

| Phase (Section 10.5) | Track | Task IDs |
|:---|:---|:---|
| Phase 1: English PDF Fix | TRACK-054 | T54.1 – T54.7 |
| Phase 2: Sanskrit Decoder | TRACK-055 | T55.1 – T55.8 |
| Phase 3: Backend Variant | TRACK-056 | T56.1 – T56.10 |
| Phase 4: Frontend Variant UI | TRACK-057 | T57.1 – T57.10 |
| Phase 5: E2E QA | TRACK-058 | T58.1 – T58.6 |

---

## 8. Transliteration & Variant Matching Verification (TRACK-058)

### 8.1 Strategy Confirmed

The matching of Sanskrit/Devanagari variants (`mdskt.pdf`) to English/IAST primaries (`mdeng.pdf`) relies on a **hybrid fallback strategy** implemented in `VariantMatchingService`:

1.  **Normalization**: The `NameNormalizationService` was enhanced to use **NFD Normalization** (Canonical Decomposition), which separates characters from accents. This allows the regex-based stripping of non-alphanumeric characters to effectively remove IAST diacritics (e.g. `ā` → `a`, `ṛ` → `r`, `ḍ` → `d`) rather than discarding the entire character. This ensures `IAST` inputs normalize to clean ASCII for fuzzy matching.
2.  **Transliteration Fallback**: The PDF Extractor (via `transliterator.py`) provides an IAST `alternateTitle` for Devanagari inputs. `VariantMatchingService` now attempts to normalize the primary `extraction.title`. If that yields an empty string (as happens for Devanagari inputs), it falls back to normalizing `extraction.alternateTitle` (IAST).

### 8.2 Outcome

This approach ensures that a Devanagari extracted title (e.g., `अखिलाण्डेश्वरि`) matches its IAST/English counterpart (`Akhilāṇḍeśvari`) by:
1.  Extractor transliterates `अखिलाण्डेश्वरि` → `Akhilāṇḍeśvari` (stored in `alternateTitle`).
2.  Matcher normalizes `Akhilāṇḍeśvari` → `akhilandesvari` (via NFD + regex).
3.  Existing Krithi title `Akhilāṇḍeśvari` normalizes to `akhilandesvari`.
4.  Match score = 1.0 (Exact Match).

This validates the pipeline for cross-script enrichment without requiring a heavy-weight Transliteration Library in the Kotlin backend.
