import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import type { ExtractionFilterParams } from '../../types/sourcing';
import { useExtractionList, useExtractionStats, useCreateExtraction, useRetryExtraction, useCancelExtraction, useRetryAllFailedExtractions, useProcessExtractionQueue } from '../../hooks/useSourcingQueries';
import { StatusChip, FormatPill, ConfidenceBar, TierBadge } from '../../components/sourcing/shared';
import StatusSummaryBar from '../../components/sourcing/StatusSummaryBar';
import ExtractionRequestModal from '../../components/sourcing/ExtractionRequestModal';

function formatDuration(ms: number | null | undefined): string {
  if (ms == null) return '—';
  if (ms < 1000) return `${ms}ms`;
  return `${(ms / 1000).toFixed(1)}s`;
}

function formatRelativeTime(iso: string): string {
  const date = new Date(iso);
  const now = new Date();
  const diffMs = now.getTime() - date.getTime();
  const diffMin = Math.floor(diffMs / 60000);
  if (diffMin < 1) return 'Just now';
  if (diffMin < 60) return `${diffMin}m ago`;
  const diffHours = Math.floor(diffMin / 60);
  if (diffHours < 24) return `${diffHours}h ago`;
  return `${Math.floor(diffHours / 24)}d ago`;
}

const ExtractionMonitorPage: React.FC = () => {
  const navigate = useNavigate();
  const [filters] = useState<ExtractionFilterParams>({});
  const [showCreateModal, setShowCreateModal] = useState(false);

  const { data, isLoading } = useExtractionList(filters, 10000); // Auto-refresh every 10s
  const { data: stats, isLoading: statsLoading } = useExtractionStats(10000);
  const createMutation = useCreateExtraction();
  const retryMutation = useRetryExtraction();
  const cancelMutation = useCancelExtraction();

  const retryAllMutation = useRetryAllFailedExtractions();
  const processMutation = useProcessExtractionQueue();

  const handleRetry = async (id: string) => {
    if (!confirm('Retry this extraction?')) return;
    await retryMutation.mutateAsync(id);
  };

  const handleCancel = async (id: string) => {
    if (!confirm('Cancel this extraction?')) return;
    await cancelMutation.mutateAsync(id);
  };

  const handleRetryAll = async () => {
    const failedCount = stats?.failed ?? 0;
    if (!confirm(`Retry all ${failedCount} failed extractions?`)) return;
    await retryAllMutation.mutateAsync();
  };

  const handleProcessPending = async () => {
    if (!confirm('Process pending extractions immediately?')) return;
    await processMutation.mutateAsync();
  };

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-2xl font-display font-bold text-ink-900">Extraction Queue</h1>
          <p className="text-sm text-ink-500 mt-1">Monitor and manage the extraction pipeline</p>
        </div>
        <div className="flex gap-2">
          <button
            onClick={handleProcessPending}
            disabled={processMutation.isPending}
            className="inline-flex items-center gap-2 px-4 py-2.5 bg-white border border-border-light text-ink-700 rounded-lg text-sm font-medium hover:bg-slate-50 transition-colors disabled:opacity-50"
          >
            <span className="material-symbols-outlined text-lg">play_arrow</span>
            Process Pending
          </button>
          {(stats?.failed ?? 0) > 0 && (
            <button
              onClick={handleRetryAll}
              disabled={retryAllMutation.isPending}
              className="inline-flex items-center gap-2 px-4 py-2.5 bg-white border border-red-200 text-red-700 rounded-lg text-sm font-medium hover:bg-red-50 transition-colors disabled:opacity-50"
            >
              <span className="material-symbols-outlined text-lg">replay</span>
              Retry All Failed ({stats?.failed ?? 0})
            </button>
          )}
          <button
            onClick={() => setShowCreateModal(true)}
            className="inline-flex items-center gap-2 px-4 py-2.5 bg-primary text-white rounded-lg text-sm font-medium hover:bg-primary/90 transition-colors"
          >
            <span className="material-symbols-outlined text-lg">add</span>
            New Extraction Request
          </button>
        </div>
      </div>

      <StatusSummaryBar stats={stats} loading={statsLoading} />

      {/* Table */}
      <div className="bg-white rounded-xl border border-border-light overflow-hidden">
        <table className="w-full">
          <thead>
            <tr className="border-b border-border-light bg-slate-50">
              <th className="px-4 py-3 text-left text-xs font-semibold text-ink-500 uppercase tracking-wider">ID</th>
              <th className="px-4 py-3 text-left text-xs font-semibold text-ink-500 uppercase tracking-wider">Source</th>
              <th className="px-4 py-3 text-left text-xs font-semibold text-ink-500 uppercase tracking-wider">Format</th>
              <th className="px-4 py-3 text-left text-xs font-semibold text-ink-500 uppercase tracking-wider">Status</th>
              <th className="px-4 py-3 text-left text-xs font-semibold text-ink-500 uppercase tracking-wider">Krithis</th>
              <th className="px-4 py-3 text-left text-xs font-semibold text-ink-500 uppercase tracking-wider">Confidence</th>
              <th className="px-4 py-3 text-left text-xs font-semibold text-ink-500 uppercase tracking-wider">Duration</th>
              <th className="px-4 py-3 text-left text-xs font-semibold text-ink-500 uppercase tracking-wider">Created</th>
              <th className="px-4 py-3 text-left text-xs font-semibold text-ink-500 uppercase tracking-wider">Actions</th>
            </tr>
          </thead>
          <tbody>
            {isLoading ? (
              Array.from({ length: 5 }).map((_, i) => (
                <tr key={i} className="border-b border-border-light animate-pulse">
                  {Array.from({ length: 9 }).map((_, j) => (
                    <td key={j} className="px-4 py-3"><div className="h-4 bg-slate-200 rounded w-3/4" /></td>
                  ))}
                </tr>
              ))
            ) : data?.items.length === 0 ? (
              <tr>
                <td colSpan={9} className="px-4 py-12 text-center">
                  <span className="material-symbols-outlined text-4xl text-ink-200 mb-2 block">manufacturing</span>
                  <p className="text-sm text-ink-500">No extractions yet — submit an extraction request to get started.</p>
                </td>
              </tr>
            ) : (
              data?.items.map((task) => (
                <tr
                  key={task.id}
                  className={`border-b border-border-light hover:bg-slate-50/50 cursor-pointer transition-colors ${task.status === 'FAILED' ? 'border-l-2 border-l-red-400' : ''
                    }`}
                  onClick={() => navigate(`/admin/sourcing/extractions/${task.id}`)}
                >
                  <td className="px-4 py-3">
                    <span className="text-xs font-mono text-ink-500" title={task.id}>
                      {task.id.slice(0, 8)}...
                    </span>
                  </td>
                  <td className="px-4 py-3">
                    <div className="flex items-center gap-1.5">
                      <span className="text-sm text-ink-700">{task.sourceName || '—'}</span>
                      {task.sourceTier && <TierBadge tier={task.sourceTier} size="sm" />}
                    </div>
                  </td>
                  <td className="px-4 py-3"><FormatPill format={task.sourceFormat} /></td>
                  <td className="px-4 py-3"><StatusChip status={task.status} /></td>
                  <td className="px-4 py-3">
                    <span className="text-sm text-ink-700 tabular-nums">{task.resultCount ?? '—'}</span>
                  </td>
                  <td className="px-4 py-3" style={{ width: 120 }}>
                    {task.confidence != null ? (
                      <ConfidenceBar value={task.confidence} showLabel size="sm" />
                    ) : (
                      <span className="text-xs text-ink-400">—</span>
                    )}
                  </td>
                  <td className="px-4 py-3">
                    <span className="text-sm text-ink-500 tabular-nums">{formatDuration(task.durationMs)}</span>
                  </td>
                  <td className="px-4 py-3">
                    <span className="text-xs text-ink-500">{formatRelativeTime(task.createdAt as string)}</span>
                  </td>
                  <td className="px-4 py-3" onClick={(e) => e.stopPropagation()}>
                    <div className="flex items-center gap-1">
                      {task.status === 'FAILED' && (
                        <button onClick={() => handleRetry(task.id)} className="p-1 text-ink-400 hover:text-primary transition-colors" title="Retry">
                          <span className="material-symbols-outlined text-lg">replay</span>
                        </button>
                      )}
                      {(task.status === 'PENDING' || task.status === 'PROCESSING') && (
                        <button onClick={() => handleCancel(task.id)} className="p-1 text-ink-400 hover:text-red-600 transition-colors" title="Cancel">
                          <span className="material-symbols-outlined text-lg">cancel</span>
                        </button>
                      )}
                    </div>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>

        {data && data.items.length > 0 && (
          <div className="px-4 py-3 border-t border-border-light bg-slate-50 text-sm text-ink-500">
            Showing {data.items.length} of {data.total} extractions
          </div>
        )}
      </div>
      <ExtractionRequestModal
        isOpen={showCreateModal}
        onClose={() => setShowCreateModal(false)}
        onSubmit={async (payload) => {
          await createMutation.mutateAsync(payload);
          setShowCreateModal(false);
        }}
        isSubmitting={createMutation.isPending}
      />
    </div>
  );
};

export default ExtractionMonitorPage;
