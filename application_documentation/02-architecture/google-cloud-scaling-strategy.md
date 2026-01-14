# Google Cloud Scaling Strategy (Cost-Effective, Global)

> **Status**: Proposal | **Version**: 1.0 | **Date**: 2026-01-14  
> **Scope**: Google Cloud deployment architecture for scaling Sangeetha Grantha to millions of global users  
> **Related**: [Scaling Evaluation](./scaling-evaluation.md), [Tech Stack](./tech-stack.md), [Backend Architecture](./backend-system-design.md)

## 1. Goals & Constraints

### Goals
- Support **millions of global read users** with low latency (public catalog + search + detail views).
- Support **editorial/admin workflows** with strong integrity (writes, audit logs, RBAC).
- Maintain **cost efficiency** at low/medium traffic while enabling smooth scale-up.
- Improve reliability with clear SLOs and observability.

### Constraints / Current Assumptions
- Backend is **Ktor + PostgreSQL** with stateless HTTP services.
- Public endpoints are **read-heavy**; admin endpoints are **write-heavy but low volume**.
- Database migrations are managed via `tools/sangita-cli` (Rust). (No Flyway/Liquibase.)
- AI integrations (transliteration/scraping) exist and should not block request threads at scale.

---

## 2. Recommended GCP Reference Architecture

### 2.1 High-Level Diagram (Phase 1–2 Target)

```
Users (Global)
   |
   |  HTTPS
   v
Cloud Load Balancing (Global External HTTP(S))
   |\
   | \__ Cloud CDN (static + cacheable public API)
   |
   v
API Gateway (optional) / Cloud Run (direct)  ---- Cloud Armor (WAF + rate limits)
   |
   v
Cloud Run (Ktor containers, multi-instance autoscaling)
   |
   +--> Memorystore (Redis)  (read caches, rate-limit counters, optional sessions)
   |
   +--> Cloud SQL for PostgreSQL (primary, HA)
          +--> Read replica(s) (same region)   [Phase 2]
          +--> Cross-region replica (DR)       [Phase 3]
   |
   +--> Pub/Sub / Cloud Tasks (async jobs)     [Phase 2–3]
          +--> Cloud Run worker(s)
                 +--> Vertex AI (Gemini) for AI tasks

Admin Web (React)
   |
   v
Cloud Storage (static hosting) -> Cloud CDN -> (optional) IAP/Identity-Aware controls
```

### 2.2 Why this is cost-effective
- **Cloud Run** scales to zero for low traffic, and scales out automatically for spikes.
- **Cloud CDN** reduces origin load and egress from compute/database, improving latency globally.
- **Memorystore (Redis)** targets the largest cost driver (database reads) early.
- **Cloud SQL** provides managed Postgres with HA and read replicas when needed, without operating your own cluster.

---

## 3. Service-by-Service Recommendations (What to use, and why)

### 3.1 Compute: Cloud Run (default)
Use **Cloud Run** for the Ktor backend (containerized).

- **Fit**: Stateless HTTP API, bursty traffic, autoscaling.
- **Key settings**:
  - **Concurrency**: tune per instance (start 40–80) to balance CPU vs latency.
  - **Min instances**: 0 for cost savings; consider 1–2 for warm start on critical APIs.
  - **CPU allocation**: consider “CPU always allocated” only for latency-sensitive endpoints; otherwise request-based CPU for cost.
- **Networking**:
  - Prefer **private IP** Cloud SQL + **Serverless VPC Access** to avoid public exposure.
- **Deployments**:
  - Use Cloud Run revisions + gradual traffic rollout.

**When to consider GKE**:
- Only when you require complex service mesh, custom networking, sidecars at scale (e.g., heavy PgBouncer topology), or long-running workers with specialized tuning.
- If you go Kubernetes, prefer **GKE Autopilot** to reduce ops burden.

### 3.2 Database: Cloud SQL for PostgreSQL (HA + replicas)
Use **Cloud SQL Postgres** for primary OLTP storage.

- **Phase 1**: single region + **HA** (regional) + automated backups + PITR.
- **Phase 2**: add **read replicas** (same region) for public read endpoints.
- **Phase 3**: add **cross-region replica** for DR (and potential geo-routing in later steps).

