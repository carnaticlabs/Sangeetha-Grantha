| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.4.0 |
| **Last Updated** | 2026-09-10 |
| **Author** | Sangeetha Grantha Team |
| **Document Type** | Current guide |

# API contract

---

This guide describes the routes mounted by the current backend. New public clients should use the catalogue contracts; editorial clients use the authenticated admin API. Read [examples](./api-examples.md) for requests and [UI integration](./integration-spec.md) for client behavior.

The executable route map is [Routing.kt](../../modules/backend/api/src/main/kotlin/com/sangita/grantha/backend/api/plugins/Routing.kt). [Shared DTOs](../../modules/shared/domain/src/commonMain/kotlin/com/sangita/grantha/shared/domain/model) define serialized fields. The [OpenAPI document](../../openapi/sangita-grantha.openapi.yaml) also contains planned operations; schema presence alone does not prove that an endpoint is mounted. [OpenAPI synchronization](./openapi-sync.md) records that distinction.

## Choose a contract

| Prefix | Purpose | Access and visibility |
|:---|:---|:---|
| `/v1/catalogue` | First public catalogue contract | Anonymous; published compositions with established musical forms |
| `/v2/catalogue` | Rasika catalogue and discovery | Anonymous; published compositions, including `UNESTABLISHED` |
| `/v1/krithis` | Legacy search/detail/notation | Optional admin identity affects visibility; do not substitute these DTOs for catalogue DTOs |
| `/v1/search` | Hybrid and semantic retrieval | Optional admin auth; anonymous/non-admin results are published-only |
| `/v1/admin` | Catalogue editing, import, sourcing, curation, users and roles | JWT plus `grp_sangita_admin` for the main admin route group |
| `/v1/auth` | Token issuance and refresh | See authentication below |

The development base URL is `http://localhost:8080`. A production hostname is a deployment decision, not a provisioned service promised by this repository.

## Public catalogue

These operations are mounted under **both** `/v1/catalogue` and `/v2/catalogue`:

| Method and suffix | Result |
|:---|:---|
| `GET /krithis` | Paged composition summaries |
| `GET /krithis/{id}` | Reader metadata and available variant references |
| `GET /krithis/{id}/lyrics/{variantId}` | One stored lyric variant, its sections and source reference |
| `GET /ragas` | Paged raga directory with published-composition counts |
| `GET /ragas/{id}` | Raga identity, aliases, available scale/lineage metadata |
| `GET /composers` | Paged composer directory with published-composition counts |
| `GET /composers/{id}` | Composer metadata and aliases |

V2 additionally mounts **`GET /v2/catalogue/discovery`**. It accepts no query parameters. V2 directory endpoints for talas, deities, temples, languages, and musical forms are planned and are not mounted yet.

### Query parameters and pagination

| Parameter | Accepted by | Contract |
|:---|:---|:---|
| `query` | Composition and directory lists | Optional; trimmed; at most 200 Unicode code points |
| `composerId` | Composition list | Optional UUID |
| `ragaId` | Composition list | Optional UUID |
| `page` | Lists | Zero-based; default `0`; integer ≥ 0 |
| `pageSize` | Lists | Default `30`; integer 1–100 |

Unsupported or repeated parameters return `400`. Detail and lyric routes accept no query parameters. Use `query`, not `q`; use `pageSize`, not `size`. Legacy/admin pagination has its own request contract.

A valid empty catalogue result is:

```json
{
  "items": [],
  "total": 0,
  "page": 0,
  "pageSize": 30
}
```

### Reader and source semantics

A composition detail response lists available variants and a nullable `defaultVariantId`. Fetch a chosen variant using its own ID and the composition ID; the server verifies that relationship. A published composition may legitimately have incomplete or unavailable lyrics.

Catalogue DTOs allowlist public metadata. They omit editorial notes, author identifiers, and workflow state. Language, script, transliteration scheme, and source reference are separate concepts. Preserve stored section labels and ordered raga associations. `UNESTABLISHED` communicates that classification has not been established; clients must not invent a form.

Successful catalogue responses set `Cache-Control: no-store`. Missing, unpublished, and unavailable compositions use the same `404` boundary. Catalogue errors use their own small envelope:

```json
{
  "code": "VALIDATION_ERROR",
  "message": "page must be an integer >= 0"
}
```

The catalogue error enum also defines `NOT_FOUND` and `UNAVAILABLE`. Do not assume every API family uses this envelope: some auth failures return plain text and other routes use shared error handling.

Sources: [query parser](../../modules/backend/api/src/main/kotlin/com/sangita/grantha/backend/api/catalogue/CatalogueParameters.kt), [catalogue DTOs](../../modules/shared/domain/src/commonMain/kotlin/com/sangita/grantha/shared/domain/model/catalogue/CatalogueDtos.kt), [V2 routes](../../modules/backend/api/src/main/kotlin/com/sangita/grantha/backend/api/routes/CatalogueV2Routes.kt).

## Hybrid and semantic search

`POST /v1/search/hybrid` and `POST /v1/search/semantic` accept JSON:

