| Metadata | Value |
|:---|:---|
| **Status** | Not Started |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-07-19 |
| **Author** | Sangeetha Grantha Team |
| **Priority** | P3 — backlog; 2.4% of corpus, no data loss |
| **Depends on** | [TRACK-093](./TRACK-093-trinity-krithi-bulk-import.md) (corpus imported) |
| **Interacts with** | [TRACK-100](./TRACK-100-multi-pass-indic-script-extraction.md) (multi-pass Indic parsing), [TRACK-079](./TRACK-079-e2e-pipeline-section-fix.md) (previous section-consistency remediation) |

# TRACK-133: Section-Count Mismatch Remediation (29 krithis)

## Goal

Resolve the residual section-count mismatches left by the Trinity import: **29 krithis (2.4% of
1,226), 108 variant rows.** Tracked rather than left to attrition because the failures are *not*
29 independent data problems — the majority share one root cause and should fall to one fix.

## Current state (dev DB, 2026-07-19)

- All 108 mismatches are **"fewer sections than canon"**. There are **zero** missing-sections rows —
  no variant is empty, so nothing was lost, only under-segmented.
- 1,225 of 1,226 krithis have canonical sections; exactly one has none.

```sql
WITH canon AS (SELECT krithi_id, COUNT(*) c FROM krithi_sections GROUP BY 1),
var AS (SELECT v.krithi_id, v.language::text AS lang, COUNT(s.id) c
  FROM krithi_lyric_variants v LEFT JOIN krithi_lyric_sections s ON s.lyric_variant_id = v.id
  GROUP BY 1,2)
SELECT k.title, COALESCE(canon.c,0) AS canon_sections, COUNT(*) AS bad_variants,
       MIN(var.c) AS min_actual, MAX(var.c) AS max_actual, string_agg(DISTINCT var.lang, ',') AS langs
FROM var JOIN krithis k ON k.id = var.krithi_id
LEFT JOIN canon ON canon.krithi_id = var.krithi_id
WHERE var.c <> COALESCE(canon.c,0)
GROUP BY k.title, canon.c ORDER BY bad_variants DESC, k.title;
```

## The dominant pattern — one root cause, not 29

**20 of the 29 krithis have every non-primary-language variant collapsed to exactly 1 section**,
while the canonical structure has 2–17. Examples:

| Krithi | Canon sections | Variants affected | Actual |
|:---|:---|:---|:---|
| `sAdhincenE` | 11 | 5 (kn, ml, sa, ta, te) | 1 each |
| `rAmAbhirAma raghurAma` | 8 | 5 | 1 each |
| `rAma Eva daivataM` | 7 | 5 | 1 each |
| `cUDarE celulAra` | 10 | 5 | 1 each |
| `Alakalallalaadaga` | 4 | 5 | 1 each |

That is the classic **"script variant never got segmented — the whole lyric landed in one section"**
failure, i.e. the section splitter matched headings in the primary script but not in the transliterated
scripts. Fixing the splitter for those scripts should clear the bulk of the 108 rows at once, and any
per-krithi curation should happen only *after* that, against the remainder.

### The rest

- **Partial segmentation** (segmented, but short): `Sri Rama Jaya Rama` (canon 17, actual 1–12 across
  6 variants), `rAma sItA rAma` (canon 10, actual 1–6), `Rama Rama Rama Sita` (canon 14, actual 1–6).
  These look like genuinely harder parses, not a clean on/off failure.
- **Single-variant outliers** (1 bad variant each): `Ivaraku jUcinadi`, `Karunaa Jaladhi`,
  `dorakunAyiTuvaNTi` (all `kn`, 5→2); `jaya mangaLaM`, `pAhi rAma candra` (both `en`, 7→5 / 10→5);
  `jAnakI ramaNa`, `tanalOnE dhyAninci`, `vina rAdA` (all `ta`, →1).

## Scope

1. **Diagnose before fixing.** Confirm the 20-krithi cluster really is one splitter failure by
   re-running extraction on 2–3 of them and inspecting where segmentation stops. Do not write
   per-krithi data fixes until the shared cause is either confirmed or ruled out.
2. **Fix the splitter** in `tools/krithi-extract-enrich-worker` for the affected scripts; add
   regression cases from the cluster above. Coordinate with TRACK-100's multi-pass architecture
   rather than bolting on a parallel path.
3. **Re-extract and re-verify** the affected krithis; the mismatch query above should shrink to the
   genuinely-hard remainder.
4. **Curate the remainder** by hand — route through the `carnatic-musicologist` subagent, since
   deciding the correct section count for e.g. a 17-section `Sri Rama Jaya Rama` is a lakshana
   judgement, not a parsing one.
5. **Investigate the single krithi with no canonical sections at all** (1 of 1,226).

## Folded-in cleanup

`CuratorService.getStats()` counts section issues by loading every row of `krithi_sections` and
`krithi_lyric_sections` into in-memory maps and diffing them in Kotlin
([CuratorService.kt:67-84](../../modules/backend/api/src/main/kotlin/com/sangita/grantha/backend/api/services/CuratorService.kt)).
At 1,226 krithis / 6,809 variants that is two full table scans on every curator-dashboard load, to
produce five integers. Replace with a SQL aggregate while this track is already in the file.

## Definition of done

- Mismatch query returns only rows that have been consciously accepted as correct.
- Remaining accepted mismatches are documented with a reason, so the next audit does not re-litigate them.
- The splitter regression suite covers the cluster, so a future import cannot silently reintroduce it.
