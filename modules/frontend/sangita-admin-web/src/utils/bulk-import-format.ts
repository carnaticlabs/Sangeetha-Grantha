import { BulkBatchStatus, BulkTaskStatus } from '../types';

/**
 * Presentation maps + formatters for the bulk-import screens.
 * Extracted from pages/BulkImport.tsx (TRACK-118 Step 2) so they can be
 * unit-tested and shared without rendering the page.
 */

export const statusChip: Record<BulkBatchStatus, string> = {
  PENDING: 'bg-slate-100 text-slate-700 border border-slate-200',
  RUNNING: 'bg-blue-50 text-blue-700 border border-blue-200',
  PAUSED: 'bg-amber-50 text-amber-700 border border-amber-200',
  SUCCEEDED: 'bg-green-50 text-green-700 border border-green-200',
  FAILED: 'bg-rose-50 text-rose-700 border border-rose-200',
  CANCELLED: 'bg-slate-100 text-slate-500 border border-slate-200'
};

export const taskChip: Record<BulkTaskStatus, string> = {
  PENDING: 'bg-slate-100 text-slate-700 border border-slate-200',
  RUNNING: 'bg-blue-50 text-blue-700 border border-blue-200',
  SUCCEEDED: 'bg-green-50 text-green-700 border border-green-200',
  FAILED: 'bg-rose-50 text-rose-700 border border-rose-200',
  RETRYABLE: 'bg-amber-50 text-amber-700 border border-amber-200',
  BLOCKED: 'bg-purple-50 text-purple-700 border border-purple-200',
  CANCELLED: 'bg-slate-100 text-slate-500 border border-slate-200'
};

export const batchStatusLabel: Record<BulkBatchStatus, string> = {
  PENDING: 'Waiting',
  RUNNING: 'In Progress',
  PAUSED: 'Paused',
  SUCCEEDED: 'Completed',
  FAILED: 'Failed',
  CANCELLED: 'Cancelled'
};

export const taskStatusLabel: Record<BulkTaskStatus, string> = {
  PENDING: 'Waiting',
  RUNNING: 'In Progress',
  SUCCEEDED: 'Completed',
  FAILED: 'Failed',
  RETRYABLE: 'Will Retry',
  BLOCKED: 'Blocked',
  CANCELLED: 'Cancelled'
};

export const jobTypeLabel: Record<string, string> = {
  MANIFEST_INGEST: 'Reading File',
  SCRAPE: 'Fetching Content',
  ENTITY_RESOLUTION: 'Matching',
};

export const eventTypeLabel: Record<string, string> = {
  BATCH_CREATED: 'Import created',
  BATCH_STARTED: 'Import started',
  BATCH_COMPLETED: 'Import completed',
  BATCH_FAILED: 'Import failed',
  BATCH_CANCELLED: 'Import cancelled',
  BATCH_PAUSED: 'Import paused',
  BATCH_RESUMED: 'Import resumed',
  MANIFEST_INGEST_STARTED: 'Reading file started',
  MANIFEST_INGEST_SUCCEEDED: 'File read successfully',
  MANIFEST_INGEST_FAILED: 'File reading failed',
  SCRAPE_STARTED: 'Fetching content started',
  SCRAPE_SUCCEEDED: 'Content fetched successfully',
  SCRAPE_FAILED: 'Content fetching failed',
  ENTITY_RESOLUTION_STARTED: 'Matching started',
  ENTITY_RESOLUTION_SUCCEEDED: 'Matching completed',
  ENTITY_RESOLUTION_FAILED: 'Matching failed',
};

export const formatDuration = (ms?: number | null): string => {
  if (ms == null) return '—';
  if (ms < 1000) return `${ms}ms`;
  return `${(ms / 1000).toFixed(1)}s`;
};

export const formatDate = (value?: string | null) => (value ? new Date(value).toLocaleString() : '—');

/** Show filename only (no full path) for manifest display. */
export const basename = (path: string) => (path ? path.replace(/^.*[/\\]/, '') : path);

export const parseError = (value?: string | null) => {
  if (!value) return '';
  try {
    const parsed = JSON.parse(value) as { message?: string; code?: string };
    return parsed.message || parsed.code || value;
  } catch {
    return value;
  }
};
