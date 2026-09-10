| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.2.0 |
| **Last Updated** | 2026-09-10 |
| **Author** | Sangeetha Grantha Team |
| **Document Type** | Current guide |

# Mutation handling and audit boundaries

---

Backend routes parse requests and delegate to services. Services coordinate domain decisions; repositories execute SQL inside `DatabaseFactory.dbQuery`. Return explicit serializable DTOs across those boundaries.

## A controlled mutation

1. Authenticate and enforce the required server-side role.
2. Validate identifiers, payload fields, ownership and operation state.
3. Resolve domain identity using canonical references and evidence.
4. Persist the intended changes in the appropriate transaction boundary.
5. Record the audit event and, for accepted canon changes, the required revision/provenance.
6. Return the actual persisted result and a meaningful failure when the operation cannot complete.

Do not confuse a requested invariant with an automatic guarantee across independently called repositories. Review the real service transaction boundary when adding multi-step changes. Use tests to establish failure atomicity where required.

## Corpus-specific rules

Lyric sections belong to a composition and a specific variant. Ordered raga membership belongs in `krithi_ragas`, not only a primary foreign key. Unknown raga identity requires controlled resolution. Accepted source corrections use extraction/reingestion/curation rather than new one-off corpus SQL migrations.

[Versioned canon](../04-database/versioned-canon.md) records historical section text; [audit logging](../04-database/audit-log.md) records mutation events. Both differ from the public current-state reader model.

## Source references

- [Admin routes](../../modules/backend/api/src/main/kotlin/com/sangita/grantha/backend/api/routes/AdminKrithiRoutes.kt)
- [Import service](../../modules/backend/api/src/main/kotlin/com/sangita/grantha/backend/api/services/ImportService.kt)
- [Raga resolution](../../modules/backend/api/src/main/kotlin/com/sangita/grantha/backend/api/services/RagaResolutionService.kt)
- [DAL repositories](../../modules/backend/dal/src/main/kotlin/com/sangita/grantha/backend/dal/repositories)
- [API contracts](../03-api/api-contract.md), [quality scenarios](../07-quality/integration-tests-approach.md)

---

[Section index](./README.md) · [Documentation home](./../README.md) · [Feature status](./../01-requirements/features/README.md)
