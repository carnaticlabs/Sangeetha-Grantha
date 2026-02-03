---
description: Comprehensive testing workflow for the bulk import pipeline covering backend unit tests, scraping services, and E2E flows.
---

# Bulk Import Testing

This workflow provides a systematic approach to testing the bulk import pipeline, which spans scraping, entity resolution, import processing, and review workflows.

## Test Categories

The bulk import system has tests at multiple layers:

| Layer | Test Files | Purpose |
|:---|:---|:---|
| Backend Unit | `ImportServiceTest.kt` | Core import logic |
| Scraping | `WebScrapingServiceTest.kt`, `TempleScrapingServiceTest.kt` | Web scraping |
| Text Processing | `TextBlockerTest.kt`, `ScrapeJsonSanitizerTest.kt` | Content parsing |
| Entity Resolution | `EntityResolutionServiceTest.kt` | Deduplication |
| Quality | `QualityScoringServiceTest.kt` | Quality scores |
| E2E | `e2e/tests/bulk-import-*.spec.ts` | Full UI flows |

## 1. Run All Bulk Import Tests

**Trigger:** "Test bulk import" or "Run import tests"

### Backend Tests
```bash
./gradlew :modules:backend:api:test --tests "*Import*" --tests "*Scrap*" --tests "*TextBlocker*" --tests "*EntityResolution*" --tests "*QualityScoring*"
```

### E2E Tests
```bash
cd modules/frontend/sangita-admin-web && bun run test:e2e -- tests/bulk-import-*.spec.ts
```

## 2. Test by Component

### 2.1 Import Service (Core Pipeline)

**Trigger:** "Test import service"

```bash
./gradlew :modules:backend:api:test --tests "ImportServiceTest"
```

**Covers:**
- `submitImports` - batch submission
- `reviewImport` - status transitions
- Import persistence

### 2.2 Web Scraping Services

**Trigger:** "Test scraping" or "Test web scraping"

```bash
./gradlew :modules:backend:api:test --tests "*ScrapingServiceTest"
```

**Test Files:**
- `WebScrapingServiceTest.kt` - General web scraping
- `TempleScrapingServiceTest.kt` - Temple-specific scraping

### 2.3 Text Processing

**Trigger:** "Test text processing" or "Test TextBlocker"

```bash
./gradlew :modules:backend:api:test --tests "TextBlockerTest" --tests "ScrapeJsonSanitizerTest" --tests "HtmlTextExtractorTest"
```

**Covers:**
- Section parsing (headers, lyrics, meaning)
- Devanagari/multi-script support
- JSON sanitization from LLM responses
- HTML to text extraction

### 2.4 Entity Resolution

**Trigger:** "Test entity resolution" or "Test deduplication"

```bash
./gradlew :modules:backend:api:test --tests "EntityResolutionServiceTest"
```

**Covers:**
- Composer matching/deduplication
- Ragam normalization
- Talam matching

### 2.5 Quality Scoring

**Trigger:** "Test quality scoring"

```bash
./gradlew :modules:backend:api:test --tests "QualityScoringServiceTest"
```

**Covers:**
- Completeness scoring
- Field validation
- Auto-approval thresholds

## 3. E2E Test Scenarios

### 3.1 Happy Path
```bash
cd modules/frontend/sangita-admin-web && bun run test:e2e -- tests/bulk-import-happy-path.spec.ts
```

**Flow:** Submit import → Process → Review → Approve → Verify in DB

### 3.2 Database Verification
```bash
cd modules/frontend/sangita-admin-web && bun run test:e2e -- tests/bulk-import-database.spec.ts
```

**Flow:** Submit → Verify records created in `krithi_import` table

### 3.3 Review Workflow
```bash
cd modules/frontend/sangita-admin-web && bun run test:e2e -- tests/bulk-import-review.spec.ts
```

**Flow:** Navigate review UI → Approve/Reject → Verify status updates

### 3.4 Error Handling
```bash
cd modules/frontend/sangita-admin-web && bun run test:e2e -- tests/bulk-import-error-cases.spec.ts
```

**Flow:** Invalid input → Verify error messages → Recovery

## 4. Troubleshoot Failures

### Backend Test Failures

**H2 Enum/Type Issues:**
```
Unknown data type: "IMPORT_STATUS"
```
Fix in `TestDatabaseFactory.kt`:
```kotlin
exec("CREATE DOMAIN IF NOT EXISTS IMPORT_STATUS AS VARCHAR")
```

**Missing Table:**
```
Table "KRITHI_IMPORT" not found
```
Add to `SchemaUtils.create(...)` in `TestDatabaseFactory.kt`.

**Constructor Mismatch:**
```
No value passed for parameter 'entityResolver'
```
Update test setup to match current service constructor.

### E2E Test Failures

**Import Not Processing:**
1. Check backend logs for errors
2. Verify Gemini API key is set (for scraping tests)
3. Check `e2e/fixtures/test-data.ts` for valid test URLs

**Review UI Not Loading:**
1. Verify imports exist: check `krithi_import` table
2. Check frontend console for API errors
3. Verify auth token in `e2e/.auth/user.json`

## 5. Test Data Setup

### Backend Tests
Tests use `TestDatabaseFactory` which creates an in-memory H2 database with the schema.

### E2E Tests
E2E tests use helpers in:
- `e2e/fixtures/test-data.ts` - Sample import data
- `e2e/fixtures/db-helpers.ts` - Direct DB operations
- `e2e/fixtures/shared-batch.ts` - Shared test state

### Reset Test State
```bash
# Reset local database
mise exec -- cargo run --manifest-path tools/sangita-cli/Cargo.toml -- db reset

# Clear E2E auth state
rm -f modules/frontend/sangita-admin-web/e2e/.auth/user.json
```

## 6. Full Regression Suite

**Trigger:** "Full import regression" or "Test all import features"

Run complete bulk import test suite:

```bash
# Step 1: Backend unit tests
./gradlew :modules:backend:api:test --tests "*Import*" --tests "*Scrap*" --tests "*TextBlocker*" --tests "*EntityResolution*" --tests "*QualityScoring*"

# Step 2: E2E tests (requires running stack)
cd modules/frontend/sangita-admin-web && bun run test:e2e -- tests/bulk-import-*.spec.ts
```

## 7. Related Tracks

Bulk import testing relates to these Conductor tracks:
- **TRACK-034**: TextBlocker parsing fixes
- **TRACK-032**: Multi-language lyric extraction
- **TRACK-029**: Kshetra & Temple mapping
- **TRACK-014**: Bulk Import Testing & QA (Proposed)

When fixing test failures, update the relevant track's progress log.
