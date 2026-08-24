# Sangeetha Krithi Analyser Reference

This file contains detailed reference material that supports the `sangeetha-krithi-analyser` skill. It is intentionally more verbose than `SKILL.md`.

## Carnatic Krithi Structure — Domain Knowledge

### What is a Krithi?

A **Krithi** (कृति) is a high-form Carnatic classical composition with a strict structural grammar: a sequence of named **sections** that define the melodic and lyric architecture. Every krithi has at minimum a **Pallavi**. Most Trinity krithis (Tyagaraja, Muthuswami Dikshitar, Syama Sastri) follow the pattern:

```text
PALLAVI → ANUPALLAVI → CHARANAM(s)
```

### Section Types (Canonical Order)

These are the canonical section types and their typical positions:

| Section Type          | Sanskrit             | Purpose                                                     | Typical Position                      |
|:----------------------|:---------------------|:------------------------------------------------------------|:--------------------------------------|
| **PALLAVI**           | पल्लवि              | Opening theme — the refrain, repeated between other parts. | Always first                          |
| **ANUPALLAVI**        | अनुपल्लवि          | Second theme, often higher register.                       | After Pallavi                         |
| **CHARANAM**          | चरणम्              | Additional stanzas with distinct lyrics.                   | After Anupallavi                      |
| **SAMASHTI_CHARANAM** | समष्टि चरणम्       | Combined Anupallavi + Charanam.                           | Replaces Anupallavi + Charanam       |
| **MADHYAMA_KALA**     | मध्यम काल साहित्य  | Middle-tempo sub-passage embedded in a parent section.     | Embedded within Anupallavi/Charanam  |
| **CHITTASWARAM**      | चित्तस्वरम्         | Swara-only passage.                                        | After Charanam                        |
| **SWARA_SAHITYA**     | स्वर साहित्य        | Swara + lyric hybrid passage.                              | After Charanam                        |
| **SOLKATTU_SWARA**    | सोल्कट्टु स्वर      | Rhythmic syllables with swaras.                            | After Charanam                        |
| **MUKTAYI_SWARA**     | मुक्तायि स्वर       | Concluding swara section.                                  | End                                   |
| **ETTUGADA_SWARA**    | एट्टु गड स्वर       | Ettu-gada with swaras.                                     | After Charanam                        |
| **ETTUGADA_SAHITYA**  | एट्टु गड साहित्य    | Ettu-gada with lyrics.                                     | After Charanam                        |
| **VILOMA_CHITTASWARAM** | विलोम चित्तस्वरम् | Reversed/inverted chittaswaram.                            | After Chittaswaram                    |
| **ANUBANDHA**         | अनुबन्ध             | Concluding ornamental passage.                             | End                                   |
| **OTHER**             | —                    | Catch-all for non-standard sections.                       | Any                                   |

### Common Krithi Structures

Some typical structural patterns:

```text
Standard Krithi:     PALLAVI → ANUPALLAVI → CHARANAM(s)
With MKS:            PALLAVI → ANUPALLAVI [+ MADHYAMA_KALA] → CHARANAM [+ MADHYAMA_KALA]
Samashti:            PALLAVI → SAMASHTI_CHARANAM
With Chittaswaram:   PALLAVI → ANUPALLAVI → CHARANAM → CHITTASWARAM
Elaborate:           PALLAVI → ANUPALLAVI → CHARANAM 1 → CHARANAM 2 → CHITTASWARAM → SWARA_SAHITYA
```

## Section Detection Reference

### Madhyama Kala Sahitya

- Always a **sub-section**; never treat it as top-level.
- Attach `MADHYAMA_KALA` to the immediately preceding section (Anupallavi or Charanam).
- Label examples:
  - `"Madhyama Kala - Anupallavi"`
  - `"Madhyama Kala - Charanam 2"`

### Dual-Format Detection (Continuous vs Word-Division)

Many sources show each script variant in two formats:

1. **Continuous text** — flowing sentence.
2. **Word-division** — space-separated words.

Detection heuristic:

- Take two consecutive blocks under the same script.
- Normalize each by stripping all whitespace.
- Compute character overlap; if overlap is > 90%, treat them as dual-format representations of the same lyric.
- Keep only one:
  - Prefer word-division if available.

