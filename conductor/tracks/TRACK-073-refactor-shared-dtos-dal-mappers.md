| Metadata | Value |
|:---|:---|
| **Track ID** | TRACK-073 |
| **Title** | Refactor Shared DTOs + DAL Mappers |
| **Status** | Complete |
| **Created** | 2026-02-28 |
| **Author** | Sangeetha Grantha Team |
| **Depends On** | None |

# TRACK-073: Refactor Shared DTOs + DAL Mappers

## Objective

Split `SourcingDtos.kt` (349 lines) and `DtoMappers.kt` (411 lines) into focused, sub-300-line files grouped by domain concern.

## Motivation

- Both files exceed 300 lines and mix unrelated domain concepts
- `SourcingDtos.kt` contains 22 DTOs spanning extraction, evidence, voting, and variant matching
- `DtoMappers.kt` contains 31 mapper functions spanning core entities, krithis, and import orchestration
- Splitting improves discoverability and reduces merge conflicts

## Scope

### Phase 1: Tests

- Write `DtoMappersTest.kt` (~10 tests) verifying field mapping correctness for key mappers
- Add serialization round-trip tests for SourcingDtos to catch `@Serializable` issues after move

### Phase 2: Split SourcingDtos.kt

| Extract To | Contents | ~Lines |
|-----------|----------|--------|
| `ExtractionDtos.kt` | ImportSource*, Extraction*, CreateExtractionRequestDto, ExtractionStatsDto | ~120 |
| `EvidenceVotingDtos.kt` | SourceEvidence*, KrithiEvidence*, Voting*, QualitySummaryDto | ~120 |
| `VariantMatchDtos.kt` | VariantMatch*, VariantMatchReportDto, PaginatedResponse | ~110 |

### Phase 3: Split DtoMappers.kt

| Extract To | Contents | ~Lines |
|-----------|----------|--------|
| `CoreEntityMappers.kt` | Composer, Raga, Tala, Deity, Temple, Tag, Sampradaya, User, Role mappers + enum converters | ~130 |
| `KrithiDtoMappers.kt` | Krithi, Notation, Section, LyricVariant, LyricSection mappers | ~150 |
| `ImportDtoMappers.kt` | ImportSource, ImportedKrithi, AuditLog, Batch, Job, Task, Event, Cache mappers | ~130 |

### Files Changed

- `modules/shared/domain/src/commonMain/kotlin/com/sangita/grantha/shared/domain/model/SourcingDtos.kt` (split into 3)
- `modules/backend/dal/src/main/kotlin/com/sangita/grantha/backend/dal/models/DtoMappers.kt` (split into 3)
- All files importing from the above (update import statements)

## Verification

1. `./gradlew :modules:backend:api:test` -- 0 failures
2. `./gradlew :modules:shared:domain:build` -- compiles
3. No file exceeds 300 lines
