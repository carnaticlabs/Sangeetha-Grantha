---
description: Comprehensive testing workflow for the bulk import pipeline covering backend unit tests and E2E flows.
---

# Bulk Import Testing

This workflow provides a systematic approach to testing the bulk import pipeline, which spans scraping, entity resolution, import processing, and review workflows.

## Test Categories

The bulk import system has tests at multiple layers:

| Layer | Test Files | Purpose |
|:---|:---|:---|
| Backend Unit | `ImportServiceTest.kt` | Core import logic |
| Extraction ingest | `ExtractionResultProcessorTest.kt`, `ExtractionResultProcessorUnitTest.kt` | Python `CanonicalExtraction` → Kotlin persistence |
| Structural voting | `StructuralVotingRoundTripTest.kt` | Multi-source section reconciliation |
| Entity Resolution | `EntityResolutionServiceTest.kt` | Deduplication |
| Quality | `QualityScoringServiceTest.kt` | Quality scores |
| E2E | `e2e/tests/bulk-import-*.spec.ts` | Full UI flows (deferred) |

## 1. Run All Backend Tests

**Trigger:** "Test bulk import" or "Run import tests"

```bash
./gradlew :modules:backend:api:test --tests "*Import*" --tests "*EntityResolution*" --tests "*QualityScoring*" --tests "*ExtractionResultProcessor*"
```

Or run the full backend test suite:

```bash
make test
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

### 2.2 Section parsing (Python)

**Trigger:** "Test structure parser" or "Test section headers"

Section headers are owned by the Python worker, not Kotlin.

```bash
cd tools/krithi-extract-enrich-worker && uv run pytest tests/ -k structure_parser
```

### 2.3 Entity Resolution

**Trigger:** "Test entity resolution" or "Test deduplication"

```bash
./gradlew :modules:backend:api:test --tests "EntityResolutionServiceTest"
```

### 2.4 Extraction Result Processing

**Trigger:** "Test extraction" or "Test PDF pipeline"

```bash
./gradlew :modules:backend:api:test --tests "*ExtractionResultProcessor*"
```

**Covers:**
- Fuzzy matching of PDF extractions to existing krithis
- Unmatched extractions routed to imported_krithis as PENDING
- Source evidence creation for matched extractions

## 3. Troubleshoot Failures

### Backend Test Failures

**Constructor Mismatch:**
```text
No value passed for parameter 'entityResolver'
```
Update test setup to match current service constructor. Check `AppModule.kt` for the current DI wiring.

**Database Schema Issues:**
Tests use the real PostgreSQL schema via Docker. Ensure `make db-reset` has been run to apply all migrations.

### Reset Test State

```bash
# Reset local database (drop → create → migrate → seed)
make db-reset
```

## 4. Full Regression Suite

**Trigger:** "Full import regression" or "Test all import features"

```bash
make test
```

## 5. Related Tracks

Bulk import testing relates to these Conductor tracks:
- **TRACK-034**: TextBlocker parsing fixes
- **TRACK-032**: Multi-language lyric extraction
- **TRACK-029**: Kshetra & Temple mapping
- **TRACK-079**: E2E pipeline validation & section fix
- **TRACK-080**: Curator review UI

When fixing test failures, update the relevant track's progress log.
