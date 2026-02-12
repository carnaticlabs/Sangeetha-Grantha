import React from 'react';
import { useNavigate } from 'react-router-dom';
import { useExtractionStats, useVotingStats, useQualitySummary, useSourceList } from '../../hooks/useSourcingQueries';
import { MetricCard } from '../../components/sourcing/shared';

const SourcingDashboardPage: React.FC = () => {
  const navigate = useNavigate();
  const { data: sources, isLoading: sourcesLoading } = useSourceList({ pageSize: 1 });
  const { data: extractionStats, isLoading: extractionLoading } = useExtractionStats(30000);
  const { data: votingStats, isLoading: votingLoading } = useVotingStats();
  const { data: qualitySummary, isLoading: qualityLoading } = useQualitySummary(30000);

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-2xl font-display font-bold text-ink-900">Sourcing & Quality</h1>
          <p className="text-sm text-ink-500 mt-1">Pipeline health, recent activity, and quick actions</p>
        </div>
      </div>

      {/* Pipeline Health Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 mb-8">
        <MetricCard
          label="Sources"
          value={sources?.total ?? '—'}
          subtitle={`${sources?.items.length ?? 0} active`}
          icon={<span className="material-symbols-outlined text-lg text-ink-300">source</span>}
          onClick={() => navigate('/admin/sourcing/sources')}
          loading={sourcesLoading}
        />
        <MetricCard
          label="Extraction Queue"
          value={extractionStats?.total ?? '—'}
          subtitle={extractionStats ? `${extractionStats.pending} pending, ${extractionStats.processing} processing` : undefined}
          icon={<span className="material-symbols-outlined text-lg text-ink-300">manufacturing</span>}
          onClick={() => navigate('/admin/sourcing/extractions')}
          loading={extractionLoading}
        />
        <MetricCard
          label="Voting Decisions"
          value={votingStats?.total ?? '—'}
          subtitle={votingStats?.total ? `${((votingStats.manual / votingStats.total) * 100).toFixed(0)}% manual review` : undefined}
          icon={<span className="material-symbols-outlined text-lg text-ink-300">how_to_vote</span>}
          onClick={() => navigate('/admin/sourcing/voting')}
          loading={votingLoading}
        />
        <MetricCard
          label="Corpus Quality"
          value={qualitySummary ? `${qualitySummary.avgQualityScore.toFixed(2)}` : '—'}
          subtitle={qualitySummary ? `${qualitySummary.enrichmentCoveragePercent.toFixed(0)}% coverage` : undefined}
          icon={<span className="material-symbols-outlined text-lg text-ink-300">analytics</span>}
          onClick={() => navigate('/admin/sourcing/quality')}
          loading={qualityLoading}
        />
      </div>

      {/* Activity & Top Sources */}
      <div className="grid grid-cols-1 lg:grid-cols-5 gap-6 mb-8">
        <div className="lg:col-span-3 bg-white rounded-xl border border-border-light p-5">
          <h2 className="text-sm font-semibold text-ink-700 uppercase tracking-wider mb-4">Recent Activity</h2>
          <div className="space-y-3">
            {extractionStats && extractionStats.done > 0 ? (
              <div className="flex items-center gap-3 py-2">
                <div className="w-8 h-8 rounded-full bg-emerald-100 flex items-center justify-center flex-shrink-0">
                  <span className="material-symbols-outlined text-sm text-emerald-600">check_circle</span>
                </div>
                <div className="flex-1">
                  <p className="text-sm text-ink-700">{extractionStats.done} extractions completed</p>
                  <p className="text-xs text-ink-400">{extractionStats.throughputPerHour.toFixed(1)}/hr throughput</p>
                </div>
              </div>
            ) : (
              <p className="text-sm text-ink-400 italic py-4">No recent activity. Submit an extraction request to get started.</p>
            )}

            {extractionStats && extractionStats.failed > 0 && (
              <div className="flex items-center gap-3 py-2">
                <div className="w-8 h-8 rounded-full bg-red-100 flex items-center justify-center flex-shrink-0">
                  <span className="material-symbols-outlined text-sm text-red-600">error</span>
                </div>
                <div className="flex-1">
                  <p className="text-sm text-ink-700">{extractionStats.failed} failed extractions need attention</p>
                  <button
                    onClick={() => navigate('/admin/sourcing/extractions')}
                    className="text-xs text-primary hover:underline"
                  >
                    View queue
                  </button>
                </div>
              </div>
            )}

            {qualitySummary && qualitySummary.multiSourceCount > 0 && (
              <div className="flex items-center gap-3 py-2">
                <div className="w-8 h-8 rounded-full bg-blue-100 flex items-center justify-center flex-shrink-0">
                  <span className="material-symbols-outlined text-sm text-blue-600">fact_check</span>
                </div>
                <div className="flex-1">
                  <p className="text-sm text-ink-700">{qualitySummary.multiSourceCount} Krithis have multi-source evidence</p>
                  <p className="text-xs text-ink-400">{qualitySummary.multiSourcePercent.toFixed(1)}% of corpus</p>
                </div>
              </div>
            )}
          </div>
        </div>

        <div className="lg:col-span-2 bg-white rounded-xl border border-border-light p-5">
          <h2 className="text-sm font-semibold text-ink-700 uppercase tracking-wider mb-4">Quick Stats</h2>
          <div className="space-y-4">
            <div className="flex items-center justify-between">
              <span className="text-sm text-ink-600">Total Krithis</span>
              <span className="text-sm font-bold text-ink-800 tabular-nums">{qualitySummary?.totalKrithis ?? '—'}</span>
            </div>
            <div className="flex items-center justify-between">
              <span className="text-sm text-ink-600">Multi-Source</span>
              <span className="text-sm font-bold text-ink-800 tabular-nums">{qualitySummary?.multiSourceCount ?? '—'}</span>
            </div>
            <div className="flex items-center justify-between">
              <span className="text-sm text-ink-600">Structural Consensus</span>
              <span className="text-sm font-bold text-ink-800 tabular-nums">{qualitySummary?.consensusCount ?? '—'}</span>
            </div>
            <div className="flex items-center justify-between">
              <span className="text-sm text-ink-600">Voting Decisions</span>
              <span className="text-sm font-bold text-ink-800 tabular-nums">{votingStats?.total ?? '—'}</span>
            </div>
          </div>
        </div>
      </div>

      {/* Quick Actions */}
      <div className="flex flex-wrap gap-3">
        <button
          onClick={() => navigate('/admin/sourcing/extractions')}
          className="inline-flex items-center gap-2 px-4 py-2 bg-primary text-white rounded-lg text-sm font-medium hover:bg-primary/90 transition-colors"
        >
          <span className="material-symbols-outlined text-lg">add_circle</span>
          Start New Extraction
        </button>
        <button
          onClick={() => navigate('/admin/sourcing/quality')}
          className="inline-flex items-center gap-2 px-4 py-2 bg-white border border-border-light text-ink-700 rounded-lg text-sm font-medium hover:bg-slate-50 transition-colors"
        >
          <span className="material-symbols-outlined text-lg">query_stats</span>
          View Quality Dashboard
        </button>
        <button
          onClick={() => navigate('/admin/sourcing/voting')}
          className="inline-flex items-center gap-2 px-4 py-2 bg-white border border-border-light text-ink-700 rounded-lg text-sm font-medium hover:bg-slate-50 transition-colors"
        >
          <span className="material-symbols-outlined text-lg">rate_review</span>
          View Voting Decisions
        </button>
      </div>
    </div>
  );
};

export default SourcingDashboardPage;
