| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-01-30 |
| **Author** | Sangeetha Grantha Team |

# Composer Deduplication – Implementation Summary

**Conductor:** [TRACK-031](../../../../../conductor/tracks/TRACK-031-composer-deduplication-analysis.md) – Composer Deduplication – Avoid Duplicate Names

## 1. Purpose

Avoid creating duplicate composer records when the same person is referred to by different names (e.g. "Dikshitar" vs "Muthuswami Dikshitar", "Thyagaraja" vs "Tyagaraja"). Ensure a single canonical composer per person and map aliases to it during import and reference-data entry.

## 2. Categorization of Changes

| Category | Description |
|:---|:---|
| **Database** | New table `composer_aliases`; seed data for known aliases |
| **DAL** | New table binding, repository, and integration with `ComposerRepository.findOrCreate` |
| **API / Resolution** | Entity resolution and composer lookup use alias map so aliases resolve to canonical composer |

## 3. Code Changes Summary

### 3.1 Database

| File | Change |
|:---|:---|
| `database/migrations/22__composer_aliases.sql` | **New.** Creates table `composer_aliases` with `alias_normalized` (PK), `composer_id` (FK to `composers` ON DELETE CASCADE), `created_at`. Index on `composer_id`. |
| `database/seed_data/03_composer_aliases.sql` | **New.** Seeds alias → canonical composer: e.g. `dikshitar` → Muthuswami Dikshitar, `thyagaraja` / `saint tyagaraja` → Tyagaraja, `muthuswamy dikshitar`, `syama sastry`, `shyama sastri`, `shyama shastri`, `papanasam shivan` (inserts only when corresponding composer exists in reference data). |

### 3.2 DAL (Data Access Layer)

| File | Change |
|:---|:---|
| `modules/backend/dal/.../tables/ComposerAliasesTable.kt` | **New.** Exposed table for `composer_aliases`: `aliasNormalized`, `composerId`, `createdAt`. Primary key on `aliasNormalized`. |
| `modules/backend/dal/.../repositories/ComposerAliasRepository.kt` | **New.** `findComposerByAliasNormalized(aliasNormalized)`, `findComposerIdByAliasNormalized(aliasNormalized)`, `loadAliasToComposerIdMap()` for cache building. |
| `modules/backend/dal/SangitaDal.kt` | **Updated.** Added `composerAliases: ComposerAliasRepository`; `composers` now constructed with `ComposerRepository(composerAliases)`. |
| `modules/backend/dal/.../repositories/ComposerRepository.kt` | **Updated.** Constructor takes `ComposerAliasRepository`. In `findOrCreate`: before `findByNameNormalized`/create, calls `composerAliases.findComposerByAliasNormalized(normalized)`; if an alias is found, returns that canonical composer and does not create a new one. |

### 3.3 API – Entity Resolution

| File | Change |
|:---|:---|
| `modules/backend/api/.../services/EntityResolutionService.kt` | **Updated.** In `ensureCache()`, after building `composerMap` from composers, loads `dal.composerAliases.loadAliasToComposerIdMap()` and merges alias → canonical composer into `composerMap`, so resolution by normalized name or alias returns the same canonical composer. |

### 3.4 Conductor

| File | Change |
|:---|:---|
| `conductor/tracks/TRACK-031-composer-deduplication-analysis.md` | **Updated.** Status set to In Progress; progress log entry for 2026-01-30 implementation. |
| `conductor/tracks.md` | **Updated.** TRACK-031 status set to In Progress. |

## 4. Behaviour After Implementation

- **Import / findOrCreate:** When creating a composer by name (e.g. "Dikshitar"), the normalized form is looked up in `composer_aliases`; if present, the existing canonical composer is returned and no new row is created.
- **Entity resolution:** Resolving an imported krithi with raw composer "Dikshitar" uses the alias map (and existing name normalization) so the single HIGH-confidence candidate is the canonical composer (e.g. Muthuswami Dikshitar).
- **Out of scope (deferred):** Reference data UI warning when normalized name matches an existing composer or alias; "Merge composer" action.

## 5. Commit Reference

Use this file as the single documentation reference for the **Composer Deduplication** commit:

```text
Ref: application_documentation/01-requirements/features/bulk-import/02-implementation/composer-deduplication-implementation.md
```

**Suggested commit scope (atomic):** All files listed in §3 above for TRACK-031 (migration, seed, DAL, EntityResolutionService, conductor TRACK-031 + tracks.md).
