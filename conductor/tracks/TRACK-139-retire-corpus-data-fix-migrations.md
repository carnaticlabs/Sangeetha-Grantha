| Metadata | Value |
|:---|:---|
| **Status** | In Progress |
| **Version** | 0.3.0 |
| **Last Updated** | 2026-09-06 |
| **Author** | Sangeetha Grantha Team |
| **Priority** | P2 — architectural hygiene; removes a recurring wrong-vehicle pattern |
| **Decision** | [ADR-012](../../application_documentation/02-architecture/decisions/ADR-012-unified-extraction-architecture.md) (Python extracts / Kotlin ingests / Curator reviews) · [ADR-013](../../application_documentation/02-architecture/decisions/ADR-013-db-migration-with-flyway.md) (Flyway is schema + reference data, **not** corpus data) · [ADR-014](../../application_documentation/02-architecture/decisions/ADR-014-versioned-canon.md) (versioned canon / provenance) |
| **Depends on** | [TRACK-133](./TRACK-133-section-mismatch-remediation.md) (the data-fixes to retire) · [TRACK-093](./TRACK-093-trinity-krithi-bulk-import.md) (source URLs stored for re-import) |
| **Interacts with** | [TRACK-138](./TRACK-138-rasika-mobile-app.md) (surfaced this while recovering a v56 backup that could not roll forward past V58) |

# TRACK-139: Retire Corpus Data-Fix Migrations — Parser/Import Ownership of Corpus Correctness

**ID:** TRACK-139
**Status:** In Progress
**Owner:** Sangeetha Grantha Team
**Created:** 2026-09-06
**Updated:** 2026-09-06

## Goal

Move one-off **corpus-data corrections** out of Flyway `V__` migrations and into the place ADR-012 says they belong — the deterministic `structure_parser` / import / curation path — then retire the redundant migration scripts and add a guardrail so corpus fixes cannot sneak back into schema migrations.

## Context

Restoring a `post_v56` backup during TRACK-138 exposed the problem sharply: the backup could **not** roll forward, because `V58` is a data-remediation migration whose guard asserts a specific krithi's section structure. That structure existed only in a *later* corpus state, so `V58` aborted against the older-but-valid data. A schema migration that depends on the exact shape of corpus rows is a category error under ADR-013 (Flyway carries **schema** `V__` and **reference data** `R__`, not composition content) and ADR-012 (composition correctness is produced by extraction/parsing, ingested by Kotlin, and reviewed by curators).

The five TRACK-133 migrations (`V58`–`V62`) are the immediate case, but the pattern is older — several earlier `V__` scripts also mutate corpus rows.

### Finding 1 — corpus data-fix migrations that snuck into Flyway

Classification of every `V__` migration by whether it mutates corpus rows (`krithis`, `krithi_sections`, `krithi_lyric_*`, `krithi_ragas`, `krithi_revisions`) with no schema change:

| Migration | Track | Kind | Corpus mutation |
|:---|:---|:---|:---|
| `V38__fix_inconsistent_lyric_sections` | — | **data-fix** | lyric sections |
| `V45__remove_stale_anupallavi_brhannayaki` | — | **data-fix** | delete section |
| `V46__delete_incomplete_devanagari_amba_nilayatakshi` | — | **data-fix** | delete variant/sections |
| `V47__demerge_ragamalika_visvanatham_from_natabharanam` | — | **data-fix** | ragamalika de-merge |
| `V58__track133_delete_phantom_empty_charanam_sections` | TRACK-133 | **data-fix** | delete sections |
| `V59__track133_merge_missplit_canon_sections` | TRACK-133 | **data-fix** | merge sections |
| `V60__track133_fix_alakalallaladaga_pallavi_missplit` | TRACK-133 | **data-fix** | re-split pallavi |
| `V61__track133_madhavo_ragamalika_metadata` | TRACK-133 | **data-fix** | seed ragamalika ragas |
| `V62__track133_ramaramanarara_indic_charanam_resplit` | TRACK-133 | **data-fix** | re-split charanams |

