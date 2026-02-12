import React from 'react';
import { useQualitySummary, useQualityDistribution, useQualityCoverage, useQualityGaps, useAuditResults, useRunAudit } from '../../hooks/useSourcingQueries';
import { MetricCard, ProgressBarRow } from '../../components/sourcing/shared';

const QualityDashboardPage: React.FC = () => {
  const { data: summary, isLoading: summaryLoading } = useQualitySummary();
  const { data: distribution } = useQualityDistribution();
  const { data: coverage } = useQualityCoverage();
  const { data: gaps } = useQualityGaps();
  const { data: auditResults } = useAuditResults();
  const runAuditMutation = useRunAudit();

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-2xl font-display font-bold text-ink-900">Corpus Quality Dashboard</h1>
          <p className="text-sm text-ink-500 mt-1">Quality metrics, coverage analysis, and data gap tracking</p>
        </div>
        <button
          onClick={() => runAuditMutation.mutate()}
          disabled={runAuditMutation.isPending}
          className="inline-flex items-center gap-2 px-4 py-2.5 bg-primary text-white rounded-lg text-sm font-medium hover:bg-primary/90 disabled:opacity-50 transition-colors"
        >
          <span className="material-symbols-outlined text-lg">{runAuditMutation.isPending ? 'hourglass_empty' : 'query_stats'}</span>
          {runAuditMutation.isPending ? 'Running...' : 'Run Audit Now'}
        </button>
      </div>

      {/* KPI Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-5 gap-4 mb-8">
        <MetricCard
          label="Total Krithis"
          value={summary?.totalKrithis ?? '—'}
          loading={summaryLoading}
        />
        <MetricCard
          label="Multi-Source"
          value={summary ? `${summary.multiSourceCount}` : '—'}
          subtitle={summary ? `${summary.multiSourcePercent.toFixed(1)}%` : undefined}
          loading={summaryLoading}
        />
        <MetricCard
          label="Structural Consensus"
          value={summary ? `${summary.consensusCount}` : '—'}
          subtitle={summary ? `${summary.consensusPercent.toFixed(1)}%` : undefined}
          loading={summaryLoading}
        />
        <MetricCard
          label="Avg Quality Score"
          value={summary ? summary.avgQualityScore.toFixed(3) : '—'}
          loading={summaryLoading}
        />
        <MetricCard
          label="Enrichment Coverage"
          value={summary ? `${summary.enrichmentCoveragePercent.toFixed(1)}%` : '—'}
          loading={summaryLoading}
        />
      </div>

      {/* Quality Distribution + Tier Coverage */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-8">
        <div className="bg-white rounded-xl border border-border-light p-5">
          <h2 className="text-lg font-semibold text-ink-800 mb-4">Quality Score Distribution</h2>
          {distribution?.buckets && distribution.buckets.length > 0 ? (
            <div className="h-48 flex items-end gap-2">
              {distribution.buckets.map((bucket, i) => {
                const maxCount = Math.max(...distribution.buckets.map(b => b.count), 1);
                const height = (bucket.count / maxCount) * 100;
                const colors = ['bg-red-400', 'bg-orange-400', 'bg-amber-400', 'bg-lime-400', 'bg-emerald-400'];
                return (
                  <div key={i} className="flex-1 flex flex-col items-center gap-1">
                    <span className="text-xs font-bold text-ink-600 tabular-nums">{bucket.count}</span>
                    <div className={`w-full ${colors[i] || 'bg-slate-300'} rounded-t transition-all`} style={{ height: `${height}%`, minHeight: 4 }} />
                    <span className="text-[10px] text-ink-400">{bucket.label}</span>
                  </div>
                );
              })}
            </div>
          ) : (
            <div className="h-48 flex items-center justify-center text-sm text-ink-400 italic">No quality score data yet</div>
          )}
        </div>
        <div className="bg-white rounded-xl border border-border-light p-5">
          <h2 className="text-lg font-semibold text-ink-800 mb-4">Source Tier Coverage</h2>
          {coverage?.tierCoverage && coverage.tierCoverage.length > 0 ? (
            <div className="h-48 flex items-end gap-3">
              {coverage.tierCoverage.map((tc) => {
                const maxCount = Math.max(...coverage.tierCoverage.map(t => t.count), 1);
                const height = (tc.count / maxCount) * 100;
                const tierColors = ['bg-amber-400', 'bg-slate-400', 'bg-orange-400', 'bg-blue-400', 'bg-slate-300'];
                return (
                  <div key={tc.tier} className="flex-1 flex flex-col items-center gap-1">
                    <span className="text-xs font-bold text-ink-600 tabular-nums">{tc.count}</span>
                    <div className={`w-full ${tierColors[tc.tier - 1] || 'bg-slate-300'} rounded-t transition-all`} style={{ height: `${height}%`, minHeight: 4 }} />
                    <span className="text-[10px] text-ink-400">T{tc.tier}</span>
                  </div>
                );
              })}
            </div>
          ) : (
            <div className="h-48 flex items-center justify-center text-sm text-ink-400 italic">No tier coverage data yet</div>
          )}
        </div>
      </div>

      {/* Enrichment Phase Progress */}
      <div className="bg-white rounded-xl border border-border-light p-5 mb-8">
        <h2 className="text-lg font-semibold text-ink-800 mb-4">Enrichment Phase Progress</h2>
        {coverage?.phaseProgress && coverage.phaseProgress.length > 0 ? (
          <div className="space-y-3">
            {coverage.phaseProgress.map((phase) => (
              <ProgressBarRow
                key={phase.phase}
                label={`P${phase.phase}: ${phase.label}`}
                target={phase.target}
                actual={phase.completed}
                inProgress={phase.inProgress}
              />
            ))}
          </div>
        ) : (
          <p className="text-sm text-ink-400 italic">Phase progress will be displayed once enrichment tracking is configured.</p>
        )}
      </div>

      {/* Data Gaps */}
      <div className="grid grid-cols-1 lg:grid-cols-5 gap-6 mb-8">
        <div className="lg:col-span-3 bg-white rounded-xl border border-border-light p-5">
          <h2 className="text-lg font-semibold text-ink-800 mb-4">Data Gaps & Priorities</h2>
          {gaps?.gaps && gaps.gaps.length > 0 ? (
            <div className="space-y-1">
              {gaps.gaps.map((gap, i) => (
                <div key={i} className="flex items-center justify-between py-2.5 border-b border-border-light last:border-b-0">
                  <div>
                    <span className="text-sm text-ink-700">{gap.label}</span>
                    <p className="text-xs text-ink-400">{gap.description}</p>
                  </div>
                  <span className="text-sm font-bold text-ink-800 tabular-nums bg-slate-100 px-2.5 py-0.5 rounded-full">{gap.count}</span>
                </div>
              ))}
            </div>
          ) : (
            <p className="text-sm text-ink-400 italic">No data gap analysis available yet. Run an audit to generate results.</p>
          )}
        </div>
        <div className="lg:col-span-2 bg-white rounded-xl border border-border-light p-5">
          <h2 className="text-lg font-semibold text-ink-800 mb-4">Composer Coverage Matrix</h2>
          <p className="text-sm text-ink-400 italic">Heatmap will be displayed once composer-field coverage data is available.</p>
        </div>
      </div>

      {/* Audit Results */}
      <div className="bg-white rounded-xl border border-border-light p-5">
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-lg font-semibold text-ink-800">Audit Results</h2>
          {auditResults?.lastRunAt && (
            <span className="text-xs text-ink-400">
              Last run: {new Date(auditResults.lastRunAt).toLocaleString()}
            </span>
          )}
        </div>
        {auditResults?.results && auditResults.results.length > 0 ? (
          <div className="space-y-1">
            {auditResults.results.map((result, i) => (
              <div key={i} className="flex items-center justify-between py-2.5 border-b border-border-light last:border-b-0">
                <span className="text-sm text-ink-700">{result.queryName}</span>
                <div className="flex items-center gap-2">
                  <span className="text-sm font-bold text-ink-800 tabular-nums">{result.violationCount}</span>
                  <span className={`material-symbols-outlined text-sm ${
                    result.trend === 'up' ? 'text-red-500' : result.trend === 'down' ? 'text-emerald-500' : 'text-ink-300'
                  }`}>
                    {result.trend === 'up' ? 'trending_up' : result.trend === 'down' ? 'trending_down' : 'trending_flat'}
                  </span>
                </div>
              </div>
            ))}
          </div>
        ) : (
          <p className="text-sm text-ink-400 italic">No audit results yet — click "Run Audit Now" to generate the first analysis.</p>
        )}
      </div>
    </div>
  );
};

export default QualityDashboardPage;
