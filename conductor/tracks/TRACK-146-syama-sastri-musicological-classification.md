| Metadata | Value |
|:---|:---|
| **Status** | In Progress |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-09-30 |
| **Author** | Sangeetha Grantha Team |

# Track: Syama Sastri Corpus Concordance & Musicological Classification

**ID:** TRACK-146  
**Status:** In Progress — Intent review  
**Owner:** Seshadri  
**Created:** 2026-09-30  
**Updated:** 2026-09-30

---

## Goal

Produce an evidence-backed classification master for the reconciled Syama Sastri catalogue, comparable to the Dikshitar master in coverage and scholarly accountability, with emphasis on musical form, rhythmic design, devotional address, group membership and carefully qualified shrine associations. Establish the shared classification conventions that TRACK-147 will extend for Tyagaraja.

## Context

- **User request:** Plan equivalents of the Dikshitar classification master for Syama Sastri and Tyagaraja; combine the musicological analysis with repository and import findings; start TRACK-146 and TRACK-147.
- **Predecessor:** [TRACK-145](TRACK-145-dikshitar-kshetra-musicological-metadata.md) and its [feature document](../../application_documentation/01-requirements/features/dikshitar-kshetra-musicological-enrichment.md).
- **Companion:** [TRACK-147](TRACK-147-tyagaraja-musicological-classification.md).
- **Historical import baseline:** [TRACK-093](TRACK-093-trinity-krithi-bulk-import.md) reports 70 imported Syama Sastri compositions against a target of 71. This is not a current database census. The supplied analysis reports 71 CSV rows and 34 distinct source raga labels; both require source-file verification and canonical identity reconciliation.
- **Existing enrichment:** [TRACK-029](TRACK-029-bulk-import-kshetra-mapping.md) was designed around TempleNet links embedded in Guru Guha posts. Its reusable resolution, cache and geocoding components do not establish composition-to-shrine evidence for this source.
- **Existing group seed:** [R__seed_07](../../database/migrations/R__seed_07_canonical_cycle_tags.sql) includes the Swarajathi Ratnatrayam tag. Its composer/title heuristics and tag-wide deletion require provenance-aware reconciliation before reviewed memberships are applied.
- **Shared substrate:** [V66](../../database/migrations/V66__dikshitar_musicological_enrichment.sql), [V67](../../database/migrations/V67__search_document_anchor_constraints.sql), the [database schema](../../application_documentation/04-database/schema.md), and the [domain model](../../application_documentation/01-requirements/domain-model.md).
- **Dikshitar reference supplied by user:** local research artifact at `/Users/seshadri/.gemini/antigravity/brain/9ed7e87b-352e-49d0-91cd-b0db50c22807/dikshitar-corpus-481-classification-master.md`. It is comparative input, not a portable repository dependency. Its grouped-composition and membership totals need reconciliation before adopting the reporting format.

## Intent

**Status:** Draft  
**Accepted by:** —  
**Accepted at:** —

### Problem

We want the same composition-by-composition accountability available for Dikshitar, without imposing his Sanskrit vibhakti and pilgrimage framework on Syama Sastri. The current small corpus is suitable for complete expert curation, but flat Kamakshi tags do not distinguish compositions by musical form, rhythm, devotional meaning or supported shrine association.

The recorded 70/71 import difference is unresolved. Current deity, temple, form, lyric, source, raga/tala junction and variant coverage has not been measured for this initiative. A populated foreign key or a title match is not proof of correct attribution or classification. In particular, composer matching on `sastri` risks conflating Syama Sastri with Subbaraya or Annaswami Sastri.

### Proposed outcome

1. **Reconciled corpus snapshot.** Account for every source candidate and database record, distinguishing imported compositions, aliases, duplicates, failed/pending imports, exclusions and unresolved attribution. Record source hashes or equivalent version identifiers and a snapshot date. Do not declare a composition missing from a count difference alone.
2. **Complete classification concordance.** Give every composition in the declared snapshot a stable identity and a review state. Maintain structured master data and generate the readable Markdown concordance and distributions from it. Report distinct compositions separately from membership rows and variant witnesses.
3. **Composer-specific axes.** Classify musical form, devotional address, deity/addressee, supported shrine association, groups and relationships. Allow multiple supported classifications; any primary display axis must not constrain the data model.
4. **Ratnatrayam pilot.** Review *Kamakshi anudinamu* (Bhairavi), *Rave himagiri kumari* (Todi), and *Kamakshi ni padayugamu* (Yadukulakambhoji) against source editions and catalogue identities. Preserve swarajathi structure and swara–sahitya alignment; document tala and rhythmic observations with their witnesses.
5. **Rhythmic annex.** Capture tala as stated by the source, eduppu with explicit units/convention, arudi and subdivision where notation or documented rendering supports them. Preserve alternative interpretations rather than promoting one rendition to a universal composition property.
6. **Qualified group and geography review.** Investigate Navaratnamalika membership source by source; do not manufacture a complete nine-item set. Review candidate Kanchi Kamakshi, Thanjavur Bangaru Kamakshi, Madurai Meenakshi and other shrine associations without assuming all Kamakshi compositions belong to one of two locations. Separate formal groups, thematic collections and related musical settings.
7. **Claim-level evidence.** Record each claim's source locator, relevant passage or notation reference, evidence type, review status and uncertainty. Distinguish textual evidence, scholarly interpretation, documented tradition and automated suggestion. Human adjudication is required before low-confidence or disputed claims become canonical assignments; unresolved claims may remain in the master.
8. **Shared conventions owned here.** Define reusable identities, provenance vocabulary, group types, geography states, review rules, master-generation conventions and distinct/member counting. Reuse existing evidence/canon structures before proposing additions. TRACK-147 extends this contract for dramatic works, ritual use and transmission; shared infrastructure must have one implementation owner.
9. **Reviewed application and search proof.** After Plan acceptance, apply curated identities and memberships through Flyway with protected provenance, reconcile legacy heuristic links, and verify data plus discriminative retrieval. Use the inherited 60-word metadata-header budget if retained by the accepted shared design. Coordinate formatter and macro-document ownership with TRACK-145 rather than implementing a competing indexer.

