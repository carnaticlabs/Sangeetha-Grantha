---
name: sangeetha-krithi-analyser
description: Analyse Carnatic Krithi lyric content and determine its structural sections. Use this skill when extracting, parsing, validating, or debugging krithi lyric sections from any source (PDF, HTML, URL, OCR, or raw text).
---

$ARGUMENTS

## Carnatic Krithi Structure — Domain Knowledge

### What is a Krithi?

A **Krithi** (कृति) is the highest form of Carnatic classical music composition. It has a strict structural grammar: a sequence of named **sections** that define the melodic and lyric architecture. Every krithi has at minimum a **Pallavi**. Most krithis by the Trinity composers (Tyagaraja, Muthuswami Dikshitar, Syama Sastri) follow the Pallavi → Anupallavi → Charanam pattern.

### Section Types (Canonical Order)

| Section Type | Sanskrit | Purpose | Typical Position |
|:---|:---|:---|:---|
| **PALLAVI** | पल्लवि | Opening theme — the refrain. Sung repeatedly between other sections. | Always first |
| **ANUPALLAVI** | अनुपल्लवि | Second theme — contrasts or extends the Pallavi, often at a higher pitch register. | After Pallavi |
| **CHARANAM** | चरणम् | "Feet" — additional stanzas with distinct lyrics. A krithi may have 1–6 charanams. | After Anupallavi |
| **SAMASHTI_CHARANAM** | समष्टि चरणम् | A single unified charanam (no separate Anupallavi). Used when the composer combines Anupallavi + Charanam into one section. | Replaces Anupallavi + Charanam |
| **MADHYAMA_KALA** | मध्यम काल साहित्य | "Middle-tempo lyrics" — a faster sub-passage embedded **within** a parent section (usually Anupallavi or Charanam). **NOT a top-level section.** | Embedded within parent |
| **CHITTASWARAM** | चित्तस्वरम् | Pure swara (note) passage without lyrics — melodic elaboration. | After Charanam |
| **SWARA_SAHITYA** | स्वर साहित्य | Swara + lyric hybrid passage — swaras interspersed with text. | After Charanam |
| **SOLKATTU_SWARA** | सोल्कट्टु स्वर | Rhythmic syllables (solkattu) combined with swara notation. | After Charanam |
| **MUKTAYI_SWARA** | मुक्तायि स्वर | Independent concluding swara section. | End |
| **ETTUGADA_SWARA** | एट्टु गड स्वर | Ettu-gada (foot-tapping rhythm) with swara. | After Charanam |
| **ETTUGADA_SAHITYA** | एट्टु गड साहित्य | Ettu-gada with lyric text. | After Charanam |
| **VILOMA_CHITTASWARAM** | विलोम चित्तस्वरम् | Reversed/inverted chittaswaram passage. | After Chittaswaram |
| **ANUBANDHA** | अनुबन्ध | Concluding ornamental passage. | End |
| **OTHER** | — | Catch-all for non-standard sections. | Any |

### Common Krithi Structures

```
Standard Krithi:     PALLAVI → ANUPALLAVI → CHARANAM(s)
With MKS:            PALLAVI → ANUPALLAVI [+ MADHYAMA_KALA] → CHARANAM [+ MADHYAMA_KALA]
Samashti:            PALLAVI → SAMASHTI_CHARANAM
With Chittaswaram:   PALLAVI → ANUPALLAVI → CHARANAM → CHITTASWARAM
Elaborate:           PALLAVI → ANUPALLAVI → CHARANAM 1 → CHARANAM 2 → CHITTASWARAM → SWARA_SAHITYA
```

---

## Section Detection Rules

### Rule 1: Madhyama Kala Sahitya is NEVER a top-level section

