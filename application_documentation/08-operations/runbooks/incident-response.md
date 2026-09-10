| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.2.0 |
| **Last Updated** | 2026-09-10 |
| **Author** | Sangeetha Grantha Team |
| **Document Type** | Current guide |

# Investigate an application incident

---

Start by identifying the affected environment, user journey, time range, and scope. Preserve the source payload, request/task identifiers, and relevant logs before retrying or changing data.

## Triage

1. Check service state and logs; basic `/health` is not a full readiness probe.
2. Identify the boundary: client transport, authorization, API/service, database, extraction, provider, or indexing.
3. Reproduce with the smallest affected request/source scope.
4. Compare the observed result with the current API/pipeline contract.

## Choose the recovery path

| Failure | Recovery direction |
|:---|:---|
| Wrong local endpoint or credentials | Correct component-specific configuration and restart |
| Migration/history failure | Preserve backup/history and rehearse a targeted Flyway reconciliation in isolation |
| Failed extraction | Inspect source/path/format/provider error, then retry the intended request |
| Incorrect accepted text | Fix parser/mapping and use targeted reingestion/curation with provenance |
| Search index mismatch | Inspect active profile, content hashes and query embedder before reindexing/activation |
| Native reader issue | Capture device/runtime, selected variant and request state; exercise the affected journey |

Avoid broad resets or queue-wide retries before understanding which records are affected. A retry may create provider usage or mutate accepted content.

## Close with evidence

Record cause, affected scope, corrective action, checks actually run, and unresolved follow-up. Verify the affected user journey and relevant database/API state after recovery. Historical “zero issues” reports and empty placeholder dashboards are not evidence for this incident.

[Monitoring](../monitoring.md) · [Configuration](../config.md) · [Database runbook](./database-runbook.md) · [Post-import checks](../../07-quality/qa/test-plan.md)

---

[Section index](./README.md) · [Documentation home](./../../README.md) · [Feature status](./../../01-requirements/features/README.md)
