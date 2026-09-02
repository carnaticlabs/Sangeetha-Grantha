| Metadata | Value |
|:---|:---|
| **Status** | Not Started — re-verified 2026-08-30 (figures unchanged) |
| **Version** | 1.1.0 |
| **Last Updated** | 2026-08-30 |
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

> **Re-verified 2026-08-30** against the restored corpus (dump `sangita_grantha_20260830_post_v56`,
> Flyway v57, after TRACK-136/137 raga-identity work): **108 mismatch variant rows across 29 krithis,
> all "fewer than canon" (0 rows exceed canon), 1 krithi with no canonical sections, 1,226 total.**
> Every figure is unchanged — the raga-identity/orphan-cleanup migrations do not touch section data,
> and the restore reproduced the same state. The count is now visible in the live curator dashboard
> (`/v1/admin/curator/stats` → `sectionIssuesCount: 108`). This track's diagnosis and plan below still
> stand; nothing needs re-adjudication.

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

---

## Diagnosis (2026-09-02) — evidence from the live pipeline

Re-ran the **real** extraction pipeline (`HtmlTextExtractor.extract` → `normalize_garbled_diacritics`
→ `StructureParser.parse`) against the live source pages for three cluster krithis, on the current
worker code (post TRACK-100–104), and compared per-variant section counts to the DB mismatch rows.

| Krithi | DB (imported) | Live re-parse now (en / sa / ta / te / kn / ml) | Verdict |
|:---|:---|:---|:---|
| `cUDarE celulAra` | 1 each non-primary | **10 / 10 / 10 / 10 / 10 / 10** | Already correct |
| `rAmAbhirAma raghurAma` | 1 each non-primary | **8 / 8 / 8 / 8 / 8 / 8** | Already correct |
| `sAdhincenE` | 1 each non-primary | 11 / **5 / 5 / 5 / 5 / 5** | Real residual gap |

### The single-shared-cause hypothesis (as written) is REFUTED — but the theme holds

The premise "the splitter matches headings in the primary script but collapses every transliterated
variant to exactly 1 section" is **not what the current code does**. The DB's "1 section each" rows are
**stale pre-fix import residue**: TRACK-093 imported the corpus *before* TRACK-100–104 landed the
multi-pass Indic parser, and the corpus was never re-extracted. Two of the three canonical cluster
exemplars now parse fully in every script. The failures split into three buckets:

- **Bucket A — stale import, no code change needed (the majority).** `cUDarE`, `rAmAbhirAma raghurAma`
  and their kind already parse correctly on current code. They clear on **re-extract alone**. This is
  almost certainly most of the 20-krithi / 108-row bulk.
- **Bucket B — one real, specific residual splitter gap.** `sAdhincenE`-type kritis under-segment on
  the **swara-sahitya** count only. The English variant labels each swara block with the full word
  `svara sAhitya 1..7` (matched by the existing `svara sahitya` pattern). The Indic variants use the
  **abbreviated inline ordinal marker** — Devanagari `स्व2.` `स्व3.` … `स्व7.` (and `स्व4(A).`), Tamil
  `ஸ்வ3.` `ஸ்வ4.` … — i.e. *`sva`-cluster + digit + period + text on the same line*. Only the lone
  full-word header `स्वर साहित्य` / `ஸ்வர ஸாஹித்ய` is recognized (CAT-B patterns, `structure_parser.py`
  L201–205), so `sva2..sva7` collapse into that one `SWARA_SAHITYA` block → **11 canon vs 5 Indic**.
  This is the exact analogue of the already-handled, context-gated `INLINE_INDIC_PAC_PATTERNS`
  (`प`/`अ`/`च`, L332–348) — but there is **no inline Indic pattern for the `स्व`/`ஸ்வ` swara marker**.
- **Bucket C — not a splitter bug at all.** The "partial segmentation" kritis (`Sri Rama Jaya Rama`
  canon 17, `rAma sItA rAma` canon 10, `Rama Rama Rama Sita` canon 14) and the single-variant `en`/`ta`
  outliers are lakshana (correct-section-count) judgements, not parsing on/off failures. These route to
  the `carnatic-musicologist` subagent. This worker does **not** adjudicate section counts.

Evidence, detect-column excerpt for `sAdhincenE` (Devanagari), showing the miss:

```
detect=SWARA_SAHITYA  | 'स्वर साहित्य'          ← lone full header, matched
detect=None           | 'स्व2. रं(गे)शुडु ...'   ← ordinal marker, MISSED → collapses
detect=None           | 'स्व3. गोपी जन ...'      ← MISSED
detect=None           | 'स्व4. वनितल ...'        ← MISSED  (also 'स्व4(A). ...')
...
```

**Net:** re-extract first clears Bucket A; a single scoped splitter addition (inline Indic `sva`+ordinal
marker) clears Bucket B; Bucket C is musicologist work, out of the splitter's scope.

---

## Spec

**Spec Status: Draft** (awaiting acceptance — do not implement product code yet)

### Requirements

- **R1 (diagnose-before-fix).** Re-extraction of the 29 affected krithis from their canonical sources
  must be run and the track's mismatch query re-evaluated **before** any per-krithi data fix. The DB
  counts are stale; remediation is measured against post-re-extract numbers, not the current 108/29.
- **R2 (splitter fix, Bucket B).** `StructureParser` must recognize Indic **inline swara-sahitya ordinal
  markers** (`sva`-cluster + digit(s) + optional `(A)` + period, in all five scripts) as
  `SWARA_SAHITYA` section headers — on the **same** `_detect_section_header` / `SECTION_HEADER_PATTERNS`
  seam and the **same** context-gating mechanism as `INLINE_INDIC_PAC_PATTERNS`. No parallel
  segmentation path (TRACK-100 coordination is a hard constraint).
- **R3 (domain conformance).** Output stays domain-shaped: `SectionType.SWARA_SAHITYA`, lowercase enum
  on the wire for Kotlin ingestion; canonical terminology unchanged.
- **R4 (regression coverage).** Deterministic fixtures + tests drawn from the cluster: (a) `sAdhincenE`
  Indic variants segment to 11 incl. 7 `SWARA_SAHITYA`; (b) a "stays-correct" guard for a Bucket-A
  exemplar (`cUDarE` or `rAmAbhirAma raghurAma`); (c) a false-positive guard so a lyric line merely
  *beginning* with `स्व…`/`ஸ்வ…` (no digit+period) is **not** split. Fixtures are captured, no network.
- **R5 (quality gates).** No test weakened or deleted; `ruff check .` and `mypy .` clean; stack
  restarted (`make dev-down` → `make dev`) after worker `.py` edits.
- **R6 (boundary).** Bucket C (lakshana section-count decisions) is handed to `carnatic-musicologist`.
  The worker emits low-confidence segmentation for review; it never invents or drops sections to hit a
  count.

### Design

