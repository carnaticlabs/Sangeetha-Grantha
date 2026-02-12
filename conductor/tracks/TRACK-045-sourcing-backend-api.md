| Metadata | Value |
|:---|:---|
| **Status** | Completed |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-02-09 |
| **Author** | Sangeetha Grantha Team |

# TRACK-045: Sourcing Backend API Layer

## 1. Objective
Implement all Ktor REST API endpoints required to power the Sourcing & Extraction Monitoring UI screens. This includes Source Registry CRUD, Extraction Queue operations, Source Evidence queries, Structural Voting endpoints, and Quality Dashboard aggregation queries.

## 2. Context
- **Reference**: [Sourcing UI/UX Plan](../../application_documentation/01-requirements/krithi-data-sourcing/ui-ux-plan.md) — §5 (API Requirements)
- **Parent Track**: [TRACK-041](./TRACK-041-enhanced-sourcing-logic.md)
- **Database Schema**: Migrations 23–27 (source authority, evidence, voting, extraction queue) are already applied.
- **Phase**: Phase 1–2 (Sprint 1–4) — API endpoints built incrementally ahead of each UI phase.

## 3. Implementation Plan

### 3.1 Source Registry API (§5.1)
- [ ] Create `SourceRegistryRoutes.kt` under `/v1/admin/sourcing/sources`.
- [ ] `GET /sources` — List sources with filters (tier, format, active, search). Pagination support.
- [ ] `GET /sources/:id` — Get source detail with contribution stats (Krithi count, field breakdown, avg confidence, extraction success rate).
- [ ] `POST /sources` — Register new source. Validate unique name and URL. Audit log.
- [ ] `PUT /sources/:id` — Update source. Audit log.
- [ ] `DELETE /sources/:id` — Soft deactivate source. Audit log.
- [ ] Create `SourceRegistryRepository.kt` with Exposed queries.
- [ ] Create `SourceRegistryService.kt` with business logic.
- [ ] Create DTOs: `SourceListResponse`, `SourceDetailResponse`, `CreateSourceRequest`, `UpdateSourceRequest`.

### 3.2 Extraction Queue API (§5.2)
- [ ] Create `ExtractionQueueRoutes.kt` under `/v1/admin/sourcing/extractions`.
- [ ] `GET /extractions` — List extraction tasks with filters (status, format, source, date range, batch). Pagination.
- [ ] `GET /extractions/:id` — Get task detail with results (parsed CanonicalExtractionDto array) and error details.
- [ ] `POST /extractions` — Submit new extraction request (creates `extraction_queue` row with PENDING status). Audit log.
- [ ] `POST /extractions/:id/retry` — Retry failed extraction (reset status, increment attempts). Audit log.
- [ ] `POST /extractions/:id/cancel` — Cancel pending/processing extraction. Audit log.
- [ ] `POST /extractions/retry-all-failed` — Bulk retry. Audit log.
- [ ] `GET /extractions/stats` — Queue summary: counts by status, throughput (extractions/hour over 24h).
- [ ] Create `ExtractionQueueRepository.kt` (extends existing extraction_queue table access).
- [ ] Create `ExtractionQueueService.kt`.
- [ ] Create DTOs: `ExtractionListResponse`, `ExtractionDetailResponse`, `CreateExtractionRequest`, `ExtractionStatsResponse`.

### 3.3 Source Evidence API (§5.3)
- [ ] Create `SourceEvidenceRoutes.kt` under `/v1/admin/sourcing/evidence`.
- [ ] `GET /evidence` — List Krithis with evidence summary (filters: min source count, tier, field, extraction method). Pagination.
- [ ] `GET /evidence/krithi/:id` — Get all source evidence for a specific Krithi (ordered by tier, with field values and confidence).
- [ ] `GET /evidence/compare/:id` — Get field-level comparison matrix across sources for a Krithi (used by FieldComparisonTable).
- [ ] Create `SourceEvidenceRepository.kt` with joins across `krithi_source_evidence`, `import_sources`, `krithis`.
- [ ] Create `SourceEvidenceService.kt`.
- [ ] Create DTOs: `EvidenceListResponse`, `KrithiEvidenceResponse`, `FieldComparisonResponse`.

### 3.4 Structural Voting API (§5.4)
- [ ] Create `StructuralVotingRoutes.kt` under `/v1/admin/sourcing/voting`.
- [ ] `GET /voting` — List voting decisions (filters: consensus type, confidence, date range, pending review, has dissents). Pagination.
- [ ] `GET /voting/:id` — Get voting detail with participants, proposed structures, dissent details, and rationale.
- [ ] `POST /voting/:id/override` — Submit manual structure override (creates new vote log entry with `MANUAL` consensus, records reviewer_id). Audit log.
- [ ] `GET /voting/stats` — Voting summary: counts by consensus type, confidence distribution.
- [ ] Create `StructuralVotingRepository.kt` with queries on `structural_vote_log`.
- [ ] Create `StructuralVotingService.kt`.
- [ ] Create DTOs: `VotingListResponse`, `VotingDetailResponse`, `ManualOverrideRequest`, `VotingStatsResponse`.

