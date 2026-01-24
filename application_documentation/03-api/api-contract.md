| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-01-24 |
| **Author** | Sangeetha Grantha Team |

# Sangita Grantha API Contract

| Metadata | Value |
|:---|:---|
| **Status** | Draft |
| **Version** | 0.2 |
| **Last Updated** | 2026-01-20 |
| **Author** | Engineering Team |
| **Related Documents** | - [Ui To Api Mapping](./ui-to-api-mapping.md)<br>- [Integration Spec](./integration-spec.md)<br>- [Admin Web Prd](../01-requirements/admin-web/prd.md)<br>- [Mobile App Prd](../01-requirements/mobile/prd.md)<br>- [Architecture](../02-architecture/backend-system-design.md)<br>- [Sangita_Schema_Overview](../04-database/schema.md) |

# 1. Overview

The **Sangita Grantha API** exposes REST endpoints under `/v1` for
public, read-only access to the Carnatic Krithi catalog, and `/v1/admin`
for authenticated editorial and curation workflows.

This document captures the canonical contract:

- Authentication and transport assumptions.
- Read models (search and detail views).
- Admin mutations (Krithi/variant/tag/import workflows).
- Error model, pagination, and filtering.

Screen-level usage for admin and mobile apps is documented in
`integration-spec.md` and `ui-to-api-mapping.md`.

---

# 2. Authentication & Transport

## 2.1 Base URL

- Development: `http://localhost:8080`
- Production: `https://api.sangitagrantha.org` (placeholder)

## 2.2 Authentication

### Public (Read-Only) Endpoints

- Public read endpoints do **not** require authentication.
- These include:
  - `GET /v1/krithis/search`
  - `GET /v1/krithis/{id}`
  - Reference lists (if exposed publicly) such as
    - `GET /v1/composers`
    - `GET /v1/ragas`
    - `GET /v1/talas`
    - `GET /v1/deities`
    - `GET /v1/temples`

### Admin Authentication (v1)

- Admins authenticate via username/password (or external IdP in future):
  - `POST /v1/admin/login`
- Returns a JWT access token and optional refresh token with role claims.
- Admin requests include header:

  ```http
  Authorization: Bearer <accessToken>
  ```

- Tokens include claims:
  - `userId`: UUID
  - `email`: string
  - `roles[]`: array of role codes (e.g. `admin`, `editor`, `reviewer`).

### Security

- All production endpoints require **HTTPS**.
- JWT access tokens expire after a configured time (e.g. 1 hour); refresh
  strategy can be added later.

## 2.3 Transport

- Content-Type: `application/json` for request/response bodies.
- Accept: `application/json` for responses.

---

# 3. Public Query Endpoints (Read Models)

## 3.1 Krithi Search

### `GET /v1/krithis/search`

Search and browse Krithis.

**Request Query Parameters**:

- `q`: optional free-text query (title, incipit, lyrics substring)
- `composerId`: optional UUID
- `ragaId`: optional UUID
- `talaId`: optional UUID
- `deityId`: optional UUID
- `templeId`: optional UUID
- `tag`: optional tag slug (e.g. `navaratri`, `bhakti`)
- `language`: optional language code (e.g. `sa`, `ta`, `te`, `kn`, `ml`, `hi`, `en`)
- `page`: page number (default: 1, min: 1)
- `size`: page size (default: 25, max: 100)

**Behaviour**:

- Only returns Krithis with `workflow_state = 'published'`.
- If `q` is provided:
  - Searches `title_normalized`, `incipit_normalized`, and lyrics substring
    via trigram index on `krithi_lyric_variants.lyrics`.
- Filtering is applied conjunctively (all provided filters must match).

**Response** (`200 OK`):

