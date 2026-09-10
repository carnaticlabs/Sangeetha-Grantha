| Metadata | Value |
|:---|:---|
| **Status** | Completed |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-09-10 |
| **Author** | Sangeetha Grantha Team |
| **Document Type** | Current guide |

# TRACK-108 — semantic search and embeddings implementation

---

TRACK-108 implements thematic retrieval over canonical compositions and lyric passages using Gemini embeddings, PostgreSQL/pgvector, and lexical/vector rank fusion. It is available in the Curator Console through Lexical, Hybrid, and Semantic search modes. Rasika currently uses the public V2 catalogue rather than the vector endpoints.

## Current implementation map

| Layer | Delivered behavior | Source |
|:---|:---|:---|
| Schema | Search documents, profile-bound embeddings, fixed 768-dimensional storage, HNSW/trigram indexes | [V58 migration](../../database/migrations/V58__semantic_search_pgvector.sql) |
| Document construction | Composition overview and per-variant section passage with musicological metadata | [Context formatter](../../tools/krithi-extract-enrich-worker/src/embeddings/context_formatter.py) |
| Index maintenance | Direct/local-batch runners, content-hash checks, obsolete-document retirement, audit and profile activation | [Shared index helpers](../../tools/krithi-extract-enrich-worker/src/embeddings/catalogue_index.py) |
| Query embedding | Provider query vector with model/dimension validation | [GeminiEmbeddingClient](../../modules/backend/api/src/main/kotlin/com/sangita/grantha/backend/api/clients/GeminiEmbeddingClient.kt) |
| Retrieval | Published/admin scope, profile binding, cosine retrieval, RRF and composition-level deduplication | [KrithiSearchRepository](../../modules/backend/dal/src/main/kotlin/com/sangita/grantha/backend/dal/repositories/KrithiSearchRepository.kt) |
| API | `POST /v1/search/hybrid`, `POST /v1/search/semantic` | [Routes](../../modules/backend/api/src/main/kotlin/com/sangita/grantha/backend/api/routes/SemanticSearchRoutes.kt) |
| Console | Search mode controls, ranked overview/passage cards and editor navigation | [KrithiList](../../modules/frontend/sangita-admin-web/src/pages/KrithiList.tsx) |

The source migration replaces the earlier approximate SQL sketch in this report. It includes original/indexed text, document and embedding hashes, a model/dimensions/task-type profile key, and actual constraint names.

## Operating guides

- [Search behavior and API](../03-api/search.md): modes, ranking, filters, visibility, missing/incompatible profile behavior.
- [Embedding pipeline](../09-ai/embeddings.md): eligible content, direct versus local-batch tools, configuration, preview/index commands, activation and coverage queries.
- [Configuration](../08-operations/config.md): query/embedder settings versus enrichment.

## Delivery boundaries

Indexing is not automatically triggered by an import or lyric edit. A profile with some vectors is not proof of full corpus coverage. Hybrid's no-profile lexical fallback still searches `search_documents`; an entirely empty search index can return no matches despite a populated ordinary catalogue.

Model/dimension mismatch is an availability error. The backend query model uses the wired client defaults; changing a Python CLI model does not reconfigure the backend. The local batch runner does not use the provider's asynchronous Batch API.

The index currently builds overview/passage text, not manuscript/media embeddings or conversational answers. Relevance scores are not scholarly validation.

## Dated verification evidence

Use [September 8 validation](./track-108-validation-2026-09-08.md), [embedding execution report](./embedding-execution-report.md), and the [track](../../conductor/tracks/TRACK-108-semantic-search.md) for recorded command results and dataset scope. This documentation refresh did not run provider-backed indexing or remeasure retrieval quality. Old counts/cost estimates are not current deployment measurements.

---

[Section index](./README.md) · [Documentation home](./../README.md) · [Feature status](./../01-requirements/features/README.md)
