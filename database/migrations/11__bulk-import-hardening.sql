-- 11__bulk-import-hardening.sql
-- Purpose: Hardening for bulk import orchestration (idempotency, watchdog, rate-limit support)

-- migrate:up
SET search_path TO public;

-- Idempotency for task runs
ALTER TABLE import_task_run
    ADD COLUMN IF NOT EXISTS idempotency_key TEXT;

-- Backfill idempotency keys using batch + source_url (or krithi_key)
UPDATE import_task_run itr
SET idempotency_key = CONCAT(
        job.batch_id::TEXT,
        '::',
        COALESCE(itr.source_url, itr.krithi_key, itr.id::TEXT)
    )
FROM import_job job
WHERE itr.job_id = job.id
  AND itr.idempotency_key IS NULL;

-- Enforce uniqueness
CREATE UNIQUE INDEX IF NOT EXISTS ux_import_task_run_idempotency_key
    ON import_task_run (idempotency_key);

-- Helper index for stuck task watchdog
CREATE INDEX IF NOT EXISTS idx_import_task_run_running_started_at
    ON import_task_run (status, started_at)
    WHERE status = 'running';

-- Idempotency for imported krithis (source scoped)
ALTER TABLE imported_krithis
    ADD CONSTRAINT ux_imported_krithis_source UNIQUE (import_source_id, source_key);

-- migrate:down
-- DROP INDEX IF EXISTS idx_import_task_run_running_started_at;
-- DROP INDEX IF EXISTS ux_import_task_run_idempotency_key;
-- ALTER TABLE import_task_run DROP COLUMN IF EXISTS idempotency_key;
-- ALTER TABLE imported_krithis DROP CONSTRAINT IF EXISTS ux_imported_krithis_source;
