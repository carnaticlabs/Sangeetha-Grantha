import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import type { SourceFilterParams, ImportSource, CreateSourceRequest, ExtractionFilterParams } from '../../types/sourcing';
import { useSourceList, useCreateSource, useDeactivateSource, useExtractionList, useExtractionStats, useCreateExtraction, useRetryExtraction, useCancelExtraction, useRetryAllFailedExtractions, useProcessExtractionQueue } from '../../hooks/useSourcingQueries';
import { TierBadge, FormatPill, StatusChip, ConfidenceBar } from '../../components/sourcing/shared';
import SourceFilterBar from '../../components/sourcing/SourceFilterBar';
import SourceFormModal from '../../components/sourcing/SourceFormModal';
import StatusSummaryBar from '../../components/sourcing/StatusSummaryBar';
import ExtractionRequestModal from '../../components/sourcing/ExtractionRequestModal';

function formatRelativeTime(iso: string | null | undefined): string {
  if (!iso) return '—';
  const date = new Date(iso);
  const now = new Date();
  const diffMs = now.getTime() - date.getTime();
  const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24));
  if (diffDays === 0) return 'Today';
  if (diffDays === 1) return 'Yesterday';
  if (diffDays < 30) return `${diffDays} days ago`;
  if (diffDays < 365) return `${Math.floor(diffDays / 30)} months ago`;
  return `${Math.floor(diffDays / 365)} years ago`;
}

function formatDuration(ms: number | null | undefined): string {
  if (ms == null) return '—';
  if (ms < 1000) return `${ms}ms`;
  return `${(ms / 1000).toFixed(1)}s`;
}

