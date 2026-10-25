| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.4.0 |
| **Last Updated** | 2026-10-25 |
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

V2 additionally mounts **`GET /v2/catalogue/discovery`**. It accepts no query parameters.

V2 also mounts reviewed cycle collections (TRACK-150), both anonymous and published-only, accepting no query parameters:

| Method and path | Result |
|:---|:---|
| `GET /v2/catalogue/collections` | `{ items }` of collections, ordered by composer then name |
| `GET /v2/catalogue/collections/{slug}` | One collection with overview text, order basis and ordered members; `404` when unknown or when no published reviewed member remains |

A collection is visible only when its memberships were written by a reviewed seed (a `krithi_tags` row with `source = 'reviewed_cycle'` for the same kriti and tag) and the kriti is published. Members sort by role (dhyana, core, optional, mangalam, then disputed), `sequenceOrder`, title. `position` numbers CORE members only, and clients show ordinals only when `orderBasis` is `SOURCE`; `PERFORMANCE_CONVENTION` marks a later concert order such as the Ghana-raga Pancharatnam, and `UNSPECIFIED` claims no sequence. `isComplete` is false for open sets (Shodasa Ganapati, Nottusvara Sahitya). Disputed members (Rahu and Ketu in the Navagraha set) carry `label: "disputed"`. Attribute maps expose an allow-list of reviewed keys (`avarana`, `yogini`, `laya`, `structure`, `mudra`, `theme`); stored provenance and approval JSON never leave the server.

`GET /v2/catalogue/krithis/{id}` adds an optional `cycleContext` (collections with position, neighbours, plus `vibhaktiCase` and `occasionNote`). It is omitted when the kriti has no reviewed context and is never present on `/v1`. V2 directory endpoints for talas, deities, languages, and musical forms are planned and are not mounted yet.

V2 also mounts kshetras (temples, TRACK-151), anonymous, published-only and read-only:

| Method and path | Result |
|:---|:---|
| `GET /v2/catalogue/temples` | `{ items }` of eligible **root** temples, ordered by mandalam (declaration order `CHOLA, PANDYA, TONDAI, NADU, CHERA, KONGU, UTTARA`, unassigned last), then name. Optional `element` (`AKASHA, VAYU, AGNI, PRITHVI, APPU`; `APPU` is water) and `mandalam`; other, repeated or invalid parameters are `400`. Not paginated |
| `GET /v2/catalogue/temples/{id}` | One temple with sthala fields, `overview`, `parent`, nested eligible `children` and the published `krithis` recorded directly there; `400` for a malformed id, `404` when unknown or ineligible |

**Eligibility contract (shared rule).** A temple is *direct* when a published kriti has `krithis.temple_id = temple.id`, or a published `KSHETRA_OVERVIEW` search document carries that `temple_id`. It is *eligible* when it is direct or has at least one direct child, so a child stays reachable when its parent has no data of its own (the parent then shows `publishedKrithiCount = 0`). Ineligible temples are never returned by list or detail, whatever their columns hold. The rule lives in one place, `TempleEligibility` in the DAL; temple search hits (TRACK-152) and region/element facets (TRACK-153) must call it, not restate it. A non-null `temple_id` counts as reviewed because only published kritis count and publishing is a curator step.

