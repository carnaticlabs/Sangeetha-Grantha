| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-09-10 |
| **Author** | Sangeetha Grantha Team |
| **Document Type** | Current guide |

# Embedding pipeline and index operations

---

The embedding capability turns canonical composition text into profile-bound search documents and vectors stored in PostgreSQL. It powers the Curator Console's Hybrid and Semantic modes. It is a separate indexing operation from source extraction and import; new or corrected content is not automatically re-embedded.

For API behavior and ranking, read [Catalogue search](../03-api/search.md). This guide covers document construction, indexing tools, activation, freshness, and verification as implemented in the repository.

## 1. What gets embedded

The runners select compositions with composer/reference metadata and available lyric content. They build two current document kinds:

| Document kind | Eligibility and content | Identity |
|:---|:---|:---|
| `COMPOSITION_OVERVIEW` | Selected primary/earliest lyric text longer than 10 trimmed characters; formatted overview includes up to 600 characters of that text | Composition-level document |
| `SECTION_PASSAGE` | Stored lyric-section text with at least 5 trimmed characters | Composition + section + lyric variant |

The overview lyric selection orders variants by primary flag, then creation time. Section documents preserve each eligible stored variant's language/script context. The schema also defines commentary/manuscript document kinds, but these runners do not implement a manuscript/media ingestion pipeline.

[context_formatter.py](../../tools/krithi-extract-enrich-worker/src/embeddings/context_formatter.py) adds composition, composer, form, section, primary-raga, tala, deity, kshetra, and tag context where available. It adds a diacritic-free raga-name alternative and removes recognized transliteration boilerplate/blank lines from indexed text.

The current indexing queries use the composition's **primary raga** for the context header. The catalogue's full ordered Ragamalika sequence remains a separate relationship; do not claim that the embedding header contains all per-section raga associations.

Example of the formatter's text shape:

```text
[Composition: <title>] [Composer: <composer>] [Section: PALLAVI] [Raga: <raga>] [Tala: <tala>]
Text:
<stored section text>
```

This is retrieval context, not generated source material. Original text and indexed text have separate fields.

## 2. Storage contract

[Migration V58](../../database/migrations/V58__semantic_search_pgvector.sql) defines:

| Table | Key fields and constraints |
|:---|:---|
| `search_documents` | Composition/section/variant/kind/chunk identity; original/indexed content; language/script; content hash; publication flag; unique identity with nulls treated consistently |
| `embedding_profiles` | Unique model/dimensions/task-type combination; active flag |
| `document_embeddings` | Unique document/profile pair; `vector(768)`; embedding content hash |

Vectors use an HNSW cosine index (`m=16`, `ef_construction=64`). Indexed text also has a trigram index. Deleting an obsolete search document cascades to its embeddings. Profiles are separate generations of a vector space, not interchangeable labels.

The code defaults to `gemini-embedding-2` and 768 output dimensions. Document requests use `RETRIEVAL_DOCUMENT`; user query requests use `RETRIEVAL_QUERY`. The storage validator rejects other dimensions before embedding calls. A new width requires a schema change, not simply a new profile or CLI flag.

## 3. Tools and execution modes

| Tool | Use | Important distinction |
|:---|:---|:---|
| [embed_catalogue.py](../../tools/krithi-extract-enrich-worker/scripts/embed_catalogue.py) | Target one composition, a bounded set, or the whole catalogue | Exposes model, dimensions, database URL, Vertex options and profile activation |
| [batch_embed_catalogue.py](../../tools/krithi-extract-enrich-worker/scripts/batch_embed_catalogue.py) | Process many compositions in paced local batches and write an execution report | Calls embeddings per document; this is not submission to the provider's asynchronous Batch API |
| [evaluate_retrieval.py](../../tools/krithi-extract-enrich-worker/scripts/evaluate_retrieval.py) | Run the repository's retrieval benchmark | Provider-backed evaluation; separate from deterministic tests |
| [catalogue_index.py](../../tools/krithi-extract-enrich-worker/src/embeddings/catalogue_index.py) | Shared profile, dimension, audit and document-retirement rules | Used by both indexing entry points |

Run tools from `tools/krithi-extract-enrich-worker` after `uv sync --frozen --extra dev`.

## 4. Configure the intended target

