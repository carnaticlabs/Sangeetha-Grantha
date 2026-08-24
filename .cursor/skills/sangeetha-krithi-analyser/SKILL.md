---
name: sangeetha-krithi-analyser
description: Analyse Carnatic krithi lyric structure and sections, enforce cross-variant consistency rules, and guide database/pipeline diagnostics for krithi_lyric_variants, krithi_sections, and krithi_lyric_sections. Use when parsing, validating, or debugging krithi lyric sections from any source (PDF, HTML, OCR, or raw text).
---

# Sangeetha Krithi Analyser

## Overview

This skill helps analyse **Carnatic krithi** lyric structure and diagnose issues in the krithi section pipeline.

- A **Krithi** is a structured Carnatic composition with named sections (Pallavi, Anupallavi, Charanam, etc.).
- Structural correctness is as important as the raw text:
  - Every krithi has at least a **PALLAVI**.
  - Most follow: `PALLAVI → ANUPALLAVI → CHARANAM(s)`.
- The structural skeleton is shared across all lyric variants (languages/scripts) for the same krithi.

For full tables, language labels, and SQL details, see `reference.md`.

## Krithi Section Taxonomy

When reasoning about sections, use these canonical types and ordering (matching the database CHECK constraint):

- **PALLAVI**: Opening refrain, always first.
- **ANUPALLAVI**: Second theme after Pallavi.
- **CHARANAM**: One or more additional stanzas after Anupallavi.
- **SAMASHTI_CHARANAM**: Single combined Anupallavi + Charanam.
- **MADHYAMA_KALA**: Embedded middle-tempo subsection under Anupallavi or Charanam (never top-level).
- **CHITTASWARAM**: Swara-only passage after main lyric sections.
- **SWARA_SAHITYA**: Swara + text hybrid passage.
- **SOLKATTU_SWARA**
- **ANUBANDHA**
- **MUKTAYI_SWARA**
- **ETTUGADA_SWARA**
- **ETTUGADA_SAHITYA**
- **VILOMA_CHITTASWARAM**
- **OTHER**: Fallback for non-standard sections.

The **structural order** typically starts with `PALLAVI`, followed by `ANUPALLAVI` or `SAMASHTI_CHARANAM`, then one or more `CHARANAM` sections, and finally optional swara/closing sections.

## Parsing & Detection Rules

When asked to parse or validate sections, follow these rules:

- **Madhyama Kala is never top-level**
  - Treat Madhyama Kala Sahitya as a **sub-section** of the immediately preceding section (usually Anupallavi or Charanam).
  - Represent it as `MADHYAMA_KALA` with a label indicating the parent (e.g., `"Madhyama Kala - Anupallavi"`).
  - Do not count `MADHYAMA_KALA` as an independent section when comparing section counts across variants.

- **Cross-variant structural consistency**
  - All lyric variants (en, sa, ta, te, kn, ml) of the same krithi must share the **same section types and counts**.
  - Use the English/IAST variant as the **canonical reference** when resolving disagreements in structure.
  - If one script parses differently, treat it as a formatting issue and re-parse to match the canonical skeleton.

- **Dual-format (continuous vs word-division) handling**
  - Many sources present the same script variant twice:
    - Continuous text (sentence-style).
    - Word-division (space-separated for readability).
  - These are **not** separate sections. Keep only one:
    - Prefer the **word-division** version when both exist.
    - Use continuous text only if no word-division is available.
  - To detect dual-format duplicates:
    - Normalize by stripping whitespace and compare; if there is high (>90%) character overlap, treat as the same content.

- **Section header detection patterns (high level)**
  - Recognise headers in multiple scripts and forms:
    - Full labels: `Pallavi`, `Anupallavi`, `Charanam`, `Chittaswaram`, `Madhyama Kala Sahitya`, etc.
    - Single-letter or short abbreviations: `P`, `A`, `C`, `Ch`, `MK`, `CS`, and equivalents in Indian scripts.
    - Bracketed tags: `[PALLAVI]`, `[ANUPALLAVI]`, `[CHARANAM]`, `[CHITTASWARAM]`, `[MADHYAMAKALA]`.
  - Map all of these back to the canonical section types listed above.
  - For full tables of script-specific labels and abbreviations, see `reference.md`.

- **Numbered Charanams**
  - Handle variants like `Charanam 1`, `Charanam 2`, `1st Charanam`, or repeated `Charanam` headings as **distinct `CHARANAM` sections**.
  - Use `order_index` to encode the position and `label` to preserve the human-readable identifier (e.g., `"Charanam 2"`).

## Validation Checklist

When validating krithi sections or debugging data issues, apply this checklist:

- **Section count consistency**
  - For each krithi, all lyric variants must have the same number of structural sections with matching types.

