| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.3.0 |
| **Last Updated** | 2026-09-10 |
| **Author** | Sangeetha Grantha Team |
| **Document Type** | Current guide |

# Source quality and curation strategy

---

Catalogue quality depends on what a source asserts, how faithfully extraction preserves it, and how curators resolve disagreements. A successful parser or a high confidence score is only part of that chain.

## 1. Quality dimensions

| Dimension | Question | Evidence |
|:---|:---|:---|
| Source authority | Why should this source inform the catalogue? | Registry metadata, source tier, scholarly context |
| Extraction fidelity | Did parsing preserve the source text and section boundaries? | Artifact, extraction payload, parser/version, source comparison |
| Identity | Does this text belong to the intended composition/raga/composer? | Candidates, aliases, mela context, curator decision |
| Structure | Are sections and their order faithful to the source? | Labels, ordered sections, cross-source comparison |
| Variants | Are script/source readings retained separately? | Variant identity and lyric-section joins |
| Provenance | Can accepted content be traced to its origin? | Source documents, extraction IDs, revisions, audit |
| Public completeness | What can a reader actually access? | Published catalogue and selected lyric variant responses |

## 2. Current implementation

The working stack includes source registration, Python HTML/PDF extraction, canonical payload exchange, Kotlin matching/persistence, source evidence, structural voting, variant matching, curator review, and quality diagnostics. The console consolidates sources with processing and evidence with verification.

Some sourcing coverage/audit endpoints return placeholder structures. Treat those charts as incomplete features, not as a clean audit. Use dedicated quality audit routes and database checks for actual structural diagnostics.

The [ingestion guide](../features/bulk-import/02-implementation/technical-implementation-guide.md) owns pipeline details. [Curator Console](../../05-frontend/admin-web/ui-specs.md) owns the current UI.

## 3. Source authority hierarchy

Source tiers provide an ordered curation signal, with lower numbers representing greater assigned authority. Record the rationale for each registry entry and distinguish published scholarship, institutional material, curated websites, and individual collections. A tier is not a blanket guarantee that every extracted section is correct.

Maintain source format, composer affinity where useful, URL/document identity, retrieval/checksum evidence, extraction method, and extractor version. Check the actual source adapter before scheduling a format merely listed in the schema.

## 4. Preserve disagreements and variants

Retain language, script, transliteration scheme, source, and tradition labels separately. A same-script variant can still be a different source reading. Per-script variations belong to that reading; boilerplate or word-division headings should not become false composition sections.

Cross-source voting can identify agreement and dissent. Curators should inspect the underlying section sequences and source authority before overriding a vote. Do not add Anupallavi, relabel a Samashti Charanam, or discard Madhyama Kala text solely to match a majority template.

## 5. Reference identity

Composer aliases and raga identity rules reduce duplicate records. Raga identity is mela-qualified and uses aliases/provenance, with ambiguous names held for curator resolution. Similarity is a candidate signal, not a license to mint a new raga or merge distinct-scale names.

See [raga identity](../../04-database/raga-identity.md), [ADR-016](../../02-architecture/decisions/ADR-016-raga-naming-authority.md), and [ADR-017](../../02-architecture/decisions/ADR-017-raga-reference-entity-identity-resolution.md).

## 6. Correction workflow

1. Identify the affected composition, source, variant, and section.
2. Compare the artifact with the extracted payload and current reader output.
3. Repair the parser or controlled curator mapping at the stage that introduced the defect.
4. Re-extract and review/reingest the affected scope.
5. Verify section/variant joins, ordered Ragamalika membership, source attribution, revision history, and public visibility.
6. Refresh the search index if the corrected text is indexed.

New one-off corpus corrections do not belong in Flyway migrations. Flyway manages schema and reference seeds; accepted content goes through the provenance-aware pipeline. See [versioned canon](../../04-database/versioned-canon.md).

## 7. Acceptance and reporting

Record the dataset/source scope, date, parser revision, commands, checks, failures, and unresolved decisions. A report should distinguish counts observed in a database from test fixture totals and product targets. Preserve unknown/unclassified states and publish only what the workflow allows.

[Post-import verification](../../07-quality/qa/test-plan.md) is the current checklist. The [older phased implementation checklist](./implementation-checklist.md), [PDF encoding analysis](./pdf-diacritic-extraction-analysis.md), and [original UI plan](./ui-ux-plan.md) preserve design history; the [feature map](../features/README.md) provides current scope.

---

[Section index](./README.md) · [Documentation home](./../../README.md) · [Feature status](./../features/README.md)