### Section Header Detection Patterns

Recognise section headers across scripts and label forms.

**Full Labels (case-insensitive):**

```text
English:     Pallavi, Anupallavi, Charanam, Chittaswaram, Madhyama Kala Sahitya
Devanagari:  पल्लवि, अनुपल्लवि, चरणम्, चित्तस्वरम्, मध्यम काल साहित्य
Tamil:       பல்லவி, அனுபல்லவி, சரணம், சித்தஸ்வரம்
Telugu:      పల్లవి, అనుపల్లవి, చరణం, చిట్టస్వరం
Kannada:     ಪಲ್ಲವಿ, ಅನುಪಲ್ಲವಿ, ಚರಣ, ಚಿಟ್ಟಸ್ವರ
Malayalam:   പല്ലവി, അനുപल्लവി, ചരണം, ചിട്ടസ്വരം
```

**Single-letter / short abbreviations (context-dependent):**

```text
Latin:       P, A, C, Ch, MK, CS
Devanagari:  प, अ, च
Tamil:       ப, அ, ச
Telugu:      ప, అ, చ
Kannada:     ಪ, ಅ, ಚ
Malayalam:   പ, അ, ച
```

**Bracketed labels (common in web sources):**

```text
[PALLAVI], [ANUPALLAVI], [CHARANAM], [CHITTASWARAM], [MADHYAMAKALA]
```

### Numbered Charanams

Examples:

- `Charanam 1`, `Charanam 2`, `Charanam 3`
- `1st Charanam`, `2nd Charanam`
- Repeated `Charanam` headings in order.

Guidance:

- Treat each as a separate `CHARANAM` section.
- Use `order_index` for the numeric order.
- Preserve the human label in a `label` field (e.g., `"Charanam 2"`).

## Database Schema Reference

### Three-Table Model

```text
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

### Section Type CHECK Constraint Values

```sql
'PALLAVI', 'ANUPALLAVI', 'CHARANAM', 'SAMASHTI_CHARANAM',
'CHITTASWARAM', 'SWARA_SAHITYA', 'MADHYAMA_KALA',
'SOLKATTU_SWARA', 'ANUBANDHA', 'MUKTAYI_SWARA',
'ETTUGADA_SWARA', 'ETTUGADA_SAHITYA', 'VILOMA_CHITTASWARAM',
'OTHER'
```

## Extraction Pipeline Architecture

### Python Extraction (`tools/krithi-extract-enrich-worker/`)

```text
Source (PDF/HTML/OCR)
  → Raw text extraction
  → Section header detection (structure_parser.py)
  → Script/language identification
  → CanonicalExtraction output:
      { sections: [CanonicalSection], lyric_variants: [CanonicalLyricVariant] }
```

- `structure_parser.py`: logic for detecting headers, handling abbreviations and dual-format blocks.
- `schema.py`: defines `CanonicalSection`, `CanonicalLyricVariant`, and the overall `CanonicalExtraction` shape.

### Kotlin Ingestion (`modules/backend/api/`)

```text
CanonicalExtraction
  → KrithiMatcherService: fuzzy-match to existing krithi or route to pending review
  → LyricVariantPersistenceService: persist sections + lyric text
  → StructuralVotingProcessor: reconcile multi-source section disagreements
```

Key files (paths abbreviated):

- `modules/backend/api/.../services/scraping/SectionHeaderDetector.kt`
- `modules/backend/api/.../services/scraping/KrithiStructureParser.kt`
- `modules/backend/api/.../services/LyricVariantPersistenceService.kt`
- `modules/backend/api/.../services/StructuralVotingProcessor.kt`

## Validation Checklist (Expanded)

For each krithi:

1. **Section count consistency**
   - Compare the count of `krithi_sections` with the per-variant `krithi_lyric_sections` count.
   - All variants should match the structural skeleton.

2. **No duplicate dual-format sections**
   - Within a given script, look for pairs of nearly identical text blocks.
   - If normalized text matches, treat as dual-format and collapse to one.

3. **Madhyama Kala subordinate**
   - Verify no `MADHYAMA_KALA` appears as a top-level entry in `krithi_sections` (see diagnostic query below).

4. **Order integrity**
   - Ensure `order_index` is 1-based and strictly increasing without gaps.

5. **No empty sections**
   - All `krithi_lyric_sections.text` values should be non-null and non-empty.

6. **Section types valid**
   - Cross-check each `section_type` against the CHECK constraint list above.

7. **Pallavi always present**
   - Ensure at least one `PALLAVI` section exists per krithi.

## Diagnostic SQL Queries

### 1. Krithis with Inconsistent Section Counts Across Variants

Use this to find krithis where some variants have a different number of lyric sections than the structural skeleton:

```sql
SELECT k.id,
       k.title,
       klv.language,
       COUNT(kls.id) AS section_count
