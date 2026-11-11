| Metadata | Value |
|:---|:---|
| **Status** | In Progress |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-11-11 |
| **Author** | Sangeetha Grantha Team |

# Track: Tyagaraja Edition Consultation & Collision Canon Split

**ID:** TRACK-149
**Status:** In Progress — Intent Accepted, Spec Accepted, Plan Accepted (implementation in progress)
**Owner:** Seshadri
**Created:** 2026-10-15
**Updated:** 2026-11-11

## Goal

Consult the institutional print editions and Music Academy Journal volumes registered as `unavailable` in TRACK-147, adjudicate the edition-dependent Kshetra Pancharatnam, Utsava Sampradaya, Divyanama, and dramatic-work leads, split the 8 raga/tala collision identities into 17 distinct catalogue compositions through the audited canon/import pipeline, and apply the non-cycle approvals deferred by TRACK-147.

## Context

- **Predecessor:** [TRACK-147](TRACK-147-tyagaraja-musicological-classification.md), completed 2026-10-15 under its Spec/Plan amendment. TRACK-147 owns the Tyagaraja classification master, `R__seed_11`, and the Ghana-raga Pancharatnam cycle integration; this track extends them without forking the schema or shared contract.
- **Companion:** [TRACK-148](TRACK-148-syama-sastri-edition-consultation-adjudication.md) (Syama Sastri edition consultation and adjudication).
- **Contract:** [Musicological classification contract](../../application_documentation/01-requirements/features/musicological-classification-contract.md).
- **Master and manifest:** `database/data/track147-tyagaraja-master.json` and `database/data/track147-tyagaraja-application-manifest.json`. The concordance is generated as [tyagaraja-classification-master.md](../../application_documentation/01-requirements/features/tyagaraja-classification-master.md).

## Intent

**Status:** Accepted
**Accepted by:** Seshadri (chat, 2026-11-03)
**Accepted at:** 2026-11-03

**Evidence-access amendment (2026-11-03, Seshadri):** no print edition is available. Only publicly accessible sources may be consulted. Edition-dependent items stay `unresolved` or `deferred` unless a public source is found and registered.

### Problem

TRACK-147 reconciled all 675 Tyagaraja catalogue identities, 684 classification subjects, and 687 CSV source rows, approved and seeded the 5 Ghana-raga Pancharatnam memberships (`R__seed_11`), and classified the corpus across devotional themes, addressees, ritual functions, dramatic works, and kshetra leads. Approved claims cite fetched `thyagaraja-vaibhavam.blogspot.com` witnesses (343 cached HTML files and 341 extraction checksums from `imported_krithis.parsed_payload`). The database snapshot (`db-snapshot`) is a hashed census, not scholarly authority. However, three categories of work remain intentionally deferred:

1. **Eight raga/tala collision identities (17 source witnesses):** Eight catalogue `krithis` rows (`jaya mangaLaM`, `pAhi rAma candra`, `paripUrNa kAma`, `Rama Rama Rama Sita`, `rAma sItA rAma`, `Sri Narada Muni`, `SrI nArasiMha`, `Sri Rama Jaya Rama`) absorb 17 distinct source imports whose raga or tala disagree. They remain marked `collision_not_classified` in the master while their 17 `tyagaraja:import:*` witnesses carry individual classifications.
2. **Unconsulted print and archive editions:** Print editions (`govinda-rao-tyagaraja`, `ramanujachari-spiritual-heritage`, `sambamoorthy-tyagaraja`) and unretrieved PDF volumes remain registered as `unavailable` witnesses, keeping Kshetra Pancharatnam sequences (Kovur, Tiruvottiyur, Lalgudi, Srirangam, including the missing *Vinarādhā nā manavi* lead), Utsava Sampradaya / Divyanama bhajana paddhati orderings, and full *Nauka Charitram* / *Prahlada Bhakti Vijayam* verse/prose context as `unreviewed` or `unresolved` claims.
3. **Deferred non-cycle approvals & structural defects:** Approved claims outside the Ghana-raga Pancharatnam cycle are marked `application: deferred` in the master and manifest pending a non-cycle application path, and the 27 missing-pallavi and 4 missing-charanam catalogue records await audited lyric-section remediation.

### Proposed outcome

1. **Edition access.** Obtain lawful access to T.K. Govinda Rao's *Compositions of Tyagaraja*, C. Ramanujachari & V. Raghavan's *The Spiritual Heritage of Tyagaraja*, P. Sambamoorthy's dramatic-work editions, and the Music Academy Journal volumes (1947, 1968, 2017). Register each consulted edition as a hashed witness.
2. **Collision split via audited canon workflow.** Split or adjudicate the 8 collapsed `krithis` identities across their 17 `imported_krithis` witnesses through the audited canon/import service path (preserving `krithi_source_evidence`, `krithi_ragas`, and `AUDIT_LOG`), then rebind the `tyagaraja:import:*` witness subjects to their new `tyagaraja:krithi:*` identities.
3. **Kshetra Pancharatnam & ritual collection adjudication.** Adjudicate Kovur, Tiruvottiyur (`sundari nI divya`), Lalgudi (`ISa pAhi`), and Srirangam (`karuNa jUDavayya` in Sāranga) Pancharatnam memberships and shrine associations (`temples` / `deities`), plus Utsava Sampradaya and Divyanama memberships, and seed approved cycles via `R__seed_11`.
4. **Dramatic works (*Nauka Charitram* & *Prahlada Bhakti Vijayam*).** Verify constituent daru/kirtana memberships, item order, speakers, addressees, and divisions against consulted editions, and decide whether padya/vacana context needs a dedicated work anchor.
5. **Deferred application & structural remediation.** Lift `application: deferred` on approved non-cycle claims once an application path is accepted, and remediate the 27 `missing_pallavi` and 4 `missing_charanam` records from verified witnesses without inventing text.

