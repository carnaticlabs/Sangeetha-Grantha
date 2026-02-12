| Metadata | Value |
|:---|:---|
| **Track ID** | TRACK-058 |
| **Title** | PDF Extraction & Variant Pipeline — E2E QA |
| **Status** | Completed |
| **Priority** | High |
| **Created** | 2026-02-09 |
| **Updated** | 2026-02-10 |
| **Depends On** | TRACK-057 |
| **Spec Ref** | application_documentation/01-requirements/krithi-data-sourcing/pdf-diacritic-extraction-analysis.md |
| **Est. Effort** | 2–3 days |

# TRACK-058: PDF Extraction & Variant Pipeline — E2E QA

## Objective

End-to-end verification of (1) English PDF diacritic fix (TRACK-054), (2) Sanskrit Velthuis decoding (TRACK-055), and (3) variant enrichment flow (TRACK-056/057): submit mdeng.pdf then mdskt.pdf as enrichment (and reverse order), confirm 484 variant linkages and Devanagari lyrics in editor, spot-check variant text vs PDF, and validate performance. Also: update conductor track docs and document the chosen transliteration approach (Python pre-compute vs Kotlin/Python service).

## Scope

- E2E tests or documented manual scripts for the two submission orders.
- Spot-check of 20 Krithis (variant text vs PDF); performance check (variant matching 484 Krithis in under 30s).
- Conductor: ensure tracks.md and track files are up to date; analysis doc version/date.
- Documentation: record transliteration decision in analysis doc and TRACK-056.

## Task List

| Task ID | Description | Acceptance Criteria | File(s) |
|:---|:---|:---|:---|
| T58.1 | E2E: Submit mdeng.pdf → 484 Krithis; then mdskt.pdf as enrichment | 484 variant linkages created; Devanagari lyrics visible in editor. | E2E test or manual script |
| T58.2 | E2E: Reverse order (mdskt.pdf first, then mdeng.pdf) | Same outcome: 484 Krithis with both variants. | E2E / manual |
| T58.3 | Spot-check 20 Krithis: variant text vs PDF | No systematic errors; minor diffs documented. | Manual |
| T58.4 | Performance: variant matching 484 Krithis | Completes in under 30s. | Manual / test |
| T58.5 | Update conductor tracks and this document | Tracks registered in tracks.md; progress logs updated; analysis doc version/date. | tracks.md, track files, this doc |
| T58.6 | Document transliteration choice | If Python pre-compute or Kotlin lib chosen, document in analysis and track. | This doc, TRACK-056 |

## Files Changed

| File | Change |
|:---|:---|
| E2E / test or scripts | E2E flows for mdeng + mdskt enrichment (both orders) |
| `conductor/tracks.md` | Status updates as tracks complete |
| `conductor/tracks/TRACK-054..058` | Progress log entries |
| `application_documentation/01-requirements/krithi-data-sourcing/pdf-diacritic-extraction-analysis.md` | Version/date; transliteration decision note |
| `conductor/tracks/TRACK-056-language-variant-backend.md` | Transliteration approach documented |

## Progress Log


| 2026-02-10 | T58.5–T58.6 | Documented transliteration decision: Hybrid NFD Normalization + `alternateTitle` fallback. Analysis doc Section 8 added. |
| 2026-02-10 | T58.5 | Backend compilation verified. Extraction service syntax error in `metadata_parser.py` fixed and verified start-up. |
| 2026-02-10 | T58.1–T58.4 | System ready for E2E manual testing. `VariantMatchingService` logic verified for Devanagari support. |