function formatRelativeTimeShort(iso: string): string {
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

const SourcesAndProcessingPage: React.FC = () => {
  const navigate = useNavigate();

  // Sources state
  const [sourceFilters, setSourceFilters] = useState<SourceFilterParams>({});
  const [showSourceModal, setShowSourceModal] = useState(false);
  const [editSource, setEditSource] = useState<ImportSource | null>(null);
  const { data: sourceData, isLoading: sourcesLoading, error: sourcesError } = useSourceList(sourceFilters);
  const createSource = useCreateSource();
  const deactivateSource = useDeactivateSource();

  // Processing state
  const [extractionFilters] = useState<ExtractionFilterParams>({});
  const [showCreateModal, setShowCreateModal] = useState(false);
  const { data: extractionData, isLoading: extractionsLoading } = useExtractionList(extractionFilters, 10000);
  const { data: stats, isLoading: statsLoading } = useExtractionStats(10000);
  const createMutation = useCreateExtraction();
  const retryMutation = useRetryExtraction();
  const cancelMutation = useCancelExtraction();
  const retryAllMutation = useRetryAllFailedExtractions();
  const processMutation = useProcessExtractionQueue();

  const handleCreateSource = async (request: CreateSourceRequest) => {
    await createSource.mutateAsync(request);
    setShowSourceModal(false);
  };

  const handleDeactivate = async (id: string) => {
    if (!confirm('Are you sure you want to remove this source from active use?')) return;
    await deactivateSource.mutateAsync(id);
  };

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
    await processMutation.mutateAsync();
  };

  return (
    <div className="space-y-10">
      {/* ─── Sources Section ─── */}
      <section>
        <div className="flex items-center justify-between mb-6">
          <div>
            <h1 className="text-2xl font-display font-bold text-ink-900">Sources</h1>
            <p className="text-sm text-ink-500 mt-1">Manage where compositions come from and their trust levels</p>
          </div>
          <button
            onClick={() => { setEditSource(null); setShowSourceModal(true); }}
            className="inline-flex items-center gap-2 px-4 py-2.5 bg-primary text-white rounded-lg text-sm font-medium hover:bg-primary/90 transition-colors"
          >
            <span className="material-symbols-outlined text-lg">add</span>
            Add New Source
          </button>
        </div>

        <SourceFilterBar filters={sourceFilters} onFiltersChange={setSourceFilters} />

        {sourcesError ? (
          <div className="bg-red-50 border border-red-200 rounded-xl p-6 text-center">
            <span className="material-symbols-outlined text-3xl text-red-400 mb-2 block">error_outline</span>
            <p className="text-sm text-red-700">Failed to load sources. Please try again.</p>
          </div>
        ) : sourcesLoading ? (
          <div className="bg-white rounded-xl border border-border-light overflow-hidden">
            <table className="w-full">
              <thead>
                <tr className="border-b border-border-light bg-slate-50">
                  {['Name', 'URL', 'Tier', 'Formats', 'Krithis', 'Last Imported', 'Actions'].map((h) => (
                    <th key={h} className="px-4 py-3 text-left text-xs font-semibold text-ink-500 uppercase tracking-wider">{h}</th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {[1, 2, 3].map((i) => (
                  <tr key={i} className="border-b border-border-light animate-pulse">
                    {Array.from({ length: 7 }).map((_, j) => (
                      <td key={j} className="px-4 py-3"><div className="h-4 bg-slate-200 rounded w-3/4" /></td>
                    ))}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : sourceData?.items.length === 0 ? (
          <div className="bg-white rounded-xl border border-border-light p-12 text-center">
            <span className="material-symbols-outlined text-5xl text-ink-200 mb-3 block">source</span>
            <h3 className="text-lg font-semibold text-ink-700 mb-1">No sources added yet</h3>
            <p className="text-sm text-ink-500 mb-4">Click "Add New Source" to get started.</p>
          </div>
        ) : (
          <div className="bg-white rounded-xl border border-border-light overflow-hidden">
            <table className="w-full">
              <thead>
                <tr className="border-b border-border-light bg-slate-50">
                  <th className="px-4 py-3 text-left text-xs font-semibold text-ink-500 uppercase tracking-wider">Name</th>
                  <th className="px-4 py-3 text-left text-xs font-semibold text-ink-500 uppercase tracking-wider">URL</th>
                  <th className="px-4 py-3 text-left text-xs font-semibold text-ink-500 uppercase tracking-wider">Tier</th>
                  <th className="px-4 py-3 text-left text-xs font-semibold text-ink-500 uppercase tracking-wider">Formats</th>
                  <th className="px-4 py-3 text-left text-xs font-semibold text-ink-500 uppercase tracking-wider">Krithis</th>
                  <th className="px-4 py-3 text-left text-xs font-semibold text-ink-500 uppercase tracking-wider">Last Imported</th>
                  <th className="px-4 py-3 text-left text-xs font-semibold text-ink-500 uppercase tracking-wider">Actions</th>
                </tr>
              </thead>
              <tbody>
                {sourceData?.items.map((source) => (
                  <tr
                    key={source.id}
                    className="border-b border-border-light hover:bg-slate-50/50 cursor-pointer transition-colors"
                    onClick={() => navigate(`/admin/sourcing/sources/${source.id}`)}
                  >
                    <td className="px-4 py-3">
                      <span className="text-sm font-medium text-ink-800">{source.name}</span>
                    </td>
                    <td className="px-4 py-3">
                      <span className="text-sm text-ink-500 truncate block max-w-[200px]" title={source.baseUrl || ''}>
                        {source.baseUrl || '—'}
                      </span>
                    </td>
                    <td className="px-4 py-3"><TierBadge tier={source.sourceTier} /></td>
                    <td className="px-4 py-3">
                      <div className="flex flex-wrap gap-1">
                        {source.supportedFormats.map((f) => (
                          <FormatPill key={f} format={f} />
                        ))}
                      </div>
                    </td>
                    <td className="px-4 py-3">
                      <span className="text-sm font-medium text-ink-700 tabular-nums">{source.krithiCount}</span>
                    </td>
                    <td className="px-4 py-3">
                      <span className="text-sm text-ink-500">
                        {formatRelativeTime(source.lastHarvestedAt as string | null)}
                      </span>
                    </td>
                    <td className="px-4 py-3" onClick={(e) => e.stopPropagation()}>
                      <div className="flex items-center gap-1">
                        <button
                          onClick={() => navigate(`/admin/sourcing/sources/${source.id}`)}
                          className="p-1 text-ink-400 hover:text-primary transition-colors"
                          title="View"
                        >
                          <span className="material-symbols-outlined text-lg">visibility</span>
                        </button>
                        <button
                          onClick={() => { setEditSource(source); setShowSourceModal(true); }}
                          className="p-1 text-ink-400 hover:text-primary transition-colors"
                          title="Edit"
                        >
                          <span className="material-symbols-outlined text-lg">edit</span>
                        </button>
                        <button
                          onClick={() => handleDeactivate(source.id)}
                          className="p-1 text-ink-400 hover:text-red-600 transition-colors"
                          title="Deactivate"
                        >
                          <span className="material-symbols-outlined text-lg">block</span>
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
            {sourceData && (
              <div className="px-4 py-3 border-t border-border-light bg-slate-50 text-sm text-ink-500">
                Showing {sourceData.items.length} of {sourceData.total} sources
              </div>
            )}
          </div>
        )}
      </section>

      {/* ─── Processing Queue Section ─── */}
      <section>
        <div className="flex items-center justify-between mb-6">
          <div>
            <h2 className="text-xl font-display font-bold text-ink-900">Processing Queue</h2>
            <p className="text-sm text-ink-500 mt-1">Track and manage composition processing from sources</p>
          </div>
          <div className="flex gap-2">
            <button
              onClick={handleProcessPending}
              disabled={processMutation.isPending}
              className="inline-flex items-center gap-2 px-4 py-2.5 bg-white border border-border-light text-ink-700 rounded-lg text-sm font-medium hover:bg-slate-50 transition-colors disabled:opacity-50"
            >
              <span className="material-symbols-outlined text-lg">play_arrow</span>
              Process Waiting
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
              Process New Source
            </button>
          </div>
        </div>

        <StatusSummaryBar stats={stats} loading={statsLoading} />

        <div className="bg-white rounded-xl border border-border-light overflow-hidden">
          <table className="w-full">
            <thead>
              <tr className="border-b border-border-light bg-slate-50">
                <th className="px-4 py-3 text-left text-xs font-semibold text-ink-500 uppercase tracking-wider">ID</th>
                <th className="px-4 py-3 text-left text-xs font-semibold text-ink-500 uppercase tracking-wider">Source</th>
                <th className="px-4 py-3 text-left text-xs font-semibold text-ink-500 uppercase tracking-wider">Format</th>
                <th className="px-4 py-3 text-left text-xs font-semibold text-ink-500 uppercase tracking-wider">Status</th>
                <th className="px-4 py-3 text-left text-xs font-semibold text-ink-500 uppercase tracking-wider">Krithis</th>
                <th className="px-4 py-3 text-left text-xs font-semibold text-ink-500 uppercase tracking-wider">Reliability</th>
                <th className="px-4 py-3 text-left text-xs font-semibold text-ink-500 uppercase tracking-wider">Time Taken</th>
                <th className="px-4 py-3 text-left text-xs font-semibold text-ink-500 uppercase tracking-wider">Created</th>
                <th className="px-4 py-3 text-left text-xs font-semibold text-ink-500 uppercase tracking-wider">Actions</th>
              </tr>
            </thead>
            <tbody>
              {extractionsLoading ? (
                Array.from({ length: 3 }).map((_, i) => (
                  <tr key={i} className="border-b border-border-light animate-pulse">
                    {Array.from({ length: 9 }).map((_, j) => (
                      <td key={j} className="px-4 py-3"><div className="h-4 bg-slate-200 rounded w-3/4" /></td>
                    ))}
                  </tr>
                ))
              ) : extractionData?.items.length === 0 ? (
                <tr>
                  <td colSpan={9} className="px-4 py-12 text-center">
                    <span className="material-symbols-outlined text-4xl text-ink-200 mb-2 block">manufacturing</span>
                    <p className="text-sm text-ink-500">No items yet — process a source to get started.</p>
                  </td>
                </tr>
              ) : (
                extractionData?.items.map((task) => (
                  <tr
                    key={task.id}
                    className={`border-b border-border-light hover:bg-slate-50/50 cursor-pointer transition-colors ${task.status === 'FAILED' ? 'border-l-2 border-l-red-400' : ''}`}
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
                      <span className="text-xs text-ink-500">{formatRelativeTimeShort(task.createdAt as string)}</span>
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
          {extractionData && extractionData.items.length > 0 && (
            <div className="px-4 py-3 border-t border-border-light bg-slate-50 text-sm text-ink-500">
              Showing {extractionData.items.length} of {extractionData.total} items
            </div>
          )}
        </div>
      </section>

      <SourceFormModal
        isOpen={showSourceModal}
        onClose={() => { setShowSourceModal(false); setEditSource(null); }}
        onSubmit={handleCreateSource}
        source={editSource}
        isSubmitting={createSource.isPending}
      />
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

export default SourcesAndProcessingPage;
