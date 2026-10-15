| Metadata | Value |
|:---|:---|
| **Status** | In Progress |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-10-01 |
| **Author** | Sangeetha Grantha Team |

# Track: Tyagaraja Corpus Concordance & Musicological Classification

**ID:** TRACK-147  
**Status:** In Progress — Classification
**Owner:** Seshadri  
**Created:** 2026-09-30  
**Updated:** 2026-10-01

---

## Goal

Produce an evidence-backed classification master for the reconciled Tyagaraja catalogue, comparable to the Dikshitar master in coverage and accountability, organised around devotional themes, ritual functions, formal groups, dramatic works, supported sacred geography and transmission witnesses.

## Context

- **User request:** Start the Tyagaraja companion to the Syama Sastri classification initiative using the combined musicological and repository analysis.
- **Predecessor:** [TRACK-145](TRACK-145-dikshitar-kshetra-musicological-metadata.md) and its [feature document](../../application_documentation/01-requirements/features/dikshitar-kshetra-musicological-enrichment.md).
- **Shared contract owner:** [TRACK-146](TRACK-146-syama-sastri-musicological-classification.md) owns the initial identity/evidence/taxonomy conventions. Tyagaraja source reconciliation and research can proceed independently after the applicable gates; shared product changes must not duplicate ownership.
- **Historical import baseline:** [TRACK-093](TRACK-093-trinity-krithi-bulk-import.md) reports 675 imported Tyagaraja compositions against a target of 692. The supplied analysis reports 687 CSV rows and 212 distinct source raga labels. CSV counts/labels require verification, and none of these figures is a fresh database census.
- **Count reconciliation:** Target minus imports is 17; reported CSV minus imports is 12; target minus CSV is 5. These differences do not identify 17 missing compositions. Source candidates must be matched to catalogue identities, aliases, exclusions and import outcomes.
- **Existing grouping:** [R__seed_07](../../database/migrations/R__seed_07_canonical_cycle_tags.sql) seeds Ghana-raga Pancharatnam through title heuristics. Reviewed identities and provenance-aware legacy reconciliation are needed.
- **Shared infrastructure:** [TRACK-029](TRACK-029-bulk-import-kshetra-mapping.md), [V66](../../database/migrations/V66__dikshitar_musicological_enrichment.sql), [V67](../../database/migrations/V67__search_document_anchor_constraints.sql), the [database schema](../../application_documentation/04-database/schema.md), and [domain model](../../application_documentation/01-requirements/domain-model.md).
- **Dikshitar comparison artifact:** `/Users/seshadri/.gemini/antigravity/brain/9ed7e87b-352e-49d0-91cd-b0db50c22807/dikshitar-corpus-481-classification-master.md`. Treat it as research input, not a portable runtime dependency or template for compulsory shrine assignment.

## Intent

**Status:** Accepted
**Accepted by:** Seshadri
**Accepted at:** 2026-09-30

### Problem

We want a complete Tyagaraja concordance with the same accountability as the Dikshitar master, while reflecting Tyagaraja's devotional arguments, musical reflection, ritual repertoire and dramatic works. Flat Rama or Pancharatnam tags do not distinguish these meanings and functions, and temple-centred classification would force unsupported geography onto many compositions.

The source inventory and recorded import total disagree. Current evidence and metadata coverage are unknown for this initiative. A title or raga label may identify an alias or a transmitted setting rather than a distinct composition. A single modern edition cannot silently collapse textual, melodic, rhythmic or structural differences across witnesses.

### Proposed outcome

