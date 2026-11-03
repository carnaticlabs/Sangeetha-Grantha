| Metadata | Value |
|:---|:---|
| **Status** | Proposed |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-10-15 |
| **Author** | Sangeetha Grantha Team |

# Track: Syama Sastri Edition Consultation & Musicological Adjudication

**ID:** TRACK-148
**Status:** Proposed — Intent Accepted, Spec Accepted, Plan Accepted (implementation in progress)
**Owner:** Seshadri
**Created:** 2026-10-15
**Updated:** 2026-11-03

## Goal

Consult the institutional print editions that TRACK-146 registered but did not inspect. Use them to promote, correct or reject the Syama Sastri claims that are still unreviewed leads, and apply the approvals that TRACK-146 deferred.

## Context

- **Predecessor:** [TRACK-146](TRACK-146-syama-sastri-musicological-classification.md), completed 2026-10-15 under its closure amendment. TRACK-146 owns the shared classification contract, master model, generator and `R__seed_10`; this track extends them and does not fork them.
- **Contract:** [Musicological classification contract](../../application_documentation/01-requirements/features/musicological-classification-contract.md).
- **Master and manifest:** `database/data/track146-syama-sastri-master.json` and `database/data/track146-syama-sastri-application-manifest.json`. The concordance is generated as [syama-sastri-classification-master.md](../../application_documentation/01-requirements/features/syama-sastri-classification-master.md).
- **Companion:** [TRACK-147](TRACK-147-tyagaraja-musicological-classification.md) (Tyagaraja) consumes the same contract.

## Intent

**Status:** Accepted
**Accepted by:** Seshadri (chat, 2026-11-03)
**Accepted at:** 2026-11-03

**Evidence-access amendment (2026-11-03, Seshadri):** no print edition is available. Only publicly accessible sources may be consulted. Edition-dependent items stay `unresolved` or `deferred` unless a public source is found and registered.

### Problem

TRACK-146 closed with an honest but incomplete evidence base. Four print editions are registered as `unavailable` witnesses and were never consulted:

- Smt. Vidya Shankar, *Syama Sastry's Compositions* (1980)
- T.K. Govinda Rao, *Compositions of Syama Sastri, Subbaraya Sastri and Annaswami Sastri* (1997)
- Prof. P. Sambamoorthy, *Great Composers: Syama Sastri* (1962)
- the unindexed Journal of the Music Academy archive volumes

Their 40 claims are `unreviewed` leads without page locators. They cover Viloma Chapu, anagata eduppu, Khanda Jhampa, Caturasra Ata, swarasahitya, Apatya Bhava and the Sambamoorthy shrine associations. Six Anandabhairavi raga-character claims are disputed, and all 70 compositions remain `UNRESOLVED` for geography. These hallmark dimensions therefore do not yet reach the catalogue or search headers.

### Proposed outcome

1. **Edition access.** Obtain lawful access to each of the four editions (physical, scanned or library). Register each consulted edition as a hashed witness, and record any edition that stays inaccessible.
2. **Lead review.** Give each of the 40 leads a decision: approve with an exact page locator, correct, reject, or keep unresolved with a reason. Never invent a locator.
3. **Adjudication carried from TRACK-146:**
   - the Todi swarajathi's tala and kalai
   - the 6 (Todi) and 11 (Yadukulakambhoji) swara-sahitya counts; JMA LXXV PDF p.91 already corroborates the Bhairavi 8
   - the five Viloma Chapu (4+3) attributions (SS-0032, SS-0035, SS-0038, SS-0044, SS-0068)
   - the six disputed Anandabhairavi raga-character claims