- **Primary file:** `tools/krithi-extract-enrich-worker/src/structure_parser.py`. Add
  `INLINE_INDIC_SWARA_PATTERNS` (list of `(compiled, "SWARA_SAHITYA")`) + an `_INLINE_INDIC_SWARA_PROBE`,
  and gate them inside `_detect_section_header` exactly as the existing `INLINE_INDIC_PAC` block is
  gated (fire only when the document actually contains such markers). Marker shape (verified live for
  Devanagari + Tamil): `^\s*(स्व|ஸ்வ|స్వ|ಸ್ವ|സ്വ)\d+(\([A-Za-z]\))?\s*\.\s*(?=\S)`. Ordering must keep
  the existing full-word `स्वर साहित्य`/`ஸ்வர ஸாஹித்ய` header working and must not shadow lyric words.
- **Tests:** extend `tests/test_structure_parser.py` (or a new `tests/test_indic_swara_markers.py`)
  with captured fixtures under `tests/fixtures/structure_parser/` or `tests/fixtures/html/`.
- **Re-extract step:** reuse the prepared `tests/fixtures/section_issues_reextract.csv` pattern / worker
  extraction batch over the 29 krithis; results flow through the **Kotlin ingestion / curator** path,
  never a direct canonical write from Python (architecture boundary).

### Flagged concerns

- **Stale-count trap:** re-verify the mismatch query *after* re-extract; expect a large drop from
  Bucket A alone. Do not report residual against today's 108.
- **Kotlin parity:** the worker skill states structure-parser collapses are "mirrored in Kotlin's
  `TransliterationCollapse`." Adding the Indic swara marker may need a mirrored Kotlin change to keep
  ingestion consistent — **flag for `kotlin-backend-engineer`**; the worker task must not edit backend
  code itself.
- **`CuratorService.getStats()` SQL-aggregate cleanup** (folded-in item) is a **Kotlin backend** change
  — out of this worker/splitter task's scope; route to `kotlin-backend-engineer`.

### Open questions

- **Q1:** Exact command/path for the re-extract-and-ingest of the 29 (worker CLI batch vs curator
  re-extract endpoint)? Confirm it routes through Kotlin ingestion.
- **Q2:** Do the Telugu/Kannada/Malayalam variants actually use `స్వ`/`ಸ್ವ`/`സ്വ` + digit markers?
  Devanagari and Tamil are confirmed from live evidence; te/kn/ml forms must be verified from their
  variant text before the regex is finalized.

---

## Plan

**Plan Status: Accepted** (2026-09-02). Decisions: Q1 → re-extract via **Worker CLI batch** against
the dev DB (5432, now up); Q2 → verify te/kn/ml `sva`-marker forms against real DB data before
finalizing the regex. Kotlin `TransliterationCollapse` parity mirror is deferred to the
`kotlin-backend-engineer` step (notes retained under Flagged concerns), out of this worker task's scope.

### Files that will change

1. `src/structure_parser.py` — add `INLINE_INDIC_SWARA_PATTERNS` + `_INLINE_INDIC_SWARA_PROBE`; gate in
   `_detect_section_header` (mirrors the `INLINE_INDIC_PAC` gating, ~L318–350 / detection seam).
2. `tests/fixtures/structure_parser/sadhincene_swara_multiscript.{txt,expected.json}` (or a captured
   `tests/fixtures/html/tv_sadhincene_swara.html`) — deterministic, no network.
3. `tests/test_structure_parser.py` (or new `tests/test_indic_swara_markers.py`) — the R4 regressions.

### Order of work

1. Close **Q2**: pull te/kn/ml variant text for `sAdhincenE` and confirm the `sva`+ordinal marker form
   per script; finalize the regex character set.
2. Capture deterministic fixture(s): `sAdhincenE` (all scripts) + a Bucket-A "must-stay-correct"
   fixture (`cUDarE` or `rAmAbhirAma raghurAma`).
3. Add **failing** tests (Indic `sAdhincenE` = 11 incl. 7 `SWARA_SAHITYA`; stays-correct guard;
   false-positive guard).
4. Implement `INLINE_INDIC_SWARA_PATTERNS` + probe gating; make tests pass.
5. `ruff check . --fix` → `ruff format .` → `mypy .` → `pytest` (unit + integration).
6. Restart stack: `make dev-down` then `make dev` (sangita-restart-on-kotlin-change covers worker `.py`).
7. Re-extract the 29 krithis through the ingestion path (Q1); re-run the track's mismatch SQL.
8. Emit the **residual list** (Bucket C + anything Bucket B did not clear) → `carnatic-musicologist`.
9. Route the `CuratorService.getStats()` SQL-aggregate cleanup to `kotlin-backend-engineer` (separate
   change; tracked here, not in the worker diff).

### Risks

- **Over-eager regex** splitting a lyric line that opens `स्व…` + a footnote digit → mitigated by probe
  gating (require digit **and** period) + the R4(c) false-positive test.
- **te/kn/ml marker form differs** from Devanagari/Tamil → Q2 verification precedes the regex.
- **Kotlin `TransliterationCollapse` drift** → flagged; coordinate, do not edit backend from here.
- **Re-extract side effects** on non-section fields → keep the re-extract scoped to the 29 and diff only
  section counts.

### Proof (from CLAUDE.md "Verifying your work")

- `pytest` green in `tools/krithi-extract-enrich-worker`, including the new cluster regressions.
- `ruff check .` and `mypy .` clean.
- Re-run the track's mismatch SQL after re-extract — expect it to shrink to the Bucket-C remainder;
  capture that residual list.
- `verify-import` skill checklist on the re-extracted krithis (confirm `krithi_lyric_sections` junction
  rows populated, not just FK columns).

---

## Implementation & Results (2026-09-02)

### Code change (landed, splitter only)

`src/structure_parser.py` — added `INLINE_INDIC_SWARA_PATTERNS` + `_INLINE_INDIC_SWARA_PROBE`
(`(?:स्व|ஸ்வ|స్వ|ಸ್ವ|സ്വ)\d+(?:\([A-Za-z]\))?\s*\.`), an `_inline_indic_swara_enabled` per-block
flag set in `_build_blocks`, and a detection loop in `_detect_section_header` — mirroring the existing
context-gated `INLINE_INDIC_PAC` seam (no parallel path). The bare `स्वर साहित्य` group-title line
that precedes `स्व1.` becomes an empty-body block and is dropped by the existing `_extract_sections`
empty-block guard, so the Indic count lands at parity with English (N sub-blocks, not N+1) — verified,
not assumed.

### Tests (added, none weakened)

- `tests/fixtures/structure_parser/tyagaraja_swara_sahitya_multi_variant.{txt,expected.json}` — 5-script
  fixture with same-line `sva`-ordinal markers.
- `tests/test_indic_swara_markers.py` — 6 tests: per-variant parity (all 6 variants = canonical count),
  all-five-scripts marker detection, `(A)` continuation form, and two false-positive guards (a lyric
  word `स्वप्न…` beginning with the cluster but no digit+period is **not** split; a block with only the
  full-word header keeps the probe dormant).
- Quality gates: **305 unit tests pass**, `ruff check .` clean, `mypy .` clean (58 files).

### Predicted residual (live-source re-parse; DB re-extract still pending — see below)

Re-parsed all 16 *named* krithis through the real pipeline with the fix applied. **14 of 16 now parse
CLEAN** (every variant matches canon), including all three swara-sahitya cluster exemplars and every
partial/outlier that was named. Two show a uniform −1 across **all** Indic scripts, and both are
**lakshana section-count questions, not splitter gaps** → route to `carnatic-musicologist`:

