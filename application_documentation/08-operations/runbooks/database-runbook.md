| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.2.0 |
| **Last Updated** | 2026-09-10 |
| **Author** | Sangeetha Grantha Team |
| **Document Type** | Current guide |

# Database operations runbook

---

The local database is Compose service `db`, database `sangita_grantha`, exposed on port 5432. PostgreSQL/pgvector stores both canonical content and extraction/search state. Flyway owns migrations.

## Inspect and migrate

```bash
make db
make migrate-status
docker compose exec -T db psql -U postgres -d sangita_grantha -c 'SELECT current_database(), version();'
```

Apply pending schema/reference changes with `make migrate`. See [migration guidance](../../04-database/migrations.md) before working with a restored or long-lived database. A passing server health endpoint does not prove database readiness.

## Backup a local corpus

From the repository root, choose a new output filename:

```bash
docker compose exec -T db pg_dump -U postgres -d sangita_grantha -Fc > /tmp/sangita-grantha-backup.dump
```

Verify the command succeeds and the dump is readable. Preserve the application revision and migration history alongside the backup. Restoring should first be rehearsed into an isolated database with the required extensions; do not restore over the only copy of a corpus.

## PostgreSQL 18 volumes

Compose mounts volume `pgdata18` at `/var/lib/postgresql`. Older pre-18 volumes may still exist separately. Do not attach an incompatible data directory to the new server or delete the old volume before a validated dump/restore migration.

The [PostgreSQL upgrade report](../postgresql-18-upgrade-implementation.md) preserves the earlier migration context. Use the actual current Compose definition and the source database version to plan a restore.

## History mismatches

Compare full migration descriptions and checksums with the current SQL files. Earlier corpus-fix migrations numbered V58–V62 were retired, while current V58–V60 now implement search and unclassified-form schema. Do not delete history entries solely by those numbers or apply an old fixed baseline command.

Generated raga-key expressions in older dumps can encounter search-path problems during restore. Diagnose against the restored schema and rehearse the repair before changing a retained database. See [migration history](../../04-database/migrations.md#5-rollback--history-tracking).

## Content corrections and checks

Use parser/extraction/reingestion/curator paths for composition repairs. Verify `krithi_ragas`, lyric variants and sections, revision attribution, source evidence, audit, and public reader output. [Post-import verification](../../07-quality/qa/test-plan.md) provides the checklist.

```bash
make raga-lakshana-checks
```

Testcontainers tests provision disposable databases; they are the default for schema/repository verification. Any explicit external test database must be disposable because test cleanup mutates data.

## Destructive development commands

`make db-reset` drops/recreates the local database, then reapplies schema/reference seeds. `make clean` removes Compose volumes. Neither restores the imported corpus, index, or admin account. Use only when discarding that local data is intended.

[Configuration](../config.md) · [Schema](../../04-database/schema.md) · [Versioned canon](../../04-database/versioned-canon.md)

---

[Section index](./README.md) · [Documentation home](./../../README.md) · [Feature status](./../../01-requirements/features/README.md)
