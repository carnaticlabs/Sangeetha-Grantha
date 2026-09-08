| Metadata | Value |
|:---|:---|
| **Status** | Completed |
| **Version** | 2.2.0 |
| **Last Updated** | 2026-09-08 |
| **Author** | Antigravity AI (for Seshadri) |
| **Priority** | P2 — first new user-facing capability after foundation is sound |

# TRACK-108: Semantic Search (Gemini Embedding 2 + pgvector)

> **Implementation & Optimization Completed (2026-09-08):** Implemented using Gemini Embedding 2 (`models/gemini-embedding-2`) with 768-D Matryoshka Representation Learning (MRL) and PostgreSQL 18 `pgvector` HNSW index. Features hybrid search with Reciprocal Rank Fusion (RRF), section/overview context chunking, backfill CLI, 23-query musicological evaluation benchmark (Recall@5: 91.3%, MRR: 0.773), musical forms taxonomy (`R__seed_08_update_musical_forms.sql`), iterative HNSW scanning, overview boosting, and React 19 Admin UI hybrid search mode with dynamic relevance scoring. See [Implementation Summary](../../application_documentation/10-implementations/track-108-semantic-search-gemini-embedding-2.md) and [Validation Report](../../application_documentation/10-implementations/track-108-validation-2026-09-08.md).

## Goal

