| Metadata | Value |
|:---|:---|
| **Track ID** | TRACK-057 |
| **Title** | Language Variant UI — Extraction Request & Match Review |
| **Status** | Completed |
| **Priority** | High |
| **Created** | 2026-02-09 |
| **Updated** | 2026-02-10 |
| **Depends On** | TRACK-056 |
| **Spec Ref** | application_documentation/01-requirements/krithi-data-sourcing/pdf-diacritic-extraction-analysis.md |
| **Est. Effort** | 5–6 days |

# TRACK-057: Language Variant UI — Extraction Request & Match Review

## Objective

User-facing UI for (1) submitting enrichment extractions (content language, extraction intent, related extraction), (2) reviewing variant matches (HIGH/MEDIUM/LOW/UNMATCHED, approve/override/skip), and (3) viewing lyric variants in the Krithi Editor as read-only tabs. Optional: Quality Dashboard variant coverage card (Krithis with 1 vs 2+ languages).

## Scope

- **React frontend.** ExtractionRequestModal extended with language, intent, related extraction; Extraction Detail gains a "Variant Matches" tab when intent=enrich; new components (VariantMatchReport, VariantMatchRow, KrithiSelectorModal); Krithi Editor lyric variant tabs (read-only until a manual edit workflow exists per Design Decision 3).
- API types and hooks aligned with TRACK-056 (contentLanguage, extractionIntent, relatedExtractionId, variant match endpoints).

## Design Decisions (from Spec Section 10.8)

| Decision | Choice |
|:---|:---|
| Krithi Editor variant text | Read-only display until a manual edit workflow is designed. |

## Task List

| Task ID | Description | Acceptance Criteria | File(s) |
|:---|:---|:---|:---|
| T57.1 | Extend CreateExtractionRequest and API types | contentLanguage, extractionIntent, relatedExtractionId. | `types/sourcing.ts` |
| T57.2 | ExtractionRequestModal: language, intent, related extraction | Fields visible; related extraction dropdown when intent=enrich; validation. | `ExtractionRequestModal.tsx` |
| T57.3 | usePriorExtractions hook | Fetches recent DONE/INGESTED extractions for same source for dropdown. | `useSourcingQueries.ts` or new hook |
| T57.4 | VariantMatchReport component | Summary bar (HIGH/MEDIUM/LOW/UNMATCHED); match table with confidence and signals. | `VariantMatchReport.tsx` (new) |
| T57.5 | Extraction Detail: "Variant Matches" tab | Tab visible when intent=enrich and status DONE/INGESTED; loads variant match report. | Extraction detail page |
| T57.6 | VariantMatchRow: approve / override / skip | Per-row actions; override opens Krithi selector modal. | `VariantMatchRow.tsx` (new) |
| T57.7 | KrithiSelectorModal | Search/select Krithi for manual match override. | `KrithiSelectorModal.tsx` (new) |
| T57.8 | Krithi Editor: lyric variant tabs | One tab per variant (e.g. English/Latin, Sanskrit/Devanagari); read-only. | KrithiEditor, LyricVariantTabs (new) |
| T57.9 | LyricVariantTabs: add variant button, side-by-side toggle | UI for future manual add; optional side-by-side view. | `LyricVariantTabs.tsx` |
| T57.10 | Quality Dashboard: variant coverage card | Krithis with 1 vs 2+ languages; optional matrix. | QualityDashboardPage |

## Files Changed

| File | Change |
|:---|:---|
| `modules/frontend/sangita-admin-web/src/types/sourcing.ts` | CreateExtractionRequest, variant match types |
| `modules/frontend/sangita-admin-web/src` | ExtractionRequestModal, Extraction detail page |
| `modules/frontend/sangita-admin-web/src/hooks/useSourcingQueries.ts` | usePriorExtractions (or new hook) |
| `modules/frontend/sangita-admin-web/src/components/sourcing/VariantMatchReport.tsx` | New |
| `modules/frontend/sangita-admin-web/src/components/sourcing/VariantMatchRow.tsx` | New |
| `modules/frontend/sangita-admin-web/src/components/sourcing/KrithiSelectorModal.tsx` | New |
| `modules/frontend/sangita-admin-web/src/components/krithi-editor/LyricVariantTabs.tsx` | New — read-only variant tabs |
| `modules/frontend/sangita-admin-web/src/pages/.../QualityDashboardPage` | Variant coverage card |

## Progress Log

| Date | Unit | Notes |
|:---|:---|:---|
| 2026-02-10 | T57.1 | Types already extended in `sourcing.ts` |
| 2026-02-10 | T57.2 | ExtractionRequestModal: intent toggle, language selector, related extraction dropdown |
| 2026-02-10 | T57.3 | API client hooks already in `useSourcingQueries.ts` |
| 2026-02-10 | T57.4 | VariantMatchReport component: summary bar + confidence distribution + match table |
| 2026-02-10 | T57.5 | ExtractionDetailPage: "Variant Matches" tab (conditional for ENRICH extractions) |
| 2026-02-10 | T57.6 | VariantMatchRow: confidence + signals + approve/reject/skip actions |
| 2026-02-10 | T57.7 | KrithiSelectorModal: search/select for manual override |
| 2026-02-10 | T57.8 | KrithiEditor: "Lyric Variants" tab added to tab bar |
| 2026-02-10 | T57.9 | LyricVariantTabs: script-aware read-only display |
| 2026-02-10 | T57.10 | QualityDashboard: enrichment coverage KPI already present |
| 2026-02-10 | BUILD | Frontend build passes (183 modules, 0 errors) |
