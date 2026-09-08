# TRACK-108: Semantic Search & Thematic Discovery with Gemini Embedding 2 and pgvector

**Status:** Completed  
**Date:** September 6, 2026  
**Scope:** TRACK-108  
**Architecture:** Postgres 18 + `pgvector` HNSW (768-D) + Gemini Embedding 2 (`models/gemini-embedding-2`) + Reciprocal Rank Fusion (RRF) + React 19 Admin UI  

---

## 1. Executive Summary

TRACK-108 implements semantic search and thematic discovery for the Sangita Grantha catalogue. By leveraging Google DeepMind's **Gemini Embedding 2** (`gemini-embedding-2`) model with **Matryoshka Representation Learning (MRL)** truncated to 768 dimensions and PostgreSQL 18's **pgvector** HNSW cosine index, the platform enables natural-language, cross-script, and thematic exploration without introducing a separate vector database or external infrastructure dependencies.

### Core Highlights
1. **Zero External Vector DB Infrastructure:** Postgres 18 + `pgvector` 0.8.6 runs locally inside Docker (`pgvector/pgvector:pg18`) with shared memory configuration (`shm_size: 1g`).
2. **Musicological Context Framing:** Context chunking formats both macro-level composition overviews and micro-level section passages (`Pallavi`, `Anupallavi`, `Charanam`, `Madhyamakala`) with structured headers (`[Composition: ...] [Composer: ...] [Raga: ...] [Tala: ...] [Deity: ...] [Temple: ...]`).
3. **Hybrid Search via Reciprocal Rank Fusion (RRF):** Blends dense vector cosine distance (`<=>`) with PostgreSQL `pg_trgm` trigram lexical matching, combining semantic recall with incipit precision.
4. **Clean Multi-Layer Architecture:**
   - Database: Migration `V58__semantic_search_pgvector.sql` with versioned `embedding_profiles`, `search_documents`, and `document_embeddings`.
   - Worker: CLI embedding backfill script (`scripts/embed_catalogue.py`) and frozen 20-query evaluation benchmark runner (`scripts/evaluate_retrieval.py`).
   - Backend: Ktor routes (`/v1/search/hybrid` and `/v1/search/semantic`), `HybridSearchService`, `KrithiSearchRepository`, and `GeminiEmbeddingClient`.
   - Frontend: Modern segmented pill search toggle (Lexical / Hybrid / Semantic) in `KrithiList.tsx` with match confidence badges.

---

## 2. Architecture & Data Model

### 2.1 Database Schema (`V58__semantic_search_pgvector.sql`)

```sql
CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE embedding_profiles (
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    profile_code VARCHAR(64) NOT NULL UNIQUE,
    model_name VARCHAR(128) NOT NULL,
    dimensions INT NOT NULL,
    task_type VARCHAR(64) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE search_documents (
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    krithi_id UUID NOT NULL REFERENCES krithis(id) ON DELETE CASCADE,
    section_id UUID REFERENCES krithi_sections(id) ON DELETE CASCADE,
    variant_id UUID REFERENCES krithi_lyric_variants(id) ON DELETE CASCADE,
    document_kind VARCHAR(32) NOT NULL, -- 'COMPOSITION_OVERVIEW' | 'SECTION_PASSAGE'
    source_chunk_index INT NOT NULL DEFAULT 0,
    indexed_content TEXT NOT NULL,
    content_hash VARCHAR(64) NOT NULL,
    is_published BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_search_documents_grain UNIQUE NULLS NOT DISTINCT (
        krithi_id, section_id, variant_id, document_kind, source_chunk_index
    )
);

CREATE TABLE document_embeddings (
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    document_id UUID NOT NULL REFERENCES search_documents(id) ON DELETE CASCADE,
    profile_id UUID NOT NULL REFERENCES embedding_profiles(id) ON DELETE CASCADE,
    embedding vector(768) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_document_embeddings_profile UNIQUE (document_id, profile_id)
);

CREATE INDEX idx_doc_embeddings_hnsw_cosine 
ON document_embeddings USING hnsw (embedding vector_cosine_ops)
WITH (m = 16, ef_construction = 64);
```

---

## 3. Python Extraction Worker & Evaluation Pipeline

- **Context Formatter (`context_formatter.py`):** Prepares rich semantic documents with musicological tags.
- **Gemini Embedder (`gemini_embedder.py`):** Calls `gemini-embedding-2` with `output_dimensionality=768` and asymmetric task types (`RETRIEVAL_DOCUMENT` vs `RETRIEVAL_QUERY`).
- **CLI Backfill Script (`scripts/embed_catalogue.py`):** Idempotent backfill with hash deduplication, progress reporting, and `--limit`, `--dry-run`, `--force`, and `--activate-profile` flags. Shares `src/embeddings/catalogue_index.py` with `scripts/batch_embed_catalogue.py`: `--dims` is pinned to the `vector(768)` column, a second model's profile is created inactive and switched atomically with `--activate-profile`, dry runs are read-only, each composition commits on its own and rolls back on failure (failed ids persisted, exit status 1), obsolete documents are retired, and every write is audited.
- **Frozen Benchmark Suite (`evals/retrieval_benchmarks.json` & `scripts/evaluate_retrieval.py`):** 20 curated queries evaluating incipits, charanam phrases, thematic deities/temples, cross-script queries, and negative separation.

---

## 4. Backend DAL & Ktor Hybrid Retrieval

- **DAL Execution (`KrithiSearchRepository.kt`):** Implements `searchSemantic()` and `searchHybrid()` with `StatementType.SELECT` over CTEs blending semantic rank and lexical rank via Reciprocal Rank Fusion ($RRF = \frac{1}{60 + \text{rank}_{\text{dense}}} + \frac{1}{60 + \text{rank}_{\text{lex}}}$).
- **Service Layer (`HybridSearchService.kt`):** Coordinates query vector generation via `GeminiEmbeddingClient` and SQL execution via `SangitaDal.krithiSearch`.
- **API Surface (`SemanticSearchRoutes.kt`):**
  - `POST /v1/search/hybrid`
  - `POST /v1/search/semantic`

---

## 5. Frontend User Experience (`sangita-admin-web`)

- **Search Mode Segmented Control:** Allows instant switching between:
  1. `Lexical` (classic filtering and exact match)
  2. `Hybrid` (dense vector + trigram RRF)
  3. `Semantic` (pure dense embedding discovery)
- **Relevance Confidence Badges:** Displays match percentage and RRF scores.
- **Passage Context Snippets:** Displays matched lines with `Passage` vs `Overview` badges and deep linking to krithi details.

---

## 6. Verification & Test Evidence

- **Database:** PostgreSQL 18 container with `vector 0.8.6` runs in Docker; Flyway migration `V58` verified.
- **Backend Tests:** 148 JUnit 5 integration tests pass in Testcontainers (`docker.io/pgvector/pgvector:pg18`), including `SemanticSearchRoutesTest`.
- **Python Tests & Linting:** 5 pytest embedding tests pass; Ruff and Mypy pass with 0 warnings.
- **Frontend Build:** `bun run build` passes with zero type or bundling errors.