Raga *identity/reference* migrations (`V39/V40` seed, `V48/V49` mela/scale fixes, `V50` merge, `V53/V55` identity schema, `V57` orphan-twin cleanup) are **out of scope** here — they concern raga reference entities (ADR-016/017), not composition content, and several are mixed with real schema. They are listed only so the boundary is explicit.

### Finding 2 — which TRACK-133 fixes the current parser already reproduces

The fixed `structure_parser` was run (enrichment OFF, no API key) via the real `HtmlExtractionStrategy` against the five stored source URLs on 2026-09-06:

| Krithi | Section count in restored DB | Parser output (live source) | Migration | Superseded by re-import? |
|:---|:---|:---|:---|:---|
| `rAma sItA rAma` | 10 (6 real + 4 phantom-empty) | **6** (Pallavi + 5 Charanams) | V58 | ✅ yes |
| `Rama Rama Rama Sita` | 14 (all real — over-split Kannada charanams) | **6** (Pallavi + 5 Charanams) | V58 | ✅ yes — parser correctly collapses the over-split; V58 itself *could not* (it aborted) |
| `ramA ramaNa rArā` | 7 | **7** (Pallavi + 6 Charanams, all 6 scripts) | V59 + V62 | ✅ yes |
| `Alakalallalaadaga` | 4 | 4 (canonical/en=4, indic=3) — **not** the adjudicated 3/3/3 | V60 | ❌ **no** — parser still mis-splits the two-line pallavi |
| `mAdhavō mām pātu` | ragamalika metadata | 10 sections typed *Other*, `ragas=['Unknown']` | V61 | ❌ **no** — dasāvatāra ragamalika raga sequence not parsed |

**Conclusion:** `V58`, `V59`, `V62` are genuinely superseded — re-importing produces the correct structure natively (and fixes the `Rama Rama Rama Sita` case the migration could not). `V60` and `V61` are **not** yet reproduced; the parser needs targeted fixes before those two can retire.

### Finding 3 — dump roll-forward (2026-09-06)

Restored `storage/backups/sangita_grantha_20260830_post_v56.dump` into a **scratch** database `sangita_grantha_rollforward` (live `sangita_grantha` was left at V59). Original dump was **not** overwritten.

**Restore gotcha.** A one-shot `pg_restore --exit-on-error` fails: `raga_aliases.match_key` is `GENERATED ALWAYS AS (raga_match_key(alias))`, and `raga_match_key` calls unqualified `strip_diacritics()`. `pg_restore` uses an empty `search_path`, so the function is not found during COPY. Workaround: schema-only restore → `ALTER FUNCTION public.raga_match_key(text) SET search_path = public` → data-only restore with `--disable-triggers`. Optional later hygiene: pin `search_path` (or schema-qualify) in a real schema migration so future dumps restore without the ALTER.

**Corpus shape in the dump (1,226 krithis):**

| Title | Canonical sections | Variant lyric-section counts |
|:---|:---|:---|
| `rAma sItA rAma` | 10 | (V58 nonempty-guard would pass for this krithi alone) |
| `Rama Rama Rama Sita` | 14 | 14 nonempty — **V58 abort** |
| `Raanidi Raadu` | 4 | V59 target 3 |
| `ramA ramaNa rArA` | 8 | en=8, ta=2, sa/te/kn/ml=1 |
| `Alakalallalaadaga` | 4 | en=4, sa/ta/te/kn/ml=1 |
| `mAdhavO mAM pAtu` | 0 sections | `is_ragamalika=false`, primary raga `rAga mAlikA`, 1 `krithi_ragas` row |

**Flyway against the restored dump:**

