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
  retryBulkImportBatch
} from '../api/client';
import { ToastContainer, useToast } from '../components/Toast';

const DEFAULT_MANIFEST_PATH =
  '/Users/seshadri/project/sangeetha-grantha/database/for_import/Dikshitar-Krithi-For-Import.csv';

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
  const [manifestPath, setManifestPath] = useState(DEFAULT_MANIFEST_PATH);
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

  const selectedProgress = useMemo(() => {
    if (!selectedBatch || selectedBatch.totalTasks === 0) return 0;
    return Math.round((selectedBatch.processedTasks / selectedBatch.totalTasks) * 100);
  }, [selectedBatch]);

  const refreshBatches = async () => {
    setLoadingList(true);
    try {
      const items = await listBulkImportBatches(undefined, 25, 0);
      setBatches(items);
      if (!selectedBatchId && items.length > 0) {
        setSelectedBatchId(items[0].id);
      }
    } catch (e) {
      error('Failed to load batches');
    } finally {
      setLoadingList(false);
    }
  };

  useEffect(() => {
    refreshBatches();
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
    setLoadingDetail(true);
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
      error('Failed to load batch detail');
    } finally {
      setLoadingDetail(false);
    }
  };

  const handleCreate = async () => {
    if (!manifestPath.trim()) {
      error('Provide a manifest path');
      return;
    }
    setCreating(true);
    try {
      const created = await createBulkImportBatch(manifestPath.trim());
      success('Batch created');
      await refreshBatches();
      setSelectedBatchId(created.id);
    } catch (e) {
      error('Failed to create batch');
    } finally {
      setCreating(false);
    }
  };

  const triggerAction = async (action: 'pause' | 'resume' | 'cancel' | 'retry', batchId: string) => {
    try {
      if (action === 'pause') await pauseBulkImportBatch(batchId);
      if (action === 'resume') await resumeBulkImportBatch(batchId);
      if (action === 'cancel') await cancelBulkImportBatch(batchId);
      if (action === 'retry') await retryBulkImportBatch(batchId, true);
      success(`Batch ${action} requested`);
      await refreshBatches();
      await loadBatchDetail(batchId);
    } catch (e) {
      error(`Failed to ${action} batch`);
    }
  };

  const filteredTasks = useMemo(
    () => (taskStatusFilter === 'ALL' ? tasks : tasks.filter(t => t.status === taskStatusFilter)),
    [taskStatusFilter, tasks]
  );

  return (
    <div className="max-w-7xl mx-auto space-y-6">
      <ToastContainer toasts={toasts} onRemove={removeToast} />
      <div className="flex flex-col gap-1">
        <h1 className="text-2xl font-display font-bold text-ink-900">Bulk Import Orchestration</h1>
        <p className="text-sm text-ink-500">
          Manage CSV-driven bulk imports with progress tracking, retries, and task drill-down.
        </p>
      </div>

      <div className="bg-white border border-border-light rounded-xl shadow-sm p-4 md:p-6 flex flex-col gap-4">
        <div className="flex flex-col md:flex-row md:items-end gap-3">
          <div className="flex-1">
            <label className="text-xs font-semibold text-ink-600 mb-1 block">Manifest Path</label>
            <input
              value={manifestPath}
              onChange={e => setManifestPath(e.target.value)}
              placeholder={DEFAULT_MANIFEST_PATH}
              className="w-full px-3 py-2 rounded-lg border border-border-light focus:ring-2 focus:ring-primary focus:border-transparent"
            />
            <p className="text-[11px] text-ink-400 mt-1">
              Example: database/for_import/*.csv — tasks will be generated per row.
            </p>
          </div>
          <button
            onClick={handleCreate}
            disabled={creating}
            className="px-4 py-2.5 bg-primary text-white rounded-lg font-medium shadow-sm hover:bg-primary-dark disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {creating ? 'Starting…' : 'Start Batch'}
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
                <div className="flex gap-2">
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
                </div>
              </div>

              <div className="grid grid-cols-2 gap-3 text-xs">
                <div className="p-3 rounded-lg bg-slate-50">
                  <div className="text-ink-500 font-semibold">Status</div>
                  <div className="mt-1">
                    <span className={`px-2 py-1 rounded-full text-[11px] font-semibold ${statusChip[selectedBatch.status]}`}>
                      {selectedBatch.status}
                    </span>
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
                <div className="border border-border-light rounded-lg">
                  <div className="px-3 py-2 border-b border-border-light text-sm font-semibold text-ink-800">Jobs</div>
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
                      <div key={task.id} className="px-3 py-2">
                        <div className="flex items-center justify-between">
                          <div className="text-xs font-semibold text-ink-900 truncate">
                            {task.sourceUrl || task.krithiKey || 'Task'}
                          </div>
                          <span className={`px-2 py-1 rounded-full text-[10px] font-bold ${taskChip[task.status]}`}>
                            {task.status}
                          </span>
                        </div>
                        <div className="text-[11px] text-ink-500">
                          Attempt {task.attempt} · Duration {task.durationMs ?? '—'}ms
                        </div>
                        {task.error && (
                          <div className="text-[11px] text-rose-600 mt-1">{parseError(task.error)}</div>
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
