| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.2.0 |
| **Last Updated** | 2026-06-13 |
| **Author** | Sangeetha Grantha Team |

# Sangita Grantha Domain Model Overview


# 1. Purpose

This document outlines the **canonical shared domain model** for
Sangita Grantha. It connects the product requirements (root PRD) to the
schema and data structures used by:

- Kotlin Multiplatform shared module (`modules/shared/domain`).
- Backend services (Ktor + Exposed).
- Admin web and mobile clients.
- Application documentation (schema, API specs, ERD).

The goal is to keep **one mental model** of the Carnatic Krithi catalog
across all surfaces.

---

# 2. Core Entities

| Entity              | Description                                         | Key Fields                                                                                                     | Related Entities                               |
|---------------------|-----------------------------------------------------|----------------------------------------------------------------------------------------------------------------|------------------------------------------------|
| `User`              | Admin/editor/reviewer identity.                   | `id`, `email`, `fullName`, `displayName`, `passwordHash`, `isActive`, timestamps                              | `RoleAssignment`, `AuditLog`, editorial fields on `Krithi`/`KrithiLyricVariant`/`KrithiNotationVariant` |
| `Role`              | RBAC role definition.                             | `code`, `name`, `capabilities` (JSONB)                                                                                | `RoleAssignment`                              |
| `RoleAssignment`    | Assignment of roles to users.                     | `userId`, `roleCode`, `assignedAt`                                                                            | `User`, `Role`                                |
| `Composer`          | Canonical Carnatic composer.                        | `id`, `name`, `nameNormalized`, `birthYear`, `deathYear`, `place`, `notes`, timestamps                                    | `Krithi`                                      |
| `Raga`              | Canonical raga dictionary (melakarta + janya).      | `id`, `name`, `nameNormalized`, `melakartaNumber`, `parentRagaId`, `arohanam`, `avarohanam`, `notes`, timestamps          | `Krithi`, `KrithiRaga`                        |
| `Tala`              | Tala / rhythmic cycle definitions.                 | `id`, `name`, `nameNormalized`, `angaStructure`, `beatCount`, `notes`, timestamps                                         | `Krithi`, `KrithiNotationVariant`                                      |
| `Deity`             | Deity addressed by a Krithi.                       | `id`, `name`, `nameNormalized`, `description`, timestamps                                                                  | `Krithi`, `Temple`                            |
| `Temple`            | Temple / kshetram metadata.                        | `id`, `name`, `nameNormalized`, `city`, `state`, `country`, `primaryDeityId`, `latitude`, `longitude`, `notes`, timestamps| `Krithi`, `Deity`, `TempleName`               |
| `TempleName`        | Multilingual/alias temple names.                   | `id`, `templeId`, `languageCode`, `scriptCode`, `name`, `normalizedName`, `isPrimary`, `source`, `createdAt`              | `Temple`                                      |
| `Krithi`            | Core musical composition (sahitya + metadata).     | `id`, `title`, `incipit`, `titleNormalized`, `incipitNormalized`, `composerId`, `primaryRagaId`, `talaId`, `deityId`, `templeId`, `primaryLanguage`, `musicalForm`, `isRagamalika`, `workflowState`, `sahityaSummary`, `notes`, `createdByUserId`, `updatedByUserId`, timestamps | `Composer`, `Raga`, `Tala`, `Deity`, `Temple`, `KrithiRaga`, `KrithiLyricVariant`, `KrithiSection`, `KrithiTag`, `KrithiNotationVariant` |
| `KrithiRaga`        | Mapping of a Krithi to one or more ragas.          | `krithiId`, `ragaId`, `orderIndex`, `section` (raga_section_enum), `notes`                                                         | `Krithi`, `Raga`                              |
| `KrithiLyricVariant` | Lyric variant in specific language/script/school. | `id`, `krithiId`, `language`, `script`, `transliterationScheme`, `isPrimary`, `sampradayaId`, `variantLabel`, `sourceReference`, `lyrics`, `createdByUserId`, `updatedByUserId`, timestamps | `Krithi`, `Sampradaya`, `KrithiLyricSection` |
| `KrithiSection`     | Structural sections of a Krithi.                   | `id`, `krithiId`, `sectionType`, `orderIndex`, `label`, `notes`, timestamps                                   | `Krithi`, `KrithiLyricSection`, `KrithiNotationRow`                |
| `KrithiLyricSection` | Per-variant text per section.                    | `id`, `lyricVariantId`, `sectionId`, `text`, `normalizedText`, timestamps                                     | `KrithiLyricVariant`, `KrithiSection`         |
| `KrithiNotationVariant` | Notation variant (swara/jathi) for Varnam/Swarajathi. | `id`, `krithiId`, `notationType` (SWARA/JATHI), `talaId`, `kalai`, `eduppuOffsetBeats`, `variantLabel`, `sourceReference`, `isPrimary`, `createdByUserId`, `updatedByUserId`, timestamps | `Krithi`, `Tala`, `KrithiNotationRow` |
| `KrithiNotationRow` | Individual notation row within a section. | `id`, `notationVariantId`, `sectionId`, `orderIndex`, `swaraText`, `sahityaText`, `talaMarkers`, timestamps | `KrithiNotationVariant`, `KrithiSection` |
| `Tag`               | Controlled vocabulary element.                    | `id`, `category`, `slug`, `displayNameEn`, `descriptionEn`, `createdAt`                                       | `KrithiTag`                                   |
| `KrithiTag`         | Join between Krithi and Tag.                      | `krithiId`, `tagId`, `source`, `confidence`                                                                    | `Krithi`, `Tag`                               |
| `Sampradaya`        | Lineage / school for patantharam.                  | `id`, `name`, `type` (PATHANTARAM/BANI/SCHOOL), `description`, `createdAt`                                                               | `KrithiLyricVariant`                          |
| `ImportSource`      | Origin of imported Krithi data.                   | `id`, `name`, `baseUrl`, `description`, `contactInfo`, `createdAt`                                            | `ImportedKrithi`                              |
| `ImportedKrithi`    | Staging record for imported compositions.         | `id`, `importSourceId`, `sourceKey`, `rawTitle`, `rawLyrics`, `rawComposer`, `rawRaga`, `rawTala`, `rawDeity`, `rawTemple`, `rawLanguage`, `parsedPayload` (JSONB), `importStatus`, `mappedKrithiId`, `reviewerUserId`, `reviewerNotes`, `reviewedAt`, `createdAt` | `ImportSource`, `Krithi`, `User`             |
| `AuditLog`          | Immutable record of admin/editor actions.         | `id`, `actorUserId`, `actorIp`, `action`, `entityTable`, `entityId`, `diff` (JSONB), `metadata` (JSONB), `changedAt`         | All mutable entities                          |