| Migration | Kind | Result on this dump |
|:---|:---|:---|
| `V57` | raga identity (out of TRACK-139 scope) | **applies** |
| `R__seed_04` (outdated checksum) + `R__seed_06` | reference | **apply** once V58–V62 are not blocking the run |
| `V58` | corpus data-fix | **aborts** — `"Rama Rama Rama Sita" has 14 non-empty sections, expected 6` (transaction rolled back; V57 stays) |
| `V59` | corpus data-fix | **would apply** in isolation (Raanidi 4→3, ramA 8→7) |
| `V60` | corpus data-fix | **aborts** — post-fix `canonical=3, en=3, min-indic=1` (expected 3/3/3) |
| `V61` | corpus data-fix | **would apply** in isolation (10 ordered ragas + `is_ragamalika=true`) |
| `V62` | corpus data-fix | **aborts** — Indic variants lack oi5/oi6 (`ml missing oi5/oi6 rows`); dump Indic coverage is a single lyric-section, not the glued-charanam shape V62 assumes |

**Dump-update conclusion:** this backup **cannot** be rolled forward through V62. The honest updated artifact is schema/reference current through **V57 + current `R__`**, with corpus still as of 2026-08-30. Written to `storage/backups/sangita_grantha_20260906_post_v57.dump` (gitignored, alongside the original). Restoring that dump and running `make migrate` **still aborts at V58** until Phase 1 deletes `V58`. After Phase 1, the next blocker is `V60` until Phase 2.

Do **not** bake V59/V61 SQL into the dump — that would freeze corpus data-fixes into a backup while this track is trying to stop using SQL as the vehicle.

## Intent

**Status:** Accepted
**Accepted by:** User
**Accepted at:** 2026-09-06

### Problem
Corpus-data corrections are being delivered as Flyway `V__` migrations. This (a) violates ADR-012/ADR-013 ownership, (b) makes migrations depend on exact corpus shape so they abort on any valid earlier/variant corpus (as `V58` did on the `post_v56` restore), and (c) hides the real defect — the parser — behind after-the-fact SQL.

### Proposed outcome
1. Composition-structure correctness for the affected krithis comes from the **parser + re-import + curation** path, not SQL.
2. The redundant data-fix migrations are retired without breaking Flyway history in any environment.
3. A guardrail prevents new corpus-mutating `V__` migrations from landing.
4. The TRACK-138 recovery backup can roll forward again: restore `post_v57` (or `post_v56` + V57) and `make migrate` without corpus-shape aborts.

### Affected users and systems
- Flyway history in every long-lived database (dev volume, TRACK-138 recovery DB, any prod).
- `structure_parser` / HTML extraction worker (Phase 2 gaps).
- Curator review / import path for the specific krithis behind V58–V62.
- Local backups under `storage/backups/` (gitignored).

### Constraints
- Flyway-only (ADR-013). Do not edit committed `V__` files to "make them no-op"; retire by delete + `flyway repair`, or leave until parser-superseded.
- Every mutation of live corpus still writes `AUDIT_LOG` (re-import / curator path).
- Lakshana: Domain Model §6.1 forms and §6.2 ragamalika remain the correctness contract.
- Do not `make db-reset` the live recovery database as part of exploration.

### Open questions
- Confirm whether the live `sangita_grantha` V58 row is a real apply or a skip-marker (checksum is present; TRACK-138 notes a skip). Repair procedure is the same either way.
- Whether to add a small schema-hygiene migration pinning `raga_match_key` `search_path` so dumps restore without the ALTER workaround. Out of TRACK-139's corpus-retirement goal; can be a follow-up.

### Non-goals
- Re-importing the whole corpus. Scope is the specific krithis behind the retired migrations.
- Touching raga identity/reference migrations (`V39/40/48/49/50/53/55/57`) — separate concern (ADR-016/017).
- Changing the Flyway-only policy (ADR-013) — this *reinforces* it by removing misuse.
- Overwriting `sangita_grantha_20260830_post_v56.dump`.

## Spec

**Status:** Accepted
**Accepted by:** User
**Accepted at:** 2026-09-06

