| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-02-12 |
| **Author** | Sangeetha Grantha Team |

# Krithi Data Sourcing & Quality — Document Index

This directory contains the strategy, implementation plan, and supporting analysis for expanding Sangeetha Grantha's data sourcing capabilities beyond HTML into multi-format (PDF, DOCX, OCR) ingestion with a comprehensive quality framework.

## Documents

| Document | Purpose |
|:---|:---|
| [quality-strategy.md](./quality-strategy.md) | **Primary Strategy Document** — Comprehensive strategy covering multi-format ingestion architecture, source authority hierarchy, multi-phase enrichment, data quality framework, containerised deployment, and risk analysis. |
| [implementation-checklist.md](./implementation-checklist.md) | **Implementation Checklist** — Detailed, actionable breakdown of every task across 6 phases with dependencies, acceptance criteria, and sprint allocation. |
| [ui-ux-plan.md](./ui-ux-plan.md) | **UI/UX Plan** — User interface and experience requirements for sourcing & extraction monitoring: source registry, extraction queue monitor, provenance browser, structural voting viewer, and quality dashboard. |
| [pdf-diacritic-extraction-analysis.md](./pdf-diacritic-extraction-analysis.md) | **PDF Diacritic Extraction Analysis** — Forensic analysis of garbled Utopia font encoding in guruguha.org PDFs causing Raga/Tala/Section extraction failures. Includes root cause, evidence, solution design, and implementation plan. |

> [!NOTE]
> The original combined strategy document has been [archived](../../archive/krithi-data-sourcing/krithi-data-sourcing-strategy-and-implementation-checklist.md) — it is superseded by the two documents above.

## Related Tracks

| Track | Focus | Status |
|:---|:---|:---|
| [TRACK-039](../../../conductor/tracks/TRACK-039-data-quality-audit-krithi-structure.md) | Structural consistency auditing — SQL queries for section count mismatch, label sequence mismatch, and orphaned lyric blobs | Active |
| [TRACK-040](../../../conductor/tracks/TRACK-040-krithi-remediation-deduplication.md) | Data remediation and deduplication — metadata cleanup, structural normalisation, dedup merging | Active |
| [TRACK-041](../../../conductor/tracks/TRACK-041-enhanced-sourcing-logic.md) | Enhanced sourcing with structural voting — multi-source authority, canonical extraction schema, PDF extraction service | Active |

## Implementation Artifacts

| Artifact | Location | Description |
|:---|:---|:---|
| SQL Audit Queries | `database/audits/` | Three TRACK-039 audit queries for structural consistency checking |
| Database Migrations 23–27 | `database/migrations/23__*` through `27__*` | Source authority, source evidence, vote log, format tracking, extraction queue |
| Source Registry Seed | `database/seed_data/04_import_sources_authority.sql` | Import sources with tier rankings for all target sources |
| Canonical Extraction DTO (Kotlin) | `modules/shared/domain/.../import/CanonicalExtractionDto.kt` | Kotlin data class for the universal extraction format |
| Canonical Extraction Schema (Python) | `tools/pdf-extractor/src/schema.py` | Pydantic model matching the Kotlin DTO |
| PDF Extraction Service | `tools/pdf-extractor/` | Python service for PDF/DOCX/OCR extraction |
| Docker Compose | `compose.yaml` | Extended with `pdf-extractor` service |

## Phase Overview

| Phase | Name | Key Deliverable | Status |
|:---|:---|:---|:---|
| 0 | Foundation & Quality Baseline | Audit queries, canonical schema, DB migrations, source registry | **In Progress** |
| 1 | PDF Ingestion — Skeleton Extraction | Python extraction service + Kotlin integration | **In Progress** |
| 2 | Structural Validation & Voting | StructuralVotingEngine, source evidence tracking | Planned |
| 3 | Lyric Enrichment | Multi-script sourcing, transliteration, variant alignment | Planned |
| 4 | Metadata Enrichment | Deity, temple, tags, TRACK-040 remediation | Planned |
| 5 | Notation Ingestion | Swara notation from shivkumar.org | Planned |
| 6 | Continuous Curation | Automated audits, quality dashboard, re-harvesting | Planned |
