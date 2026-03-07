| Metadata | Value |
|:---|:---|
| **Track ID** | TRACK-075 |
| **Title** | Refactor Core Services |
| **Status** | Completed |
| **Created** | 2026-02-28 |
| **Author** | Sangeetha Grantha Team |
| **Depends On** | TRACK-074 |

# TRACK-075: Refactor Core Services

## Objective

Split `ImportService.kt` (906), `ExtractionResultProcessor.kt` (604), `ReferenceDataService.kt` (579), and `VariantMatchingService.kt` (420) into focused, sub-300-line services.

## Motivation

- ImportService is the largest service file (906 lines), mixing import workflow, lyric persistence, and report generation
- ExtractionResultProcessor (604 lines) mixes orchestration, matching, voting, and variant enrichment
- ReferenceDataService (579 lines) has repetitive CRUD boilerplate across 7 entity types
- VariantMatchingService (420 lines) mixes scoring, persistence, and review workflows

## Scope

### Phase 1: Expand Tests

- Expand `ImportServiceTest.kt` (9 to ~25 tests): lyric persistence, reporting, batch ops
- Expand `ExtractionResultProcessorUnitTest.kt` (8 to ~20 tests): voting, error handling
- Write `ReferenceDataServiceTest.kt` (~14 tests, 2 per entity type)
- Expand `VariantMatchingServiceTest.kt` (14 to ~20 tests): edge cases

### Phase 2: Split ImportService.kt

| Extract To | Responsibility | ~Lines |
|-----------|---------------|--------|
| `LyricVariantPersistenceService.kt` | persistLyricVariants, script/language normalization | ~250 |
| `ImportReportGenerator.kt` | generateQAReport, generateJsonReport, generateCsvReport | ~200 |
| `ImportService.kt` (slimmed) | submitImports, reviewImport, batch ops | ~280 |

### Phase 3: Split ExtractionResultProcessor.kt

| Extract To | Responsibility | ~Lines |
|-----------|---------------|--------|
| `KrithiMatcherService.kt` | processExtractionResult, candidate scoring, tiebreaker | ~200 |
| `StructuralVotingProcessor.kt` | runVotingForKrithi, consensus building | ~180 |
| `ExtractionResultProcessor.kt` (slimmed) | processCompletedExtractions loop, linkImports | ~220 |

### Phase 4: Refactor ReferenceDataService.kt

- Extract generic CRUD helper or split into per-entity services
- Target: ~250 lines total (reduce repetitive boilerplate)

### Phase 5: Split VariantMatchingService.kt

| Extract To | Responsibility | ~Lines |
|-----------|---------------|--------|
| `VariantScorer.kt` | computeMatchSignals, levenshtein, structure mismatch | ~150 |
| `VariantMatchingService.kt` (slimmed) | matchVariants, reviewMatch, listMatches, persistMatch | ~250 |

## Verification

1. `./gradlew :modules:backend:api:test` -- 0 failures
2. No file exceeds 300 lines
3. All new services properly wired in routes/DI
