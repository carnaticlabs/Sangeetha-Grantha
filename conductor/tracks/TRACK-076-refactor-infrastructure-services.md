| Metadata | Value |
|:---|:---|
| **Track ID** | TRACK-076 |
| **Title** | Refactor Infrastructure Services |
| **Status** | Completed |
| **Created** | 2026-02-28 |
| **Author** | Sangeetha Grantha Team |
| **Depends On** | TRACK-073 |

# TRACK-076: Refactor Infrastructure Services

## Objective

Split `GeminiApiClient.kt` (471), `WebScrapingService.kt` (456), `KrithiStructureParser.kt` (547), and `AuditRunnerService.kt` (474) into focused, sub-300-line files.

## Motivation

- KrithiStructureParser has 50+ regex patterns in section header detection (547 lines)
- GeminiApiClient mixes HTTP client, retry strategy, and JSON sanitization (471 lines)
- WebScrapingService mixes orchestration, prompt building, and JSON schema definitions (456 lines)
- AuditRunnerService has 3 distinct audit types with 10 embedded SQL queries (474 lines)

## Scope

### Phase 1: Expand Tests

- Expand `GeminiApiClientTest.kt` (1 to ~8 tests): retry logic, rate limiting, error handling
- Expand `WebScrapingServiceTest.kt` (1 to ~8 tests): prompt building, caching
- Expand `KrithiStructureParserTest.kt` (5 to ~15 tests): regex pattern coverage
- Write `AuditRunnerServiceTest.kt` (~10 tests): SQL result aggregation

### Phase 2: Split GeminiApiClient.kt

| Extract To | Responsibility | ~Lines |
|-----------|---------------|--------|
| `GeminiRetryStrategy.kt` | shouldRetry, handleThrottle, jitteredDelay, backoff logic | ~150 |
| `GeminiApiClient.kt` (slimmed) | generateContent, generateStructured, HTTP client | ~280 |

### Phase 3: Split WebScrapingService.kt

| Extract To | Responsibility | ~Lines |
|-----------|---------------|--------|
| `ScrapingPromptBuilder.kt` | buildStructuredText, buildPrompt, JSON schema builders | ~180 |
| `WebScrapingService.kt` (slimmed) | scrapeKrithi, caching, orchestration | ~250 |

### Phase 4: Split KrithiStructureParser.kt

| Extract To | Responsibility | ~Lines |
|-----------|---------------|--------|
| `SectionHeaderDetector.kt` | detectHeader, detectLanguageHeader, detectSectionHeader, regex patterns | ~200 |
| `KrithiStructureParser.kt` (slimmed) | buildBlocks, extractSections, extractLyricVariants | ~280 |

### Phase 5: Split AuditRunnerService.kt

| Extract To | Responsibility | ~Lines |
|-----------|---------------|--------|
| `SectionCountAudit.kt` | runSectionCountAudit + SQL queries | ~150 |
| `LabelSequenceAudit.kt` | runLabelSequenceAudit + SQL queries | ~150 |
| `OrphanedBlobsAudit.kt` | runOrphanedBlobsAudit + SQL queries | ~150 |
| `AuditRunnerService.kt` (slimmed) | runFullAudit orchestration | ~80 |

## Verification

1. `./gradlew :modules:backend:api:test` -- 0 failures
2. No file exceeds 300 lines
3. E2E: `mise exec -- cargo run --manifest-path tools/sangita-cli/Cargo.toml -- test steel-thread`
