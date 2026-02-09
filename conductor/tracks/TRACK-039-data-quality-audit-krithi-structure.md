| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.1.0 |
| **Last Updated** | 2026-02-08 |
| **Author** | Sangita Grantha Architect |

# TRACK-039: Data Quality Audit – Krithi Structural Consistency

## 1. Objective
Identify and document structural inconsistencies in the Krithi database, specifically where language variants of the same composition have mismatching sections or missing data.

## 2. Context
- **Hypothesis**: The structure (Pallavi, Anupallavi, Charanam) and lyric content are invariant across scripts in Carnatic musicology.
- **Problem**: Legacy imports or parsing errors (pre-TRACK-034/036) might have resulted in variants with different section counts or missing section mapping.

## 3. Implementation Plan
- [x] Research and validate hypothesis (Completed: 2026-02-07).
- [x] Write SQL audit query: section count mismatch across lyric variants per Krithi.
- [x] Write SQL audit query: section label sequence mismatch across variants.
- [x] Write SQL audit query: orphaned lyric blobs (lyric sections without parent section mapping).
- [ ] Run all three audits against production database.
- [ ] Analyze failure patterns (Source/Language).
- [ ] Document in `application_documentation/07-quality/results/krithi-structural-audit-2026-02.md`.
- [ ] Create quality baseline metrics snapshot.

## 4. Artifacts

| Artifact | Path | Description |
|:---|:---|:---|
| Section count mismatch audit | `database/audits/audit_section_count_mismatch.sql` | Detects Krithis where variant section counts differ from canonical sections |
| Label sequence mismatch audit | `database/audits/audit_label_sequence_mismatch.sql` | Detects section label ordering differences across variants |
| Orphaned lyric blobs audit | `database/audits/audit_orphaned_lyric_blobs.sql` | Finds lyric content without proper section mappings |
| Interim audit results | `application_documentation/07-quality/results/krithi-structural-audit-2026-02.md` | Case studies and sourcing hierarchy proposal |
| Remediation plan | `application_documentation/07-quality/remediation-implementation-plan-2026-02.md` | Remediation checklist driven by audit findings |

## 5. Progress Log
- **2026-02-07**: Initialized track. Validated hypothesis via Thyagaraja Vaibhavam and Guruguha.org. Defined audit queries. Completed initial DB mismatch scan. Expanded scope to analyze additional authoritative PDF and web sources.
- **2026-02-08**: Implemented all three SQL audit queries as part of Phase 0 of the Krithi Data Sourcing & Quality Strategy:
  - `database/audits/audit_section_count_mismatch.sql` — Detects Krithis where lyric variant section counts differ from canonical `krithi_sections` count. Includes per-composer summary and Krithis with variants but no canonical sections.
  - `database/audits/audit_label_sequence_mismatch.sql` — Detects ordering differences across variants (e.g., P-C vs P-A-C). Builds ordered section type strings per variant and flags disagreements. Includes variant coverage gap analysis.
  - `database/audits/audit_orphaned_lyric_blobs.sql` — Finds monolithic lyric blobs never decomposed into sections, cross-Krithi section references, and completely empty Krithi shells.
  - All queries produce both detail and summary (by-composer) views for analysis.
  - Added parser normalization for Tamil subscripts and Madhyama Kala shorthand to reduce structural drift noise ahead of final SQL audits.
  - Documented interim findings in `application_documentation/07-quality/results/krithi-structural-audit-2026-02.md` with case studies (Section Count Drift, Metadata Pollution, Redundant Variants) and proposed sourcing hierarchy.
  - These audit queries serve as the automated quality gate for Phase 2 (Structural Validation) in the [Krithi Data Sourcing & Quality Strategy](../../application_documentation/01-requirements/krithi-data-sourcing/quality-strategy.md#63-track-039-integration-structural-auditing).
  - Updated documentation: schema.md, backend-system-design.md, 07-quality README, and root README with links to audit artifacts.