Notes:
- `Krithi` is the central root for most flows (search, browse, edit).
- `KrithiLyricVariant` + `KrithiSection` + `KrithiLyricSection` allow rich
  sectioned lyrics per language/script/sampradaya.
- `KrithiNotationVariant` + `KrithiNotationRow` support notation for Varnam
  and Swarajathi compositions (swara/jathi notation).
- `Tag`/`KrithiTag` and `Sampradaya` provide thematic and lineage
  classification.
- `musicalForm` field on `Krithi` supports KRITHI, VARNAM, SWARAJATHI.

---

# 3. Shared Enums

Database enums are mirrored in KMM enums; values and semantics must
stay aligned.

| Enum (DB)          | Enum (KMM)          | Values                                           | Usage                          |
|--------------------|---------------------|--------------------------------------------------|--------------------------------|
| `workflow_state_enum` | `WorkflowStateDto` | `draft`, `in_review`, `published`, `archived`    | Editorial lifecycle of Krithis |
| `language_code_enum`  | `LanguageCodeDto` | `sa`, `ta`, `te`, `kn`, `ml`, `hi`, `en`         | Composition & variant languages|
| `script_code_enum`    | `ScriptCodeDto`   | `devanagari`, `tamil`, `telugu`, `kannada`, `malayalam`, `latin` | Scripts for lyrics/translit   |
| `raga_section_enum`   | `RagaSectionDto`  | `pallavi`, `anupallavi`, `charanam`, `other`     | Optional raga–section mapping  |
| `import_status_enum`  | `ImportStatusDto` | `pending`, `in_review`, `mapped`, `rejected`, `discarded` | Import review lifecycle     |
| `musical_form_enum`     | `MusicalFormDto`   | `KRITHI`, `VARNAM`, `SWARAJATHI`                 | Musical form classification     |

