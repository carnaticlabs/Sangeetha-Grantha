# TRACK-099: Backend Compiler Warning Cleanup — Zero Warnings Target

## Status: Completed

## Progress Log
- 2026-03-17: Implemented all legacy code removals and DTO migrations to clear compiler warnings. Staged for commit.

## Summary

Eliminate all 53 Kotlin compiler warnings across 9 backend files to achieve a zero-warning build. The vast majority (50/53) stem from TRACK-096 deprecated types that are still referenced by legacy scraping code. The remaining 3 are code quality issues.

## Warning Inventory

### By Deprecated Type (50 warnings)

| Deprecated Type | Replacement | Count |
|----------------|-------------|-------|
| `ScrapedKrithiMetadata` | `CanonicalExtractionDto` | 20 |
| `ScrapedSectionDto` | `CanonicalSectionDto` | 14 |
| `KrithiStructureParser` | Python `structure_parser.py` | 7 |
| `IWebScraper` | Remove (Python worker handles extraction) | 4 |
| `ScrapedLyricVariantDto` | `CanonicalLyricVariantDto` | 3 |
| `ScrapedTempleDetails` | `CanonicalExtractionDto` temple fields | 2 |

### Code Quality (3 warnings)

| Warning | File | Line |
|---------|------|------|
| Condition is always `true` | `ImportService.kt` | 491 |
| Condition is always `true` | `ImportService.kt` | 517 |
| Unnecessary safe call on non-null `String` | `ImportRoutes.kt` | 186 |

### By File (53 warnings)

| File | Warnings | Category |
|------|----------|----------|
| `api/services/WebScrapingService.kt` | 23 | Legacy scraping — candidate for removal |
| `api/services/DeterministicWebScraper.kt` | 11 | Legacy scraping — candidate for removal |
| `api/services/LyricVariantPersistenceService.kt` | 7 | Active — migrate to Canonical DTOs |
| `api/services/ImportService.kt` | 4 | 2 deprecated refs + 2 code quality |
| `api/services/scraping/StructuralVotingEngine.kt` | 3 | Legacy scraping — migrate to Canonical DTOs |
| `api/di/AppModule.kt` | 2 | Remove IWebScraper DI binding |
| `api/services/StructuralVotingProcessor.kt` | 1 | Migrate ScrapedSectionDto ref |
| `api/services/ScrapingPromptBuilder.kt` | 1 | Remove KrithiStructureParser import |
| `api/routes/ImportRoutes.kt` | 1 | Fix unnecessary safe call |

## Approach

### Phase 1: Remove Dead Code (target: ~38 warnings)
Files that implement the legacy Kotlin scraping pipeline superseded by the Python extraction worker:
- **Delete or gut** `WebScrapingService.kt` (23 warnings) — if no active callers remain
- **Delete or gut** `DeterministicWebScraper.kt` (11 warnings) — if no active callers remain
- **Remove** `IWebScraper` binding from `AppModule.kt` (2 warnings)
- **Remove** `KrithiStructureParser` import from `ScrapingPromptBuilder.kt` (1 warning)
- **Remove** `KrithiStructureParser` usage from `LyricVariantPersistenceService.kt` (1 warning — constructor call)

### Phase 2: Migrate Active Code to Canonical DTOs (target: ~12 warnings)
Files that are actively used but still reference deprecated DTOs:
- `LyricVariantPersistenceService.kt` — migrate `ScrapedSectionDto` → `CanonicalSectionDto` (6 remaining warnings)
- `ImportService.kt` — migrate `ScrapedKrithiMetadata` → `CanonicalExtractionDto` (2 warnings)
- `StructuralVotingEngine.kt` — migrate `ScrapedSectionDto` → `CanonicalSectionDto` (3 warnings)
- `StructuralVotingProcessor.kt` — migrate `ScrapedSectionDto` → `CanonicalSectionDto` (1 warning)

### Phase 3: Fix Code Quality (target: 3 warnings)
- `ImportService.kt:491,517` — remove redundant `true` conditions
- `ImportRoutes.kt:186` — remove unnecessary `?.` safe call

## Parallelization Strategy

Three parallel agents:
- **Agent 1 — Dead Code Removal**: Phase 1 files (WebScrapingService, DeterministicWebScraper, AppModule, ScrapingPromptBuilder)
- **Agent 2 — DTO Migration**: Phase 2 files (LyricVariantPersistenceService, ImportService, StructuralVotingEngine, StructuralVotingProcessor)
- **Agent 3 — Code Quality Fixes**: Phase 3 files (ImportService, ImportRoutes)

## Verification

```bash
# Must produce 0 lines of "w:" output
./gradlew :modules:backend:dal:clean :modules:backend:api:clean \
  :modules:backend:dal:compileKotlin :modules:backend:api:compileKotlin 2>&1 | grep "w:"

# All tests must pass
./gradlew :modules:backend:api:test
```

## Dependencies

- TRACK-096 (Payload Format Convergence) — this track completes the cleanup that TRACK-096 initiated by marking types as deprecated. Coordinate to avoid conflicts.

## Risks

- `WebScrapingService` and `DeterministicWebScraper` may still have active route callers — verify with usage analysis before deletion
- DTO migration in `LyricVariantPersistenceService` touches critical lyric persistence logic — test thoroughly

## Ref
Ref: application_documentation/02-architecture/tech-stack.md
