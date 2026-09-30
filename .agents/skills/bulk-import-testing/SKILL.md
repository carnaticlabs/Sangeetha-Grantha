---
name: bulk-import-testing
description: Comprehensive testing workflow for the Sangita Grantha bulk import pipeline covering backend unit tests, Python extraction worker structure parsing, entity resolution, and E2E flows. Use when testing imports, verifying parser changes, or validating import regressions.
---

# Bulk Import Testing

This skill provides a systematic, multi-layered approach to testing the bulk import pipeline, which spans web scraping, entity resolution, Python section parsing, backend import processing, and curator review workflows.

## Test Matrix by Architecture Layer

| Layer | Primary Test Files | Purpose |
|:---|:---|:---|
| **Backend Unit** | `ImportServiceTest.kt` | Core batch submission and status transitions |
| **Extraction Ingestion** | `ExtractionResultProcessorTest.kt`, `ExtractionResultProcessorUnitTest.kt` | Python `CanonicalExtractionDto` mapping to database |
| **Structural Reconciliation** | `StructuralVotingRoundTripTest.kt` | Multi-source section voting and lyric alignment |
| **Entity Resolution** | `EntityResolutionServiceTest.kt` | Deduplication and fuzzy matching of compositions |
| **Quality Scoring** | `QualityScoringServiceTest.kt` | Metadata quality metrics calculation |
| **Python Structure Parser** | `tools/krithi-extract-enrich-worker/tests/` | Section header regex and text extraction |
| **E2E Journeys** | `modules/frontend/sangita-admin-web/e2e/tests/bulk-import-*.spec.ts` | Full curator review UI flows |

---

## 1. Run Backend Import Tests

To run the full suite of backend import and entity resolution tests:

```bash
./gradlew :modules:backend:api:test \
  --tests "*Import*" \
  --tests "*EntityResolution*" \
  --tests "*QualityScoring*" \
  --tests "*ExtractionResultProcessor*"
```

Or execute the complete backend suite:
```bash
make test
```

---

## 2. Component-Level Testing

### 2.1 Import Service (Core Pipeline)
Covers batch submission, curator review transitions, and import persistence:
```bash
./gradlew :modules:backend:api:test --tests "ImportServiceTest"
```

### 2.2 Python Structure Parser (Section Detection)
Section header detection is owned by the Python extraction worker, not Kotlin:
```bash
cd tools/krithi-extract-enrich-worker && uv run pytest tests/ -k structure_parser
```

### 2.3 Entity Resolution & Deduplication
Validates fuzzy matching, raga/composer ID matching, and duplicate candidate selection:
```bash
./gradlew :modules:backend:api:test --tests "EntityResolutionServiceTest"
```

### 2.4 Extraction Result Processing
Validates fuzzy matching of extractions to existing krithis, pending routing, and evidence tracking:
```bash
./gradlew :modules:backend:api:test --tests "*ExtractionResultProcessor*"
```

---

## 3. Diagnosing Test Failures

### Constructor / DI Signature Mismatches
**Symptom**: `No value passed for parameter 'entityResolver'`
- Check service constructor signatures against Koin dependency injection in `modules/backend/api/src/main/kotlin/.../di/AppModule.kt`.
- Update unit test mocks or factory instantiations accordingly.

### Database Schema Out of Sync
**Symptom**: `Table "xyz" not found` or SQL migration discrepancies.
- Reset the local database (drop -> create -> migrate with Flyway -> seed):
```bash
make db-reset
```

---

## 4. Full Regression Verification

Before finalizing pipeline changes:
```bash
# 1. Verify backend tests pass
make test

# 2. Verify Python worker tests pass
cd tools/krithi-extract-enrich-worker && uv run pytest
```
