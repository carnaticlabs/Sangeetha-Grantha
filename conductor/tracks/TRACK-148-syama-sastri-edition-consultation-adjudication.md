| Metadata | Value |
|:---|:---|
| **Status** | Proposed |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-10-15 |
| **Author** | Sangeetha Grantha Team |

# Track: Syama Sastri Edition Consultation & Musicological Adjudication

**ID:** TRACK-148
**Status:** Proposed — Intent Draft
**Owner:** Seshadri
**Created:** 2026-10-15
**Updated:** 2026-10-15

## Goal

Consult the institutional print editions that TRACK-146 registered but did not inspect. Use them to promote, correct or reject the Syama Sastri claims that are still unreviewed leads, and apply the approvals that TRACK-146 deferred.

## Context

- **Predecessor:** [TRACK-146](TRACK-146-syama-sastri-musicological-classification.md), completed 2026-10-15 under its closure amendment. TRACK-146 owns the shared classification contract, master model, generator and `R__seed_10`; this track extends them and does not fork them.
- **Contract:** [Musicological classification contract](../../application_documentation/01-requirements/features/musicological-classification-contract.md).
- **Master and manifest:** `database/data/track146-syama-sastri-master.json` and `database/data/track146-syama-sastri-application-manifest.json`. The concordance is generated as [syama-sastri-classification-master.md](../../application_documentation/01-requirements/features/syama-sastri-classification-master.md).
- **Companion:** [TRACK-147](TRACK-147-tyagaraja-musicological-classification.md) (Tyagaraja) consumes the same contract.

## Intent

**Status:** Draft
**Accepted by:** —
**Accepted at:** —

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

- **2026-10-15**: Track drafted from the TRACK-146 closure amendment. Intent is Draft and awaits acceptance. No implementation started.
