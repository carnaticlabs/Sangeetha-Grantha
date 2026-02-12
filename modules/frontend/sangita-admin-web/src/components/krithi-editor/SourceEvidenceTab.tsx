import React from 'react';
import { Link } from 'react-router-dom';
import { useKrithiEvidence } from '../../hooks/useSourcingQueries';
import { TierBadge, ConfidenceBar, FieldComparisonTable, StructureVisualiser } from '../sourcing/shared';

interface SourceEvidenceTabProps {
  krithiId: string;
  krithiTitle?: string;
}

/**
 * Source Evidence tab for the Krithi Editor.
 * Displays all contributing sources and field provenance for the current Krithi.
 * Lazy-loaded only when the tab is selected to avoid impacting Krithi Editor performance.
 */
export const SourceEvidenceTab: React.FC<SourceEvidenceTabProps> = ({ krithiId, krithiTitle }) => {
  const { data: evidence, isLoading, error } = useKrithiEvidence(krithiId);

  if (isLoading) {
    return (
      <div className="flex items-center justify-center py-16">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary" />
        <span className="ml-3 text-sm text-ink-500">Loading source evidence...</span>
      </div>
    );
  }

  if (error) {
    return (
      <div className="bg-rose-50 border border-rose-200 rounded-lg p-6 text-center">
        <span className="material-symbols-outlined text-rose-500 text-3xl mb-2">error</span>
        <p className="text-sm text-rose-700">Failed to load source evidence data.</p>
        <p className="text-xs text-rose-500 mt-1">{(error as Error).message}</p>
      </div>
    );
  }

  const sources = evidence?.sources ?? [];
  const hasNoEvidence = sources.length === 0;

  if (hasNoEvidence) {
    return (
      <div className="bg-slate-50 border border-border-light rounded-lg p-8 text-center">
        <span className="material-symbols-outlined text-ink-300 text-4xl mb-3">source</span>
        <h3 className="text-lg font-semibold text-ink-700 mb-1">No Source Evidence</h3>
        <p className="text-sm text-ink-500 mb-4">
          No external sources have contributed data for this composition yet.
        </p>
        <Link
          to="/admin/sourcing/sources"
          className="inline-flex items-center gap-1.5 text-sm text-primary font-medium hover:underline"
        >
          <span className="material-symbols-outlined text-base">add_circle</span>
          Browse Source Registry
        </Link>
      </div>
    );
  }

  // Build field comparison data from sources
  const fieldNames = ['title', 'composer', 'raga', 'tala', 'language', 'deity', 'temple'] as const;
  const comparisonSources = sources.map(s => ({
    name: s.sourceName,
    tier: s.sourceTier,
  }));

  const comparisonFields = fieldNames.map(field => ({
    name: field.charAt(0).toUpperCase() + field.slice(1),
    values: sources.map(s => {
      const contributed = s.contributedFields ?? [];
      const hasField = contributed.includes(field);
      return hasField ? (s.extractedData?.[field] ?? '—') : null;
    }),
  }));

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-lg font-semibold text-ink-800">Source Evidence</h2>
          <p className="text-sm text-ink-500 mt-0.5">
            {sources.length} source{sources.length !== 1 ? 's' : ''} have contributed data for this composition.
          </p>
        </div>
        <Link
          to={`/admin/sourcing/evidence?krithi=${krithiId}`}
          className="inline-flex items-center gap-1.5 px-3 py-2 text-sm text-primary font-medium border border-primary/30 rounded-lg hover:bg-primary-light transition-colors"
        >
          <span className="material-symbols-outlined text-base">open_in_new</span>
          Full Evidence Browser
        </Link>
      </div>

      {/* Contributing Sources */}
      <div className="bg-white rounded-xl border border-border-light p-5">
        <h3 className="text-sm font-bold text-ink-800 mb-4">Contributing Sources</h3>
        <div className="divide-y divide-border-light">
          {sources.map((source, idx) => (
            <div key={idx} className="flex items-center justify-between py-3 first:pt-0 last:pb-0">
              <div className="flex items-center gap-3">
                <TierBadge tier={source.sourceTier} />
                <div>
                  <Link
                    to={`/admin/sourcing/sources/${source.sourceId}`}
                    className="text-sm font-medium text-ink-800 hover:text-primary transition-colors"
                  >
                    {source.sourceName}
                  </Link>
                  <div className="text-xs text-ink-400 mt-0.5">
                    {(source.contributedFields ?? []).length} field{(source.contributedFields ?? []).length !== 1 ? 's' : ''} contributed
                    {source.extractedAt && (
                      <> &middot; Extracted {new Date(source.extractedAt).toLocaleDateString()}</>
                    )}
                  </div>
                </div>
              </div>
              <div className="flex items-center gap-3">
                {source.confidence != null && (
                  <div className="w-24">
                    <ConfidenceBar value={source.confidence} showLabel />
                  </div>
                )}
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* Field Provenance Comparison */}
      {comparisonFields.some(f => f.values.some(v => v != null)) && (
        <div className="bg-white rounded-xl border border-border-light p-5">
          <h3 className="text-sm font-bold text-ink-800 mb-4">Field Provenance</h3>
          <p className="text-xs text-ink-500 mb-3">
            Shows which source provided each field value. Conflicts are highlighted.
          </p>
          <FieldComparisonTable
            sources={comparisonSources.map(s => s.name)}
            fields={comparisonFields}
          />
        </div>
      )}

      {/* Structural Analysis */}
      {sources.some(s => s.structure) && (
        <div className="bg-white rounded-xl border border-border-light p-5">
          <h3 className="text-sm font-bold text-ink-800 mb-4">Structural Analysis per Source</h3>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {sources.filter(s => s.structure).map((source, idx) => (
              <div key={idx} className="bg-slate-50 rounded-lg p-3">
                <div className="flex items-center gap-2 mb-2">
                  <TierBadge tier={source.sourceTier} />
                  <span className="text-sm font-medium text-ink-700">{source.sourceName}</span>
                </div>
                <StructureVisualiser sections={source.structure!} mode="compact" />
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Voting Decisions */}
      {evidence?.votingDecisions && evidence.votingDecisions.length > 0 && (
        <div className="bg-white rounded-xl border border-border-light p-5">
          <h3 className="text-sm font-bold text-ink-800 mb-4">Voting Decisions</h3>
          <div className="divide-y divide-border-light">
            {evidence.votingDecisions.map((vote, idx) => (
              <div key={idx} className="flex items-center justify-between py-3 first:pt-0 last:pb-0">
                <div>
                  <span className="text-sm text-ink-700">{vote.field}</span>
                  <span className={`ml-2 text-xs px-2 py-0.5 rounded-full font-medium ${
                    vote.consensusType === 'UNANIMOUS' ? 'bg-emerald-50 text-emerald-700' :
                    vote.consensusType === 'MAJORITY' ? 'bg-blue-50 text-blue-700' :
                    vote.consensusType === 'MANUAL_OVERRIDE' ? 'bg-amber-50 text-amber-700' :
                    'bg-slate-100 text-ink-600'
                  }`}>
                    {vote.consensusType}
                  </span>
                </div>
                <Link
                  to={`/admin/sourcing/voting/${vote.id}`}
                  className="text-xs text-primary hover:underline font-medium"
                >
                  View Detail
                </Link>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
};
