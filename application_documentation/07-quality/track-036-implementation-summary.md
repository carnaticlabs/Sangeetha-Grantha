| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-02-03 |
| **Author** | Sangita Grantha Team |

# Implementation Summary: TRACK-036 Enhanced TextBlocker Extraction Strategy

## 1. Executive Summary
Successfully enhanced the text extraction strategy to prioritize deterministic parsing via `TextBlocker` over LLM-based extraction. This change improves reliability for standard formats (especially Guru Guha and Vaibhavam blogs), reduces LLM token usage, and correctly handles complex structures like `MADHYAMA_KALA` sections interspersed within lyrics.

## 2. Key Changes

### A. Backend - `TextBlocker.kt` Evolution
- **Deterministic Extraction**: Introduced `extractSections(rawText): List<ScrapedSection>` to directly return strongly-typed sections.
- **Enhanced Regex**: Added support for `(madhyama kAla sAhityam)` and variations, ensuring they are correctly mapped to `RagaSectionDto.MADHYAMA_KALA`.
- **Noise Filtering**: Added boilerplate filtering for pronunciation guides (`A i I u U`, `ch j jh`) common in Vaibhavam blogs.
- **Mapping Integration**: Moved label-to-enum mapping logic from `WebScrapingService` into `TextBlocker`.

### B. Backend - `WebScrapingService.kt` Refactor
- **Hybrid Strategy**: Updated `scrapeKrithiInternal` to run `TextBlocker.extractSections` first.
- **Prioritization**: If `TextBlocker` finds sections, these are used directly, bypassing the LLM for section parsing.
- **Focused Prompting**: The Gemini prompt is dynamically adjusted. If sections are pre-extracted, the LLM is instructed to focus **only** on metadata (Raga, Tala, Meaning, Temple) and not re-parse lyrics.

### C. Testing - Validation & Fixes
- **H2 Compatibility**: Updated `TestDatabaseFactory` to explicitly create Postgres ENUMs as types in the test database, ensuring compatibility for local testing.
- **New Test Cases**: Added `test dikshitar bala kuchambike structure` to `TextBlockerTest` to verify the specific user-reported issue with `MADHYAMA_KALA`.
- **Service Verification**: Updated `WebScrapingServiceTest` to ensure full stack verification (including Temple enrichment) using a local test database.

## 3. Verification Results
- **Hypothesis Validation**: Validated against 11 sample URLs from Dikshitar and Thyagaraja/Syama Sastri blogs. 100% extraction success.
- **Unit Tests**: `TextBlockerTest` passed all scenarios, including complex nested sections.
- **Integration Tests**: `WebScrapingServiceTest` passed with real network calls (to Gemini and target sites) and local DB persistence.

## 4. Files Modified/Created
- `modules/backend/api/src/main/kotlin/com/sangita/grantha/backend/api/services/scraping/TextBlocker.kt` (Modified)
- `modules/backend/api/src/main/kotlin/com/sangita/grantha/backend/api/services/WebScrapingService.kt` (Modified)
- `modules/backend/api/src/test/kotlin/com/sangita/grantha/backend/api/services/scraping/TextBlockerTest.kt` (Modified)
- `modules/backend/api/src/test/kotlin/com/sangita/grantha/backend/api/services/WebScrapingServiceTest.kt` (Modified)
- `modules/backend/api/src/test/kotlin/com/sangita/grantha/backend/api/support/TestDatabaseFactory.kt` (Modified)

## 5. References
- Ref: `conductor/tracks/TRACK-036-enhanced-text-blocker-extraction.md`
