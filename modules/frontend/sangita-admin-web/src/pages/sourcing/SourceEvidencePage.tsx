import React, { useState } from 'react';
import type { EvidenceFilterParams } from '../../types/sourcing';
import { useEvidenceList } from '../../hooks/useSourcingQueries';
import { TierBadge, ConfidenceBar } from '../../components/sourcing/shared';

const SourceEvidencePage: React.FC = () => {
  const [filters, setFilters] = useState<EvidenceFilterParams>({});
  const { data, isLoading, error } = useEvidenceList(filters);

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-2xl font-display font-bold text-ink-900">Source Evidence</h1>
          <p className="text-sm text-ink-500 mt-1">Browse provenance data and field-level comparison across sources</p>
        </div>
      </div>

      {/* Filter Bar */}
      <div className="bg-white rounded-xl border border-border-light p-4 mb-6">
        <div className="flex flex-wrap items-center gap-3">
          <div className="flex items-center gap-2">
            <span className="text-xs font-semibold text-ink-500 uppercase">Min Sources</span>
            <input
              type="number"
              min={1}
              value={filters.minSourceCount ?? ''}
              onChange={(e) => setFilters({ ...filters, minSourceCount: e.target.value ? parseInt(e.target.value) : undefined })}
              placeholder="1"
              className="w-16 px-2 py-1.5 text-sm border border-border-light rounded-lg focus:outline-none focus:ring-2 focus:ring-primary/20"
            />
          </div>
          <div className="w-px h-6 bg-border-light" />
          <div className="flex-1 min-w-[200px]">
            <div className="relative">
              <span className="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-ink-400 text-lg">search</span>
              <input
                type="text"
                placeholder="Search Krithi title..."
                value={filters.search || ''}
                onChange={(e) => setFilters({ ...filters, search: e.target.value || undefined })}
                className="w-full pl-9 pr-3 py-2 text-sm border border-border-light rounded-lg focus:outline-none focus:ring-2 focus:ring-primary/20"
              />
            </div>
          </div>
        </div>
      </div>

      {/* Table */}
      <div className="bg-white rounded-xl border border-border-light overflow-hidden">
        <table className="w-full">
          <thead>
            <tr className="border-b border-border-light bg-slate-50">
              <th className="px-4 py-3 text-left text-xs font-semibold text-ink-500 uppercase tracking-wider">Krithi</th>
              <th className="px-4 py-3 text-left text-xs font-semibold text-ink-500 uppercase tracking-wider">Sources</th>
              <th className="px-4 py-3 text-left text-xs font-semibold text-ink-500 uppercase tracking-wider">Top Source</th>
              <th className="px-4 py-3 text-left text-xs font-semibold text-ink-500 uppercase tracking-wider">Contributed Fields</th>
              <th className="px-4 py-3 text-left text-xs font-semibold text-ink-500 uppercase tracking-wider">Avg Confidence</th>
              <th className="px-4 py-3 text-left text-xs font-semibold text-ink-500 uppercase tracking-wider">Voting</th>
            </tr>
          </thead>
          <tbody>
            {isLoading ? (
              Array.from({ length: 5 }).map((_, i) => (
                <tr key={i} className="border-b border-border-light animate-pulse">
                  {Array.from({ length: 6 }).map((_, j) => (
                    <td key={j} className="px-4 py-3"><div className="h-4 bg-slate-200 rounded w-3/4" /></td>
                  ))}
                </tr>
              ))
            ) : error ? (
              <tr><td colSpan={6} className="px-4 py-12 text-center text-sm text-red-600">Failed to load evidence data.</td></tr>
            ) : data?.items.length === 0 ? (
              <tr>
                <td colSpan={6} className="px-4 py-12 text-center">
                  <span className="material-symbols-outlined text-4xl text-ink-200 mb-2 block">fact_check</span>
                  <p className="text-sm text-ink-500">No source evidence found — evidence is created during extraction and import.</p>
                </td>
              </tr>
            ) : (
              data?.items.map((item) => (
                <tr key={item.krithiId} className="border-b border-border-light hover:bg-slate-50/50 transition-colors">
                  <td className="px-4 py-3">
                    <div>
                      <span className="text-sm font-medium text-ink-800">{item.krithiTitle}</span>
                      {(item.krithiRaga || item.krithiTala) && (
                        <p className="text-xs text-ink-400 mt-0.5">
                          {[item.krithiRaga, item.krithiTala].filter(Boolean).join(' · ')}
                        </p>
                      )}
                    </div>
                  </td>
                  <td className="px-4 py-3">
                    <div className="flex items-center gap-1.5">
                      <span className="text-sm font-bold text-ink-700 tabular-nums">{item.sourceCount}</span>
                      <TierBadge tier={item.topSourceTier} size="sm" />
                    </div>
                  </td>
                  <td className="px-4 py-3">
                    <span className="text-sm text-ink-700">{item.topSourceName || '—'}</span>
                  </td>
                  <td className="px-4 py-3">
                    <div className="flex flex-wrap gap-1">
                      {item.contributedFields.map((f) => (
                        <span key={f} className="text-[10px] bg-slate-100 text-ink-600 px-1.5 py-0.5 rounded-full font-medium capitalize">{f}</span>
                      ))}
                    </div>
                  </td>
                  <td className="px-4 py-3" style={{ width: 120 }}>
                    <ConfidenceBar value={item.avgConfidence} showLabel size="sm" />
                  </td>
                  <td className="px-4 py-3">
                    {item.votingStatus ? (
                      <span className="text-xs font-semibold text-primary">{item.votingStatus}</span>
                    ) : (
                      <span className="text-xs text-ink-400">No Vote</span>
                    )}
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>

        {data && data.items.length > 0 && (
          <div className="px-4 py-3 border-t border-border-light bg-slate-50 text-sm text-ink-500">
            Showing {data.items.length} of {data.total} Krithis with evidence
          </div>
        )}
      </div>
    </div>
  );
};

export default SourceEvidencePage;
