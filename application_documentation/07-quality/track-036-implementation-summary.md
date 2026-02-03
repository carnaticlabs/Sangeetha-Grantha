| Metadata | Value |
|:---|:---|
| **Status** | Completed |
| **Version** | 1.1.0 |
| **Last Updated** | 2026-02-03 |
| **Author** | Sangita Grantha Team |

# Implementation Summary: TRACK-036 Enhanced TextBlocker Extraction Strategy

## 1. Executive Summary
Successfully enhanced the text extraction strategy to prioritize deterministic parsing via `TextBlocker` over LLM-based extraction. This change improves reliability for standard formats (especially Guru Guha and Vaibhavam blogs), reduces LLM token usage, and correctly handles complex structures like `MADHYAMA_KALA` sections interspersed within lyrics. Additionally, metadata extraction (Raga/Tala) was refined based on a 30-URL sample validation.

## 2. Key Changes

### A. Backend - `TextBlocker.kt` Evolution
- **Deterministic Extraction**: Introduced `extractSections(rawText): List<ScrapedSection>` to directly return strongly-typed sections.
- **Enhanced Regex**: Added support for `(madhyama kAla sAhityam)` and variations, ensuring they are correctly mapped to `RagaSectionDto.MADHYAMA_KALA`.
- **Noise Filtering**: Added boilerplate filtering for pronunciation guides (`A i I u U`, `ch j jh`) common in Vaibhavam blogs.
- **Mapping Integration**: Moved label-to-enum mapping logic from `WebScrapingService` into `TextBlocker`.

### B. Backend - `WebScrapingService.kt` Refactor
- **Hybrid Strategy**: Updated `scrapeKrithiInternal` to run `TextBlocker.extractSections` first.
- **Prioritization**: If `TextBlocker` finds sections, these are used directly, bypassing the LLM for section parsing.
- **Focused Prompting**: 
    - The Gemini prompt is dynamically adjusted. If sections are pre-extracted, the LLM is instructed to focus **only** on metadata and not re-parse lyrics.
    - **Metadata Refinement**: Explicitly directed the LLM to check the `=== HEADER META ===` section for Raga and Tala, as validation confirmed these details are reliably captured there by `TextBlocker`.

### C. Testing - Validation & Fixes
- **H2 Compatibility**: Updated `TestDatabaseFactory` to explicitly create Postgres ENUMs as types in the test database, ensuring compatibility for local testing.
- **New Test Cases**: Added `test dikshitar bala kuchambike structure` to `TextBlockerTest` to verify the specific user-reported issue with `MADHYAMA_KALA`.
- **Metadata Validation**: Created and ran `MetadataExtractionTest` against 30 random URLs to validate the availability of Raga/Tala in extracted headers.
- **Service Verification**: Updated `WebScrapingServiceTest` to ensure full stack verification (including Temple enrichment) using a local test database.

## 3. Verification Results
- **Section Extraction**: Validated against 11 sample URLs. 100% extraction success for sections.
- **Metadata Extraction**: Validated against 30 URLs. Confirmed that Raga and Tala information is present in the `metaLines` captured by `TextBlocker` for Dikshitar and Syama Sastri, and often for Thyagaraja (though Tala is sometimes implicit). The prompt refinement leverages this.
- **Unit Tests**: `TextBlockerTest` passed all scenarios.
- **Integration Tests**: `WebScrapingServiceTest` passed with real network calls and local DB persistence.

## 4. Files Modified/Created
- `modules/backend/api/src/main/kotlin/com/sangita/grantha/backend/api/services/scraping/TextBlocker.kt` (Modified)
- `modules/backend/api/src/main/kotlin/com/sangita/grantha/backend/api/services/WebScrapingService.kt` (Modified)
- `modules/backend/api/src/test/kotlin/com/sangita/grantha/backend/api/services/scraping/TextBlockerTest.kt` (Modified)
- `modules/backend/api/src/test/kotlin/com/sangita/grantha/backend/api/services/WebScrapingServiceTest.kt` (Modified)
- `modules/backend/api/src/test/kotlin/com/sangita/grantha/backend/api/support/TestDatabaseFactory.kt` (Modified)

## 5. References
- Ref: `conductor/tracks/TRACK-036-enhanced-text-blocker-extraction.md`