| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.3.0 |
| **Last Updated** | 2026-09-10 |
| **Author** | Sangeetha Grantha Team |
| **Document Type** | Current guide |

# OpenAPI and implementation synchronization

---

The [OpenAPI document](../../openapi/sangita-grantha.openapi.yaml), shared DTOs, route implementation, and client behavior must be reviewed together. The current OpenAPI file includes planned V2 metadata-directory operations; it is not an inventory of only mounted endpoints.

## Current boundary

V2 mounts discovery, compositions, composition lyrics, ragas, and composers. Tala, deity, temple, language, and musical-form directories remain later-release scope. Authentication uses `/v1/auth/token` and `/v1/auth/refresh`, not an assumed `/v1/admin/login` path.

The [API contract](./api-contract.md) provides the current human-readable guide and source links. This documentation refresh does not silently remove future schemas from the machine contract.

## Change checklist

1. Identify whether an operation is implemented, planned, or superseded.
2. Match HTTP method/path and authorization with the mounted route configuration.
3. Match request parameters, defaults, ranges, enum values, nullability and response fields with shared DTOs/parsers.
4. Check public visibility and editorial-field allowlisting; preserve V1 decoding compatibility when V2 adds values.
5. Update affected clients, examples, and screen mapping.
6. Exercise success, invalid request, empty result, missing/unpublished entity, and variant ownership as relevant.
7. Record any remaining machine-contract drift explicitly rather than labeling every described operation “available.”

Catalogue pagination is `page`/`pageSize`, zero-based, with its own strict parser. Search results and admin lists have separate envelopes. Do not replace all response shapes with one generic pagination example.

[Routing](../../modules/backend/api/src/main/kotlin/com/sangita/grantha/backend/api/plugins/Routing.kt) · [Shared DTOs](../../modules/shared/domain/src/commonMain/kotlin/com/sangita/grantha/shared/domain/model) · [Integration tests](../07-quality/integration-tests-approach.md)

---

[Section index](./README.md) · [Documentation home](./../README.md) · [Feature status](./../01-requirements/features/README.md)
