| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.2.0 |
| **Last Updated** | 2026-09-10 |
| **Author** | Sangeetha Grantha Team |
| **Document Type** | Current guide |

# Client integration guide

---

Rasika and the Curator Console share backend infrastructure but use different read models. Select a contract by audience and preserve its visibility, pagination, and error semantics.

## Rasika

The [Ktor catalogue client](../../modules/shared/mobile-data/src/commonMain/kotlin/com/sangita/grantha/shared/mobile/network/KtorCatalogueApi.kt) targets `/v2/catalogue`. It consumes allowlisted catalogue DTOs, never editorial `KrithiDto` as a shortcut.

- Submit `query`, exact composer/raga IDs, zero-based `page`, and `pageSize` only where supported.
- Load reader metadata first, then a chosen stored variant by ID.
- Preserve the selected source/script identity and old labeled text until a variant swap succeeds.
- Treat empty, incomplete, unavailable, and failed requests as distinct states.
- Display ordered ragas; only attach a raga to a section when the DTO supplies the association.
- Do not invent a form badge for `UNESTABLISHED` or expose internal editorial fields.
- Persist bookmarks/preferences locally; do not infer a complete offline catalogue from that storage.

Later V2 directories are not mounted yet. See the [mobile PRD](../01-requirements/mobile/prd.md).

## Curator Console

The browser [API client](../../modules/frontend/sangita-admin-web/src/api/client.ts) defaults to `/v1`. It combines legacy reference/catalogue reads, admin operations, and vector-search routes. The [route map](./ui-to-api-mapping.md) identifies the main workflows.

Token exchange uses an admin token and existing identity; subsequent protected requests send a Bearer JWT. The backend reloads roles on refresh and requires the current admin role. Do not infer privileges from visible UI controls.

Lexical and vector result models differ. Catalogue pagination fields are not a universal envelope for all admin lists. Hybrid/semantic responses return ranked items and `totalMatches`, not a paginated catalogue total.

## Cross-client consistency

Public publication rules must be verified at the server. V1 and V2 deliberately differ on `UNESTABLISHED`; otherwise avoid leaking editorial fields through a reused DTO. Request parameter errors and variant ownership should be exercised in contract tests.

Successful catalogue reads use `no-store`. Legacy/reference endpoint cache behavior is distinct, so do not impose a global caching assumption. Changes to source text may also require a separate search-index refresh.

[API contract](./api-contract.md) · [Examples](./api-examples.md) · [Configuration](../08-operations/config.md) · [Quality](../07-quality/README.md)

---

[Section index](./README.md) · [Documentation home](./../README.md) · [Feature status](./../01-requirements/features/README.md)
