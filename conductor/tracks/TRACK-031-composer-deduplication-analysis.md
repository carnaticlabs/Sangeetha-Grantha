| Metadata | Value |
|:---|:---|
| **Status** | In Progress |
| **Version** | 1.0.1 |
| **Last Updated** | 2026-02-01 |
| **Author** | Sangeetha Grantha Team |
| **Related Tracks** | TRACK-001 (Bulk Import), TRACK-008 (Entity Resolution) |

---

# TRACK-031: Composer Deduplication – Avoid Duplicate Composer Names

## 1. Goal
Avoid creating duplicate composer records when the same person is referred to by different names (e.g. "Dikshitar" vs "Muthuswami Dikshitar", "Thyagaraja" vs "Tyagaraja"). Ensure a single canonical composer per person and map aliases to it during import and reference-data entry.

## 2. Problem Statement
- **Observed**: Reference data lists show "Dikshitar" (Unknown Place • ? - ?) and "Muthuswami Dikshitar" (1775 - 1835) as separate composers; they are the same person.
- **Cause**: Scraped or manual input uses short forms ("Dikshitar"); entity resolution may suggest candidates but review flow can still create a new composer instead of mapping to the existing canonical one. No alias/canonical rules or merge flow.
- **Impact**: Duplicate composers fragment krithi attribution, complicate search, and clutter reference data.

## 3. Scope (Analysis)
- **In scope**: Composers (primary); patterns may extend to Ragas/Talas alias handling.
- **Out of scope**: Merging existing duplicate records in DB (can be a follow-up track).

## 4. Implementation Plan (To Be Refined)

### 4.1 Canonical Names and Aliases
- Define canonical composer names (e.g. "Muthuswami Dikshitar", "Tyagaraja", "Syama Sastri") and a small alias table or config (e.g. "Dikshitar" → "Muthuswami Dikshitar").
- **Options**: DB table `composer_aliases (alias_normalized, composer_id)` or config-driven map; prefer DB for maintainability.

### 4.2 Entity Resolution
- In `EntityResolutionService` / `NameNormalizationService`: before fuzzy match, resolve known aliases to canonical composer and match by canonical id.
- When resolution returns HIGH confidence for an alias → return the canonical composer as the single candidate (no "create new" suggestion for the alias).

### 4.3 Import Review Flow
- On approve, if user selects a composer from resolution candidates, use that composer id.
- If user does not select (or overrides with a new name): resolve alias → canonical before create; if alias maps to existing canonical, use that composer id and do not create a new composer.
- **findOrCreate**: When creating a composer by name, normalize and check alias map first; if alias exists, return existing canonical composer.

### 4.4 Reference Data UI
- Composer create/edit: warn or block when the normalized name matches an existing composer or a known alias of another composer.
- Optional: "Merge composer" action to merge a duplicate into a canonical one (data migration).

### 4.5 Default Language for Composers
- Majority of Dikshitar compositions are in Sanskrit; consider defaulting primary language for krithis when composer is identified as Dikshitar (or from composer metadata) to SA when not specified. (Can be a small follow-up.)

## 5. References
- [TRACK-008](./TRACK-008-entity-resolution-hardening.md) – Entity Resolution Hardening (composer/raga/tala normalization).
- [Bulk Import Strategy](../application_documentation/01-requirements/features/bulk-import/01-strategy/master-analysis.md) – Lyric structure and entity mapping.
- Source example: [Guru Guha – Bala Kuchambike](http://guru-guha.blogspot.com/2008/02/dikshitar-kriti-bala-kuchambike-raga.html) (composer cited as "Dikshitar" in context of Muthuswami Dikshitar).

## 6. Progress Log
- 2026-01-30: Track created; analysis and implementation plan drafted.
- 2026-01-30: Implemented composer_aliases migration (22__composer_aliases.sql), ComposerAliasesTable, ComposerAliasRepository, seed 03_composer_aliases.sql; wired alias resolution into EntityResolutionService (alias map in composerMap) and ComposerRepository.findOrCreate (resolve alias before create). Reference data UI warning/merge deferred.
- 2026-02-01: Fixed composer alias lookup to avoid implicit join without FK, preventing bulk approve failures during import review.
