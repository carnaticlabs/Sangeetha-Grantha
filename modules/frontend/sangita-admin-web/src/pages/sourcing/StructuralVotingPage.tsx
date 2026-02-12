import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import type { VotingFilterParams } from '../../types/sourcing';
import { useVotingList, useVotingStats } from '../../hooks/useSourcingQueries';
import { MetricCard, StructureVisualiser } from '../../components/sourcing/shared';

const consensusBadge: Record<string, { color: string; label: string }> = {
  UNANIMOUS: { color: 'bg-emerald-100 text-emerald-700', label: 'Unanimous' },
  MAJORITY: { color: 'bg-blue-100 text-blue-700', label: 'Majority' },
  AUTHORITY_OVERRIDE: { color: 'bg-amber-100 text-amber-700', label: 'Authority' },
  SINGLE_SOURCE: { color: 'bg-slate-100 text-slate-600', label: 'Single' },
  MANUAL: { color: 'bg-purple-100 text-purple-700', label: 'Manual' },
};

const confidenceBadge: Record<string, string> = {
  HIGH: 'bg-emerald-100 text-emerald-700',
  MEDIUM: 'bg-amber-100 text-amber-700',
  LOW: 'bg-red-100 text-red-700',
};

const StructuralVotingPage: React.FC = () => {
  const navigate = useNavigate();
  const [filters] = useState<VotingFilterParams>({});
  const { data, isLoading } = useVotingList(filters);
  const { data: stats, isLoading: statsLoading } = useVotingStats();

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-2xl font-display font-bold text-ink-900">Structural Voting</h1>
          <p className="text-sm text-ink-500 mt-1">Cross-source voting decisions and manual override controls</p>
        </div>
      </div>

      {/* Summary Cards */}
      <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-6 gap-3 mb-6">
        <MetricCard label="Total" value={stats?.total ?? '—'} loading={statsLoading} />
        <MetricCard label="Unanimous" value={stats?.unanimous ?? '—'} loading={statsLoading} />
        <MetricCard label="Majority" value={stats?.majority ?? '—'} loading={statsLoading} />
        <MetricCard label="Authority" value={stats?.authorityOverride ?? '—'} loading={statsLoading} />
        <MetricCard label="Single Source" value={stats?.singleSource ?? '—'} loading={statsLoading} />
        <MetricCard label="Manual" value={stats?.manual ?? '—'} loading={statsLoading} />
      </div>

      {/* Table */}
      <div className="bg-white rounded-xl border border-border-light overflow-hidden">
        <table className="w-full">
          <thead>
            <tr className="border-b border-border-light bg-slate-50">
              <th className="px-4 py-3 text-left text-xs font-semibold text-ink-500 uppercase tracking-wider">Krithi</th>
              <th className="px-4 py-3 text-left text-xs font-semibold text-ink-500 uppercase tracking-wider">Voted At</th>
              <th className="px-4 py-3 text-left text-xs font-semibold text-ink-500 uppercase tracking-wider">Sources</th>
              <th className="px-4 py-3 text-left text-xs font-semibold text-ink-500 uppercase tracking-wider">Consensus</th>
              <th className="px-4 py-3 text-left text-xs font-semibold text-ink-500 uppercase tracking-wider">Structure</th>
              <th className="px-4 py-3 text-left text-xs font-semibold text-ink-500 uppercase tracking-wider">Confidence</th>
              <th className="px-4 py-3 text-left text-xs font-semibold text-ink-500 uppercase tracking-wider">Actions</th>
            </tr>
          </thead>
          <tbody>
            {isLoading ? (
              Array.from({ length: 5 }).map((_, i) => (
                <tr key={i} className="border-b border-border-light animate-pulse">
                  {Array.from({ length: 7 }).map((_, j) => (
                    <td key={j} className="px-4 py-3"><div className="h-4 bg-slate-200 rounded w-3/4" /></td>
                  ))}
                </tr>
              ))
            ) : data?.items.length === 0 ? (
              <tr>
                <td colSpan={7} className="px-4 py-12 text-center">
                  <span className="material-symbols-outlined text-4xl text-ink-200 mb-2 block">how_to_vote</span>
                  <p className="text-sm text-ink-500">No structural voting decisions yet — voting occurs automatically during multi-source extraction.</p>
                </td>
              </tr>
            ) : (
              data?.items.map((decision) => {
                const consensus = consensusBadge[decision.consensusType] || consensusBadge.SINGLE_SOURCE;
                const conf = confidenceBadge[decision.confidence] || confidenceBadge.MEDIUM;
                let structure: { sectionType: string; orderIndex: number; label?: string | null }[] = [];
                try { structure = JSON.parse(decision.consensusStructure); } catch { /* ignore */ }

                return (
                  <tr
                    key={decision.id}
                    className="border-b border-border-light hover:bg-slate-50/50 cursor-pointer transition-colors"
                    onClick={() => navigate(`/admin/sourcing/voting/${decision.id}`)}
                  >
                    <td className="px-4 py-3">
                      <span className="text-sm font-medium text-ink-800">{decision.krithiTitle}</span>
                    </td>
                    <td className="px-4 py-3">
                      <span className="text-xs text-ink-500">{new Date(decision.votedAt as string).toLocaleDateString()}</span>
                    </td>
                    <td className="px-4 py-3">
                      <span className="text-sm font-medium text-ink-700 tabular-nums">{decision.sourceCount}</span>
                    </td>
                    <td className="px-4 py-3">
                      <span className={`inline-flex items-center text-xs font-semibold px-2 py-0.5 rounded-full ${consensus.color}`}>
                        {consensus.label}
                      </span>
                    </td>
                    <td className="px-4 py-3">
                      {structure.length > 0 ? (
                        <StructureVisualiser sections={structure} compact />
                      ) : (
                        <span className="text-xs text-ink-400">—</span>
                      )}
                    </td>
                    <td className="px-4 py-3">
                      <span className={`inline-flex items-center text-xs font-semibold px-2 py-0.5 rounded-full ${conf}`}>
                        {decision.confidence}
                      </span>
                    </td>
                    <td className="px-4 py-3" onClick={(e) => e.stopPropagation()}>
                      <button
                        onClick={() => navigate(`/admin/sourcing/voting/${decision.id}`)}
                        className="p-1 text-ink-400 hover:text-primary transition-colors"
                        title="View Detail"
                      >
                        <span className="material-symbols-outlined text-lg">visibility</span>
                      </button>
                    </td>
                  </tr>
                );
              })
            )}
          </tbody>
        </table>

        {data && data.items.length > 0 && (
          <div className="px-4 py-3 border-t border-border-light bg-slate-50 text-sm text-ink-500">
            Showing {data.items.length} of {data.total} decisions
          </div>
        )}
      </div>
    </div>
  );
};

export default StructuralVotingPage;