**Connection management (critical for Cloud Run)**:
- Cloud Run scales horizontally; uncontrolled DB connections can overwhelm Cloud SQL.
- Recommended actions:
  - Keep **HikariCP max pool size modest per instance** (e.g., 5–15) and scale at the service layer.
  - Add **PgBouncer** if/when connection pressure is high:
    - Option A (simple): run PgBouncer on a small managed VM (Compute Engine) in the same VPC/subnet.
    - Option B (K8s): PgBouncer as a Deployment in GKE (if you already adopt GKE).

**When to consider AlloyDB / Spanner**:
- **AlloyDB** if Postgres compatibility is desired with higher throughput and improved read scaling (cost is higher; evaluate with real load).
- **Spanner** only if you need global writes and ultra-high availability with strict SLAs; it requires significant schema/transaction model adaptation.

### 3.3 Caching: Memorystore (Redis)
Use **Memorystore for Redis** as the first scaling lever.

- **Cache targets (highest ROI)**:
  - Reference data lists (composers, ragas, talas, etc.) with long TTL.
  - Krithi detail read models with short/medium TTL + explicit invalidation on publish/update.
  - Search results (short TTL), especially for common queries and filters.
- **Distributed rate limiting**: store counters/token buckets in Redis if rate limiting is done at the app layer.

### 3.4 Edge + Static: Cloud Storage + Cloud CDN
Host the React admin web build in **Cloud Storage** and serve via **Cloud CDN**.

- **Benefits**: low cost, global performance, and no servers to manage.
- **Hardening**:
  - Use separate buckets for environments (staging/prod).
  - Use cache invalidation on deploy, or hashed asset filenames (preferred).

### 3.5 API front door: Global Load Balancer (+ optional API Gateway)
Two good patterns:

- **Simpler** (often enough): Global HTTPS Load Balancer → Cloud Run (serverless NEG) with Cloud Armor.
- **Stronger governance**: Load Balancer → **API Gateway** → Cloud Run.
  - Use API Gateway for:
    - Quotas/keys for external consumers
    - Centralized auth enforcement
    - Request validation (where practical)

For admin APIs, consider keeping them on a **separate hostname** (e.g., `admin-api.*`) so you can apply stricter policies.

### 3.6 DDoS/WAF/rate limiting: Cloud Armor
Use **Cloud Armor** at the edge for:
- IP-based rate limiting (especially on search endpoints).
- Bot protection/WAF rules.
- Geo-based rules when needed.

This prevents expensive origin and DB work for abusive traffic.

### 3.7 Async processing: Pub/Sub + Cloud Tasks (+ Cloud Scheduler)
Move AI and heavy workflows off the request path.

- **Pub/Sub**: event stream for “publish/update”, “import created”, “index update requested”.
- **Cloud Tasks**: reliable, rate-controlled task execution (great for “do X within N minutes”, retries with backoff).
- **Cloud Scheduler**: cron for periodic refresh (e.g., refresh materialized views, cleanup jobs, sitemap generation).

**Worker runtime**: Cloud Run services (or Cloud Run Jobs for batch) that consume Pub/Sub or Tasks.

### 3.8 Search: staged approach
Search is usually the first feature that breaks at global scale; treat it as its own subsystem.

- **Phase 1** (cost-minimal): keep PostgreSQL (`pg_trgm`, FTS), add caching + strict rate limits.
- **Phase 2** (recommended for millions): adopt a dedicated search engine:
  - Managed option: **Elastic Cloud on GCP** (operationally simpler than self-hosting).
  - Self-managed option: OpenSearch/Elasticsearch on **GKE Autopilot**.
- **Indexing**: publish/update triggers Pub/Sub; search worker updates index asynchronously.

### 3.9 AI: Vertex AI (Gemini) + async execution
Use **Vertex AI Gemini** instead of direct outbound calls from the request path.

- Run AI work in workers (Cloud Run) triggered via Pub/Sub/Tasks.
- Cache AI outputs where safe (e.g., transliteration results keyed by hash of input + settings).
- Record AI job metadata and outcomes for audit/traceability.