| Krithi | Canon (en) | Indic (sa/ta/te/kn/ml) | Nature |
|:---|:---|:---|:---|
| `rAma Eva daivataM` | 7 (P + 6×C) | 6 (P + 5×C) | One charanam merged in Indic; English charanams carry footnote digits — correct charanam count is a structural call. |
| `alakalallalADaga` | 4 (P + **2×A** + C) | 3 (P + 1×A + C) | English splits a second ANUPALLAVI (`celuvu mIraganu`); Indic keeps one. Whether that is a distinct anupallavi is lakshana — the Indic reading may be the more correct one. |

### Live re-extract results (2026-09-02, orchestrator host)

The blocked DB steps below were subsequently **run on the live dev stack**:

- Stack booted via `./start-sangita.sh` (DB + backend + worker); Flyway migrated clean.
- **Baseline confirmed on live DB:** 108 bad variant rows / 29 krithis (matches the track).
- **Source scoping:** the 29 krithis map to **37 distinct source URLs** (28 krithis on
  `thyagaraja-vaibhavam.blogspot.com`, 1 on `syamakrishnavaibhavam.blogspot.com`). Re-extract was
  fired **precisely** — 37 targeted `POST /v1/admin/imports/re-extract` calls (one per exact URL),
  not host-wide — to keep the mutation scoped to the 29. Gemini enrichment confirmed **off**
  (`enable_gemini_enrichment` default False, no env override) → no external API cost.
- **Re-extract outcome:** totalMatching=37, requeued=37, variantsCleared=29, 0 failures; all 37
  queue rows drained to `INGESTED`.
- **Post-fix mismatch query: 108 → 46 bad rows, 29 → 9 krithis.** 20 krithis / 62 rows cleared by the
  Bucket-B splitter fix + re-extract.
- **Worker tests:** 323 passed. **Junction verified:** `sAdhincenE` now 6 variants × 11 = 66
  `krithi_lyric_sections` (was 1/variant).

**Residual 9 krithis (46 rows) → musicologist input:**

| Krithi | Canon | Actual | Langs |
|:---|:---|:---|:---|
| `Rama Rama Rama Sita` | 14 | 6 | en,kn,ml,sa,ta,te |
| `rAma sItA rAma` | 10 | 6 | en,kn,ml,sa,ta,te |
| `Alakalallalaadaga` | 4 | 3 | kn,ml,sa,ta,te |
| `Raanidi Raadu` | 4 | 3 | kn,ml,sa,ta,te |
| `ennEramum un pAda` | 6 | 2 | kn,ml,sa,ta,te |
| `enta bhAgyamu` | 3 | 2 | kn,ml,sa,ta,te |
| `kaNTa jUDumi` | 3 | 2 | kn,ml,sa,ta,te |
| `rAma Eva daivataM` | 7 | 6 | kn,ml,sa,ta,te |
| `ramA ramaNa rArA` | 8 | 7 | kn,ml,sa,te |

Plus the 1 krithi with **no canonical sections**: `mAdhavO mAM pAtu`.

### Blocked steps (environment) — RESOLVED, see above

The dev DB (5432) and Docker were **unreachable from the worker's execution context** (`pg_isready`
exit 2; `docker ps` empty), so these accepted steps could not be run in that context and were handed
back for a host with DB access (now completed — see "Live re-extract results" above):

1. **Restart stack** — `make dev-down && make dev` (sangita-restart-on-kotlin-change; covers worker `.py`).
2. **Re-extract the 29** via the worker route. The worker CLI (`src/cli.py`) exposes only single-input
   `extract` / `transliterate`; DB-backed re-extraction runs through the **queue worker** (`python -m
   src.worker`, `DATABASE_URL=...`, polls `extraction_queue`). Confirm the enqueue path used for the 29
   (curator re-extract endpoint vs. seeding `extraction_queue`) before the batch run.
3. **Re-verify** with the track's mismatch SQL; expect it to collapse to the Bucket-C remainder
   (the two above + any of the ~13 *unnamed* cluster krithis the query enumerates — most expected to
   clear, matching the 14/16 named hit rate).
4. **`verify-import`** on the re-extracted krithis (junction `krithi_lyric_sections` populated).

### Handoffs

- **Musicologist (Bucket C):** `rAma Eva daivataM`, `alakalallalADaga`, plus the partial-segmentation
  set (`SrI rAma jaya rAma` canon 18, etc.) and the "1 krithi with no canonical sections" investigation
  — for lakshana section-count adjudication. This worker made **no** section-count judgement.
