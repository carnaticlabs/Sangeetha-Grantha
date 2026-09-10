| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.2.0 |
| **Last Updated** | 2026-09-10 |
| **Author** | Sangeetha Grantha Team |
| **Document Type** | Current guide |

# Runtime diagnostics and monitoring

---

Start with process and request evidence, then trace the affected workflow. Current repository observability includes Compose logs, basic health routes, authenticated Prometheus metrics, audit events, extraction/task state, and catalogue instrumentation. It does not provision a complete hosted dashboard/alerting system.

## Local inspection

```bash
docker compose ps
docker compose logs --tail=100 backend extraction
curl --fail http://localhost:8080/health
```

`/health` and `/v1/health` return `OK`. They are basic liveness responses, not database/queue readiness checks. `/health/ready` and `/health/live` are not mounted.

`GET /metrics` is inside the authenticated admin role group. Configure a metrics collector with the intended credentials and avoid exposing its token in logs. The worker Docker health check only verifies module imports; inspect queue progression and errors to establish operational readiness.

Sources: [HealthRoutes](../../modules/backend/api/src/main/kotlin/com/sangita/grantha/backend/api/routes/HealthRoutes.kt), [MetricsRoutes](../../modules/backend/api/src/main/kotlin/com/sangita/grantha/backend/api/routes/MetricsRoutes.kt), [Routing](../../modules/backend/api/src/main/kotlin/com/sangita/grantha/backend/api/plugins/Routing.kt).

## Trace an issue

| Area | Correlate |
|:---|:---|
| HTTP failure | Request path/status, caller role, backend exception, request/interaction identifiers when present |
| Import backlog | Batch, job/task attempts, extraction ID/status, worker logs, result processor logs |
| Incorrect lyrics | Source artifact, payload, variant/section IDs, accepted revision and current reader |
| Raga mismatch | Raw name, candidates, alias/key/mela context, curator queue decision |
| Search failure | Active profile, query embedder configuration, indexed hashes, provider response |
| Mobile journey | Client request and state, native host/runtime, network address, selected variant/bookmark |

## Operational measurements

Measure API errors/latency, failed/retrying extraction counts, queue age, review backlog, and search index coverage in the intended environment. Establish thresholds from observed workload; historical targets are not deployed alerts.

Sourcing quality coverage/audit-summary endpoints include placeholders. Use the actual [quality checks](../07-quality/README.md) when reporting a scan. Keep corpus counts and extraction success rates dated and scoped.

[Incident response](./runbooks/incident-response.md) · [Configuration](./config.md) · [Database runbook](./runbooks/database-runbook.md)

---

[Section index](./README.md) · [Documentation home](./../README.md) · [Feature status](./../01-requirements/features/README.md)
