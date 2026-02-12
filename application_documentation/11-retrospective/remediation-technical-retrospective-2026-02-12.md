| Metadata | Value |
|:---|:---|
| **Status** | Final |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-02-12 |
| **Author** | Sangita Grantha Architect |

# Technical Retrospective: Multi-Source Import Remediation

## 1. Executive Summary
This retrospective analyzes the specific technical and process hurdles encountered during the final remediation of the Multi-Source Import pipeline (Feb 12, 2026). While the pipeline is now functional, several "leaky abstractions" in the architecture and development environment caused significant delays.

## 2. Key Technical Findings

### 2.1. Architectural Divergence: The Heuristic Split
The most persistent bug (`MADHYAMAKALA` splitting) stemmed from logic duplication between the Kotlin backend and the Python extraction service.
*   **Issue**: We maintained separate regex-based parsers (`KrithiStructureParser.kt` and `structure_parser.py`).
*   **Impact**: Improvements in one were not automatically reflected in the other, leading to "stale" behavior when processing PDFs vs. Web sources.
*   **Learning**: Complex domain heuristics should be centralized. A shared configuration or a single service should handle all raw text structure detection.

### 2.2. Process Failure: The "Shadow" Container
Development productivity was severely hindered by a lack of volume synchronization in the Docker environment.
*   **Issue**: The `Dockerfile` used `COPY src/`, meaning code changes required a full `docker build` to take effect.
*   **Impact**: Several "fix iterations" appeared to fail simply because the container was still running old code. 
*   **Learning**: Development `compose.yaml` files must use volume mounts (`./src:/app/src`) to ensure local edits are live immediately.

### 2.3. Ingestion Logic: Matching vs. Refreshing
The pipeline favored matching to existing records, which masked logic improvements.
*   **Issue**: `ExtractionResultProcessor` matched extractions to existing Krithis but only appended `source_evidence`, skipping section/lyric updates.
*   **Impact**: Even after the extractor was fixed, the database still displayed "dirty" data from previous failed runs.
*   **Learning**: During development and QA, the pipeline needs a "Force Refresh" mode, or we must prioritize a `db reset` to verify end-to-end correctness.

## 3. Prompting & LLM Interaction Lessons

### 3.1. Symptom vs. System Handover
*   **Observation**: Prompting often focused on the symptom (e.g., "Docker process not running") rather than the desired state (e.g., "Composition X should have 5 sections").
*   **Improvement**: Prompts should define a "Gold Standard" outcome. Instead of "Check if it's clean," use "Verify that title X is clean and has section count Y."

### 3.2. Verification Gates
*   **Observation**: The LLM often reported "Production Ready" based on compilation rather than data flow verification.
*   **Improvement**: Explicitly ask the agent to show the raw results of a database query or a log trace *after* a fix is applied to prove the logic change is active.

## 4. Operational Recommendations
1.  **Centralize Heuristics**: Move structure detection regexes into a shared YAML/JSON manifest.
2.  **Harden Tools**: Add `sangita-cli extraction build` to streamline the rebuild/restart cycle.
3.  **Mandatory Voluming**: Update `compose.yaml` to mount `tools/pdf-extractor/src` into the container.
4.  **Data Cleanliness**: Always delete targeted test data records before verifying an extraction fix to avoid matching stale entities.

---
*Retrospective conducted by Sangita Grantha Architect.*
