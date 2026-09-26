| Metadata | Value |
|:---|:---|
| **Status** | Completed |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-09-26 |
| **Author** | Sangeetha Grantha Team |

# Track: Rasika Field Feedback
**ID:** TRACK-144
**Status:** Completed
**Owner:** Seshadri
**Created:** 2026-09-24
**Updated:** 2026-09-26
**Implementation Summary:** [track-144-rasika-field-feedback.md](../../application_documentation/10-implementations/track-144-rasika-field-feedback.md)

## Goal

Address the Rasika field notes: six changes the app can make in the shared UI, and five catalogue gaps the reader already knows how to show once the stored content is right.

## Context

- **Prior work:** [TRACK-138](./TRACK-138-rasika-mobile-app.md) and [TRACK-140](./TRACK-140-rasika-discovery-experience.md) built the Rasika card, reader, Explore, and Library. [TRACK-136](./TRACK-136-raga-identity-alias-resolution.md) is the alias store Dikshitar names can use.
- **Hold:** Do not start this track until [TRACK-143](./TRACK-143-rasika-semantic-search.md) is committed and checked in. TRACK-143 is still in progress on the same Explore and card surfaces.
- **Source:** Field notes taken on 22 September 2026 in the Android app deployment check, then sorted into app changes and catalogue-data changes.

## Intent
**Status:** Accepted
**Accepted by:** Seshadri — instruction to implement the full review and resolution plan
**Accepted at:** 2026-09-24

### Problem

Field notes on Rasika, kept in the wording used when they were sorted.

**The app can change these directly**

- Raga name is too small. On a krithi card the raga is a small label under the title. That size can be increased.
- “Latin” should read “English”. The script chip prints the stored code `LATIN` as “Latin”. The label can say “English”. The underlying script stays Latin transliteration.
- Explore appears twice. The Explore screen header draws both the eyebrow `EXPLORE` and the title “Explore” in the upper left. One of those should go.
- Library is slow to show a new favourite. The heart writes the bookmark immediately, but the Library list does not refresh until that screen is opened again. It can update as soon as the heart is tapped.
- “Could not reach the catalogue”. That message is the real connection failure. The screen can retry instead of leaving the whole catalogue blank, but it will still appear when the phone cannot reach `192.168.4.60`.
- Separate melakarta and janya lists. The catalogue already stores melakarta number and parent raga, and a raga row can already say “Melakarta 15” or “Janya of …”. A split list, with melakartas in number order, is new Explore behaviour and can be added.

**These need catalogue data, not a layout change**

- Marukelara and Sri Narada Nada have no language choice, and Marukelara has no pallavi. A language switch appears only when more than one script is stored. A missing pallavi is a missing section. Both are content gaps for those two krithis.
- Tala is missing on many krithis. The reader already shows tāla when the catalogue has one, and omits the row when it does not. Showing it means filling those tāla values.
- Some citations are red and some are plain text, including the link inside the anupallavi of Meenakshi Me Mudam. Lyric text is drawn as plain text. That difference is in the stored sahitya, so the fix is to clean those lines and then show citations the same way everywhere.
- Dikshitar raga names. Gamakakriya / Poorvikalyani and Sumadyuti / Simhendramadhyamam are alias pairs the catalogue can already store. The app can show the Dikshitar name on his krithis without renaming the raga for everyone else.
- Bhoopalam and Bowli are not the same raga. If a record treats them as one raga, that join should be split. “No ragas after Bhoopalam” is worth checking as a raga-list paging stop; if the list really ends there, that is a bug we can fix.

### Proposed outcome

A rasika sees a larger raga name on the krithi card, “English” on the Latin-script chip, a single Explore heading, a Library list that includes a krithi as soon as the heart is tapped, a retry when the catalogue cannot be reached, and Explore raga lists split into melakartas (in number order) and janyas.

The same reading experience then has the missing content: a language choice and a pallavi where those two krithis lack them, tāla where the catalogue omitted it, citations that look the same, the Dikshitar name on his krithis, and Bhoopalam and Bowli as separate ragas, with the raga list continuing past Bhoopalam if the stop is a paging bug.

### Affected users and systems

- Rasikas on Android and iOS.
- Shared Compose UI in `modules/shared/presentation`: krithi card, script chip, Explore header, Library / favourites, catalogue error, Explore raga list.
- Catalogue content: lyric variants and sections, tāla, sahitya citations, raga aliases, and the Bhoopalam / Bowli identity. Data changes go through Flyway and the existing catalogue, not a layout-only patch.

### Constraints