The direct runner takes `--db-url`, defaulting to `DATABASE_URL` or the local PostgreSQL connection. The batch runner uses `DATABASE_URL`, falling back to `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, and `DB_PASSWORD`.

The Python embedder resolves an explicit key, then `SG_GEMINI_API_KEY`/`GEMINI_API_KEY`, then worker settings and a local-config fallback. Prefer explicit environment configuration for predictable runs. The direct runner also exposes `--vertexai` and `--project-id` for its implemented Vertex/ADC path.

Backend query embedding is wired in [AppModule.kt](../../modules/backend/api/src/main/kotlin/com/sangita/grantha/backend/api/di/AppModule.kt) with the backend Gemini key and the client's default model/dimensions. There is currently no dedicated embedding-model environment override wired there. A CLI `--model` change does not change the running query embedder.

[Runtime configuration](../08-operations/config.md) distinguishes enrichment settings from embedding/query configuration. Normal public catalogue reads do not need an embedding provider.

## 5. Preview before indexing

The following read the selected database and preview indexing work without calling the embedding provider or mutating the database:

```bash
uv run python scripts/embed_catalogue.py --limit 5 --dry-run
uv run python scripts/batch_embed_catalogue.py --max-krithis 5 --dry-run --report-path /tmp/embedding-preview.md
```

A batch preview still writes its local report. It does not replace the latest real execution report in application documentation. Read-only profile lookup means a dry run does not create an active profile on a fresh database.

## 6. Run and maintain an index

For an authorized bounded run:

```bash
uv run python scripts/embed_catalogue.py --limit 5
uv run python scripts/batch_embed_catalogue.py --max-krithis 50 --batch-size 10 --delay 1 --report-path /tmp/embedding-run.md
```

Use `--krithi-id` for a targeted direct run after a known correction. Use `--all` for a deliberate direct full-catalogue run; the batch runner defaults to all candidates unless limited. Real runs call an external provider and write database records.

The runners compare formatted-content hashes to skip already-indexed work and use document/profile upserts. `--force` requests re-embedding even when the skip check would normally apply. Removed/ineligible source text leads to obsolete-document retirement. Re-run after imports, lyric edits, section changes, and changes to metadata used by the formatter.

Hashes are implementation freshness keys (currently MD5 of formatted text), not cryptographic provenance checksums. Inspect both document and embedding hashes when validating a model-profile refresh. The scripts are incremental maintenance tools, not a background scheduler or a complete guarantee of corpus coverage.

Each composition commits separately. A failure rolls back that composition's transaction, allows other compositions to proceed, and causes a nonzero run outcome. The direct runner logs failed IDs; the batch runner also persists failure details. Shared helpers audit profile creation/activation and indexing changes.

## 7. Profile activation

With no active profile, a newly created first profile becomes active. When another profile is active, a replacement model's profile is created inactive so its partial backfill does not immediately replace live retrieval.

`--activate-profile` requests activation after a successful run. The helper verifies the profile exists and has embeddings, then deactivates other profiles and activates the target in one transaction. Activation is skipped when a runner reports failures.

**Coverage remains an operator responsibility:** “has at least one embedding” is not “all intended compositions are indexed.” A successful limited run can satisfy the activation helper's minimum check while leaving most of the corpus unindexed. Review intended coverage and evaluation before activation, and align the backend query model/dimensions at the same time.

The runtime service checks profile model/dimensions against its query client. An incompatible active profile produces an availability error rather than mixing vectors. If multiple profiles are active, the repository chooses the newest and logs a warning; the schema does not enforce a single active row by itself.

## 8. Inspect freshness and coverage

These are read-only diagnostic queries:

```sql
SELECT id, model_name, dimensions, task_type, is_active
FROM embedding_profiles
ORDER BY created_at DESC;

SELECT p.id, p.model_name, p.is_active,
       count(e.id) AS vectors,
       count(DISTINCT d.krithi_id) AS compositions
FROM embedding_profiles p
LEFT JOIN document_embeddings e ON e.profile_id = p.id
LEFT JOIN search_documents d ON d.id = e.document_id
GROUP BY p.id, p.model_name, p.is_active;

SELECT count(*) AS mismatched_content_hashes
FROM document_embeddings e
JOIN search_documents d ON d.id = e.document_id
WHERE e.content_hash <> d.content_hash;
```

Compare coverage with eligible source content, not only total `krithis` count: empty/short lyrics are deliberately excluded from some documents. Scripts can index non-published canonical rows; public retrieval also checks the current composition workflow state and document visibility. Do not infer publication from an embedding row.

## 9. Evaluate and record evidence

Run deterministic worker embedding tests and backend route/repository checks for code behavior. A provider-backed benchmark is a separate, intentionally configured run:

```bash
uv run python scripts/evaluate_retrieval.py --mode all --k 5
```

The evaluator uses [retrieval_benchmarks.json](../../tools/krithi-extract-enrich-worker/evals/retrieval_benchmarks.json). Record corpus scope, active model/profile, application revision, query set, and actual results. The evaluator has its own query implementation, so also spot-check the mounted API and console filters/visibility.

The batch runner writes a report and mirrors real runs into [embedding-execution-report.md](../10-implementations/embedding-execution-report.md). Its token/cost fields are estimates, not provider billing. Historical reports do not prove the current database's index state.

[Search API and ranking](../03-api/search.md) · [TRACK-108 evidence](../10-implementations/track-108-validation-2026-09-08.md) · [Quality](../07-quality/README.md)

---

[Section index](./README.md) · [Documentation home](./../README.md) · [Feature status](./../01-requirements/features/README.md)
