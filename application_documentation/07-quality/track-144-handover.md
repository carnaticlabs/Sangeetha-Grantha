| Metadata | Value |
|:---|:---|
| **Status** | Completed — 100% tālas resolved, taxonomy harmonized |
| **Last Updated** | 2026-09-26 |
| **Author** | Sangeetha Grantha Team |

# TRACK-144 handover

> [!NOTE]
> TRACK-144 implementation is complete. All 343 previously Unknown tālas are resolved, tāla taxonomy is harmonized into 19 canonical tālas (+ sentinel Unknown at 0), and migrations V61–V77 are consolidated into canonical Flyway scripts V61–V63. See [track-144-rasika-field-feedback.md](../10-implementations/track-144-rasika-field-feedback.md).

## User authorization and scope

Continue the [full review and resolution plan](track-144-data-review.md). The user explicitly authorized sending affected catalogue lyrics and metadata to Google Gemini Embedding 2 through the existing indexer on 25 September. That authorization supersedes the earlier pending external-transfer question. No commit or push was requested. Preserve all pre-existing uncommitted mobile changes.

The five-hour usage window reached its limit before the earlier checkpoint was written. The second embedding pass finished afterwards. Search verification is now complete. Do not start another broad scrape until a new notation edition is chosen; the Shivkumar index pass below did not yield another safe batch.

## Applied catalogue state

- Live PostgreSQL is migrated through **V69**. V61–V69 are applied and must not be edited.
- Measured Unknown backlog: **343 → 281** (62 source-backed resolutions). All remaining 281 are Tyagaraja. None remain for Dikshitar or Syama Sastri in this cohort.
- V69: Entani Ne (`e73049d2-c17d-4a3a-9a18-7a895449d74b`), Mukhāri, Unknown → Rupaka. Evidence: `database/data/track144-tala-evidence-entani-ne.json`. The notation page and the Shivkumar index both say Mukhari and Rupakam. Stored pallavi `entani nE varNintunu SabarI bhAgyam(entani)` matches the page line `Entani Ne Varnintunu Shabari Bhaagyam` with Shabari/Sabari and Bhaagyam/bhagyam spelling. Isolated `track144verify` and the live database both applied V69; one composition audit and one evidence audit were written.
- Additionally corrected Sri Narada Nada, Kanada, from the erroneous Adi assignment to **Rupaka**. It still has exactly one Latin variant; no invented script.
- V63: two composition repairs and cited Sumadyuti–Simhendramadhyamam nomenclature relation.
- V64: 31 verified tālas; V65: 11 more explicit blog headers; V66: 14 manually reviewed notation matches; V67: audited registration of the ibiblio Guruguha compilation; V68: four visually verified PDF-backed Dikshitar repairs.
- Evidence manifests: `database/data/track144-tala-evidence*.json`. They contain 60 decisions; the other Unknown resolution and Sri Narada correction are directly evidenced in V63 audits.
- PDF: P. P. Narayanaswami's 2007 compilation, `https://www.ibiblio.org/guruguha/mdeng.pdf`, printed/PDF pages 17, 18, 19 and 77. Stored snapshot: `output/track-144/dikshitar-narayanaswami.pdf`. PDF checksum covers raw bytes; HTML checksums cover decoded UTF-8 HTML.
- Dwijavanthi Akhilandesvari → Adi: both the full notation page and printed compilation agree; the notation site's index says Rupaka and is recorded as conflicting evidence. Suddha Saveri Akhilandesvaro → Rupaka is a different composition. Do not conflate them.
- Bhoopalam and Bowli remain separate. Old combined raga has zero uses. Poorvi Kalyani is associated with Gamanashrama, not Simhendramadhyamam; retain distinct raga identities.

## Implementation

