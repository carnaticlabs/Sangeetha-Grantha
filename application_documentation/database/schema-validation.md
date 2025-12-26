---
title: Schema & API Alignment (Sangita Grantha)
status: Draft
version: 0.1
last_updated: 2025-12-21
owners:
  - Sangita Grantha Backend Team
related_docs:
  - ./SANGITA_SCHEMA_OVERVIEW.md
  - ../api/api-contract.md
  - ../requirements/domain-model.md
---

# Schema & API Alignment

This document summarizes how the **database schema** and **public/admin
APIs** align for Sangita Grantha.

For full schema details, see `SANGITA_SCHEMA_OVERVIEW.md`. For full API
contracts, see `../api/api-contract.md`.

---

## 1. Krithi Catalog

- `krithis` ↔ `KrithiDto`
  - All key fields (title, incipit, composerId, primaryRagaId, talaId,
    deityId, templeId, primaryLanguage, isRagamalika, workflowState,
    timestamps) are one-to-one.

- `krithi_ragas` ↔ `KrithiRagaDto`
  - Order index and optional section mapping supported.

- `krithi_lyric_variants` ↔ `KrithiLyricVariantDto`
  - language/script enums, sampradaya, variant label, source reference,
    `isPrimary`, full lyrics.

- `krithi_sections` + `krithi_lyric_sections` ↔ section-level models.

APIs:
- `GET /v1/krithis/search`
- `GET /v1/krithis/{id}`
- `GET /v1/admin/krithis[...]`

---

## 2. Reference Data

- `composers`, `ragas`, `talas`, `deities`, `temples`, `temple_names`
  ↔ `*Dto` types.
- API may expose:
  - `/v1/composers`, `/v1/ragas`, etc., for public/mobile.
  - `/v1/admin/...` variants for admin lookup forms.

---

## 3. Tags & Sampradayas

- `tags` / `krithi_tags` ↔ `TagDto`, `KrithiTagDto`.
- `sampradayas` ↔ `SampradayaDto`, referenced from lyrics.

APIs (admin):
- `POST /v1/admin/tags`, `PUT /v1/admin/tags/{id}`.
- `POST /v1/admin/krithis/{id}/tags`.

---

## 4. Import Pipeline

- `import_sources` ↔ `ImportSourceDto`.
- `imported_krithis` ↔ `ImportedKrithiDto`.

APIs (admin):
- `GET /v1/admin/imports/krithis[...]`.
- `POST /v1/admin/imports/krithis/{id}/map`.
- `POST /v1/admin/imports/krithis/{id}/reject`.

---

## 5. Gaps & Future Work

- Any new APIs (audio, notation, user accounts) will require:
  - New tables and/or columns.
  - Updated DTOs.
  - Updated documentation here and in the domain model.

This file should be updated whenever the schema or API surface change
in non-trivial ways.
