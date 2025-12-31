---
title: Mutation Handlers
status: Draft
version: 0.2
last_updated: 2025-01-27
owners:
  - Sangita Grantha Backend Team
related_docs:
  - ../api/api-contract.md
  - ./architecture.md
  - ./security-requirements.md
---

# Mutation Handlers – Sangita Grantha

This document captures the **editorial mutation workflows** exposed
via `/v1/admin/**` for Sangita Grantha. It describes how write-paths
around Krithis, lyric variants, tags, and imports should behave.

Implementation status for each area will be tracked separately in code
and tests; this file is the behavioural spec.

---

## 1. Krithi Lifecycle

### 1.1 Create Krithi

**UI Trigger**
- "New Krithi" action in Admin Web (e.g. Krithi list page or composer
  detail page).

**Endpoint**
- `POST /v1/admin/krithis`

**Request**
- Based on `KrithiDto` minus generated fields:

```json
{
  "title": "Vatapi Ganapatim",
  "incipit": "Vatapi ganapatim bhaje ham",
  "composerId": "uuid",
  "primaryRagaId": "uuid",
  "talaId": "uuid",
  "deityId": "uuid?",
  "templeId": "uuid?",
  "primaryLanguage": "SA",
  "musicalForm": "KRITHI",
  "isRagamalika": false,
  "sahityaSummary": "Short English meaning",
  "notes": "Editorial notes"
}
```

**Behaviour**
1. **Auth**: Require admin JWT; roles `editor` or `admin`.
2. **Validation**:
   - `title` non-empty.
   - `composerId` exists.
   - If provided: raga/tala/deity/temple IDs exist.
3. **Persistence**:
   - Insert into `krithis`.
   - Set `workflow_state = 'draft'`.
   - Set `created_by_user_id` from principal.
4. **Audit**:
   - Insert `audit_log` row with `action = 'CREATE'` and diff containing
     the new record.
5. **Response**:
   - Return `201 Created` with `KrithiDto`.

### 1.2 Update Krithi

**Endpoint**
- `PUT /v1/admin/krithis/{id}`

**Behaviour**
1. Auth & role checks as above.
2. Validate fields and allowed state transitions (e.g. cannot edit
   certain fields when archived; detail rules TBD).
3. Wrap in `DatabaseFactory.dbQuery {}`.
4. Fetch `before` snapshot, perform update, fetch `after` snapshot.
5. Write audit log with `action = 'UPDATE'` and `diff` containing
   `before`/`after`.

### 1.3 Workflow Transitions

Workflow state is `draft | in_review | published | archived`.

- Transitions must be explicit; e.g. dedicated actions in UI:
  - **Submit for Review**: `draft → in_review`.
  - **Publish**: `in_review → published`.
  - **Send Back to Draft**: `in_review → draft`.
  - **Archive**: `published → archived` (or via separate action).
- Implementation detail: transitions may be modeled as dedicated
  endpoints or as part of `PUT /v1/admin/krithis/{id}` with a
  `workflowState` field and validation.
- Every transition must generate an audit record with action names
  like `SUBMIT_FOR_REVIEW`, `PUBLISH`, `ARCHIVE`.

---

## 2. Lyric Variants & Sections

### 2.1 Create Lyric Variant

**Endpoint**
- `POST /v1/admin/krithis/{id}/variants`

**Behaviour**
1. Validate Krithi ID exists.
2. Validate language/script enums.
3. Optionally validate sampradaya ID if provided.
4. Insert into `krithi_lyric_variants` with:
   - `krithi_id`.
   - `language`, `script`, `transliteration_scheme`.
   - `sampradaya_id`, `variant_label`, `source_reference`.
   - `lyrics`.
5. If `is_primary = true`, ensure no other variant for the same
   `(krithi_id, language, script)` is primary.
6. Audit with `action = 'CREATE_VARIANT'`.

### 2.2 Update Lyric Variant

**Endpoint**
- `PUT /v1/admin/variants/{variantId}`

**Behaviour**
- Allow edits to:
  - `lyrics`, `is_primary`, `sampradaya_id`, `variant_label`,
    `source_reference`.
- Do **not** allow language/script changes in v1.
- Enforce primary uniqueness per `(krithi_id, language, script)`.
- Audit with before/after diff.

### 2.3 Define Krithi Sections

**Endpoint**
- `POST /v1/admin/krithis/{id}/sections`

**Behaviour**
- Accept an ordered list of sections with `section_type`,
  `order_index`, `label`.
- Upsert into `krithi_sections` for the Krithi.
- Enforce `UNIQUE (krithi_id, order_index)`.
- Audit per create/update.

### 2.4 Attach Section Text for Variant

**Endpoint**
- `POST /v1/admin/variants/{variantId}/sections`

**Behaviour**
- For each section ID and text:
  - Insert or update into `krithi_lyric_sections`.
  - Maintain the `UNIQUE (lyric_variant_id, section_id)` constraint.
- Optional: maintain a normalized text column for search.
- Audit with `action = 'UPDATE_LYRIC_SECTIONS'`.

---

## 3. Tags & Themes

### 3.1 Maintain Tag Catalog

**Endpoints**
- `POST /v1/admin/tags` (create)
- `PUT /v1/admin/tags/{id}` (update)

