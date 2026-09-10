| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-09-10 |
| **Author** | Sangeetha Grantha Team |
| **Document Type** | Evidence record |

# TRACK-139: Retire Corpus Data-Fix Migrations

---

> [!NOTE]
> Historical evidence: results, counts, commands, and observations below belong to the original work described here. The editorial update date is not a new test or corpus verification. For present behavior, use [current feature map](./../01-requirements/features/README.md).

Working log: [TRACK-139](../../conductor/tracks/TRACK-139-retire-corpus-data-fix-migrations.md). Policy: [ADR-012](../02-architecture/decisions/ADR-012-unified-extraction-architecture.md), [ADR-013](../02-architecture/decisions/ADR-013-db-migration-with-flyway.md), [ADR-014](../02-architecture/decisions/ADR-014-versioned-canon.md) (corpus corrections are import/curation, not `V__` SQL). Ops: [migrations.md](../04-database/migrations.md).

## What landed

- **Parser:** hyphen-wrap two-line pallavi (`Alakalallalaadaga`); Dashavatara raga names on `RAGA_SEGMENT` (`mAdhavO mAM pAtu`).
- **Ingest:** `ImportService.reingestMappedKrithi` writes `is_ragamalika` and ordered `krithi_ragas`. Do not gate payload raga names through `NameNormalizationService.normalizeRaga` — that helper strips honorific `sri` and would drop raga Sri.
- **Reference data:** `R__seed_06` aliases `nATa`/`gauLa`/`kEdAra`/`saurAshTra` → keepers (ITRANS fold does not reach them).
- **Retired:** `V58`–`V62` deleted. Long-lived DBs: Flyway Community `repair` does not drop missing history rows; delete those `flyway_schema_history` versions so latest available is V57. Next schema file is **V58**.
- **Kept:** `V38`, `V45`–`V47` (evaluated, not parser-superseded as a batch).
- **Guardrail:** `.claude/hooks/protect-migrations.py` + `make agent-evals` deny new corpus DML in `V__` unless `-- corpus-data-fix: allow`. Grandfather cutoff is V57.

## Recovery dump

Local only (gitignored): `storage/backups/sangita_grantha_20260906_post_track139.dump` — live V57 plus the five reingested krithis. Restore still needs the `raga_match_key` `search_path` workaround in [migrations.md §5](../04-database/migrations.md#5-rollback--history-tracking).

---

[Section index](./README.md) · [Documentation home](./../README.md) · [Feature status](./../01-requirements/features/README.md)
