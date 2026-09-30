-- V66__dikshitar_musicological_enrichment.sql
-- TRACK-145: Temple hierarchy, composition lakshana, cycle memberships,
-- and search-document anchors for Dikshitar kshetra metadata.
--
-- New search_document_kind_enum labels are added here and are not referenced
-- by any constraint. PostgreSQL rejects use of a newly added enum value in
-- the same transaction. The anchor check lands in V67.
-- Ref: application_documentation/01-requirements/features/dikshitar-kshetra-musicological-enrichment.md

SET search_path TO public;

CREATE TYPE mandalam_enum AS ENUM (
    'CHOLA', 'PANDYA', 'TONDAI', 'NADU', 'CHERA', 'KONGU', 'UTTARA'
);

CREATE TYPE place_kind_enum AS ENUM (
    'LOCALITY', 'COMPLEX', 'SANNIDHI', 'MANDAPAM'
);

CREATE TYPE cycle_member_role_enum AS ENUM (
    'CORE', 'DHYANA', 'MANGALAM', 'OPTIONAL', 'DISPUTED_CONJECTURE'
);

CREATE TYPE vibhakti_enum AS ENUM (
    'PRATHAMA', 'DVITIYA', 'TRITIYA', 'CHATURTHI', 'PANCHAMI',
    'SHASHTHI', 'SAPTAMI', 'SAMBODHANA', 'SARVA_VIBHAKTI'
);

CREATE TYPE raga_mudra_enum AS ENUM ('SUDDHA', 'SLESHA', 'NONE');

CREATE TYPE yati_pattern_enum AS ENUM ('GOPUCCHA', 'SROTOVAHA', 'DAMARU', 'NONE');

CREATE TYPE deity_posture_enum AS ENUM ('SAYANA', 'STHANAKA', 'ASINA', 'TANDAVA');

CREATE TYPE bhuta_enum AS ENUM ('AKASHA', 'VAYU', 'AGNI', 'PRITHVI', 'APPU');

ALTER TABLE temples
    ADD COLUMN parent_temple_id UUID REFERENCES temples (id) ON DELETE SET NULL,
    ADD COLUMN place_kind place_kind_enum NOT NULL DEFAULT 'LOCALITY',
    ADD COLUMN mandalam mandalam_enum,
    ADD COLUMN bhuta bhuta_enum,
    ADD COLUMN sthala_vriksha VARCHAR(64),
    ADD COLUMN sthala_tirtha VARCHAR(64),
    ADD COLUMN nadi_tirtha VARCHAR(64),
    ADD COLUMN deity_posture deity_posture_enum;

CREATE INDEX idx_temples_parent ON temples (parent_temple_id);

COMMENT ON COLUMN temples.mandalam IS
    'Classical pilgrimage province. KONGU is defined and left unassigned by the TRACK-145 seed.';

ALTER TABLE krithis
    ADD COLUMN vibhakti_case vibhakti_enum,
    ADD COLUMN vibhakti_stem VARCHAR(64),
    ADD COLUMN raga_mudra_kind raga_mudra_enum NOT NULL DEFAULT 'NONE',
    ADD COLUMN raga_mudra_phrase VARCHAR(128),
    ADD COLUMN yati_pattern yati_pattern_enum NOT NULL DEFAULT 'NONE',
    ADD COLUMN is_manipravala BOOLEAN NOT NULL DEFAULT false,
    ADD COLUMN occasion_note TEXT;

CREATE TABLE krithi_cycle_memberships (
    id UUID PRIMARY KEY DEFAULT uuidv7(),
    krithi_id UUID NOT NULL REFERENCES krithis (id) ON DELETE CASCADE,
    tag_id UUID NOT NULL REFERENCES tags (id) ON DELETE CASCADE,
    sequence_order INT NOT NULL DEFAULT 0,
    role cycle_member_role_enum NOT NULL DEFAULT 'CORE',
    axis_value VARCHAR(64),
    discriminative_attributes JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),
    CONSTRAINT uq_krithi_cycle UNIQUE (krithi_id, tag_id)
);

CREATE INDEX idx_krithi_cycle_tag ON krithi_cycle_memberships (tag_id, sequence_order);
CREATE INDEX idx_krithi_cycle_attributes ON krithi_cycle_memberships USING gin (discriminative_attributes);

ALTER TABLE search_documents
    ALTER COLUMN krithi_id DROP NOT NULL,
    ADD COLUMN temple_id UUID REFERENCES temples (id) ON DELETE CASCADE,
    ADD COLUMN tag_id UUID REFERENCES tags (id) ON DELETE CASCADE;

ALTER TABLE search_documents
    DROP CONSTRAINT uq_search_doc_identity;

ALTER TABLE search_documents
    ADD CONSTRAINT uq_search_doc_identity UNIQUE NULLS NOT DISTINCT (
        krithi_id, temple_id, tag_id, section_id, variant_id, document_kind, source_chunk_index
    );

CREATE INDEX idx_search_docs_temple_id ON search_documents (temple_id);
CREATE INDEX idx_search_docs_tag_id ON search_documents (tag_id);

ALTER TYPE search_document_kind_enum ADD VALUE IF NOT EXISTS 'CYCLE_OVERVIEW';
ALTER TYPE search_document_kind_enum ADD VALUE IF NOT EXISTS 'KSHETRA_OVERVIEW';