- **No duplicate dual-format sections**
  - Ensure continuous and word-division formats for the same text do not become separate stored sections.

- **Madhyama Kala subordinate**
  - Confirm no `MADHYAMA_KALA` appears as a top-level section in `krithi_sections`.
  - It should always be linked to a parent section at the representation layer.

- **Order integrity**
  - `order_index` values for sections must be strictly sequential (1, 2, 3, …) with no gaps.

- **No empty sections**
  - Every persisted lyric section (`krithi_lyric_sections.text`) must be non-empty.

- **Valid section types**
  - All `section_type` values must be from the canonical enum list in the database CHECK constraint.

- **Pallavi always present**
  - Every krithi must include at least one `PALLAVI` section.

When reviewing automated output, explicitly walk this checklist and note any violations.

## Pipeline & Schema Awareness

Keep the high-level architecture in mind when debugging:

- **Three-table model**
  - `krithi_sections`: structural skeleton per krithi.
    - Fields: `id`, `krithi_id`, `section_type`, `order_index`, `label`, `notes`.
  - `krithi_lyric_variants`: one row per language/script variant of a krithi.
    - Fields include: `id`, `krithi_id`, `language`, `script`, `transliteration_scheme`, `is_primary`, `variant_label`, `source_reference`, `sampradaya_id`, `lyrics`.
  - `krithi_lyric_sections`: join table of text per variant × section.
    - Fields include: `id`, `lyric_variant_id`, `section_id`, `text`, `normalized_text`.
  - Key constraint: `UNIQUE (lyric_variant_id, section_id)` — at most one text per variant per section.

- **Extraction and ingestion flow**
  - Python worker (`tools/krithi-extract-enrich-worker/`):
    - `structure_parser.py`: detects section headers and builds a structured representation.
    - `schema.py`: defines the `CanonicalExtraction` and related schema.
  - Kotlin backend (`modules/backend/api/`):
    - `.../services/scraping/SectionHeaderDetector.kt`
    - `.../services/scraping/KrithiStructureParser.kt`
    - `.../services/LyricVariantPersistenceService.kt`
    - `.../services/StructuralVotingProcessor.kt`
  - Flow:
    - `CanonicalExtraction` (Python) → section & variant data.
    - Kotlin services match to existing krithis, persist variants and sections, and reconcile conflicts.

When an issue appears, decide whether it is:

- An **extraction bug** (Python side: header detection, dual-format handling, or script identification).
- An **ingestion bug** (Kotlin side: mapping to section types, persistence, or structural voting).

## Diagnostics & SQL Helpers

Use the following diagnostics when the user wants to investigate database-level problems. The full queries live in `reference.md`; this section explains when to use them and how to interpret results.

- **Inconsistent section counts across variants**
  - Purpose: Find krithis where some variants have a different number of lyric sections than the structural skeleton.
  - Action:
    - Run the “inconsistent section counts” query from `reference.md`.
    - For any krithi in the result:
      - Compare `krithi_sections` vs `krithi_lyric_sections` per variant.
      - Use the English/IAST variant as canonical and align others accordingly.

- **Variants with zero sections**
  - Purpose: Find lyric variants with no associated lyric sections.
  - Action:
    - Run the “zero sections in any variant” query from `reference.md`.
    - For each result:
      - Verify whether extraction failed, mapping failed, or the variant should be deleted/flagged.

- **Madhyama Kala stored as top-level section**
  - Purpose: Detect `MADHYAMA_KALA` entries incorrectly stored in `krithi_sections`.
  - Action:
    - Run the “Madhyama Kala top-level” query from `reference.md`.
    - For each offending row:
      - Decide whether the section should be retyped (e.g., CHARANAM with embedded MKS) or merged into its parent.

## Usage Examples

- **Example: Dual-format duplication bug**
  - If a user reports that one Tamil variant shows twice as many sections as expected, suspect dual-format text.
  - Compare consecutive blocks under the same script; if normalized text matches at a high rate, treat them as duplicates and keep only the word-division version.
  - Reconstruct sections so each structural section has a single text per variant.

- **Example: Inconsistent structure across scripts**
  - If Telugu shows 3 sections while English shows 4:
    - Run the “inconsistent section counts” query to confirm.
    - Use the English structure (e.g., `PALLAVI → ANUPALLAVI → CHARANAM 1 → CHARANAM 2`) as canonical.
    - Re-parse or adjust the Telugu variant so its sections match this skeleton, ensuring `order_index` and types align.

## Additional Resources

- Original command spec: `.claude/commands/Sangeetha-Krithi-Analyser.md`.
- Database schema documentation: `application_documentation/04-database/schema.md`.

For full label tables, script-specific header patterns, and complete SQL query text, see `reference.md`.

