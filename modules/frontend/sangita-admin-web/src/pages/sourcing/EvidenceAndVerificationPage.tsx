import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import type { EvidenceFilterParams, VotingFilterParams } from '../../types/sourcing';
import { useEvidenceList, useVotingList, useVotingStats } from '../../hooks/useSourcingQueries';
import { TierBadge, ConfidenceBar, MetricCard, StructureVisualiser } from '../../components/sourcing/shared';

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

const EvidenceAndVerificationPage: React.FC = () => {
  const navigate = useNavigate();

  // Evidence state
  const [evidenceFilters, setEvidenceFilters] = useState<EvidenceFilterParams>({ page: 1, pageSize: 20 });
  const { data: evidenceData, isLoading: evidenceLoading, error: evidenceError } = useEvidenceList(evidenceFilters);
  const totalPages = evidenceData ? Math.ceil(evidenceData.total / (evidenceFilters.pageSize ?? 20)) : 0;
  const currentPage = evidenceFilters.page ?? 1;

  // Voting state
  const [votingFilters] = useState<VotingFilterParams>({});
  const { data: votingData, isLoading: votingLoading } = useVotingList(votingFilters);
  const { data: votingStats, isLoading: statsLoading } = useVotingStats();

  const hasVotingData = votingStats && votingStats.total > 0;

  return (
    <div className="space-y-10">
      {/* ─── Evidence Section ─── */}
      <section>
        <div className="flex items-center justify-between mb-6">
          <div>
            <h1 className="text-2xl font-display font-bold text-ink-900">Evidence</h1>
            <p className="text-sm text-ink-500 mt-1">See which sources contributed to each composition and compare their data</p>
          </div>
        </div>

        {/* Filter Bar */}
        <div className="bg-white rounded-xl border border-border-light p-4 mb-6">
          <div className="flex flex-wrap items-center gap-3">
            <div className="flex items-center gap-2">
              <span className="text-xs font-semibold text-ink-500 uppercase">Minimum Sources</span>
              <input
                type="number"
                min={1}
                value={evidenceFilters.minSourceCount ?? ''}
                onChange={(e) => setEvidenceFilters({ ...evidenceFilters, minSourceCount: e.target.value ? parseInt(e.target.value) : undefined, page: 1 })}
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
                  value={evidenceFilters.search || ''}
                  onChange={(e) => setEvidenceFilters({ ...evidenceFilters, search: e.target.value || undefined, page: 1 })}
                  className="w-full pl-9 pr-3 py-2 text-sm border border-border-light rounded-lg focus:outline-none focus:ring-2 focus:ring-primary/20"
                />
              </div>
            </div>
          </div>
        </div>

        <div className="bg-white rounded-xl border border-border-light overflow-hidden">
          <table className="w-full">
            <thead>
              <tr className="border-b border-border-light bg-slate-50">
                <th className="px-4 py-3 text-left text-xs font-semibold text-ink-500 uppercase tracking-wider">Krithi</th>
                <th className="px-4 py-3 text-left text-xs font-semibold text-ink-500 uppercase tracking-wider">Sources</th>
                <th className="px-4 py-3 text-left text-xs font-semibold text-ink-500 uppercase tracking-wider">Top Source</th>
                <th className="px-4 py-3 text-left text-xs font-semibold text-ink-500 uppercase tracking-wider">Fields Provided</th>
                <th className="px-4 py-3 text-left text-xs font-semibold text-ink-500 uppercase tracking-wider">Reliability</th>
                <th className="px-4 py-3 text-left text-xs font-semibold text-ink-500 uppercase tracking-wider">Verification</th>
              </tr>
            </thead>
            <tbody>
              {evidenceLoading ? (
                Array.from({ length: 5 }).map((_, i) => (
                  <tr key={i} className="border-b border-border-light animate-pulse">
                    {Array.from({ length: 6 }).map((_, j) => (
                      <td key={j} className="px-4 py-3"><div className="h-4 bg-slate-200 rounded w-3/4" /></td>
                    ))}
                  </tr>
                ))
              ) : evidenceError ? (
                <tr><td colSpan={6} className="px-4 py-12 text-center text-sm text-red-600">Failed to load evidence data.</td></tr>
              ) : evidenceData?.items.length === 0 ? (
                <tr>
                  <td colSpan={6} className="px-4 py-12 text-center">
                    <span className="material-symbols-outlined text-4xl text-ink-200 mb-2 block">fact_check</span>
                    <p className="text-sm text-ink-500">No evidence found yet. Evidence appears as compositions are processed from multiple sources.</p>
                  </td>
                </tr>
              ) : (
                evidenceData?.items.map((item) => (
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
                        <span className="text-xs text-ink-400">Not yet verified</span>
                      )}
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
          {evidenceData && evidenceData.total > 0 && (
            <div className="px-4 py-3 border-t border-border-light bg-slate-50 flex items-center justify-between">
              <span className="text-sm text-ink-500">
                Showing {(currentPage - 1) * (evidenceFilters.pageSize ?? 20) + 1}–{Math.min(currentPage * (evidenceFilters.pageSize ?? 20), evidenceData.total)} of {evidenceData.total}
              </span>
              {totalPages > 1 && (
                <div className="flex items-center gap-1">
                  <button
                    onClick={() => setEvidenceFilters({ ...evidenceFilters, page: currentPage - 1 })}
                    disabled={currentPage <= 1}
                    className="p-1.5 rounded-lg text-ink-500 hover:bg-slate-200 disabled:opacity-30 disabled:cursor-not-allowed transition-colors"
                  >
                    <span className="material-symbols-outlined text-lg">chevron_left</span>
                  </button>
                  {Array.from({ length: Math.min(totalPages, 7) }, (_, i) => {
                    let pageNum: number;
                    if (totalPages <= 7) {
                      pageNum = i + 1;
                    } else if (currentPage <= 4) {
                      pageNum = i + 1;
                    } else if (currentPage >= totalPages - 3) {
                      pageNum = totalPages - 6 + i;
                    } else {
                      pageNum = currentPage - 3 + i;
                    }
                    return (
                      <button
                        key={pageNum}
                        onClick={() => setEvidenceFilters({ ...evidenceFilters, page: pageNum })}
                        className={`min-w-[32px] h-8 rounded-lg text-sm font-medium transition-colors ${
                          pageNum === currentPage
                            ? 'bg-primary text-white'
                            : 'text-ink-600 hover:bg-slate-200'
                        }`}
                      >
                        {pageNum}
                      </button>
                    );
                  })}
                  <button
                    onClick={() => setEvidenceFilters({ ...evidenceFilters, page: currentPage + 1 })}
                    disabled={currentPage >= totalPages}
                    className="p-1.5 rounded-lg text-ink-500 hover:bg-slate-200 disabled:opacity-30 disabled:cursor-not-allowed transition-colors"
                  >
                    <span className="material-symbols-outlined text-lg">chevron_right</span>
                  </button>
                </div>
              )}
            </div>
          )}
        </div>
      </section>

      {/* ─── Verification Section (hidden when no data) ─── */}
      {hasVotingData && (
        <section>
          <div className="flex items-center justify-between mb-6">
            <div>
              <h2 className="text-xl font-display font-bold text-ink-900">Verification</h2>
              <p className="text-sm text-ink-500 mt-1">How agreement was reached on each composition's structure</p>
            </div>
          </div>

          {/* Summary Cards */}
          <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-6 gap-3 mb-6">
            <MetricCard label="All Decisions" value={votingStats?.total ?? '—'} loading={statsLoading} />
            <MetricCard label="Unanimous" value={votingStats?.unanimous ?? '—'} loading={statsLoading} />
            <MetricCard label="Majority" value={votingStats?.majority ?? '—'} loading={statsLoading} />
            <MetricCard label="Authority" value={votingStats?.authorityOverride ?? '—'} loading={statsLoading} />
            <MetricCard label="Single Source" value={votingStats?.singleSource ?? '—'} loading={statsLoading} />
            <MetricCard label="Manual" value={votingStats?.manual ?? '—'} loading={statsLoading} />
          </div>

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
                {votingLoading ? (
                  Array.from({ length: 3 }).map((_, i) => (
                    <tr key={i} className="border-b border-border-light animate-pulse">
                      {Array.from({ length: 7 }).map((_, j) => (
                        <td key={j} className="px-4 py-3"><div className="h-4 bg-slate-200 rounded w-3/4" /></td>
                      ))}
                    </tr>
                  ))
                ) : votingData?.items.length === 0 ? (
                  <tr>
                    <td colSpan={7} className="px-4 py-12 text-center">
                      <span className="material-symbols-outlined text-4xl text-ink-200 mb-2 block">how_to_vote</span>
                      <p className="text-sm text-ink-500">No verification decisions yet.</p>
                    </td>
                  </tr>
                ) : (
                  votingData?.items.map((decision) => {
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
            {votingData && votingData.items.length > 0 && (
              <div className="px-4 py-3 border-t border-border-light bg-slate-50 text-sm text-ink-500">
                Showing {votingData.items.length} of {votingData.total} decisions
              </div>
            )}
          </div>
        </section>
      )}
    </div>
  );
};

export default EvidenceAndVerificationPage;