```json
{
  "items": [
    {
      "id": "uuid",
      "title": "Vatapi Ganapatim",
      "incipit": "Vātāpi gaṇapatim bhajeham",
      "titleNormalized": "vatapi ganapatim",
      "incipitNormalized": "vatapi ganapatim bhajeham",
      "composerId": "uuid",
      "primaryRagaId": "uuid",
      "talaId": "uuid",
      "deityId": "uuid",
      "templeId": "uuid",
      "primaryLanguage": "sa",
      "musicalForm": "KRITHI",
      "isRagamalika": false,
      "workflowState": "PUBLISHED",
      "createdAt": "2025-01-01T00:00:00Z",
      "updatedAt": "2025-01-02T00:00:00Z"
    }
  ],
  "page": 1,
  "size": 25,
  "total": 123
}
```

(Response body aligns with `KrithiDto` and `KrithiSearchResult`.)

---

## 3.2 Krithi Detail

### `GET /v1/krithis/{id}`

Fetch full details for a single Krithi.

Path params:
- `id`: UUID of Krithi.

**Response** (`200 OK`):

```json
{
  "krithi": { /* KrithiDto */ },
  "composer": { /* ComposerDto */ },
  "primaryRaga": { /* RagaDto or null */ },
  "tala": { /* TalaDto or null */ },
  "deity": { /* DeityDto or null */ },
  "temple": { /* TempleDto or null */ },
  "ragas": [ /* KrithiRagaDto[] with RagaDto embedded or referenced */ ],
  "lyricVariants": [ /* KrithiLyricVariantDto[] */ ],
  "sections": [ /* KrithiSectionDto[] + optional KrithiLyricSectionDto by variant */ ],
  "notationVariants": [ /* KrithiNotationVariantDto[] - only for VARNAM/SWARAJATHI */ ],
  "tags": [ /* TagDto[] */ ]
}
```

**Error Cases**:
- `404 Not Found` with `code = "not_found"` if `id` does not exist or is not `published`.

---

## 3.3 Reference Data (optional public)

Depending on product decisions, some reference endpoints may be public:

- `GET /v1/composers`
- `GET /v1/ragas`
- `GET /v1/talas`
- `GET /v1/deities`
- `GET /v1/temples`
- `GET /v1/tags`

Each returns a paginated list of the corresponding `*Dto` objects.

---

# 4. Admin Query Endpoints

All endpoints under `/v1/admin/**` require admin JWT and role checks.

## 4.1 Admin Krithi Browsing

### `GET /v1/admin/krithis`

Admin-facing list of Krithis with extended filters.

Query parameters:
- Same as public `/v1/krithis/search` plus:
  - `workflowState`: filter by `draft`, `in_review`, `published`, `archived`.
  - `hasImports`: optional boolean (Krithis linked to `imported_krithis`).

Response: same pagination shape as public search, but can include
non-published Krithis.

### `GET /v1/admin/krithis/{id}`

Same data as public detail endpoint, but includes non-published Krithis
and editorial metadata.

---

## 4.2 Import Review

### `GET /v1/admin/imports/krithis`

List imported Krithis for review.

Query parameters:
- `status`: `pending | in_review | mapped | rejected | discarded` (string)
- `sourceId`: optional import source UUID
- `q`: optional free-text (raw title/composer/lyrics)
- `page`, `size`: pagination

Response:

```json
{
  "items": [
    {
      "id": "uuid",
      "importSourceId": "uuid",
      "sourceKey": "https://karnatik.com/song.php?id=123",
      "rawTitle": "Vatapi Ganapatim",
      "rawComposer": "Dikshitar",
      "rawRaga": "Hamsadhvani",
      "rawTala": "Adi",
      "rawLanguage": "Sanskrit",
      "importStatus": "PENDING",
      "mappedKrithiId": null,
      "reviewerUserId": null,
      "reviewerNotes": null,
      "reviewedAt": null,
      "createdAt": "2025-01-01T00:00:00Z"
    }
  ],
  "page": 1,
  "size": 25,
  "total": 42
}
```

### `GET /v1/admin/imports/krithis/{id}`

Returns full `ImportedKrithiDto` plus any parsed payload and
links to candidate canonical entities.

---

# 5. Admin Mutation Endpoints (v1 Scope)

These are **editorial** mutations; there are no participant/user-facing
mutations in v1 of Sangita Grantha.

## 5.1 Auth

### `POST /v1/admin/login`