`publishedKrithiCount` is direct only, not rolled up over children, and `childCount` counts eligible children. `recordedGap` is derived, not stored and applies to root temples only (a child shrine's overview describes it and is never a gap reason): the temple is eligible, has a published overview, and neither it nor any eligible child has a published kriti; the overview text is then the recorded reason. Child shrines are nested in detail and not repeated in the list. `place` is optional (`googlePlaceId` and `precision` `TEMPLE` or `PARENT_COMPLEX`), present only for a CONFIRMED place, and always absent until TRACK-154 lands; the id builds a Maps link client-side and no coordinates are stored or exposed.

### Lakshana facets (TRACK-153, V2 only)

Five facets exist; wire keys equal the query-parameter names. Each is derived only from stored, reviewed columns and memberships, never from text, and only published kritis count.

| Facet | Source | Offered today |
|:---|:---|:---|
| `vibhakti` | `krithis.vibhakti_case` (written only by `R__seed_09`) | 8 values, Dikshitar only |
| `collection` | reviewed cycle memberships (same `reviewed_cycle` guard as collections, all roles), value = tag slug | 12 cycles, 3 composers |
| `tune` | `axis_value` of the `nottusvara-sahitya` cycle only (pinned slug; other cycles' axis values are not tunes) | 7 tunes, Dikshitar only |
| `mandalam` (copy: Region) | the kriti's own `temple_id` temple's `mandalam`, only when `TempleEligibility` says the temple is eligible; not inherited from a parent | 7 regions, Dikshitar only |
| `element` | the same temple's `bhuta` (`APPU` is water) | 5 values, Dikshitar only |

- `GET /v2/catalogue/krithis` accepts `vibhakti`, `collection`, `tune`, `mandalam`, `element` (one value each); all filters combine with AND. A value that is blank, over 64 code points, not an enum member, or not currently offered over the whole published catalogue (stale slug, unknown tune, `SARVA_VIBHAKTI` with no kritis) is `400`; an offered value that matches nothing in combination is `200` with `total = 0`. Responses for requests without these parameters are unchanged. `/v1/catalogue/krithis` rejects them as unsupported parameters.
- `GET /v2/catalogue/facets` (`query`, `composerId`, `ragaId`, the five filters, `counts=true|false`) returns `{ facets: [{ key, values: [{ value, label?, count? }] }] }`. A facet is listed only when it has a value in the scope of `query`/`composerId`/`ragaId`, so there is no vibhakti for Tyagaraja or Syama Sastri. Listed values ignore the facet filters; with `counts=true` each facet's counts apply every other facet's filter but not its own, so a listed value can show `0`. Counts are never computed by default. `facets` is `[]` when nothing is in scope.
- `GET /v2/catalogue/krithis/{id}` adds `facetChips` (`vibhakti`, `tune`, `mandalam` only, only where a value is stored); the field is omitted when empty and never present on V1. Every chip value is accepted by the filter.
- Adding a later facet (Viloma Chapu, eduppu, swarasahitya) is one `FacetDefinition` in the DAL plus one member of `CatalogueFacetKeyDto`; no new endpoint, parser or response shape.

**Data readiness.**

| Discriminator | Stored today? | Facet status | Waits on |
|:---|:---|:---|:---|
| Vibhakti case | Yes, 481 Dikshitar | Ships | none (Tyagaraja and Syama Sastri have no vibhakti data) |
| Cycle membership | Yes, 124 reviewed memberships, 12 cycles, 3 composers | Ships | none |
| Nottusvara tune | Yes, 7 of 38 memberships | Ships | The 31 untuned memberships need curator review |
| Kshetra region (`mandalam`) | Yes, 97 temples, 481 Dikshitar kritis | Ships | Syama Sastri geography is `UNRESOLVED` (TRACK-146); Tyagaraja kritis carry no temple |
| Kshetra element (`bhuta`) | Yes, 5 temples, 25 kritis | Ships | none (small by nature) |
| Viloma Chapu, anagata eduppu, swarasahitya | No (TRACK-146 master only) | Not a facet | TRACK-148 / TRACK-149 |
| Raga mudra kind | `NONE` on all 1,226 | Not a facet | A reviewed raga-mudra pass (no track yet) |
| Yati pattern | `NONE` on 1,225, one `GOPUCCHA` | Not a facet | A reviewed yati pass (no track yet) |
| Manipravala | 1 kriti | Not a facet | Not worth a facet |

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

Sources: [query parser](../../modules/backend/api/src/main/kotlin/com/sangita/grantha/backend/api/catalogue/CatalogueParameters.kt), [catalogue DTOs](../../modules/shared/domain/src/commonMain/kotlin/com/sangita/grantha/shared/domain/model/catalogue/CatalogueDtos.kt), [V2 routes](../../modules/backend/api/src/main/kotlin/com/sangita/grantha/backend/api/routes/RasikaCatalogueRoutes.kt), [temple eligibility](../../modules/backend/dal/src/main/kotlin/com/sangita/grantha/backend/dal/repositories/TempleEligibility.kt).

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
