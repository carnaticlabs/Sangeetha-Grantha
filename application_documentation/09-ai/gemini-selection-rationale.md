| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.1.0 |
| **Last Updated** | 2026-02-08 |
| **Author** | Sangeetha Grantha Team |

# Google Gemini Selection Rationale


> **Related**: [ADR-006](../02-architecture/decisions/ADR-006-gemini-integration.md)

## 1. Executive Summary

This document details the rationale behind selecting **Google Gemini** as the primary AI engine for the Sangita Grantha platform. The decision focuses on specific capabilities required for **Carnatic Music** domain processing: Indic language proficiency, cost-effective batch processing, and multimodal extraction.

## 2. Key Requirements & Evaluation Criteria

To support the "Intelligent Content Ingestion" feature, the AI model must excel in:

1.  **Indic Language Transliteration**: High accuracy in converting between Devanagari, Tamil, Telugu, Kannada, and Malayalam. Nuance is critical (e.g., preserving distinctions between 'La', 'Lla', 'Zha').
2.  **Structured Data Extraction**: Ability to reliably output strictly formatted JSON from messy HTML/text.
3.  **Cost Efficiency**: The platform aims for a "System of Record" containing thousands of compositions. Per-document processing cost must be low.
4.  **Context Window**: Ability to ingest full web pages or long chapters without losing context.

## 3. Why Google Gemini?

### 3.1 Superior Indic Language Performance
Google has a long history of investment in Indic language models (through initiatives like Project Vani and Vaani). Gemini models exhibit:
*   **Vocabulary Richness**: Better handling of Sanskritized terms common in Carnatic music compared to models primarily optimized for English/European languages.
*   **Script Fidelity**: Lower hallucination rates when switching between scripts (e.g., avoiding mixing Tamil and Grantha characters incorrectly).

### 3.2 2.0 Flash: The Efficiency Breakthrough
For our use case (batch processing thousands of items), **Gemini 2.0 Flash** is a game-changer:
*   **Speed**: Extremely low latency allows for near-real-time feedback in the "Generate Variants" UI.
*   **Cost**: Significantly cheaper than GPT-4o or Claude 3.5 Sonnet, enabling us to run validations on every single edit without budget concerns.
*   **Quality**: Despite being a "light" model, its performance on extraction tasks and translation matches or exceeds larger legacy models.

### 3.3 Comparison with Alternatives

| Feature | **Google Gemini 2.0 Flash** | **OpenAI GPT-4o** | **Anthropic Claude 3.5 Sonnet** |
| :--- | :--- | :--- | :--- |
| **Indic Transliteration** | **Excellent**. High fidelity for Dravidian scripts. | **Good**. Occasional phonetic errors in complex clusters. | **Good**. Very strong reasoning but slower/more expensive. |
| **JSON Reliability** | **Native JSON Mode**. Very strict adherence. | **JSON Mode**. Reliable. | **Artifacts/XML**. Strong, but JSON mode slightly less "native" feel. |
| **Cost** | **Lowest**. Ideal for bulk jobs. | **High**. Cost-prohibitive for scraping thousands of pages. | **Medium/High**. |
| **Context Window** | **1M+ Tokens**. Can process entire books. | **128k Tokens**. Sufficient for pages, tight for books. | **200k Tokens**. |
| **Ecosystem** | **Vertex AI**. Deep integration with Google Cloud. | **Azure/Native**. Requires separate billing/infra. | **AWS/GCP**. Good availability. |

### 3.4 Integration Benefits
Since our infrastructure is likely grounded in Google Cloud (or easily compatible with it via Vertex AI):
*   **Unified Billing**: AI costs roll into standard cloud billing.
*   **Grounding**: Future potential to use "Google Search Grounding" to verify Raga facts against the live web.

## 4. Implementation Strategy

### 4.1 Model Selection by Task

| Task | Selected Model | Reason |
| :--- | :--- | :--- |
| **Transliteration** | **Gemini 2.0 Flash** | Speed and low cost are paramount for generating 5+ script variants per line. |
| **Web Scraping** | **Gemini 2.0 Flash** | Long context to ingest HTML; efficiency for high-volume batch jobs. |
| **Musicological Validation** | **Gemini 1.5 Pro** | Requires complex reasoning to check Raga scales (Arohanam/Avarohanam) against lyrics, which involves understanding musical grammar. |
| **Metadata Normalization** | **Gemini 2.0 Flash** | Simple fuzzy matching and extraction tasks. |

## 5. Conclusion

Google Gemini offers the "sweet spot" of **domain-relevant accuracy** (Indic languages) and **operational viability** (Flash model costs). It enables features that were previously too expensive or unreliable to automate, positioning Sangita Grantha to scale from a manual curated list to a comprehensive, AI-assisted digital archive.

---

## Conductor Tracking for Gemini-Related Work

This rationale informs multiple implementation tracks (transliteration, scraping, validation, search). To keep design and execution aligned:

- **Every new Gemini-backed feature or major enhancement MUST be tracked via Conductor**:
  - Register or update a `TRACK-XXX` row in `conductor/tracks.md`.
  - Create or update `conductor/tracks/TRACK-XXX-*.md` describing the specific Gemini usage, architecture, and rollout plan (see `TRACK-001-bulk-import-krithis.md` for structure).
- **When this document is used as justification for implementation**:
  - Reference the relevant `TRACK-XXX` from the implementation PRD / feature spec.
  - Ensure the TRACK file’s **Progress Log** is updated as the Gemini capability moves from evaluation → POC → production.

Conductor thus captures **how** and **where** Gemini is used operationally, while this document captures **why** it was chosen.