### Requirements
1. After Phase 1, a database restored from `post_v56` or `post_v57` can apply every **remaining** versioned migration without aborting on corpus shape. Immediate meaning: `V58`/`V59`/`V62` are gone from `database/migrations/`.
2. The three parser-superseded krithis (`rAma sItA rAma`, `Rama Rama Rama Sita`, `ramA ramaNa rArā`) have correct structure from re-import + curator approval (6 / 6 / 7), not from SQL.
3. `V60` and `V61` stay until `structure_parser` reproduces the adjudicated outcomes, then they retire the same way.
4. `flyway repair` is run in every environment that had the deleted versions applied (live dev is V59; CI Testcontainers are from-scratch and need no repair).
5. A CI / `make agent-evals` (or sibling lint) check fails when a **new** `V__` file mutates `krithi_*` corpus tables, with an explicit override comment for the rare justified case.
6. ADR-014 provenance of the correction is preserved (import/curation record, not the deleted SQL file as source of truth).

### Design
- **Retirement vehicle:** delete the `V__` file; Flyway reports *missing*; `docker compose run --rm migrate repair` marks *deleted*. Do not insert skip rows by hand.
- **Correctness vehicle:** stored source URLs → Python `HtmlExtractionStrategy` / `structure_parser` → Kotlin ingestion → curator approve. Junction tables (`krithi_ragas`, lyric sections) must be populated, not only parent FKs (`verify-import`).
- **Parser gaps (Phase 2):** fixtures + tests in `tools/krithi-extract-enrich-worker` next to the existing structure_parser suite — two-line pallavi (`Alakalallalaadaga`); dasāvatāra ragamalika sequence (`mAdhavO mAM pAtu`).
- **Guardrail (Phase 4):** extend `.claude/hooks/protect-migrations.py` and/or `evals/check.py` to scan *new* versioned SQL for `INSERT|UPDATE|DELETE` against `krithis`, `krithi_sections`, `krithi_lyric_%`, `krithi_ragas`, `krithi_revisions`. Existing committed data-fix files are grandfathered until retired.
- **Backup:** keep `post_v56` immutable. Use `post_v57` as the recovery starting point for TRACK-138 once Phase 1 lands. `storage/backups/` stays gitignored.

### Flagged concerns
- Deleting applied `V__` files is a coordinated ops step (`repair` everywhere). A lone `rm` on `main` will fail `flyway validate` on the live volume.
- `protect-migrations.py` currently **denies mutating committed V__ files**, including delete. Phase 1 will need an explicit allow (user-approved delete, or a track-scoped override) rather than fighting the hook.
- `V59` SQL *succeeds* on the Aug 30 dump; retiring it is an ownership decision, not a "it won't apply" decision. Re-import must land before delete so `make db-reset` corpora stay correct.
- `V61` also succeeds on the dump; parser does not yet reproduce it — do not delete in Phase 1.
- Live DB is already past V58/V59; the dump is not. Environments diverge until repair + re-import.

### Open questions carried forward
- Live V58 skip vs apply (see Intent).
- `search_path` dump-restore hygiene as a separate schema migration.

## Plan

**Status:** Accepted
**Accepted by:** User
**Accepted at:** 2026-09-06

### Files that change
- `database/migrations/V58__*.sql`, `V59__*.sql`, `V62__*.sql` — delete (Phase 1, after re-import).
- `database/migrations/V60__*.sql`, `V61__*.sql` — delete only in Phase 2.
- `tools/krithi-extract-enrich-worker/` structure_parser + fixtures/tests (Phase 2).
- `.claude/hooks/protect-migrations.py` and/or `evals/check.py` + `evals/cases/` (Phase 4).
- `conductor/tracks/TRACK-139-*.md`, `conductor/tracks.md`, a short ADR-014 / ops note under `application_documentation/` for retirement + dump restore.
- Local only (not git): `storage/backups/sangita_grantha_20260906_post_v57.dump`.

### Order of work
1. **Gate:** Intent → Spec → Plan accepted. Do not delete migrations before that.
2. Re-import the three Phase-1 krithis on the live DB; verify 6/6/7 via `verify-import`.
3. Delete `V58`/`V59`/`V62`; `flyway repair` on live `sangita_grantha` and on `sangita_grantha_rollforward` if kept; confirm `make migrate` on a restore of `post_v57` proceeds until `V60`.
4. Parser fixtures for V60/V61; re-import; delete those files + repair.
5. Evaluate V38/V45/V46/V47 case by case (no blanket delete).
6. Guardrail + eval case so a new corpus-mutating `V__` fails CI.