- Request:

  ```json
  {
    "email": "editor@example.org",
    "password": "string"
  }
  ```

- Response (`200 OK`):

  ```json
  {
    "accessToken": "jwt",
    "expiresInSeconds": 3600,
    "user": {
      "id": "uuid",
      "email": "editor@example.org",
      "fullName": "Editor Name",
      "roles": ["editor"]
    }
  }
  ```

---

## 5.2 Krithi & Variant Management

### `POST /v1/admin/krithis`

Create a new Krithi.

- Request body aligns with `KrithiDto` minus generated fields:

  ```json
  {
    "title": "string",
    "incipit": "string?",
    "composerId": "uuid",
    "primaryRagaId": "uuid?",
    "talaId": "uuid?",
    "deityId": "uuid?",
    "templeId": "uuid?",
    "primaryLanguage": "SA",
    "musicalForm": "KRITHI",
    "isRagamalika": false,
    "sahityaSummary": "string?",
    "notes": "string?"
  }
  ```

- Response (`201 Created`): `KrithiDto`.

- Rules:
  - Default `workflowState = DRAFT`.
  - All creates must write an `audit_log` entry.


### `PUT /v1/admin/krithis/{id}`

Update an existing Krithi (idempotent, full update or patch semantics,
to be defined in implementation).

### `POST /v1/admin/krithis/{id}/variants`

Create a new lyric variant for a Krithi.

- Request: subset of `KrithiLyricVariantDto` without IDs or audit fields.
- Response: created `KrithiLyricVariantDto`.

### `PUT /v1/admin/variants/{variantId}`

Update lyric variant (language/script cannot be changed after creation
in v1; text, sampradaya, labels, and primary flag can be changed).

### `POST /v1/admin/krithis/{id}/sections`

Define structural sections (pallavi, anupallavi, charanams, etc.) for a
Krithi.

### `POST /v1/admin/variants/{variantId}/sections`

Attach section text for a given lyric variant using
`krithi_lyric_sections`.

All these endpoints follow the patterns:
- Role checks (`editor`/`reviewer`/`admin`).
- Validation.
- Transaction via `DatabaseFactory.dbQuery {}`.
- Audit logging.

---

## 5.2.1 Notation Management (Varnams & Swarajathis)

Notation endpoints are only applicable for compositions with `musicalForm` of `VARNAM` or `SWARAJATHI`.

### `GET /v1/admin/krithis/{id}/notation`

Retrieve all notation variants for a Krithi.

- Response: Array of `KrithiNotationVariantDto` objects, each containing:
  - `id`, `krithiId`, `notationType` (SWARA | JATHI)
  - `talaId`, `kalai`, `eduppuOffsetBeats`
  - `variantLabel`, `sourceReference`, `isPrimary`
  - `notationRows`: Array of `KrithiNotationRowDto` objects

### `POST /v1/admin/krithis/{id}/notation`

Create a new notation variant.

- Request:

  ```json
  {
    "notationType": "SWARA",
    "talaId": "uuid?",
    "kalai": 1,
    "eduppuOffsetBeats": 0,
    "variantLabel": "string?",
    "sourceReference": "string?",
    "isPrimary": false,
    "notationRows": [
      {
        "sectionId": "uuid",
        "orderIndex": 0,
        "swaraText": "S R G M P D N S",
        "sahityaText": "string?",
        "talaMarkers": "string?"
      }
    ]
  }
  ```

- Response: Created `KrithiNotationVariantDto` with all rows.

### `PUT /v1/admin/notation/{variantId}`

Update a notation variant (metadata only; rows updated separately).

### `POST /v1/admin/notation/{variantId}/rows`

Add or update notation rows for a variant.

- Request: Array of `KrithiNotationRowDto` objects.

### `DELETE /v1/admin/notation/{variantId}`

Delete a notation variant and all its rows.

---

## 5.3 Tags

### `POST /v1/admin/tags`

Create new tag.

### `PUT /v1/admin/tags/{id}`

Update tag metadata.

### `POST /v1/admin/krithis/{id}/tags`

Assign tags to a Krithi.

