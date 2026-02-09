| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.1.0 |
| **Last Updated** | 2026-02-08 |
| **Author** | Sangeetha Grantha Team |

# 04 Database


---


## Contents

- [schema.md](./schema.md) - Authoritative PostgreSQL schema overview (includes new tables from migrations 23–27)
- [migrations.md](./migrations.md) - Migration conventions and history
- [audit-log.md](./audit-log.md) - Audit log design and usage
- [schema-validation.md](./schema-validation.md) - Schema validation rules

## Recent Schema Extensions (Feb 2026)

Five new migrations added as part of the [Krithi Data Sourcing & Quality Strategy](../01-requirements/krithi-data-sourcing/quality-strategy.md):

| Migration | File | Purpose |
|:---|:---|:---|
| 23 | `23__source_authority_enhancement.sql` | Source tier, supported formats, composer affinity on `import_sources` |
| 24 | `24__krithi_source_evidence.sql` | Per-source provenance tracking table |
| 25 | `25__structural_vote_log.sql` | Cross-source structural voting audit trail |
| 26 | `26__import_task_format_tracking.sql` | Source format and page range on `import_task_run` |
| 27 | `27__extraction_queue.sql` | Database-backed extraction queue (Kotlin ↔ Python) |

See [schema.md §10](./schema.md#10-import-pipeline) and [schema.md §14](./schema.md#14-recent-migrations-feb-2026) for details.