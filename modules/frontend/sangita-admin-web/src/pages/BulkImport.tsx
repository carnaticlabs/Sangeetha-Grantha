import React, { useEffect, useMemo, useState } from 'react';
import {
  BulkBatchStatus,
  BulkTaskStatus,
  ImportBatch,
  ImportEvent,
  ImportJob,
  ImportTaskRun
} from '../types';
import {
  cancelBulkImportBatch,
  createBulkImportBatch,
  getBulkImportBatch,
  getBulkImportEvents,
  getBulkImportJobs,
  getBulkImportTasks,
  listBulkImportBatches,
  pauseBulkImportBatch,
  resumeBulkImportBatch,
  retryBulkImportBatch,
  uploadBulkImportFile,
  deleteBulkImportBatch,
  approveAllInBulkImportBatch,
  rejectAllInBulkImportBatch,
  finalizeBulkImportBatch,
  exportBulkImportReport
} from '../api/client';
import { ToastContainer, useToast } from '../components/Toast';

const statusChip: Record<BulkBatchStatus, string> = {
  PENDING: 'bg-slate-100 text-slate-700 border border-slate-200',
  RUNNING: 'bg-blue-50 text-blue-700 border border-blue-200',
  PAUSED: 'bg-amber-50 text-amber-700 border border-amber-200',
  SUCCEEDED: 'bg-green-50 text-green-700 border border-green-200',
  FAILED: 'bg-rose-50 text-rose-700 border border-rose-200',
  CANCELLED: 'bg-slate-100 text-slate-500 border border-slate-200'
};

const taskChip: Record<BulkTaskStatus, string> = {
  PENDING: 'bg-slate-100 text-slate-700 border border-slate-200',
  RUNNING: 'bg-blue-50 text-blue-700 border border-blue-200',
  SUCCEEDED: 'bg-green-50 text-green-700 border border-green-200',
  FAILED: 'bg-rose-50 text-rose-700 border border-rose-200',
  RETRYABLE: 'bg-amber-50 text-amber-700 border border-amber-200',
  BLOCKED: 'bg-purple-50 text-purple-700 border border-purple-200',
  CANCELLED: 'bg-slate-100 text-slate-500 border border-slate-200'
};

const formatDate = (value?: string | null) => (value ? new Date(value).toLocaleString() : '—');

const parseError = (value?: string | null) => {
  if (!value) return '';
  try {
    const parsed = JSON.parse(value) as { message?: string; code?: string };
    return parsed.message || parsed.code || value;
  } catch {
    return value;
  }
};

