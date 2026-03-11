| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-03-11 |
| **Author** | Sangeetha Grantha Team |

# Goal
Implement comprehensive raga reference dataset.

# Proposed Changes
| File | Change |
| --- | --- |
| `tools/raga-reference-extractor/` | New python script and requirements to extract ragas |
| `database/seed_data/05_raga_reference_data.sql` | Seed file for 72 melakartas and 889 janyas |
| `database/seed_data/01_reference_data.sql` | Removed melakarta_number from sample janyas |
| `database/seed_data/02_sample_data.sql` | Update sample data to ensure krithi_ragas junction row |
| `Makefile` | Add seed-ragas target |
| `conductor/tracks.md` | Add TRACK-091 |
| `conductor/tracks/TRACK-091-comprehensive-raga-reference-data.md` | Track file |
