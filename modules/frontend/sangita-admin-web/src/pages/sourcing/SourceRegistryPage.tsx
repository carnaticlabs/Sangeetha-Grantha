import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import type { SourceFilterParams, ImportSource, CreateSourceRequest } from '../../types/sourcing';
import { useSourceList, useCreateSource, useDeactivateSource } from '../../hooks/useSourcingQueries';
import { TierBadge, FormatPill } from '../../components/sourcing/shared';
import SourceFilterBar from '../../components/sourcing/SourceFilterBar';
import SourceFormModal from '../../components/sourcing/SourceFormModal';

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

const SourceRegistryPage: React.FC = () => {
  const navigate = useNavigate();
  const [filters, setFilters] = useState<SourceFilterParams>({});
  const [showModal, setShowModal] = useState(false);
  const [editSource, setEditSource] = useState<ImportSource | null>(null);

  const { data, isLoading, error } = useSourceList(filters);
  const createSource = useCreateSource();
  const deactivateSource = useDeactivateSource();

  const handleCreate = async (request: CreateSourceRequest) => {
    await createSource.mutateAsync(request);
    setShowModal(false);
  };

  const handleDeactivate = async (id: string) => {
    if (!confirm('Are you sure you want to deactivate this source?')) return;
    await deactivateSource.mutateAsync(id);
  };

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-2xl font-display font-bold text-ink-900">Source Registry</h1>
          <p className="text-sm text-ink-500 mt-1">Manage import sources and authority tiers</p>
        </div>
        <button
          onClick={() => { setEditSource(null); setShowModal(true); }}
          className="inline-flex items-center gap-2 px-4 py-2.5 bg-primary text-white rounded-lg text-sm font-medium hover:bg-primary/90 transition-colors"
        >
          <span className="material-symbols-outlined text-lg">add</span>
          Register New Source
        </button>
      </div>

      <SourceFilterBar filters={filters} onFiltersChange={setFilters} />

      {error ? (
        <div className="bg-red-50 border border-red-200 rounded-xl p-6 text-center">
          <span className="material-symbols-outlined text-3xl text-red-400 mb-2 block">error_outline</span>
          <p className="text-sm text-red-700">Failed to load sources. Please try again.</p>
          <button onClick={() => window.location.reload()} className="mt-2 text-sm text-primary hover:underline">Retry</button>
        </div>
      ) : isLoading ? (
        <div className="bg-white rounded-xl border border-border-light overflow-hidden">
          <table className="w-full">
            <thead>
              <tr className="border-b border-border-light bg-slate-50">
                {['Name', 'URL', 'Tier', 'Formats', 'Krithis', 'Last Harvested', 'Actions'].map((h) => (
                  <th key={h} className="px-4 py-3 text-left text-xs font-semibold text-ink-500 uppercase tracking-wider">{h}</th>
                ))}
              </tr>
            </thead>
            <tbody>
              {[1, 2, 3, 4, 5].map((i) => (
                <tr key={i} className="border-b border-border-light animate-pulse">
                  {Array.from({ length: 7 }).map((_, j) => (
                    <td key={j} className="px-4 py-3"><div className="h-4 bg-slate-200 rounded w-3/4" /></td>
                  ))}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ) : data?.items.length === 0 ? (
        <div className="bg-white rounded-xl border border-border-light p-12 text-center">
          <span className="material-symbols-outlined text-5xl text-ink-200 mb-3 block">source</span>
          <h3 className="text-lg font-semibold text-ink-700 mb-1">No sources registered yet</h3>
          <p className="text-sm text-ink-500 mb-4">Click "Register New Source" to get started.</p>
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
                <th className="px-4 py-3 text-left text-xs font-semibold text-ink-500 uppercase tracking-wider">Last Harvested</th>
                <th className="px-4 py-3 text-left text-xs font-semibold text-ink-500 uppercase tracking-wider">Actions</th>
              </tr>
            </thead>
            <tbody>
              {data?.items.map((source) => (
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
                  <td className="px-4 py-3">
                    <TierBadge tier={source.sourceTier} />
                  </td>
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
                        onClick={() => { setEditSource(source); setShowModal(true); }}
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

          {/* Pagination info */}
          {data && (
            <div className="px-4 py-3 border-t border-border-light bg-slate-50 text-sm text-ink-500">
              Showing {data.items.length} of {data.total} sources
            </div>
          )}
        </div>
      )}

      <SourceFormModal
        isOpen={showModal}
        onClose={() => { setShowModal(false); setEditSource(null); }}
        onSubmit={handleCreate}
        source={editSource}
        isSubmitting={createSource.isPending}
      />
    </div>
  );
};

export default SourceRegistryPage;