4. **Geography.** Resolve shrine associations where an edition supports them (Bangaru Kamakshi, Madurai Meenakshi and others). Leave compositions `UNRESOLVED` where none does.
5. **Navaratnamalika.** Re-attempt the 1954 Music Academy souvenir (S. Vidya, printed p.11) and other sources. Allow incomplete or disputed membership, and never manufacture a nine-item set.
6. **Deferred application.** Design and accept an application path for approved claims outside a cycle, starting with the four deferred mudras (SS-0032, SS-0034, SS-0046, SS-0051). Lift their `application: deferred` only when that path exists.
7. **Search headers.** Decide whether approved claims outside a cycle need separate Spec 14 labels (`[Mudra: …]`, `[Eduppu: …]`, `[Raga Trait: …]`). If so, extend the formatter within the 60-word budget, then re-index and rerun the SS-146 probes in hybrid mode.
8. **Spec 12 title.** TRACK-146 Spec 12 names *Samini Rammanave* for Caturasra Ata, but the only stored Caturasra Ata composition is SS-0036 (*nA manavini*). Confirm whether this is a title error or a missing composition.
9. **Data corrections.** Pick up catalogue corrections that the reviewed evidence requires (form, tala, attribution) through the audited canon/import path, never through direct SQL.
10. **Test debt.** Add a pytest case for the `application: deferred` validation rule; it was blocked during TRACK-146 without `SANGITA_ALLOW_TEST_EDITS=1`.

### Affected users and systems

- Musicologists and curators: claim review against primary editions.
- Data: `track146-syama-sastri-master.json`, the manifest, the generated concordance, `R__seed_10` or its successor, and canon corrections.
- Search: composition headers and cycle overviews, followed by a re-index (paid embedding run).

### Constraints

- Follow [CLAUDE.md](../../CLAUDE.md) and the Conductor workflow: Intent → Spec → Plan acceptance before implementation.
- Flyway only. Never edit committed versioned migrations. Repeatable seeds must keep their provenance guards, conflict abort and system audit attribution.
- Only explicit human approval promotes a claim. Agent review supports checking but does not replace musicological acceptance.
- Never derive eduppu, arudi, kalai or subdivision from lyrics. Rhythm claims need units, counting origin and convention.
- Paid embedding runs, commits and production changes each need separate authorization.

### Open questions

1. Which of the four editions can be accessed, in what form, and on what timeline?
2. Who adjudicates claims that the editions leave ambiguous?
3. Should approved claims outside a cycle be stored as catalogue attributes, a new membership type, or search-only metadata?
4. Is the 1954 souvenir obtainable in full?

## Spec

**Status:** Accepted
**Accepted by:** Seshadri (chat, 2026-11-03)
**Accepted at:** 2026-11-03

**Acceptance note:** accepted with the Spec's recommended defaults for its open decisions: `openly_hosted` witnesses may approve with a locator and short quote; Seshadri adjudicates with the musicologist subagent checking; non-cycle path is option D1.


### Evidence policy (applies to every requirement)

No print edition is available to the owner, so the track runs on **publicly accessible sources only**. Three tiers, recorded per witness in a new optional `rights` field:

| `rights` | Meaning | May approve a claim? |
|:---|:---|:---|
| `public_domain` | Work is out of copyright (for example texts published before 1965 whose author died before 1965; the compositions themselves). | Yes, with hash and locator. |
| `openly_hosted` | Freely downloadable from an institution (for example the Music Academy journal PDFs already fetched as `ma:jma-*`), copyright status not established. | Yes as a cited locator only. Do not copy passages beyond a short quotation. Record the rights uncertainty. |
| `inaccessible` | Not obtainable (the three print editions). | No. |

A claim that only an `inaccessible` edition could settle ends as `unresolved` with reason `edition_inaccessible`, not as a perpetual `unreviewed` lead. That is an honest terminal state for this track, not a failure.

### Requirements

