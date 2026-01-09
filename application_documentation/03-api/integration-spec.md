# Sangita Grantha API Integration Specification

> **Status**: Draft | **Version**: 0.1 | **Last Updated**: 2026-01-09
> **Owners**: Mobile Team, Admin Web Team, Backend Team

**Related Documents**
- [Api Contract](./api-contract.md)
- [Ui To Api Mapping](./ui-to-api-mapping.md)
- [Mobile Ui](../05-frontend/mobile/ui-specs.md)
- [Ui Specs](../05-frontend/mobile/ui-specs.md)
- [Architecture](../02-architecture/backend-system-design.md)

# Sangita Grantha API Integration Specification

This document focuses on **client integration patterns** and
**screen-level API usage** for:

- The **public mobile app** (rasika/learner facing).
- The **admin web console** (editor/reviewer facing).

The canonical endpoint set lives in [`api-contract.md`](./api-contract.md).

---

## 1. Principles

- **Single Source of Truth**: Schema and DTOs defined in
  `modules/shared/domain` and documented in
  `../database/SANGITA_SCHEMA_OVERVIEW.md` are canonical.
- **Thin Routes, Rich Services**: Ktor routes are thin; services handle
  validation, mapping, and audit logging.
- **Read-Heavy, Write-Light**: Public app is read-only; admin app handles
  curated writes through controlled workflows.
- **Stable DTOs**: Clients depend on `KrithiDto`, `KrithiLyricVariantDto`,
  `ComposerDto`, etc., which must remain backwards compatible within `/v1`.

---

## 2. Domain Overview for Clients

| Domain            | Key DTOs                             | Surfaces                     |
|-------------------|--------------------------------------|------------------------------|
| Krithi Catalog    | `KrithiDto`, `KrithiRagaDto`         | Mobile search & detail, Admin |
| Lyrics & Variants | `KrithiLyricVariantDto`, `KrithiSectionDto`, `KrithiLyricSectionDto` | Mobile detail, Admin editing |
| Reference Data    | `ComposerDto`, `RagaDto`, `TalaDto`, `DeityDto`, `TempleDto`, `TempleNameDto` | Both |
| Tags & Themes     | `TagDto`, `KrithiTagDto`             | Both                          |
| Imports           | `ImportSourceDto`, `ImportedKrithiDto` | Admin curation                |

---

## 3. Mobile App Integration (Public)

The mobile app is **read-only** in v1.

### 3.1 Search Screen (`SearchScreen`)

**Purpose**: Provide free-text and faceted search over Krithis.

**APIs**:

| Endpoint                | Method | Purpose                         |
|-------------------------|--------|---------------------------------|
| `/v1/krithis/search`    | GET    | Main search entry point        |
| `/v1/composers`         | GET    | Composer filter list (optional) |
| `/v1/ragas`             | GET    | Raga filter list (optional)     |
| `/v1/talas`             | GET    | Tala filter list (optional)     |
| `/v1/deities`           | GET    | Deity filter list (optional)    |
| `/v1/temples`           | GET    | Temple filter list (optional)   |
| `/v1/tags`              | GET    | Tag filter list (optional)      |

**Integration Notes**:

- Use `q` for free-text search, combining title/incipit and lyrics
  substring.
- Use additional filters only when the corresponding picker is set
  (composer, raga, etc.).
- Hold search state in a ViewModel to survive configuration changes.


### 3.2 Krithi Detail Screen (`KrithiDetailScreen`)

**Purpose**: Present full Krithi details, metadata, and lyric variants.

**APIs**:

| Endpoint             | Method | Purpose                 |
|----------------------|--------|-------------------------|
| `/v1/krithis/{id}`   | GET    | Krithi detail + related |

**Usage**:

- On navigation from search or favourites, fetch `/v1/krithis/{id}`.
- The response includes:
  - `krithi`: `KrithiDto`
  - `composer`, `primaryRaga`, `tala`, `deity`, `temple`
  - `ragas`: ragamalika mapping
  - `lyricVariants`: multiple `KrithiLyricVariantDto`
  - `sections`: `KrithiSectionDto` + `KrithiLyricSectionDto` by variant
  - `tags`: `TagDto[]`

**Rendering Guidance**:

- Use tabs or segmented controls for **language/script** combinations.
- Within each variant, show sections in `orderIndex` order.
- Show sampradaya/variant label and source reference prominently where present.


### 3.3 Favourites & Offline Cache

The mobile app should maintain local storage for:

- Favourited Krithis (IDs and minimal metadata).
- Recently viewed Krithis.

**Implementation Sketch**:

- Use a local DB (e.g. SQLDelight) keyed by Krithi ID.
- On entering detail screen, cache `KrithiDetail` payload.
- When offline:
  - `SearchScreen` can fall back to local subset (e.g. favourites).
  - `KrithiDetailScreen` shows cached version with offline indicator.

No dedicated API is required for favourites in v1.

---

## 4. Admin Web Integration

The admin web uses many more endpoints under `/v1/admin/**`. This
section only highlights the **primary flows**; see `ui-to-api-mapping.md`
for component-level details.

### 4.1 Admin Krithi List (`AdminKrithisPage`)