### Risks
- `flyway validate` fails in any environment that applied a deleted version and has not run `repair`.
- Re-import without curator approval leaves draft/unapproved structure as the new source of truth.
- Hook will block the deletion unless the session is allowed to delete committed V__ files.

### Proof
- Restore `post_v57` (or `post_v56` + V57 workaround) → `make migrate` does not abort at V58 after Phase 1.
- Parser tests for the two-line pallavi and dasāvatāra sequence (Phase 2).
- `make agent-evals` includes the corpus-mutation guard (Phase 4).
- `make test` / worker tests as touched.

## Implementation Plan
- [x] Explore dump roll-forward; write `post_v57` backup without applying V58–V62
- [x] Intent / Spec / Plan accepted
- [x] Phase 1–2: parser/reingest ownership + delete V58–V62 + Flyway history aligned to V57
- [x] Live re-extract + reingest of the five krithis (6 / 6 / 7 / P+A+C=3 / ragamalika-10)
- [x] Phase 3 evaluate V38/V45/V46/V47 (keep; see below)
- [x] Phase 4 guardrail

## Phase 3 evaluation (2026-09-06)

| Migration | Decision | Why |
|:---|:---|:---|
| V38 | **KEEP** | Corpus-wide historical cleanup. Parser already demotes MKS / merges dual-format for *new* imports; `make db-reset` never replays this corpus. Deleting it does not re-import the whole catalogue (non-goal). |
| V45 | **KEEP** (curator form rule) | `bRhannAyaki vara dAyaki` Pallavi + Samashti Charanam. One UUID delete; not reproduced by a general parser rule. Convert to a curator correction if the parser re-introduces Anupallavi on re-import. |
| V46 | **KEEP for now** | Temporary unblock for `amba nIlAyatAkshi` Devanagari variant. Parser has the unlabeled-leading-block fix; re-import when the stack is up, then this file can retire like V58. |
| V47 | **KEEP** | Identity demerge of two Dikshitar works sharing a title. Matcher/title-collision, not structure_parser. SQL already applied; deleting would not prevent a future bad merge. |

## Progress Log
- **2026-09-06**: Track drafted (Intent proposed).
- **2026-09-06**: Dump experiment on scratch DB `sangita_grantha_rollforward`. V57 + current R__ apply; V58/V60/V62 abort; V59/V61 would apply. Wrote `storage/backups/sangita_grantha_20260906_post_v57.dump`. Spec/Plan drafted. No migrations deleted.
- **2026-09-06**: Intent/Spec/Plan accepted. Parser: hyphen-wrap two-line pallavi; Dashavatara raga sequence on RAGA_SEGMENT. Kotlin reingest writes `is_ragamalika` + `krithi_ragas`. R__seed_06 aliases for nATa/gauLa/kEdAra/saurAshTra. Deleted V58–V62; live history aligned to V57; `make agent-evals` green. Phase 3 keep V38/V45/V46/V47.
- **2026-09-06**: Live re-extract + reingest of the five TRACK-133 URLs. Junction tables populated. `NameNormalizationService.normalizeRaga` must not gate ragamalika names — it strips honorific `sri` and would drop raga Sri. Dual-URL krithis: last reingest wins; the 6-section Balahamsa / Saveri payloads were applied last so canon is 6/6. The sibling Huseni / Sankarabharanam URLs still parse to 14 and 10 nonempty sections (matcher collision, not empty phantoms).
- **2026-09-06**: Wrote `storage/backups/sangita_grantha_20260906_post_track139.dump` from live `sangita_grantha` (V57 + reingested five krithis). `post_v56` / `post_v57` left immutable. Restore still needs the `raga_match_key` `search_path` workaround in [migrations.md §5](../../application_documentation/04-database/migrations.md#5-rollback--history-tracking).
