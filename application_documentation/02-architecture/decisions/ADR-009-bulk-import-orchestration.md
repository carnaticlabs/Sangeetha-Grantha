| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-01-27 |
| **Author** | Sangeetha Grantha Team |

---

# ADR-009: Bulk Import Orchestration Architecture

## Context
Bulk import ingests large volumes of krithis from CSV manifests and web sources. The workflow involves multi-stage tasks (manifest ingest → scrape → entity resolution → review), with retries, rate limiting, and batch-level progress tracking.

## Decision
Adopt a **worker-based orchestration model** with explicit job/task tables and a multi-stage pipeline.

## Rationale
- **Scalability**: Task queues with configurable worker counts support horizontal scaling.
- **Resilience**: Retries and watchdogs handle transient failures and stuck tasks.
- **Observability**: Batch/job/task tables provide clear visibility into progress and errors.
- **Extensibility**: New job types can be added without rewriting the pipeline.

## Consequences
- Requires careful synchronization of counters and status transitions.
- Channels and worker pools must be monitored for backpressure.
- Additional operational complexity (rate limiting, watchdog tuning).

## Follow-up
- Add metrics for worker throughput and queue depth.
- Continue decomposing worker responsibilities to maintain clarity.
