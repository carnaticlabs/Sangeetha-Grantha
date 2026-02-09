| Metadata | Value |
|:---|:---|
| **Status** | Archived |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-02-08 |
| **Author** | Sangeetha Grantha Team |

# Krithi Data Sourcing Strategy and Implementation Checklist

> [!WARNING]
> This document has been **archived**. It is superseded by:
> - [quality-strategy.md](../../01-requirements/krithi-data-sourcing/quality-strategy.md) — Primary strategy document
> - [implementation-checklist.md](../../01-requirements/krithi-data-sourcing/implementation-checklist.md) — Implementation checklist

---

## 1. Objective

Design and implement a long-term sourcing strategy that ingests Krithis and related metadata from high-integrity sources (HTML, PDF, and other document formats), while preserving musicological correctness, provenance, and editorial quality.

---

## 2. Context and Drivers

- Current bulk import is hyperlink + HTML-centric.
- Target sources include:
  - `https://guruguha.org/wp-content/uploads/2025/01/mdskt.pdf`
  - `https://guruguha.org/wp-content/uploads/2022/03/mdeng.pdf`
  - `https://swathithirunalfestival.org/swathi-thirunal/`
  - `https://www.shivkumar.org/music/`
  - `https://karnatik.com/composers.shtml`
- Existing tracks:
  - `conductor/tracks/TRACK-039-data-quality-audit-krithi-structure.md`
  - `conductor/tracks/TRACK-040-krithi-remediation-deduplication.md`
  - `conductor/tracks/TRACK-041-enhanced-sourcing-logic.md`

---

## 3. Current Baseline (As-Is)

### Strengths

- Mature orchestration model with batch/job/task lifecycle, retries, watchdogs, and event logs.
- Staging table (`imported_krithis`) already supports:
  - parsed payload
  - resolution candidates
  - duplicate candidates
  - quality metrics
- Deterministic section extraction exists and is already integrated with review flow.
- Entity resolution and dedup pipelines exist and are worker-driven.

### Gaps

- Manifest and scrape flows are primarily URL + HTML driven.
- No first-class PDF/document extraction stage in the worker pipeline.
- Structural voting and authority source prioritization are pending.
- Quality scoring is generic and does not yet include format confidence or source authority.
- Review UX does not fully expose source-level evidence and provenance.

---

## 4. Strategic Principles

1. **Authority first**: prefer curated, composer-specific authoritative sources.
2. **Structure before enrichment**: lock canonical section structure first, enrich metadata later.
3. **Provenance by default**: every asserted field should be traceable to source artifact and extractor run.
4. **Confidence-gated progression**: do not auto-approve low-confidence structural imports.
5. **Reproducible pipeline**: version all extraction and normalization logic for replayability.
6. **Fix-forward operations**: remediation is auditable, reversible, and measurable.

---

## 5. Target Architecture (To-Be)

### 5.1 Multi-Layer Pipeline

1. **Acquisition Layer**
   - Accepts URL, PDF URL, local document upload, and mixed manifests.
   - Produces source artifacts with checksum and MIME metadata.
2. **Format Extraction Layer**
   - HTML extractor.
   - PDF text extractor.
   - OCR extractor for scanned PDFs/images.
   - Optional DOC/DOCX/RTF extractor.
3. **Structure Canonicalization Layer**
   - Runs structural voting across multiple extractions/sources.
   - Produces canonical section template and confidence.
4. **Enrichment Layer**
   - Adds language variants, deity, temple, notes, and secondary metadata.
5. **Governance Layer**
   - Applies quality gates, dedup checks, review routing, and audit tracking.

### 5.2 Source Authority Policy

Initial authority map (field-sensitive, composer-sensitive):

- **Muthuswami Dikshitar**: Guruguha (primary)
- **Swathi Thirunal**: Swathi Thirunal Festival (primary)
- **General composer metadata / fallback indexing**: Karnatik, Shivkumar

Note: authority should be resolved by **field type** (structure vs metadata vs meaning), not a single global source rank.

---

## 6. Data and Schema Evolution Plan

### 6.1 Short-Term (No breaking changes)

Use `parsed_payload` and task `evidence_path` to store:

- Source artifact metadata
- Extractor output candidates
- Candidate section structures
- Chosen structure and vote rationale

### 6.2 Medium-Term (Recommended schema additions)

Add dedicated tables:

- `source_documents`
  - one row per acquired source artifact
- `extraction_runs`
  - extractor type/version, confidence, status, error details
- `structure_candidates`
  - candidate section sequences, source weights, vote scores
- `field_assertions`
  - per-field provenance (source, extractor, confidence)
- `enrichment_runs`
  - phase-wise enrichment status and outcomes

### 6.3 Compatibility Goals

- Keep existing `imported_krithis` and `import_batch` flows operational.
- Integrate new tables incrementally.
- Avoid rewiring canonical `krithis` write paths until confidence model is stable.

---

## 7. Multi-Phase Ingestion Strategy

### Phase A: Structural Grounding

- Ingest from authority source(s).
- Parse and normalize section markers.
- Run structural voting where multi-source evidence exists.
- Persist only structural template + minimal core metadata.

