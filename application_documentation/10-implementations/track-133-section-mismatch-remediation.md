| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-09-05 |
| **Author** | Sangeetha Grantha Team |

# Section-Count Mismatch Remediation (TRACK-133)

Close the residual Trinity-import section-count mismatches (29 krithis, 108 variant rows, all “fewer than canon”) and leave a durable worker parser so a later re-extract cannot reintroduce the last glue.

Working log: [TRACK-133](../../conductor/tracks/TRACK-133-section-mismatch-remediation.md). Domain rules: [Domain Model §6](../01-requirements/domain-model.md#6-musicological-correctness-rules-lakshana). Architecture boundary: [ADR-012](../02-architecture/decisions/ADR-012-unified-extraction-architecture.md) — Python segments, Kotlin persists, Curator reviews.

## Outcome

| Stage | Mismatch rows | Krithis |
|:---|:---|:---|
| Trinity import residue (TRACK-093) | 108 | 29 |
| Parser (Indic swara / charanam / ragamalika markers) + re-extract | 26 | 5 |
| Flyway V58–V61 (canon repairs + `mAdhavO` ragamalika metadata) | 4 | 1 |
| Flyway V62 (snapshot re-split of `ramA ramaNa rArA`) | **0** | **0** |
| Durable pallavi-echo parser (2026-09-05) | **0** (and a fresh missing-C5 extract stays 0) | **0** |

Live mismatch query (dev, 2026-09-05): **0 rows**. Corpus size unchanged at **1,226** krithis. Schema current at **V62**. Curator `sectionIssuesCount` is a SQL aggregate of that same query.

## Root causes (not 29 independent bugs)

Most DB “1 section per Indic variant” rows were **stale pre-TRACK-100 import residue** and cleared on re-extract. Remaining gaps were:

1. **Splitter** — Indic inline `sva`+ordinal swara markers; bare / full-word / digit-without-period charanam markers; Indic ragamalika `<raga>` headers.
2. **Canon over-count** — phantom empty charanams; English pallavi/charanam mis-splits treated as extra canonical sections.
3. **Missing-heading glue** — `ramA ramaNa rArA` Indic C4+C5 merged because C5 headings were absent; each stanza already closed with a parenthesised pallavi echo `(రమా)` / `(रमा)`.

Bucket C lakshana judgements (correct section *count*) stayed with the musicologist; the worker never invents or drops sections to hit a number.

## Durable pallavi-echo split

`StructureParser._split_charanam_pallavi_echoes` runs in `_sections_from_variant_blocks` **after** ragamalika prefix merge and **before** canonical type-queue mapping.

It fires only when all of the following hold:

- The variant has a **charanam deficit** versus canon.
- The variant Pallavi is Indic and ends with a parenthesised echo that is also the Pallavi’s opening word.
- **Exactly one** `CHARANAM` block contains internal line-final copies of that echo (plus a closing echo on its last line).
- That block’s extra stanza count **equals the deficit**.
- The line after each cut is the same script.

Otherwise the blocks are left unchanged. The echo stays on the stanza it closes. Already-aligned variants, Latin text, Anupallavi bodies, inline parentheses, terminal-only refrains, and ragamalika `OTHER` stanzas are no-ops. No composition title, database id, or V62 dependency is in the parser.

V62 remains the **imported-snapshot** repair for rows already in the DB. The parser is what a future re-extract uses when Indic C5 headings are missing.

### Tests and fixtures

| Path | Role |
|:---|:---|
| `tools/krithi-extract-enrich-worker/tests/test_pallavi_echo_split.py` | Indic C4/C5/C6 restore (language-header and TRACK-100 post-boundary), English/Tamil unchanged, explicit-heading guard, ordinary/ambiguous refrains, Anupallavi unsplit body, mixed true+false cuts across blocks, two missing headings in one glued block, stable re-parse |
| `tests/fixtures/structure_parser/rama_ramana_rara_echo.md` | Provenance (live HTML SHA-256; merged fixture is a controlled C5-prefix strip, not an original failing snapshot) |
| `rama_ramana_rara_internal_echo.txt` / `_explicit_charanams.txt` / `_echo.expected.json` | Merged vs intact captures |

Worker suite after this follow-up: **386 passed**. `ruff` and `mypy .` (62 files) clean.

The current live Thyagaraja Vaibhavam page already has C5 headings and parses to 7 without this repair. The merged fixture is the documented failure shape.

## Corpus migrations (already applied)

All write `audit_log`. They are no-ops if the target krithi is absent (`make db-reset` before corpus load).

| Migration | Repair |
|:---|:---|
| `V58__track133_delete_phantom_empty_charanam_sections.sql` | Empty trailing charanams: `rAma sItA rAma` 10→6, `Rama Rama Rama Sita` 14→6 |
| `V59__track133_merge_missplit_canon_sections.sql` | `Raanidi Raadu` P,A,C,A→P,A,C; `ramA ramaNa rArA` `tvac-caraNam` over-split |
| `V60__track133_fix_alakalallaladaga_pallavi_missplit.sql` | `Alakalallalaadaga` English pallavi line-2 mislabelled as anupallavi → P+A+C |
| `V61__track133_madhavo_ragamalika_metadata.sql` | `mAdhavO mAM pAtu`: `is_ragamalika`, 10 ordered `krithi_ragas`, aliases `kEdAra`→Kedaram and `saurAshTra`→saurAshTraM |
| `V62__track133_ramaramanarara_indic_charanam_resplit.sql` | Indic C4+C5 snapshot split at the pallavi echo |

See [migrations.md](../04-database/migrations.md).

## Kotlin (already in PR #19)

- `CuratorService.getStats()` — SQL aggregate of the mismatch query inside `DatabaseFactory.dbQuery` (replaces two full-table scans + in-memory maps).
- No `TransliterationCollapse` mirror: ingestion (`LyricVariantPersistenceService`) persists worker sections verbatim. The deprecated Kotlin scraper parser is unreferenced in `src/main`.

Also in this track: uncapped `/re-extract` by source URL, `POST /v1/admin/imports/{id}/reingest` for already-mapped krithis, ragamalika-descriptor handling so “mAlika” is not a bogus raga, and Tamil madhyamakala-numeral demotion.

## Boundary

The worker does not write canonical tables. Re-extract / reingest goes through Kotlin. Do not treat V62 as a substitute for the parser, and do not treat the parser as a reason to rewrite V62.