const BulkImportPage: React.FC = () => {
  const { toasts, removeToast, success, error } = useToast();
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [batches, setBatches] = useState<ImportBatch[]>([]);
  const [selectedBatchId, setSelectedBatchId] = useState<string | null>(null);
  const [selectedBatch, setSelectedBatch] = useState<ImportBatch | null>(null);
  const [jobs, setJobs] = useState<ImportJob[]>([]);
  const [tasks, setTasks] = useState<ImportTaskRun[]>([]);
  const [events, setEvents] = useState<ImportEvent[]>([]);
  const [taskStatusFilter, setTaskStatusFilter] = useState<BulkTaskStatus | 'ALL'>('ALL');
  const [loadingList, setLoadingList] = useState<boolean>(true);
  const [loadingDetail, setLoadingDetail] = useState<boolean>(false);
  const [creating, setCreating] = useState<boolean>(false);
  const [deleteConfirmBatchId, setDeleteConfirmBatchId] = useState<string | null>(null);
  const [selectedTaskForLog, setSelectedTaskForLog] = useState<ImportTaskRun | null>(null);

  const selectedProgress = useMemo(() => {
    if (!selectedBatch || selectedBatch.totalTasks === 0) return 0;
    return Math.round((selectedBatch.processedTasks / selectedBatch.totalTasks) * 100);
  }, [selectedBatch]);

  const currentStage = useMemo(() => {
    if (!jobs.length) return 'Initializing';
    if (jobs.some(j => j.jobType === 'ENTITY_RESOLUTION' && j.status === 'RUNNING')) return 'Resolving Entities';
    if (jobs.some(j => j.jobType === 'SCRAPE' && j.status === 'RUNNING')) return 'Scraping Content';
    if (jobs.some(j => j.jobType === 'MANIFEST_INGEST' && j.status === 'RUNNING')) return 'Analyzing Manifest';
    if (selectedBatch?.status === 'SUCCEEDED') return 'Completed';
    if (selectedBatch?.status === 'FAILED') return 'Failed';
    return 'Processing';
  }, [jobs, selectedBatch]);

  // Polling for active batches
  useEffect(() => {
    let interval: NodeJS.Timeout;
    const isRunning = selectedBatch?.status === 'RUNNING' || selectedBatch?.status === 'PENDING';
    
    if (isRunning && selectedBatchId) {
        interval = setInterval(() => {
            void loadBatchDetail(selectedBatchId);
            void refreshBatches();
        }, 2000);
    }
    return () => clearInterval(interval);
  }, [selectedBatch?.status, selectedBatchId]);

  const refreshBatches = async () => {
    // Silent refresh if already have data, otherwise show loader (handled by caller or initial state)
    try {
      const items = await listBulkImportBatches(undefined, 25, 0);
      setBatches(items);
      // Don't auto-select if we have one selected
    } catch (e) {
      console.error('Failed to load batches', e);
    } finally {
      setLoadingList(false);
    }
  };
  
  // Initial load
  useEffect(() => {
    setLoadingList(true);
    refreshBatches().then(() => {
        // Auto-select first if none selected
        setBatches(prev => {
            if (prev.length > 0 && !selectedBatchId) {
                setSelectedBatchId(prev[0].id);
            }
            return prev;
        });
    });
  }, []);

  useEffect(() => {
    if (selectedBatchId) {
      void loadBatchDetail(selectedBatchId);
    } else {
      setSelectedBatch(null);
      setJobs([]);
      setTasks([]);
      setEvents([]);
    }
  }, [selectedBatchId, taskStatusFilter]);

  const loadBatchDetail = async (batchId: string) => {
    // Don't set loadingDetail on every poll to avoid UI flicker
    try {
      const [batch, jobsResponse, tasksResponse, eventsResponse] = await Promise.all([
        getBulkImportBatch(batchId),
        getBulkImportJobs(batchId),
        getBulkImportTasks(batchId, taskStatusFilter === 'ALL' ? undefined : taskStatusFilter, 200),
        getBulkImportEvents(batchId, 100)
      ]);
      setSelectedBatch(batch);
      setJobs(jobsResponse);
      setTasks(tasksResponse);
      setEvents(eventsResponse);
    } catch (e) {
      console.error('Failed to load batch detail', e);
    }
  };

  const handleCreate = async () => {
    if (!selectedFile) {
      error('Select a CSV file first');
      return;
    }
    setCreating(true);
    try {
      const created = await uploadBulkImportFile(selectedFile);
      success('Batch created from upload');
      await refreshBatches();
      setSelectedBatchId(created.id);
      setSelectedFile(null); // Reset input
    } catch (e) {
      error('Failed to create batch: ' + (e as Error).message);
    } finally {
      setCreating(false);
    }
  };

  const triggerAction = async (action: 'pause' | 'resume' | 'cancel' | 'retry' | 'delete' | 'approveAll' | 'rejectAll' | 'finalize' | 'export', batchId: string) => {
    console.log(`triggerAction called: ${action} for batch ${batchId}`);
    try {
      if (action === 'delete') {
          setDeleteConfirmBatchId(batchId);
          return;
      }

      if (action === 'approveAll') {
          if (!window.confirm('Approve all pending items in this batch? This will create canonical krithis for all entries.')) return;
          await approveAllInBulkImportBatch(batchId);
      }
      if (action === 'rejectAll') {
          if (!window.confirm('Reject all pending items in this batch?')) return;
          await rejectAllInBulkImportBatch(batchId);
      }

      if (action === 'finalize') {
          if (!window.confirm('Finalize this batch? This will mark it as complete and generate a summary.')) return;
          const summary = await finalizeBulkImportBatch(batchId);
          success(`Batch finalized: ${summary.approved} approved, ${summary.rejected} rejected, ${summary.pending} pending`);
          await refreshBatches();
          await loadBatchDetail(batchId);
          return;
      }

      if (action === 'export') {
          const format = window.confirm('Export as CSV? (Cancel for JSON)') ? 'csv' : 'json';
          const blob = await exportBulkImportReport(batchId, format);
          const url = window.URL.createObjectURL(blob);
          const a = document.createElement('a');
          a.href = url;
          a.download = `batch-${batchId}-report.${format}`;
          document.body.appendChild(a);
          a.click();
          window.URL.revokeObjectURL(url);
          document.body.removeChild(a);
          success(`Report exported as ${format.toUpperCase()}`);
          return;
      }

      if (action === 'pause') await pauseBulkImportBatch(batchId);
      if (action === 'resume') await resumeBulkImportBatch(batchId);
      if (action === 'cancel') await cancelBulkImportBatch(batchId);
      if (action === 'retry') await retryBulkImportBatch(batchId, true);

      success(`Batch ${action} requested`);
      await refreshBatches();
      await loadBatchDetail(batchId);
    } catch (e) {
      error(`Failed to ${action} batch: ${(e as Error).message}`);
    }
  };

  const executeDelete = async () => {
      if (!deleteConfirmBatchId) return;
      try {
          await deleteBulkImportBatch(deleteConfirmBatchId);
          success('Batch delete requested');
          await refreshBatches();
          if (selectedBatchId === deleteConfirmBatchId) {
              setSelectedBatchId(null);
          }
      } catch(e) {
          error('Failed to delete batch');
      } finally {
          setDeleteConfirmBatchId(null);
      }
  };

  const filteredTasks = useMemo(
    () => (taskStatusFilter === 'ALL' ? tasks : tasks.filter(t => t.status === taskStatusFilter)),
    [taskStatusFilter, tasks]
  );

  return (
    <div className="max-w-7xl mx-auto space-y-6">
      <ToastContainer toasts={toasts} onRemove={removeToast} />

      {/* Log Viewer Drawer */}
      {selectedTaskForLog && (
        <div className="fixed inset-0 bg-black/50 flex items-end justify-end z-50" onClick={() => setSelectedTaskForLog(null)}>
          <div
            className="bg-white h-full w-full md:w-2/3 lg:w-1/2 shadow-2xl overflow-hidden flex flex-col animate-slide-in-right"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="px-6 py-4 border-b border-border-light bg-slate-50 flex justify-between items-center">
              <div>
                <h3 className="text-lg font-bold text-ink-900">Task Log Viewer</h3>
                <p className="text-xs text-ink-500 truncate max-w-md">{selectedTaskForLog.sourceUrl || selectedTaskForLog.krithiKey}</p>
              </div>
              <button
                onClick={() => setSelectedTaskForLog(null)}
                className="text-ink-400 hover:text-ink-600"
              >
                <span className="material-symbols-outlined">close</span>
              </button>
            </div>

            <div className="flex-1 overflow-y-auto p-6 space-y-6">
              {/* Task Overview */}
              <div className="bg-slate-50 rounded-lg p-4 space-y-3">
                <h4 className="text-sm font-bold text-ink-900">Task Overview</h4>
                <div className="grid grid-cols-2 gap-4 text-xs">
                  <div>
                    <div className="text-ink-500 font-semibold mb-1">Task ID</div>
                    <div className="font-mono text-[10px] text-ink-700">{selectedTaskForLog.id}</div>
                  </div>
                  <div>
                    <div className="text-ink-500 font-semibold mb-1">Status</div>
                    <span className={`px-2 py-1 rounded-full text-[10px] font-bold ${taskChip[selectedTaskForLog.status]}`}>
                      {selectedTaskForLog.status}
                    </span>
                  </div>
                  <div>
                    <div className="text-ink-500 font-semibold mb-1">Attempt</div>
                    <div className="text-ink-700">{selectedTaskForLog.attempt}</div>
                  </div>
                  <div>
                    <div className="text-ink-500 font-semibold mb-1">Duration</div>
                    <div className="text-ink-700">{selectedTaskForLog.durationMs ?? '—'}ms</div>
                  </div>
                  <div className="col-span-2">
                    <div className="text-ink-500 font-semibold mb-1">Source URL</div>
                    <a
                      href={selectedTaskForLog.sourceUrl || '#'}
                      target="_blank"
                      rel="noreferrer"
                      className="text-primary hover:underline text-[11px] break-all"
                    >
                      {selectedTaskForLog.sourceUrl || 'N/A'}
                    </a>
                  </div>
                </div>
              </div>

              {/* Error Details */}
              {selectedTaskForLog.error && (
                <div className="bg-rose-50 border border-rose-200 rounded-lg p-4 space-y-2">
                  <h4 className="text-sm font-bold text-rose-900 flex items-center gap-2">
                    <span className="material-symbols-outlined text-base">error</span>
                    Error Details
                  </h4>
                  <pre className="text-xs font-mono text-rose-700 whitespace-pre-wrap break-words bg-white p-3 rounded border border-rose-100 overflow-x-auto">
                    {selectedTaskForLog.error}
                  </pre>
                </div>
              )}

              {/* Task Payload (if available) */}
              {selectedTaskForLog.krithiKey && (
                <div className="border border-border-light rounded-lg p-4 space-y-2">
                  <h4 className="text-sm font-bold text-ink-900">Task Data</h4>
                  <div className="text-xs space-y-1">
                    <div className="flex justify-between">
                      <span className="text-ink-500 font-semibold">Krithi Key:</span>
                      <span className="text-ink-700 font-mono">{selectedTaskForLog.krithiKey}</span>
                    </div>
                  </div>
                </div>
              )}

              {/* Actions */}
              <div className="flex gap-3">
                <button
                  onClick={() => {
                    navigator.clipboard.writeText(JSON.stringify(selectedTaskForLog, null, 2));
                    success('Task data copied to clipboard');
                  }}
                  className="flex-1 px-4 py-2 text-sm font-medium text-primary border border-primary rounded-lg hover:bg-primary-light"
                >
                  Copy Task JSON
                </button>
                <button
                  onClick={() => setSelectedTaskForLog(null)}
                  className="px-4 py-2 text-sm font-medium text-ink-600 border border-border-light rounded-lg hover:bg-slate-50"
                >
                  Close
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      {deleteConfirmBatchId && (
          <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
              <div className="bg-white p-6 rounded-lg shadow-lg max-w-sm w-full mx-4 border border-border-light">
                  <h3 className="text-lg font-bold text-ink-900 mb-2">Delete Batch?</h3>
                  <p className="text-sm text-ink-600 mb-6">This action cannot be undone. All associated jobs, tasks, and events will be permanently removed.</p>
                  <div className="flex justify-end gap-3">
                      <button 
                          onClick={() => setDeleteConfirmBatchId(null)}
                          className="px-4 py-2 text-sm font-medium text-ink-600 hover:bg-slate-100 rounded-md border border-border-light"
                      >
                          Cancel
                      </button>
                      <button 
                          onClick={executeDelete}
                          className="px-4 py-2 text-sm font-medium text-white bg-rose-600 hover:bg-rose-700 rounded-md shadow-sm"
                      >
                          Delete
                      </button>
                  </div>
              </div>
          </div>
      )}
      <div className="flex flex-col gap-1">
        <h1 className="text-2xl font-display font-bold text-ink-900">Bulk Import Orchestration</h1>
        <p className="text-sm text-ink-500">
          Manage CSV-driven bulk imports with progress tracking, retries, and task drill-down.
        </p>
      </div>

      <div className="bg-white border border-border-light rounded-xl shadow-sm p-4 md:p-6 flex flex-col gap-4">
        <div className="flex flex-col md:flex-row md:items-end gap-3">
          <div className="flex-1">
            <label className="text-xs font-semibold text-ink-600 mb-1 block">Upload CSV Manifest</label>
            <input
              type="file"
              accept=".csv"
              onChange={e => setSelectedFile(e.target.files?.[0] || null)}
              className="w-full px-3 py-2 rounded-lg border border-border-light text-sm file:mr-4 file:py-2 file:px-4 file:rounded-full file:border-0 file:text-xs file:font-semibold file:bg-primary-light file:text-primary hover:file:bg-primary-light/80"
            />
            <p className="text-[11px] text-ink-400 mt-1">
              Select a CSV file with columns: Krithi, Raga, Hyperlink.
            </p>
          </div>
          <button
            onClick={handleCreate}
            disabled={creating || !selectedFile}
            className="px-4 py-2.5 bg-primary text-white rounded-lg font-medium shadow-sm hover:bg-primary-dark disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {creating ? 'Uploading…' : 'Start Import'}
          </button>
        </div>
      </div>

      <div className="grid grid-cols-1 xl:grid-cols-3 gap-6">
        <div className="xl:col-span-2 bg-white border border-border-light rounded-xl shadow-sm">
          <div className="flex items-center justify-between px-4 py-3 border-b border-border-light">
            <div>
              <h3 className="text-sm font-bold text-ink-900">Batches</h3>
              <p className="text-xs text-ink-500">Recent batch runs with progress and status.</p>
            </div>
            <button
              onClick={() => refreshBatches()}
              className="text-sm text-primary font-semibold hover:text-primary-dark flex items-center gap-1"
            >
              <span className="material-symbols-outlined text-base">refresh</span>
              Refresh
            </button>
          </div>
          {loadingList ? (
            <div className="py-10 text-center text-ink-400">Loading batches…</div>
          ) : batches.length === 0 ? (
            <div className="py-10 text-center text-ink-400">No batches found.</div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead className="bg-slate-50 text-ink-500 border-b border-border-light">
                  <tr>
                    <th className="px-4 py-3 text-left">Manifest</th>
                    <th className="px-4 py-3 text-left">Status</th>
                    <th className="px-4 py-3 text-left">Progress</th>
                    <th className="px-4 py-3 text-left">Created</th>
                    <th className="px-4 py-3 text-right">Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {batches.map(batch => {
                    const progress =
                      batch.totalTasks > 0 ? Math.round((batch.processedTasks / batch.totalTasks) * 100) : 0;
                    return (
                      <tr
                        key={batch.id}
                        className={`border-b border-border-light hover:bg-slate-50 cursor-pointer ${
                          batch.id === selectedBatchId ? 'bg-slate-50' : ''
                        }`}
                        onClick={() => setSelectedBatchId(batch.id)}
                      >
                        <td className="px-4 py-3 max-w-[260px]">
                          <div className="font-semibold text-ink-900 truncate" title={batch.sourceManifest}>
                            {batch.sourceManifest}
                          </div>
                          <div className="text-[11px] text-ink-500">#{batch.id.slice(0, 8)}</div>
                        </td>
                        <td className="px-4 py-3">
                          <span className={`px-2 py-1 rounded-full text-[11px] font-semibold ${statusChip[batch.status]}`}>
                            {batch.status}
                          </span>
                        </td>
                        <td className="px-4 py-3">
                          <div className="flex items-center gap-2">
                            <div className="h-2 w-28 bg-slate-100 rounded-full overflow-hidden">
                              <div className="h-full bg-primary rounded-full" style={{ width: `${progress}%` }}></div>
                            </div>
                            <span className="text-xs text-ink-600 tabular-nums">
                              {batch.processedTasks}/{batch.totalTasks || 0}
                            </span>
                          </div>
                        </td>
                        <td className="px-4 py-3 text-xs text-ink-500">{formatDate(batch.createdAt)}</td>
                        <td className="px-4 py-3 text-right">
                          <div className="flex items-center justify-end gap-2">
                            <button
                              onClick={e => {
                                e.stopPropagation();
                                triggerAction('retry', batch.id);
                              }}
                              className="px-2 py-1 text-xs rounded-md border border-border-light hover:border-primary text-primary font-semibold"
                            >
                              Retry
                            </button>
                            {batch.status === 'RUNNING' ? (
                              <button
                                onClick={e => {
                                  e.stopPropagation();
                                  triggerAction('pause', batch.id);
                                }}
                                className="px-2 py-1 text-xs rounded-md border border-amber-200 text-amber-700 bg-amber-50"
                              >
                                Pause
                              </button>
                            ) : (
                              <button
                                onClick={e => {
                                  e.stopPropagation();
                                  triggerAction('resume', batch.id);
                                }}
                                className="px-2 py-1 text-xs rounded-md border border-primary text-primary bg-primary-light"
                              >
                                Resume
                              </button>
                            )}
                            <button
                              onClick={e => {
                                e.stopPropagation();
                                triggerAction('delete', batch.id);
                              }}
                              className="px-2 py-1 text-xs rounded-md border border-slate-200 text-slate-700 bg-slate-50 hover:bg-slate-100"
                            >
                              Delete
                            </button>
                          </div>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          )}
        </div>

        <div className="bg-white border border-border-light rounded-xl shadow-sm p-4 space-y-4">
          {selectedBatch ? (
            <>
              <div className="flex items-center justify-between gap-2">
                <div>
                  <h3 className="text-sm font-bold text-ink-900">Batch Detail</h3>
                  <p className="text-xs text-ink-500 truncate">{selectedBatch.sourceManifest}</p>
                </div>
                <div className="flex flex-wrap gap-2">
                  <button
                    onClick={() => triggerAction('approveAll', selectedBatch.id)}
                    className="px-2.5 py-1.5 text-xs rounded-md border border-green-200 text-green-700 bg-green-50 hover:bg-green-100 font-bold"
                  >
                    Approve All
                  </button>
                  <button
                    onClick={() => triggerAction('rejectAll', selectedBatch.id)}
                    className="px-2.5 py-1.5 text-xs rounded-md border border-amber-200 text-amber-700 bg-amber-50 hover:bg-amber-100"
                  >
                    Reject All
                  </button>
                  <button
                    onClick={() => triggerAction('retry', selectedBatch.id)}
                    className="px-2.5 py-1.5 text-xs rounded-md border border-border-light text-primary font-semibold hover:border-primary"
                  >
                    Retry Failed
                  </button>
                  <button
                    onClick={() => triggerAction('cancel', selectedBatch.id)}
                    className="px-2.5 py-1.5 text-xs rounded-md border border-rose-200 text-rose-700 bg-rose-50"
                  >
                    Cancel
                  </button>
                  <button
                    onClick={() => triggerAction('delete', selectedBatch.id)}
                    className="px-2.5 py-1.5 text-xs rounded-md border border-slate-200 text-slate-700 bg-slate-50 hover:bg-slate-100"
                  >
                    Delete
                  </button>
                </div>
              </div>

              {/* TRACK-004: Finalize and Export buttons */}
              {selectedBatch.status === 'SUCCEEDED' && (
                <div className="flex gap-2 mt-2">
                  <button
                    onClick={() => triggerAction('finalize', selectedBatch.id)}
                    className="flex-1 px-3 py-2 text-xs rounded-md border border-purple-200 text-purple-700 bg-purple-50 hover:bg-purple-100 font-semibold"
                  >
                    📋 Finalize Batch
                  </button>
                  <button
                    onClick={() => triggerAction('export', selectedBatch.id)}
                    className="flex-1 px-3 py-2 text-xs rounded-md border border-indigo-200 text-indigo-700 bg-indigo-50 hover:bg-indigo-100 font-semibold"
                  >
                    📊 Export Report
                  </button>
                </div>
              )}

              <div className="grid grid-cols-2 gap-3 text-xs">
                <div className="p-3 rounded-lg bg-slate-50">
                  <div className="text-ink-500 font-semibold">Status</div>
                  <div className="mt-1 flex items-center gap-2">
                    <span className={`px-2 py-1 rounded-full text-[11px] font-semibold ${statusChip[selectedBatch.status]}`}>
                      {selectedBatch.status}
                    </span>
                    {selectedBatch.status === 'RUNNING' && (
                        <span className="text-xs text-ink-600 font-medium border border-border-light bg-white px-2 py-0.5 rounded-full">
                            {currentStage}
                        </span>
                    )}
                  </div>
                </div>
                <div className="p-3 rounded-lg bg-slate-50">
                  <div className="text-ink-500 font-semibold">Progress</div>
                  <div className="flex items-center gap-2 mt-1">
                    <div className="h-2 w-full bg-slate-200 rounded-full overflow-hidden">
                      <div className="h-full bg-primary" style={{ width: `${selectedProgress}%` }}></div>
                    </div>
                    <span className="text-ink-700 font-semibold tabular-nums">{selectedProgress}%</span>
                  </div>
                </div>
                <div className="p-3 rounded-lg bg-slate-50">
                  <div className="text-ink-500 font-semibold">Started</div>
                  <div className="text-ink-800 mt-1">{formatDate(selectedBatch.startedAt)}</div>
                </div>
                <div className="p-3 rounded-lg bg-slate-50">
                  <div className="text-ink-500 font-semibold">Completed</div>
                  <div className="text-ink-800 mt-1">{formatDate(selectedBatch.completedAt)}</div>
                </div>
              </div>

              {jobs.length > 0 && (
                <>
                  {/* Job Pipeline Stepper Visualization */}
                  <div className="border border-border-light rounded-lg p-4 bg-slate-50">
                    <div className="text-sm font-semibold text-ink-800 mb-3">Pipeline Progress</div>
                    <div className="relative flex items-center justify-between">
                      {['MANIFEST_INGEST', 'SCRAPE', 'ENTITY_RESOLUTION'].map((stage, index) => {
                        const job = jobs.find(j => j.jobType === stage);
                        const isActive = job?.status === 'RUNNING';
                        const isCompleted = job?.status === 'SUCCEEDED';
                        const isFailed = job?.status === 'FAILED';

                        return (
                          <React.Fragment key={stage}>
                            {index > 0 && (
                              <div className={`flex-1 h-0.5 ${
                                isCompleted || (index < jobs.findIndex(j => j.status === 'RUNNING'))
                                  ? 'bg-primary'
                                  : 'bg-slate-300'
                              }`}></div>
                            )}
                            <div className="flex flex-col items-center gap-1.5">
                              <div className={`w-10 h-10 rounded-full flex items-center justify-center text-xs font-bold border-2 ${
                                isActive
                                  ? 'bg-blue-50 border-blue-500 text-blue-700 animate-pulse'
                                  : isCompleted
                                    ? 'bg-green-50 border-green-500 text-green-700'
                                    : isFailed
                                      ? 'bg-rose-50 border-rose-500 text-rose-700'
                                      : 'bg-slate-100 border-slate-300 text-slate-500'
                              }`}>
                                {isCompleted ? '✓' : isFailed ? '✗' : index + 1}
                              </div>
                              <div className="text-[10px] font-semibold text-center max-w-[80px] leading-tight">
                                {stage === 'MANIFEST_INGEST' ? 'Ingest' : stage === 'SCRAPE' ? 'Scrape' : 'Resolve'}
                              </div>
                              {job && (
                                <span className={`px-1.5 py-0.5 rounded text-[8px] font-bold ${taskChip[job.status]}`}>
                                  {job.status}
                                </span>
                              )}
                            </div>
                          </React.Fragment>
                        );
                      })}
                    </div>
                  </div>

                  {/* Job Details List */}
                  <div className="border border-border-light rounded-lg">
                    <div className="px-3 py-2 border-b border-border-light text-sm font-semibold text-ink-800">Job Details</div>
                    <div className="divide-y divide-border-light max-h-40 overflow-y-auto">
                      {jobs.map(job => (
                        <div key={job.id} className="px-3 py-2 flex items-center justify-between text-xs">
                          <span className="font-semibold text-ink-900">{job.jobType}</span>
                          <span className={`px-2 py-1 rounded-full text-[10px] font-bold ${taskChip[job.status]}`}>
                            {job.status}
                          </span>
                        </div>
                      ))}
                    </div>
                  </div>
                </>
              )}

              <div className="border border-border-light rounded-lg">
                <div className="flex items-center justify-between px-3 py-2 border-b border-border-light">
                  <div className="text-sm font-semibold text-ink-800">Tasks</div>
                  <select
                    value={taskStatusFilter}
                    onChange={e => setTaskStatusFilter(e.target.value as BulkTaskStatus | 'ALL')}
                    className="text-xs border border-border-light rounded-md px-2 py-1"
                  >
                    <option value="ALL">All</option>
                    {Object.keys(taskChip).map(key => (
                      <option key={key} value={key}>
                        {key}
                      </option>
                    ))}
                  </select>
                </div>
                {loadingDetail ? (
                  <div className="p-4 text-ink-400 text-sm">Loading tasks…</div>
                ) : filteredTasks.length === 0 ? (
                  <div className="p-4 text-ink-400 text-sm">No tasks found.</div>
                ) : (
                  <div className="max-h-64 overflow-y-auto divide-y divide-border-light">
                    {filteredTasks.slice(0, 50).map(task => (
                      <div key={task.id} className="px-3 py-2 hover:bg-slate-50 cursor-pointer" onClick={() => setSelectedTaskForLog(task)}>
                        <div className="flex items-center justify-between">
                          <div className="text-xs font-semibold text-ink-900 truncate">
                            {task.sourceUrl || task.krithiKey || 'Task'}
                          </div>
                          <div className="flex items-center gap-2">
                            <span className={`px-2 py-1 rounded-full text-[10px] font-bold ${taskChip[task.status]}`}>
                              {task.status}
                            </span>
                            <span className="material-symbols-outlined text-sm text-ink-400">chevron_right</span>
                          </div>
                        </div>
                        <div className="text-[11px] text-ink-500">
                          Attempt {task.attempt} · Duration {task.durationMs ?? '—'}ms
                        </div>
                        {task.error && (
                          <div className="text-[11px] text-rose-600 mt-1 truncate">{parseError(task.error)}</div>
                        )}
                      </div>
                    ))}
                  </div>
                )}
              </div>

              <div className="border border-border-light rounded-lg">
                <div className="px-3 py-2 border-b border-border-light text-sm font-semibold text-ink-800">Events</div>
                {loadingDetail ? (
                  <div className="p-4 text-ink-400 text-sm">Loading events…</div>
                ) : events.length === 0 ? (
                  <div className="p-4 text-ink-400 text-sm">No events yet.</div>
                ) : (
                  <div className="max-h-48 overflow-y-auto divide-y divide-border-light">
                    {events.slice(0, 30).map(event => (
                      <div key={event.id} className="px-3 py-2 text-xs">
                        <div className="flex items-center justify-between">
                          <span className="font-semibold text-ink-900">{event.eventType}</span>
                          <span className="text-ink-500">{formatDate(event.createdAt)}</span>
                        </div>
                        {event.data && <div className="text-ink-600 mt-1">{parseError(event.data)}</div>}
                      </div>
                    ))}
                  </div>
                )}
              </div>
            </>
          ) : (
            <div className="text-sm text-ink-500">Select a batch to view details.</div>
          )}
        </div>
      </div>
    </div>
  );
};

export default BulkImportPage;
