| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.7.0 |
| **Last Updated** | 2026-09-10 |
| **Author** | Sangeetha Grantha Team |
| **Document Type** | Current guide |

# Database schema guide

---

PostgreSQL stores the canonical catalogue, editorial state, ingestion work, source provenance, and the semantic-search index. This guide explains how the table groups fit together. [Flyway SQL](../../database/migrations) is the executable schema; [Exposed tables](../../modules/backend/dal/src/main/kotlin/com/sangita/grantha/backend/dal/tables) and [shared DTOs](../../modules/shared/domain/src/commonMain/kotlin/com/sangita/grantha/shared/domain/model) define application mappings.

## 1. Composition and reference data

| Table/group | Meaning |
|:---|:---|
| `krithis` | Composition identity, metadata, form, language, primary relationships, and workflow state |
| `composers`, `composer_aliases` | Composer identities and alternate names |
| `ragas`, raga identity/alias/relation tables | Canonical raga metadata, mela-qualified identity, alternate names, nomenclature links |
| `talas`, `deities`, `temples`, `temple_names` | Rhythm and devotional/location references |
| `krithi_ragas` | Ordered raga membership, including Ragamalika and optional section association |
| `tags`, `krithi_tags`, `sampradayas` | Controlled thematic and tradition metadata |

A composition must retain its raga junction rows as well as any primary-raga foreign key. Public raga-filtered and Ragamalika views depend on those relationships. [Raga identity](./raga-identity.md) explains why display-name normalization is not a unique identity key.

## 2. Form and lifecycle

Musical forms are `KRITHI`, `VARNAM`, `SWARAJATHI`, and `UNESTABLISHED`. V59 adds the unclassified value; V60 makes it the default for new rows. This does not reclassify all existing corpus rows.

Workflow states stored in PostgreSQL are `draft`, `in_review`, `published`, and `archived`; API enum representations use their serialized DTO contract. Public catalogue V1 excludes unclassified forms; V2 includes published unclassified compositions. Publication and completeness are different concepts.

The [domain model](../01-requirements/domain-model.md) describes form-specific musical correctness. The schema permits representation; documentation must not claim every musicological requirement is a database constraint.

## 3. Lyrics and notation

| Table | Responsibility |
|:---|:---|
| `krithi_sections` | Ordered composition structure and labels |
| `krithi_lyric_variants` | Language, script, source/tradition identity, primary selection, text metadata |
| `krithi_lyric_sections` | Text for a particular variant and composition section |
| `krithi_notation_variants` | Notation type, tala/kalai/eduppu, interpretation and source |
| `krithi_notation_rows` | Ordered swara/jathi rows and optional sahitya/tala alignment |

Language and script are independent. Preserve source variants and do not fill missing lyrics with guessed text. Notation remains distinct from lyric sections. A reader may receive segmented sections, unsegmented stored text, or an explicit incomplete state according to available content.

## 4. Ingestion and quality

| Table/group | Responsibility |
|:---|:---|
| `import_sources` | Registry, source authority, supported-format metadata |
| `imported_krithis` | Imported/staged source data, candidate mapping, review state |
| Import batch/job/task/event tables | Orchestration progress, attempts, failures and operator controls |
| `extraction_queue` | Kotlin requests and Python canonical extraction results |
| `krithi_source_evidence` | Composition-level contributions and source extraction context |
| `structural_vote_log` | Source agreement, dissent and manual verification decisions |
| Variant matching tables | Candidate variant-to-composition review/persistence context |
| `raga_resolution_queue` | Unknown or conflicting raga inputs requiring curator decisions |

Queue row claiming uses locks to coordinate concurrent workers. It does not establish exactly-once processing across retries and downstream persistence. See [ingestion architecture](../01-requirements/features/bulk-import/02-implementation/technical-implementation-guide.md).

## 5. Content history and audit

`source_documents`, `krithi_revisions`, and `krithi_section_revisions` retain source artifacts and accepted historical section content. Current composition/lyric tables remain the API read projection. `audit_log` captures mutation events, while revisions retain materializable text.

Use [versioned canon](./versioned-canon.md) for attribution and as-of semantics, and [audit logging](./audit-log.md) for event handling. Public history endpoints are not implied by the existence of these tables.

## 6. Semantic index

| Table | Responsibility |
|:---|:---|
| `embedding_profiles` | Model, dimensions, task type and active-profile state |
| `search_documents` | Composition/section/variant/chunk identity, original/indexed text, hashes and visibility |
| `document_embeddings` | A document's vector under one profile, with content hash |

[Migration V58](../../database/migrations/V58__semantic_search_pgvector.sql) defines `vector(768)` storage, HNSW cosine indexing, and trigram indexing. Indexing scripts populate and refresh data separately from ordinary imports. [Search](../03-api/search.md) explains profile compatibility and visibility.

## 7. Schema evolution and verification

Flyway applies versioned schema changes and repeatable reference seeds. Environment accounts, development sample content, and test fixtures have separate creation paths. Do not copy a historical “next migration number”; inspect the current directory and registry.

```bash
make migrate-status
make migrate
make raga-lakshana-checks
```

Use [migrations](./migrations.md) for lifecycle/history handling and [database operations](../08-operations/runbooks/database-runbook.md) for backup and diagnostics. After import/repair work, verify junctions, variants, revisions, and actual API output, not only the `krithis` row count.

For document construction, indexing commands, profile activation, and coverage checks, read [Embedding pipeline and index operations](./../09-ai/embeddings.md).

---

[Section index](./README.md) · [Documentation home](./../README.md) · [Feature status](./../01-requirements/features/README.md)