### 3.10 Observability: Cloud Operations suite + OpenTelemetry
Adopt Google Cloud’s observability stack early:
- **Cloud Logging**: structured logs (JSON), include requestId/traceId.
- **Cloud Monitoring**: latency, error rate, saturation (DB pool), cache hit ratio, queue backlog.
- **Cloud Trace**: request tracing (via OpenTelemetry).
- **Error Reporting**: grouping for uncaught exceptions.

---

## 4. Cost Levers (Practical tactics)

### 4.1 Reduce database spend first (highest impact)
- Add Redis caching for reference data + krithi detail read models.
- Use CDN caching for **public** endpoints that can be cached safely (ETag + Cache-Control).
- Add read replicas only when caches + query tuning are insufficient.

### 4.2 Control Cloud SQL connection pressure
- Limit per-instance DB connections (small Hikari pools).
- Add PgBouncer only when needed (it’s a cost/complexity tradeoff but can unlock higher concurrency).

### 4.3 Control egress and origin compute
- Serve static assets via Cloud CDN.
- Cache public JSON at CDN for short TTL where possible.
- Compress responses (gzip/br) and use HTTP/2/3 at the edge.

### 4.4 Avoid “always-on” heavy platforms early
- Start with Cloud Run, Cloud SQL, Redis, CDN.
- Adopt GKE only when the operational benefits outweigh costs (e.g., search cluster, heavy connection pooling topologies).

---

## 5. Phased Roadmap (GCP-specific)

### Phase 1 — Foundation (Weeks 1–6)
- Cloud Run for backend + CI/CD via Cloud Build (or GitHub Actions → Artifact Registry).
- Cloud SQL Postgres (HA) + private IP + backups/PITR.
- Cloud Storage + Cloud CDN for admin web.
- Cloud Armor policies (WAF + rate limits).
- Memorystore (Redis) caching for reference + krithi detail.
- Basic Cloud Monitoring dashboards + alerting for p95 latency, error rate, DB saturation.

**Expected impact**: major reduction in DB load and origin compute, global latency improvement from CDN.

### Phase 2 — Scale Reads & Search (Months 2–4)
- Cloud SQL read replicas + read routing for public endpoints.
- Pub/Sub + Cloud Run workers for async AI jobs and search indexing.
- Introduce dedicated search engine (Elastic Cloud on GCP or GKE Autopilot).
- Strong cache invalidation strategy tied to publish/update transitions.

**Expected impact**: sustained search performance and predictable scaling under high read concurrency.

### Phase 3 — Global Reliability (Months 4–8)
- Cross-region replica for DR and faster recovery.
- Multi-region Cloud Run deployments behind global load balancer (active-active for reads, controlled writes).
- Formal SLOs, error budgets, capacity testing, and runbooks.

**Expected impact**: resilient global platform that can withstand regional incidents and large traffic spikes.

---

## 6. Key Design Changes to Make in the Application (Cloud-agnostic but GCP-motivated)

### 6.1 Cacheability and edge-friendliness
- Add **ETag** / **Last-Modified** semantics for public read models.
- Add explicit **Cache-Control** headers for safe endpoints.
- Ensure public read endpoints are **pure** (no per-user variance).

### 6.2 Split read models from write models
For global scale, treat these separately:
- **Write path**: strict consistency on Cloud SQL primary.
- **Read path**: cache + replica + eventually-consistent search index.

### 6.3 Async-first AI and import workloads
- Replace synchronous AI calls with job submission + polling/webhooks.
- Use idempotency keys to prevent duplicate work under retries.

### 6.4 Rate limiting and abuse protection
- Enforce rate limits at the edge (Cloud Armor) and optionally at the app layer (Redis counters).
- Separate admin and public hostnames; apply stricter policies to admin.

---

## 7. Concrete “First Deployment” Recommendation (minimal, production-viable)

If starting today on GCP, the most cost-effective, scalable baseline is:
- **Cloud Run** (backend)
- **Cloud SQL Postgres (HA)** (database)
- **Memorystore Redis** (cache)
- **Cloud Storage + Cloud CDN** (admin web)
- **Cloud Load Balancer + Cloud Armor** (front door)
- **Cloud Logging/Monitoring/Trace** (ops)

Then add: **read replicas**, **Pub/Sub workers**, and **dedicated search** as traffic grows.