### Affected users and systems

- Musicologists and curators: complete concordance, source comparison and an explicit unresolved-claims register.
- Rasikas and catalogue consumers: reliable distinction between similarly named Kamakshi compositions, swarajathis and source-qualified groups.
- Data pipeline and database: source evidence, attribution, musical forms, raga/tala junctions, tags, memberships and shrine relationships.
- Semantic search: composition and section discriminators, reviewed group/shrine overviews and composer attribution.
- Consumer-facing UI changes are not assumed; any necessary API/search integration must be scoped explicitly in Spec.

### Constraints

- Follow [CLAUDE.md](../../CLAUDE.md) and the [Conductor workflow](../../.agents/skills/conductor-track-manager/SKILL.md): Intent → Spec → Plan acceptance; no product implementation or corpus mutation before Plan acceptance.
- Flyway is the only migration engine. Never edit committed versioned migrations; allocate new migration/seed identifiers during the accepted Plan, not here. Repeatable updates must not overwrite independent reviewed assignments.
- Preserve `DatabaseFactory.dbQuery`, DTO boundaries and mutation auditing for runtime paths; explicitly decide migration/seed audit treatment in Spec.
- Respect the domain model's musical-form, ordered-raga and independent-notation contracts. Lyrics alone cannot establish eduppu, sangatis or detailed rhythmic design.
- Prefer existing `BHAVA`, `PHILOSOPHY`, `KSHETRA` and other controlled categories before expanding taxonomy. Group type and tag category are separate decisions.
- An empty `temple_id` must not silently mean both unreviewed and non-specific. Proposed geography states are `UNREVIEWED`, `SPECIFIC_SHRINE_SUPPORTED`, `NON_SPECIFIC_ADDRESS`, `UNRESOLVED` and `DISPUTED`; these are candidates, not enacted schema values.
- Completion means every snapshot record is accounted for and reviewed or explicitly unresolved; it does not require every composition to have a temple or formal group.
- Agent review supports scholarly checking; it does not replace human musicological acceptance.
- No commits, production deployment, paid embedding runs or destructive database resets are authorised by this track creation.

### Open questions

1. Where is the authoritative 71-row source inventory, and what explains its difference from the recorded 70 imports?
2. Which editions/notation witnesses are available for corpus-wide attribution and rhythmic review? Which sources establish Navaratnamalika membership?
3. Which existing source/canon tables can represent claim-level evidence and musical witnesses, and which additions are necessary?
4. Who provides human musicological adjudication, and what minimum evidence makes a claim seedable?
5. What is the boundary between these tracks and TRACK-145's unfinished formatter/macro indexing work? Macro-document presence must not be mistaken for live API exposure.
6. What ranked-retrieval thresholds and reviewed expected identities will be accepted for the benchmark?

### Intended deliverables and completion evidence

- Snapshot and source reconciliation report; baseline coverage by field and junction table.
- Structured Syama Sastri master, generated Markdown concordance, rhythmic annex, source bibliography and unresolved-claims register.
- Shared classification contract referenced by TRACK-147, with ownership decisions documented before implementation.
- Reviewed membership/shrine change manifest and provenance-aware application strategy.
- Retrieval probes covering Bhairavi Kamakshi swarajathi identity, cross-Sastri attribution, uncertain shrine abstention, source-qualified Navaratnamalika membership and related musical settings.
- After accepted implementation: [data-quality audit](../../.agents/skills/data-quality-audit/SKILL.md), [verify-import](../../.claude/skills/verify-import/SKILL.md), repeatable rerun protection, affected-layer checks and retrieval results. Exact implementation proof belongs in Plan.

### Research starting points

- Source blog: `syamakrishnavaibhavam.blogspot.com`; candidate evidence requires edition cross-checking.
- [Music Academy catalogue: Govinda Rao's combined Sastri volume](https://musicacademymadras.in/catalogue/bookbrowse.php?id=747): candidate edition; distinguish the three composers within it.
- [Music Academy Journal 2023](https://www.musicacademymadras.in/wp-content/uploads/2024/11/M-A-Journal-2023.pdf): discussion of the Madurai/Navaratnamalika association; not a complete membership proof.
- [Music Academy 1983 souvenir](https://musicacademymadras.in/catalogue/files/souv/1983_57th%20conference%20souvenir%2C%20programme.pdf): historical rhythmic discussion and related musical settings; historical corpus estimates are not the present catalogue denominator.

## Spec

**Status:** Not Started — pending Intent acceptance  
**Accepted by:** —  
**Accepted at:** —

Requirements, design, flagged concerns and carried-forward questions will be written from the accepted Intent using the repository's spec workflow. No schema or taxonomy choice above is accepted implementation design.

## Plan

**Status:** Not Started — pending Spec acceptance  
**Accepted by:** —  
**Accepted at:** —

File ownership, execution order, migration allocation, risk controls and verification commands will be specified after Spec acceptance.

## Progress Log

- **2026-09-30**: Created draft Intent from the combined user-reviewed analyses; registered TRACK-146 and assigned the Syama Sastri pilot/shared conventions here. Historical counts remain provisional pending source and database reconciliation. Spec, Plan, data curation and implementation have not started.
