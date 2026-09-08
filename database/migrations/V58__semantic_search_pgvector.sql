-- V58__semantic_search_pgvector.sql
-- Purpose: Provision pgvector extension, search documents, embedding profiles, and vector tables
-- for Gemini Embedding 2 (768-D MRL) semantic search and hybrid retrieval.
-- Ref: application_documentation/10-implementations/track-108-semantic-search-gemini-embedding-2.md
-- Conductor Track: TRACK-108

SET search_path TO public;

-- 1. Extensions
CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- 2. Enums
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'search_document_kind_enum') THEN
    CREATE TYPE search_document_kind_enum AS ENUM (
      'COMPOSITION_OVERVIEW',
      'SECTION_PASSAGE',
      'COMMENTARY_LAKSHANA',
      'MANUSCRIPT_FACSIMILE'
    );
  END IF;
END$$;

-- 3. Search Documents table (anchors text & media to canonical entities)
CREATE TABLE IF NOT EXISTS search_documents (
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    krithi_id UUID NOT NULL REFERENCES krithis(id) ON DELETE CASCADE,
    section_id UUID REFERENCES krithi_sections(id) ON DELETE CASCADE,
    variant_id UUID REFERENCES krithi_lyric_variants(id) ON DELETE CASCADE,
    document_kind search_document_kind_enum NOT NULL,
    language_code VARCHAR(16),
    script_code VARCHAR(16),
    source_chunk_index INT NOT NULL DEFAULT 0,
    original_content TEXT NOT NULL,
    indexed_content TEXT NOT NULL,
    content_hash VARCHAR(64) NOT NULL,
    media_gcs_uri VARCHAR(512),
    is_published BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),
    CONSTRAINT uq_search_doc_identity UNIQUE NULLS NOT DISTINCT (
        krithi_id, section_id, variant_id, document_kind, source_chunk_index
    )
);

-- 4. Embedding Profiles table (versioned model and dimension parameters)
CREATE TABLE IF NOT EXISTS embedding_profiles (
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    model_name VARCHAR(64) NOT NULL, -- e.g. 'models/gemini-embedding-2'
    dimensions INT NOT NULL,         -- e.g. 768
    task_type VARCHAR(32) NOT NULL DEFAULT 'RETRIEVAL_DOCUMENT',
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),
    CONSTRAINT uq_embedding_profile UNIQUE (model_name, dimensions, task_type)
);

-- 5. Document Embeddings table (vector(768) storage bound to document and profile)
CREATE TABLE IF NOT EXISTS document_embeddings (
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    document_id UUID NOT NULL REFERENCES search_documents(id) ON DELETE CASCADE,
    profile_id UUID NOT NULL REFERENCES embedding_profiles(id) ON DELETE RESTRICT,
    embedding vector(768) NOT NULL,
    content_hash VARCHAR(64) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),
    CONSTRAINT uq_doc_embedding_profile UNIQUE (document_id, profile_id)
);

-- 6. Indexes
CREATE INDEX IF NOT EXISTS idx_doc_embeddings_hnsw_cosine 
ON document_embeddings 
USING hnsw (embedding vector_cosine_ops)
WITH (m = 16, ef_construction = 64);

CREATE INDEX IF NOT EXISTS idx_search_docs_krithi_id ON search_documents (krithi_id);
CREATE INDEX IF NOT EXISTS idx_search_docs_section_id ON search_documents (section_id);
CREATE INDEX IF NOT EXISTS idx_search_docs_variant_id ON search_documents (variant_id);
CREATE INDEX IF NOT EXISTS idx_search_docs_published ON search_documents (is_published);
CREATE INDEX IF NOT EXISTS idx_search_docs_trgm ON search_documents USING gin (indexed_content gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_doc_embeddings_document_id ON document_embeddings (document_id);
CREATE INDEX IF NOT EXISTS idx_doc_embeddings_profile_id ON document_embeddings (profile_id);