### 3.5 Quality Dashboard API (§5.5)
- [ ] Create `QualityDashboardRoutes.kt` under `/v1/admin/sourcing/quality`.
- [ ] `GET /quality/summary` — KPI summary: total Krithis, multi-source count/%, consensus count/%, avg quality score, enrichment coverage %.
- [ ] `GET /quality/distribution` — Quality score histogram (buckets: 0–0.2, 0.2–0.4, 0.4–0.6, 0.6–0.8, 0.8–1.0).
- [ ] `GET /quality/coverage` — Tier coverage (Krithis per tier) and Composer × Field matrix.
- [ ] `GET /quality/gaps` — Data gap analysis: missing lyrics, single-language, no deity/temple, low-confidence voting, no notation.
- [ ] `GET /quality/audit` — Latest audit query results (section count mismatch, label sequence mismatch, orphaned lyric blobs).
- [ ] `POST /quality/audit/run` — Trigger fresh audit run against the database. Audit log.
- [ ] Create `QualityDashboardRepository.kt` with aggregation queries.
- [ ] Create `QualityDashboardService.kt`.
- [ ] Create DTOs: `QualitySummaryResponse`, `QualityDistributionResponse`, `CoverageResponse`, `GapAnalysisResponse`, `AuditResultResponse`.

### 3.6 Shared Concerns
- [ ] Add route registration in the main Ktor `Application` module under `/v1/admin/sourcing`.
- [ ] Apply JWT authentication with role-based guards (Admin, IngestionOps roles for write operations).
- [ ] Ensure all mutation endpoints write to `audit_log` table.
- [ ] Add request validation using Ktor's request validation plugin.
- [ ] Add OpenAPI annotations for all new endpoints.

## 4. Artifacts

| Artifact | Path | Description |
|:---|:---|:---|
| Source Registry routes | `modules/backend/api/.../routes/sourcing/SourceRegistryRoutes.kt` | CRUD endpoints |
| Extraction Queue routes | `modules/backend/api/.../routes/sourcing/ExtractionQueueRoutes.kt` | Queue operations |
| Source Evidence routes | `modules/backend/api/.../routes/sourcing/SourceEvidenceRoutes.kt` | Provenance queries |
| Structural Voting routes | `modules/backend/api/.../routes/sourcing/StructuralVotingRoutes.kt` | Voting endpoints |
| Quality Dashboard routes | `modules/backend/api/.../routes/sourcing/QualityDashboardRoutes.kt` | Aggregation queries |
| Repositories | `modules/backend/api/.../repositories/sourcing/` | Data access layer |
| Services | `modules/backend/api/.../services/sourcing/` | Business logic |
| DTOs | `modules/shared/domain/.../sourcing/` | Shared request/response models |

## 5. Acceptance Criteria
- All 25+ endpoints return correct data from seeded database.
- All mutation endpoints create audit_log entries.
- JWT authentication enforced on all routes.
- Pagination works correctly with offset/limit parameters.
- Filter parameters correctly narrow result sets.
- Error responses use consistent format with appropriate HTTP status codes.

## 6. Dependencies
- TRACK-041 (database migrations 23–27, seed data).
- Existing `ExtractionQueueRepository` and `ExtractionQueueService` from TRACK-041 (to be completed).

## 7. Progress Log
- **2026-02-09**: Track created based on Sourcing UI/UX Plan §5 (API Requirements).
- **2026-02-09**: Implementation complete:
  - Added `ExtractionStatus` enum to `DbEnums.kt`.
  - Created `SourcingTables.kt` with Exposed table definitions for `ImportSourcesEnhancedTable`, `ExtractionQueueTable`, `KrithiSourceEvidenceTable`, `StructuralVoteLogTable`.
  - Created `SourcingDtos.kt` shared domain DTOs for all sourcing entities (serializable with Kotlinx).
  - Created 5 DAL repositories: `SourceRegistryRepository`, `ExtractionQueueRepository`, `SourceEvidenceRepository`, `StructuralVotingRepository`, `QualityDashboardRepository`.
  - Created `SourcingService.kt` with business logic and audit logging for mutations.
  - Created `SourcingRoutes.kt` with full REST endpoints under `/v1/admin/sourcing`.
  - Updated `SangitaDal` interface + impl with new repositories.
  - Registered `SourcingService` in Koin DI module.
  - Registered sourcing routes in Ktor routing within authenticated block.
