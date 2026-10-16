| Metadata | Value |
|:---|:---|
| **Status** | Proposed |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-10-15 |
| **Author** | Sangeetha Grantha Team |

# Track: Tyagaraja Edition Consultation & Collision Canon Split

**ID:** TRACK-149
**Status:** Proposed — Intent Draft
**Owner:** Seshadri
**Created:** 2026-10-15
**Updated:** 2026-10-15

## Goal

Consult the institutional print editions and Music Academy Journal volumes registered as `unavailable` in TRACK-147, adjudicate the edition-dependent Kshetra Pancharatnam, Utsava Sampradaya, Divyanama, and dramatic-work leads, split the 8 raga/tala collision identities into 17 distinct catalogue compositions through the audited canon/import pipeline, and apply the non-cycle approvals deferred by TRACK-147.

## Context

- **Predecessor:** [TRACK-147](TRACK-147-tyagaraja-musicological-classification.md), completed 2026-10-15 under its Spec/Plan amendment. TRACK-147 owns the Tyagaraja classification master, `R__seed_11`, and the Ghana-raga Pancharatnam cycle integration; this track extends them without forking the schema or shared contract.
- **Companion:** [TRACK-148](TRACK-148-syama-sastri-edition-consultation-adjudication.md) (Syama Sastri edition consultation and adjudication).
- **Contract:** [Musicological classification contract](../../application_documentation/01-requirements/features/musicological-classification-contract.md).
- **Master and manifest:** `database/data/track147-tyagaraja-master.json` and `database/data/track147-tyagaraja-application-manifest.json`. The concordance is generated as [tyagaraja-classification-master.md](../../application_documentation/01-requirements/features/tyagaraja-classification-master.md).

## Intent

**Status:** Draft
**Accepted by:** —
**Accepted at:** —

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

**Status:** Draft
**Accepted by:** —
**Accepted at:** —

### Requirements

To be written with `/spec-from-track` after Intent acceptance.

## Plan

**Status:** Draft
**Accepted by:** —
**Accepted at:** —

### Files that change

To be written with `/plan-from-spec` after Spec acceptance.

## Implementation Checklist

- [ ] Intent accepted.

## Progress Log

- **2026-10-15**: Track drafted from the TRACK-147 completion scope to own print-edition consultation, the 8-identity / 17-witness collision split, Kshetra/Utsava/Dramatic cycle promotion, and deferred non-cycle claim application. Intent is Draft and awaits acceptance.