Mapping rules:

- DB values are lowercase; KMM enums are uppercase equivalents
  (e.g. `draft` → `DRAFT`).
- Serialization uses `kotlinx.serialization` with string values matching
  the DB enums.

---

# 4. Relationships

High-level relationships between core entities:

## 4.1 Krithi & Reference Data

- `Krithi` → `Composer`: **many-to-one** via `composerId`.
- `Krithi` → `Raga` / `KrithiRaga`:
  - `primaryRagaId` for the main raga (non-ragamalika).
  - `KrithiRaga` rows for ragamalika and richer raga mapping.
- `Krithi` → `Tala`: optional `talaId`.
- `Krithi` → `Deity` / `Temple`: optional associations.

## 4.2 Lyric Variants & Sections

- `Krithi` → `KrithiLyricVariant`: **one-to-many**.
  - Variants differ by language, script, sampradaya, or source.
  - At most one primary variant per `(krithiId, language, script)`.
- `Krithi` → `KrithiSection`: **one-to-many**.
  - Sections define canonical structure (pallavi, anupallavi,
    charanam N, chittaswaram, swara-sahitya, madhyama-kala, other).
- `KrithiLyricVariant` → `KrithiLyricSection`: **one-to-many**.
  - Each lyric variant may have text per section.
- `KrithiSection` → `KrithiLyricSection`:
  - Links structure to per-variant text.

## 4.2.1 Notation Variants & Rows

- `Krithi` → `KrithiNotationVariant`: **one-to-many**.
  - Supports multiple notation variants (e.g., different pathantharams).
  - Notation types: SWARA (swara notation) or JATHI (jathi notation).
  - Includes tala, kalai, and eduppu metadata.
- `KrithiNotationVariant` → `KrithiNotationRow`: **one-to-many**.
  - Individual rows of notation organized by section and order.
  - Each row contains swara text, optional sahitya, and tala markers.
- `KrithiSection` → `KrithiNotationRow`:
  - Links notation rows to structural sections.

## 4.3 Tags & Sampradaya

- `Krithi` ↔ `Tag`: **many-to-many** via `KrithiTag`.
  - Tag categories: BHAVA, FESTIVAL, PHILOSOPHY, KSHETRA, STOTRA_STYLE, NAYIKA_BHAVA, OTHER.
- `KrithiLyricVariant` → `Sampradaya`:
  - Optional `sampradayaId` plus free-form `variantLabel`.
  - Sampradaya types: PATHANTARAM, BANI, SCHOOL.

## 4.4 Temples & Names

- `Temple` → `TempleName`: **one-to-many**.
  - Supports multilingual and alias naming.
  - `TempleName.isPrimary` marks canonical name per language/script.

## 4.5 Import Pipeline

- `ImportSource` → `ImportedKrithi`: **one-to-many**.
- `ImportedKrithi` → `Krithi`:
  - Optional `mappedKrithiId` once canonicalized.
- `ImportedKrithi` → `User`:
  - `reviewerUserId` for mapping/rejection decisions.

## 4.6 Identity & Audit

- `User` ↔ `Role` ↔ `RoleAssignment`:
  - Standard RBAC for admin/editor/reviewer.
- `AuditLog` → all mutating entities:
  - `entityTable` + `entityId` identify the affected row.
  - `action` and `diff` capture what changed.

These relationships should be reflected consistently in:

- ERD (`../diagrams/erd.md`).
- KMM models (`modules/shared/domain`).
- Exposed table definitions and repositories
  (`modules/backend/dal`).

---

# 5. Alignment Notes

- The **database schema** is defined in migrations under
  `database/migrations/` and documented in
  `database/SANGITA_SCHEMA_OVERVIEW.md`.
- The **shared DTOs** in `modules/shared/domain` (e.g. `KrithiDto`,
  `KrithiLyricVariantDto`, `TagDto`) form the **public and admin API
  contracts**.
