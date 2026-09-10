| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.2.0 |
| **Last Updated** | 2026-09-10 |
| **Author** | Sangeetha Grantha Team |
| **Document Type** | Current guide |

# Audit events and content history

---

Every application mutation must create an `audit_log` event. Audit records identify the affected entity, action, actor where available, time, and available diff/metadata. They support operational and editorial investigation.

## Audit versus canon revisions

| Question | Use |
|:---|:---|
| Who performed an operation and on which entity? | Audit event |
| What section text was accepted at a particular time? | Canon revision and section snapshots |
| Which document/extraction produced a section? | Revision provenance and source-document/extraction joins |
| Which sources contributed to this composition overall? | Source evidence |

An audit event is not automatically a complete replayable snapshot. The [versioned canon](./versioned-canon.md) deliberately stores historical content separately.

## Write and read paths

Kotlin mutation paths use audit services/repositories alongside controlled persistence. Embedding-index scripts use shared audited write helpers for profile/index changes. Actorless automation should retain its script/run context rather than impersonating a curator.

The console exposes audit context through editorial views. [AuditRoutes](../../modules/backend/api/src/main/kotlin/com/sangita/grantha/backend/api/routes/AuditRoutes.kt) and [AuditLogRepository](../../modules/backend/dal/src/main/kotlin/com/sangita/grantha/backend/dal/repositories/AuditLogRepository.kt) define query behavior. Do not infer a public history API from an admin audit screen.

## Verify

For a representative mutation, confirm entity identity, action, actor/script attribution, timestamp and relevant metadata agree with the accepted result. For content changes, additionally inspect revision text/source attribution and current API output. See [post-import verification](../07-quality/qa/test-plan.md).

---

[Section index](./README.md) · [Documentation home](./../README.md) · [Feature status](./../01-requirements/features/README.md)
