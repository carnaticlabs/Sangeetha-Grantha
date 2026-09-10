| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-09-10 |
| **Author** | Sangeetha Grantha Team |
| **Document Type** | Evidence record |

# Goal

---

> [!NOTE]
> Historical evidence: results, counts, commands, and observations below belong to the original work described here. The editorial update date is not a new test or corpus verification. For present behavior, use [current feature map](./../01-requirements/features/README.md).
Implement comprehensive raga reference dataset.

## Proposed Changes
| File | Change |
| --- | --- |
| `tools/raga-reference-extractor/` | New python script and requirements to extract ragas |
| `database/seed_data/05_raga_reference_data.sql` | Seed file for 72 melakartas and 889 janyas |
| `database/seed_data/01_reference_data.sql` | Removed melakarta_number from sample janyas |
| `database/seed_data/02_sample_data.sql` | Update sample data to ensure krithi_ragas junction row |
| `Makefile` | Add seed-ragas target |
| `conductor/tracks.md` | Add TRACK-091 |
| `conductor/tracks/TRACK-091-comprehensive-raga-reference-data.md` | Track file |

---

[Section index](./README.md) · [Documentation home](./../README.md) · [Feature status](./../01-requirements/features/README.md)