1. **Reconciled corpus snapshot.** Map source rows and track targets to stable catalogue identities. Classify differences as aliases, duplicates, omissions, failed/pending imports, exclusions or unresolved records. Begin research on a versioned snapshot, but account for every candidate before claiming final completeness.
2. **Full master and source concordance.** Maintain structured classification data and generate readable Markdown, source coverage and distributions. Report distinct compositions, membership rows and musical/textual witnesses separately. Preserve unresolved records instead of filling gaps with assumptions.
3. **Multiple evidenced themes.** Classify candidate themes such as Rama bhakti, nama mahima, nadopasana, musical knowledge, surrender, instruction to the mind, ethical reflection, saints/teachers and narrative allusion. Cite passages and translations; distinguish literal topic from interpretive bhava. Devotional complaint must not automatically become nindastuti.
4. **Formal and kshetra group review.** Establish reviewed Ghana-raga Pancharatnam identities and investigate Kovur, Tiruvottiyur, Lalgudi and Srirangam Pancharatna memberships and shrine associations. Distinguish conventional presentation order from source-attested sequence. Standalone compositions need no artificial group membership.
5. **Ritual and congregational collections.** Investigate Utsava Sampradaya and Divyanama membership by edition/tradition, allowing overlap. Record evidenced ritual function such as awakening, invitation, procession, marriage celebration, lullaby or concluding benediction. Do not impose a universal ritual order where sources differ.
6. **Dramatic-work annex.** Review *Nauka Charitram* and *Prahlada Bhakti Vijayam* as works with constituent items, source-defined divisions, item order, speakers, addressees and narrative events. Preserve musical songs, verse and prose context where available. Dramatic speech must not automatically be indexed as the composer's autobiographical statement. No invented reconstruction of lost works is included.
7. **Transmission annex.** Distinguish composition identity, textual witness, musical setting, school and modern performance version. Preserve source-specific raga/tala, charanam order and text readings; sangati observations require notation or documented rendering. Inventory existing source/canon facilities before selecting new structures.
8. **Qualified deity and geography.** Identify the actual addressee/deity/avatar rather than assigning Rama universally. Separate shrine evidence, broad devotional association, pilgrimage tradition, place of performance and place of composition. Use the shared geography-status convention; no dummy temple or unsupported no-kshetra Boolean.
9. **Reviewed application and retrieval.** After Plan acceptance, apply approved classifications and memberships through Flyway, protect independently curated links, and evaluate structured filtering plus semantic retrieval. Extend TRACK-146's shared contract for ritual, drama and transmission. Coordinate shared formatter/macro generation with TRACK-145 and distinguish direct database evaluation from live search API behaviour.

### Affected users and systems

- Musicologists and curators: source concordance, thematic interpretation, disputed attribution/membership and version comparison.
- Rasikas: discovery by devotional meaning, ritual function, dramatic context and supported kshetra association.
- Import and evidence pipeline: source-specific blog extraction, catalogue reconciliation and scholarly cross-checking.
- Database/canon: identities, source claims, tags, memberships, witnesses, musical settings and work relationships.
- Semantic search: discriminative composition/section headers and group/work/kshetra overview discovery. API or consumer UI changes require explicit Spec scope.

### Constraints

- Follow [CLAUDE.md](../../CLAUDE.md) and the [Conductor workflow](../../.agents/skills/conductor-track-manager/SKILL.md): no product implementation, imports or corpus mutation before accepted Plan.
- Flyway only; never edit committed versioned migrations. Allocate schema/seed numbers at planning time. Repeatable seeds must be idempotent and preserve assignments outside their explicit provenance ownership.
- Preserve runtime database-query, DTO and mutation-audit contracts; resolve seed/migration audit treatment in Spec.
- Reuse the shared evidence/taxonomy contract from TRACK-146. Extend existing `BHAVA`, `PHILOSOPHY` and other categories only where the accepted design establishes a gap; formal set, thematic collection, ritual collection and dramatic work are different group types.
- Raga identity reconciliation must use canonical IDs/aliases rather than treating every CSV label as a distinct raga. Preserve ordered ragas and musical-form structure according to the domain model.
- Claim-level evidence and review status are required; inferred classifications must not silently become canonical. Human adjudication is needed for disputed claims before seeding, with unresolved records allowed in the master.
- No forced temple, formal group, deity or single theme assignment. Non-specific address, unresolved association and unreviewed evidence are distinct states.
- Completion is relative to a reconciled dated snapshot, not a claim to recover the whole historical corpus.
- No new paid embedding runs, production deployment, destructive resets or commits are authorised by creation of this track.

### Open questions

1. Which source inventory explains the 675/687/692 discrepancy, and which candidates actually require import remediation?
2. Which editions and manuscript/notation witnesses are accessible for the corpus and the two dramatic works?
3. How will source-dependent collection membership and ordering be represented without conflating performance convention with original sequence?
4. Which witness differences require separate musical settings, and which can be represented by existing canon/variant facilities?
5. What minimum evidence and human adjudication are required for thematic interpretation, disputed attribution and kshetra claims?
6. Can dramatic verse/prose and work overviews fit existing document anchors, or does the accepted Spec require a new work anchor? Do not assume cycle anchoring is sufficient.
7. Which shared formatter/search work remains with TRACK-145/146, and what live API behaviour must this track implement or explicitly defer?
8. What retrieval targets will be accepted for exact groups, thematic search, ritual function, source-qualified variants and uncertainty handling?

### Intended deliverables and completion evidence