FROM krithis k
JOIN krithi_lyric_variants klv
  ON klv.krithi_id = k.id
LEFT JOIN krithi_lyric_sections kls
  ON kls.lyric_variant_id = klv.id
GROUP BY k.id, k.title, klv.language
HAVING COUNT(kls.id) != (
    SELECT COUNT(ks.id)
    FROM krithi_sections ks
    WHERE ks.krithi_id = k.id
)
ORDER BY k.title, klv.language;
```

### 2. Krithis with Zero Sections in Any Variant

Use this to find lyric variants that have no associated lyric sections:

```sql
SELECT k.id,
       k.title,
       klv.language,
       klv.script
FROM krithis k
JOIN krithi_lyric_variants klv
  ON klv.krithi_id = k.id
LEFT JOIN krithi_lyric_sections kls
  ON kls.lyric_variant_id = klv.id
WHERE kls.id IS NULL
ORDER BY k.title;
```

### 3. Madhyama Kala Stored as Top-Level Section

Use this to detect `MADHYAMA_KALA` entries in `krithi_sections`:

```sql
SELECT k.title,
       ks.section_type,
       ks.order_index,
       ks.label
FROM krithi_sections ks
JOIN krithis k
  ON k.id = ks.krithi_id
WHERE ks.section_type = 'MADHYAMA_KALA'
ORDER BY k.title, ks.order_index;
```

Interpretation:

- Any rows returned indicate a structural violation.
- Each should be reviewed and either:
  - Re-modelled as a sub-section under a parent, or
  - Reclassified if mis-typed.

# Sangeetha Krithi Analyser Reference

This file contains detailed reference material that supports the `sangeetha-krithi-analyser` skill. It is intentionally more verbose than `SKILL.md`.

## Carnatic Krithi Structure — Domain Knowledge

### What is a Krithi?

A **Krithi** (कृति) is a high-form Carnatic classical composition with a strict structural grammar: a sequence of named **sections** that define the melodic and lyric architecture. Every krithi has at minimum a **Pallavi**. Most Trinity krithis (Tyagaraja, Muthuswami Dikshitar, Syama Sastri) follow the pattern:

```text
PALLAVI → ANUPALLAVI → CHARANAM(s)
```

### Section Types (Canonical Order)

These are the canonical section types and their typical positions:

| Section Type          | Sanskrit             | Purpose                                                     | Typical Position                      |
|:----------------------|:---------------------|:------------------------------------------------------------|:--------------------------------------|
| **PALLAVI**           | पल्लवि              | Opening theme — the refrain, repeated between other parts. | Always first                          |
| **ANUPALLAVI**        | अनुपल्लवि          | Second theme, often higher register.                       | After Pallavi                         |
| **CHARANAM**          | चरणम्              | Additional stanzas with distinct lyrics.                   | After Anupallavi                      |
| **SAMASHTI_CHARANAM** | समष्टि चरणम्       | Combined Anupallavi + Charanam.                           | Replaces Anupallavi + Charanam       |
| **MADHYAMA_KALA**     | मध्यम काल साहित्य  | Middle-tempo sub-passage embedded in a parent section.     | Embedded within Anupallavi/Charanam  |
| **CHITTASWARAM**      | चित्तस्वरम्         | Swara-only passage.                                        | After Charanam                        |
| **SWARA_SAHITYA**     | स्वर साहित्य        | Swara + lyric hybrid passage.                              | After Charanam                        |
| **SOLKATTU_SWARA**    | सोल्कट्टु स्वर      | Rhythmic syllables with swaras.                            | After Charanam                        |
| **MUKTAYI_SWARA**     | मुक्तायि स्वर       | Concluding swara section.                                  | End                                   |
| **ETTUGADA_SWARA**    | एट्टु गड स्वर       | Ettu-gada with swaras.                                     | After Charanam                        |
| **ETTUGADA_SAHITYA**  | एट्टु गड साहित्य    | Ettu-gada with lyrics.                                     | After Charanam                        |
| **VILOMA_CHITTASWARAM** | विलोम चित्तस्वरम् | Reversed/inverted chittaswaram.                            | After Chittaswaram                    |
| **ANUBANDHA**         | अनुबन्ध             | Concluding ornamental passage.                             | End                                   |
| **OTHER**             | —                    | Catch-all for non-standard sections.                       | Any                                   |

### Common Krithi Structures

Some typical structural patterns:

```text
Standard Krithi:     PALLAVI → ANUPALLAVI → CHARANAM(s)
With MKS:            PALLAVI → ANUPALLAVI [+ MADHYAMA_KALA] → CHARANAM [+ MADHYAMA_KALA]
Samashti:            PALLAVI → SAMASHTI_CHARANAM
With Chittaswaram:   PALLAVI → ANUPALLAVI → CHARANAM → CHITTASWARAM
Elaborate:           PALLAVI → ANUPALLAVI → CHARANAM 1 → CHARANAM 2 → CHITTASWARAM → SWARA_SAHITYA
```

## Section Detection Reference

### Madhyama Kala Sahitya

- Always a **sub-section**; never treat it as top-level.
- Attach `MADHYAMA_KALA` to the immediately preceding section (Anupallavi or Charanam).
- Label examples:
  - `"Madhyama Kala - Anupallavi"`
  - `"Madhyama Kala - Charanam 2"`

### Dual-Format Detection (Continuous vs Word-Division)

Many sources show each script variant in two formats:

1. **Continuous text** — flowing sentence.
2. **Word-division** — space-separated words.

Detection heuristic:

- Take two consecutive blocks under the same script.
- Normalize each by stripping all whitespace.
- Compute character overlap; if overlap is > 90%, treat them as dual-format representations of the same lyric.
- Keep only one:
  - Prefer word-division if available.

### Section Header Detection Patterns

Recognise section headers across scripts and label forms.

**Full Labels (case-insensitive):**

```text
English:     Pallavi, Anupallavi, Charanam, Chittaswaram, Madhyama Kala Sahitya
Devanagari:  पल्लवि, अनुपल्लवि, चरणम्, चित्तस्वरम्, मध्यम काल साहित्य
Tamil:       பல்லவி, அனுபல்லவி, சரணம், சித்தஸ்வரம்
Telugu:      పల్లవి, అనుపల్లవి, చరణం, చిట్టస్వరం
Kannada:     ಪಲ್ಲವಿ, ಅನುಪಲ್ಲವಿ, ಚರಣ, ಚಿಟ್ಟಸ್ವರ
Malayalam:   പല്ലവി, അനുപല്ലവി, ചരണം, ചിട്ടസ്വരം
```

**Single-letter / short abbreviations (context-dependent):**

```text
Latin:       P, A, C, Ch, MK, CS
Devanagari:  प, अ, च
Tamil:       ப, அ, ச
Telugu:      ప, అ, చ
Kannada:     ಪ, ಅ, ಚ
Malayalam:   പ, അ, ച
```

**Bracketed labels (common in web sources):**

```text
[PALLAVI], [ANUPALLAVI], [CHARANAM], [CHITTASWARAM], [MADHYAMAKALA]
```

### Numbered Charanams

Examples:

- `Charanam 1`, `Charanam 2`, `Charanam 3`
- `1st Charanam`, `2nd Charanam`
- Repeated `Charanam` headings in order.

Guidance:

- Treat each as a separate `CHARANAM` section.
- Use `order_index` for the numeric order.
- Preserve the human label in a `label` field (e.g., `"Charanam 2"`).

## Database Schema Reference

### Three-Table Model

```text
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