### Phase B: Core Metadata Consolidation

- Resolve composer/raga/tala with current resolver.
- Resolve duplicates.
- Apply confidence scoring for core fields.

### Phase C: Variant and Language Enrichment

- Add lyric variants in additional scripts/languages.
- Align variants to canonical section template.
- Flag drift (missing sections, merged sections, metadata pollution).

### Phase D: Contextual Enrichment

- Deity, temple, kshetra, geo metadata.
- Meaning/notes extraction with explicit provenance.

### Phase E: Continuous Remediation

- Scheduled audits for section drift and duplication regressions.
- Controlled remediation jobs with before/after evidence.

---

## 8. Quality Model and Review Gates

### 8.1 Scoring Dimensions

1. Structural confidence
2. Source authority score
3. Extraction confidence (including OCR confidence for scans)
4. Entity resolution confidence
5. Deduplication confidence
6. Validation and consistency score

### 8.2 Gate Policy

- **Auto-approve eligible** only if:
  - high structural confidence
  - high authority match
  - no high-confidence duplicate conflict
  - no unresolved critical fields
- Otherwise route to manual review with evidence.

### 8.3 Review UX Requirements

- Show per-field source lineage.
- Show all structure candidates and selected winner.
- Show artifact links (HTML/PDF pages or snapshots) and extraction logs.

---

## 9. TRACK Alignment

### TRACK-039 (Audit)

- Complete full structural audit by source/language/composer.
- Add confidence drift and metadata pollution quantification.

### TRACK-041 (Enhanced Sourcing Logic)

- Implement `ComposerSourcePriority` + field-level authority logic.
- Implement `StructuralVotingEngine`.
- Extend parser normalization for technical headers and script-specific markers.

### TRACK-040 (Remediation and Dedup)

- Build remediation jobs for:
  - section normalization
  - lyric metadata cleanup
  - duplicate variant merge
- Start with Dikshitar high-priority set, then expand.

---

## 10. Risks and Mitigations

1. **Scanned PDF quality variance**
   - Mitigation: OCR confidence thresholds + mandatory manual review below threshold.
2. **Source structure drift**
   - Mitigation: versioned extractor profiles, source-specific handlers, regression fixtures.
3. **False-positive dedup merges**
   - Mitigation: conservative merge policy + human sign-off for medium confidence.
4. **Model-driven extraction instability**
   - Mitigation: deterministic parser precedence where available + schema-constrained JSON output.
5. **Operational load increase**
   - Mitigation: staged rollout, capped concurrency, queue-level observability.

---

## 11. 90-Day Delivery Roadmap

### Sprint Window 1 (Days 1-20)

- Add source artifact abstraction.
- Add PDF extraction worker stage (MVP).
- Persist extraction evidence and confidence.

### Sprint Window 2 (Days 21-45)

- Implement structural voting engine.
- Implement authority map and source weighting.
- Add review UI evidence panel.

### Sprint Window 3 (Days 46-70)

- Add enrichment phases and gating.
- Upgrade quality scoring with structural/authority dimensions.

### Sprint Window 4 (Days 71-90)

- Execute remediation pilot (Dikshitar).
- Measure KPIs and adjust thresholds.
- Prepare production rollout guide and runbook updates.

---

## 12. Detailed Implementation Checklist

Use this as the execution checklist for engineering, data, and editorial teams.

### 12.1 Discovery and Design

- [ ] Finalize authoritative source matrix by composer and field.
- [ ] Define accepted file/document format matrix (HTML, PDF, DOC/DOCX, image).
- [ ] Define extraction confidence taxonomy (high/medium/low + numeric band).
- [ ] Define structural confidence rubric for voting.
- [ ] Define reviewer decision rubric for conflict cases.
- [ ] Define field-level provenance requirements for review and audit.
- [ ] Align track owners and milestones across TRACK-039, TRACK-040, TRACK-041.

### 12.2 Schema and Contracts

- [ ] Draft JSON contract for `SourceArtifact`, `ExtractionCandidate`, `StructureVoteResult`.
- [ ] Add short-term payload contract support in import services.
- [ ] Design migration for `source_documents`.
- [ ] Design migration for `extraction_runs`.
- [ ] Design migration for `structure_candidates`.
- [ ] Design migration for `field_assertions`.
- [ ] Design migration for `enrichment_runs`.
- [ ] Add indexes for artifact lookup by source domain/checksum.
- [ ] Add indexes for extraction run lookup by batch/status.
- [ ] Add indexes for structure candidate lookup by import and confidence.

### 12.3 Acquisition and Manifest Handling

- [ ] Extend manifest schema to support mixed source types (url/file/reference).
- [ ] Add manifest validation for MIME and format type.
- [ ] Add checksum generation during artifact ingestion.
- [ ] Add artifact dedupe by checksum + source key.
- [ ] Add artifact storage path conventions and retention policy.
- [ ] Add evidence lifecycle policy (retention + archival).

### 12.4 Extraction Layer

