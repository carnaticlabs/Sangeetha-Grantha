-- 10__bulk-import-orchestration.sql
-- Purpose: Tables for orchestrating bulk import of Krithis from CSV manifests
-- Supports batch/job/task workflow with progress tracking, retries, and event logging

-- migrate:up
SET search_path TO public;

-- Enum types for batch and task statuses
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'batch_status_enum') THEN
    CREATE TYPE batch_status_enum AS ENUM (
      'pending',    -- Batch created, not started
      'running',    -- Batch is actively processing
      'paused',     -- Batch paused by admin
      'succeeded',  -- All tasks completed successfully
      'failed',     -- All tasks failed
      'cancelled'   -- Batch cancelled by admin
    );
  END IF;
END$$;

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'job_type_enum') THEN
    CREATE TYPE job_type_enum AS ENUM (
      'manifest_ingest',    -- Parse CSV and create tasks
      'scrape',             -- Scrape URL and extract metadata
      'enrich',             -- Enrich metadata (future)
      'entity_resolution',  -- Resolve entities (composer, raga, etc.)
      'review_prep'         -- Prepare for review workflow
    );
  END IF;
END$$;

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'task_status_enum') THEN
    CREATE TYPE task_status_enum AS ENUM (
      'pending',     -- Task queued, not started
      'running',     -- Task currently processing
      'succeeded',   -- Task completed successfully
      'failed',      -- Task failed (retryable)
      'retryable',   -- Task marked for retry (stuck detection)
      'blocked',     -- Task blocked (low confidence, manual review needed)
      'cancelled'    -- Task cancelled
    );
  END IF;
END$$;

-- import_batch: Top-level batch tracking
CREATE TABLE IF NOT EXISTS import_batch (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  source_manifest TEXT NOT NULL,              -- CSV file path or identifier
  created_by_user_id UUID REFERENCES users (id) ON DELETE SET NULL,
  status batch_status_enum NOT NULL DEFAULT 'pending',
  
  -- Statistics
  total_tasks INTEGER NOT NULL DEFAULT 0,
  processed_tasks INTEGER NOT NULL DEFAULT 0,
  succeeded_tasks INTEGER NOT NULL DEFAULT 0,
  failed_tasks INTEGER NOT NULL DEFAULT 0,
  blocked_tasks INTEGER NOT NULL DEFAULT 0,
  
  -- Timestamps
  started_at TIMESTAMPTZ,
  completed_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('UTC', now()),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT timezone('UTC', now())
);

CREATE INDEX IF NOT EXISTS idx_import_batch_status ON import_batch (status);
CREATE INDEX IF NOT EXISTS idx_import_batch_created_at ON import_batch (created_at DESC);

-- import_job: Job-level tracking within a batch
CREATE TABLE IF NOT EXISTS import_job (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  batch_id UUID NOT NULL REFERENCES import_batch (id) ON DELETE CASCADE,
  job_type job_type_enum NOT NULL,
  status task_status_enum NOT NULL DEFAULT 'pending',
  retry_count INTEGER NOT NULL DEFAULT 0,
  
  -- Job payload and result (JSONB for flexibility)
  payload JSONB,
  result JSONB,
  
  -- Timestamps
  started_at TIMESTAMPTZ,
  completed_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('UTC', now()),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT timezone('UTC', now())
);

CREATE INDEX IF NOT EXISTS idx_import_job_batch_id ON import_job (batch_id);
CREATE INDEX IF NOT EXISTS idx_import_job_status ON import_job (status);
CREATE INDEX IF NOT EXISTS idx_import_job_type_status ON import_job (job_type, status);

-- import_task_run: Individual task execution tracking
CREATE TABLE IF NOT EXISTS import_task_run (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  job_id UUID NOT NULL REFERENCES import_job (id) ON DELETE CASCADE,
  krithi_key TEXT,                           -- Unique key for deduplication (batch_id + source_url)
  status task_status_enum NOT NULL DEFAULT 'pending',
  attempt INTEGER NOT NULL DEFAULT 0,
  
  -- Task execution details
  source_url TEXT,                           -- URL to scrape
  error JSONB,                               -- Error details (code, message, http_status, stack)
  duration_ms INTEGER,                       -- Task execution duration
  checksum TEXT,                             -- Content checksum for deduplication
  evidence_path TEXT,                        -- Path to scraped content evidence
  
  -- Timestamps
  started_at TIMESTAMPTZ,
  completed_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('UTC', now()),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT timezone('UTC', now())
);

CREATE INDEX IF NOT EXISTS idx_import_task_run_job_id ON import_task_run (job_id);
CREATE INDEX IF NOT EXISTS idx_import_task_run_status ON import_task_run (status);
CREATE INDEX IF NOT EXISTS idx_import_task_run_krithi_key ON import_task_run (krithi_key);
CREATE INDEX IF NOT EXISTS idx_import_task_run_source_url ON import_task_run (source_url);
-- Index for task queue polling (pending tasks by job_type via job)
CREATE INDEX IF NOT EXISTS idx_import_task_run_pending ON import_task_run (status, created_at) 
  WHERE status = 'pending';

-- import_event: Immutable event log for audit trail
CREATE TABLE IF NOT EXISTS import_event (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  ref_type TEXT NOT NULL,                    -- 'batch', 'job', or 'task'
  ref_id UUID NOT NULL,                      -- ID of batch/job/task
  event_type TEXT NOT NULL,                  -- e.g., 'status_changed', 'retry_triggered', 'error_occurred'
  data JSONB,                                 -- Event-specific data
  created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('UTC', now())
);

CREATE INDEX IF NOT EXISTS idx_import_event_ref ON import_event (ref_type, ref_id);
CREATE INDEX IF NOT EXISTS idx_import_event_created_at ON import_event (created_at DESC);

-- migrate:down
-- DROP INDEX IF EXISTS idx_import_event_created_at;
-- DROP INDEX IF EXISTS idx_import_event_ref;
-- DROP TABLE IF EXISTS import_event;
-- DROP INDEX IF EXISTS idx_import_task_run_pending;
-- DROP INDEX IF EXISTS idx_import_task_run_source_url;
-- DROP INDEX IF EXISTS idx_import_task_run_krithi_key;
-- DROP INDEX IF EXISTS idx_import_task_run_status;
-- DROP INDEX IF EXISTS idx_import_task_run_job_id;
-- DROP TABLE IF EXISTS import_task_run;
-- DROP INDEX IF EXISTS idx_import_job_type_status;
-- DROP INDEX IF EXISTS idx_import_job_status;
-- DROP INDEX IF EXISTS idx_import_job_batch_id;
-- DROP TABLE IF EXISTS import_job;
-- DROP INDEX IF EXISTS idx_import_batch_created_at;
-- DROP INDEX IF EXISTS idx_import_batch_status;
-- DROP TABLE IF EXISTS import_batch;
-- DROP TYPE IF EXISTS task_status_enum;
-- DROP TYPE IF EXISTS job_type_enum;
-- DROP TYPE IF EXISTS batch_status_enum;
