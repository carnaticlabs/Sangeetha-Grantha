-- V67__search_document_anchor_constraints.sql
-- TRACK-145: Anchor check for search documents, after V66 has committed
-- CYCLE_OVERVIEW and KSHETRA_OVERVIEW.
-- Ref: application_documentation/01-requirements/features/dikshitar-kshetra-musicological-enrichment.md

SET search_path TO public;

ALTER TABLE search_documents
    ADD CONSTRAINT chk_search_doc_anchor CHECK (
        CASE
            WHEN document_kind IN (
                'COMPOSITION_OVERVIEW',
                'SECTION_PASSAGE',
                'COMMENTARY_LAKSHANA',
                'MANUSCRIPT_FACSIMILE'
            ) THEN
                krithi_id IS NOT NULL
                AND temple_id IS NULL
                AND tag_id IS NULL
            WHEN document_kind = 'CYCLE_OVERVIEW' THEN
                tag_id IS NOT NULL
                AND krithi_id IS NULL
                AND temple_id IS NULL
                AND section_id IS NULL
                AND variant_id IS NULL
            WHEN document_kind = 'KSHETRA_OVERVIEW' THEN
                temple_id IS NOT NULL
                AND krithi_id IS NULL
                AND tag_id IS NULL
                AND section_id IS NULL
                AND variant_id IS NULL
            ELSE FALSE
        END
    );
