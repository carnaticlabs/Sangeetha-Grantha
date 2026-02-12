| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.2.0 |
| **Last Updated** | 2026-02-09 |
| **Author** | Sangita Grantha Architect |

# TRACK-040: Krithi Data Remediation & Deduplication

## 1. Objective
Systematically clean up the `krithis` database by merging redundant lyric variants and normalizing inconsistent section structures identified in the Feb 2026 audit.

## 2. Context
- **Source**: [Data Quality Audit Report](../application_documentation/07-quality/results/krithi-structural-audit-2026-02.md)
- **Key Problem**: "Section Count Drift" across language variants and metadata pollution in lyric text.
- **Strategy**: [Krithi Data Sourcing & Quality Strategy](../application_documentation/01-requirements/krithi-data-sourcing/quality-strategy.md) — Section 6.4

## 3. Implementation Plan
- [x] Develop `RemediationService` orchestrator with variant-level deduplication (Levenshtein >90% similarity).
- [x] Create `MetadataCleanupService` to strip boilerplate from `krithi_lyric_sections` (URLs, source attributions, copyright, page numbers, meaning markers, BOM, excessive whitespace).
- [x] Implement `StructuralNormalizationService` to analyze variant alignment to canonical template (gap detection, extra section detection).
- [x] Create REST API routes: preview (`GET /v1/admin/quality/remediation/preview`), execute (`POST /v1/admin/quality/remediation/execute`), plus individual preview endpoints for cleanup, dedup, and normalization.
- [x] Wire all services into DI (`AppModule.kt`) and routing (`Routing.kt`).
- [ ] Run remediation on `Dikshitar` compositions (High Priority).
- [ ] Run remediation on `Tyagaraja` compositions.
- [ ] Re-run `QualityScoringService` on all remediated Krithis.

## 4. Dependencies

| Dependency | Status | Notes |
|:---|:---|:---|
| TRACK-039 audit execution | Queries written, execution pending | Audit results drive remediation priorities |
| TRACK-041 structural voting | Engine created, integration pending | Voting determines canonical structure for alignment |
| `krithi_source_evidence` table | Migration 24 written | Per-source tracking enables remediation comparison |
| `CanonicalExtractionDto` | Defined (Kotlin + Python) | Universal format for multi-source comparison |

## 5. Artifacts

| Artifact | Path | Description |
|:---|:---|:---|
| Remediation plan | `application_documentation/07-quality/remediation-implementation-plan-2026-02.md` | Detailed checklist with cleanup + hardening tasks |
| Source evidence table | `database/migrations/24__krithi_source_evidence.sql` | Per-source tracking for remediation comparison |
| Sourcing strategy report | `application_documentation/07-quality/reports/sourcing-strategy-2026.md` | Progress tracking for remediation prerequisites |
| MetadataCleanupService | `modules/backend/api/.../services/MetadataCleanupService.kt` | Boilerplate detection and removal from lyric sections |
| StructuralNormalizationService | `modules/backend/api/.../services/StructuralNormalizationService.kt` | Variant-to-canonical alignment analysis |
| RemediationService | `modules/backend/api/.../services/RemediationService.kt` | Orchestrator: cleanup + normalization + deduplication |
| Remediation API routes | `modules/backend/api/.../routes/RemediationRoutes.kt` | REST API for preview and execution |

## 6. Progress Log
- **2026-02-07**: Track created following TRACK-039 audit conclusion.
- **2026-02-08**: Foundation laid as part of Phase 0 of the Krithi Data Sourcing & Quality Strategy:
  - Database schema extensions completed: `krithi_source_evidence` table (migration 24) enables per-source tracking needed for remediation comparison.
  - TRACK-039 SQL audit queries implemented — these will produce the baseline data that drives remediation priorities.
  - Canonical extraction schema defined (Kotlin `CanonicalExtractionDto` + Python `CanonicalExtraction`) — enables the multi-source comparison that TRACK-040 remediation relies on.
  - Updated remediation checklist (`application_documentation/07-quality/remediation-implementation-plan-2026-02.md`) to reflect completed sourcing-logic hardening; cleanup services remain pending.
  - Remediation implementation deferred to Phase 4 (per strategy) — depends on TRACK-039 audit execution and TRACK-041 structural voting.
  - Updated all relevant documentation: schema.md (new tables §10.4), remediation plan, sourcing strategy report, and README files across application_documentation/.
- **2026-02-09**: Implemented all three remediation services and API layer:
  - **MetadataCleanupService**: Detects and strips 8 boilerplate pattern types (URLs, source attributions, copyright, page numbers, meaning markers, BOM, excessive whitespace, boilerplate headers). Supports preview mode and composer-filtered execution.
  - **StructuralNormalizationService**: Analyzes variant alignment against canonical section structure. Reports gaps (missing sections) and extras per variant, with composer-level filtering.
  - **RemediationService**: Orchestrates all three remediation phases — cleanup, normalization analysis, and variant deduplication. Deduplication uses Levenshtein distance (>90% threshold) on normalized lyric text to find near-identical variants in the same language, keeping the longer/more complete variant.
  - **API routes**: Preview (`GET /v1/admin/quality/remediation/preview`), execute (`POST /v1/admin/quality/remediation/execute`), plus individual endpoints for cleanup-preview, dedup-preview, and normalization analysis.
  - All services wired into DI and routing. Audit logging for all mutation operations.
  - Remaining: Run remediation against production data for Dikshitar and Tyagaraja compositions.