| Metadata | Value |
|:---|:---|
| **Status** | Not Started |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-06-06 |
| **Author** | Principal Data & AI Engineering review (for Seshadri) |
| **Priority** | P2 — first new user-facing capability after foundation is sound |

# TRACK-108: Semantic Search (Embeddings + pgvector)

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
- [ ] Add `pgvector` extension via a Flyway migration (`database/migrations/VNN__*.sql`, ADR-013 — never Liquibase or custom runners).
- [ ] Create `krithi_embedding` (`krithi_id` FK, `section_id` FK nullable, `vector vector(768)`, `model_version`, `dims`, `created_at`); HNSW index with cosine ops.
- [ ] Update `04-database/schema.md`.

### Phase 2 — Embedding generation
- [ ] Add an `embed_content` path in the extraction worker using the new `google-genai` SDK (depends on TRACK-107).
- [ ] Offline backfill job over the existing catalogue via Batch; idempotent and resumable (checkpoint by `krithi_id`).
- [ ] Re-embed hook: when a krithi's lyrics change, enqueue a re-embed.

### Phase 3 — Query API
- [ ] `POST /v1/search/semantic` (Ktor route → service → DAL `dbQuery`), returns ranked krithis with similarity scores and matched section.
- [ ] Hybrid option: combine semantic rank with existing keyword search for precision.
- [ ] Enforce audit logging per `CLAUDE.md` if any mutation occurs (search is read-only — likely none).

### Phase 4 — UI & evaluation
- [ ] Admin console: "Find similar" affordance on a krithi + a semantic search box.
- [ ] Build a small relevance eval set (hand-labelled "should match" pairs); measure recall@k and eyeball musical sensibility.
- [ ] Decide 768 vs 1536 dims based on the eval, not by default.

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

Ref: application_documentation/sangeetha-grantha-state-of-nation-july-2026.md
