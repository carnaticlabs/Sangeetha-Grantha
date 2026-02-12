| Metadata | Value |
|:---|:---|
| **Track ID** | TRACK-063 |
| **Title** | Database Reset & Full Re-Extraction E2E Validation |
| **Status** | Planned |
| **Priority** | Critical |
| **Created** | 2026-02-10 |
| **Updated** | 2026-02-10 |
| **Depends On** | TRACK-059, TRACK-060, TRACK-061, TRACK-062 |
| **Spec Ref** | analysis-extraction-pipeline-failures.md (all findings) |
| **Est. Effort** | 1–2 days |

# TRACK-063: Database Reset & Full Re-Extraction E2E Validation

## Objective

After all code fixes from TRACK-059 through TRACK-062 are in place, reset the
database and re-run the three extraction/import sources end-to-end. Verify that
the pipeline produces correct, deduplicated, variant-linked results.

This track validates the entire chain: PDF extraction → diacritic normalisation →
page segmentation → title normalisation → deduplication → Krithi creation →
source evidence → variant matching → lyric variant persistence.

## Scope

- Database reset via `sangita-cli db reset`.
- Re-submit mdeng.pdf as PRIMARY extraction.
- Re-submit mdskt.pdf as ENRICH extraction.
- Re-run blogspot bulk import.
- Verify all acceptance criteria via SQL queries and spot checks.

## Acceptance Criteria (quantitative)

| Metric | Target | Previous (broken) |
|:---|:---|:---|
| Total Krithis (unique) | ~484 (Dikshitar corpus) + blogspot-only | 707 (with 32 dupes) |
| Duplicate Krithis | 0 | 32 |
| Composer records for Dikshitar | 1 | 2 |
| mdeng.pdf titles with garbled diacritics | 0 | 480 |
| mdeng.pdf Krithis with "Unknown" raga | ≤ 5 | ~480 |
| mdskt.pdf extraction result count | ~484 (±10) | 1 |
| Variant matches (variant_match table) | ≥ 450 (of ~484) | 0 |
| Source evidence records (mdeng.pdf) | ~484 | 480 |
| Source evidence records (blogspot) | ~364 (blogspot batch size) | 0 |
| Source evidence records (mdskt.pdf) | ~484 (via variant match) | 0 |
| Blogspot duplicate Krithis | 0 | 32 |
| Cross-source title matches (mdeng ↔ blogspot) | ≥ 200 | 14 |

## Submission Order

The recommended submission order is:

1. **Blogspot bulk import first** — This creates Krithis with clean titles
   (Harvard-Kyoto transliteration is well-formed).
2. **mdeng.pdf PRIMARY** — With TRACK-059 diacritic fix, titles will be clean IAST.
   With TRACK-061 transliteration-aware normalisation, these should match blogspot
   Krithis via `findDuplicateCandidates`. Unmatched compositions create new Krithis.
3. **mdskt.pdf ENRICH** — With TRACK-060 segmentation fix, this produces ~484
   results. `VariantMatchingService` matches them to existing Krithis and creates
   Devanagari lyric variants.

Alternative: mdeng.pdf first, then blogspot, then mdskt.pdf. Either order should
work with the dedup fixes in place.

## Task List

| Task ID | Description | Acceptance Criteria | File(s) |
|:---|:---|:---|:---|
| T63.1 | Database reset | `sangita-cli db reset` succeeds. All tables empty except seed data. | CLI |
| T63.2 | Run blogspot bulk import | Batch completes. ~364 Krithis created with source evidence records. 0 duplicates. | Admin UI / API |
| T63.3 | Submit mdeng.pdf PRIMARY extraction | Extraction queue picks it up. Python extractor produces ~484 results with clean IAST titles. | API |
| T63.4 | Verify mdeng.pdf ingestion | ExtractionResultProcessor matches existing blogspot Krithis where possible, creates new for remainder. Source evidence written for all. 1 composer record for Dikshitar. | SQL queries |
| T63.5 | Submit mdskt.pdf ENRICH extraction | Extraction queue processes it. Python extractor segments into ~484 compositions (not 1). | API |
| T63.6 | Verify variant matching | `variant_match` table has ≥ 450 rows. HIGH confidence auto-approved. Devanagari lyric variants visible in Krithi editor. | SQL queries + UI |
| T63.7 | Spot-check 20 Krithis | For 20 randomly selected Krithis: title is clean IAST, raga/tala correct, sections complete, source evidence from all relevant sources, lyric variants in both English and Sanskrit present. | Manual |
| T63.8 | Performance check | Variant matching for 484 Krithis completes in under 60 seconds. Full mdeng.pdf extraction completes in under 10 minutes. | Timing |
| T63.9 | Update conductor tracks | All tracks 059–063 marked complete. tracks.md updated. | conductor/ |

## SQL Verification Queries

```sql
-- Q1: Total Krithis and duplicates
SELECT COUNT(*) as total, COUNT(DISTINCT title_normalized) as unique_titles
FROM krithis;

-- Q2: Composer dedup
SELECT name, name_normalized, COUNT(*)
FROM composers WHERE name_normalized LIKE '%dikshit%'
GROUP BY name, name_normalized;

-- Q3: Garbled titles remaining
SELECT COUNT(*) FROM krithis WHERE title LIKE '%¯%' OR title LIKE '%˙%' OR title LIKE '%´%';

-- Q4: Source evidence per source
SELECT
  CASE
    WHEN source_url LIKE '%mdeng%' THEN 'mdeng.pdf'
    WHEN source_url LIKE '%mdskt%' THEN 'mdskt.pdf'
    WHEN source_url LIKE '%blogspot%' THEN 'blogspot'
    ELSE 'other'
  END as source,
  COUNT(*)
FROM krithi_source_evidence
GROUP BY 1;

-- Q5: Variant match summary
SELECT confidence_tier, match_status, COUNT(*)
FROM variant_match
GROUP BY 1, 2;

-- Q6: Krithis with both English and Sanskrit lyric variants
SELECT COUNT(DISTINCT k.id)
FROM krithis k
JOIN krithi_lyric_variants v1 ON k.id = v1.krithi_id AND v1.language = 'en'
JOIN krithi_lyric_variants v2 ON k.id = v2.krithi_id AND v2.language = 'sa';
```

## Files Changed

| File | Change |
|:---|:---|
| `conductor/tracks.md` | Register TRACK-059 through TRACK-063; update statuses |
| `conductor/tracks/TRACK-059..063` | Progress log entries |

## Progress Log

| Date | Unit | Notes |
|:---|:---|:---|
| 2026-02-10 | Planning | Track created from analysis-extraction-pipeline-failures.md |