**Behaviour**
- Validate category, slug uniqueness, and display name.
- Reserved categories: `BHAVA`, `FESTIVAL`, `PHILOSOPHY`, `KSHETRA`,
  `STOTRA_STYLE`, `NAYIKA_BHAVA`, `OTHER`.
- Audit all tag creations/updates.

### 3.2 Assign/Unassign Tags to Krithis

**Endpoints**
- `POST /v1/admin/krithis/{id}/tags`
- `DELETE /v1/admin/krithis/{id}/tags/{tagId}`

**Behaviour**
- Create/delete rows in `krithi_tags`.
- Optionally accept `source` (`manual` vs `import`) and `confidence`.
- Audit with `action = 'TAG_ASSIGN'` / `'TAG_REMOVE'`.

---

## 4. Import Review & Mapping

### 4.1 Map ImportedKrithi to Canonical Krithi

**Endpoint**
- `POST /v1/admin/imports/krithis/{id}/map`

**Behaviour**
1. Validate import row exists and is in a mappable state
   (`pending` or `in_review`).
2. Validate canonical `krithiId` exists.
3. Set `mapped_krithi_id` and `import_status = 'mapped'`.
4. Set `reviewer_user_id`, `reviewer_notes`, and `reviewed_at`.
5. Audit with `action = 'IMPORT_MAP'` and diff showing old/new
   status and mapping.

### 4.2 Reject ImportedKrithi

**Endpoint**
- `POST /v1/admin/imports/krithis/{id}/reject`

**Behaviour**
- Set `import_status = 'rejected'` and record reviewer notes.
- Audit with `action = 'IMPORT_REJECT'`.

---

## 5. Reference Data Maintenance

Reference data includes composers, ragas, talas, deities, temples,
`sampradayas`, and `tags`.

### 5.1 General Pattern

- Endpoints (examples, not exhaustive):
  - `POST /v1/admin/composers`
  - `PUT /v1/admin/composers/{id}`
  - `POST /v1/admin/ragas`
  - `PUT /v1/admin/ragas/{id}`
- Behaviour:
  - Validate names, normalized names, and any unique constraints.
  - Avoid hard deletes; prefer archival flags if deletion semantics
    are needed later.
  - Audit all changes.

---

## 6. Implementation Patterns

### 6.1 Transactions via dbQuery

All mutations must be implemented using:

```kotlin
suspend fun <T> dbQuery(block: Transaction.() -> T): T
```

Example:

```kotlin
suspend fun createKrithi(request: CreateKrithiRequest, actorId: Uuid): KrithiDto =
    DatabaseFactory.dbQuery {
        val id = KrithisTable.insertAndGetId { row ->
            row[title] = request.title
            // ... set other fields ...
        }.value

        // audit_log insert here

        KrithisTable.select { KrithisTable.id eq id }
            .map { it.toKrithiDto() }
            .single()
    }
```

### 6.2 Validation

- Route handlers should validate JSON shape (via DTOs) and required
  fields.
- Services should enforce business rules and relationships
  (existence of foreign keys, allowed state transitions, uniqueness
  of primary variants, etc.).

### 6.3 Audit Logging

- Use a small helper in DAL or service layer to insert into `audit_log`.
- Always capture:
  - Actor ID.
  - Action.
  - Entity table and ID.
  - Timestamp.
  - Diff (where meaningful).

### 6.4 Error Handling

- Throw typed exceptions (e.g. `ValidationException`, `NotFoundException`).
- Map them to structured error responses via Ktor `StatusPages`.
- Do not include stack traces in responses.

---

## 7. Notation Mutations (Varnams & Swarajathis)

Notation mutations are only applicable for compositions with `musicalForm`
of `VARNAM` or `SWARAJATHI`.

### 7.1 Create Notation Variant

**UI Trigger**
- "Add Notation" action in Admin Web for Varnam/Swarajathi compositions.

**Endpoint**
- `POST /v1/admin/krithis/{id}/notation`

**Request**
- Notation variant metadata plus array of notation rows:

```json
{
  "notationType": "SWARA",
  "talaId": "uuid?",
  "kalai": 1,
  "eduppuOffsetBeats": 0,
  "variantLabel": "Lalgudi bani",
  "sourceReference": "SSP",
  "isPrimary": false,
  "notationRows": [
    {
      "sectionId": "uuid",
      "orderIndex": 0,
      "swaraText": "S R G M P D N S",
      "sahityaText": "optional",
      "talaMarkers": "| | | |"
    }
  ]
}
```

**Behavior**
- Creates `krithi_notation_variant` record
- Creates all `krithi_notation_row` records
- Writes audit log entry
- Validates that Krithi has `musicalForm` of `VARNAM` or `SWARAJATHI`

### 7.2 Update Notation Variant

**Endpoint**
- `PUT /v1/admin/notation/{variantId}`

Updates metadata only (tala, kalai, eduppu, labels). Rows must be updated
separately.

### 7.3 Update Notation Rows

**Endpoint**
- `POST /v1/admin/notation/{variantId}/rows`

Replaces all rows for a variant. Request is array of `KrithiNotationRowDto`.

### 7.4 Delete Notation Variant

**Endpoint**
- `DELETE /v1/admin/notation/{variantId}`

Cascades to all notation rows. Writes audit log.

---

## 8. Out of Scope (v1)

- Participant/user-facing mutations (accounts, comments, ratings).
- Audio file upload or processing.
- Bulk export APIs.

These may be added in later phases with their own mutation specs.
