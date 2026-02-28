-- 37__pg18_uuidv7_defaults.sql
-- Purpose: Switch UUID primary key defaults from gen_random_uuid() (v4) to uuidv7() (v7).
-- PostgreSQL 18 introduces uuidv7() which generates time-ordered UUIDs, providing
-- better B-tree index locality and natural chronological ordering for new rows.
-- Existing v4 UUIDs remain valid; only new inserts use v7.
-- Ref: application_documentation/04-database/schema.md

-- migrate:up

-- Domain tables (02)
ALTER TABLE users ALTER COLUMN id SET DEFAULT uuidv7();
ALTER TABLE composers ALTER COLUMN id SET DEFAULT uuidv7();
ALTER TABLE ragas ALTER COLUMN id SET DEFAULT uuidv7();
ALTER TABLE talas ALTER COLUMN id SET DEFAULT uuidv7();
ALTER TABLE deities ALTER COLUMN id SET DEFAULT uuidv7();
ALTER TABLE temples ALTER COLUMN id SET DEFAULT uuidv7();
ALTER TABLE krithis ALTER COLUMN id SET DEFAULT uuidv7();
ALTER TABLE krithi_lyric_variants ALTER COLUMN id SET DEFAULT uuidv7();

-- Import pipeline (04)
ALTER TABLE import_sources ALTER COLUMN id SET DEFAULT uuidv7();
ALTER TABLE imported_krithis ALTER COLUMN id SET DEFAULT uuidv7();

-- Sections, tags, sampradaya, temple names (05)
ALTER TABLE krithi_sections ALTER COLUMN id SET DEFAULT uuidv7();
ALTER TABLE krithi_lyric_sections ALTER COLUMN id SET DEFAULT uuidv7();
ALTER TABLE tags ALTER COLUMN id SET DEFAULT uuidv7();
ALTER TABLE sampradayas ALTER COLUMN id SET DEFAULT uuidv7();
ALTER TABLE temple_names ALTER COLUMN id SET DEFAULT uuidv7();

-- Notation tables (06)
ALTER TABLE krithi_notation_variants ALTER COLUMN id SET DEFAULT uuidv7();
ALTER TABLE krithi_notation_rows ALTER COLUMN id SET DEFAULT uuidv7();

-- Bulk import orchestration (10)
ALTER TABLE import_batch ALTER COLUMN id SET DEFAULT uuidv7();
ALTER TABLE import_job ALTER COLUMN id SET DEFAULT uuidv7();
ALTER TABLE import_task_run ALTER COLUMN id SET DEFAULT uuidv7();
ALTER TABLE import_event ALTER COLUMN id SET DEFAULT uuidv7();

-- Entity resolution cache (17/20)
ALTER TABLE entity_resolution_cache ALTER COLUMN id SET DEFAULT uuidv7();

-- Temple source cache (21)
ALTER TABLE temple_source_cache ALTER COLUMN id SET DEFAULT uuidv7();

-- Source evidence & voting (24, 25)
ALTER TABLE krithi_source_evidence ALTER COLUMN id SET DEFAULT uuidv7();
ALTER TABLE structural_vote_log ALTER COLUMN id SET DEFAULT uuidv7();

-- Extraction queue (27)
ALTER TABLE extraction_queue ALTER COLUMN id SET DEFAULT uuidv7();

-- Variant match (29)
ALTER TABLE variant_match ALTER COLUMN id SET DEFAULT uuidv7();

-- Audit log (01)
ALTER TABLE audit_log ALTER COLUMN id SET DEFAULT uuidv7();

-- migrate:down
-- Revert all tables back to gen_random_uuid()
ALTER TABLE users ALTER COLUMN id SET DEFAULT gen_random_uuid();
ALTER TABLE composers ALTER COLUMN id SET DEFAULT gen_random_uuid();
ALTER TABLE ragas ALTER COLUMN id SET DEFAULT gen_random_uuid();
ALTER TABLE talas ALTER COLUMN id SET DEFAULT gen_random_uuid();
ALTER TABLE deities ALTER COLUMN id SET DEFAULT gen_random_uuid();
ALTER TABLE temples ALTER COLUMN id SET DEFAULT gen_random_uuid();
ALTER TABLE krithis ALTER COLUMN id SET DEFAULT gen_random_uuid();
ALTER TABLE krithi_lyric_variants ALTER COLUMN id SET DEFAULT gen_random_uuid();
ALTER TABLE import_sources ALTER COLUMN id SET DEFAULT gen_random_uuid();
ALTER TABLE imported_krithis ALTER COLUMN id SET DEFAULT gen_random_uuid();
ALTER TABLE krithi_sections ALTER COLUMN id SET DEFAULT gen_random_uuid();
ALTER TABLE krithi_lyric_sections ALTER COLUMN id SET DEFAULT gen_random_uuid();
ALTER TABLE tags ALTER COLUMN id SET DEFAULT gen_random_uuid();
ALTER TABLE sampradayas ALTER COLUMN id SET DEFAULT gen_random_uuid();
ALTER TABLE temple_names ALTER COLUMN id SET DEFAULT gen_random_uuid();
ALTER TABLE krithi_notation_variants ALTER COLUMN id SET DEFAULT gen_random_uuid();
ALTER TABLE krithi_notation_rows ALTER COLUMN id SET DEFAULT gen_random_uuid();
ALTER TABLE import_batch ALTER COLUMN id SET DEFAULT gen_random_uuid();
ALTER TABLE import_job ALTER COLUMN id SET DEFAULT gen_random_uuid();
ALTER TABLE import_task_run ALTER COLUMN id SET DEFAULT gen_random_uuid();
ALTER TABLE import_event ALTER COLUMN id SET DEFAULT gen_random_uuid();
ALTER TABLE entity_resolution_cache ALTER COLUMN id SET DEFAULT gen_random_uuid();
ALTER TABLE temple_source_cache ALTER COLUMN id SET DEFAULT gen_random_uuid();
ALTER TABLE krithi_source_evidence ALTER COLUMN id SET DEFAULT gen_random_uuid();
ALTER TABLE structural_vote_log ALTER COLUMN id SET DEFAULT gen_random_uuid();
ALTER TABLE extraction_queue ALTER COLUMN id SET DEFAULT gen_random_uuid();
ALTER TABLE variant_match ALTER COLUMN id SET DEFAULT gen_random_uuid();
ALTER TABLE audit_log ALTER COLUMN id SET DEFAULT gen_random_uuid();
