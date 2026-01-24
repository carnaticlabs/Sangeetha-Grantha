| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-01-24 |
| **Author** | Sangeetha Grantha Team |

# ADR-006: Integration of Google Gemini for Content Ingestion and Validation

| Metadata | Value |
|:---|:---|
| **Status** | Accepted |
| **Version** | 1.0 |
| **Last Updated** | 2026-01-20 |
| **Author** | Engineering Team |

## Context

The Sangita Grantha platform faces a significant challenge in scaling its content library. Currently, the ingestion of "Kritis" (musical compositions) is a manual, labor-intensive process requiring:
1.  **Manual Data Entry**: Copying text from physical books or legacy websites.
2.  **Manual Transliteration**: typing out lyrics in multiple scripts (Devanagari, Tamil, Telugu, Kannada, Malayalam, Latin) to support a diverse user base.
3.  **Manual Validation**: Expert review to ensure the Raga (melody) and Tala (rhythm) annotations align with the lyrics.

This manual workflow limits the growth of the repository and introduces human error. To transform the platform into an authoritative "system of record", we need to automate these processes.

## Decision

We will integrate **Google Gemini** models (specifically **Gemini 2.0 Flash** and **Gemini 1.5 Pro**) as the core AI engine for the "Intelligent Content Ingestion and Musicological Validation Suite".

The integration will cover:
1.  **Multi-Script Transliteration**: Automating conversion between Indian scripts and Latin.
2.  **Intelligent Web Scraping**: Extracting structured JSON data from unstructured HTML sources.
3.  **Metadata Normalization**: Fuzzy matching and canonicalizing entities (Composers, Ragas).
4.  **Musicological Validation**: Verifying structural integrity (Pallavi/Charanam detection) and Raga alignment.

## Rationale

The decision to choose Google Gemini is driven by several key factors:
1.  **Superior Indic Language Support**: Gemini models have demonstrated high proficiency in Indian languages, which is critical for accurate transliteration of Carnatic lyrics.
2.  **Structured Output (JSON Mode)**: Native support for enforcing JSON schemas ensures reliably parsable output for our backend services.
3.  **Cost-Performance Ratio**: Gemini 2.0 Flash offers a very low cost per token, making high-volume transliteration and scraping economically viable (~$3-5 per 1,000 kritis).
4.  **Long Context Window**: The large context window allows for processing entire web pages or potentially full documents in a single pass without complex chunking strategies.

## Consequences

### Positive
*   **Scalability**: Ingestion throughput is expected to increase by an order of magnitude.
*   **Accessibility**: Automatic multi-script generation makes content immediately available to users reading different scripts.
*   **Data Quality**: Automated validation acts as a first line of defense, reducing the burden on expert reviewers.

### Negative
*   **External Dependency**: The platform will rely on Google Cloud's availability and API pricing/stability.
*   **Latency**: AI processing adds non-trivial latency to the import pipeline (though "Flash" models mitigate this).
*   **Verification Overhead**: While manual entry is reduced, a new workflow for "reviewing AI suggestions" must be built.

## Compliance
*   **Audit Logging**: All AI-generated changes must be logged in the `AUDIT_LOG` table with the model version used.
*   **Human-in-the-Loop**: AI outputs (especially imported data) must be stored in a staging area (`imported_krithis`) and require human approval before becoming canonical.

## References
*  - [Feature Requirements](../../01-requirements/features/intelligent-content-ingestion.md)
*   [Gemini Selection Rationale](../../09-ai/gemini-selection-rationale.md)