**Purpose**: Manage Krithis with filters, workflow states, and tags.

**API**:

| Endpoint                | Method | Purpose                         |
|-------------------------|--------|---------------------------------|
| `/v1/admin/krithis`     | GET    | Paged list of Krithis (all states) |

**Filters**:

- `q`, `composerId`, `ragaId`, `talaId`, `deityId`, `templeId`,
  `tag`, `language`, `workflowState`, `hasImports`.

**Usage**:

- Use query params driven by filter UI.
- Persist filter state in URL query string (for sharable URLs).

### 4.2 Krithi Edit / Detail (`AdminKrithiDetailPanel`)

**Purpose**: Single Krithi workspace for:

- Core metadata.
- Lyric variants and sections.
- Tags.
- Import mapping.

**APIs**:

| Endpoint                               | Method | Purpose              |
|----------------------------------------|--------|----------------------|
| `/v1/admin/krithis/{id}`               | GET    | Full Krithi detail   |
| `/v1/admin/krithis`                    | POST   | Create Krithi        |
| `/v1/admin/krithis/{id}`               | PUT    | Update Krithi        |
| `/v1/admin/krithis/{id}/variants`      | POST   | Create variant       |
| `/v1/admin/variants/{variantId}`       | PUT    | Update variant       |
| `/v1/admin/krithis/{id}/sections`      | POST   | Define sections      |
| `/v1/admin/variants/{variantId}/sections` | POST | Attach section text |
| `/v1/admin/krithis/{id}/tags`          | POST   | Assign tags          |
| `/v1/admin/krithis/{id}/tags/{tagId}`  | DELETE | Remove tag           |

**Integration Notes**:

- The admin panel can load `/v1/admin/krithis/{id}` once and split the
  response into sub-panels (metadata, lyrics, tags, imports).
- Use optimistic UI updates with toasts/snackbars; reconcile with
  server state after mutation succeeds.


### 4.3 Import Review (`ImportReviewPage`)

**Purpose**: Triage and map imported Krithis to canonical entities.

**APIs**:

| Endpoint                              | Method | Purpose                        |
|---------------------------------------|--------|--------------------------------|
| `/v1/admin/imports/krithis`           | GET    | List imported Krithis         |
| `/v1/admin/imports/krithis/{id}`      | GET    | ImportedKrithi detail          |
| `/v1/admin/imports/krithis/{id}/map`  | POST   | Map import to Krithi           |
| `/v1/admin/imports/krithis/{id}/reject` | POST | Reject import with notes      |

**Integration Notes**:

- The UI should highlight:
  - Parsed vs canonical fields.
  - Suggested matches (if the backend starts providing them later).
- After mapping/rejecting, update list row in-place; no full reload
  needed.

---

## 5. Ktor Client Integration (KMM)

All clients should prefer a shared Ktor client in
`modules/shared/domain`, using the same DTOs.

Example sketch (namespaced for Sangita Grantha):

```kotlin
package com.sangita.grantha.shared.domain.api

import com.sangita.grantha.shared.domain.model.KrithiDto
import com.sangita.grantha.shared.domain.model.KrithiSearchResult
import io.ktor.client.*
import io.ktor.client.call.body
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

class SangitaApiClient(
    private val baseUrl: String,
) {
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    suspend fun searchKrithis(
        q: String? = null,
        composerId: String? = null,
        page: Int = 1,
        size: Int = 25,
    ): KrithiSearchResult = client.get("$baseUrl/v1/krithis/search") {
        q?.let { parameter("q", it) }
        composerId?.let { parameter("composerId", it) }
        parameter("page", page)
        parameter("size", size)
    }.body()

    suspend fun getKrithi(id: String): KrithiDto =
        client.get("$baseUrl/v1/krithis/$id").body()
}
```

Clients can extend this client with admin-only methods behind an
injected auth header provider.

---

## 6. Error Handling on Clients

### 6.1 Common Patterns

- On `4xx` responses, parse error body using shared error DTO
  (`code`, `message`, `fields`).
- On `404` for `/v1/krithis/{id}`, show a friendly "Krithi not found or
  not yet published" message.
- On `500`, show generic error with retry option.

### 6.2 Retries & Timeouts

- Ktor client should have reasonable timeouts (e.g. 5–10 seconds).
- Avoid automatic retries for non-idempotent admin mutations.

---

## 7. Future Extensions

These are **out of scope for v1**, but should be kept in mind when
extending clients:

- Authenticated rasika accounts (sync favourites, history across
  devices).
- Audio/notation endpoints linked to `Krithi`.
- Advanced recommendation/search facets.
- Bulk data export for research use (with rate limiting and keys).

All such extensions must go through the same change management process
as core endpoints.

---

## 8. Change Management

- When adding a new client-visible field:
  - Update DTOs in `modules/shared/domain`.
  - Update `api-contract.md` and this integration spec.
  - Adjust admin and mobile views as needed.
- When introducing a breaking change:
  - Consider adding a new endpoint or version (`/v2/...`).
  - Coordinate rollout across admin and mobile clients.

This document should be reviewed whenever:
- New endpoints are added.
- DTOs change.
- New clients (e.g. web search) are introduced.