- Corpus reconciliation, field/junction/evidence coverage audit and import-disposition register.
- Structured Tyagaraja master, generated Markdown concordance, source bibliography and unresolved-claims register.
- Formal-group/kshetra membership tables; ritual-function, dramatic-work and transmission annexes.
- Extensions to TRACK-146's shared contract with one owner per shared change.
- Reviewed change manifest, provenance-aware legacy-tag reconciliation and safe repeatable application strategy.
- Reviewed retrieval probes for music-as-spiritual-practice themes, awakening functions, dramatic membership/speaker context, supported shrine associations and source-qualified variants. Include a cross-composer probe separating Dikshitar's Tyagaraja deity/vibhakti cycle from composer Tyagaraja.
- After accepted implementation: [data-quality audit](../../.agents/skills/data-quality-audit/SKILL.md), [verify-import](../../.claude/skills/verify-import/SKILL.md), protected reruns, affected-layer checks and retrieval results. Exact commands and thresholds belong in Plan.

### Research starting points

- Source blog: `thyagaraja-vaibhavam.blogspot.com`; scholarly cross-checking is required before canonical assignment.
- [Music Academy Journal 1947](https://musicacademymadras.in/catalogue/files/journals/Vol.18_1947.pdf) and [1968](https://musicacademymadras.in/catalogue/files/journals/Vol.39_1968.pdf): Walajapet collection and transmission-related research starting points; manuscript discussion is not evidence for every catalogue row.
- [Music Academy Journal 2017](https://musicacademymadras.in/catalogue/files/journals/Music%20Academy%20Journal%202017.pdf): dramatic-work discussion, including daru, padya and vacana context.
- [Compiled Sanskrit compositions](https://sanskritdocuments.org/doc_deities_misc/tyAgarAjakRRitayaH.pdf): initial group inventory and textual leads; verify membership against editions rather than treating an introductory list as exhaustive proof.
- Candidate editions for access/review include Govinda Rao's *Compositions of Tyagaraja*, *The Spiritual Heritage of Tyagaraja*, and editions of the dramatic works. Exact edition identifiers and locators remain to be established.

## Spec

**Status:** Accepted
**Accepted by:** Seshadri
**Accepted at:** 2026-09-30

This spec records what must be true for every Tyagaraja candidate in the 2026-09-30 catalogue snapshot. It does not apply classifications, split identities, or change schema. Product implementation waits for an accepted Plan.

A Carnatic musicologist reviewed this design before it was written. Findings below that were re-checked against `sangita_grantha` are census facts. Conventional kshetra lists remain unreviewed claims.

### Census (2026-09-30, `sangita_grantha`)

Denominator for this snapshot:

| Unit | Count | Meaning |
|:---|---:|:---|
| Catalogue identities | 675 | `krithis` with composer Tyagaraja (`b1bdccd8-8cb1-4a14-a9e6-7ffc843cd7b6`). 674 `published`, 1 `draft` (`Urakae Kalgunaa`). |
| Source rows on those identities | 684 | All `source_key` hosts are `thyagaraja-vaibhavam.blogspot.com`. Status: 652 approved, 31 mapped, 1 `in_review`. |
| Identities with no import row | 0 | |
| Unmapped Tyagaraja import rows | 0 | |
| External figures not in this database | 687 and 692 | Post-mortem unique CSV count and TRACK-093 target. They are an unresolved external inventory, not 17 missing compositions. |

Coverage on the 675 identities:

- Primary raga and tala are populated. Each identity has exactly one `krithi_ragas` row. `is_ragamalika` is false on all 675.
- 211 distinct `raw_raga` labels resolve to 209 canonical ragas. `Kedaram` and `kEdAra gauLa` both exist. `bRndAvana sAranga` exists separately from names containing Sāranga. `kEsari - sindhu kannaDa` exists beside `Kesari` and `Sindhu Kannada`.
- Deity, temple, vibhakti, and occasion note are empty on all 675. `krithi_cycle_memberships` has no Tyagaraja rows.
- The only tag is `tyagaraja-pancharatnam` (`STOTRA_STYLE`, source `canonical_cycle`) on the five Ghana-raga titles below, applied by the title heuristic in `R__seed_07`.
- `musical_form` is `KRITHI` for all 675. Section shape: 27 identities have no `PALLAVI`, 123 have no `ANUPALLAVI`, 4 have no `CHARANAM`, 347 have more than one `CHARANAM`, and 7 `SWARA_SAHITYA` rows exist in the corpus.
- `primary_language` is `en` for 674 and `te` for 1. Lyric-variant rows also exist in Kannada, Tamil, Malayalam, Telugu, and Sanskrit for most identities. The `en` value is a storage fact.
- `krithi_source_evidence` has 1,020 Tyagaraja rows. Contributed fields are `{title,composer,raga,tala}` (684) and `{tala}` (336). That table is extraction provenance, not a musicological claim store.
- Canonical tala counts: Adi 441, Rupaka 103, Misra Capu 55, Capu 30, Jhampa 18, Triputa 16, Khanda Capu 9, Tisra Ekam 2, Catusra Ekam 1. Eleven import rows whose raw tala is `desadi` are stored as Adi. No raw `madhyadi` label is present.

Ghana-raga Pancharatnam identities currently tagged, all Adi. The musicologist review treats this as the standard set. Nāṭṭai–Gowla–Ārabhi–Varāli–Sri order is an Aradhana performance convention, not a composer sequence.

| Title | Raga |
|:---|:---|
| jagadAnanda kAraka | Nāṭṭai |
| duDuku gala | Gowla |
| sAdhincenE | Ārabhi |
| kana kana rucirA | Varāli |
| Endaro Mahaanubhaavulu | Sri |

Eight identities absorb 17 source rows whose raw raga or raw tala disagree. These are unresolved collisions, not confirmed duplicates:

| Catalogue title | Conflicting source raga / tala |
|:---|:---|
| jaya mangaLaM | Mohanam / Unknown; Nada namakriya / Adi; Ghanta / Jhampa |
| pAhi rAma candra | Sankarabharanam / Unknown (`in_review`); Yadukula Kambhoji / Unknown (approved) |
| paripUrNa kAma | Hamsa bhramari / Adi; Puri Kalyani / Adi |
| Rama Rama Rama Sita | Huseni / Unknown; Saveri / Adi |
| rAma sItA rAma | Balahamsa / Unknown; Sankarabharanam / Unknown |
| Sri Narada Muni | Bhairavi / Unknown; Pantuvarali / Rupakam |
| SrI nArasiMha | Bilahari / Misra Capu; Phalaranjani / Unknown |
| Sri Rama Jaya Rama | Madhyamavati / Unknown; Varali / Misra Capu |

Catalogue titles that match conventional kshetra-pancharatna incipits are leads only. The review moves `sundari nI divya` from a Kovur guess to Tiruvottiyur, and `ISa pAhi` from a Tiruvottiyur guess to Lalgudi. Both moves stay unreviewed until a named edition is cited. `sundarESvaruni` (Sankarabharanam, Adi) and `dArini telusukoNTi` (Suddha Saveri, Adi) are already identities; they were absent from the first candidate list, not from the catalogue. No title matching *Vinarādhā nā manavi* was found. `nannu kanna talli` (Kesari–Sindhu Kannada) is a different identity from `kanna talli` (Saveri). `Chootaamu Raareyee Vedkanu` (Kapi) is a different identity from `cUtAmu rArE` (Arabhi). `karuNa jUDavayya` is stored in Sāranga; the review treats Sāranga as the usual raga for the Srirangam song and warns against confusing it with Brindavana Saranga.

### Requirements

1. **Account for every snapshot unit.** The master must list all 675 identities and all 684 source rows, plus an external-gap register for the 687 and 692 figures. A count difference is not a missing composition. Completion is this dated snapshot, not the historical corpus.
2. **Split or explicitly justify the eight collisions before classification.** No theme, deity, group, or ritual label may be attached to a collapsed identity whose source rows disagree on raga or tala. `jaya mangaLaM` is three source settings, not one mangalam.
3. **One review state per claim.** Each classification carries a source locator, evidence type (textual passage, notation, scholarly interpretation, documented tradition, or automated suggestion), and a status of unreviewed, supported, disputed, or unresolved. Inferred labels do not become canonical. Human adjudication is required before a disputed claim is seeded.
4. **Multiple themes, none by default.** Candidate axes are Rama bhakti, nama mahima, nadopasana, musical knowledge, surrender, instruction to the mind, ethical reflection, saints and teachers, and narrative allusion. A composition may have several. Literal topic and interpretive bhava stay separate. A devotional complaint is not nindastuti unless the passage has that lexical turn. Theme is not inferred from raga. A song that names the seven swaras does not alter raga or tala reference data.
5. **Deity is the addressee of that composition.** Rama is not the default. Null deity remains `UNREVIEWED`. Kovur addresses Siva; Tiruvottiyur and Lalgudi addresses include the goddess; other addressees include Ganesha, Krishna, Hanuman, and Tulasi. Group membership does not copy a deity onto every member.
6. **Geography uses the shared states from TRACK-146.** Proposed states remain `UNREVIEWED`, `SPECIFIC_SHRINE_SUPPORTED`, `NON_SPECIFIC_ADDRESS`, `UNRESOLVED`, and `DISPUTED`. An empty `temple_id` is `UNREVIEWED`, not a no-kshetra flag. Shrine evidence, broad devotion, pilgrimage tradition, place of performance, and place of composition stay distinct. No dummy temple.
7. **Group types stay distinct.** Formal set, thematic collection, ritual collection, and dramatic work are different. Standalone compositions have no artificial membership.
   - Ghana-raga Pancharatnam: the five identities above are the review candidate. Store performance order and any source order separately. The grouping is later tradition, not a declaration by Tyagaraja. Membership belongs in reviewed `krithi_cycle_memberships`. The existing `STOTRA_STYLE` tag is legacy heuristic provenance.
   - Kovur, Tiruvottiyur, Lalgudi, and Srirangam pancharatnas: membership and sequence require a named edition. The title leads above are not seedable as written.
   - Utsava Sampradaya and Divyanama: overlap is allowed. Order and membership are per edition or bhajana paddhati. There is no universal ritual order. Ritual function (awakening, invitation, procession, marriage, lullaby, mangalam) is a claim of its own. A shared title prefix is not membership: `Nagu Momu Kana Leni` (Abheri) is not the ritual piece `Nagu Momu Gala Vaani`; the three Tulasi titles are three identities.
8. **Dramatic works are an annex.** *Nauka Charitram* and *Prahlada Bhakti Vijayam* are works with edition-qualified items. Item type (`DARU`, `KIRTANA`, `PADYA`, `VACANA`, `DVIPADA`, or other), speaker, addressee, and division are properties of the membership, not a replacement for `musical_form`. Sung items that already have krithi structure may remain `KRITHI` as the current storage value. Padya and vacana must not be invented as new `KRITHI` rows. `UNESTABLISHED` is the domain-model value for an unknown form; it is not a fourth concert form. Dramatic speech is not the composer's autobiography. Lost works are not reconstructed. `ODanu jaripE` (Saranga) is a lead only.
9. **Transmission stays layered.** Composition identity, textual witness, musical setting, school, and modern performance version are separate. The 684 source rows are witnesses. Sangati observations require notation or a documented rendering. Ragamalika stays false unless a source shows a raga change. Raw raga and raw tala labels are retained beside canonical IDs, including the eleven `desadi` rows now stored as Adi.
10. **Structure audit does not invent text.** The 27 missing pallavi rows and 4 missing charanam rows are defects to investigate. A missing anupallavi is allowed, and is expected for many Divyanama and Utsava items. Swara passages, including those in the Ghana-raga five, stay in notation variants rather than being copied into lyric text. The composer mudra supports attribution review; its absence does not exclude a song.
11. **Shared contract.** TRACK-146 owns identity, evidence vocabulary, group types, geography states, review rules, and distinct-versus-member counting. This track extends that contract for ritual function, dramatic item type, and transmission witnesses. One owner per shared change. Formatter and macro-document generation stay with TRACK-145 unless the accepted Plan assigns a gap to this track. Dikshitar vibhakti, bhuta, and posture columns are not copied onto Tyagaraja.
12. **Application is later and provenance-safe.** After Plan acceptance, approved claims go through a new Flyway migration or idempotent repeatable seed. `R__seed_07` must not be edited into a second writer of Tyagaraja memberships. Legacy `canonical_cycle` tag links are reconciled only for the provenance this track owns. `primary_language = en` is not silently rewritten.

### Design

Lives in the existing catalogue, not a parallel store.

- **Identities** remain `krithis`. Splitting a false merge creates or remaps identities through the import/curation path named in the Plan, with an audit record. This spec does not choose the split mechanism.
- **Witnesses** remain `imported_krithis` plus `krithi_source_evidence`. A musicological claim is not an extra value in `contributed_fields`.
- **Claims** need a structure that can store locator, evidence type, review status, and the classified value, and can remain unresolved. Reuse is preferred. If no current table can hold a disputed theme or an edition-qualified ritual membership without overloading `krithi_tags.confidence`, the Plan adds one table in a new versioned migration after V67. Do not edit V66 or V67.
- **Groups** use `tags` plus `krithi_cycle_memberships`. Tag category stays inside the existing check (`BHAVA`, `FESTIVAL`, `PHILOSOPHY`, `KSHETRA`, `STOTRA_STYLE`, `NAYIKA_BHAVA`, `OTHER`). New group kinds are not new tag categories unless the accepted design shows a gap. `cycle_member_role_enum` (`CORE`, `DHYANA`, `MANGALAM`, `OPTIONAL`, `DISPUTED_CONJECTURE`) is not the dramatic item-type axis; item type, speaker, and edition order belong in membership attributes or a dedicated column specified in the Plan.
- **Geography and deity** use `deities`, `temples`, and the TRACK-146 state vocabulary. No Tyagaraja-specific temple Boolean.
- **Readable master** is generated from the structured claims: one section per identity, source-row concordance, collision register, unresolved register, and distributions of distinct compositions versus membership rows versus witnesses. The Dikshitar markdown artifact is a format comparison only.
- **Retrieval**, if the Plan keeps it in scope, probes nadopasana, an awakening function, a dramatic speaker, a supported shrine, a source-qualified variant, and a cross-composer case that separates Dikshitar's Tyagaraja-deity cycle from composer Tyagaraja. Macro-document presence is not live API behaviour.
- **Out of scope for this spec:** UI changes, paid embedding runs, production deployment, `db-reset`, and commits.

### Flagged concerns

- The eight multi-URL identities are false merges on present evidence. Classifying them as single krithis would publish the wrong raga.
- `R__seed_07` deletes and reinserts the Syama Sastri ratnatrayam tag and assigns Tyagaraja Pancharatnam by title pattern. A later seed must not inherit that delete-all behaviour for reviewed Tyagaraja links.
- `krithi_source_evidence` cannot carry theme, ritual, or dramatic claims without changing its meaning.
- Putting `DARU` or `PADYA` into `musical_form_enum` or `cycle_member_role_enum` would break the domain-model form contract. `UNESTABLISHED` already exists for unknown form.
- Eleven `desadi` witnesses stored as Adi lose eduppu information if the raw label is dropped.
- `kEsari - sindhu kannaDa` may be a garbled source string. It must not be silently merged with `Sindhu Kannada` or `Kesari`.
- Empty deity and temple on all 675 will look like a completed "no kshetra" corpus if a consumer treats null as a decision.
- TRACK-146's shared contract is still at Intent review. This spec depends on it and must not implement a second copy.
- Corpus-row DML does not belong in a versioned `V__` migration (TRACK-139) unless the file carries the explicit allow marker. The Plan chooses seed versus curation.

### Open questions carried forward

1. Which external files are the 687-row and 692-target inventories, and which of their candidates are aliases of the 675? *(2026-10-01: the 687 rows are `database/for_import/Thyagaraja-Krithi-For-Import.csv`, now hashed in the master. Only the 692 target remains external.)*
2. Which named edition will adjudicate kshetra pancharatna membership, including `ISa pAhi`, `sundari nI divya`, `karuNa jUDavayya`, and the missing *Vinarādhā nā manavi* lead?
3. For each of the eight collisions, is the right outcome two catalogue identities, two musical settings of one identity, or a duplicate witness?
4. Which current table, if any, can store claim-level review status without a new migration?
5. Who adjudicates thematic interpretation before a claim is seedable?
6. Do dramatic padya and vacana need document anchors of their own, or do they remain non-indexed work context?
7. Which formatter and live-search gaps stay with TRACK-145, and which retrieval probes are in this track's Plan?
8. What minimum evidence distinguishes `SPECIFIC_SHRINE_SUPPORTED` from `NON_SPECIFIC_ADDRESS` for a Tyagaraja composition?

## Plan

**Status:** Accepted
**Accepted by:** Seshadri
**Accepted at:** 2026-10-01

Accepting this plan authorises the classification master and local reversible work. It does not approve any musical claim, seed, commit, paid embedding run, or `make db-reset`.

### Files that change

TRACK-146 owns `musicological-classification-contract.md`, `musicological_classification.py`, and the shared generator. This track does not edit those files or TRACK-146's corpus artifacts. Tyagaraja extensions (ritual function, dramatic item, transmission witness, collision subject) are fields on the Tyagaraja master. If the shared module is already on the branch when implementation starts, the Tyagaraja checker imports it. If it is absent because TRACK-146's plan is still unaccepted, the checker validates the same claim shape from TRACK-146's accepted spec and is folded into the shared module later. A second contract document is not created.

| Path | Role |
|:---|:---|
| `database/data/track147-tyagaraja-master.json` | Every classification subject, census facts, claims, and the external-gap register |
| `database/data/track147-tyagaraja-application-manifest.json` | Approved canonical changes only. Stays empty until a claim has an explicit human approval |
| `application_documentation/01-requirements/features/tyagaraja-classification-master.md` | Generated concordance, collision register, unresolved register, and distributions |
| `tools/krithi-extract-enrich-worker/src/tyagaraja_classification.py` | Tyagaraja validation: subject grain, collision ban, extension axes |
| `tools/krithi-extract-enrich-worker/scripts/generate_tyagaraja_master.py` | Validate the master and render the markdown. No network and no database writes |
| `tools/krithi-extract-enrich-worker/tests/test_tyagaraja_classification.py` | Accounting, collision rejection, unapproved-claim rejection, stable IDs |
| `tools/krithi-extract-enrich-worker/tests/test_tyagaraja_source_inventory.py` | CSV hash, every CSV row mapped once, curated non-subject dispositions |
| `database/migrations/R__seed_07_canonical_cycle_tags.sql` | Later, and only the Tyagaraja title-heuristic `INSERT` into `krithi_tags`. The tag dictionary row and the Syama Sastri block stay with TRACK-146 |
| `database/migrations/R__seed_11_tyagaraja_reviewed_memberships.sql` | Created only when the manifest has approved identities. Recheck the next free `R__seed` number first |
| `conductor/tracks/TRACK-147-tyagaraja-musicological-classification.md`, `conductor/tracks.md` | Acceptance and progress |

No new versioned migration in the first pass. Claims stay in the master and point at existing `imported_krithis` and `krithi_source_evidence` ids. `krithi_source_evidence.contributed_fields` is not a claim store. Dikshitar vibhakti, bhuta, and posture columns are not copied. Geography states stay master vocabulary (`UNREVIEWED`, `SPECIFIC_SHRINE_SUPPORTED`, `NON_SPECIFIC_ADDRESS`, `UNRESOLVED`, `DISPUTED`), not a new PostgreSQL enum.

### Order of work

1. **Skeleton for every snapshot unit.** Read the live database. Write one master record per classification subject, with stable ids that do not depend on file order. Census facts already measured are filled: catalogue id, title, canonical raga and tala, raw raga and raw tala, source URL, import status, section flags, and `primary_language` left as stored. Every theme, deity, geography, group, ritual, and dramatic claim starts `unreviewed`. Nothing is seedable.
2. **Collision grain before any label.** The eight collapsed identities are register entries with disposition `collision_not_classified`. Their 17 source rows are the classification subjects. No theme, deity, group, or ritual claim may attach to the collapsed `krithi_id`. `jaya mangaLaM` is three subjects (Mohanam, Nada namakriya, Ghanta). The other seven pairs stay split the same way. Splitting catalogue rows in the database waits for a curator decision recorded on the manifest.
3. **Per-krithi classification, in this order.** Work every remaining subject. A claim needs a locator and an evidence kind (`textual_evidence`, `scholarly_interpretation`, `documented_tradition`, `automated_suggestion`). Review states are `unreviewed`, `reviewed`, `unresolved`, `disputed`, `rejected`. Only `reviewed` plus an explicit human approval may enter the manifest.
   - Formal sets: Ghana-raga Pancharatnam as a candidate cycle, with Aradhana order stored as performance convention. Kovur, Tiruvottiyur, Lalgudi, and Srirangam leads stay `unreviewed` until a named edition is cited. `sundari nI divya` and `ISa pAhi` are not assigned from the earlier guess.
   - Ritual: Utsava Sampradaya and Divyanama membership and function, edition-qualified, overlap allowed. Title-prefix matches such as the two Nagu Momu songs and the three Tulasi songs stay separate.
   - Dramatic annex: *Nauka Charitram* and *Prahlada Bhakti Vijayam* item type, speaker, addressee, and division on the membership. `ODanu jaripE` stays a lead. No new `KRITHI` rows for padya or vacana. No lost-work reconstruction.
   - Themes and addressee: passage-cited, multi-valued, no default Rama, no raga-to-bhava inference. Complaint is not nindastuti. Empty deity and temple stay `UNREVIEWED`.
   - Transmission notes: retain raw labels, including the eleven `desadi` rows stored as Adi. Do not merge `kEsari - sindhu kannaDa` into `Sindhu Kannada` or `Kesari`.
   - Structure findings: 27 missing pallavi rows and 4 missing charanam rows are audit entries. Missing anupallavi is allowed. Do not invent lyric text.
4. **Generate the concordance** from the master: 675 identities, 684 source rows, 667 single-source subjects, 17 collision subjects, 8 unclassified collapsed identities, and the 687/692 external gap with no invented titles.
5. **Database application, only for approved manifest rows.** Remove the Tyagaraja title-heuristic tag insert from `R__seed_07` without touching the Syama Sastri delete/insert. Add `R__seed_11` with exact UUIDs, composer and raga guards, and provenance limited to this track. Ghana-raga membership goes to `krithi_cycle_memberships`. The legacy `STOTRA_STYLE` tag may remain as a mirror only when the manifest says so. Identity splits go through the existing audited import/canon path, not a `V__` corpus update. If that path cannot preserve the witness, stop and amend this plan.

### Risks

- **Highest risk:** labelling a collapsed identity. The checker must reject any claim whose subject is one of the eight `krithi_id`s.
- **Shared-file clash:** TRACK-146 also plans to edit `R__seed_07`. This track touches only the Tyagaraja `krithi_tags` insert. If that hunk conflicts, keep both composers' protections and do not restore the title heuristic.
- **False completeness:** an `unreviewed` row is required for every subject. A missing row is a failure. A filled Rama deity with no passage is a failure.
- **Destructive rerun:** `R__seed_11` must not delete tags or memberships it does not own. The Syama Sastri delete-all pattern in `R__seed_07` is rejected as a template.
- **Search:** retrieval probes stay out of this pass. TRACK-145 remains the formatter owner. No embedding run.

### Options rejected

- Classifying all 675 catalogue rows as the subject grain, which would publish one raga for `jaya mangaLaM`.
- Waiting to create a row until a theme is known. The skeleton is the start of per-krithi classification; unknown axes stay `unreviewed`.
- A new claims table, geography enum, or `musical_form` value for daru and padya in this pass.
- Editing `V66` or `V67`, or writing corpus repairs in a versioned migration.
- Using `krithi_source_evidence` or `krithi_tags.confidence` as the review state.

### Proof

- From `tools/krithi-extract-enrich-worker`: `uv run pytest tests/test_tyagaraja_classification.py` and `uv run python scripts/generate_tyagaraja_master.py --master ../../database/data/track147-tyagaraja-master.json --output ../../application_documentation/01-requirements/features/tyagaraja-classification-master.md --check`. The test must fail if the subject count is not 684, if any of the eight collapsed ids carries a theme or group claim, if a seedable claim lacks human approval, or if regenerating the markdown differs.
- `uv run ruff check` and `uv run mypy` on the new Python files.
- `make check-docs` once the generated feature document is tracked. Do not weaken the validator.
- After a manifest is non-empty and seeds change: `make test`, `make test-integration`, then `make migrate` on the local database only. No `make db-reset`. Run the data-quality audit and verify-import against current table names (`krithi_source_evidence`, `import_status`). Composer Tyagaraja must still be 675 identities until an approved split changes that count on purpose. Dikshitar and Syama Sastri tag links must be unchanged.
- `git diff --check`.

## Implementation checklist

- [x] Write the 684-subject skeleton with census facts and `unreviewed` claims.
- [x] Record the eight collisions at source-row grain.
- [ ] Classify formal sets, ritual collections, and the dramatic annex with locators. Ghana-raga Pancharatnam is recorded only as five unreviewed automated suggestions.
- [ ] Classify addressee, theme, and geography for every remaining subject, or mark the axis `unresolved`.
- [x] Generate the concordance and prove the counts.
- [ ] Apply only manifest-approved memberships, and protect `R__seed_07`.

## Progress Log

- **2026-09-30**: Created draft Intent from the combined user-reviewed analyses and registered TRACK-147. Recorded the three-way corpus-count discrepancy and the shared-contract dependency on TRACK-146. Spec, Plan, imports, classification application and implementation have not started.
- **2026-09-30**: Intent accepted by Seshadri. Census of `sangita_grantha`: 675 identities, 684 thyagaraja-vaibhavam source rows, eight raga/tala collisions, deity and temple empty, Ghana-raga Pancharatnam present only as a heuristic tag. Musicologist review recorded. Spec written as Draft. No schema, seed, or per-krithi classification applied.
- **2026-09-30**: Spec accepted by Seshadri. Draft plan sets the classification grain at 684 subjects: 667 single-source identities plus 17 source rows behind the eight collisions. Those eight catalogue ids stay unclassified. Shared contract remains with TRACK-146. No master file, seed, or per-krithi claim written yet.
- **2026-10-01**: Plan accepted by Seshadri. Wrote the 684-subject master from the live catalogue, the empty application manifest, and the generated concordance. The eight collisions are source-row subjects. Five Ghana-raga group claims are unreviewed automated suggestions. No deity, shrine, ritual, or dramatic claim is assigned, and nothing is seedable.
- **2026-10-01**: Reconciled the 687-row import CSV, using TRACK-146's hashed-source pattern. The master now records its path and SHA-256, and the generator re-verifies every row's title, raga and URL. All 684 source rows match by exact URL. The other three rows are recorded and left unresolved. Row 241, `karuNa jUDavamma` (tODi), is mapped to Syama Sastri's `karuNa jUDavammA` (Varāli, `cf983d2a`); TRACK-146 flags the same witness. Row 587, `SrI gaNa nAthaM` (kanakAngi), is mapped to Dikshitar's `SrI gaNa nAthaM` (Eeshamanohari, `e8fdd062`). Row 557, `sItA nAyaka` (rIti gauLa), has no import row. The external-gap register now holds only the 692 target. The two cross-composer mappings change neither the 675 identities nor the 684 subjects. No catalogue data was changed.