```json
{
  "query": "compositions about Ganesha",
  "limit": 20
}
```

Optional `composerId` and `ragaId` fields filter by UUID. The response contains `query`, `totalMatches`, and `items`. Each item identifies a composition and a matched overview or passage, with `similarityScore` and optional lexical/RRF scores. `totalMatches` is the returned item count, not a paginated catalogue total.

With no active embedding profile, hybrid search uses lexical retrieval and semantic search returns an empty list. An incompatible active model/dimension profile is an availability error; the service refuses to mix vector spaces. See [search behavior and operations](./search.md).

## Authentication and authorization

The current console uses **`POST /v1/auth/token`**, submitting `adminToken` and either `email` or `userId` for an existing user. The response contains `token` and `expiresInSeconds`. Role claims come from stored assignments. Client-supplied roles do not grant access.

Authenticated calls carry:

```http
Authorization: Bearer <token>
Content-Type: application/json
```

`POST /v1/auth/refresh` requires a valid JWT and reloads role assignments before issuing a replacement. Main admin routes require `grp_sangita_admin`. The dashboard statistics route is an explicit optional-auth exception in the routing configuration.

`make bootstrap-admin` provisions an account with an argon2id password hash. This does **not** add an interactive password-login endpoint. `/v1/admin/login` is not mounted; OAuth/OTP remains deferred. Use the [authentication reference](../00-meta/quick-reference-auth.md) for setup.

## Editorial operations

The following route families are mounted in the authenticated admin group. Request fields and detailed validation live in the linked routes and shared request DTOs; each family has its own list/filter semantics.

| Family | Core operations | Implementation |
|:---|:---|:---|
| `/v1/admin/krithis` | Search, create, detail/update, sections, variants, tags read, transliterate, validate | [AdminKrithiRoutes](../../modules/backend/api/src/main/kotlin/com/sangita/grantha/backend/api/routes/AdminKrithiRoutes.kt) |
| `/v1/admin/variants` | Update variant; save its lyric sections | [AdminKrithiRoutes](../../modules/backend/api/src/main/kotlin/com/sangita/grantha/backend/api/routes/AdminKrithiRoutes.kt) |
| Notation routes | Notation variants and rows | [AdminNotationRoutes](../../modules/backend/api/src/main/kotlin/com/sangita/grantha/backend/api/routes/AdminNotationRoutes.kt) |
| `/v1/admin/imports` | List/create imports, scrape, review, bulk review, reingest, validation | [ImportRoutes](../../modules/backend/api/src/main/kotlin/com/sangita/grantha/backend/api/routes/ImportRoutes.kt) |
| `/v1/admin/bulk-import` | Upload; batches, jobs, tasks, events; batch controls and export | [BulkImportRoutes](../../modules/backend/api/src/main/kotlin/com/sangita/grantha/backend/api/routes/BulkImportRoutes.kt) |
| `/v1/admin/sourcing` | Sources, extractions, evidence, voting, variants, quality | [SourcingRoutes](../../modules/backend/api/src/main/kotlin/com/sangita/grantha/backend/api/routes/SourcingRoutes.kt) |
| `/v1/admin/curator` | Statistics, section issues, raga-resolution queue | [CuratorRoutes](../../modules/backend/api/src/main/kotlin/com/sangita/grantha/backend/api/routes/CuratorRoutes.kt) |
| `/v1/admin/quality` | Structural audits, remediation preview/execute, extraction processing | [RemediationRoutes](../../modules/backend/api/src/main/kotlin/com/sangita/grantha/backend/api/routes/RemediationRoutes.kt) |
| Reference entities | Composer/raga/tala/deity/temple/tag administration | [ReferenceDataRoutes](../../modules/backend/api/src/main/kotlin/com/sangita/grantha/backend/api/routes/ReferenceDataRoutes.kt) |
| Users and roles | Account and role administration | [UserManagementRoutes](../../modules/backend/api/src/main/kotlin/com/sangita/grantha/backend/api/routes/UserManagementRoutes.kt) |
| Audit | Audit queries | [AuditRoutes](../../modules/backend/api/src/main/kotlin/com/sangita/grantha/backend/api/routes/AuditRoutes.kt) |

Admin import review is `POST /v1/admin/imports/{id}/review`; reingestion is `POST /v1/admin/imports/{id}/reingest`. Older `/imports/krithis/{id}/map` and `/reject` sketches are not the mounted contract.

Some sourcing quality coverage/audit endpoints return placeholder structures. An HTTP success from those routes is not proof of a completed quality scan; use the implemented quality audit routes and diagnostic checks described in [Quality](../07-quality/README.md).

## Contract changes

Update route behavior, shared DTOs, affected clients, OpenAPI, examples, and integration tests together. Verify visibility with anonymous and admin requests; validate variant ownership; exercise invalid parameters and empty results. Preserve V1 decoding compatibility when V2 introduces new public values. Use the [OpenAPI checklist](./openapi-sync.md) to keep implemented and planned operations distinct.

---

[Section index](./README.md) · [Documentation home](./../README.md) · [Feature status](./../01-requirements/features/README.md)
