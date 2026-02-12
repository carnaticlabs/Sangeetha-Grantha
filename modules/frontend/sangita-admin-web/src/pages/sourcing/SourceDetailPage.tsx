import React from 'react';
import { useParams, Link } from 'react-router-dom';
import { useSourceDetail } from '../../hooks/useSourcingQueries';
import { TierBadge, FormatPill, ConfidenceBar, MetricCard } from '../../components/sourcing/shared';

const SourceDetailPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const { data: source, isLoading, error } = useSourceDetail(id!);

  if (isLoading) {
    return (
      <div>
        <div className="flex items-center gap-3 mb-6">
          <Link to="/admin/sourcing/sources" className="text-ink-400 hover:text-primary transition-colors">
            <span className="material-symbols-outlined text-xl">arrow_back</span>
          </Link>
          <div className="animate-pulse">
            <div className="h-7 w-48 bg-slate-200 rounded mb-1" />
            <div className="h-4 w-32 bg-slate-100 rounded" />
          </div>
        </div>
        <div className="space-y-6">
          {[1, 2, 3].map((i) => (
            <div key={i} className="bg-white rounded-xl border border-border-light p-5 animate-pulse">
              <div className="h-5 w-40 bg-slate-200 rounded mb-4" />
              <div className="space-y-3">
                <div className="h-4 bg-slate-200 rounded w-3/4" />
                <div className="h-4 bg-slate-100 rounded w-1/2" />
              </div>
            </div>
          ))}
        </div>
      </div>
    );
  }

  if (error || !source) {
    return (
      <div>
        <Link to="/admin/sourcing/sources" className="inline-flex items-center gap-1 text-sm text-primary hover:underline mb-4">
          <span className="material-symbols-outlined text-lg">arrow_back</span> Back to Sources
        </Link>
        <div className="bg-red-50 border border-red-200 rounded-xl p-6 text-center">
          <span className="material-symbols-outlined text-3xl text-red-400 mb-2 block">error_outline</span>
          <p className="text-sm text-red-700">Source not found or failed to load.</p>
        </div>
      </div>
    );
  }

  const stats = source.contributionStats;

  return (
    <div>
      {/* Header */}
      <div className="flex items-center gap-3 mb-6">
        <Link to="/admin/sourcing/sources" className="text-ink-400 hover:text-primary transition-colors">
          <span className="material-symbols-outlined text-xl">arrow_back</span>
        </Link>
        <div className="flex-1">
          <div className="flex items-center gap-3">
            <h1 className="text-2xl font-display font-bold text-ink-900">{source.name}</h1>
            <TierBadge tier={source.sourceTier} size="lg" />
            <div className="flex gap-1">
              {source.supportedFormats.map((f) => <FormatPill key={f} format={f} />)}
            </div>
          </div>
          {source.baseUrl && (
            <a href={source.baseUrl} target="_blank" rel="noopener noreferrer" className="text-sm text-primary hover:underline flex items-center gap-1 mt-1">
              {source.baseUrl}
              <span className="material-symbols-outlined text-sm">open_in_new</span>
            </a>
          )}
        </div>
      </div>

      {/* Source Profile */}
      <div className="bg-white rounded-xl border border-border-light p-5 mb-6">
        <h2 className="text-lg font-semibold text-ink-800 mb-4">Source Profile</h2>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div>
            <span className="text-xs font-semibold text-ink-400 uppercase">Description</span>
            <p className="text-sm text-ink-700 mt-1">{source.description || 'No description provided.'}</p>
          </div>
          <div>
            <span className="text-xs font-semibold text-ink-400 uppercase">Composer Affinities</span>
            <div className="mt-1 space-y-1">
              {Object.entries(source.composerAffinity || {}).length > 0 ? (
                Object.entries(source.composerAffinity || {}).map(([name, weight]) => (
                  <div key={name} className="flex items-center gap-2">
                    <span className="text-sm text-ink-700">{name}</span>
                    <div className="flex-1 h-2 bg-slate-100 rounded-full overflow-hidden max-w-[120px]">
                      <div className="h-full bg-primary rounded-full" style={{ width: `${(weight as number) * 100}%` }} />
                    </div>
                    <span className="text-xs text-ink-500 tabular-nums">{((weight as number) * 100).toFixed(0)}%</span>
                  </div>
                ))
              ) : (
                <p className="text-sm text-ink-400 italic">No composer affinities set.</p>
              )}
            </div>
          </div>
        </div>
      </div>

      {/* Contribution Summary */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
        <MetricCard
          label="Total Krithis"
          value={stats?.totalKrithis ?? 0}
          icon={<span className="material-symbols-outlined text-lg">music_note</span>}
        />
        <MetricCard
          label="Avg Confidence"
          value={stats?.avgConfidence != null ? `${(stats.avgConfidence * 100).toFixed(0)}%` : '—'}
          icon={<span className="material-symbols-outlined text-lg">verified</span>}
        />
        <MetricCard
          label="Extraction Success"
          value={stats?.extractionSuccessRate != null ? `${(stats.extractionSuccessRate * 100).toFixed(0)}%` : '—'}
          icon={<span className="material-symbols-outlined text-lg">check_circle</span>}
        />
        <MetricCard
          label="Krithi Count"
          value={source.krithiCount}
          icon={<span className="material-symbols-outlined text-lg">library_books</span>}
        />
      </div>

      {/* Extraction History Placeholder */}
      <div className="bg-white rounded-xl border border-border-light p-5 mb-6">
        <h2 className="text-lg font-semibold text-ink-800 mb-4">Extraction History</h2>
        <p className="text-sm text-ink-400 italic">Extraction history will be displayed here once extraction tasks are linked to this source.</p>
      </div>

      {/* Contributed Krithis Placeholder */}
      <div className="bg-white rounded-xl border border-border-light p-5">
        <h2 className="text-lg font-semibold text-ink-800 mb-4">Contributed Krithis</h2>
        <p className="text-sm text-ink-400 italic">Contributed Krithis will be displayed here once source evidence is available.</p>
      </div>
    </div>
  );
};

export default SourceDetailPage;
