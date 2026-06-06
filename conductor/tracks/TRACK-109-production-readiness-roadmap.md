| Metadata | Value |
|:---|:---|
| **Status** | Not Started |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-06-06 |
| **Author** | Principal Data & AI Engineering review (for Seshadri) |
| **Type** | **Epic** — umbrella track; each workstream spawns its own `TRACK-XXX` when scheduled |
| **Priority** | P2 (strategic) — runs in parallel; gates first production launch |

# TRACK-109: Production Readiness Roadmap

## Goal

Define the path from "works on `make dev`" to "**runs in production, safely, observably, and affordably**." Today the application is a well-factored dev-stage system (Docker Compose, local Makefile workflows, manual imports). This epic enumerates the workstreams that must close before — and shortly after — a first production deployment, sequenced so the highest-risk gaps (secrets, backups, deploy) are addressed first. This is the long-term vision the other new tracks (105–108) feed into.

## Context — current state vs production bar

What already exists and is genuinely good (build on it, don't rebuild):
- **Audit logging** — `AUDIT_LOG` table with a hard rule that all mutations write to it (`CLAUDE.md`).
- **Provenance** — source-evidence / extraction-tier tracking (TRACK-041/062) — a strong base for content licensing and reconciliation.
- **AuthN/Z** — JWT with role-based claims; login implemented (TRACK-027).
- **Connection pooling** — HikariCP; **metrics** — Micrometer; **structured logging** — Logback + Logstash encoder.
- **Migrations** — Python `db-migrate` (deterministic, versioned).

What's missing for production (the gaps this epic closes):

| Workstream | Gap today | Production bar |
|:---|:---|:---|
| **W1 — Secrets management** | API keys / JWT secret / DB creds via plaintext `.env` (`SG_GEMINI_API_KEY` etc.) | Managed secrets (GCP Secret Manager / Vault); no secrets in repo or image; rotation policy |
| **W2 — CI/CD** | Local Makefile + manual `./gradlew`/`bun`; no pipeline | CI builds all 3 layers, runs tests + migrations, scans deps; CD with promotion gates + rollback |
| **W3 — Deployment & runtime** | Docker Compose (dev only) | Prod orchestration (Cloud Run / GKE per GCP affinity); health/readiness probes; horizontal scale for API + worker |
| **W4 — Observability** | Micrometer wired but no backend; logs local | Prometheus + Grafana dashboards; alerting; distributed tracing; SLOs (latency, extraction success rate, queue depth) |
| **W5 — Data durability & DR** | Local Postgres volume | Automated backups + PITR; tested restore; retention policy; RPO/RTO targets |
| **W6 — Security hardening** | Basic | Rate limiting, input validation, dependency/CVE scanning, secret scanning, least-privilege DB roles, TLS everywhere, security review (see `security-review` skill) |
| **W7 — Extraction pipeline reliability** | `extraction_queue` with retries | Dead-letter queue, idempotency guarantees, poison-message handling, backpressure, worker autoscale |
| **W8 — Data quality gates** | Manual review + quality scoring (TRACK-011) | Automated DQ checks (Great Expectations / Deequ-style) in CI/import; block bad payloads before persist |
| **W9 — Cost governance (FinOps)** | None explicit | Gemini spend tracking (Batch from TRACK-107), per-import cost visibility, budget alerts |
| **W10 — Content licensing & compliance** | Provenance captured, policy implicit | Explicit source licensing/attribution policy for scraped krithis; takedown process; PII review (low — public texts) |
| **W11 — Mobile release pipeline** | KMP app builds locally | Signing, store provisioning (iOS/Android), staged rollout, crash reporting |

## Approach

Treat each workstream as an independently schedulable track. **Do not** attempt all of W1–W11 before launch; sequence by risk:

- **Pre-launch blockers (must close first):** W1 (secrets), W2 (CI/CD), W3 (deploy), W5 (backups/DR), W6 (security baseline).
- **Launch-adjacent (close within first weeks):** W4 (observability/SLOs), W7 (pipeline reliability), W8 (DQ gates).
- **Continuous / maturing:** W9 (FinOps), W10 (licensing), W11 (mobile) — driven by when those surfaces go live.

Each workstream, when scheduled, gets a `TRACK-XXX` with its own HLD, NFRs, failure modes, and rollback plan (use the `engineering:system-design`, `engineering:deploy-checklist`, and `security-review` skills).

## Implementation Plan (epic-level milestones)

### Milestone A — Deployability (W1, W2, W3)
- [ ] Externalise all secrets to a managed store; strip `.env` secrets from any committed/imaged artifact.
- [ ] CI: build + test backend/frontend/worker; run migrations against an ephemeral DB; dependency + secret scanning.
- [ ] CD: deploy to a staging environment on GCP; health probes; one-command rollback.

### Milestone B — Survivability (W5, W6)
- [ ] Automated Postgres backups + PITR; **rehearse a restore** and record RPO/RTO.
- [ ] Security baseline: TLS, rate limiting, input validation, least-privilege DB roles; run a `security-review` pass on the API surface.

### Milestone C — Operability (W4, W7, W8)
- [ ] Stand up Prometheus/Grafana on the existing Micrometer metrics; define SLOs (API p95, extraction success rate, queue depth) + alerts.
- [ ] Harden `extraction_queue`: DLQ, idempotency, poison handling, worker autoscale.
- [ ] Wire automated data-quality gates into the import path; bad payloads blocked pre-persist.

### Milestone D — Sustainability (W9, W10, W11)
- [ ] Gemini cost dashboard + budget alerts (consumes TRACK-107 Batch metrics).
- [ ] Publish a source-licensing/attribution + takedown policy; map to existing provenance data.
- [ ] Mobile release pipeline (signing, stores, crash reporting, staged rollout).

## Acceptance Criteria (launch gate)
- No secret in any repo, image, or log; rotation documented.
- A commit flows through CI to staging automatically, with tests + migrations + scans green, and can be rolled back in one step.
- A database restore has been **performed**, not just configured, with documented RPO/RTO.
- Security baseline checklist passed; SLOs + alerts live for API and extraction worker.
- A documented, rehearsed runbook exists for deploy, rollback, and the top incident classes.

## Risks
- **Big-bang temptation** — trying to productionise everything at once. Mitigation: ship Milestone A to staging early; iterate.
- **Secrets already in history** — if any key was ever committed, rotation + history scrub is required, not just `.gitignore` (TRACK-105 Group D guards the entry point).
- **Cost surprise at scale** — without W9, Gemini/embedding spend is invisible; pair FinOps with the AI tracks.

## Dependencies
- Builds on: TRACK-105/106 (clean, truthful repo), TRACK-107 (supported AI deps + Batch cost signal), TRACK-108 (search endpoint inherits these NFRs).
- Spawns: per-workstream tracks (W1–W11) scheduled as capacity allows.

## Progress Log
- 2026-06-06: Epic created. Inventoried existing production-grade foundations (audit log, provenance, JWT, Micrometer, HikariCP, db-migrate) vs 11 gap workstreams. Sequenced into Milestones A–D with a launch gate.

Ref: application_documentation/sangeetha-grantha-uplift-tasks.md