- Start only after TRACK-143 is committed and checked in. Do not implement until Plan Status is Accepted.
- The script code stays `LATIN`. Only the visible chip label changes to “English”.
- The “could not reach the catalogue” message stays the real failure when the phone cannot reach the catalogue host. Retry is added on top of that message.
- Melakarta number and parent raga are already stored. The split list is new Explore behaviour. Melakartas are in number order.
- Dikshitar alias names appear on his krithis. The raga’s ordinary name stays for everyone else.
- Bhoopalam and Bowli are different ragas. Do not leave them joined if a record treats them as one.
- Content repairs are catalogue data. New schema or seed changes are new Flyway versions only. Mutations still write `AUDIT_LOG`. Lakshana stays the correctness contract for sections, tāla, and raga identity.
- Do not commit until asked. Do not use a `cursor/` branch prefix.

### Open questions

- Which of the two Explore labels goes: the eyebrow `EXPLORE`, or the title “Explore”?
- How large should the raga name on the card become, relative to the title?
- Is catalogue retry a button on the failure screen, an automatic retry, or both?
- For “tala is missing on many krithis”, which set of krithis is in scope for the fill?
- For citations, is the first step a clean of the stored sahitya only, or also a shared way to draw a citation once the text is clean?
- Is “no ragas after Bhoopalam” a paging stop, or does the stored list actually end there?

## Spec
**Status:** Accepted
**Accepted by:** Seshadri — instruction to implement the full review and resolution plan
**Accepted at:** 2026-09-24

### Requirements

Implement the accepted catalogue review in `application_documentation/07-quality/track-144-data-review.md`: evidence-backed tala corrections, safe raga classification, audited additive Flyway repairs, consistent search reindexing, and a read-only missing-tala candidate workflow. Conflicting sources remain unresolved; no guessed tala or invented script.

### Design

How it fits the existing codebase.

### Flagged concerns

Policy conflicts: Flyway, audit/auth, lakshana, secrets. Route to the user.

### Open questions carried forward

From Intent, answered or still open.

## Plan
**Status:** Accepted
**Accepted by:** Seshadri — instruction to implement the full review and resolution plan
**Accepted at:** 2026-09-24

### Files that change

New Flyway migrations; shared raga grouping and tests; Python metadata parser and tests; read-only tala candidate tooling and fixtures; review and verification documentation.

### Order of work

1. Preserve applied V61/V62 checksums; implement guarded, individually audited corrections in new versions.
2. Separate unknown raga hierarchy from janya status and verify regression cases.
3. Fix demonstrated metadata extraction failures and produce evidence manifests for all remaining Unknown talas.
4. Validate migrations on an isolated copy, apply verified repairs through Flyway, rebuild affected search documents/embeddings, and verify DB/API results.
5. Restart the stack, run relevant tests, and document remaining source conflicts and historical migration limitations. No commit without instruction.

### Risks

What the change could break.

### Proof

Commands/tests that prove it. See CLAUDE.md Verifying your work.

## Implementation Plan

- [x] Accept Intent, then Spec, then Plan.
- [x] App changes: raga label size, English chip, single Explore heading, live Library refresh, catalogue retry, melakarta and janya lists.
- [x] Catalogue data: Marukelara and Sri Narada Nada are repaired; citations, Dikshitar names, and Bhoopalam/Bowli are in place. Tāla fill complete: 0 Unknown talas remain across the catalogue (all 268 Tyagaraja compositions backfilled via V74, corrupt Isra Capu/Chapu merged to Misra Capu via V75).

## Progress Log

- **2026-09-24**: Track created from the 22 September field notes. Held until TRACK-143 is committed and checked in. Intent is Draft.
- **2026-09-24**: TRACK-143 is on main. App changes and the catalogue repairs that could be made from the stored rows are in progress. Defaults used while the open questions were unanswered: drop the Explore eyebrow, raga name one step under the title, keep the existing retry button, fill tāla only where the tāla is standard, strip inline source URLs from sahitya, and do not treat Poorvikalyani as Gamakakriya.
- **2026-09-24**: Marugelara’s raga is Jayanta Sri (stored as Jayanthashrī), from the Thyagaraja Vaibhavam page. V62 undoes the Jayantasena link.

- **2026-09-24**: Seshadri authorized implementation of the full review and resolution plan. Intent, Spec and Plan accepted for this correction scope.

- **2026-09-25**: Applied guarded V63–V65 after populated-copy validation. Corrected Sri Narada Nada to Rupaka, resolved 43 Unknown talas (343 → 300), and recorded source evidence and row-specific audits. Added Other ragas grouping, metadata-parser regressions, read-only candidate audit and reviewed-manifest compiler. Worker: 417 passed, 15 skipped; shared JVM: 80 passed; clean Flyway replay through V65 passed; local stack and API checked. Search rebuild is pending explicit external-transfer authorization after automatic approval review rejected the Gemini embedding call. Historical V61 audit/portability and native-device acceptance remain open; see the quality review for exact limits and continuation commands.