### Affected users and systems

- Musicologists and curators: primary-edition adjudication and collision demerging.
- Data: `track147-tyagaraja-master.json`, the application manifest, the generated concordance, `R__seed_11`, and audited canon splits.
- Search: composition headers, Kshetra/Ritual/Dramatic cycle overviews, and live vector re-indexing (paid embedding run).

### Constraints

- Follow [CLAUDE.md](../../CLAUDE.md) and the Conductor workflow: Intent → Spec → Plan acceptance before implementation.
- Flyway only for schema/reference seeds; audited canon/import service paths for splitting the 8 collision identities and repairing missing lyric sections.
- Only explicit human approval promotes a claim. Paid embedding runs, commits, and production changes each need separate authorization.

### Open questions

1. Which print editions of Govinda Rao, Ramanujachari/Raghavan, and Sambamoorthy will be used as the primary locators?
2. For each of the 8 collision identities, which witness retains the existing `krithi_id` and which witnesses receive newly minted `krithis` rows?
3. Should approved non-cycle themes, ritual functions, and `desadi` tala notes be surfaced as separate search-header micro-clauses in `context_formatter.py` alongside `TRACK-148`?

## Spec

**Status:** Accepted
**Accepted by:** Seshadri (chat, 2026-11-03)
**Accepted at:** 2026-11-03

**Acceptance note:** accepted with the Spec's recommended defaults for its open decisions: `openly_hosted` witnesses may approve with a locator and short quote; Seshadri adjudicates with the musicologist subagent checking; non-cycle path is option D1.


### Evidence policy (applies to every requirement)

No print edition is available to the owner, so the edition-dependent work runs on **publicly accessible sources only**, using the same `rights` tiers as [TRACK-148](TRACK-148-syama-sastri-edition-consultation-adjudication.md) (`public_domain`, `openly_hosted`, `inaccessible`). The collision split (requirement 3) does not need an edition: it is a catalogue-integrity task over data already in the repository. It can proceed first.

### Requirements

1. **Witness rights.** Adopt the shared optional `rights` / `rights_note` witness fields from TRACK-148. Default existing `fetched` witnesses to `openly_hosted` and `unavailable` ones to `inaccessible`. The three print editions (`govinda-rao-tyagaraja`, `ramanujachari-spiritual-heritage`, `sambamoorthy-tyagaraja`) are recorded as `inaccessible`.
2. **Public-source sweep.** Try the unretrieved witnesses that are reachable: `ma:jma-vol18-1947`, `ma:jma-vol39-1968`, `ma:jma-2017` (Music Academy catalogue PDFs) and `sanskrit-docs-tyagaraja` (`sanskritdocuments.org`). Search public-domain digitisations (archive.org, Wikisource, Digital Library of India) for Tyagaraja kriti collections and for *Nauka Charitram* and *Prahlada Bhakti Vijayam*. Register each with SHA-256, URL, access date and rights, including "searched, nothing relevant". No full copyrighted text enters the repository.
3. **Collision split (8 identities, 17 witnesses).** For each of the eight collided `krithis` rows (`jaya mangaLaM`, `pAhi rAma candra`, `paripUrNa kAma`, `Rama Rama Rama Sita`, `rAma sItA rAma`, `Sri Narada Muni`, `SrI nArasiMha`, `Sri Rama Jaya Rama`):
   1. Produce a decision sheet listing every `tyagaraja:import:*` witness with its raga, tala, source URL and lyric hash, and what differs (raga, tala or both).
   2. Decide per identity: **split** (distinct compositions that share a title) or **keep merged** (the same composition with a source-labelling difference). Raga or tala disagreement alone does not prove distinct compositions. Record the evidence for each decision, and keep a merge where a public source shows a single composition.
   3. For each split, name the witness that **retains** the existing `krithi_id` and the witnesses that receive new `krithis` rows. Prefer retaining the witness that existing links (`krithi_ragas`, tags, evidence, search documents) already describe.
   4. Execute through the audited canon/import service path, preserving `krithi_source_evidence`, populating the junction tables (`krithi_ragas`, tala links, lyric sections) for every new row, and writing `AUDIT_LOG`. No direct SQL.
   5. Rebind the `tyagaraja:import:*` witness subjects to their new `tyagaraja:krithi:*` identities. New master subject IDs follow the contract (minted once, never recomputed).
   6. Move the identities out of `collision_not_classified`. Each resolved witness is classified, or explicitly left unclassified with a reason.
   7. Expected end state if all eight split fully: 8 rows become 17 (9 new rows); the catalogue count rises from 675 to 684. Fewer splits are a valid outcome and change the count accordingly. The accounting section of the master must reconcile at every outcome.
