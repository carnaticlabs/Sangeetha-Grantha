| Metadata | Value |
|:---|:---|
| **Status** | In Progress |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-09-30 |
| **Author** | Sangeetha Grantha Team |

# Track: Tyagaraja Corpus Concordance & Musicological Classification

**ID:** TRACK-147  
**Status:** In Progress — Intent review  
**Owner:** Seshadri  
**Created:** 2026-09-30  
**Updated:** 2026-09-30

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

**Status:** Draft  
**Accepted by:** —  
**Accepted at:** —

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

**Status:** Not Started — pending Intent acceptance  
**Accepted by:** —  
**Accepted at:** —

Requirements, design, flagged concerns and carried-forward questions will be written from the accepted Intent using the repository's spec workflow. Proposed classifications and relationships above are not accepted schema design.

## Plan

**Status:** Not Started — pending Spec acceptance  
**Accepted by:** —  
**Accepted at:** —

File ownership, source reconciliation/import strategy, execution order, migration allocation, risk controls and proof will be specified after Spec acceptance.

## Progress Log

- **2026-09-30**: Created draft Intent from the combined user-reviewed analyses and registered TRACK-147. Recorded the three-way corpus-count discrepancy and the shared-contract dependency on TRACK-146. Spec, Plan, imports, classification application and implementation have not started.