- No Exposed types or internal DAL models should leak across the API
  boundary.

Technical constraints:

- Date/time: `kotlinx.datetime.Instant` in KMM, `TIMESTAMPTZ`/`DATE` in
  Postgres; avoid Java legacy date/time classes.
- IDs: `kotlin.uuid.Uuid` serialized as strings; `UUID` columns in DB.
- Enums: DB + KMM enums must remain in sync; changes require:
  1. Migration.
  2. DTO/enum updates.
  3. Documentation updates.

Process:

- Any change to core entities or relationships must update:
  - This file.
  - ERD (`../diagrams/erd.md`).
  - API specs (`../api/*.md`).
  - Root PRD, if behaviourally significant.

---

# 6. Musicological Correctness Rules (Lakshana)

These rules encode the musical-domain correctness (*lakshana*) that **all**
surfaces — data entry, extraction/enrichment, validation, and generated
SQL/seed data — must respect. They complement the structural model above:
the schema defines what *can* be stored; these rules define what is
*musically valid*. Prefer musicological accuracy over convenience — an
incorrect raga scale or tala anga is a data-quality defect, not a cosmetic one.

## 6.1 Musical Forms

`musical_form_enum` distinguishes `KRITHI`, `VARNAM`, and `SWARAJATHI`; each
has different structural requirements, so validation must not treat one
form's sections as another's.

- **Krithi**: Pallavi → Anupallavi (optional) → one or more Charanams. May
  also carry Chittaswaram, Swara-sahitya, or Madhyamakala sahitya.
- **Varnam**: requires Pallavi, Anupallavi, **Muktayi Swaram**, the
  Charanam (pallavi-of-charanam) line, and a set of Chittaswarams (Ettugada
  swaras). Swara and sahitya are positionally aligned — model the notation
  via `KrithiNotationVariant` / `KrithiNotationRow`.
- **Swarajathi**: Pallavi, Anupallavi (optional), and Charanams built on
  swara passages.

## 6.2 Ragamalika

- A Ragamalika changes raga across sections. Represent the ordered raga
  sequence via `KrithiRaga` rows (`orderIndex`, optional `section`) and set
  `Krithi.isRagamalika = true`.
- A single `primaryRagaId` is insufficient for a ragamalika — never collapse
  a ragamalika down to one raga.

## 6.3 Notation vs. Lyrics

- Swara notation is modeled **independently** of lyrics
  (`KrithiNotationVariant` / `KrithiNotationRow`) **except where explicitly
  aligned** — as in Varnams, where swara and sahitya are positionally tied.
- Keep `notationType` (SWARA/JATHI) and tala/kalai/eduppu metadata on the
  notation variant; do not infer them from lyric text.

## 6.4 Raga, Tala & Terminology

- **Ragas**: respect melakarta/janya relationships (`melakartaNumber`,
  `parentRagaId`, `arohanam`/`avarohanam`). Any generated scale must be valid
  for the named raga.
- **Talas**: respect anga structure and beat count (`angaStructure`,
  `beatCount`); generated tala data must have correct angas.
- **Terminology**: use correct Sanskrit/Tamil section terms — Pallavi,
  Anupallavi, Charanam, Chittaswaram, Muktayi Swaram, Madhyamakala.

---

# 7. Open Questions

- **Section Granularity**: Do we need line-level metadata (e.g. for
  mapping specific sahitya phrases to ragas in ragamalika), or are
  section-level blocks sufficient?
- **Tag Taxonomy**: Is the current category set (BHAVA, FESTIVAL,
  PHILOSOPHY, KSHETRA, STOTRA_STYLE, NAYIKA_BHAVA, OTHER) adequate, or
  do we need a more granular ontology?
- **Sampradaya Modelling**: Should we model hierarchical relationships
  between sampradayas (e.g. school → sub-school), or keep a flat list
  and use `variantLabel` for finer distinctions?
- **Temple Aliases**: Do we need additional structure for historic names
  and multilingual display preferences beyond `TempleName.isPrimary`?

These should be resolved before significantly expanding the public API
surface or ingesting very large, diverse data sets.