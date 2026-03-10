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
          <h1 className="text-2xl font-display font-bold text-ink-900">Collection Review</h1>
          <p className="text-sm text-ink-500 mt-1">Overview of your collection's sources, processing, and quality</p>
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
          label="Processing Queue"
          value={extractionStats?.total ?? '—'}
          subtitle={extractionStats ? `${extractionStats.pending} pending, ${extractionStats.processing} processing` : undefined}
          icon={<span className="material-symbols-outlined text-lg text-ink-300">manufacturing</span>}
          onClick={() => navigate('/admin/sourcing/sources')}
          loading={extractionLoading}
        />
        <MetricCard
          label="Verification Decisions"
          value={votingStats?.total ?? '—'}
          subtitle={votingStats?.total ? `${((votingStats.manual / votingStats.total) * 100).toFixed(0)}% manual review` : undefined}
          icon={<span className="material-symbols-outlined text-lg text-ink-300">how_to_vote</span>}
          onClick={() => navigate('/admin/sourcing/evidence')}
          loading={votingLoading}
        />
        <MetricCard
          label="Collection Quality"
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
                  <p className="text-sm text-ink-700">{extractionStats.done} sources processed</p>
                  <p className="text-xs text-ink-400">{extractionStats.throughputPerHour.toFixed(1)} processed per hour</p>
                </div>
              </div>
            ) : (
              <p className="text-sm text-ink-400 italic py-4">No recent activity. Process a source to get started.</p>
            )}

            {extractionStats && extractionStats.failed > 0 && (
              <div className="flex items-center gap-3 py-2">
                <div className="w-8 h-8 rounded-full bg-red-100 flex items-center justify-center flex-shrink-0">
                  <span className="material-symbols-outlined text-sm text-red-600">error</span>
                </div>
                <div className="flex-1">
                  <p className="text-sm text-ink-700">{extractionStats.failed} failed items need attention</p>
                  <button
                    onClick={() => navigate('/admin/sourcing/sources')}
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
                  <p className="text-sm text-ink-700">{qualitySummary.multiSourceCount} compositions verified by multiple sources</p>
                  <p className="text-xs text-ink-400">{qualitySummary.multiSourcePercent.toFixed(1)}% of collection</p>
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
              <span className="text-sm text-ink-600">Multiple Sources</span>
              <span className="text-sm font-bold text-ink-800 tabular-nums">{qualitySummary?.multiSourceCount ?? '—'}</span>
            </div>
            <div className="flex items-center justify-between">
              <span className="text-sm text-ink-600">Structure Agreed</span>
              <span className="text-sm font-bold text-ink-800 tabular-nums">{qualitySummary?.consensusCount ?? '—'}</span>
            </div>
            <div className="flex items-center justify-between">
              <span className="text-sm text-ink-600">Verification Decisions</span>
              <span className="text-sm font-bold text-ink-800 tabular-nums">{votingStats?.total ?? '—'}</span>
            </div>
          </div>
        </div>
      </div>

      {/* Quick Actions */}
      <div className="flex flex-wrap gap-3">
        <button
          onClick={() => navigate('/admin/sourcing/sources')}
          className="inline-flex items-center gap-2 px-4 py-2 bg-primary text-white rounded-lg text-sm font-medium hover:bg-primary/90 transition-colors"
        >
          <span className="material-symbols-outlined text-lg">add_circle</span>
          Process New Source
        </button>
        <button
          onClick={() => navigate('/admin/sourcing/quality')}
          className="inline-flex items-center gap-2 px-4 py-2 bg-white border border-border-light text-ink-700 rounded-lg text-sm font-medium hover:bg-slate-50 transition-colors"
        >
          <span className="material-symbols-outlined text-lg">query_stats</span>
          View Collection Health
        </button>
        <button
          onClick={() => navigate('/admin/sourcing/evidence')}
          className="inline-flex items-center gap-2 px-4 py-2 bg-white border border-border-light text-ink-700 rounded-lg text-sm font-medium hover:bg-slate-50 transition-colors"
        >
          <span className="material-symbols-outlined text-lg">rate_review</span>
          View Verification Decisions
        </button>
      </div>
    </div>
  );
};

export default SourcingDashboardPage;