1. **Witness rights.** Extend the witness record with optional `rights` (`public_domain`, `openly_hosted`, `inaccessible`) and an optional `rights_note`. The validator accepts the field, defaults existing witnesses (`fetched` to `openly_hosted`, `unavailable` to `inaccessible`) and rejects approval from an `inaccessible` witness (already true through status). No schema change to the database.
2. **Public-source sweep.** Search and register, with SHA-256, URL, access date and `rights`: (a) the Music Academy journal volumes and souvenirs not yet fetched, including the 1954 souvenir already listed as `academy-1954-lead`; (b) digitised public-domain works that may carry Syama Sastri text or notation (Subbarama Dikshitar's *Sangita Sampradaya Pradarsini*, 1904, plus archive.org, Wikisource and the Digital Library of India); (c) Sahapedia and similar open scholarship. Each source is recorded as consulted with an outcome, including "searched, nothing relevant". Fetched artifacts are cached outside git or hashed only. No full copyrighted text enters the repository.
3. **Lead review for the 40 unreviewed claims.** Each claim receives exactly one disposition: `approved` (public witness, exact page locator, named approver), `corrected`, `rejected`, `unresolved` with a reason (`edition_inaccessible`, `no_public_source`, `sources_conflict`) or `disputed`. Never invent a locator. Expected outcome: most leads close as `unresolved/edition_inaccessible`.
4. **Carried adjudication.** Re-examine, using the already-fetched journal volumes first: the Todi swarajathi tala and kalai; the 6 (Todi) and 11 (Yadukulakambhoji) swara-sahitya counts; the five Viloma Chapu (4+3) attributions SS-0032, SS-0035, SS-0038, SS-0044, SS-0068; and the six disputed Anandabhairavi raga-character claims. The Bhairavi count of 8 (JMA LXXV PDF p.91) is already corroborated and is not reopened. Rhythm claims keep the contract's units, counting origin and convention rules. Nothing is derived from lyrics.
5. **Geography.** Approve a shrine association only when a registered public witness names the place. Otherwise the composition stays `UNRESOLVED`. `NO_PLACE_STATED` observations remain unapprovable, per the contract.
6. **Navaratnamalika.** Retry the 1954 souvenir and any public source for the nine-item list. Partial, conflicting or disputed membership is recorded as such. A nine-member set is never manufactured. No Navaratnamalika membership is seeded without approved claims.
7. **Deferred application path.** Design (not necessarily build) how approved claims outside a cycle reach the catalogue, starting with the four deferred mudras SS-0032, SS-0034, SS-0046, SS-0051. `application: deferred` is lifted only when the accepted path exists. See Design option D.
8. **Search headers.** Decide whether non-cycle claims get their own Spec 14 labels (`[Mudra: …]`, `[Eduppu: …]`, `[Raga Trait: …]`). Any change stays within the 60-word header budget and keeps cycle clauses unchanged. A re-index is a separate, separately authorised paid run. This track prepares and tests the formatter change but does not run embeddings.
9. **Spec 12 title check.** Determine whether TRACK-146 Spec 12's *Samini Rammanave* for Caturasra Ata is a title error (the stored Caturasra Ata composition is SS-0036, *nA manavini*) or a missing composition. Resolve from public sources or record it as unresolved. Do not add a composition without a witness.
10. **Catalogue corrections.** Corrections implied by approved evidence (form, tala, attribution) go through the audited canon/import path, one reviewed change at a time, each writing `AUDIT_LOG`. No direct SQL, no edit of a committed `V__` file.
11. **Test debt.** Add a pytest case for the `application: deferred` validation rule (only approved claims can be deferred, and a reason is required). It needs `SANGITA_ALLOW_TEST_EDITS=1`, set deliberately for this one addition.
12. **Human gate.** Only named human approval promotes a claim. The reviewer for the 40 leads and for adjudication is an open question (below). Agent review supports checking only.
13. **Reporting.** After the sweep the generated concordance reports claims by final disposition, witnesses by `rights`, and the list of `edition_inaccessible` items so a future owner with edition access can reopen them.

### Design

- **Where it lives (extends TRACK-146, no fork):** `database/data/track146-syama-sastri-master.json` and `track146-syama-sastri-application-manifest.json`; `tools/krithi-extract-enrich-worker/src/musicological_classification.py` (validator, `rights` field, new reason codes); `scripts/generate_classification_master.py` (reporting); the generated `application_documentation/01-requirements/features/syama-sastri-classification-master.md`; the shared contract doc; `R__seed_10_syama_sastri_reviewed_memberships.sql` or a new `R__` successor only if a membership changes.
- **Review workflow:** an agent prepares a per-claim review sheet (claim, candidate public passages, locator candidates, recommended disposition). Seshadri, with the musicologist subagent as a checking aid, decides. Decisions are written to the master with approver, date and note. The generator then regenerates the manifest.
- **Reason codes:** extend `unresolved` with `reason` (`edition_inaccessible`, `no_public_source`, `sources_conflict`). Validator requires a reason on every `unresolved` claim touched by this track.
- **Option D, non-cycle application path** (decision needed):
  - **D1 (recommended): manifest-driven search metadata.** Approved non-cycle claims are carried in the manifest and read by the formatter. No schema change, no catalogue write, fully reversible, and consistent with the contract ("no additional public search API"). Mudra stays a documented claim rather than a catalogue column.
  - D2: catalogue attribute (new column or table via a new Flyway `V__`). Right if mudra should be filterable or shown in the UI, but it is a schema and DTO change across all three layers.
  - D3: new membership type on `krithi_cycle_memberships`. Rejected: a mudra is a property of a composition, not membership of a cycle.
- **Formatter:** if D1, add optional non-cycle clauses in the shared formatter within the 60-word budget, with unit tests for budget and abstention (no approved claim, no clause).
- **Audit:** reference seed changes use the existing provenance guards and system audit attribution. Canon corrections use the import service and write `AUDIT_LOG`. Re-runs create no audit events.
- **No UI work** is planned. If D2 is chosen, the admin UI surfacing is out of scope and becomes its own track.

### Flagged concerns

1. **"Public domain" is narrower than "publicly accessible".** The journal volumes on the Music Academy site are freely downloadable but copyrighted. The `rights` tiers make this explicit. Decision needed on whether `openly_hosted` locators may approve claims (recommended: yes, locator and short quote only).
2. **Approval authority.** The owner has no editions and, per the intent, no one has been named to adjudicate ambiguous claims. With public sources only, many claims cannot be approved at all. This is by design, but the track's value is mostly the honest `edition_inaccessible` ledger plus whatever the open journals settle.
3. **Scope realism.** Intent items 1 and 2 (obtain the four editions) cannot be met. They are replaced by requirements 1 to 3 above. The Intent text is otherwise unchanged.
4. **Paid actions.** Re-indexing needs a paid embedding run, which needs its own authorisation. Commits and production changes likewise.
5. **Lakshana.** Do not collapse Viloma Chapu (4+3) into Misra Chapu (3+4), and do not state eduppu, arudi or kalai without witness notation. Domain Model §6 governs.
6. **Audit path.** Requirement 10 must not be done with SQL. If a canon correction needs a code path that does not exist, it is a flagged gap, not a workaround.

### Musicologist review (2026-11-03, subagent; advisory, not an approval)

Adopted into this Spec as additional requirements:

14. **Witness kind.** Add `witness_kind` (`notated_edition`, `scholarly_article`, `secondary_text`) alongside `rights`. Eduppu, arudi, kalai and subdivision claims are approvable only from a `notated_edition`. A prose statement can approve a tala name only. Blogs and Sahapedia-type pages are `secondary_text`.
15. **Unit normalisation before conflict.** Convert to one unit and one counting origin (sama or tala start) before two sources are called conflicting. `sources_conflict` must not fire on arithmetic differences alone (anagata offsets of 1/2, 3/4 and 1½ matras; chapu half-beat counting).
16. **Counting convention field.** Misra Chapu is counted 3+4 in some sources and 3+2+2 in others. Every chapu claim records its grouping. Viloma Chapu (4+3) stays distinct from both.
17. **Tala claims carry jati and akshara count.** Standard Ata is khanda jati, 14 aksharas; a caturasra-jati Ata would be 12. Khanda Jhampa is 5+1+2 = 8. Without these, the Spec 12 title check (requirement 9) cannot be decided.
18. **Bhashanga ragas.** Anandabhairavi is a janya of Natabhairavi with an anya swara (verify). Scale facts and character/prayoga claims are separate, and any parent-subset check must exempt bhashanga ragas.
19. **Define "swara-sahitya count".** State what is counted (segments per charanam, or total passages), and keep it distinct from chittaswara in a varnam (Domain Model §6.1).
20. **Glossary.** Define arudi, eduppu, kalai and jati in the contract doc.

Corrections to the Evidence policy: the "before 1965" example is a rule of thumb for life + 60 and varies by jurisdiction, so the owner confirms each `public_domain` call. Sambamoorthy (d. 1973) is not public domain. *Sangita Sampradaya Pradarsini* (1904) is public domain and digitised, but it is Dikshitar-school, so treat it as a tala/raga attribution witness, not a complete source. Reviewer could not name other early collections with confidence; the sweep registers hits as unverified.

### Open questions carried forward

1. Which public sources exist for the 40 leads? Answered by the requirement 2 sweep.
2. Who adjudicates claims the public sources leave ambiguous? Proposed: Seshadri decides, with the musicologist subagent reviewing. A human subject-matter reviewer would be better if available.
3. Non-cycle storage: D1, D2 or D3? Recommended D1.
4. Is the 1954 souvenir retrievable in full from the Music Academy site? Answered by the sweep.
5. May `openly_hosted` (copyright-unknown) witnesses approve claims?

## Plan

**Status:** Accepted
**Accepted by:** Seshadri (chat, 2026-11-03)
**Accepted at:** 2026-11-03

**Decision (2026-11-03, Seshadri):** "public domain" means **publicly accessible**. Copyright is not a gate for now. Consequences: `openly_hosted` and `public_domain` witnesses are treated alike for approval (locator required, hash required); `rights` stays as an informational field only and no longer limits quoting. Fetched artifacts are still cached outside git for size, not for rights. This supersedes the copyright cautions in the Spec's Evidence policy and flagged concern 1.

Grounded in a read-only pass over `musicological_classification.py`, `generate_classification_master.py`, `embeddings/context_formatter.py`, `R__seed_10`, and the shared contract. No product code is changed until this Plan is Accepted.

### Files that change

| Area | File | Change |
|:---|:---|:---|
| Validator | `tools/krithi-extract-enrich-worker/src/musicological_classification.py` | `Witness`: add optional `rights`, `rights_note`, `witness_kind`; defaults derived from `status` so existing data still validates. `Claim`: add optional `unresolved_reason` (`edition_inaccessible`, `no_public_source`, `sources_conflict`), required when `review_state == unresolved` for claims this track touches. `RhythmObservation`: add `jati`, `akshara_count`, `grouping` (counting convention). Rules: rhythm axes (eduppu/arudi/kalai/subdivision) approvable only from `witness_kind == notated_edition`; no approval from `rights == inaccessible`. |
| Generator | `tools/krithi-extract-enrich-worker/scripts/generate_classification_master.py` and `MasterModel.render()` | Report claims by disposition, witnesses by `rights`, and the `edition_inaccessible` ledger. |
| Data | `database/data/track146-syama-sastri-master.json`, `track146-syama-sastri-application-manifest.json` | New witnesses from the sweep, dispositions for the 40 leads, adjudication results, `unresolved_reason` values. Generated manifest regenerated, never hand-edited. |
| Generated doc | `application_documentation/01-requirements/features/syama-sastri-classification-master.md` | Regenerated. |
| Contract doc | `application_documentation/01-requirements/features/musicological-classification-contract.md` | Add `rights`, `witness_kind`, `unresolved_reason`, unit-normalisation and counting-convention rules, glossary (arudi, eduppu, kalai, jati), non-cycle application path (D1). |
| Formatter (D1) | `tools/krithi-extract-enrich-worker/src/embeddings/context_formatter.py` | Optional non-cycle clauses (`Mudra`, `Raga Trait`, `Eduppu`) read from the manifest, within `MAX_HEADER_WORDS = 60`, abstaining when no approved claim exists. Only if the sweep produces an approved non-cycle claim; otherwise this stays unbuilt. |
| Tests | `tests/test_musicological_classification.py` | Deferred-validation case (needs `SANGITA_ALLOW_TEST_EDITS=1`, one deliberate use), plus `rights` / `witness_kind` / rhythm-convention cases. `tests/test_syama_formatter.py` for the D1 clauses. |
| Seed | `database/migrations/R__seed_10_syama_sastri_reviewed_memberships.sql` | Touched only if an approved claim changes a Ratnatrayam membership. Not expected. No `V__` file edited. |
| Track docs | `conductor/tracks/TRACK-148-*.md`, `conductor/tracks.md`, `current-versions.md` untouched | Checklist and Progress Log updates. |

Not changed: Kotlin backend, database schema, frontend. Catalogue corrections (Spec 10) are not planned as code: if the evidence demands one, it is raised as its own change through TRACK-149's audited path.

### Order of work

1. **Validator and model first (offline, no data).** Add the new fields with backward-compatible defaults; run existing tests to prove existing master still validates byte-for-byte (`--check` clean).
2. **Public-source sweep.** Fetch candidate public sources (Music Academy volumes not yet fetched, the 1954 souvenir URL already in `academy-1954-lead`, archive.org / Wikisource / DLI hits, *Sangita Sampradaya Pradarsini*). Cache outside git, record SHA-256, URL, access date, `rights`, `witness_kind`, and outcome including "nothing relevant". This step takes network access and is the main unknown.
3. **Review sheet for the 40 leads** plus the carried adjudication items, generated per claim with candidate passages and a recommended disposition. Present to Seshadri. Nothing is written as `reviewed` yet.
4. **Seshadri decides; musicologist subagent checks.** Approved decisions are written with approver, date, note and exact locator. Everything else is `unresolved` with a reason, or `disputed`.
5. **Resolve Spec 12 title check and Navaratnamalika** from what the sweep found, else record as unresolved.
6. **Regenerate** concordance and manifest with `--check` to prove determinism.
7. **Formatter D1 (conditional).** Build only if step 4 yields an approved non-cycle claim. Lift `application: deferred` for the four mudras only if their claims survive review and the D1 path is built and tested.
8. **Docs.** Update contract and concordance, Progress Log.
9. **Re-index is NOT run.** If step 7 happened, a re-index is proposed separately and needs its own paid authorisation.

### Risks and mitigations

- **Riskiest: step 2 yields little.** Public sources may not settle most leads. Mitigation: that is a valid outcome. The deliverable becomes the honest ledger; the plan does not widen scope to chase it.
- **Copyright.** Not a gate (see decision above). PDFs are cached outside git for size; hash and locator are still recorded.
- **False conflicts.** Unit-normalisation rule prevents `sources_conflict` from firing on arithmetic differences.
- **Validator regression.** New fields must default so the 638 existing claims validate; proved by an unchanged `--check`.
- **Single reviewer.** All approvals come from one person; the Plan keeps the subagent as a checker only and records that limitation in the concordance.
- **Test-edit gate.** The hook blocks test edits without `SANGITA_ALLOW_TEST_EDITS=1`. Used once, on purpose, for the deferred-rule test and new cases.

### Options rejected

- **D2 catalogue attribute / D3 cycle-membership type:** schema and DTO changes across three layers for four claims; D3 is musicologically wrong (mudra is not cycle membership).
- **Direct SQL or a new `V__` to apply mudras:** violates Flyway and audit policy for no benefit while D1 exists.
- **Forking the classification model for this track:** contract says extend, not fork.
- **Pursuing the print editions by interlibrary loan inside this track:** out of scope per your constraint; the ledger lets a future owner reopen it.

### Proof and acceptance criteria

Run from `tools/krithi-extract-enrich-worker` unless noted; the worker has no `make` target, so these are the direct commands:

```bash
uv run pytest tests/test_musicological_classification.py tests/test_syama_formatter.py tests/test_macro_indexer.py
uv run python scripts/generate_classification_master.py \
  --master ../../database/data/track146-syama-sastri-master.json \
  --output ../../application_documentation/01-requirements/features/syama-sastri-classification-master.md \
  --manifest ../../database/data/track146-syama-sastri-application-manifest.json --check
```

From the repository root: `make check-docs` (documentation links) and `make check-doc-impact`. If `R__seed_10` is touched: `make db-reset` then the `verify-import` skill (junction tables, not just FKs). `make test` only if backend code changes, which this Plan does not expect.

Done when: every one of the 40 leads has exactly one final disposition with a reason or locator; the generated outputs are current (`--check` clean); the concordance reports claims by disposition and witnesses by `rights`; no claim is `reviewed` without a named approver and a hashed available witness; all commands above pass and their output is pasted into the Progress Log.

## Implementation Checklist

- [x] Intent accepted (2026-11-03).
- [x] Spec accepted (2026-11-03).
- [x] Plan accepted (2026-11-03).
- [x] Implementation, up to the owner gate: validator fields, source sweep, review sheet for the 40 leads, carried findings.
- [x] Owner decisions on the 40 lead proposals recorded 2026-11-03 (Seshadri): 15 approved (deferred), 24 unresolved, 1 disputed; master and manifest regenerated.
- [ ] D1 formatter and lifting `application: deferred` for the four mudras, conditional on approvals.

## Progress Log

- **2026-10-15**: Track drafted from the TRACK-146 closure amendment. Intent is Draft and awaits acceptance. No implementation started.
- **2026-11-03**: Intent accepted by Seshadri. No print editions available; scope narrowed to publicly accessible sources. Spec drafted (Draft); awaits review and acceptance. No implementation started.
- **2026-11-03**: Spec accepted by Seshadri with the recommended defaults. Plan not yet written; no implementation started.
- **2026-11-03**: Plan drafted (Draft); awaits acceptance. No implementation started.
- **2026-11-03**: Seshadri clarified that public domain means publicly accessible; copyright is not a gate. Plan stays Draft.
- **2026-11-03**: Plan accepted by Seshadri. Implementation begins with the read-only steps.
- **2026-11-03**: Implementation to the owner gate. `Witness` gains `rights`, `rights_note`, `witness_kind`; `Claim` gains `unresolved_reason`; `RhythmObservation` gains `jati`, `akshara_count`, `grouping`. Rhythm interpretation needs a `notated_edition` witness and no claim is approved from an inaccessible witness. The existing master validates unchanged. Added the deferred-validation test (one deliberate use of `SANGITA_ALLOW_TEST_EDITS=1`, plus an updated witness count).
- **2026-11-03**: Source sweep (see `database/data/track148-149-source-sweep-ledger.md`). The 1954 souvenir (`academy-1954-lead`, now fetched) lists the nine Navaratnamalika gems, seven by title and two by raga only; the 1973 souvenir lists seven Navaratnamalika kritis for Meenakshi and the full set of attributed compositions with raga, tala and deity; JMA LV (1984) names four svara sahitya kritis. Review sheet `database/data/track148-lead-review-sheet.md`: of 40 leads, 15 recommended approve, 24 unresolved with a reason, 1 disputed. Nothing is approved.
- **2026-11-03**: Spec 9 resolved from public evidence: *Samini* (Anandabhairavi, Ata) is a distinct tana varnam, not a mistitled SS-0036, and the catalogue holds no varnam by Syama Sastri; adding *Samini* or *Dayanidhe* is a separate audited import decision. Spec 6: a nine-member Navaratnamalika is not recorded; seven titled members agree across two sources. The six disputed Anandabhairavi raga-character claims have no new public evidence and stay disputed. D1 formatter not built (conditional on approvals); no re-index.
- **2026-11-03**: Seshadri accepted the recommended outcomes on the lead sheet as their decisions. Recorded in `database/data/track148-lead-decisions.json` and applied to the master. 15 claims are now `reviewed` with `application: deferred` (one Viloma chapu, one khanda jhampa, four svara sahitya, two Apatya Bhava and seven Meenakshi geography claims); 24 are `unresolved` with a reason and `SS-0055` laya is `disputed` (Adi lead versus Rupaka in the 1973 list). Approved manifest changes: 18 to 33. No `unreviewed` claims remain. The seven Meenakshi approvals make seven compositions SPECIFIC_SHRINE_SUPPORTED. The D1 formatter is now unblocked but not built; lifting `deferred` for the four mudras still needs it.
