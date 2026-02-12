---
name: ingestion-pipeline-guardian
description: Guidelines for managing the Sangita Grantha multi-source ingestion pipeline. Use when adding new sources, fixing extraction heuristics, or modifying the Kotlin/Python data flow to ensure architectural consistency, process synchronization, and high data quality.
---

# Ingestion Pipeline Guardian

This skill ensures the Sangita Grantha ingestion pipeline remains robust, "truth-aware," and free from stale logic.

## 1. Architectural Mandates: "Centralized Truth"

To prevent logic divergence across the Kotlin backend and Python extraction service:

- **Centralize Heuristics**: Domain-specific heuristics (regex for section detection, name normalization rules) should be centralized. If a rule is updated in Kotlin, it MUST be mirrored in the Python `structure_parser.py` or vice-versa.
- **Contract Parity**: Ensure the `CanonicalExtraction` schema in Python exactly matches the `CanonicalExtractionDto` in Kotlin.
- **Transliteration Awareness**: Normalization must handle the specific transliteration schemes of Indian Classical Music (IAST, Harvard-Kyoto, Velthuis).

## 2. Process Mandates: "In-Sync Development"

To avoid the "Shadow Container" problem where old code persists in Docker:

- **Volume Mounts**: During development, verify that `compose.yaml` mounts the `src/` directory as a volume (`./tools/pdf-extractor/src:/app/src`).
- **Rebuild vs. Restart**: Never trust `sangita-cli extraction restart` to pick up code changes if volumes aren't mounted. Use `docker compose --profile extraction build pdf-extractor` to be certain.
- **Version Verification**: Always include a version string or a unique log marker when patching extractor logic to confirm the fix is active in the running container.

## 3. Data & Ingestion Mandates

- **Matching vs. Refreshing**: Be aware that `ExtractionResultProcessor` matches extractions to *existing* records. If you are fixing structural logic (e.g., section splitting), you MUST delete the targeted test records or perform a `db reset` before re-running the extraction.
- **Idempotency**: All ingestion paths (Bulk CSV and PDF Extraction) must share the same `findDuplicateCandidates` logic and apply consistent idempotency guards.
- **Clean Slate Verification**: Verify logic fixes against raw database queries (e.g., `SELECT COUNT(*) FROM krithi_sections`) rather than UI reports which may use cached or stale data.

## 4. Workflow Pattern: "Vertical Slices"

When implementing new pipeline features, follow the **Vertical Slice** pattern:

1.  **Extract**: Manually extract raw text from a sample source page.
2.  **Normalize**: Write the normalization logic against that specific raw text fixture.
3.  **Flow**: Get data flowing from Source -> Python -> Database for ONE composition.
4.  **Validate**: Verify the database state with SQL before building any UI components.
5.  **Commit**: Commit the working slice before expanding scope.

## References

- [Remediation Implementation Plan](../../application_documentation/07-quality/remediation-implementation-plan-2026-02.md)
- [Multi-Source Import Retrospective](../../application_documentation/11-retrospective/multi-source-import-retrospective-2026-02.md)
- [Technical Retrospective (Remediation)](../../application_documentation/11-retrospective/remediation-technical-retrospective-2026-02-12.md)