4. **Kshetra Pancharatnam, Utsava Sampradaya, Divyanama.** Adjudicate Kovur, Tiruvottiyur (`sundari nI divya`), Lalgudi (`ISa pAhi`) and Srirangam (`karuNa jUDavayya`, Sarangam) and the *Vinarādhā nā manavi* lead, plus the Utsava Sampradaya and Divyanama sequences, **only** where a registered public witness supports them. Otherwise they stay `unresolved/edition_inaccessible`. Shrine associations (`temples`, `deities`) follow the same rule. Approved cycles are seeded through `R__seed_11` or a successor `R__` with the existing provenance guards, conflict abort and system audit attribution.
5. **Dramatic works.** Verify the constituent kirtana/daru memberships, order, speakers, addressees and divisions of *Nauka Charitram* and *Prahlada Bhakti Vijayam* against public witnesses. Decide whether padya/vacana context needs a work anchor. Absent a public witness, record the current state as unverified and add nothing.
6. **Deferred application path.** Adopt whatever non-cycle path TRACK-148 accepts (its Design option D, recommended D1 manifest-driven search metadata), so both composers share one mechanism. `application: deferred` on approved non-cycle Tyagaraja claims is lifted only after that path exists. Do not fork.
7. **Structural remediation.** The 27 `missing_pallavi` and 4 `missing_charanam` records are repaired **only** from verified witnesses, through the audited lyric-section path, never inferred or invented. Records with no verifiable witness are listed as "unrepairable without source" and left unchanged. Section structure follows Domain Model §6 (KRITHI requires pallavi and charanam; check the composition's form first).
8. **Search headers.** Reuse TRACK-148's formatter decision for non-cycle micro-clauses (theme, ritual function, `desadi` tala note) so there is one shared design. The 60-word budget holds. A re-index needs separate paid authorisation and is not part of this track's implementation.
9. **Human gate.** Only named human approval promotes a claim. Agent review supports checking only. Collision split decisions are approved by the owner before any write.
10. **Reporting.** The regenerated concordance reports: collisions resolved/kept merged, claims by disposition, witnesses by rights, repaired and unrepairable structural records, and the `edition_inaccessible` ledger.

### Design

- **Where it lives (extends TRACK-147, no fork):** `database/data/track147-tyagaraja-master.json` and `track147-tyagaraja-application-manifest.json`; the TRACK-147 classification tooling and tests (`test_tyagaraja_classification.py`, `test_tyagaraja_source_inventory.py`); `R__seed_11_tyagaraja_reviewed_memberships.sql`; the generated `tyagaraja-classification-master.md`; the shared contract doc; the backend canon/import service for splits and section repair (`modules/backend/api`, `modules/backend/dal`).
- **Collision split mechanism:** first inspect the existing backend merge/canon service for a split or re-point operation. If one exists, use it. If it does not, that is a **backend change** (service method, route, repository, DTOs, tests, audit) and must be written into the Plan as such; it is not to be replaced with SQL. The `ktor-exposed-backend` layer rules apply: `DatabaseFactory.dbQuery`, DTOs not entities, audit on every mutation, junction tables populated.
- **Order of work:** (1) decision sheet for the 8 identities from repository data only; (2) owner decisions; (3) service capability check; (4) split with before/after verification across DB, API and UI (the `verify-import` skill, junction tables included); (5) rebind and regenerate the master; (6) public-source sweep and adjudication; (7) structural repair.
- **Verification:** a post-split check that every new row has its raga link, tala, source evidence, lyric sections and audit entry, and that no `krithi_id` referenced by the other 667 single-source rows changed.
- **Why splitting is the safe-to-start slice:** it needs no edition and removes an accounting hole (`collision_not_classified`), but it is also the only requirement that writes catalogue rows, so it is the one that needs the strictest gate.

### Flagged concerns

1. **A raga or tala mismatch is not proof of two compositions.** Tyagaraja kritis circulate in variant raga and tala attributions. A title shared by two witnesses may be one composition recorded differently. The Intent frames this as a "split into 17"; the Spec makes it a per-identity decision, and 17 is an upper bound, not a target. The musicologist subagent should review the decision sheet.
2. **Catalogue write path.** If no audited split operation exists, the track needs backend work that the Intent did not scope. The Plan must size it.
3. **Public-domain scope.** As for TRACK-148: freely hosted is not the same as public domain. The edition-dependent items (requirement 4, 5) will largely end as `edition_inaccessible`.
4. **Structural repair risk.** Filling a missing pallavi or charanam from a different witness can merge two versions of a text. Repair only from a witness already attached to the same composition, and record which.
5. **Cross-track coupling.** Requirements 6 and 8 depend on TRACK-148's decisions (option D, formatter). Sequence TRACK-148's Spec acceptance first, or accept that these two requirements wait.
6. **Paid and external actions.** Re-indexing, commits, production changes: separate authorisation each time.
7. **Companion artifacts.** `Samini Rammanave`-style title issues in the other track show that titles drift between sources. Compare by identity and lyric hash, not title alone.

### Musicologist review (2026-11-03, subagent; advisory, not an approval)

Adopted into this Spec as additional requirements:

11. **Collision decision rules.** The sheet applies, in order: (a) normalise raga and tala aliases first (for example Saranga/Sarangam, Adi/Desadi; verify each) so a label difference is not counted as a real one; (b) compare per-section lyric hashes after transliteration normalisation, not a whole-text hash; (c) same pallavi with different anupallavi or charanams means distinct kritis or a recension, which is a human call; (d) generic incipits (*Rama Rama Rama Sita*, *rAma sItA rAma*, *Sri Rama Jaya Rama*) are likely distinct pieces, including Divyanama, Utsava and dramatic-work items sharing an incipit with a standalone kriti; (e) check for ragamalika or a raga change before treating a mismatch as a conflict; (f) record doubtful attribution to Tyagaraja separately. A shared title does not prove one composition either.
12. **Form matrix before repair.** Classify each of the 27 `missing_pallavi` and 4 `missing_charanam` records by form first (Domain Model §6.1). Padya, sloka, dvipada and vacana items in the dramatic works legitimately have no pallavi or charanam, and short Divyanama or Utsava items may have one charanam. Those are reclassified, not repaired.
13. **Relabel versus insert.** A missing pallavi is often a mislabelled first section or an omitted refrain. Relabelling existing text with evidence is allowed. Inserting text is forbidden. Sections are never silently merged.
14. **Order provenance.** Utsava and Divyanama sequence is editorial or traditional. Record the source of every order.
15. **Names.** *Vinarādhā nā manavi* uses a different transliteration scheme from the other titles. The Intent's "Sāranga" and the Spec's "Sarangam" may be different ragas. A human checks both before they are conflated.
16. **Witness kind.** Adopt TRACK-148's `witness_kind`. The `thyagaraja-vaibhavam` blog is `secondary_text`, not a scholarly authority, so it can support a tala name or a lead but not a rhythm or ordering claim.

Corrections to the Evidence policy: as in TRACK-148 (public-domain test is jurisdiction-dependent; Sambamoorthy is not public domain).

### Open questions carried forward

1. Which public witnesses exist for the kshetra, Utsava, Divyanama and dramatic-work items? Answered by the requirement 2 sweep.
2. For each of the 8 identities, which witness keeps the existing `krithi_id`? Needs the decision sheet and the owner's call.
3. Does the backend already have an audited split or re-point operation for collided canon rows?
4. Non-cycle header clauses: same decision as TRACK-148 (recommended D1).
5. May `openly_hosted` witnesses approve claims? Same decision as TRACK-148.

## Plan

**Status:** Accepted
**Accepted by:** Seshadri (chat, 2026-11-03)
**Accepted at:** 2026-11-03

**Decision (2026-11-03, Seshadri):** "public domain" means **publicly accessible**. Copyright is not a gate for now. Consequences: `openly_hosted` and `public_domain` witnesses are treated alike for approval (locator required, hash required); `rights` stays as an informational field only and no longer limits quoting. Fetched artifacts are still cached outside git for size, not for rights. This supersedes the copyright cautions in the Spec's Evidence policy and flagged concern 1.

**Decision (2026-11-03, Seshadri): split execution uses Option A**, a new audited `unmapImport` backend operation, followed by the existing `reviewImport(APPROVED, null)` to create each new krithi. Options B and C are not used. Backend files in the table above are now definite. The unmap is still designed only after step 1 shows what attaches to each witness, so step 3 below becomes "finalise the `unmapImport` design", and the Plan is amended in the same change if that design diverges.

Grounded in a read-only pass over `tyagaraja_classification.py`, the TRACK-147 master, `ImportService.reviewImport`, `RemediationService`, `CuratorRoutes`, migration V47 and `R__seed_11`.

### Plan amendment (2026-11-03, found while implementing)

`ImportService.reviewImport` runs a TRACK-062 title dedup before it creates a krithi. An unmapped import approved with no `mappedKrithiId` would therefore be **re-linked to the same krithi**, which is how the collisions arose. The split needs both halves:

1. `POST /v1/admin/imports/{id}/unmap` (new `unmapImport`): detach, remove only the evidence for that import's source URL, reset to `PENDING`, audit twice (import and krithi). Fails closed if the import is unmapped, is the krithi's last witness, or supplied the krithi's lyric variants.
2. `ImportReviewRequest.forceNew` (default `false`, so existing callers are unchanged): skips the dedup so approval mints a krithi of its own.

Files added to the table below: `ImportRequests.kt` (`forceNew`, `UnmapImportRequest`), `ImportRoutes.kt`, `ImportService.kt`, DAL `ImportRepository.countMappedTo`, `SourceEvidenceRepository.deleteByKrithiAndSourceUrl`, `KrithiLyricRepository.countVariantsBySourceReference`, and a new `UnmapImportTest`. No schema change, no migration.

### What the code review found (changes the plan)

1. **There is no audited operation that splits a collided krithi.** `RemediationService` only merges duplicate lyric variants. The Curator `attach` / `confirm-new` routes act on the **raga** resolution queue, not on krithis.
2. **Promotion already works.** `ImportService.reviewImport(APPROVED, mappedKrithiId = null)` creates a new krithi in one transaction (TRACK-112), with audit and revision attribution. But an import that is already `APPROVED` and mapped is *adopted* back into its existing krithi (the "alreadyPromoted" branch). So a split first needs an **unmap**, which does not exist.
3. **Precedent: migration V47** demerged a Dikshitar ragamalika with a Flyway `V__` that hard-codes UUIDs, deletes the wrong variants, nulls `mapped_krithi_id`, resets the extraction queue and inserts `audit_log` rows. It is Flyway-compliant but environment-specific, because catalogue UUIDs are generated at import and differ in a rebuilt catalogue.
4. **Tyagaraja tooling is separate from Syama's.** `tyagaraja_classification.py` has its own `Witness`, `Approval` and `ClaimAxis`, and the contract says it stays independent until an explicit adapter migration. TRACK-148's new fields must therefore be added to **both** modules, with a parity test, not shared by import.
5. **Cannot plan the exact unmap until the impact is known:** which variants, sections, source evidence, junction rows and search documents attach to each of the 17 witnesses. That is step 1.

### Files that change

| Area | File | Change |
|:---|:---|:---|
| Worker validator | `tools/krithi-extract-enrich-worker/src/tyagaraja_classification.py` | Add `rights`, `rights_note`, `witness_kind` to `Witness`; `unresolved_reason` on `ClassificationClaim`; collision disposition values (`split`, `kept_merged`, `unresolved`). Update `EXPECTED_*` accounting constants to follow the decisions. |
| Generators | `scripts/generate_tyagaraja_master.py`, `scripts/populate_tyagaraja_master.py` | Rebuild master and snapshot after the split; report rights, dispositions, repaired/unrepairable records. |
| Data | `database/data/track147-tyagaraja-master.json`, `track147-tyagaraja-application-manifest.json`, `track147-catalogue-snapshot.json` | Collision decisions, rebound witness subjects, new witnesses, dispositions. Regenerated, not hand-edited. |
| Generated doc | `application_documentation/01-requirements/features/tyagaraja-classification-master.md` | Regenerated. |
| Backend (conditional, see Option A) | `modules/backend/api/.../services/ImportService.kt` (+ `ImportReviewer` interface), `routes/ImportRoutes.kt`, `modules/backend/dal/.../repositories` (imports, lyric variants, source evidence), `modules/shared/domain` DTOs | New audited `unmapImport` operation and route. |
| Backend tests | `modules/backend/api/src/test/.../services/` and `routes/` | Unit and integration tests for unmap + re-promotion. |
| Seed | `database/migrations/R__seed_11_tyagaraja_reviewed_memberships.sql` | Only if a kshetra / Utsava / Divyanama cycle is approved. Else untouched. |
| Contract and tests | contract doc (shared edits owned by TRACK-148); `tests/test_tyagaraja_classification.py`, `tests/test_tyagaraja_source_inventory.py` | Parity and accounting tests. |

### How to execute the split (decided: Option A)

- **Option A (recommended): new audited `unmapImport` service operation**, then the existing `reviewImport(APPROVED, null)` creates each new krithi. It is generic, UUID-independent (works on a rebuilt catalogue), audited, and covered by tests. Cost: backend change across DAL, service, route and tests.
- **Option B: V47-style Flyway migration** with hard-coded UUIDs and guarded updates. Cheaper, but specific to this database, silently a no-op on a rebuilt one, and its SQL audit rows are hand-written. It conflicts with the accepted Spec's "audited canon/import service path, no direct SQL".
- **Option C: do nothing to the catalogue;** record the 8 identities as `kept_merged` or `unresolved` in the master only. Honest if the decision sheet shows most are not distinct.

Decided: A. If the decision sheet shows no identity is really distinct, no unmap is built and the outcome is recorded as `kept_merged`; that is the only case where the backend work is skipped.

### Order of work

1. **Impact and decision sheet (read-only, repository data only).** For each of the 8 identities and 17 witnesses: raga, tala (after alias normalisation), per-section lyric hashes, source URL, what hangs off the existing krithi (variants, sections, evidence, `krithi_ragas`, tags, search documents). Apply the rules in Spec requirement 11 in order. No writes.
2. **Subagent musicologist reviews the sheet; Seshadri decides** per identity: split / keep merged / unresolved, and which witness retains the `krithi_id`. Recorded in the master. This is the gate for any write.
3. **Finalise the `unmapImport` design** from the impact inventory (what to detach, what to remove, audit metadata). Builds only for identities decided `split`.
4. **Execute splits** (if any). Per identity: unmap, remove only what is attributable to the unmapped witness, re-promote through `reviewImport`, populate junction rows (`krithi_ragas`, tala, sections), audit. One identity at a time, each verified before the next.
5. **Verify with the `verify-import` skill** against DB, API and UI. Check the other 667 single-source krithi are untouched.
6. **Rebind** witness subjects to new `tyagaraja:krithi:*` ids, rebuild snapshot and master, reconcile accounting (675 + number of splits = catalogue identities).
7. **Public-source sweep** for the kshetra, Utsava, Divyanama and dramatic-work items (`ma:jma-vol18-1947`, `ma:jma-vol39-1968`, `ma:jma-2017`, `sanskritdocuments.org`, archive.org / DLI). Record rights and outcome for each.
8. **Adjudicate** only what a registered public witness supports; the rest ends `unresolved/edition_inaccessible`. Seed approved cycles through `R__seed_11` with its existing provenance guards.
9. **Structural triage of the 27 + 4 records:** classify by form first (Spec requirement 12). Reclassify padya/vacana/short items; relabel with evidence where a section is mislabelled; insert nothing. List the unrepairable.
10. **Formatter and deferred application:** adopt TRACK-148's D1 outcome; depends on TRACK-148 step 7. If TRACK-148 builds nothing, this track records the dependency and stops.
11. **Docs and regeneration;** no re-index.

Steps 1 to 2 can start immediately and need nothing from TRACK-148. Step 10 waits for it.

### Risks and mitigations

- **Riskiest: step 4.** It writes catalogue rows and removes data. Mitigation: owner approval per identity, one at a time, take a database backup first (`pg_dump`) and verify before the next, rollback by restoring if verification fails.
- **Wrong merge or wrong split.** A raga/tala mismatch can be one composition. Mitigation: the rule order, musicologist review, and `kept_merged` as a valid outcome.
- **Losing evidence on unmap.** Unmapping could orphan `krithi_source_evidence` or search documents. Mitigation: step 1 inventories them; step 5 checks them.
- **Stale search documents** after a split. Mitigation: flagged, re-index proposed separately (paid, needs authorisation).
- **UUID instability.** Hard-coded UUIDs break on rebuild; Option A avoids this, and the master's rebindable-binding design already tolerates it.
- **Cross-track coupling** on D1; mitigated by sequencing and the explicit stop in step 10.
- **Structural repair inventing text.** Mitigated by the relabel-not-insert rule.

### Options rejected

- **Editing `krithis` rows or lyric sections by ad-hoc SQL:** violates the accepted Spec and CLAUDE.md.
- **Merging both tooling stacks into one now:** the contract defers the adapter migration.
- **Splitting all 8 into 17 by default:** the Spec makes 17 an upper bound.
- **Running a re-index in this track:** separate paid authorisation.

### Proof and acceptance criteria

From `tools/krithi-extract-enrich-worker`:

```bash
uv run pytest tests/test_tyagaraja_classification.py tests/test_tyagaraja_source_inventory.py tests/test_musicological_classification.py
```

and the Tyagaraja generator's own `--check` mode (the exact flags are read from `scripts/generate_tyagaraja_master.py` at implementation time and pasted into the Progress Log). From the repository root: `make check-docs`, `make check-doc-impact`.

If any catalogue write happens: `make test` and `make test-integration` (new unmap/re-promotion tests included), `make db-reset` to prove migrations and seeds still apply cleanly, and the `verify-import` skill for the split krithis (junction tables `krithi_ragas`, lyric sections, source evidence, audit rows). If the Kotlin backend or shared DTOs change, rebuild per the `sangita-restart-on-kotlin-change` skill and exercise the changed flow through the API, since no UI is changed.

Done when: each of the 8 identities has a recorded decision with evidence; the catalogue count matches the master accounting; every new krithi has its junction rows, lyric sections, source evidence and audit entry; the 667 single-source krithi are unchanged; every edition-dependent item has a final disposition or an `edition_inaccessible` reason; and all commands above pass with their output pasted into the Progress Log.

## Implementation Checklist

- [x] Intent accepted (2026-11-03).
- [x] Spec accepted (2026-11-03).
- [x] Plan accepted (2026-11-03).
- [x] Implementation, up to the owner gate: collision split, source sweep, lead proposals, structural-repair diagnosis and the first repair.
- [x] Owner decisions on 94 lead proposals recorded 2026-11-03 (Seshadri): 25 approved (deferred), 69 unresolved; master regenerated.
- [x] Controlled re-extraction run to repair the remaining 29 structural records: 27 repaired, 2 documented exceptions (2026-11-11).
- [x] Non-cycle application path: TRACK-148 D1 adopted (shared formatter and manifest loader). The 25 Tyagaraja approvals stay deferred.
- [x] Kshetra Pancharatnam membership seed: `R__seed_13` applies the 10 approved kshetra group claims (the 15 in the manifest include the 5 Ghana-raga ones already applied by `R__seed_11`); the Tiruvottiyur claim is geography, not a membership (2026-11-11).
- [x] Re-index (separately authorised paid run) for Syama Sastri and Tyagaraja with before/after search evidence (2026-11-11; 1 krithi, *kAla haraNa*, failed on a timeout and was not retried).
- [ ] Owner decision pending: relabel *munupE teliyaka* with evidence or accept the typo; decide how to reclassify *duDuku gala*.
- [ ] Owner decision pending: the "Ivasudha Neevanti" title looks corrupted; a duplicate `Poornalalitha` raga row exists.
- [ ] Remaining Tyagaraja claims (ritual, dramatic, theme, deity, geography) stay deferred until the owner lifts them.

## Progress Log

- **2026-10-15**: Track drafted from the TRACK-147 completion scope to own print-edition consultation, the 8-identity / 17-witness collision split, Kshetra/Utsava/Dramatic cycle promotion, and deferred non-cycle claim application. Intent is Draft and awaits acceptance.
- **2026-11-03**: Intent accepted by Seshadri. No print editions available; edition-dependent scope narrowed to publicly accessible sources. Spec drafted (Draft); awaits review and acceptance. No implementation started.
- **2026-11-03**: Spec accepted by Seshadri with the recommended defaults. Plan not yet written; no implementation started.
- **2026-11-03**: Plan drafted (Draft); awaits acceptance. No implementation started.
- **2026-11-03**: Seshadri chose Option A (`unmapImport`) and clarified that public domain means publicly accessible; copyright is not a gate. Plan stays Draft.
- **2026-11-03**: Plan accepted by Seshadri. Implementation begins with the read-only steps.
- **2026-11-03**: Plan step 1 done (read-only). Decision sheet at `database/data/track149-collision-decision-sheet.md`: all 8 identities look like distinct compositions; the witness already feeding each krithi can retain its `krithi_id`, so the split needs only unmapping, not deletion of lyric data. Awaiting owner decisions (step 2). No catalogue write.
- **2026-11-03**: Seshadri approved all 8 splits. Decisions recorded in `track147-tyagaraja-master.json` (`CollisionEntry.decision`), concordance regenerated, 28 worker tests pass. Implementation of `unmapImport` + `forceNew` started; Plan amended above.
- **2026-11-03**: Backend built per amended Plan: `unmapImport` (service, `POST /v1/admin/imports/{id}/unmap`, DAL helpers) and `ImportReviewRequest.forceNew`. New `UnmapImportTest`: 6/6 pass. `make test` (BUILD SUCCESSFUL). Splits NOT executed: they need a restarted backend and an admin session, and a `pg_dump` backup first.
- **2026-11-03**: Executed 8 of 9 splits (backup taken first; each went unmap 200, review 200). Verified in DB: each new krithi has 6 lyric variants, sections, 1 evidence row, raga junction row and 4 audit entries; the retained krithis kept their variants and lost one evidence row. **Sri Narada Muni not split**: the guard refused because 5 of its 6 variants came from the witness being split off. Awaiting owner decision on removing them. Master not yet rebound/regenerated (waits for the 9th).
- **2026-11-03**: Executed the 9th split (*Sri Narada Muni*). Owner approved removing the 5 variants that came from the split-off witness; done via the existing audited reingest of the retained Bhairavi witness (`REINGEST_MAPPED_KRITHI`), then unmap + `forceNew` review of c702d09d. New krithi `6d55561e-eedf-48c1-b1ea-2961a2750965` (draft / UNESTABLISHED, 6 variants). All 9 splits complete.
- **2026-11-03**: Rebound and regenerated the master. The 17 witness subjects became catalogue identities (`tyagaraja:krithi:*`; claim ids re-prefixed), the 8 register entries are `split_executed`, accounting is 684 identities / 684 single-source / 0 collisions. Missing-pallavi count 27 -> 26 (the retained *Sri Narada Muni* krithi gained a pallavi when its witness was reingested). Added `split_executed` validation and 3 tests; edited one committed test (`SANGITA_ALLOW_TEST_EDITS=1`) because it assumed register ids are never subjects. Documented `unmap` and `forceNew` in the OpenAPI file and API contract. Proof: worker suite 541 passed, `UnmapImportTest` 6 passed, `make check-docs` and `make check-doc-impact` pass, `verify-import` queries clean. Not done: step 2 (public-source sweep), 4-8.
- **2026-11-03**: Public-source sweep. All 188 Music Academy journals and souvenirs fetched and hashed (2 broken links, 3 scans without a text layer); other public sources checked (see `database/data/track148-149-source-sweep-ledger.md`). Witnesses `ma:jma-vol18-1947`, `ma:jma-vol39-1968`, `ma:jma-2017` and `sanskrit-docs-tyagaraja` moved from unavailable to fetched; seven new witnesses registered. Public passages name the Srirangam, Lalgudi and (partly) Kovur Pancharatnams, one Tiruvottiyur kriti and a few Utsava Sampradaya and dramatic-work kritis. Findings are in `database/data/track149-lead-review-sheet.md`: of 94 valued leads, 25 are recommended for approval and 69 stay unresolved (`edition_inaccessible` or `no_public_source`). Nothing is approved; only the owner can promote a claim. Added `rights`, `witness_kind` and `unresolved_reason` to both models and a shared decision mechanism (`src/lead_decisions.py`) whose approvals are `application: deferred`.
- **2026-11-03**: Structural repair. The 30 records are not missing text: the sources carry the pallavi as an inline `P` label with a footnote digit, which an older parser dropped. The current parser finds it on all 25 cached pages after one fix (`1 P ...` form, with a test that fails without it). *Sri Rama Jaya Rama* was repaired through the audited reingest (missing_pallavi 26 to 25). The other 29 need a controlled re-extraction run; see `database/data/track149-structural-repair-report.md`.
- **2026-11-03**: Dramatic works, Utsava, Divyanama and kshetra adjudication stay at recommendation stage. D1 search-metadata formatter not built, because it is conditional on approved non-cycle claims.
- **2026-11-03**: Seshadri accepted the recommended outcomes on the lead sheet as their decisions. Recorded in `database/data/track149-lead-decisions.json`; the generator applies them. 25 claims are now `reviewed` with `application: deferred` (Srirangam, Lalgudi and Kovur kshetra group and geography claims, one Tiruvottiyur geography claim, two Utsava Sampradaya and two dramatic-work claims); 69 are `unresolved` with a reason. Approved application manifest changes: 53 to 78. Nothing reaches the catalogue until a membership seed (kshetra groups) and the D1 path exist.
- **2026-11-03**: Adopted TRACK-148 D1 for Tyagaraja non-cycle claims (`[Mudra]`, `[Ritual Function]`, `[Dramatic Context]`, `[Theme]`), so both composers share one mechanism. The approvals recorded today stay `deferred`; lifting them needs an owner decision. Kshetra group approvals need a membership seed, not D1.
- **2026-11-11**: Structural repair run (authorised by Seshadri). `pg_dump` taken first and restore-tested: `storage/backups/track149-pre-reextract-20261111-0914.dump` (gitignored). Re-extracted and reingested the 29 records one source URL at a time through `re-extract` + `reingest` (`REINGEST_MAPPED_KRITHI`), in batches of 3, 8, 13 and 5, spot-checking each. Result: missing_pallavi 25 to 0 and missing_charanam 4 to 2. Found and fixed a second parser family (footnote digit glued to `pallavi`/`caraNam` headers, worker suite 570 passed). One regression (*Kalugunaa* lost its raga link to an ambiguous Poornalalita/Poornalalitha match) was caught by the per-batch check and restored through the audited `disambiguate` route. Two documented exceptions: *duDuku gala* (no charanam by form) and *munupE teliyaka* (source typo `cararaNam`). Master section flags, snapshot and concordance regenerated (`--check` passes). DB, API and UI verified. Details in `database/data/track149-structural-repair-report.md`.
- **2026-11-11**: Kshetra membership seed. Added `database/migrations/R__seed_13_tyagaraja_kshetra_pancharatnam_memberships.sql` (three new tags, ids ...0d/0e/0f, and 10 `krithi_cycle_memberships` plus `krithi_tags` rows with TRACK-149 provenance guards and system audit rows). Lifted the deferral on exactly the 10 owner-approved claims through a new `application`/`lift_reason` field in `track149-lead-decisions.json` (the generator rebuilds the master from that file, so a hand edit of the master would be overwritten). Nothing unapproved was applied; the Tiruvottiyur geography claim stays deferred. Reviews: `carnatic-musicologist` found the raga/tala pairings plausible and the two Lalgudi titles distinct, but pointed out that the Srirangam and Lalgudi sources name five kritis (so descriptions now say "partial set", not "set not established") and that *Ivasudha Neevanti* looks like a corrupted title (`Ee vasudha nIvanti`), left for an owner decision; `postgres-engineer` found no blockers, and its `DROP TABLE IF EXISTS` and missing-krithi notice suggestions were adopted. Backend: the three kshetra slugs were added to `OPEN_COLLECTION_SLUGS` so the API reports `isComplete: false`. Verified on the dev DB: 10 memberships (3/3/4), 10 `reviewed_cycle` tag rows, 20 audit rows, no growth on re-run. API `/v2/catalogue/collections/tyagaraja-lalgudi-pancharatnam` shows 4 members, `orderBasis UNSPECIFIED`, `isComplete false`; UI Tags tab on *ISa pAhi* shows the tag. Clean-database proof was run on scratch databases (an empty one took all 81 migrations; a restored copy of the dev data took the seed) rather than `make db-reset`, because that would have dropped the dev catalogue that the re-index needs.
- **2026-11-11**: Re-index done. One paid re-index on 2026-11-11 (Seshadri authorised it with a $5 cap), run once per composer with `batch_embed_catalogue.py` and the D1 claim-metadata formatter. Syama Sastri: 70 krithis, 142 embeddings created, 1,908 unchanged and skipped, 0 failed (~$0.004). Tyagaraja: 683 of 684 krithis, 9,797 embeddings created, 8,403 skipped, **1 failed** (*kAla haraNa*, a read timeout; failure file `reports/embedding_failures_20261111_112217.json`), ~1.96M tokens, ~$0.29. Total about $0.30 against the $5 cap. The one failed krithi was not retried because a second paid run was outside the authorisation; it was not re-embedded this run, so it may still carry the older header text. The run did not activate a new profile (activation is opt-in and was not requested). Before the run, header samples showed only `[Mudra]`, `[Laya]` and `[Theme]` clauses, at most 24 words against the 60-word budget. `[Raga Trait]`, `[Bhava]`, `[Ritual Function]` and `[Dramatic Context]` cannot appear yet because those claims are still `deferred`. Before/after search on 8 queries (hybrid and semantic, 16 result sets): 6 unchanged, 10 changed in the top 5. Evidence: `application_documentation/10-implementations/track148-149-reindex-search-evidence.md`. Backups taken earlier: `storage/backups/track149-pre-reextract-20261111-0914.dump` and `track149-pre-seed13-20261111-0939.dump` (gitignored). The track stays In Progress with the owner decisions above still open.
- **2026-11-11**: Correction on *duDuku gala*. Using the public page the owner supplied (`shivkumar.org/music/dudukugala-new.htm`), the `svara-sAhitya 1..10` blocks in our source are the charanams (that page numbers them `Charanams 1..9`). The earlier exception ("no charanam by form") is withdrawn; the record is a real charanam gap awaiting an owner go-ahead for a parser change and an audited reingest. The `eevasudha.htm` page the owner supplied shows *Ivasudha Neevanti* is the Kovur kriti *I vasudha nIvaNTi* (Sahana), so that title looks like a spelling variant rather than corruption; awaiting the owner's confirmation.