- [ ] Introduce extraction dispatcher by source format.
- [ ] Implement HTML extraction adapter (reuse existing logic).
- [ ] Implement PDF text extraction adapter for born-digital PDFs.
- [ ] Implement OCR extraction adapter for scanned PDFs/images.
- [ ] Implement optional DOC/DOCX extraction adapter.
- [ ] Normalize raw extraction outputs into a common candidate schema.
- [ ] Persist extraction run metadata and confidence.
- [ ] Persist extraction errors with retry classification.
- [ ] Add extractor version tags for reproducibility.

### 12.5 Structure Canonicalization and Voting

- [ ] Implement `ComposerSourcePriority` model.
- [ ] Implement field-aware authority weighting.
- [ ] Implement `StructuralVotingEngine` core algorithm.
- [ ] Add heuristics for technical markers (`madhyama kala`, `chittaswaram`, etc.).
- [ ] Add script-specific normalization for Tamil/Telugu/Kannada/Malayalam markers.
- [ ] Add ghost-section detection (line-volume vs section-count mismatch).
- [ ] Persist all structure candidates and winning rationale.
- [ ] Expose chosen structure confidence in import payload.

### 12.6 Entity Resolution and Dedup

- [ ] Extend resolver input to include authority source context.
- [ ] Tune name normalization for composer/raga/tala edge patterns.
- [ ] Include structure fingerprints in dedupe matching.
- [ ] Distinguish duplicate title vs duplicate composition-structure conflicts.
- [ ] Add manual override pathway for dedupe false positives.
- [ ] Persist dedupe rationale for review and audit.

### 12.7 Enrichment Pipeline

- [ ] Split enrichment into explicit stages (core, variants, contextual).
- [ ] Add stage transition rules and required confidence thresholds.
- [ ] Add variant-to-section alignment validation.
- [ ] Add metadata pollution filters for lyric section text.
- [ ] Add deity/temple enrichment with source confidence handling.
- [ ] Add fallback behavior when contextual enrichment is inconclusive.

### 12.8 Review and Admin UX

- [ ] Show source artifacts list per import.
- [ ] Show extraction runs and confidence.
- [ ] Show structure candidate comparison panel.
- [ ] Show voting winner and reasons.
- [ ] Show field-level provenance panel.
- [ ] Add filters for format type (HTML/PDF/OCR/document).
- [ ] Add filters for low-confidence structural imports.
- [ ] Add bulk actions by confidence tier with safeguards.
- [ ] Add clear manual override audit notes template.

### 12.9 Quality Scoring and Auto-Approval

- [ ] Extend scoring model with structural confidence.
- [ ] Extend scoring model with source authority score.
- [ ] Extend scoring model with OCR confidence for scanned docs.
- [ ] Define and configure phase-specific auto-approval thresholds.
- [ ] Add guardrails to block auto-approval when structural uncertainty exists.
- [ ] Add score explainability payload for reviewer visibility.

### 12.10 Observability and Operations

- [ ] Add per-format success/failure dashboards.
- [ ] Add queue depth and stage latency metrics.
- [ ] Add source-level error-rate monitoring.
- [ ] Add structured logs for extraction and voting decisions.
- [ ] Add alerting for sudden confidence drops by source.
- [ ] Add runbook updates for ingestion failures and retries.
- [ ] Add operational playbook for source parser breakage.

### 12.11 Testing and Validation

- [ ] Build fixture corpus for each target source and format.
- [ ] Add parser unit tests for section marker variants.
- [ ] Add integration tests for mixed-format batch processing.
- [ ] Add regression tests for known drift cases from TRACK-039.
- [ ] Add end-to-end tests for review flow with provenance display.
- [ ] Add non-regression tests for existing CSV + HTML pathway.
- [ ] Add remediation dry-run verification tests.

### 12.12 Remediation Execution

- [ ] Define pilot dataset (Dikshitar priority set).
- [ ] Run dry-run structural normalization and capture diffs.
- [ ] Review false positives with editorial team.
- [ ] Execute controlled merge/remediation in staged batches.
- [ ] Re-audit structure consistency after remediation.
- [ ] Expand remediation to Tyagaraja and other composer sets.

### 12.13 Governance and Change Control

- [ ] Define approval workflow for source authority changes.
- [ ] Version and publish source authority matrix.
- [ ] Version and publish scoring threshold configuration.
- [ ] Add periodic audit cadence (weekly ops, monthly quality review).
- [ ] Add archival strategy for superseded extraction runs.
- [ ] Publish decision log for major ingestion rule changes.

---

## 13. Success Criteria

- Structural consistency across variants improves to target threshold.
- Reduction in manual correction effort per import batch.
- High-confidence auto-approval precision improves without quality regressions.
- Clear provenance coverage for all critical metadata fields.
- Remediation backlog decreases with no increase in duplicate regressions.

---

## 14. Immediate Next Actions (Execution Starter)

1. Freeze and approve authority matrix and confidence rubric.
2. Implement PDF extraction MVP and common candidate schema.
3. Implement structural voting MVP and review UI evidence panel.
4. Run Dikshitar pilot batch with end-to-end quality measurement.
5. Finalize production rollout and runbook updates based on pilot outcomes.