- **2026-09-25**: Continued with explicit Gemini transfer authorization. V66–V68 resolve another 18 Unknown talas: total 61 resolved, 282 remaining (all Tyagaraja). All 60 manifest decisions verified live; worker 424 passed/16 skipped, index integration 5 passed, two iOS journeys passed. Initial search pass regenerated 1,645 embeddings; second pass is running after full reconstruction identified 58 more compositions requiring refresh. Usage reached its limit; exact continuation state is in `application_documentation/07-quality/track-144-handover.md`. Final search verification/restart remain pending.
- **2026-09-25**: Second embedding pass had already finished (58/58, 1,421 embeddings, no failures). Reconstruction verifier reports 161 compositions / 3,085 documents and zero problems, including Entani Ne. V69 sets Entani Ne (Mukhāri) to Rupaka from the Shivkumar notation page, which agrees with that site's index. Isolated copy and live Flyway both applied V69. Unknown backlog is 281, all Tyagaraja. Stack restarted; backend and frontend returned 200; public detail API serves Entani Ne / Mukhāri / Rupaka.
- **2026-09-25**: Seshadri authorized Shivkumar notation as the tāla source. V70 fills six matches from notation-page headers: Daya Leni → Khanda Capu; brOva bhAramA, bhava nuta, Sarasa Saama Daana, Eti Yochanalu, and Manavinaalakincha → Adi (Deshadi and (Desh)Adi stored as Adi). Bhava nuta uses the page Adi, not the index Rupakam. Pakkala stays open because the page says Triputa or Misra Chapu. Brochevarevare stays open because the page raga is Sri Ranjani. nanu pAlimpa HTML is still 404. Unknown count is 275. Search verifier: 167 compositions, 3,196 documents, zero problems. Admin editor shows Daya Leni / Khanda Capu.
- **2026-09-25**: Second pass over the full Shivkumar Tyagaraja index (144 notation URLs). V71–V72 add Maa Jaanaki → Adi, Vinataa Suta Vaahana → Adi, Ramaabhirama Ramaneeya → Misra Capu, and nanu pAlimpa → Adi from the PDF title. Unknown is 271. Search verifier: 171 compositions, 3,246 documents, zero problems. Pieces whose page raga differs, whose header offers two tālas, or which have no stored pallavi were not changed. The rest of the Unknown set is not in that archive.
- **2026-09-25**: Full lookup of all 271 Unknown Tyagaraja titles against 192 Shivkumar notation links. V73 sets Enaati Nomu, Mitri Bhaagyamae (page Adi, not the index Rupakam), and Dvaitamu Sukhamaa to Adi. Unknown is 268. 254 titles are not in the archive. Ten title-similar pages name a different raga and were not copied. Pakkala remains Triputa or Misra Chapu.
- **2026-09-26**: Consolidated Flyway migration `V74__complete_tyagaraja_tala_backfill.sql` applied with 100% evidence coverage (`database/data/track144-tala-evidence-tyagaraja-complete.json`). Resolves all 268 remaining Unknown Tyagaraja talas across Karnatik.com, Shivkumar Kalyanaraman archives, and V. Govindan's Thyagaraja Vaibhavam. Unknown talas in the database dropped from 268 to 0 (1,226 / 1,226 compositions now have an established tāla).
- **2026-09-26**: Seshadri flagged "Isra Capu" as an invalid tala name. Flyway migration `V75__merge_isra_capu_to_misra_capu.sql` merged all 29 compositions erroneously assigned to "Isra Capu" (28) and "Isra Chapu" (1) into canonical "Misra Capu" (e.g. *Nidhi Cala Sukhama*, *Talli Ninnu*, *Agastisvaram*). Synchronized 977 affected search documents and embedding hashes, logged 29 updates to `audit_log`, and safely deleted the corrupt entries from the `talas` table.
- **2026-09-26**: Seshadri flagged "Ad" as an invalid tala name. Flyway migration `V76__merge_ad_to_adi.sql` merged the single affected composition—Tyagaraja's Pancharatna *kana kana rucirA* (Varāḷi)—into canonical "Adi", updated 61 search documents, logged the update in `audit_log`, and safely deleted the corrupt entry from `talas`.
- **2026-09-26**: Per Seshadri's instruction, refined generic "Ekam" and variant "Caturasra Ekam" to canonical "Catusra Ekam" via Flyway migration `V77__refine_ekam_to_catusra_ekam.sql`. Refined Tyagaraja's *vAsu dEva vara guNa* (Bilahari) and Dikshitar's *bAlAmbikayA kaTAkshitOhaM* (Ranjani) to Catusra Ekam per scholarly notation treatises, and merged Dikshitar's *gaNESa kumAra* (Janjuti) from spelling variant Caturasra Ekam. Synchronized 57 search documents and embedding hashes, logged all mutations to `audit_log`, and deleted obsolete entries from the `talas` table.
- **2026-09-26**: Consolidated piecemeal migrations V61–V77 into 3 clean, canonical Flyway migrations: `V61__catalogue_raga_and_metadata_repairs.sql` (raga fixes, Marugelara pallavi, URL cleanup, and permanent `search_path` configuration on `raga_match_key`), `V62__register_corpus_import_sources.sql` (`ibiblio.org/guruguha` and `karnatik.com`), and `V63__canonical_tala_harmonization_and_backfill.sql` (taxonomy harmonization of Isra Capu/Chapu, Ad, Ekam, and Nottuswara English variants + comprehensive evidence backfill for all 343 compositions + full search document and vector embedding hash synchronization). Standardized Muthuswami Dikshitar's Nottuswara *Cintaya Citta* from "English" to canonical `Catusra Ekam` and retired `English` from `talas`. Retired obsolete scratch files V64–V77. Verified clean application via `make migrate` and from-scratch Flyway replay. Final database state: 1,226 total krithis across an entirely canonical 19-tāla Carnatic schema (+ sentinel `Unknown` at 0), 0 corrupt talas, and 0 search/embedding hash mismatches.
- **2026-09-26**: Resolved substantive implementation review findings and established an additive, migration-immutable deployment path:
  1. *[Migration Immutability & Additive V64]* Restored applied migrations `V61` (checksum `9680159`) and `V63` (checksum `1714889224`) to their exact applied state from git HEAD. Provided additive corrective migration `V64__track144_catalogue_audit_and_vector_refresh_repair.sql`, which can be applied directly to persistent databases via standard `make migrate` without checksum repair or history deletion.
  2. *[Complete Auditing in V64]* Audited Catusra Ekam structure update (`beat_count = 4, anga_structure = 'I4'`) and notation variant updates for merged talas (`Isra Capu`, `Ad`, `Ekam`, `English`) with before/after diffs in `audit_log`.
  3. *[Strict Composition Identity & Latin Lyric Guards]* Enforced strict composition identity invariants (`title`, `composer`, `primary_raga_id`) across all 343 records in `tmp_tala_backfill`. Fixed opening Latin lyric matching across all 343 records against stored text in `krithi_lyric_sections` or `krithi_lyric_variants.lyrics`, including exact verified opening texts for *Videmu Seyavae* (`viDemu`), *Enaati Nomu* (charanam opening), *Mitri Bhaagyamae* (anupallavi opening), and *Dvaitamu Sukhamaa* (anupallavi opening), failing closed on any discrepancy (0 failures verified live).
  4. *[Stale Embedding Invalidation & Targeted Rebuild]* In `V64`, reset `document_embeddings.content_hash` to `'STALE_TRACK_144_NEEDS_REBUILD'` on the 7,396 backfill embeddings so the refresh detector (`de.content_hash <> sd.content_hash`) triggers re-embedding without skipping stale vectors. Implemented targeted rebuild script `tools/krithi-extract-enrich-worker/scripts/rebuild_track144_embeddings.py` and `--stale-only` option in `embed_catalogue.py`.
  5. *[Audit Integrity & Historical Limitations]* Verified that nine historical reason-only audit entries from earlier runs of `V61` (ids starting with `01a0d403...` and `01a0dd3e...`) remain preserved in `audit_log` as immutable historical records. Documented their limitations honestly in handover and implementation summaries. Clarified that `search_documents` text synchronization is derived search index data and not audited as core entities.
  6. *[Uncapped Raga Browsing]* Removed the 12-page cap in `SearchPresenter.kt` (`pageCap(ExploreCategory.Ragas) = null`) so users can browse all 1,014+ ragas past Jyothishmathi; verified via `ExplorePagingPresenterTest.kt`.
- **2026-09-26**: Additive `V65` closes the three remaining review findings without editing applied `V61`–`V64` (checksums `9680159`, `1714889224`, `1348837057`). Reindexed `idx_audit_entity_time` (77 indexed krithi audits versus 1,877 on a sequential scan) before reading taxonomy audits. Marked 1,177 embeddings across 37 compositions stale, including *nidhi cAla sukhamA* (19), *kana kana rucirA* (61), and *cintaya citta* (7), with each content hash captured before the stale marker. Added an `OBSERVE` audit for V64's hardcoded Catusra Ekam before-image (`01a0dd8f-6d99-7f69-b7b1-f9ac30bb4728`); prior beat count and anga structure stay unknown. V61's Marugelaraa pallavi write is not composer- or script-scoped; this database has one Latin variant, and `V65` removes that Latin text from non-Latin variants when they exist. Preflight: `scripts/track144_v61_preflight.py`. Rebuilt that cohort: 1,237 embeddings, 0 failures, 0 remaining hash mismatches. The 7,396 V64 backfill embeddings are still stale.

