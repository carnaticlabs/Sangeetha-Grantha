| Metadata | Value |
|:---|:---|
| **Track ID** | TRACK-056 |
| **Title** | Language Variant Backend — Matching & Enrichment API |
| **Status** | Completed |
| **Priority** | High |
| **Created** | 2026-02-09 |
| **Updated** | 2026-02-10 |
| **Depends On** | TRACK-055 |
| **Spec Ref** | application_documentation/01-requirements/krithi-data-sourcing/pdf-diacritic-extraction-analysis.md |
| **Est. Effort** | 4–5 days |

# TRACK-056: Language Variant Backend — Matching & Enrichment API

## Objective

Backend support for the "enrich" extraction intent: when a user submits a second PDF (e.g. Sanskrit) as a language variant of an existing extraction (e.g. English), the system must store the intent and related extraction, match each extracted Krithi to the related extraction's Krithis, auto-persist HIGH-confidence matches (≥0.85), and expose a variant-match review API for MEDIUM/LOW and anomalies. Lyric variants are persisted for approved matches; anomalies and structural mismatches are flagged for user decision.

## Scope

- Database migration: add `content_language`, `extraction_intent`, `related_extraction_id` to extraction_queue.
- Kotlin: ExtractionQueueRepository/DTOs, Sourcing API (POST /extractions with new fields), VariantMatchingService (multi-signal matching, confidence, anomaly/structure flags), ExtractionResultProcessor branch for intent=ENRICH, lyric variant persistence, GET/POST variant-match endpoints.
- Transliteration: use IAST from extraction payload when present, or evaluate Kotlin vs Python per Design Decision 5; document choice.

## Design Decisions (from Spec Section 10.8)

| Decision | Choice |
|:---|:---|
| HIGH matches (≥0.85) | Auto-persist without user review; user can review post-hoc. |
| Variant extraction finds Krithi NOT in related extraction | Flag as ANOMALY for user to decide (link/create/skip). |
| Structural mismatches (e.g. 3 vs 4 sections) | Align by section type; set structureMismatch when order/counts differ. |
| Transliteration Devanagari↔IAST | Prefer pre-compute in Python (payload); or Kotlin/Python service per evaluation. |

## Task List

| Task ID | Description | Acceptance Criteria | File(s) |
|:---|:---|:---|:---|
| T56.1 | Migration: add content_language, extraction_intent, related_extraction_id to extraction_queue | Columns added with defaults; CHECK on extraction_intent. | `database/migrations/29__extraction_variant_support.sql` (new) |
| T56.2 | Update ExtractionQueueRepository and DTOs for new columns | Create/read/write new fields. | DAL + shared domain |
| T56.3 | Sourcing API: accept new fields in POST /extractions | Request body accepts contentLanguage, extractionIntent, relatedExtractionId. | `SourcingRoutes.kt`, `CreateExtractionRequest` (frontend types) |
| T56.4 | Implement VariantMatchingService — multi-signal matching | matchToExistingKrithis() returns VariantMatchReport with confidence and signals; ≥0.85 = HIGH. | `services/VariantMatchingService.kt` (new) |
| T56.5 | Devanagari↔IAST for matching | Title matching uses IAST from extraction payload (if present) or Kotlin/Python transliteration per evaluation. | TransliterationService or use payload field |
| T56.6 | ExtractionResultProcessor: branch for intent=ENRICH | When intent=enrich, run variant matching; auto-persist HIGH; store MEDIUM/LOW for review. | `ExtractionResultProcessor.kt` |
| T56.7 | Persist lyric variants for approved matches | Create krithi_lyric_variants + krithi_lyric_sections; link source evidence. | VariantMatchingService / existing lyric persistence |
| T56.8 | API: GET extraction/{id}/variant-matches, POST approve/override/skip | Endpoints implemented and secured. | `SourcingRoutes.kt` |
| T56.9 | Flag anomalies and structure mismatches | Matches outside related extraction scope flagged ANOMALY; section count/order diff set structureMismatch. | VariantMatchingService |
| T56.10 | Wire services in Koin; integration test | Enrichment extraction produces match report and HIGH matches create variants. | `AppModule.kt`, test |

## Files Changed

| File | Change |
|:---|:---|
| `database/migrations/29__extraction_variant_support.sql` | New migration |
| `modules/backend/dal` | ExtractionQueueRepository, tables, DTOs |
| `modules/shared/domain` | DTOs for extraction intent, variant match report |
| `modules/backend/api/.../routes/SourcingRoutes.kt` | POST body, GET variant-matches, POST approve/override/skip |
| `modules/backend/api/.../services/VariantMatchingService.kt` | New — matching, confidence, anomaly/structure flags, lyric persistence |
| `modules/backend/api/.../services/ExtractionResultProcessor.kt` | Branch for intent=ENRICH |
| `modules/backend/api/.../di/AppModule.kt` | Wire VariantMatchingService |

## Progress Log

| Date | Unit | Notes |
|:---|:---|:---|
| 2026-02-10 | T56.1 | Migration `29__extraction_variant_support.sql` created — adds `content_language`, `extraction_intent`, `related_extraction_id` to `extraction_queue`; creates `variant_match` table with confidence, signals, status, anomaly/structure flags. |
| 2026-02-10 | T56.2–T56.3 | `ExtractionQueueRepository`, `VariantMatchTable`, `VariantMatchRepository` updated/created. `SourcingRoutes` and `SourcingService` accept new fields. DTOs (`VariantMatchDto`, `VariantMatchReportDto`, `VariantMatchReviewRequestDto`) added to shared domain. |
| 2026-02-10 | T56.4–T56.5 | `VariantMatchingService` created with multi-signal matching. Verified: matches Devanagari via IAST `alternateTitle` fallback + NFD normalization (fixed in TRACK-058). |
| 2026-02-10 | T56.6–T56.7 | `ExtractionResultProcessor` branches on `intent=ENRICH`; HIGH-confidence matches (≥0.85) auto-approved. Lyric variants persisted via existing `KrithiLyricVariantRepository`. |
| 2026-02-10 | T56.8–T56.10 | Variant match API endpoints (GET pending, GET by extraction, GET report, POST review). Anomaly and structure mismatch flags set during matching. All services wired in Koin `AppModule`. Backend compiles. |
