| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.3.0 |
| **Last Updated** | 2026-09-10 |
| **Author** | Sangeetha Grantha Team |
| **Document Type** | Current guide |

# Builds and deployment readiness

---

The repository provides a local Compose stack, a backend container build, an admin static build, and native mobile hosts. It does not establish a live staging/production URL or a completed cloud deployment pipeline. Cloud Run/Cloud SQL designs elsewhere in the docs are planning material.

## Available artifacts

| Artifact | Build path | Deployment consideration |
|:---|:---|:---|
| Backend | `./gradlew :modules:backend:api:build`; [Dockerfile](../../modules/backend/api/Dockerfile) | Database access, explicit secrets, HTTP ingress, health verification |
| Admin web | Module `bun run typecheck` and `bun run build` | Serve `dist/` with SPA fallback and the correct API routing/base |
| Extraction worker | [Dockerfile](../../tools/krithi-extract-enrich-worker/Dockerfile) | Persistent queue access, provider configuration, artifact cache |
| Android | `make mobile-android` | Debug build; release signing/store distribution are additional work |
| iOS | `make mobile-ios` | Simulator build; signing/device acceptance/store distribution are additional work |
| Database | Flyway over [database/migrations](../../database/migrations) | PostgreSQL with required extensions and rehearsed migration history |

Use [Current Versions](../00-meta/current-versions.md) for pins and [CI](../../.github/workflows/ci.yml) for actual validation jobs. A successful frontend Vite build does not replace the separate TypeScript check.

## Local production-style backend build

The Compose `prod` profile builds `backend-prod` and the extraction worker, runs migrations, and uses the same local database definition. It does not include a production frontend service, TLS termination, cloud resources, or production secret management.

```bash
docker compose --profile prod build backend-prod extraction
```

Starting the profile would run services and migrations against the configured database. Treat it as a local packaging path; it is not a hardened deployment environment.

## Before deploying to an environment

Choose and record the actual target infrastructure, hostnames, credentials, database topology, artifact locations, and rollback owner. Then verify:

1. **Application artifact:** backend, worker, and frontend builds correspond to the intended revision.
2. **Database:** required PostgreSQL/pgvector features are available; Flyway validates a restored representative database as well as a clean one.
3. **Identity:** explicit token/signing configuration replaces development defaults; provisioned users and stored roles are correct.
4. **Frontend transport:** `/v1` is reverse-proxied appropriately or `VITE_API_BASE_URL` is set at build time; browser bundles contain no service credentials.
5. **Worker:** queue access, local/remote artifact paths, retries, and optional provider variables are configured in the worker environment.
6. **Public visibility:** anonymous catalogue reads return only intended published records; V1/V2 behavior and `no-store` responses are preserved.
7. **Search:** intended embedding profile and index coverage match the deployed query embedder.
8. **Evidence:** critical import/edit/read journeys, operational diagnostics, and restore rehearsal are recorded for that environment.

The [north-star readiness plan](../north-star-production-readiness-implementation-plan.md) remains a planning reference. It must not be read as proof that every gate is closed.

## Migration and rollback boundaries

`make migrate` targets the committed local Compose migration service. A shell `DATABASE_URL` assignment does not redirect it to Cloud SQL or another remote service. Configure an explicit Flyway deployment job for the intended database and review its target before running it.

Flyway Community does not supply undo migrations. Application rollback is only safe when the earlier binary remains compatible with the database. Rehearse data restore or forward repair independently; versioned canon is content history, not a substitute for a database backup.

Use the [database runbook](./runbooks/database-runbook.md) and [migration history guidance](../04-database/migrations.md). Local `db-reset`/`clean` commands are destructive development tools, not production rollback procedures.

## Mobile release gate

Shared tests and Android/iOS build jobs provide compilation and logic evidence. TRACK-140 still requires native runtime journeys and manual TalkBack/VoiceOver assessment before release acceptance. See [Rasika evidence](../10-implementations/track-140-rasika-discovery.md).

[Configuration](./config.md) · [Monitoring](./monitoring.md) · [Quality gates](../07-quality/README.md) · [Cloud architecture proposal](../02-architecture/google-cloud-scaling-strategy.md)

---

[Section index](./README.md) · [Documentation home](./../README.md) · [Feature status](./../01-requirements/features/README.md)
