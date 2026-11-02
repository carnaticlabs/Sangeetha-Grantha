| Metadata | Value |
|:---|:---|
| **Status** | In Progress |
| **Version** | 1.1.0 |
| **Last Updated** | 2026-11-02 |
| **Author** | Sangeetha Grantha Team |

# Track: Macro Search Follow-ups

**ID:** TRACK-157
**Status:** In Progress (Intent, Spec and Plan accepted 2026-11-02; live runs not authorised)
**Owner:** Seshadri
**Created:** 2026-11-01
**Updated:** 2026-11-02

## Goal

Close the open items [TRACK-152](TRACK-152-rasika-search-over-cycles-and-kshetras.md) left on the collection and kshetra cards in Rasika search: make the relevance floor robust, get a human decision on the cards that appeared on non-macro probes, and give other environments a safe, checkable way to bring macro embeddings up to date.

## Context

- **Shipped by TRACK-152:** `POST /v2/catalogue/search/{hybrid|semantic}` returns `macroHits` (cap 3, one per anchor) beside the untouched kriti `items`. A card must clear a floor in `RasikaSearchService`: dense similarity at or above `0.83`, or (hybrid only) lexical score at or above `0.8`.
- **Open items from the TRACK-152 Progress Log** that this track takes: items 1 (other environments), 2 (thin floor margin) and 3 (cards on non-macro probes), plus the Gemini usage check.
- **Left alone here:** TRACK-152 open items 4 (exact-title ordering seen in the simulator), 5 (thin-kshetra snippet reuse on the detail screen) and 6 (KSHETRA cards, already enabled by PR #49). Mobile list UX is TRACK-155 and Tamil lyric data is TRACK-156; this track touches neither.
- **Hard constraint for drafting this track:** no paid Gemini call, no embedding run, no `evaluate_retrieval.py` against a live API or database was made. Everything below comes from the repo, TRACK-152, and the session notes it cites. Where evidence is missing, the Plan says exactly how to produce it and what Seshadri must authorise.

## Intent

**Status:** Accepted
**Accepted by:** Seshadri (chat instruction; live runs not authorised)
**Accepted at:** 2026-11-02

### Problem

1. **The floor sits almost on top of the weakest real hit.** The weakest macro hit scores 0.836 against a floor of 0.83 (a 0.006 margin) while the exact-title kriti probes peak at 0.787 (a 0.043 margin on the other side). A re-embedded overview, a reworded overview or a new embedding profile can move a score by more than 0.006 and silently remove a correct card.
2. **Three non-macro probes, plus a Ratnatrayam query, now show cards** that nobody has looked at (`EVAL-22`, `SS-146-02`, `SS-146-03`). Some may be right; none has been judged.
3. **Other environments are one step behind.** The dev database re-embedded 6 updated overviews; every other environment needs `make migrate` and a macro re-embed. The only documented command for it does not run (see Spec), and the obvious script flag walks the whole catalogue.
4. **A Gemini usage question is open** (about 1,000 paid calls beyond what was authorised, per the TRACK-152 session's own disclosure). That check is Seshadri's, in the Google console.

### Proposed outcome

1. A floor rule with a stated, tested margin on both sides, calibrated from recorded per-probe scores rather than two data points, and tied to the embedding profile it was calibrated on.
2. A reviewed table for the cards on non-macro probes, with Seshadri's keep or drop decision on each, and a rule for any "drop" (floor change, or a recorded exception) that does not hurt the macro probes.
3. A runbook step and an automatic check that prove macro embeddings are up to date in any environment, using no paid call for the check.
4. The Gemini usage check recorded as done or not done by Seshadri.

### Constraints

- No change to kriti ranking, to V1, or to the response contract (`CatalogueSearchResponseDto` stays as is).
- No change to overview text, chunking or embeddings as part of this track, except that the ops step re-embeds overviews that are already stale (existing behaviour, a handful of calls, authorised per run).
- Any live run (query embeddings, `--via-api`, re-embed) needs Seshadri's explicit go-ahead for that run.
- Benchmark probes marked `requires_human_approval` are changed only by Seshadri.

### Open questions

1. Which direction should the floor take if the recorded distribution is ambiguous: prefer fewer cards (high floor) or more cards (low floor)? Draft answer: fewer; a missing card is recoverable by the kriti results, a wrong card is noise on top of the list.
2. Is a per-profile floor stored in code (a map keyed by model and dimensions) acceptable, or should it live in the database beside `embedding_profiles`? Draft answer: code, because the repo has no config surface for it and a migration is not warranted.
3. Who judges the cards on non-macro probes? Draft answer: Seshadri, with the carnatic-musicologist subagent only if a card makes a lakshana claim (none expected; cards show stored overview text).

## Spec

**Status:** Accepted
**Accepted by:** Seshadri (chat instruction; live runs not authorised)
**Accepted at:** 2026-11-02

### Evidence read (all from the repo and TRACK-152 notes)

- `RasikaSearchService`: `DEFAULT_HYBRID_FLOOR = MacroFloor(0.83, 0.8)`, `DEFAULT_SEMANTIC_FLOOR = MacroFloor(0.83, Double.MAX_VALUE)`; a candidate clears when `similarity >= minSimilarity || lexicalScore >= minLexical`. The constants are global: not per mode in value (both 0.83), and not tied to a profile.
- `MacroSearchRepository`: scores every eligible macro document (about 105) by exact distance in one query and returns the top 10 anchors by RRF (hybrid) or similarity (semantic); the floor is applied afterwards in the service, so the SQL already has the similarity of all candidates in hand.
- `evaluate_retrieval.py --via-api` prints macro hits and their similarity per probe and the list "Non-macro probes with macro hits", but it neither saves its output nor prints scores of cards that did not clear the floor. The DB path (`search_macro_documents`) returns top-k with similarity but is not floor-aware and also saves nothing.
- `application_documentation/03-api/search.md`, the `RasikaSearchService` comment and TRACK-152 agree that the exact-title kriti probes peak at similarity 0.787 (lexical 0.63).

### Findings

**F1. The calibration rests on two positives and six negatives, and the track records disagree about which positive is weakest.**
- `evals/retrieval_benchmarks.json` has four `is_macro` probes: `EVAL-31`, `SS-146-11`, `SS-146-12`, `TY-147-11`. The TRACK-152 Phase 0 baseline (notes folder, `track-152-phase0-baseline.md`) shows `EVAL-31` and `SS-146-11` as HIT in both modes and `TY-147-11` as MISS in both; `SS-146-12` is not in the table at all (it is `review_state: unresolved`, the one probe filtered as needing human approval). So the floor was calibrated on **two** hits.
- The code comment and `search.md` say the macro hits sit at 0.837 and 0.854. TRACK-152's Progress Log says the weakest hit is `EVAL-31` at 0.836. TRACK-145 (`track-145-...md`, section 4) records `EVAL-31` at 0.854. These cannot all be right: either `EVAL-31` is 0.854 and the weak 0.836/0.837 is `SS-146-11`, or `EVAL-31` moved after the overview or snippet changes. The per-probe scores are not stored anywhere in the repo (the baseline file has hit and reciprocal rank only; the `track-108` benchmark JSON has kriti scores, and its 0.8369/0.8360/0.7873 values belong to kriti items such as `EVAL-16`, not to macro documents). **The Plan fixes this first by recording the scores.**
- `TY-147-11` misses in the evaluator. Whether that is the floor, the composer predicate (`expected_composer: Tyagaraja` against a cycle that abstains on attribution) or the document is unknown from the repo. It is a labelled positive that does not currently pass and needs to be understood before it can serve as calibration evidence.

**F2. An absolute cosine threshold is specific to one embedding model, dimension and document text.** `0.83` has no meaning after a profile change, and a re-embed of the 6 updated overviews already moved scores once (TRACK-152 notes). Nothing in the code ties the floor to `embedding_profiles`, so a profile switch would keep a threshold calibrated on a different space.

**F3. Asymmetric margin.** With the recorded numbers the floor is 0.043 above the strongest recorded negative and 0.006 below the weakest recorded positive. The midpoint of 0.787 and 0.836 is about 0.8115, which would give about 0.025 on each side. Moving there without the full distribution would probably admit more cards on non-macro probes (item 2 below), so the midpoint is a candidate to test, not a decision.

**F4. The negatives are the wrong negatives.** The six exact-title probes are queries a user would type to find a specific kriti, so they are easy negatives. The probes that actually produced unreviewed cards (`EVAL-22`, `SS-146-02`, `SS-146-03`, and a Ratnatrayam query) are composer/form queries that sit near a cycle overview in embedding space. The floor has never been measured against those; they are exactly the hard cases.

**F5. Rank-based rules alone cannot say "no card".** A rule such as "top 3 of 105" always returns cards; the floor exists because the group should often be empty (TRACK-152 Spec 13). A rank or gap rule is useful only on top of an absolute guard.

### Options for the floor (to be chosen on recorded data)

| Option | Idea | For | Against |
|:---|:---|:---|:---|
| A. Per-profile absolute floor at the measured midpoint | Keep `similarity >= f`, but keep `f` in a map keyed by profile (model, dimensions); set `f` to the midpoint between the best-scoring labelled negative and worst-scoring labelled positive | Smallest code change; same behaviour shape; fixes F2 and F3 | Still an absolute number; needs the full distribution (F1, F4) and a re-check per profile |
| B. Relative to the query's own score distribution | Require `similarity >= mean + k * sd` over all ~105 macro scores of that query (already computed in SQL), plus a low absolute guard | Adapts to the profile and to broad queries where everything scores high | New moving part; needs `k` calibrated; harder to explain; a query that is legitimately close to several overviews can suppress a true card |
| C. Gap rule | Show the best candidate only if it beats the 4th candidate (or the best kriti's similarity) by a margin, plus a low absolute guard | Directly encodes "stands out from the pack"; no per-profile constant except the guard | Weak when two cycles are both right (Ratnatrayam and Navaratnamalika); compares scores across different documents types for the kriti variant |
| D. Margin test only | Keep `0.83`; add a test that fails when recorded labelled scores come within 0.02 of the floor | Cheapest; makes the thinness visible | Does not make the floor robust; fails today (0.006) and would force A anyway |

**Draft recommendation:** A plus D. Move to a per-profile absolute floor at the measured midpoint of the recorded labelled distributions, and add a committed margin test that reads the recorded scores file and fails below a 0.02 margin on either side. Treat B and C as fallbacks, taken only if the recorded distribution shows no absolute value achieving 0.02 on both sides. The choice is confirmed in Plan step 4 from the data, not from this draft.

### Requirements

**A. Floor robustness**

1. A recorded scores file (`tools/krithi-extract-enrich-worker/evals/macro_scores.json`) holds, per probe, the dense similarity and the lexical score (both mode-independent; the hybrid rank is not needed because the floor reads only these two) of every candidate the macro query returned (all eligible anchors, not only those clearing the floor), the profile id/model/dimensions, and the date. It is data, committed, and regenerated only by an authorised run.
2. The floor is chosen by a stated rule: all labelled positives (macro probes that currently HIT, plus any added with Seshadri's approval) clear it, the six exact-title probes return no card, and the margin to the nearest labelled positive and the nearest labelled negative is at least 0.02 on each side. If no value satisfies this, the Plan stops and reports to Seshadri before any code changes (options B/C).
3. The floor constants are keyed by embedding profile (model and dimensions). A profile with no entry returns an empty macro group and logs once at WARN, rather than reusing another profile's number.
4. An offline unit test (no network, no database) reads `macro_scores.json` and asserts requirement 2 against the constants in code, so a change to either fails the build.
5. `TY-147-11` is diagnosed: it is either explained and recorded as a known non-hit with its reason, or fixed in a way that does not touch overview text without approval.
6. `search.md`, the `RasikaSearchService` comment and the `track-145` note are corrected to the recorded numbers.

**B. Cards on non-macro probes**

7. A review table lists, for `EVAL-22`, `SS-146-02`, `SS-146-03` and the Ratnatrayam query, the probe text, the card(s) shown, the card's similarity and lexical score, and a keep or drop recommendation. The table is produced from `macro_scores.json` and the production floor (not typed by hand) and is committed in this track file.
8. A card is "keep" when the probe's own expected kriti is a member of the card's collection or sits at the card's kshetra (checkable from `krithi_cycle_memberships` / `krithis.temple_id` with `expected_krithi_id`, which the Syama Sastri probes carry), or when Seshadri says so. Otherwise "drop".
9. A "drop" is implemented only through requirement 2 (floor) or a recorded exception list in the Plan; never by special-casing a probe id in service code.
10. Seshadri's decision on each row is recorded in the Progress Log. Rows are not auto-counted as failures.

**C. Environment follow-up**

11. A runbook step in `application_documentation/08-operations/runbooks/` (new file `macro-embeddings-runbook.md`) and a pointer from `search.md` describes: `make migrate`, a no-spend dry run, an authorised macro-only re-embed, and the check below.
12. A **macro-only** mode exists in the worker (`embed_catalogue.py --macro-only`) that never fetches krithi candidates and never embeds a kriti document. `--macro-only --dry-run` makes no API call and prints how many cycle and kshetra overviews would be embedded. Today there is no such mode: see C-finding below.
13. A **freshness check** (`embed_catalogue.py --macro-only --check`) exits non-zero when any published macro document is stale, using only database reads and local hashing (no embedding call). A document is stale when any of these holds for the active profile: no embedding row exists; `document_embeddings.content_hash <> search_documents.content_hash`; `search_documents.content_hash <> md5(indexed_content)`; or the overview text regenerated from current source data hashes differently from the stored `content_hash`. The last case is what catches a source-data change (for example a TRACK-154 temple update) that has not yet been re-indexed. A read-only SQL form of the first three conditions is also given in the runbook for environments without the worker.
14. The runbook includes the expected success output (`stale = 0`, counts of cycle and kshetra documents by kind) and what to do on failure.

**D. Gemini usage item**

15. Recorded as an open item for Seshadri only. This track makes no attempt to read or verify usage. It is closed by a Progress Log line from Seshadri.

### C-finding: the documented macro-only command does not work and the nearby flag is unsafe

- `search.md` ("To index macro cycle and kshetra overviews") shows a `python -c` snippet that imports `psycopg2` and `src.embeddings.client.GeminiEmbeddingClient`. The worker depends on `psycopg[binary]` (v3) and has `src/embeddings/gemini_embedder.py` with `GeminiEmbedder`; there is no `client` module. The snippet cannot run as written. It also calls the indexers without a `profile_id`, which the functions require.
- `embed_catalogue.py --include-macro` passes the argument guard but still calls `fetch_krithi_candidates(...)` with no limit, then `index_krithi` for every returned krithi, before the macro step. Hashes make unchanged krithis cheap, but any krithi whose stored hash does not match is re-embedded with a paid call. Whether this is how the extra calls in TRACK-152's disclosure happened is **not established from the repo**; it is the nearest candidate and Seshadri should compare it with the console figures.
- Hence the new `--macro-only` mode and the rule that the runbook never uses `--include-macro` for a macro refresh.

### Design

- **Scores capture (worker, no backend change).** `evaluate_retrieval.py` gains `--dump-macro-scores <path>`. On the database path it already embeds the query and ranks macro documents; the option records the full list (not top 5) and the profile, for hybrid and semantic, one embedding call per distinct probe query shared across modes. The API path stays for end-to-end proof. This adds paid query embeddings, so it is a run Seshadri authorises (Plan, "Live runs").
- **Floor.** `MacroFloor` gains no new fields; `RasikaSearchService` takes a `Map<ProfileKey, FloorPair>` (hybrid, semantic) and looks up by the active profile's model and dimensions (`PreparedQuery.profile`). Missing key means no macro group.
- **Margin test.** A Kotlin unit test in `:modules:backend:api` reads `macro_scores.json` through a test resource copy (or a path from the repo root) and checks the constants; no network.
- **Freshness check.** Reuses `format_cycle_overview` / `format_kshetra_overview` and the same queries as `macro_indexer` to regenerate text and hash, but only compares. Implemented as a function beside the indexers so the indexer and the check cannot drift (the same code builds the text).
- **Runbook.** New `macro-embeddings-runbook.md`, linked from `runbooks/README.md` and `search.md`; the broken snippet is replaced by the `--macro-only` command.

### Flagged concerns

- **Spend.** The track adds two live runs (scores capture; re-embed per environment). Both are listed with exact commands and need Seshadri's go-ahead per run. The check and the dry run spend nothing.
- **Overfitting.** The floor is chosen on the same probes it is judged by. Mitigation: requirement 2 uses a margin on both sides, and the test fixes it to recorded data; adding new labelled probes needs Seshadri's approval and is the real remedy.
- **Probe ownership.** `requires_human_approval` probes (`SS-146-*`, `TY-147-*`) are not edited. New labelled negatives or positives are proposed in the table for approval, not added.
- **Audit and writes.** Search and the check are read-only. The re-embed writes `search_documents` and `document_embeddings`; that is existing worker behaviour (not an application mutation covered by `AUDIT_LOG`). No migration in this track.
- **Musicology.** Cards show stored, reviewed overview text; no lakshana claim is added. Keep or drop is a relevance decision, not a musicological one.

### Open questions carried forward

The three Intent questions, with draft answers above.

## Plan

**Status:** Accepted
**Accepted by:** Seshadri (chat instruction; live runs not authorised)
**Accepted at:** 2026-11-02

Nothing is implemented. Do not start until Seshadri accepts this Plan. Phases land separately.

### Files that change

| Path | Phase | Change |
|:---|:---|:---|
| `tools/krithi-extract-enrich-worker/scripts/evaluate_retrieval.py` | 1 | `--dump-macro-scores <path>` on the DB path; full macro candidate list per probe and mode, profile metadata |
| `tools/krithi-extract-enrich-worker/evals/macro_scores.json` (new) | 1 | Recorded output of the authorised run |
| `tools/krithi-extract-enrich-worker/tests/test_evaluate_retrieval_sql.py` (or a new test) | 1 | Dump format test with a fake embedder |
| `modules/backend/api/.../services/RasikaSearchService.kt` | 2 | Profile-keyed floor map; empty group and WARN for an unknown profile; corrected comment |
| `modules/backend/api/src/test/.../RasikaSearchFloorTest.kt` (new) and a test resource copy of `macro_scores.json` | 2 | Offline margin test (requirement 4) |
| `tools/krithi-extract-enrich-worker/scripts/embed_catalogue.py`, `src/embeddings/macro_indexer.py` | 3 | `--macro-only`, `--check`, shared text-regeneration helper |
| `tools/krithi-extract-enrich-worker/tests/test_macro_indexer.py` (extend) | 3 | `--macro-only` fetches no krithis; `--check` stale and fresh cases with a fake connection |
| `application_documentation/08-operations/runbooks/macro-embeddings-runbook.md` (new), `runbooks/README.md`, `application_documentation/03-api/search.md` | 3 | Runbook, corrected command, corrected numbers |
| `application_documentation/10-implementations/track-145-dikshitar-kshetra-musicological-metadata.md` | 2 | Correct the `EVAL-31` figure only if the recorded run disagrees with it |
| `conductor/tracks/TRACK-157-macro-search-followups.md`, `conductor/tracks.md` | all | Review table, decisions, status |

No migration. No change to kriti SQL, V1, DTOs or mobile code. No `.claude/` or `CLAUDE.md` change (so `make agent-evals` is not needed).

### Live runs (need Seshadri's explicit authorisation, each one)

These spend Gemini quota and are **not** run by anyone until Seshadri says so for that run.

1. **Scores capture.** One embedding call per distinct probe query (53 probes loaded; about 53 calls, shared between modes by the Phase 1 change). From the worker directory, against the intended database and with the credentials the worker already uses:
   ```bash
   uv run python scripts/evaluate_retrieval.py --dump-macro-scores evals/macro_scores.json
   ```
   The dump mode embeds each distinct probe query once and exits; it does not take `--mode`. Needs `DATABASE_URL` (or `--db-url`) for the intended database and the worker's Gemini credentials.
2. **End-to-end proof after Phase 2** (calls the backend, which embeds each probe query: about 53 calls per mode, the backend's own cache applies within a run):
   ```bash
   uv run python scripts/evaluate_retrieval.py --mode all --via-api http://localhost:8080
   ```
3. **Macro re-embed, per environment** (a few calls: only overviews whose text changed; see runbook):
   ```bash
   uv run python scripts/embed_catalogue.py --macro-only --check
   uv run python scripts/embed_catalogue.py --macro-only --dry-run
   uv run python scripts/embed_catalogue.py --macro-only
   ```
   The first two make no API call. Run the third only after reading their output.

### Order of work

**Phase 0: sign-off (no code).** Seshadri accepts Intent, Spec and Plan, or edits them. Seshadri also checks Gemini usage in the Google console (Progress Log, item 15).

**Phase 1: record the scores (worker only).**
1. Write the dump format test (red), then implement `--dump-macro-scores`.
2. With authorisation, run live run 1 and commit `macro_scores.json`.
3. From the file, fill the "Results" subsection below: per-probe macro scores for the four macro probes, the six exact-title probes and every non-macro probe that has any candidate above 0.80; reconcile the 0.836 / 0.837 / 0.854 discrepancy; explain `TY-147-11`.

**Phase 2: choose and test the floor (backend).**
4. Compute the floor options A to D on the recorded file with a short script run offline (no API): for each option list pass/fail per labelled probe and the margins. Pick A if it meets requirement 2; otherwise stop and report to Seshadri with the numbers (options B or C need an amended Spec).
5. Write `RasikaSearchFloorTest` (red), then the profile-keyed floor and the empty-group-for-unknown-profile behaviour. Existing route and repository tests unchanged.
6. Generate the review table (requirement 7) from the recorded file using the final floor, with keep/drop per requirement 8 (membership check by read-only SQL, no paid call). Put it in this file and ask Seshadri to decide each row.
7. With authorisation, run live run 2 and record that macro probes equal the Phase 1 outcomes and `items` parity shows zero differences.
8. Correct the documented numbers (requirement 6).

**Phase 3: environments and the freshness check (worker, docs).**
9. Write tests (red), then `--macro-only` and `--check`.
10. Write the runbook and the pointers; replace the broken snippet in `search.md`.
11. Run `--macro-only --check` against the dev database (read-only) and paste the output; with authorisation, run it in each other environment after its `make migrate` and re-embed, and paste the outputs.

### Review table (from `macro_scores.json`, recorded 2026-11-02)

Cards that a floor of 0.83 would show, per probe, using each probe's best macro document per anchor (dense similarity, lexical score, `gap4` = top similarity minus the 4th-best anchor's). The recording is not eligibility-filtered, so a row can disappear in the API if its anchor is ineligible. Keep or drop is **provisional** until Seshadri decides; the first four rows follow requirement 8 (the expected kriti is a member of the card's collection, checked by a read-only query on `krithi_cycle_memberships`).

| Probe | Probe text | Card | Sim / lex / gap4 | Recommendation |
|:---|:---|:---|:---|:---|
| `EVAL-22` | Swarajathi Kritis of Syama Sastri | Syama Sastri Swarajathi Ratnatrayam (collection) | 0.838 / 0.69 / 0.044 | Keep: the form by this composer is that set (no `expected_krithi_id`; judgement) |
| `SS-146-03` | Syama Sastri Swarajathi in Yadukulakambhoji ... Kamakshi Ni Padayugamu | same | 0.839 / 0.45 / 0.043 | Keep: expected kriti is a member |
| `SS-146-02` | Syama Sastri Swarajathi in Todi ... Rave Himagiri Kumari | same | 0.831 / 0.38 / 0.049 | Keep: expected kriti is a member |
| `SS-146-01` ("Ratnatrayam" query) | Syama Sastri Swarajathi Ratnatrayam first gem in Bhairavi ... | same | 0.824 / 0.47 / 0.042 | Keep: expected kriti is a member (below 0.83, so no card today; shows at floor 0.82) |
| `EVAL-05` | Dikshitar krithi in Chidambaram describing the cosmic dance | Nataraja Temple (kshetra) | 0.849 / 0.30 / 0.041 | Keep, plausible (Seshadri to confirm) |
| `EVAL-20` | Praise of Lord Shiva Nataraja in Kedaram raga | Nataraja Temple (kshetra) | 0.834 / 0.33 / 0.038 | Keep, plausible |
| `EVAL-27` | Fifth enclosure Kamalamba Navavarnam Panchami vibhakti Bhairavi | Kamalamba Sannidhi (kshetra) | 0.843 / 0.34 / 0.038 | Keep, plausible (a Kamalamba Navavarnam collection card would fit better; it is second, gap2 0.005) |
| `EVAL-24` | Muthuswami Dikshitar Agni lingam fire element at Tiruvannamalai | Arunachaleswarar Temple (kshetra) | 0.8304 / 0.45 / 0.047 | Keep: the TRACK-152 Spec expected this card; only 0.0004 above the floor |
| `EVAL-06` | Abhayamba series krithi in Kalyani raga | Abhayamba Vibhakti Krithis (collection) | 0.8305 / 0.34 / 0.020 | Keep, plausible; weak separation (gap4 0.020) |

Unreviewed cards the TRACK-152 note did not mention appear in this recording at 0.83 (`EVAL-05`, `EVAL-20`, `EVAL-27`, `EVAL-24`, `EVAL-06`): either they are filtered by eligibility in the API or the note was incomplete; the `--via-api` run settles which.

Probes just under 0.83 whose top card looks wrong (so a lower absolute floor would add them): `EVAL-08` Tiruvarur Pancha Linga Krithis for "Dikshitar krithi in Anandabhairavi" (0.823, gap4 0.009), `TY-147-10` (0.821, gap4 0.004), `EVAL-29` (0.819, gap4 0.013) and `TY-147-04` (0.819, gap4 0.012), all Anandavalli Sametha Thanjapureeswarar Temple. These are my reading of the probe text, not labels.

### Results (Phase 1 live run, 2026-11-02)

Authorised by Seshadri in chat; 53 query embeddings; recorded in `tools/krithi-extract-enrich-worker/evals/macro_scores.json` (profile `gemini-embedding-2`, 768, id `01a07605-793b-7209-bbd2-f173e41b6d92`; 106 macro anchors per probe).

- **Labelled positives** (best macro hit, similarity / lexical): `TY-147-11` 0.852 / 0.64 (Tyagaraja Ghana Raga Pancharatnam), `SS-146-11` 0.847 / 0.79 (Ratnatrayam), `EVAL-31` **0.836** / 0.49 (Bhagavati Amman Temple, the weakest). `SS-146-12` is not loaded by the evaluator (unresolved review).
- **Exact-title negatives**: `EVAL-13` 0.787, `EVAL-01` 0.777, `EVAL-23` 0.743, `EVAL-04` 0.732, `EVAL-09` 0.724, `EVAL-17` 0.624. Peak 0.787, as recorded. Their best lexical scores stay at or below 0.52 against a lexical floor of 0.8.
- **Reconciled figures**: `EVAL-31` is 0.836 (TRACK-152 was right). TRACK-145's 0.854 is stale. The "0.837 and 0.854" in the code comment and `search.md` are not the weakest hit; the other two positives are 0.847 and 0.852. Margins at 0.83: +0.006 above the weakest positive's side, 0.043 above the strongest exact-title negative.
- **`TY-147-11`** ranks first at 0.852 with composer Tyagaraja (the stored text carries `[Composer: Tyagaraja]`), so the evaluator's DB path would now score it a HIT. The Phase 0 baseline MISS is stale or predates the macro text fix; the floor was never the cause.
- **Requirement 2 cannot be met by an absolute floor alone.** The 0.02 margin on both sides needs a value no higher than 0.816 (to sit 0.02 below 0.836) and no lower than 0.807 (0.02 above 0.787), so about 0.81 is feasible against the exact-title probes. But below 0.83 the top cards for `EVAL-08`, `TY-147-10`, `EVAL-29` and `TY-147-04` start to clear, and they look wrong.
- **Relative rules alone do not work either.** A z-score over the 106 anchors does not separate (`EVAL-13`, an exact-title probe, has z 3.58, higher than `EVAL-31` at 3.16). The gap to the 4th anchor does separate the four wrong-looking cards (gap4 0.004 to 0.013) from the positives (0.044 to 0.050), but only as a second condition on top of an absolute floor.
- **Candidate rule for decision**: similarity at or above 0.81 and gap4 at or above 0.03, keyed to this profile. On the recording this keeps all three positives (margins +0.026 similarity and +0.014 gap4), drops all six exact-title probes (0.023 below), and drops the four wrong-looking cards (gap4 0.017 below). It misses the 0.02 margin on the gap dimension, uses only three positives, and the "wrong-looking" labels are mine. That needs Seshadri's decision and a Spec amendment (option C layered on A), as the Plan requires.

### Risks

- **Floor moves and cards change.** Mitigated by the recorded-data rule, the margin test and the review table before merge.
- **No value achieves 0.02 on both sides.** The Plan stops and reports; the fallback options B and C need a Spec amendment, not a quiet loosening of the margin.
- **A profile with no floor entry hides all cards.** Deliberate and logged; the runbook lists adding an entry as part of any profile activation.
- **Freshness check false negatives.** The regenerated-text comparison depends on `format_*_overview` being the single source of the text; the helper is shared with the indexer to prevent drift, and the test covers a changed temple field.
- **Cost surprise.** Dry-run first, `--macro-only` guarantees no krithi calls, and each live run is authorised separately.
- **Rejected:** hard-coding a new constant without recorded scores; special-casing probe ids; changing the response contract; editing overviews to move scores.

### Completion evidence (fixed)

- Phase 1: `macro_scores.json` committed with profile metadata; the dump test; worker `make`-equivalent checks (`uv run pytest` for the touched tests, `ruff`, `mypy` as the worker already requires).
- Phase 2: `RasikaSearchFloorTest` and the existing search tests pass under `make test`; `--via-api` output for the four macro probes equal to recorded outcomes with zero `items` differences from V1; the review table with Seshadri's decisions.
- Phase 3: worker tests for `--macro-only` and `--check`; pasted `--macro-only --check` output (`stale = 0`) for each environment brought up to date; `make check-docs`.
- All phases: `git diff --check`.

## Implementation Plan

- [x] Phase 0: Seshadri accepted Intent, Spec and Plan (2026-11-02). Gemini usage check remains Seshadri's.
- [x] Phase 1 code: `--dump-macro-scores` with tests (fake embedder).
- [x] Phase 1 live: scores captured 2026-11-02, `macro_scores.json` committed, figures reconciled, `TY-147-11` explained (see Results).
- [ ] Phase 2: choose floor from data, margin test, profile-keyed floor, review table and decisions, authorised `--via-api` proof, corrected docs. **Blocked on Seshadri's decision on the floor rule (see Results): an absolute floor alone cannot give a 0.02 margin without admitting wrong-looking cards.**
- [x] Phase 3 code and docs: `--macro-only`, `--check`, shared freshness logic, tests, runbook, corrected `search.md` snippet.
- [ ] Phase 3 per-environment: `--macro-only --check` output for each environment (dev check is read-only but not yet run here; re-embeds need authorisation).

## Open items for Seshadri (not done by this track)

1. Check Gemini usage in the Google console for the reported extra calls (about 1,000 paid embedding calls beyond what was authorised, reversed from a hash-verified backup, per the TRACK-152 Progress Log). Not verified here and not verifiable from the repo.
2. Authorise each live run in the Plan when its turn comes.
3. Confirm which probe the "Syama Sastri Ratnatrayam" query is.

## Progress Log

- **2026-11-01**: Track created as Proposed from the TRACK-152 follow-ups. Intent, Spec and Plan drafted in one pass for review; none accepted. Evidence read: TRACK-152 and its notes, `RasikaSearchService`, `MacroSearchRepository`, `evaluate_retrieval.py`, `retrieval_benchmarks.json`, `embed_catalogue.py`, `macro_indexer.py`, `search.md`. No application code, data or embedding changed; no paid Gemini call and no live evaluator run made. Findings that change the picture: the floor was calibrated on two macro hits, and the records disagree on which is weakest (0.836 `EVAL-31` in TRACK-152, 0.854 `EVAL-31` in TRACK-145, 0.837 and 0.854 in the code comment); per-probe macro scores are not stored anywhere in the repo; the documented macro-only command cannot run and `--include-macro` alone walks the whole catalogue.
- **2026-11-02**: Intent, Spec and Plan accepted by Seshadri; implementation approved with no live run authorised. Implemented without any live run: `evaluate_retrieval.py --dump-macro-scores` (one embedding per distinct query, all published macro documents with dense and lexical scores, tested with a fake embedder); `embed_catalogue.py --macro-only` (never fetches krithis), `--macro-only --check` (database reads and local hashing, exit 1 when stale) and `--macro-only --dry-run`; the indexers now share `build_*_documents` and `stale_reasons` with the check, so they skip exactly what the check calls fresh (a vector whose hash differs from the document is now re-embedded, which the old skip test missed); runbook `macro-embeddings-runbook.md` and the corrected `search.md` command. Worker `pytest` (538 passed), `ruff check`, `ruff format --check` and `mypy .` pass. Not run: any live evaluator, API probe, embedding or database check. Phase 2 is blocked on live run 1.
- **2026-11-02 (later)**: Seshadri authorised live run 1. It ran against the local database (53 query embeddings; the worker found its key itself, no key was read or printed) and wrote `evals/macro_scores.json`. Results: `EVAL-31` is the weakest positive at 0.836 (TRACK-145's 0.854 is stale); exact-title peak 0.787; `TY-147-11` ranks first at 0.852 and would now HIT. Requirement 2 cannot be met by an absolute floor alone; a stop-and-report per the Plan. Review table filled (SS-146-01/02/03 keep, by membership). Phase 2 code (profile-keyed floor, margin test) waits for Seshadri's choice of rule.
