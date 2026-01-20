# Feature Request: Intelligent Content Ingestion & Musicological Validation Suite

| Metadata | Value |
|:---|:---|
| **Status** | Draft |
| **Version** | 0.1.0 |
| **Last Updated** | 2026-01-20 |
| **Author** | System |

---


## 1. Executive Summary

This feature request outlines the integration of **Google Gemini AI** and **Google Cloud Services** into the Sangita Grantha platform to automate the ingestion, normalization, and validation of Carnatic music compositions. The primary objective is to transform the platform into an authoritative "system of record" by leveraging AI for multi-script transliteration, intelligent web scraping, and automated metadata extraction.

---

## 2. Targeted User Personas

* **Editor (Admin):** Needs to ingest hundreds of kritis from legacy sources without manual copy-pasting.
* **Reviewer (Admin):** Requires automated validation tools to ensure musicological correctness (raga/tala alignment) before publishing.
* **Rasika / Learner (Public User):** Benefits from semantic search and multi-script support for lyrics and notation.

---

## 3. Functional Requirements

### 3.1 Automatic Transliteration Service (High Priority)

* **Requirement:** Implement a `TransliterationService` using Gemini 2.0 Flash to convert lyrics and notation between Indian scripts (Devanagari, Tamil, Telugu, Kannada, Malayalam) and Latin.
* **UI Integration:** Add a "Generate Variants" button in `KrithiEditor.tsx` to trigger multi-script generation.
* **Constraint:** Maintain exact pronunciation markers and musical notation alignment during script conversion.

### 3.2 Intelligent Web Scraping & Extraction (High Priority)

* **Requirement:** Build a `WebScrapingService` to fetch and parse kritis from external sources like *shivkumar.org*.
* **AI Logic:** Use Gemini to extract structured JSON (title, composer, raga, tala, deity) from varied and potentially malformed HTML structures.
* **Workflow:** Scraped data must be stored in the `imported_krithis` table for manual review before canonicalization.

### 3.3 Metadata Normalization & Auto-Mapping (Medium Priority)

* **Requirement:** Use Gemini to extract metadata from unstructured text and suggest mappings to canonical entities (e.g., mapping "Tyagayya" to the "Tyagaraja" ID).
* **Logic:** Implement fuzzy matching combined with Gemini-based matching to resolve composer and raga variations.

### 3.4 Content Validation & Quality Assurance (Medium Priority)

* **Requirement:** Implement a `ValidationService` to perform AI-powered quality checks.
* **Validators:**
* **Raga Alignment:** Check if lyrics/notation consistent with raga scale.
* **Section Detection:** Automatically identify *Pallavi*, *Anupallavi*, and *Charanam* boundaries.
* **Tala Verification:** Ensure notation row counts match the assigned Tala's cycles.



### 3.5 Semantic Search Enhancement (Future Enhancement)

* **Requirement:** Integrate Google Cloud Vertex AI embeddings (`text-embedding-004`) to enable thematic search (e.g., "Ganesha kritis in Madurai").

---

## 4. Technical Architecture & Setup

### 4.1 Cloud Infrastructure

* **API Platform:** Google Gemini API (Flash 2.0 and Pro 1.5).
* **Environment Variables:**
* `SG_GEMINI_API_KEY`: Required for service authentication.
* `SG_GEMINI_MODEL`: Defaulting to `gemini-2.0-flash-exp`.



### 4.2 Backend Implementation Patterns (Vibe Coding Compatible)

* **Database:** Use `DatabaseFactory.dbQuery { }` for all AI-triggered mutations.
* **Audit Logging:** Every AI-generated variant or auto-mapped field must be recorded in the `AUDIT_LOG` table.
* **Serialization:** Use `kotlinx.serialization` for all AI-generated DTOs.

---

## 5. Implementation Roadmap

* **Phase 1 (Weeks 1-4) [COMPLETED]:** Core Transliteration Service and Admin UI integration.
* **Phase 2 (Weeks 5-8) [COMPLETED]:** Web Scraping infrastructure and Shivkumar.org handler.
* **Phase 3 (Weeks 9-12):** Metadata extraction and auto-mapping logic.
* **Phase 4 (Weeks 13-16):** Quality validation suite and section detection.

---

## 6. Success Metrics

* **Transliteration Accuracy:** >95% for major Indian languages.
* **Data Extraction Accuracy:** >85% correctly mapped fields from scraped sources.
* **Operational Efficiency:** >80% reduction in manual data entry time per kriti.
* **Cost Efficiency:** Maintain processing costs at approximately ~$3-5 per 1,000 kritis.