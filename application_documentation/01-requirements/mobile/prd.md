# Sangita Grantha Public Mobile App PRD

> **Status**: Draft | **Version**: 0.1 | **Last Updated**: 2025-12-21
> **Owners**: Sangita Grantha Product Team

**Related Documents**
- [Mobile Ui](../../05-frontend/mobile/ui-specs.md)
- [Api Contract](../../03-api/api-contract.md)
- [Sangita_Schema_Overview](../../04-database/schema.md)
- [Domain Model](../domain-model.md)

# 1. Executive Summary

The **Sangita Grantha Public Mobile App** is the primary interface for
rasikas, students, and performers to explore the canonical catalog of
Carnatic Krithis curated by the Sangita Grantha platform.

The admin web application handles data curation. The mobile app focuses
on **read-only discovery**:

- Search by Krithi name, first line, composer, raga, tala, deity, temple.
- View lyrics in multiple scripts and languages.
- Understand basic metadata and theming (tags, festivals, bhava).
- Optionally bookmark favourites and cache for offline access.

---

# 2. Product Overview

## 2.1 Problem

- Rasikas and students often rely on unstructured, inconsistent online
  sources or scans for Krithi texts and metadata.
- Searching by **lyrics fragment** is difficult or impossible in most
  current resources.
- There is no unified way to see multiple variants (patantharams,
  sampradayas, scripts) of a Krithi.

## 2.2 Solution

Provide a **Kotlin Multiplatform (KMM)** mobile app with Compose
Multiplatform UI that integrates with the shared Sangita Grantha backend.
The app surfaces:

- Fast, multi-field search over the curated Krithi catalog.
- Clean Krithi detail screens with metadata and multiple lyric variants.
- Basic theming (tags, festivals, bhava) to support repertoire selection.
- Offline-friendly access to recently viewed and favourited Krithis.

## 2.3 Target Users

- **Primary:** Carnatic rasikas and learners.
- **Secondary:** Performers and teachers using the app as a reference.

---

# 3. Core Features (v1)

## 3.1 Search & Browse

- Search box with support for:
  - Krithi title / incipit.
  - Lyrics substring (uses backend trigram search on lyrics).
  - Composer, raga, tala, deity, temple.
- Browse by facets:
  - Composer list (A–Z).
  - Raga list (melakarta vs. janya, optional).
  - Deity / temple.
  - Tags (e.g. **Navaratri**, **Bhakti**, **Advaita**), if available.

Results show:
- Krithi title and incipit.
- Composer, primary raga, tala.
- Deity/temple (if available).
- Badges for language(s) and sampradaya(s) when present.


## 3.2 Krithi Detail

Detail screen for a single Krithi:

- Header:
  - Title and incipit.
  - Composer, raga(s), tala.
  - Deity and temple.
  - Tags and theme badges.
- Metadata section:
  - Primary language of composition.
  - Workflow state (only `PUBLISHED` shown to the public).
  - Short sahitya summary where available.
- Lyric variants:
  - Tabs or dropdown for language/script (e.g. Tamil script, Devanagari,
    Latin transliteration).
  - Within a variant, display text organized by sections (pallavi,
    anupallavi, charanams, etc.).
  - Indicate sampradaya/variant label and source reference.

Non-goals (v1):
- Audio playback or notation rendering.
- In-app annotations or user-added notes.


## 3.3 Favourites & Offline Access

- Allow users to **favourite** Krithis.
- Cache:
  - Recently viewed Krithis.
  - Favourited Krithis.
- Provide basic offline behaviour:
  - When offline, users can still open cached Krithi details and lyrics.
  - Search indicates offline state and may limit to cached items.


## 3.4 Minimal Settings

- Theme (light/dark, if adopted from shared design).
- Language preferences for UI (initially English; extended later).
- Feedback link or contact channel for reporting data issues (which
  ultimately route to editors via backend tooling).

---

# 4. User Roles & Permissions

The public app is primarily **read-only**.

| Role        | Capabilities                                                     |
|-------------|------------------------------------------------------------------|
| Public User | Search, browse, view Krithis and lyrics, manage local favourites |

Authentication is **not required** for v1. If later features need user
accounts (e.g. sync favourites across devices), they will be added in a
separate iteration and documented here.

---

# 5. Technical Requirements

- **Platforms:** Android and iOS via Kotlin Multiplatform + Compose
  Multiplatform.
- **Networking:** Ktor HTTP client using the shared `KrithiDto` and
  related DTOs from `modules/shared/domain`.
- **Offline support:** Local persistence of cached Krithis and favourites.
- **Security:** All backend calls over HTTPS; no sensitive personal data
  is stored or transmitted in v1.
- **Observability:** Basic client logging and crash reporting to support
  stability monitoring.

---

# 6. UI / UX Requirements

- Simple primary navigation:
  - Search
  - Browse (by composer, raga, deity, tags)
  - Favourites
- Typography and layout optimised for lyric readability.
- Consistent display of script and transliteration; avoid mixing
  multiple scripts in a confusing way.
- Clear indicators when multiple variants are available,
  and which variant is primary.

---

# 7. Success Metrics

| Metric                              | Target |
|-------------------------------------|--------|
| Monthly active users                | Measured & improving |
| Search success (no-result rate)     | < 10% on typical queries |
| App crash rate                      | < 0.1% of sessions |
| Average time spent per session      | Indicates engagement with content |

---

# 8. Risks & Mitigation

- **Data gaps in early catalog:** Clearly label coverage as partial;
  prioritise import/curation for high-demand composers and ragas.
- **Script rendering issues:** Test extensively on common device fonts
  for Indian scripts; provide fallbacks where necessary.
- **Evolving schema:** Maintain strict DTO versioning and backward
  compatible APIs as the data model evolves.

---

# 9. Implementation Status

This PRD describes the **target v1 behaviour**. Implementation status
for mobile clients will be tracked in the main project board; shared
KMM models already exist, but no dedicated Sangita Grantha mobile app
has been implemented yet.
