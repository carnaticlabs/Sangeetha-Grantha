import React, { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import {
  BulkTaskStatus,
  ImportBatch,
  ImportEvent,
  ImportJob,
  ImportTaskRun
} from '../types';
import {
  getBulkImportBatch,
  getBulkImportEvents,
  getBulkImportJobs,
  getBulkImportTasks,
  listBulkImportBatches,
  uploadBulkImportFile
} from '../api/client';
import { ToastContainer, useToast } from '../components/Toast';
import { useBatchActions } from '../hooks/useBatchActions';
import { BatchList, TaskLogDrawer, UploadPanel } from '../components/bulk-import';
import {
  basename,
  batchStatusLabel,
  eventTypeLabel,
  formatDate,
  formatDuration,
  jobTypeLabel,
  parseError,
  statusChip,
  taskChip,
  taskStatusLabel
} from '../utils/bulk-import-format';

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
  const [creating, setCreating] = useState<boolean>(false);
  const [selectedTaskForLog, setSelectedTaskForLog] = useState<ImportTaskRun | null>(null);

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

  const { triggerAction } = useBatchActions(async () => {
    await refreshBatches();
    if (selectedBatchId) await loadBatchDetail(selectedBatchId);
  });

  const selectedProgress = useMemo(() => {
    if (!selectedBatch || selectedBatch.totalTasks === 0) return 0;
    return Math.round((selectedBatch.processedTasks / selectedBatch.totalTasks) * 100);
  }, [selectedBatch]);

  const currentStage = useMemo(() => {
    if (!jobs.length) return 'Initializing';
    if (jobs.some(j => j.jobType === 'SCRAPE' && j.status === 'RUNNING')) return 'Fetching Content';
    if (jobs.some(j => j.jobType === 'MANIFEST_INGEST' && j.status === 'RUNNING')) return 'Reading File';
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
      }, 5000);
    }
    return () => clearInterval(interval);
  }, [selectedBatch?.status, selectedBatchId]);

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

  const filteredTasks = useMemo(
    () => (taskStatusFilter === 'ALL' ? tasks : tasks.filter(t => t.status === taskStatusFilter)),
    [taskStatusFilter, tasks]
  );

  /** Task counts by status (from loaded tasks; accurate when filter is ALL). */
  const taskBreakdown = useMemo(() => {
    const byStatus: Record<BulkTaskStatus, number> = {
      PENDING: 0,
      RUNNING: 0,
      SUCCEEDED: 0,
      FAILED: 0,
      RETRYABLE: 0,
      BLOCKED: 0,
      CANCELLED: 0
    };
    tasks.forEach(t => {
      if (t.status in byStatus) byStatus[t.status as BulkTaskStatus]++;
    });
    return byStatus;
  }, [tasks]);

  const notCompleteCount = (selectedBatch?.totalTasks ?? 0) - (selectedBatch?.processedTasks ?? 0);

  return (
    <div className="max-w-7xl mx-auto space-y-6">
      <ToastContainer toasts={toasts} onRemove={removeToast} />

      {selectedTaskForLog && (
        <TaskLogDrawer
          task={selectedTaskForLog}
          onClose={() => setSelectedTaskForLog(null)}
          onCopied={() => success('Task data copied to clipboard')}
        />
      )}

      <div className="flex flex-col gap-1">
        <h1 className="text-2xl font-display font-bold text-ink-900">Add Compositions</h1>
        <p className="text-sm text-ink-500">
          Upload a list of compositions and track their progress.
        </p>
      </div>

      <UploadPanel
        selectedFile={selectedFile}
        creating={creating}
        onFileSelect={setSelectedFile}
        onUpload={handleCreate}
      />

      <div className="grid grid-cols-1 xl:grid-cols-3 gap-6 min-w-0">
        <BatchList
          batches={batches}
          loading={loadingList}
          selectedBatchId={selectedBatchId}
          onSelect={setSelectedBatchId}
          onRefresh={() => refreshBatches()}
          onAction={triggerAction}
        />

        <div className="bg-white border border-border-light rounded-xl shadow-sm p-4 space-y-4 min-w-0">
          {selectedBatch ? (
            <>
              <div className="flex flex-col gap-3">
                <div className="min-w-0">
                  <h3 className="text-sm font-bold text-ink-900">Batch Detail</h3>
                  <p className="text-xs text-ink-500 truncate" title={selectedBatch.sourceManifest || undefined}>
                    {basename(selectedBatch.sourceManifest || '') || '—'}
                  </p>
                </div>
                <div className="flex flex-wrap gap-2">
                  <button
                    onClick={() => triggerAction('approveAll', selectedBatch.id)}
                    title="Accept all completed compositions into the collection"
                    className="px-2.5 py-1.5 text-xs rounded-md border border-green-200 text-green-700 bg-green-50 hover:bg-green-100 font-bold"
                  >
                    Approve All
                  </button>
                  <button
                    onClick={() => triggerAction('rejectAll', selectedBatch.id)}
                    title="Mark all compositions in this batch as rejected"
                    className="px-2.5 py-1.5 text-xs rounded-md border border-amber-200 text-amber-700 bg-amber-50 hover:bg-amber-100"
                  >
                    Reject All
                  </button>
                  <button
                    onClick={() => triggerAction('retry', selectedBatch.id)}
                    title="Re-process any compositions that failed"
                    className="px-2.5 py-1.5 text-xs rounded-md border border-border-light text-primary font-semibold hover:border-primary"
                  >
                    Retry Failed
                  </button>
                  <button
                    onClick={() => triggerAction('cancel', selectedBatch.id)}
                    title="Stop processing remaining compositions"
                    className="px-2.5 py-1.5 text-xs rounded-md border border-rose-200 text-rose-700 bg-rose-50"
                  >
                    Cancel
                  </button>
                  <button
                    onClick={() => triggerAction('delete', selectedBatch.id)}
                    title="Remove this batch and all its data"
                    className="px-2.5 py-1.5 text-xs rounded-md border border-slate-200 text-slate-700 bg-slate-50 hover:bg-slate-100"
                  >
                    Delete
                  </button>
                </div>
              </div>

              {/* TRACK-052: Extraction Queue cross-link */}
              <Link
                to="/admin/sourcing/extractions"
                className="flex items-center gap-2 px-3 py-2 text-xs font-medium text-primary border border-primary/30 rounded-lg hover:bg-primary-light transition-colors"
              >
                <span className="material-symbols-outlined text-sm">queue</span>
                View Processing Queue
              </Link>

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

              {/* Task breakdown: explains "other N" when progress is 8/14 (e.g. 6 are RETRYABLE/PENDING) */}
              {selectedBatch.totalTasks > 0 && (
                <div className="p-3 rounded-lg bg-slate-50 border border-border-light">
                  <div className="text-ink-500 font-semibold text-xs mb-1">Progress breakdown</div>
                  <div className="flex flex-wrap gap-x-3 gap-y-1 text-xs text-ink-700">
                    <span>Completed: {selectedBatch.succeededTasks}</span>
                    <span>Failed: {selectedBatch.failedTasks}</span>
                    {taskBreakdown.RETRYABLE > 0 && <span>Will retry: {taskBreakdown.RETRYABLE}</span>}
                    {taskBreakdown.PENDING > 0 && <span>Waiting: {taskBreakdown.PENDING}</span>}
                    {taskBreakdown.RUNNING > 0 && <span>In progress: {taskBreakdown.RUNNING}</span>}
                    {notCompleteCount > 0 && (
                      <span className="text-ink-600">
                        ({notCompleteCount} not yet complete)
                      </span>
                    )}
                  </div>
                  {notCompleteCount > 0 && selectedBatch.status === 'RUNNING' && (
                    <p className="text-[11px] text-ink-500 mt-1.5">
                      These tasks will be retried automatically, or use <strong>Retry Failed</strong> to requeue retryable tasks now.
                    </p>
                  )}
                </div>
              )}

              <div className="grid grid-cols-2 gap-3 text-xs">
                {/* Status/Progress chips same as before */}
                <div className="p-3 rounded-lg bg-slate-50">
                  <div className="text-ink-500 font-semibold">Status</div>
                  <div className="mt-1 flex items-center gap-2">
                    <span className={`px-2 py-1 rounded-full text-[11px] font-semibold ${statusChip[selectedBatch.status]}`}>
                      {batchStatusLabel[selectedBatch.status]}
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
                    <div className="text-sm font-semibold text-ink-800 mb-3">Processing Steps</div>
                    <div className="relative flex items-stretch justify-between gap-2">
                      {['MANIFEST_INGEST', 'SCRAPE'].map((stage, index) => {
                        const job = jobs.find(j => j.jobType === stage);
                        const isActive = job?.status === 'RUNNING';
                        const isCompleted = job?.status === 'SUCCEEDED';
                        const isFailed = job?.status === 'FAILED';
                        const succeededCount = tasks.filter(t => t.status === 'SUCCEEDED').length;
                        const failedCount = tasks.filter(t => t.status === 'FAILED').length;
                        const showTaskSummary = stage === 'SCRAPE' && tasks.length > 0 && (isCompleted || isFailed);

                        return (
                          <React.Fragment key={stage}>
                            {index > 0 && (
                              <div className="flex-1 flex items-center min-w-2 self-stretch">
                                <div className={`h-0.5 w-full ${isCompleted || (index < jobs.findIndex(j => j.status === 'RUNNING'))
                                  ? 'bg-primary'
                                  : 'bg-slate-300'
                                  }`} />
                              </div>
                            )}
                            <div className="flex flex-col items-center gap-1.5 shrink-0 w-[84px] min-h-[88px]">
                              <div className={`w-10 h-10 rounded-full flex items-center justify-center text-xs font-bold border-2 shrink-0 ${isActive
                                ? 'bg-blue-50 border-blue-500 text-blue-700 animate-pulse'
                                : isCompleted
                                  ? 'bg-green-50 border-green-500 text-green-700'
                                  : isFailed
                                    ? 'bg-rose-50 border-rose-500 text-rose-700'
                                    : 'bg-slate-100 border-slate-300 text-slate-500'
                                }`}>
                                {isCompleted ? '✓' : isFailed ? '✗' : index + 1}
                              </div>
                              <div className="text-[10px] font-semibold text-center leading-tight">
                                {jobTypeLabel[stage] || stage}
                              </div>
                              {job && (
                                <span className={`px-1.5 py-0.5 rounded text-[8px] font-bold ${taskChip[job.status]}`}>
                                  {taskStatusLabel[job.status] || job.status}
                                </span>
                              )}
                              <div className="min-h-[1.25rem] flex items-center justify-center">
                                {showTaskSummary ? (
                                  <span className="text-[10px] text-ink-600 text-center">
                                    {succeededCount} ok{failedCount > 0 ? `, ${failedCount} failed` : ''}
                                  </span>
                                ) : null}
                              </div>
                            </div>
                          </React.Fragment>
                        );
                      })}
                    </div>
                  </div>

                  {/* Job Details List */}
                  <div className="border border-border-light rounded-lg">
                    <div className="px-3 py-2 border-b border-border-light text-sm font-semibold text-ink-800">Step Details</div>
                    <div className="divide-y divide-border-light max-h-40 overflow-y-auto">
                      {jobs.filter(job => job.jobType !== 'ENTITY_RESOLUTION').map(job => (
                        <div key={job.id} className="px-3 py-2 flex items-center justify-between text-xs">
                          <span className="font-semibold text-ink-900">{jobTypeLabel[job.jobType] || job.jobType}</span>
                          <span className={`px-2 py-1 rounded-full text-[10px] font-bold ${taskChip[job.status]}`}>
                            {taskStatusLabel[job.status] || job.status}
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
                    {(Object.keys(taskChip) as BulkTaskStatus[]).map(key => (
                      <option key={key} value={key}>
                        {taskStatusLabel[key]}
                      </option>
                    ))}
                  </select>
                </div>
                {filteredTasks.length === 0 ? (
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
                              {taskStatusLabel[task.status]}
                            </span>
                            <span className="material-symbols-outlined text-sm text-ink-400">chevron_right</span>
                          </div>
                        </div>
                        <div className="text-[11px] text-ink-500">
                          Try #{task.attempt} · {formatDuration(task.durationMs)}
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
                <div className="px-3 py-2 border-b border-border-light text-sm font-semibold text-ink-800">Activity</div>
                {events.length === 0 ? (
                  <div className="p-4 text-ink-400 text-sm">No activity yet.</div>
                ) : (
                  <div className="max-h-56 overflow-y-auto px-3 py-2">
                    <div className="relative border-l-2 border-slate-200 ml-2 space-y-3">
                      {events.slice(0, 30).map(event => {
                        const isError = event.eventType.includes('FAILED');
                        const isSuccess = event.eventType.includes('SUCCEEDED') || event.eventType.includes('COMPLETED');
                        const dotColor = isError ? 'bg-rose-400' : isSuccess ? 'bg-green-400' : 'bg-blue-400';
                        let parsedData: Record<string, unknown> | null = null;
                        if (event.data) {
                          try { parsedData = JSON.parse(event.data) as Record<string, unknown>; } catch { /* use raw */ }
                        }
                        return (
                          <div key={event.id} className="relative pl-5 text-xs">
                            <div className={`absolute -left-[5px] top-1 w-2.5 h-2.5 rounded-full ${dotColor} border-2 border-white`} />
                            <div className="flex items-center justify-between">
                              <span className="font-semibold text-ink-900">{eventTypeLabel[event.eventType] || event.eventType}</span>
                              <span className="text-ink-400 text-[10px]">{formatDate(event.createdAt)}</span>
                            </div>
                            {parsedData && (
                              <dl className="mt-1 grid grid-cols-[auto_1fr] gap-x-3 gap-y-0.5 text-[11px]">
                                {Object.entries(parsedData).map(([k, v]) => (
                                  <React.Fragment key={k}>
                                    <dt className="text-ink-400 capitalize">{k.replace(/([A-Z])/g, ' $1').replace(/_/g, ' ').trim()}</dt>
                                    <dd className="text-ink-700 font-medium">{String(v)}</dd>
                                  </React.Fragment>
                                ))}
                              </dl>
                            )}
                            {event.data && !parsedData && (
                              <div className="text-ink-600 mt-1">{parseError(event.data)}</div>
                            )}
                          </div>
                        );
                      })}
                    </div>
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
