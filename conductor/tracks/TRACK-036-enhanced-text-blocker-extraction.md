| Metadata | Value |
|:---|:---|
| **Status** | Completed |
| **Version** | 1.2.0 |
| **Last Updated** | 2026-02-03 |
| **Author** | Sangita Grantha Team |

# TRACK-036: Enhanced TextBlocker Extraction Strategy

## 1. Goal
Shift the primary responsibility for **Lyric and Section Extraction** from the LLM-based `WebScrapingService` to the deterministic `TextBlocker` component. This ensures consistent, accurate parsing of standard formats (like `guru-guha.blogspot.com`) while reserving the LLM for high-value enrichment (metadata, meaning, translations).

## 2. Context
- **Current State**: `WebScrapingService` fetches HTML, extracts text, and sends it to Gemini to parse sections (Pallavi, Charanam, etc.) and metadata. `TextBlocker` is used mainly to structure the prompt, with a fallback logic (`deriveSectionsFromBlocks`) that is secondary.
- **Problem**: Relying on LLMs for structural extraction is token-expensive and occasionally prone to formatting hallucinations, whereas the source text (especially on blogs like Guru Guha) is highly structured and regex-friendly.
- **Opportunity**: `TextBlocker` already identifies blocks. We can promote it to be the definitive source for sections, significantly reducing LLM load and increasing reliability for supported formats.

## 3. Implementation Plan

### Phase 1: Enhance `TextBlocker` (The Parser)
- [x] Refactor `TextBlocker` to return strongly-typed `List<ScrapedSectionDto>` instead of just `PromptBlocks`.
- [x] Move the label-to-enum mapping logic (currently in `WebScrapingService`) into `TextBlocker` or a dedicated `SectionMapper`.
- [x] Verify `guru-guha` specific patterns:
    - Handle `Samashti Charanam` followed by `(madhyama kAla sAhityam)`.
    - Ensure "Madhyama Kala" is treated as a distinct section or correctly associated suffix based on domain rules (typically `MADHYAMA_KALA` is a section in our Enum).
- [x] Add unit tests for the specific URL example provided (`bAla kucAmbikE`).

### Phase 2: Refactor `WebScrapingService` (The Orchestrator)
- [x] Update `scrapeKrithi` flow:
    1. Fetch & Clean Text.
    2. Run `TextBlocker.extractSections()`.
    3. **Decision Point**:
        - If `TextBlocker` returns a valid set of sections (e.g., contains PALLAVI), **prioritize these sections**.
        - Modify the Gemini Prompt:
            - If sections are found: "Here are the extracted lyrics/sections. Please extract ONLY metadata (Raga, Tala, Meaning, Temple) and do not re-parse the lyrics."
            - OR simply populate the prompt with the text but instruct Gemini to focus on missing fields.
    4. Merge strategies: Use `TextBlocker` sections + Gemini Metadata.

### Phase 3: Verification
- [x] Test with `https://guru-guha.blogspot.com/2008/02/dikshitar-kriti-bala-kuchambike-raga.html`.
- [x] Verify that `MADHYAMA_KALA` is correctly extracted.
- [x] Validate Raga/Tala extraction availability across 30 random URLs.
- [x] Ensure no regression for other sites.

## 4. Progress Log
- **2026-02-03**: Refactored `TextBlocker` to support deterministic section extraction using `extractSections` method. Added mapping for `RagaSectionDto`.
- **2026-02-03**: Enhanced `TextBlocker` regex to support `(madhyama kAla sAhityam)` and variations, mapping them to `MADHYAMA_KALA`.
- **2026-02-03**: Updated `WebScrapingService` to prioritize sections extracted by `TextBlocker` and adjusted the Gemini prompt to focus on metadata when sections are already found.
- **2026-02-03**: Validated Raga/Tala extraction against 30 random URLs from import CSVs. Confirmed reliable presence in header meta for Dikshitar/Syama Sastri and refined LLM prompt to explicitly target `HEADER META`.
- **2026-02-03**: Refined Gemini prompt to remove internal "TRACK-032" reference and clarify multi-language extraction instructions.
- **2026-02-03**: Verified changes with `TextBlockerTest` (including `bAla kRshNaM bhAvayAmi` case) and `WebScrapingServiceTest`. All tests passed.
- **2026-02-03**: Implemented **Deterministic Variant Extraction** in `TextBlocker` and optimized `WebScrapingService`. Now, if deterministic sections/variants are found, the prompt sent to Gemini EXCLUDES the lyric blocks, reducing token usage by ~90% while preserving multi-script data.

## 5. References
- `modules/backend/api/src/main/kotlin/com/sangita/grantha/backend/api/services/scraping/TextBlocker.kt`
- `modules/backend/api/src/main/kotlin/com/sangita/grantha/backend/api/services/WebScrapingService.kt`