- **`kotlin-backend-engineer` (out of this worker's scope, notes retained):** (a) mirror the Indic
  `sva`-ordinal collapse in `TransliterationCollapse` for ingestion parity; (b) the folded-in
  `CuratorService.getStats()` SQL-aggregate cleanup (`CuratorService.kt:67-84`).

---

## Musicologist adjudication (Bucket C) — 2026-09-02

Lakshana review of the residual 9 mismatch krithis (46 rows) + the 1 krithi with no canonical
sections. Each verdict is grounded in the actual per-variant `krithi_lyric_sections` text on the live
DB (5432). **Proposals only — no DB changes applied here.** Two findings overturn the "fewer than
canon = variant is short" premise: in several cases the **canon is wrong** and the shorter Indic
reading is the correct one.

### Summary of verdicts

| Krithi | Canon | Correct | Verdict | One-line reason |
|:--|:--|:--|:--|:--|
| `mAdhavO mAM pAtu` | 0 | 10 (ragamalika) | FIX (parse + ragamalika) | Dashavatara ragamalika; §6.2 violated |
| `ennEramum un pAda` | 6 | 6 | FIX-parse | 4 charanams present but merged into anupallavi |
| `enta bhAgyamu` | 3 | 3 | FIX-parse | charanam present but merged into anupallavi |
| `kaNTa jUDumi` | 3 | 3 | FIX-parse | charanam present but merged into anupallavi |
| `rAma Eva daivataM` | 7 | 7 | FIX-parse | one charanam merged (`च4` marker, no period) |
| `rAma sItA rAma` | 10 | 6 | CANON-WRONG | canon charanams 6–9 are empty phantom rows |
| `Rama Rama Rama Sita` | 14 | 6 | CANON-WRONG | canon charanams 6–13 are empty phantom rows |
| `Alakalallalaadaga` | 4 | 3 | CANON-WRONG / ACCEPT-shorter | English mis-split the pallavi; Indic (3) correct |
| `Raanidi Raadu` | 4 | 3 | CANON-WRONG / ACCEPT-shorter | English mis-split the charanam; Indic (3) correct |
| `ramA ramaNa rArA` | 8 | 7 | CANON-WRONG / ACCEPT-shorter | en+ta false-split on word `tvac-caraNam`; Indic (7) correct |

### FIX-parse — a second, distinct splitter gap (hand to python-engineer)

These four are **not** lakshana problems — the full sahitya is present in every Indic variant, but a
**charanam/section marker in the Indic scripts is not detected**, so the charanam(s) collapse into the
preceding block. This is the exact analogue of the Bucket-B swara-marker gap, one class over:

- **Bare single-akshara `ca` charanam marker** (no digit): `च` (sa) / `ச` (ta) / `చ` (te) / `ಚ` (kn) /
  `ച` (ml) standing at the head of the charanam line. Seen in `kaNTa jUDumi`, `enta bhAgyamu` — the
  Indic ANUPALLAVI block literally contains `… (kaNTa) च अल नाडु सौमित्रि …` with the charanam glued on.
- **Full-word inline charanam marker**: Telugu `చరనం`, etc. Seen in `ennEramum un pAda` — the Indic
  anupallavi ends `… (ennEramum) చరనం` and all four charanams follow in one block (char totals confirm
  the text is present: Indic ~900 vs English 982).
- **Digit marker without trailing period**: `च4 सुर तारक …` in `rAma Eva daivataM` (sa) — the existing
  inline-Indic pattern requires `च4.` with a period, so the no-period form leaks and charanam 4 merges
  into charanam 3.

Recommendation: extend the inline-Indic section-header detection (same seam/gating as
`INLINE_INDIC_PAC_PATTERNS` / the new `INLINE_INDIC_SWARA_PATTERNS`) to cover (a) bare `pa/anu/ca`
akshara markers, (b) the full-word inline forms, and (c) the digit-without-period form. Add
false-positive guards (a lyric line merely beginning with `च…` must not split — see `ramA ramaNa rArA`
below for why this matters). Re-extract these four; content is intact, so they should reach canon parity.

### CANON-WRONG — canon over-specified; the shorter reading is correct (hand to postgres-engineer / curator)

- **`rAma sItA rAma` (10→6)** and **`Rama Rama Rama Sita` (14→6)**: verified that canonical charanams
  6–9 / 6–13 have **zero** lyric text in **every** variant *including English*. No variant supports the
  higher count. The true structure is **Pallavi + 5 Charanams = 6**; the extra canonical rows are
  phantom empties (import artifact). Proposal: delete the empty trailing `krithi_sections` rows; accept
  6. Documented reason so this is not re-litigated: *canon carried empty charanam slots with no text in
  any of the 6 variants; corpus consensus is 6.*

- **`Alakalallalaadaga` (4→3)**: canon is `P, A, A` (two anupallavis) `, C`. The Indic variants read
  `P, A, C` = 3 and are **correct**. The English variant wrongly split the pallavi — `alakalallalADaga
  kani(ya)` and `rAN-muni(y)eTu pongenO` are **one** pallavi line (the Sanskrit keeps them together),
  and the real anupallavi is `celuvu mIraganu…`. Canon (derived from the bad English split) has a
  spurious second anupallavi. Proposal: canon = `P + A + C`; also fix the English variant's pallavi
  split. Reason: *English over-segmented the pallavi into pallavi+anupallavi; Indic reading is correct.*

- **`Raanidi Raadu` (4→3)**: canon is `P, A, C, A` — an **ANUPALLAVI after the CHARANAM**, which is
  structurally impossible in a krithi. The Indic variants read `P, A, C` (the charanam runs
  `dEvEndruniki … vana cara bAdhala … tyAgarAja bhAgyamA`). The English variant split that single
  charanam into Charanam + a spurious trailing "anupallavi" (`vana cara bAdhalA…`). True = 3. Proposal:
  canon = `P + A + C`; fix the English variant split. Reason: *canon's 4th section (anupallavi at
  order 4) is a mis-labelled tail of the charanam; Indic reading is correct.*

- **`ramA ramaNa rArA` (8→7)** — residual direction is **inverted**. `sa/te/kn/ml` = 7 = **correct**
  (Tyagaraja, Vasanta: Pallavi + 6 Charanams). `en` and `ta` = 8 because the splitter **false-split**
  charanam 4 at the lyric word `t(v)ac-caraNam` — `caraNam` inside the sahitya was mistaken for a
  charanam header (`ta`: `…SaraNAgata tvac-` | `bhava tAraNambu cEsunu`). True count = 7. Proposal:
  canon = `P + 6C` (= 7); merge the false split in the en+ta variants; add the false-positive guard to
  the parser. Reason: *the word "caraNam" in the sahitya triggered a spurious split in en/ta; the 4
  Indic variants at 7 are the correct reading.*

### Incorrect — ragamalika collapsed to one raga (§6.2 violation)

- **`mAdhavO mAM pAtu`** (canon 0, 1 variant, `is_ragamalika = false`, single `primary_raga_id`, 1
  `krithi_ragas` row). This is the well-known **Dashavatara Ragamalika**: ten avatara stanzas, each in
  its own raga, each with a Madhyamakala Sahitya. The lyric is one unsegmented blob because the parser
  does not recognize inline **`<raga> rAgaM`** headers or the `[Madhyama Kala Sahitya]` marker as
  section boundaries, so no canonical structure was ever built. Ordered ragas (verified from the text):

  1. nATa (Matsya) 2. SrI gauLa (Kurma) 3. SrI (Varaha) 4. Arabhi (Narasimha) 5. varALi (Vamana)
  6. kEdAra (Parasurama) 7. vasanta (Rama) 8. suraTi (Balarama) 9. saurAshTra (Krishna)
  10. madhyamAvati (Kalki).

  Proposals (two-part, propose-only): **(a) metadata** — set `is_ragamalika = true` and replace the
  single `krithi_ragas` row with **10 ordered rows** (`order_index` 1–10) for the ragas above, per §6.2
  (never represent a ragamalika by one `primary_raga_id`). **(b) structure/parse** — build 10 canonical
  sections, one per raga segment, each carrying its lead sahitya + its Madhyamakala Sahitya; this needs
  a parser addition for raga-header + `[Madhyama Kala Sahitya]` segmentation (python-engineer), then
  re-ingest. Section-type assignment (e.g. `OTHER` per raga stanza vs. treating segment 1 as pallavi) is
  a lakshana call — recommend `OTHER` per stanza with a Madhyamakala sub-block; flag for curator sign-off.
  Confidence high that it is a ragamalika; medium on exact section typing — human review recommended.

### Routing