### Section Type CHECK Constraint Values

```sql
'PALLAVI', 'ANUPALLAVI', 'CHARANAM', 'SAMASHTI_CHARANAM',
'CHITTASWARAM', 'SWARA_SAHITYA', 'MADHYAMA_KALA',
'SOLKATTU_SWARA', 'ANUBANDHA', 'MUKTAYI_SWARA',
'ETTUGADA_SWARA', 'ETTUGADA_SAHITYA', 'VILOMA_CHITTASWARAM',
'OTHER'
```

## Extraction Pipeline Architecture

### Python Extraction (`tools/krithi-extract-enrich-worker/`)

```text
Source (PDF/HTML/OCR)
  → Raw text extraction
  → Section header detection (structure_parser.py)
  → Script/language identification
  → CanonicalExtraction output:
      { sections: [CanonicalSection], lyric_variants: [CanonicalLyricVariant] }
```

- `structure_parser.py`: logic for detecting headers, handling abbreviations and dual-format blocks.
- `schema.py`: defines `CanonicalSection`, `CanonicalLyricVariant`, and the overall `CanonicalExtraction` shape.

### Kotlin Ingestion (`modules/backend/api/`)

```text
CanonicalExtraction
  → KrithiMatcherService: fuzzy-match to existing krithi or route to pending review
  → LyricVariantPersistenceService: persist sections + lyric text
  → StructuralVotingProcessor: reconcile multi-source section disagreements
```