- Request:

  ```json
  {
    "tagIds": ["uuid", "uuid"]
  }
  ```

### `DELETE /v1/admin/krithis/{id}/tags/{tagId}`

Unassign a single tag from a Krithi.

---

## 5.4 Import Status Updates

### `POST /v1/admin/imports/krithis/{id}/map`

Map an `ImportedKrithi` to a canonical `Krithi`.

- Request:

  ```json
  {
    "krithiId": "uuid",
    "notes": "Mapped to existing Vatapi Ganapatim record"
  }
  ```

- Behaviour:
  - Sets `mapped_krithi_id`.
  - Moves `import_status` to `MAPPED`.
  - Writes audit log.

### `POST /v1/admin/imports/krithis/{id}/reject`

Reject an imported entry.

- Sets `import_status = REJECTED` with reviewer notes.

---

## 5.5 AI & Import Operations

### `POST /v1/admin/imports/scrape`

Scrape metadata and content from a supported external URL (e.g., shivkumar.org).

- Request: `ScrapeRequest`
  ```json
  { "url": "http://shivkumar.org/musical/..." }
  ```
- Response: `ImportedKrithiDto` (Created status `PENDING`).

### `GET /v1/admin/imports`

List imported records with optional filtering.

- Query Params:
  - `status`: `PENDING`, `IMPORTED`, `REJECTED`

### `POST /v1/admin/krithis/{id}/transliterate`

AI-powered transliteration of lyrics or notation.

- Request: `TransliterationRequest`
  ```json
  {
    "content": "raw text...",
    "targetScript": "latn",
    "sourceScript": "deva"
  }
  ```
- Response: `TransliterationResponse`
  ```json
  {
    "transliterated": "transliterated text...",
    "targetScript": "latn"
  }
  ```

---

# 6. Error Model

## 6.1 Error Response Format

```json
{
  "code": "string",
  "message": "human readable summary",
  "fields": {
    "fieldName": "issue description"
  },
  "timestamp": "2025-12-21T10:30:00Z",
  "requestId": "optional-request-id"
}
```

## 6.2 Error Codes and HTTP Status Codes

| HTTP Status | Error Code         | Description                             |
|-------------|--------------------|-----------------------------------------|
| 400         | `validation_error` | Validation errors on input              |
| 401         | `unauthorized`     | Missing/invalid admin token             |
| 403         | `forbidden`        | Lacking required role                   |
| 404         | `not_found`        | Resource not found                      |
| 409         | `conflict`         | Uniqueness or state conflict            |
| 429         | `throttled`        | Rate limit exceeded (if applicable)     |
| 500         | `internal_error`   | Unexpected server error                 |

---

# 7. Pagination & Filtering

## 7.1 Pagination

Standard pagination for list endpoints:

```json
{
  "items": [...],
  "page": 1,
  "size": 25,
  "total": 123
}
```

Query parameters:
- `page`: default 1
- `size`: default 25, max 100 for public, higher for admin where safe.

## 7.2 Filtering

Typical filters:
- Krithis: `q`, `composerId`, `ragaId`, `talaId`, `deityId`, `templeId`,
  `tag`, `language`, `workflowState` (admin only).
- Imports: `status`, `sourceId`, `q`.

---

# 8. Authentication & Authorization Summary

- Public read endpoints: no authentication, only published Krithis.
- Admin endpoints:
  - `POST /v1/admin/login` to obtain JWT.
  - JWT required for all `/v1/admin/**` requests.
  - Role-based checks enforced at route or service boundaries.

---

# 9. Change Management

- Any change to this contract must be reflected in:
  - KMM shared DTOs (`modules/shared/domain`).
  - Backend route handlers and services.
  - Admin web and mobile integration specs.
- Backward incompatible changes require versioning (`/v2/...`).

---

# 10. Implementation Status

This contract describes **intended v1 endpoints**. Actual implementation
status for Sangita Grantha will be tracked via:

- Backend route and service tests.
- Admin web UI integration.
- Migration notes in `database/migrations/`.

Unset endpoints MUST return `501 Not Implemented` with
`code = "not_implemented"` until they are complete.
