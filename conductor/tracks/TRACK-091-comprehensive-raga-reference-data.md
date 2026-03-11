| Metadata | Value |
|:---|:---|
| **Status** | Completed |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-03-10 |
| **Author** | Sangeetha Grantha Team |

# Goal

Build a comprehensive raga reference dataset covering all 72 melakarta ragas and their janya ragas, sourced from Wikipedia ("List of Janya ragas") and karnatik.com. This replaces the current 3-raga seed data with a production-quality reference set that serves as the canonical raga authority for all existing and new krithis.

# Sources

- https://en.wikipedia.org/wiki/List_of_Janya_ragas — structured tables with melakarta-to-janya mappings, arohanam, avarohanam
- https://www.karnatik.com/ragas.shtml — 1000+ raga names with canonical spellings and scale data

# Implementation Summary

## Python Extraction Script (`tools/raga-reference-extractor/`)
- `extract_ragas.py` — scrapes Wikipedia janya raga tables, cross-references karnatik.com
- Computes 72 melakarta scales via Katapayadi formula
- Merges melakartas by number (not name) to handle spelling variations
- Normalizes swara notation (Unicode subscripts → numbered swarasthanas)
- Flags: `--skip-wikipedia`, `--skip-karnatik`, `--output`

## Curated SQL Seed File (`database/seed_data/05_raga_reference_data.sql`)
- 72 melakarta ragas with melakarta_number 1–72, full arohanam/avarohanam
- 889 janya ragas with parent_raga_id referencing parent melakarta
- Idempotent: `ON CONFLICT (name_normalized) DO UPDATE`
- Janyas clear stale melakarta_number on update
- Total: 961 ragas

## Additional Changes
- Fixed `01_reference_data.sql` — removed incorrect melakarta_number from sample janyas (Sri, Hamsadhwani, Mohanam)
- Added `make seed-ragas` target in Makefile for standalone raga seeding
- `make seed` automatically includes the new file via glob

# Scale Notation Convention

Project convention: `S R2 G3 M1 P D2 N3 S` — numbered swarasthanas (R1/R2, G2/G3, M1/M2, D1/D2, N2/N3).

# Files Changed

- `tools/raga-reference-extractor/extract_ragas.py` (new)
- `tools/raga-reference-extractor/requirements.txt` (new)
- `database/seed_data/05_raga_reference_data.sql` (new)
- `database/seed_data/01_reference_data.sql` (edited — removed melakarta_number from sample janyas)
- `Makefile` (edited — added seed-ragas target)
- `conductor/tracks.md` (edited — added TRACK-091)
- `conductor/tracks/TRACK-091-comprehensive-raga-reference-data.md` (new)
