-- 13__optimize_polling_indices.sql
-- Purpose: Optimize indices for high-frequency polling queries by workers
-- Ref: TRACK-006

-- migrate:up
SET search_path TO public;

-- Optimize the join path: Job -> TaskRun
-- This allows efficient lookup of pending tasks for a specific job (which we resolve via the join)
CREATE INDEX IF NOT EXISTS idx_import_task_run_polling_optimization
    ON import_task_run (job_id, status, created_at);

-- migrate:down
-- DROP INDEX IF EXISTS idx_import_task_run_polling_optimization;