Madhyama Kala Sahitya (MKS) is always a **sub-section** embedded within its parent (Anupallavi or Charanam). When parsing, if you encounter a `[MADHYAMAKALA]` or `Madhyama Kala Sahitya` header:
- Attach its text to the **preceding** section (the parent).
- Store it as `MADHYAMA_KALA` section type with a `label` indicating the parent (e.g., "Madhyama Kala - Anupallavi").
- Do **not** count it as an independent structural section when comparing section counts across variants.

### Rule 2: Cross-variant section consistency

All lyric variants (en, sa, ta, te, kn, ml) of the **same krithi** MUST have the **same section types and counts**. The structural skeleton (`krithi_sections`) is shared — only the text differs per variant. If parsing produces different section counts per script:
- The source likely has a formatting issue (dual-format text, merged sections).
- Resolve by using the **English/IAST variant as the canonical structure reference** — it typically has the cleanest section labels.
- Re-parse other script variants to match the canonical structure.

### Rule 3: Dual-format detection (continuous text vs. word-division)

Many blogspot sources (guru-guha.blogspot.com) present each script variant in **two formats**:
1. **Continuous text** — the sahitya as a flowing sentence
2. **Word-division** — the same sahitya with spaces between words for readability

These are **NOT separate sections**. When detected:
- Keep only the **word-division** format (it's more useful for rendering and search).
- If only continuous text exists, keep that.
- Never store both as separate lyric sections.

**Detection heuristic:** If two consecutive text blocks under the same script have near-identical content but different whitespace patterns, they are dual-format. Compare normalized text (strip all whitespace) — if >90% character overlap, merge.

### Rule 4: Section header detection patterns

Section headers appear in multiple scripts and abbreviation forms:

**Full Labels (case-insensitive):**
```
English:     Pallavi, Anupallavi, Charanam, Chittaswaram, Madhyama Kala Sahitya
Devanagari:  पल्लवि, अनुपल्लवि, चरणम्, चित्तस्वरम्, मध्यम काल साहित्य
Tamil:       பல்லவி, அனுபல்லவி, சரணம், சித்தஸ்வரம்
Telugu:      పల్లవి, అనుపల్లవి, చరణం, చిట్టస్వరం
Kannada:     ಪಲ್ಲವಿ, ಅನುಪಲ್ಲವಿ, ಚರಣ, ಚಿಟ್ಟಸ್ವರ
Malayalam:   പല്ലവി, അനുപല്ലവി, ചരണം, ചിട്ടസ്വരം
```

**Single-letter abbreviations (common in PDFs):**
```
Latin:       P, A, C, Ch, MK, CS
Devanagari:  प, अ, च
Tamil:       ப, அ, ச
Telugu:      ప, అ, చ
Kannada:     ಪ, ಅ, ಚ
Malayalam:   പ, അ, ച
```

**Bracketed labels (common in web sources):**
```
[PALLAVI], [ANUPALLAVI], [CHARANAM], [CHITTASWARAM], [MADHYAMAKALA]
```

### Rule 5: Numbered Charanams

Many krithis have multiple charanams. These appear as:
- `Charanam 1`, `Charanam 2`, `Charanam 3` (explicit numbering)
- `Charanam`, `Charanam`, `Charanam` (same label repeated — infer order from position)
- `1st Charanam`, `2nd Charanam` (ordinal numbering)

Each numbered charanam is a separate `CHARANAM` section with `order_index` reflecting its position and `label` storing the number (e.g., "Charanam 2").

---

## Database Schema Reference

### Three-table model

```
krithi_sections (structural skeleton — shared across all variants)
  ├── id, krithi_id, section_type, order_index, label, notes
  │
  └── krithi_lyric_sections (text per variant × section)
        ├── id, lyric_variant_id → krithi_lyric_variants.id
        ├── section_id → krithi_sections.id
        ├── text (the actual lyric text)
        └── normalized_text (for search)

krithi_lyric_variants (one per language/script combination per krithi)
  ├── id, krithi_id, language, script, transliteration_scheme
  ├── is_primary, variant_label, source_reference, sampradaya_id
  └── lyrics (full concatenated text — denormalized for convenience)
```

**Key constraint:** `UNIQUE (lyric_variant_id, section_id)` — one text per variant per section.

### Section type CHECK constraint values
```sql
'PALLAVI', 'ANUPALLAVI', 'CHARANAM', 'SAMASHTI_CHARANAM',
'CHITTASWARAM', 'SWARA_SAHITYA', 'MADHYAMA_KALA',
'SOLKATTU_SWARA', 'ANUBANDHA', 'MUKTAYI_SWARA',
'ETTUGADA_SWARA', 'ETTUGADA_SAHITYA', 'VILOMA_CHITTASWARAM',
'OTHER'
```

---

## Extraction Pipeline Architecture

### Python extraction (tools/krithi-extract-enrich-worker/)
```
Source (PDF/HTML/OCR)
  → Raw text extraction
  → Section header detection (structure_parser.py)
  → Script/language identification
  → CanonicalExtraction output:
      { sections: [CanonicalSection], lyric_variants: [CanonicalLyricVariant] }
```

### Kotlin ingestion (modules/backend/api/)
```
CanonicalExtraction
  → KrithiMatcherService: fuzzy-match to existing krithi or route to pending review
  → LyricVariantPersistenceService: persist sections + lyric text
  → StructuralVotingProcessor: reconcile multi-source section disagreements
```

### Key files
| Component | File |
|:---|:---|
| Python section parser | `tools/krithi-extract-enrich-worker/src/structure_parser.py` |
| Python schema | `tools/krithi-extract-enrich-worker/src/schema.py` |
| Kotlin section detector | `modules/backend/api/.../services/scraping/SectionHeaderDetector.kt` |
| Kotlin structure parser | `modules/backend/api/.../services/scraping/KrithiStructureParser.kt` |
| Variant persistence | `modules/backend/api/.../services/LyricVariantPersistenceService.kt` |
| Structural voting | `modules/backend/api/.../services/StructuralVotingProcessor.kt` |

---

## Validation Checklist

When analysing or validating krithi lyric sections, check:

1. **Section count consistency** — all 6 language variants must have the same number of sections with the same types
2. **No duplicate sections** — dual-format text (continuous + word-division) must not produce separate sections
3. **MKS is subordinate** — Madhyama Kala Sahitya must be linked to a parent section, not standalone
4. **Order integrity** — `order_index` must be sequential (1, 2, 3...) with no gaps
5. **No empty sections** — every `krithi_lyric_sections.text` must be non-empty
6. **Section types valid** — all types must match the CHECK constraint enum
7. **Pallavi always present** — every krithi must have at least a PALLAVI section

## Diagnostic SQL Queries

### Find krithis with inconsistent section counts across variants
```sql
SELECT k.id, k.title,
       klv.language, COUNT(kls.id) AS section_count
FROM krithis k
JOIN krithi_lyric_variants klv ON klv.krithi_id = k.id
LEFT JOIN krithi_lyric_sections kls ON kls.lyric_variant_id = klv.id
GROUP BY k.id, k.title, klv.language
HAVING COUNT(kls.id) != (
    SELECT COUNT(ks.id) FROM krithi_sections ks WHERE ks.krithi_id = k.id
)
ORDER BY k.title, klv.language;
```

### Find krithis with zero sections in any variant
```sql
SELECT k.id, k.title, klv.language, klv.script
FROM krithis k
JOIN krithi_lyric_variants klv ON klv.krithi_id = k.id
LEFT JOIN krithi_lyric_sections kls ON kls.lyric_variant_id = klv.id
WHERE kls.id IS NULL
ORDER BY k.title;
```

### Find Madhyama Kala stored as top-level section
```sql
SELECT k.title, ks.section_type, ks.order_index, ks.label
FROM krithi_sections ks
JOIN krithis k ON k.id = ks.krithi_id
WHERE ks.section_type = 'MADHYAMA_KALA'
ORDER BY k.title, ks.order_index;
```