Key files (paths abbreviated):

- `modules/backend/api/.../services/scraping/SectionHeaderDetector.kt`
- `modules/backend/api/.../services/scraping/KrithiStructureParser.kt`
- `modules/backend/api/.../services/LyricVariantPersistenceService.kt`
- `modules/backend/api/.../services/StructuralVotingProcessor.kt`

## Validation Checklist (Expanded)

For each krithi:

1. **Section count consistency**
   - Compare the count of `krithi_sections` with the per-variant `krithi_lyric_sections` count.
   - All variants should match the structural skeleton.

2. **No duplicate dual-format sections**
   - Within a given script, look for pairs of nearly identical text blocks.
   - If normalized text matches, treat as dual-format and collapse to one.

3. **Madhyama Kala subordinate**
   - Verify no `MADHYAMA_KALA` appears as a top-level entry in `krithi_sections` (see diagnostic query below).

4. **Order integrity**
   - Ensure `order_index` is 1-based and strictly increasing without gaps.

5. **No empty sections**
   - All `krithi_lyric_sections.text` values should be non-null and non-empty.

6. **Section types valid**
   - Cross-check each `section_type` against the CHECK constraint list above.

7. **Pallavi always present**
   - Ensure at least one `PALLAVI` section exists per krithi.

## Diagnostic SQL Queries

### 1. Krithis with Inconsistent Section Counts Across Variants

Use this to find krithis where some variants have a different number of lyric sections than the structural skeleton:

```sql
SELECT k.id,
       k.title,
       klv.language,
       COUNT(kls.id) AS section_count
FROM krithis k
JOIN krithi_lyric_variants klv
  ON klv.krithi_id = k.id
LEFT JOIN krithi_lyric_sections kls
  ON kls.lyric_variant_id = klv.id
GROUP BY k.id, k.title, klv.language
HAVING COUNT(kls.id) != (
    SELECT COUNT(ks.id)
    FROM krithi_sections ks
    WHERE ks.krithi_id = k.id
)
ORDER BY k.title, klv.language;
```

### 2. Krithis with Zero Sections in Any Variant

Use this to find lyric variants that have no associated lyric sections:

```sql
SELECT k.id,
       k.title,
       klv.language,
       klv.script
FROM krithis k
JOIN krithi_lyric_variants klv
  ON klv.krithi_id = k.id
LEFT JOIN krithi_lyric_sections kls
  ON kls.lyric_variant_id = klv.id
WHERE kls.id IS NULL
ORDER BY k.title;
```

### 3. Madhyama Kala Stored as Top-Level Section

Use this to detect `MADHYAMA_KALA` entries in `krithi_sections`:

```sql
SELECT k.title,
       ks.section_type,
       ks.order_index,
       ks.label
FROM krithi_sections ks
JOIN krithis k
  ON k.id = ks.krithi_id
WHERE ks.section_type = 'MADHYAMA_KALA'
ORDER BY k.title, ks.order_index;
```

Interpretation:

- Any rows returned indicate a structural violation.
- Each should be reviewed and either:
  - Re-modelled as a sub-section under a parent, or
  - Reclassified if mis-typed.

