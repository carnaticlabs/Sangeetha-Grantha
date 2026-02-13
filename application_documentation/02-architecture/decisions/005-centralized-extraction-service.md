| Metadata | Value |
|:---|:---|
| **Status** | Proposed |
| **Version** | 1.1.0 |
| **Last Updated** | 2026-02-12 |
| **Author** | Sangita Grantha Architect |

# Architectural Proposal: Centralized Extraction Service (Python UEE)

## 1. Context & Motivation
The [Technical Retrospective (2026-02-12)](../../11-retrospective/remediation-technical-retrospective-2026-02-12.md) identified that the "Heuristic Split" between Kotlin and Python is the root cause of ingestion failures. Specifically:
1.  **Dirty Data**: Inconsistent splitting of multi-language variants and merged "Meaning/Notes" blocks.
2.  **Stale Logic**: Fixes for section detection (e.g. parenthesized Madhyama Kala) required manual porting across languages.
3.  **Orchestration Stalls**: Task count mismatches between manifest ingest and resolution stages.

## 2. Proposed Architecture: The Unified Extraction Engine (UEE)

### 2.1. Centralized Heuristics (Single Source of Truth)
The Python `structure_parser.py` will be the exclusive owner of:
-   **Section Detection**: Advanced regex for Pallavi, Anupallavi, Charanam, and Madhyama Kala (supporting all parenthesized and script-specific variations).
-   **Metadata Isolation**: **MANDATORY** separation of `MEANING`, `NOTES`, `GIST`, and `WORD_DIVISION` into their own JSON fields. These must never be appended to lyric sections.
-   **Multi-Script Variant Splitting**: Python will use `BeautifulSoup4` to detect language boundaries in HTML (e.g., Blogspot tags or script changes) and produce separate `CanonicalLyricVariant` objects. This prevents the "Merged Language" bug where Telugu and Malayalam were stored in the same row.

### 2.2. Identity Resolution & Fuzzy Matching
Moving candidate discovery to Python leverages high-performance libraries:
-   **RapidFuzz Integration**: Python will propose entity matches (Raga/Composer) in the `resolutionCandidates` field.
-   **Affinity Logic**: Candidates will be weighted by composer (e.g. if source is `guru-guha`, bias heavily toward `Muthuswami Dikshitar`).
-   **Thresholding**: Implement the 75% (Metadata match) and 85% (Title only) logic centrally.

## 3. Implementation Strategy

### 3.1. Expanded Extraction Queue
The `extraction_queue` will support `HTML`, `PDF`, and `DOCX`.
1.  **Async Flow**: Kotlin writes a `PENDING` task; Python polls and processes.
2.  **Orchestration Integrity**: The `BulkImportRepository` must atomically increment `total_tasks` whenever a sub-task (like Resolution) is spawned, ensuring progress bars never stall at 50%.

### 3.2. Manual Intervention UI
Kotlin remains the owner of the **Decision UI**:
-   **Entity Resolution Mode**: If Python provides candidates with < 90% confidence, the UI triggers a "Resolution Required" banner: *"We found 'Begada' in CSV. Database has 'Begada (29.1)'. Is this a match?"*
-   **Truth Persistence**: User confirmations are written to the `entity_resolution_cache` to train future extractions.

### 3.3. Kotlin Simplification (The "Pure Ingestor")
-   **Decommission**: `KrithiStructureParser.kt`, `DeterministicWebScraper.kt`.
-   **Responsibility**: Kotlin receives clean JSON, resolves IDs via the cache, and performs the final `INSERT`.

## 4. Detailed Impact Analysis

| Feature | Current State (Brittle) | Proposed UEE (Robust) |
|:---|:---|:---|
| **Language Logic** | Kotlin tries to split scraped text. | Python splits at the source (HTML nodes). |
| **Heuristics** | Duplicated in 2 languages. | Single Python module for all formats. |
| **Data Quality** | Meanings merged into lyrics. | Hard-stop at Metadata markers. |
| **Progress Tracking** | Stalls at job boundaries. | Dynamic total_tasks incrementing. |

## 5. Migration Plan
1.  **Phase 1 (Heuristic Consolidation)**: Port all Kotlin regexes to Python `structure_parser.py`.
2.  **Phase 2 (HTML Scraper)**: Build `HtmlExtractor.py` using `BeautifulSoup4`.
3.  **Phase 3 (Orchestration Fix)**: Update `BulkImportRepository` to support dynamic task totals.
4.  **Phase 4 (Integration)**: Refactor `ImportService.kt` to call the UEE queue.
5.  **Phase 5 (Verification)**: Perform a `db reset` and run `Dikshitar-Krithi-Test-20.csv` to verify zero "dirty data" variants.

## 6. Conclusion
Centralizing the "Intelligence" of the pipeline in Python transforms the Kotlin backend into a durable "System of Record" that is decoupled from the volatility of web scraping and document parsing.
