| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.1.0 |
| **Last Updated** | 2026-07-18 |
| **Author** | Sangeetha Grantha Team |

# Quality Audit Results

This directory contains the results of automated and manual quality audits run against the Sangita Grantha database.

## Contents

| Path | What it is | Status |
|:---|:---|:---|
| [krithi-structural-audit-2026-02.md](./krithi-structural-audit-2026-02.md) | Structural consistency audit of the krithi corpus | Complete |
| [section-issues-cleanup/](./section-issues-cleanup/) | Curator "Section Issues" queue remediation — findings log, the open work list, and its machine-readable companion | **Open** — ~112 variants remain; see [ADR-015](../../02-architecture/decisions/ADR-015-govindan-extraction-adapter.md) Part B |

---

## Contents

- [krithi-structural-audit-2026-02.md](./krithi-structural-audit-2026-02.md) - Structural consistency audit results (Feb 2026)
  - Section count drift analysis across lyric variants
  - Case studies: metadata pollution, redundant variants
  - Proposed sourcing hierarchy (Composer → Authority Source mapping)
  - Remediation roadmap

## Related

- **Audit Queries**: `database/audits/` — SQL queries used to generate these results
  - `audit_section_count_mismatch.sql`
  - `audit_label_sequence_mismatch.sql`
  - `audit_orphaned_lyric_blobs.sql`
- **Track**: [TRACK-039](../../../conductor/tracks/TRACK-039-data-quality-audit-krithi-structure.md) — Structural consistency auditing
- **Remediation Plan**: [remediation-implementation-plan-2026-02.md](../remediation-implementation-plan-2026-02.md)
- **Quality Strategy**: [Krithi Data Sourcing & Quality Strategy §6](../../01-requirements/krithi-data-sourcing/quality-strategy.md#6-data-quality-framework)
