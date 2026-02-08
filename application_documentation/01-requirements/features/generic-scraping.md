| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.1.0 |
| **Last Updated** | 2026-02-08 |
| **Author** | Sangeetha Grantha Team |

# Intelligent Web Scraping and Domain Mapping


---

## Context and Rationale

The Sangeetha Grantha application uses a rich domain model (`KrithiDto`) that represents Carnatic compositions with high fidelity. A key aspect of this model is the structured representation of lyrics into specific `RagaSectionDto` types:
- **Pallavi**
- **Anupallavi**
- **Charanam**

External sources of Carnatic music notation and lyrics (web archives, blogs) often present this data in semi-structured HTML formats. Simply extracting the full text as a blob loses the semantic structure required by our domain model. To bridge this gap and ensure high-quality data ingestion, we are implementing an **Intelligent Web Scraping Service**.

## Feature Overview

This feature leverages Generative AI (Google Gemini) to transform unstructured or semi-structured web content into our strict domain entities. It is designed to be **source-agnostic**, capable of interpreting visual layouts from various websites (e.g., Shivkumar.org, blogspot archives) and mapping them to our internal data structures.

## Capabilities

### 1. Structure-Aware parsing
The core value of this feature is not just downloading text, but understanding it. The service:
- Analyzes the HTML structure (preserving visual cues like paragraphs and line breaks).
- Identifies section headers (e.g., "Pallavi", "Anupallavi") implicitly or explicitly.
- Maps extracted text segments directly to `RagaSectionDto` enums (`PALLAVI`, `ANUPALLAVI`, `CHARANAM`).

### 2. Intelligent Field Extraction
Beyond sections, the service extracts metadata crucial for the `KrithiDto`:
- **Composer**, **Raga**, **Tala**
- **Deity** and **Temple** context
- **Language** detection
- **Notation** separation (distinguishing musical notation from lyrical text)

### 3. Domain Model Alignment
The output is strictly typed to align with the database schema:
- **Sections**: Returned as a list of `ScrapedSectionDto` objects, which directly map to `KrithiNotationSectionGroupDto` and `KrithiNotationRowDto` structures during the import process.
- **Lyrics**: While the structured sections are the primary target, the system also ensures a full lyrical representation is available as a fallback or aggregate view.

## Implementation Strategy

### WebScrapingService
The `WebScrapingService` exposes a `scrapeKrithi(url: String)` method.
- **Input**: Any public URL containing Carnatic compositions.
- **Process**:
    1.  **Fetch**: Downloads HTML content.
    2.  **Clean**: sanitizes the HTML while strictly preserving structural markers (newlines, block elements) that denote logical separation of content.
    3.  **Analyze**: Uses a specialized LLM prompt to "read" the visual structure and extract the JSON representation.
- **Output**: `ScrapedKrithiMetadata` containing both flat metadata and the critical `sections` list.

### Integration Plan
This service powers the **Import Workflow**. When an administrator provides a URL:
1.  The system scrapes and structures the data.
2.  The resulting JSON is presented in the review UI.
3.  Upon approval, the `sections` are persisted as distinct entities in the database, preserving the structural integrity of the composition from the start.