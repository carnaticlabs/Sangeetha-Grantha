| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.3.0 |
| **Last Updated** | 2026-09-10 |
| **Author** | Sangeetha Grantha Team |
| **Document Type** | Current guide |

# Troubleshooting local development

---

Identify which component failed before changing data or configuration. The commands below inspect local behavior; schema resets and volume deletion are not routine troubleshooting steps.

## Start with service state

```bash
docker compose ps
docker compose logs --tail=100 backend extraction
make migrate-status
curl --fail http://localhost:8080/health
```

`/health` only confirms the basic API response. The worker container health check verifies imports, not queue progress. See [monitoring](../08-operations/monitoring.md).

## Common symptoms

| Symptom | Likely boundary | Next step |
|:---|:---|:---|
| mise reports a missing environment file | `config/postgres-local.env` is referenced by `.mise.toml` | Follow [fresh setup](./getting-started.md); preserve existing values |
| Port 5432/8080/5001 already in use | Duplicate host/Compose service | Inspect the running process/service; choose one execution mode |
| Backend never starts | Migration service or Gradle failure | Read the relevant service logs; fix the actual failure |
| Fresh catalogue is empty | Reference seeds do not include the corpus | Load intended sample/imported content and verify publication state |
| Login says user not found | Account not provisioned in this database | Run configured `make bootstrap-admin`; do not reset the database |
| Login/token is rejected | Token, identity, expiry, issuer or role mismatch | Follow [authentication](../00-meta/quick-reference-auth.md) |
| Console talks to the wrong API | Browser base/proxy configuration | Check `VITE_API_BASE_URL`, `API_PROXY_TARGET`, Vite environment location |
| Worker remains idle | Wrong database/queue or no pending work | Inspect worker settings and extraction/task state |
| Local PDF cannot be opened | Host path differs from container path | Use a worker-visible path such as the configured `/app/pdfs` mount |
| Extraction DONE but composition incomplete | Result processing, mapping, variant or section persistence | Trace the [ingestion stages](../01-requirements/features/bulk-import/02-implementation/technical-implementation-guide.md) |
| Semantic search returns no results | No active profile or no indexed matches | Inspect index/profile and [search behavior](../03-api/search.md) |
| Hybrid search reports unavailable | Provider error or incompatible profile | Inspect query embedder and active model/dimensions |
| Mobile cannot reach backend | Emulator/simulator/device addressing | Use host-specific debug configuration from the [mobile guide](../05-frontend/mobile/README.md) |

## Migration and restore problems

Flyway is the only engine. Match history entries by complete identity and checksum. Earlier corpus-fix V58–V62 files were retired; current V58–V60 contain different legitimate schema work. Do not copy an old history-row deletion or fixed baseline command.

The PostgreSQL 18 volume is `pgdata18` mounted at `/var/lib/postgresql`. Older volumes need a planned/rehearsed dump-and-restore migration. Preserve the old data until the restored database and API have been verified. See [database runbook](../08-operations/runbooks/database-runbook.md).

## Test failures

Backend integration tests and some worker tests require Docker/Testcontainers. They provision their own database by default. An external test-database override must identify a disposable database; reset-capable tests must never point at a retained corpus.

Use the checked-in package scripts and toolchain. `make test` includes API/DAL database-backed tests; `bun run build` does not replace `bun run typecheck`. Shared mobile JVM tests do not replace native runtime acceptance.

## Stale behavior after edits

Restart the Compose stack after backend/shared Kotlin or worker Python changes using `make dev-down`, then `make dev`. Check the command actually rebuilt/started the intended service. Do not delete volumes or caches before evidence points to them as the cause.

[Configuration](../08-operations/config.md) · [Quality guide](../07-quality/README.md) · [Onboarding](./README.md)

---

[Section index](./README.md) · [Documentation home](./../README.md) · [Feature status](./../01-requirements/features/README.md)
