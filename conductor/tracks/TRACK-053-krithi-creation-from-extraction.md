| Metadata | Value |
|:---|:---|
| **Track ID** | TRACK-053 |
| **Title** | Krithi Creation from Extraction Results |
| **Status** | In Progress |
| **Priority** | High |
| **Created** | 2026-02-09 |
| **Updated** | 2026-02-09 |
| **Depends On** | TRACK-041, TRACK-045 |
| **Spec Ref** | application_documentation/01-requirements/product-requirements-document.md |

# TRACK-053: Krithi Creation from Extraction Results

## Objective

Enable the system to automatically create new Krithi records from PDF/web extraction
results when no matching Krithi exists in the database. Currently, the
`ExtractionResultProcessor` only links extraction results to **existing** Krithis — any
extracted composition that does not match an existing record is silently dropped. This
track closes that gap, making it possible to populate the corpus from scratch using
reliable sources (e.g. guruguha.org PDF compendium with ~486 Muthuswami Dikshitar
compositions).

## Scope

1. **KrithiCreationFromExtractionService** — new service that:
   - Resolves reference entities (composer, raga, tala, deity) via `findOrCreate`
   - Creates a `DRAFT` Krithi record with all resolved foreign keys
   - Persists canonical sections and lyric variants from `CanonicalExtractionDto`
   - Creates source evidence linking the new Krithi to its extraction
   - Writes audit log entries for every creation

2. **ExtractionResultProcessor update** — when `findDuplicateCandidates` returns no
   match, delegate to the creation service instead of returning `null`.

3. **ProcessingReport enhancement** — report how many Krithis were *created* vs
   *matched* so operators can see the impact.

## Design Decisions

| Decision | Choice | Rationale |
|:---|:---|:---|
| New Krithi workflow state | `DRAFT` | Auto-created records should be reviewed before publishing |
| Entity resolution strategy | `findOrCreate` via DAL | Reuses existing repository patterns for composers, ragas, talas |
| Primary language default | `SA` (Sanskrit) | Most Carnatic compositions are in Sanskrit; override if lyric variants provide a hint |
| Deduplication window | Normalized title + composer | Same logic as existing `findDuplicateCandidates` |

## Files Changed

| File | Change |
|:---|:---|
| `modules/backend/api/.../services/KrithiCreationFromExtractionService.kt` | New service |
| `modules/backend/api/.../services/ExtractionResultProcessor.kt` | Use creation service for unmatched extractions |
| `modules/backend/api/.../di/AppModule.kt` | Wire new service into Koin |
| `modules/backend/dal/.../repositories/DeityRepository.kt` | Add `findByNameNormalized` and `findOrCreate` |
| `conductor/tracks.md` | Register this track |

## Progress Log

| Date | Unit | Notes |
|:---|:---|:---|
| 2026-02-09 | Exploration | Analysed schema, DAL, DTOs, ExtractionResultProcessor |
| 2026-02-09 | Implementation | Created KrithiCreationFromExtractionService, updated processor + DI |