Deliver the long-parked "semantic search beyond keyword matching" capability (uplift finding F6; opportunity #5 in `09-ai/integration-opportunities.md`) as a shippable v1 — "find similar krithis / search by meaning" over the catalogue — **without standing up a separate ML stack or vector database.** Embeddings live beside the relational data in the existing PostgreSQL 18 instance via `pgvector`.

## Context

The blocker for this feature used to be infrastructure (an embeddings model + a vector store). Both are now cheap:

- **`gemini-embedding-001`** is generally available: multilingual across 100+ languages (covers the Dravidian + Sanskrit corpus), with **Matryoshka Representation Learning** — emit 3072-dim vectors and truncate to 1536 or 768 with minimal quality loss.
- **`pgvector`** keeps vectors in Postgres 18 — no new datastore, no new operational surface, consistent with the project's open-source / minimise-integration-debt posture.

This is a *new feature*, not maintenance — it gets its own track and its own rollout, and it is explicitly **not** gated on any just-announced model (e.g. Gemini Embedding 2); build on the GA `001` model today.

## Architecture / Approach

```text
krithi sections / lyrics ──(offline backfill, batchable)──> gemini-embedding-001 (768-dim)
                                                                   │
                                                   krithi_embedding table (pgvector)
                                                   (krithi_id, section_id, vector, model_version, dims)
                                                                   │
                                                       HNSW index (cosine)
                                                                   │
   query text ──embed──> ANN search ──> ranked similar krithis ──> /v1/search/semantic endpoint ──> Admin UI
```

Design choices:
- **Start at 768 dims** to keep the index small/cheap; raise to 1536/3072 only if recall on the eval set demands it.
- **Store `model_version` and `dims`** per row so re-embeds (model upgrades) are detectable and reversible — reconciliation as a first-class concern.
- **Backfill is offline and batchable** — reuse the Batch pattern from TRACK-107.
- **Embed at the section grain** (pallavi/anupallavi/charanam) as well as whole-krithi, so search can match on a remembered line, not just the title.

## Implementation Plan

### Phase 1 — Schema & infra
- [x] Add `pgvector` extension via a Flyway migration (`database/migrations/V58__semantic_search_pgvector.sql`, ADR-013).
- [x] Create `document_embeddings`, `search_documents`, `embedding_profiles` with 768-D MRL vectors; HNSW index with cosine ops.
- [x] Pinned `pgvector/pgvector:pg18` in Docker Compose and Testcontainers.

### Phase 2 — Embedding generation
- [x] Add `embed_content` path in extraction worker with `google-genai` SDK and Gemini Embedding 2 (`src/embeddings/gemini_embedder.py`).
- [x] CLI backfill tool over existing catalogue with idempotent hashing and resumption (`scripts/embed_catalogue.py`).
- [x] Musicological context framing for composition overviews and section passages (`src/embeddings/context_formatter.py`).

### Phase 3 — Query API
- [x] `POST /v1/search/semantic` and `POST /v1/search/hybrid` Ktor routes (`SemanticSearchRoutes.kt` → `HybridSearchService.kt` → `KrithiSearchRepository.kt`).
- [x] Hybrid search combining dense vector cosine similarity and trigram lexical matching with Reciprocal Rank Fusion (RRF).
- [x] Integration tests in `SemanticSearchRoutesTest.kt` passing against Testcontainers.

### Phase 4 — UI & evaluation
- [x] Admin console: segmented search mode toggle (Lexical / Hybrid / Semantic) in `KrithiList.tsx` with relevance scores and passage previews.
- [x] 20-query frozen musicological evaluation benchmark (`evals/retrieval_benchmarks.json` & `scripts/evaluate_retrieval.py`).
- [x] 768-D Matryoshka dimensionality verified for HNSW performance and sub-60MB memory footprint.

## Acceptance Criteria
- `pgvector` enabled via a Flyway migration; `krithi_embedding` populated for the full catalogue.
- Semantic query returns musically sensible neighbours on a hand-checked sample; recall@k meets an agreed bar.
- Latency acceptable at catalogue scale (HNSW); no separate datastore introduced.
- Re-embed path works on lyric change; `model_version` recorded for every vector.

## Risks
- **Quality at 768 dims insufficient** → MRL lets you raise dims without re-architecting; eval-gate the choice.
- **Embedding drift across model versions** → `model_version` column + reconciliation job; never mix versions silently in one index.
- **Scope creep into "AI search assistant"** → v1 is similarity retrieval only; defer RAG/generative answers.

## Dependencies
- Blocked by: TRACK-107 (new SDK provides `embed_content`).
- Related: TRACK-093 (more catalogue content = better search corpus); TRACK-109 (search endpoint inherits the production-readiness NFRs).

## Progress Log
- 2026-06-06: Track created. Approach fixed: gemini-embedding-001 @ 768-dim MRL + pgvector in Postgres 18; section-grain + whole-krithi embeddings; offline batched backfill.
- 2026-09-05: Prepared a [detailed scope and architecture analysis](../../application_documentation/10-implementations/track-108-conversational-discovery-analysis-sep-2026.md), including current provider documentation, repository findings, proposed features, evaluation gates and delivery stages. Recommendations remain Draft; no implementation or model benchmark was performed.
- 2026-09-08: Addressed live validation anomalies: fixed DAL `1AND` SQL syntax error on authenticated search, configured `hnsw.iterative_scan = 'strict_order'` and `ef_search = 100`, added overview preference boosting, updated Flyway musical forms taxonomy (`R__seed_08_update_musical_forms.sql`) classifying Syama Sastri's Swarajathi Ratnatrayam, embedded musical form metadata, refined Admin UI with default hybrid discovery and dynamic score badges, expanded frozen benchmark to 23 queries achieving 91.3% Recall@5 and 0.773 MRR, and resolved documentation link checks.
- 2026-09-08: Closed validation findings 3, 4, 11, 12, 13 (findings 1, 2, 6–10 were already fixed). New shared module `src/embeddings/catalogue_index.py`: dimension guard (`vector(768)`), inactive replacement profiles with atomic `--activate-profile`, read-only dry-run profile lookup, obsolete-document retirement, and `write_audit`. Both embed scripts commit per composition, roll back on failure, persist failed ids and exit 1; upserts refresh `original_content`; `update_search_headers.py` audits each document. Backend binds each request to one resolved profile (`activeEmbeddingProfile`) and returns 503 on model/dimension mismatch; hybrid degrades to lexical-only when nothing is indexed. Remediation table in the [Validation Report](../../application_documentation/10-implementations/track-108-validation-2026-09-08.md#remediation-status-2026-09-08-same-day).

## Follow-up (separate plan): retrieval evaluator rework

Deferred from the validation remediation by decision on 2026-09-08. Finding 5 of the validation report remains open and is to be planned as its own Intent → Spec → Plan:

- Canonical expected krithi IDs/sets per benchmark query instead of title/composer substring matching (fixes EVAL-09 spacing false negatives and composer-only false positives such as EVAL-14).
- Explicit negative assertions alongside positive criteria; native-script and spelling-variant cases.
- Run the benchmark through the live `POST /v1/search/{semantic,hybrid}` routes (including the publication predicate) and a lexical baseline, not only the evaluator's private SQL.
- Report hit rate separately from recall over a known relevant set; agree relevance and latency thresholds; fail with a non-zero exit code below them so the gate can run in CI.

Ref: application_documentation/10-implementations/track-108-validation-2026-09-08.md