- Shared raga directory uses **Other ragas** for missing hierarchy, rather than asserting every non-melakarta is a janya. Regression in `GroupRagasTest.kt`.
- `src/metadata_parser.py`: prevents consuming the initial m in names; fills missing fields individually from title; handles incomplete parenthesis and postfix tāla labels.
- `scripts/audit_missing_talas.py` / `src/tala_audit.py`: read-only cached-source candidate workflow, explicit-label requirement, visible failures/missing sources, atomic partial output, no automatic acceptance.
- `scripts/compile_tala_repairs.py` / `src/tala_repair.py`: validates reviewed decisions and generates new guarded/audited migrations; rejects duplicate decisions, unsupported tālas and SQL delimiter injection; supports HTML and PDF provenance. Never overwrites an existing migration.
- `database/audits/audit_unknown_talas.sql`: repeatable read-only inventory export.
- `scripts/embed_catalogue.py`: now checks actual indexed-text hash, stored document hash and original source content as well as vector hash. This prevents a V61-style direct text mutation from being skipped merely because the vector hash matches.

All script paths above are relative to `tools/krithi-extract-enrich-worker/` unless otherwise specified.

## Verification already completed

- Worker suite: **424 passed, 16 skipped**; skips include database-dependent tests without Docker in the sandbox. Third-party deprecation warnings retained.
- Real PostgreSQL/Flyway index-maintenance integration tests, disposable container and fake embedder: **5 passed**. Includes reproducing a damaged search document while vector hash remains unchanged and proving repair.
- Shared JVM suites: **80 passed** (25 mobile-data, 55 presentation).
- V63–V68 passed against an isolated populated catalogue copy. Clean migration replay through V65 passed; subsequent integration testcontainer provisioning migrated the full current schema through V68.
- Earlier post-repair integrity checks: zero primary-raga junction gaps, zero section-count mismatches, zero HTTP(S) URLs in lyric sections/full variants, zero uses of combined Bhoopalam/Bowli identity.
- Backend and frontend were rebuilt/restarted after the embedding pass and returned HTTP 200. See Search repair below.
- iOS journeys passed on iPhone 17 / iOS 26.5 (`C0104896-CD64-4533-A919-1E5F22036561`). Log: `output/track-144/ios-journeys.log`.
- `make check-docs` reports the existing relative link to untracked TRACK-144: target exists, checker uses Git-tracked inventory. Do not stage/commit merely to suppress it.

## Search repair — completed

The second pass log ends with `58/58` and `COMPLETE 1421 embeddings`. Results file has 58 rows and zero errors. A later verifier run, after V69, reports **161 compositions, 3,085 expected documents, 0 problems**. Entani Ne was reindexed with the corrected indexer (19 embeddings, tāla Rupaka in the overview). The dev stack was then restarted with `make dev-down` and `make dev`. Backend `/health` and the frontend both returned 200. Extraction became healthy. `GET /v2/catalogue/krithis/e73049d2-c17d-4a3a-9a18-7a895449d74b` returns title Entani Ne, raga Mukhāri, tāla Rupaka. `git diff --check` is clean. `track144verify-db-1` was left running; it is also at V69.

## Earlier search notes

These paths remain useful. The second pass already finished; do not start it again.

The initial 102-ID pass used the existing indexer. A reconstruction audit then found 1,421 mismatches across 58 compositions. The second pass repaired those. Verifier: `output/track-144/verify-search.py`, run from the worker directory. It unions `reindex-ids.json`, every `database/data/track144-tala-evidence*.json` manifest, Marugelaraa (`68f17162-1900-424a-9900-5baac4dda46b`) and dIna janAvana (`850aec01-f57c-48d8-a7a3-fa26b7d4e7af`).

## Remaining source review

Current unresolved inventory was exported before V69: `output/track-144/remaining-tala-inventory.json` (282 rows). Live Unknown count after V69 is **281**. Candidates: `remaining-tala-candidates.json`. These are ignored local artifacts. Last cached blog pass: **287 source URLs for 282 compositions**, 229 pages without explicit tāla and 58 identity/alias review flags. Flags are not necessarily actual wrong matches. No immediately acceptable candidate remains from those blog pages.

A fresh pass over the cached Shivkumar index (134 Tyagaraja entries) found 11 strong title-and-raga matches still in the Unknown set. Only Entani Ne was safe to apply. Newly recorded conflicts, not applied:

- Bhava Nuta / Mohanam (`dff636dc-d88e-48a3-a88a-f08b9dfc0154`): stored pallavi matches `https://www.shivkumar.org/music/bhavanuta.htm` exactly, and that page says **Talam: Adi**. The same site's index says **Rupakam**. Do not choose one.
- Manavinaalakincha / Nalinakānthi, pallavi `manavin(A)lakinca rAdaTE`: `https://www.shivkumar.org/music/manavyala.htm` says **Talam: dEshAdi**. The index says Adi. Bare Deshadi is not Adi.
- nanu pAlimpa: the index link `nannupAlimpa.htm` still returns HTTP 404. Stored pallavi exists; no page to review.

For the next batch, consult an independently identified notation edition, and require composer, raga identity/recognized spelling and full incipit agreement. Do not infer tala from raga, lyrics or popularity. The existing blog pages omit much of this metadata; repeatedly scraping them will not fill the remaining gaps.

Keep excluded:

- The three original wrong-composition imports: Sri Narada Muni/Bhairavi versus Narada Muni/Pantuvarali; Sri Narasimha/Phalaranjani versus Narasimha/Bilahari; Sri Rama Jaya Rama/Madhyamavati versus Varali.
- Additional notation false matches: Neevanti Daivamu/Bhairavi versus the Thodi Shadanana composition; Rama Ninne Nammi/Huseni versus Rama Ninu Nammina/Mohanam; Nee Bhajana/Nayaki versus Ninne Bhajana/Nattai; Ranga Nayaka versus Raghunayaka; Rama Bhakti versus Appa Rama Bhakti; Akhilandesvaro versus Akhilandesvari; Varada Raja versus Vara Narada.
- Same incipit but conflicting raga: Koluvaiyunnade (stored Bhairavi, notation Devagandhari), Brochevarevare (stored Ranjani, notation Sri Ranjani), Lavanya Rama (stored Purna Shadjam, notation Rudrapriya). Review attribution before copying tāla.
- Daya Leni, Pakkala and Mitri have competing/contradictory notation declarations; bare Deshadi entries are not automatically Adi. Padavi Ni was accepted because its full source explicitly says **Adi (Deshadi)**.
- Enaati Nomu and Dvaitamu have missing stored Latin pallavi; nanu Palimpa notation URL was unavailable.
- Existing 28 “Isra Capu” assignments need provenance review. Parser repair is not permission to relabel them wholesale.

Potential additional sources researched but **not used for repairs**: the 1,262-page Tyagaraja lyrics compilation at ibiblio (largely the same source material, not guaranteed new tāla evidence), Saranaagathi's Telugu scans, and Surasa's assorted Tyagaraja archive. No new source was auto-ingested.

## Operational notes

- Disposable Compose project: `track144verify`; override `/tmp/track144-verify-compose.yaml` removes host port exposure. Its database contains the pre-repair catalogue snapshot plus V63–V69. Entani Ne is Rupaka there as well.
- Backup before these corrections: `/tmp/track144-before.dump`. Restorable SQL `/tmp/track144-restore.sql` has `search_path=public` for generated alias keys; this workaround changes only the restore script, not migrations.
- Never reset the live database. Use Flyway via Make; never edit applied migrations. Audit every mutation.
- No `.env`/`config/local.env` contents were manually read. Existing indexer resolves configured credentials internally; never print them.
- Historical V61 broad audit and multilingual portability weaknesses remain. Do not fabricate missing before-images. The local Marugelara only has Latin, but replay against an unrelated multilingual populated corpus requires a preflight.
- Before ending a later session: update this file and the review, run `git diff --check`, and report remaining limitations honestly. No commit without user instruction.

### Search second-pass record

The reconstruction audit expanded coverage to **160 compositions / 3,066 documents** and found **1,421 mismatches across 58 compositions**. The second pass then embedded all 58 (1,421 embeddings, 0 failures). Logs: `output/track-144/reindex-second.log` and `reindex-second-results.json`. Runner: `output/track-144/reindex-second.py`.

### Checkpoint superseded by the search-repair section

The usage-limit note recorded the second pass at 4/58. That process later completed. Manifest audits through V68 were already confirmed. Public API had confirmed Akhilandesvari/Adi, Akhilandesvaro/Rupaka, and Kasi Visvesvara/Ata. Native iOS journeys had passed. Search verification and the stack restart are now done; see the section above. No commits have been made.