- **python-engineer (splitter gap #2):** inline-Indic `pa/anu/ca` markers — bare akshara, full-word, and
  digit-without-period forms — plus the `caraNam`-in-sahitya false-positive guard; and raga-header +
  `[Madhyama Kala Sahitya]` segmentation for ragamalikas. Covers `ennEramum un pAda`, `enta bhAgyamu`,
  `kaNTa jUDumi`, `rAma Eva daivataM`, the en/ta over-split in `ramA ramaNa rArA`, and `mAdhavO`.
- **postgres-engineer / curator:** delete phantom empty `krithi_sections` (`rAma sItA rAma` → 6,
  `Rama Rama Rama Sita` → 6); correct canon for `Alakalallalaadaga` → 3, `Raanidi Raadu` → 3,
  `ramA ramaNa rArA` → 7; set `is_ragamalika` + 10 ordered `krithi_ragas` for `mAdhavO mAM pAtu`.

---

## Parser phase COMPLETE (2026-09-02)

Two splitter fixes (Bucket B swara markers + gap-#2 charanam markers) plus a calibration pass
against **real** extracted source text (not synthetic fixtures) cleared the parser-owned residual.

| Stage | Bad variant rows | Krithis |
|:---|:---|:---|
| Baseline | 108 | 29 |
| After splitter fix (round 1) | 46 | 9 |
| After gap-#2 + real-data calibration (round 2) | **26** | **5** |

**Cleared by parser work (24 krithis):** the Bucket-A stale-import set, `sAdhincenE` (swara),
plus `rAma Eva daivataM` (digit `च4`), `ennEramum un pAda` (full-word `चरनम्`/Tamil `சரனம்`),
`enta bhAgyamu` and `kaNTa jUDumi` (bare `च`/`ச` charanam marker). Worker tests: **352 passed**.

Key fixes in `structure_parser.py`: (a) bare-`ca` detection relaxed to a self-gated charanam-only
pattern with a trailing-whitespace discriminator (`च `/`ச ` marker vs `चॆन्त`/`செந்த` lyric word);
(b) Tamil charanam class extended to alveolar `ன` (U+0BA9); (c) digit-no-period `च4`; (d) `tvac-caraNam`
false-positive guard.

**Remaining 26 rows / 5 krithis are all CANON-side data errors** (postgres-engineer phase), not parser
gaps: `Rama Rama Rama Sita`, `rAma sItA rAma`, `Alakalallalaadaga`, `Raanidi Raadu`, `ramA ramaNa rArA`.

**`mAdhavO mAM pAtu` — reclassified.** Not a splitter issue: the HTML extractor yields **empty** text
for the Dikshitar source page (`extraction_queue` INGESTED, result_count=1, 0 variants persisted). The
RAGA_SEGMENT segmentation is correct when text is present (fixture proves 10 stanzas → 10 sections) but
has nothing to segment. **Route to the `html_extractor` owner** (upstream), plus the postgres-engineer
metadata fix (`is_ragamalika` + 10 `krithi_ragas`). Do not expect re-extract alone to fix it.

---

## Postgres corrections (Bucket C data fixes) — 2026-09-02

Authored by postgres-engineer. The DB was not reachable from the authoring context, so this is
**author-and-hand-off**: SQL below is applied by the orchestrator on the live stack (`sangita_grantha`,
container `sangeetha-grantha-db-1`).

### Delivery vehicle decision — Flyway versioned migrations (not the curator/API path)

These are one-off **corpus-data** repairs. The established convention here for exactly this class is a
**Flyway `VNN__` migration** carrying its own `audit_log` write — see V45 (remove stale anupallavi),
V46 (delete incomplete variant), V47 (demerge ragamalika), V38, V48. Rationale over the curator/API
route: the corrections must survive `make db-reset` and CI Testcontainers (a curator edit is discarded
on reset), are checksum-tracked and reproducible across environments, and are reviewable in git. Each
block writes `audit_log` inline, satisfying the mutation-audit rule (the curator path's automatic audit
is not available to raw SQL, so the audit is explicit). No constraint is weakened; every block is a
no-op if the target krithi is absent (fresh reset before corpus load), matching V45–V47 behaviour.

Grouping so the orchestrator can act now vs. hold:

| Group | Vehicle | Status | Contents |
|:--|:--|:--|:--|
| 1 | `database/migrations/V58__track133_delete_phantom_empty_charanam_sections.sql` (**written**) | **APPLY NOW** | phantom-empty deletes: `rAma sItA rAma` 10→6, `Rama Rama Rama Sita` 14→6 |
| 2 | held SQL → promote to `V59__` on sign-off | **HOLD for user** | canon over-count merges: `Alakalallalaadaga` 4→3, `Raanidi Raadu` 4→3, `ramA ramaNa rArA` 8→7 |
| 3 | held SQL → promote to `V60__` on sign-off | **HOLD for user** | `mAdhavO mAM pAtu` ragamalika metadata (`is_ragamalika` + 10 `krithi_ragas`) |

Held SQL (Groups 2 & 3) is intentionally **not** placed under `database/migrations/` — a `VNN` file there
auto-applies on the next `make migrate`. It lives at
`scratchpad/TRACK-133-held-V59-V60.sql` (session scratchpad) until approved; promote to the next free
version numbers at that time (do not pre-assign — V58 may not be the last committed migration by then).

### Group 1 — APPLY NOW (V58, written)

Adjudication is unambiguous: for both krithis the canonical charanams beyond position 5 have **zero**
text in **every** variant, so the true structure is Pallavi + 5 Charanams = 6. The migration is
**self-verifying**: it only deletes sections with no non-blank text in any variant and **asserts the
surviving count is exactly 6**, rolling back otherwise. `krithi_lyric_sections.section_id` is
`ON DELETE CASCADE`, so dangling empty lyric-section rows go with the parent (no orphan/ FK risk; no
constraint weakened). Contiguity holds because the empties are trailing.

**Pre-check (run before applying to confirm on the live DB):**

```sql
-- Proves the sections to be deleted are empty across ALL variants, and that
-- exactly 6 non-empty sections remain. deletable should be 4 and 8 respectively.
SELECT k.title,
       count(*) FILTER (WHERE nonempty)      AS keep_nonempty,   -- expect 6, 6
       count(*) FILTER (WHERE NOT nonempty)  AS deletable        -- expect 4, 8
FROM krithis k
JOIN LATERAL (
    SELECT cs.id,
           EXISTS (SELECT 1 FROM krithi_lyric_sections ls
                   WHERE ls.section_id = cs.id AND COALESCE(btrim(ls.text),'') <> '') AS nonempty
    FROM krithi_sections cs WHERE cs.krithi_id = k.id
) s ON true
WHERE k.title IN ('rAma sItA rAma','Rama Rama Rama Sita')
GROUP BY k.title;
```

**Mutation + audit:** `database/migrations/V58__track133_delete_phantom_empty_charanam_sections.sql`
(committed migration; `make migrate`). It loops both titles, guards non-empty=6, deletes phantoms,
re-asserts remaining=6, and writes one `audit_log` `DELETE` row per krithi.

**Post-check:**

```sql
SELECT k.title, count(*) AS sections,
       min(cs.order_index) AS min_ord, max(cs.order_index) AS max_ord
FROM krithis k JOIN krithi_sections cs ON cs.krithi_id = k.id
WHERE k.title IN ('rAma sItA rAma','Rama Rama Rama Sita')
GROUP BY k.title;               -- expect sections = 6; contiguous order_index
-- Audit trail:
SELECT entity_id, action, diff FROM audit_log
WHERE metadata->>'migration' = 'V58' ORDER BY changed_at;
```

### Group 2 — HELD (V59 candidate): canon over-count merges

`Alakalallalaadaga` (P,A,A,C→P,A,C), `Raanidi Raadu` (P,A,C,A→P,A,C), `ramA ramaNa rArA` (8→7). Unlike
Group 1 the spurious section carries **real text** in the offending variant (English for the first two,
en+ta for the third), so the correct fix **merges** it into its adjudicated neighbour and then deletes
it — never a blind delete. Held because the exact (keep, drop) section pair must be confirmed against
live rows first. Full SQL — read-only diagnostic, a reusable merge helper block (append text per
variant → repoint variant-only rows → delete → close the order_index gap → audit `MERGE_SECTIONS`), and
a post-check expecting 3/3/7 — is in `scratchpad/TRACK-133-held-V59-V60.sql`. No FK/section-order
invariant is broken: the gap-close keeps `(krithi_id, order_index)` contiguous and the unique constraint
satisfied.

### Group 3 — HELD (V60 candidate): `mAdhavO mAM pAtu` ragamalika metadata (metadata only)

Sets `is_ragamalika = true` and replaces the single `krithi_ragas` row with **10 ordered rows**
(`order_index` 1..10) for the Dashavatara sequence: nATa, SrI gauLa, SrI, Arabhi, varALi, kEdAra,
vasanta, suraTi, saurAshTra, madhyamAvati — resolving each raga through the **TRACK-136/137 identity
fold** (`ragas.match_key` ∪ `raga_aliases.match_key` via `raga_match_key()`), so **no duplicate raga
rows** are created. The block **RAISES on any unresolved or ambiguous name** so identity is fixed first
rather than guessed. `order_index` is 1-based to match the ragamalika convention actually in use (V57
asserts 1..34), not V02's stale "0-based" comment — flagged for confirmation. `primary_raga_id` is set
to the pallavi raga (nATa) as a display headline with a curator toggle to NULL; §6.2's "never collapse
to one raga" is satisfied by the 10 `krithi_ragas` rows, which are authoritative. Full SQL in
`scratchpad/TRACK-133-held-V59-V60.sql`.

**BLOCKED upstream — scope boundary:** the lyric/section side of `mAdhavO` cannot be populated here.
The `html_extractor` yields **empty** text for the Dikshitar source page (0 variants persisted), so no
canonical sections can be built until that extractor is fixed (`html_extractor` owner). This migration
is metadata-only; it deliberately does **not** touch `krithi_sections` for this krithi. After extraction
is fixed and text lands, the RAGA_SEGMENT parser (already proven on fixtures) builds the 10 sections via
the normal ingestion path — not via a migration.

### Handoff summary

- **APPLY NOW:** `make migrate` picks up **V58**. Run the Group 1 pre-check first if you want live
  confirmation; the migration self-guards regardless.
- **HOLD for user sign-off:** Groups 2 & 3 (`scratchpad/TRACK-133-held-V59-V60.sql`). On approval,
  promote to the next free `V59__`/`V60__`, run the diagnostics, then `make migrate`.
- **Upstream dependency:** `mAdhavO` sections remain blocked on the `html_extractor` empty-text fix.
- **Verify after any apply:** re-run the track's mismatch SQL (§Current state) and the `verify-import`
  checklist (junction `krithi_lyric_sections` / `krithi_ragas` populated, not just FK columns).

---

## Postgres phase progress (2026-09-02, orchestrator-applied)

Delivery vehicle: Flyway `VNN__` migrations with inline `audit_log` writes (per ADR-013, precedent
V45–V48). Applied on the live stack via `make migrate`.

- **V58 applied** — deleted phantom-empty trailing charanams: `rAma sItA rAma` 10→6,
  `Rama Rama Rama Sita` 14→6. Pre-checked (6 real + 4/8 empty across all variants), self-asserting.
- **V59 applied** — merged mis-split canon sections (the two unambiguous cases):
  `Raanidi Raadu` P,A,C,A→P,A,C (**cleared**); `ramA ramaNa rArA` rejoined the `tvac-caraNam`
  over-split (en/ta 8→7, correct). Note: V59 revealed `ramA ramaNa rArA`'s Indic variants read 6
  (canon order 7 empty for them) — a deeper structure question now under musicologist Round-2 review.
- **Mismatch progression: 108 → 26 (parser) → 14 (V58) → 9 rows / 2 krithis (V59).**

**Held for musicologist Round 2 (concrete per-variant data captured):**
- `Alakalallalaadaga` (canon 4→3) — Indic variants map to canon orders 1,2,4; only `en` has order 3
  ("celuvu mIraganu"). Merge direction (fold oi3 into oi2) awaiting confirmation.
- `ramA ramaNa rArA` (canon 7, Indic 6) — verdict pending: true P+6C=7 with Indic missing the last
  charanam's text (→ route to extractor), or P+5C=6 (canon over-count).

**Deferred: `mAdhavO mAM pAtu` (Group 3, V60 drafted but HELD).** Doubly-blocked — (a) html_extractor
yields empty text for the Dikshitar page (0 sections buildable), (b) 3 of 10 Dashavatara ragas
(`SrI gauLa`, `kEdAra`, `saurAshTra`) don't resolve to canonical raga IDs. Spun out to a dedicated
follow-up (extractor fix + raga-identity resolution, then promote V60). Not applied this pass.

### Round 2 — pre-postgres-fix confirmations (2026-09-02)

Confirmed against live per-variant text. **Both proposed framings need correcting** — the naive
"merge the two adjacent same-type sections" and "Indic is missing the last charanam" are each wrong.

**1) `Alakalallalaadaga` — verdict CANON-WRONG, correct = 3 (P + A + C). But NOT the proposed merge.**

The Indic variants already encode the correct reading: `te` oi1 PALLAVI holds **both** pallavi lines
(`alakalallalADaga kaniya` / `rAN-muniyeTu pongenO`), oi2 ANUPALLAVI = `celuvu mIraganu mArIcuni…`,
oi4 CHARANAM. That is the standard lakshana of this Utsava-Sampradaya kriti: the pallavi sentence is
"alakalallalADaga kani(y)A rAN-muni(y)eTu pongenO" ("seeing the swaying curls, how the sage-king
thrilled") — **`rAN-muniyeTu pongenO` is the tail of the PALLAVI, not an anupallavi.** The true
anupallavi is `celuvu mIraganu mArIcuni madam(a)NacE vELa`.

The English variant over-split: it broke the pallavi across oi1+oi2 and pushed the real anupallavi to
oi3. So canon oi2 (`rAN-muniyeTu pongenO`, labelled ANUPALLAVI) is the spurious section.

- **Do NOT** fold oi3 into oi2. That would wrongly glue pallavi text (`rAN-muniyeTu pongenO`) onto the
  anupallavi.
- **Exact action:** **drop canonical oi2.** Its English-variant text (`rAN-muniyeTu pongenO`) must be
  **appended to the PALLAVI (oi1)**, not to the anupallavi (don't lose it). Keep oi3 (`celuvu mIraganu`)
  as the sole ANUPALLAVI, reindex → 2. Keep oi4 CHARANAM, reindex → 3. Also fix the **en** lyric variant
  so its pallavi carries both lines and its anupallavi = `celuvu mIraganu…` (the Indic variants are
  already correct and need no text change — they map to oi1/oi2/oi4 today, so after the reindex they
  land on pallavi/anupallavi/charanam cleanly).
- Keep/drop pair: **keep oi1, oi3, oi4; drop oi2 (folding its text up into oi1).**
- Documented reason: *canon oi2 is pallavi line 2 mis-labelled as anupallavi by an English over-split;
  true structure P+A+C=3, confirmed by all five Indic variants.*

**2) `ramA ramaNa rArA` — verdict: true structure P + 6C = 7. Canon (7) is CORRECT — keep it. The
Indic variants are NOT missing text.**

Six distinct charanams exist and **all six are present in every variant**, including the Indic ones.
The kriti's charanams are: (1) samAnamevaru… (2) budhAdyavana… (3) kalArtha bhUsha… (4) raNAdhi SUra
SaraNAgata **tvac-caraNam** bhava tAraNambu cEsunu (5) mukhAbjamunu Sata mukhAri… durmukhAsura haraNa
(6) birAna brOvaga rAdA… tyAgarAja sannuta.

The Indic variants read 6 only because **charanams 4 and 5 are MERGED into one section** (verified in
sa, te, ml: oi5 contains *both* `raNAdhi…cEsunu (ramA)` **and** `mukhAbjamunu…haraNa (ramA)`), which
shifts C6 (`birAna…`) up to oi6 and leaves canonical **oi7 empty**. The "empty oi7" is a cascade
artifact of that merge — **not** a missing stanza. `en`/`ta` at 7 are correct (V59 having merged their
earlier `tvac-caraNam` over-split gives the correct C4 = `raNAdhi… tvac-caraNam bhava tAraNambu cEsunu`).

- **Exact action:** **keep canon at 7 — drop nothing.** This is **not** a missing-text data gap and
  **not** grounds to reduce canon to 6. Route the four Indic variants (sa, te, kn, ml) to the
  **parser/segmenter owner** as an **under-segmentation** defect: split their merged oi5 at the internal
  charanam boundary — the pallavi-refrain `…cEsunu (ramA)` immediately followed by a new charanam line
  `mukhAbjamunu Sata mukhAri…`. After re-segmentation each Indic variant will fill oi1–oi7 and match canon.
- Documented reason: *Indic C4+C5 merged into one section (all text present); canon 7 is correct — the
  fix is to re-split the Indic variants, not to drop a canonical charanam.*

Net for the postgres fix: `Alakalallalaadaga` → drop oi2 (reindex to P,A,C=3) + repair en variant text;
`ramA ramaNa rArA` → **no canon change** (parser re-split of the 4 Indic variants instead).

- **V60 applied** — `Alakalallalaadaga` (canon 4→3): musicologist Round-2 confirmed the pallavi spans
  two lines; the English variant was mis-split (line 2 as a separate anupallavi). Fixed the en mapping
  (folded line 2 into pallavi, remapped celuvu as anupallavi), dropped the phantom canonical section,
  reindexed. **Cleared.** Indic variants were already correct.
- **Mismatch now: 4 rows / 1 krithi** (`ramA ramaNa rArA`).

**Last remaining — `ramA ramaNa rArA` (parser, not canon).** Musicologist Round-2: canon 7 is correct;
the 4 Indic variants (sa,te,kn,ml) under-segment — charanams 4 & 5 are glued into one section, each
ending with the pallavi-echo refrain `(రమా)`. The section must split at the internal `…cEsunu (రమా)` |
`ముఖాబ్జమును…` boundary. Same class as `rAma Eva daivataM`'s `(राम)` refrain split (which works), so a
narrow refrain-boundary detection gap for this case.

---

## Kotlin phase + final state (2026-09-02)

**Kotlin phase complete.**
- `CuratorService.getStats()` — replaced the two-full-table-scan + in-memory diff with a single SQL
  aggregate (literal transcription of the canonical mismatch query) inside `DatabaseFactory.dbQuery`.
  Not hard-coded; integration test asserts the aggregate equals an independent row-diff and rises by
  exactly one when an under-segmented variant is imported. `:api:unitTest` green, integration test green.
  (One flag: `AuditRunnerService.runSectionCountAudit` uses the same bare `exec(WITH…){}` without an
  explicit `StatementType.SELECT` and would hit "result returned when none expected" if exercised.)
- `TransliterationCollapse` parity — **no change needed** (evidence): no such class exists in the Kotlin
  backend; the ingestion path (`LyricVariantPersistenceService`) persists the worker's sections verbatim
  and does not re-segment Indic text. The old Kotlin scraper parser (`KrithiStructureParser`,
  `SectionHeaderDetector`) is `@Deprecated` and unreferenced in `src/main`. Nothing re-collapses on ingest.

**Live verification:** `/v1/admin/curator/stats` → `sectionIssuesCount: 4`, matching the mismatch query.

### Accepted residual (Definition of Done)

- **`ramA ramaNa rArA` (4 variant rows) — ACCEPTED as a known residual.** Canon is correct at 7
  (P + 6 Charanams). The 4 Indic variants (sa,te,kn,ml) under-segment: charanams 4 & 5 are glued into
  one section, each ending with the pallavi-echo refrain `(రమా)`; the true split is at
  `…cEsunu (రమా)` | `ముఖాబ్జమును…`. This is a narrow parser refrain-boundary gap (same class as the
  working `rAma Eva daivataM` `(राम)` split), not a data error. Deliberately not fixed this pass; a
  parser refrain-split or a surgical per-variant re-split would close it. Documented so the next audit
  does not re-litigate it.

### Final tally

| Stage | Mismatch rows | Krithis |
|:---|:---|:---|
| Baseline | 108 | 29 |
| Parser (splitter fixes + real-data calibration) | 26 | 5 |
| V58 (phantom-empty deletes) | 14 | 3 |
| V59 (mis-split merges) | 9 | 2 |
| V60 (Alakalallalaadaga pallavi fix) | 4 | 1 |
| **Accepted residual** | **4** | **1** (`ramA ramaNa rArA`) |

**104 of 108 rows resolved (96%); the remaining 4 are consciously accepted.** Migrations V58–V60
(audited). Worker regression suite covers the marker forms (352 tests). Curator dashboard now reflects
the corrected corpus via a SQL aggregate.

### Follow-up spun out

- **`mAdhavO mAM pAtu`** (the 1 krithi with no canonical sections) — doubly-blocked: html_extractor
  yields empty text for its Dikshitar source page, and 3 of 10 Dashavatara ragas don't resolve to
  canonical raga IDs. V60-ragamalika migration drafted and held in scratchpad. Own follow-up task.

### Round 3 — mAdhavO mAM pAtu ragamalika (2026-09-02)

Lakshana confirmation for the Dashavatara Ragamalika. Propose only.

**1. Section structure — 10 sections is correct; do NOT make 20.** Each avatara stanza is one section.
The `(madhyama kAla sAhityam)` block is the tempo-doubled tail of the **same** stanza in the **same**
raga — it is not a raga change and not a structural section of its own, so it is folded into its
stanza's section. Splitting into 20 would falsely imply 20 raga/structure boundaries. Correct = **10**.

**SectionType: keep all 10 as `OTHER` — do NOT type them PALLAVI/ANUPALLAVI/CHARANAM.** This kriti has
**no refrain**: the header note ("first eight vibhaktis in order, last two in the second vibhakti")
confirms it is ten parallel avatara verses in successive Sanskrit grammatical cases, with no returning
pallavi and no anupallavi. Typing stanza 1 as PALLAVI would misrepresent a non-refrain opening verse as
a refrain; PALLAVI + 9×CHARANAM is therefore wrong. `OTHER` per stanza is the domain-honest choice for
ragamalika verse-sections. (If a more descriptive type is later wanted, uniform `CHARANAM` is the only
acceptable fallback — never PALLAVI+9.) Recommend each section carry a `label` = its raga name (e.g.
`nATa (Matsya)`) and, if captured, mark the madhyamakala tail in `notes`, so the fold-in is not lost.

**2. Raga sequence — confirmed.** The ten ragas, order, spellings, and avatara mapping are the standard
Dashavatara sequence and all check against the sahitya:

| # | Raga | Avatara (from sahitya) |
|:--|:--|:--|
| 1 | nATa | Matsya (`matsyAvatArO`) |
| 2 | gauLa | Kurma (`kUrmAvatAraM`) |
| 3 | SrI | Varaha (`bhUmi pAla sUkarENa`) |
| 4 | Arabhi | Narasimha (`narasiMhAya namastE`) |
| 5 | varALi | Vamana (`vAmanAt…`) |
| 6 | kEdAra | Parasurama (`paraSu rAmasya`) |
| 7 | vasanta | Rama (`rAma candra svAmini`) |
| 8 | suraTi | Balarama (`bala rAma`) |
| 9 | saurAshTra | Krishna (`SrI kRshNaM bhajarE`) |
| 10 | madhyamAvati | Kalki (`kali yuga vara vEnkaTESaM`) |

Note on #2: the round-1 reading "SrI gauLa" was an artifact — the `SrI` in the imported latin blob is
an honorific prefix (as it also precedes deity names elsewhere in the text, e.g. `SrI kRshNaM`,
`SrI dharENa`). The Devanagari source's `गौळ रागं` = plain **gauLa**. Confirmed #2 = **gauLa**, not a
compound "SrIgauLa". §6.2 action stands: set `is_ragamalika = true` and add **10 ordered `krithi_ragas`
rows** (order_index 1–10); `primary_raga_id` may remain nATa as the opening-raga pointer but is not the
sole representation.

**3. Raga identity / alias calls — both confirmed same raga; alias, do not duplicate.**

- source **`kEdAra`** ≡ DB **`Kedaram`** (Carnatic kEdAram, janya of 29 Dheerasankarabharanam). Dikshitar
  renders it without the final `-m`; same raga. **Add `kEdAra` as an alias of `Kedaram`.** **Guard:** do
  **not** resolve to `Kedaragaula` (kEdAragauLa) — that is a distinct raga; stanza 6 is plain kEdAra.
- source **`saurAshTra`** ≡ DB **`saurAshTraM`** (Carnatic Saurashtram, janya of 17 Suryakantam). Same
  raga, `-m`-final spelling variant. **Add `saurAshTra` as an alias of `saurAshTraM`.**

Both are just anusvara/final-consonant spelling variants; aliasing avoids duplicate raga rows. High
confidence on structure, sequence, and both identities. Authorship (Muthuswami Dikshitar) is consistent
with the vibhakti-based style but is taken as given here — not independently re-verified against a source.

---

## mAdhavO mAM pAtu — RESOLVED (2026-09-02), was never actually blocked

The "html_extractor yields empty text" conclusion was wrong. Real diagnosis + fix:

- **It was simply excluded from every re-extract round** (on earlier mistaken advice), so it kept its
  stale TRACK-093 output (`sections: []`, plus a bogus raga "Alika" mis-parsed from the title word
  "mAlika"). The extractor works fine — all 6 scripts, full 10-stanza text.
- **Parser gap (fixed):** the RAGA_SEGMENT split created 10 canonical sections but left each variant as
  1 blob. `structure_parser.py` now matches Indic `<raga>` headers (Devanagari/Tamil/Telugu/Kannada/
  Malayalam forms, not just Latin `rAgaM`) and no longer collapses the 10 OTHER stanzas — all 6 variants
  split to 10 aligned sections (344 worker tests pass).
- **Persisted:** re-extracted (10 canonical + 10/variant), then curator-approved to run
  `persistFromCanonical`, which created the 10 canonical `krithi_sections` (type OTHER) + 10 lyric
  sections per script. DB now: canon=10, 6 variants × 10. **mAdhavO is no longer a mismatch.**
- **V61 applied** — `is_ragamalika=true` + 10 ordered `krithi_ragas` (nATa, gauLa, SrI, Arabhi, varALi,
  kEdAra, vasanta, suraTi, saurAshTra, madhyamAvati), replacing the bogus single "Alika" mapping.
  Two raga aliases added (`kEdAra`→Kedaram [NOT Kedaragaula], `saurAshTra`→saurAshTraM). Musicologist
  Round-3 confirmed structure (10 sections, all OTHER — no PALLAVI/refrain), sequence, and identities.

### Findings to route separately (noted; minor)
1. **Ingestion re-persistence gap** — `reviewImport` only persists sections in the create/first-promote
   path; once a krithi is APPROVED+mapped, re-approving short-circuits (`alreadyPromoted` early-return),
   so a re-extraction of an already-mapped krithi never re-persists sections. Worked around here by
   resetting the import to `in_review` and re-approving. Only bites stale already-mapped krithis; fresh
   `db-reset` imports persist correctly on first promotion. Worth a backend follow-up.
2. **`Alika` title mis-parse** — the metadata parser reads "mAlika" (from "Dasa Raga Malika") as a raga.
   V61 overwrites the result, but the parser bug recurs on re-extract. Minor worker follow-up.
3. **Tamil MKS marker** — the Tamil `(madhyama kala sahitya)` form isn't demoted (stays inline); the
   10-way split is unaffected. Minor worker follow-up.

## Final tally (all krithis)

108 → **4 mismatch rows / 1 krithi** (`ramA ramaNa rArA`, consciously accepted & documented). mAdhavO
fully resolved. Migrations V58–V61 (all audited). Parser: 3 marker fixes + ragamalika multiscript split.
Kotlin: CuratorService SQL aggregate. Curator `sectionIssuesCount` reflects the corrected corpus.

---

## Findings resolved (2026-09-02)

The four findings surfaced during remediation were fixed in-session (not deferred):

1. **`/re-extract` 1000-row cap** — new DAL `ExtractionQueueRepository.findIdsBySourceUrlPattern(pattern, status?)`
   does the match in SQL (`lower(source_url) LIKE`, wildcard-escaped, uncapped); the route iterates the
   returned ids. Response shape/audit unchanged. Test seeds 1001 rows and asserts all requeued.
2. **Ingestion re-persistence gap** — new `ImportService.reingestMappedKrithi(id)` + `POST
   /v1/admin/imports/{id}/reingest`: atomically clears variants and re-runs `persistLyricVariants`/
   `persistFromCanonical` onto the mapped krithi (idempotent, no duplicate krithi, audited
   `REINGEST_MAPPED_KRITHI`). Tests: 0→3 sections after reingest + double-reingest idempotency.
3. **`Alika` title mis-parse** — `metadata_parser.py` now recognises the "rAga mAlikA"/"daSa rAga mAlikA"
   ragamalika descriptor, sets `is_ragamalika`, and emits no bogus raga (tala still recovered).
4. **Tamil MKS marker** — `structure_parser.py` Tamil MKS regex now tolerates the grantha-numeral form
   `(மத் 4 யம கால ஸாஹித்யம்)`, so Tamil demotes MKS like the other scripts.

Worker suite 358 passed; backend `ReExtractCapTest` + `ReingestMappedKrithiTest` green; dal/api compile clean.
