import React, { useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { useVotingDetail, useSubmitOverride } from '../../hooks/useSourcingQueries';
import { TierBadge, ConfidenceBar, StructureVisualiser, MetricCard, JsonViewer } from '../../components/sourcing/shared';

const VotingDetailPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const { data: detail, isLoading, error } = useVotingDetail(id);
  const overrideMutation = useSubmitOverride();
  const [showOverrideForm, setShowOverrideForm] = useState(false);
  const [overrideReason, setOverrideReason] = useState('');
  const [overrideStructure, setOverrideStructure] = useState('');

  if (isLoading) {
    return (
      <div className="flex items-center justify-center py-16">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary" />
      </div>
    );
  }

  if (error || !detail) {
    return (
      <div className="bg-rose-50 border border-rose-200 rounded-lg p-6 text-center">
        <span className="material-symbols-outlined text-rose-500 text-3xl mb-2">error</span>
        <p className="text-sm text-rose-700">Failed to load voting detail.</p>
        <Link to="/admin/sourcing/voting" className="text-sm text-primary hover:underline mt-2 inline-block">
          Back to Voting List
        </Link>
      </div>
    );
  }

  const handleOverrideSubmit = () => {
    if (!id || !overrideReason.trim()) return;
    overrideMutation.mutate({
      id,
      payload: {
        structure: [], // TODO: parse structure from user input
        notes: overrideReason,
      },
    });
    setShowOverrideForm(false);
  };

  const consensusColor = detail.consensusType === 'UNANIMOUS' ? 'bg-emerald-50 text-emerald-700 border-emerald-200' :
    detail.consensusType === 'MAJORITY' ? 'bg-blue-50 text-blue-700 border-blue-200' :
    detail.consensusType === 'MANUAL_OVERRIDE' ? 'bg-amber-50 text-amber-700 border-amber-200' :
    'bg-slate-100 text-ink-600 border-border-light';

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <div className="flex items-center gap-3">
          <Link to="/admin/sourcing/voting" className="text-ink-400 hover:text-primary transition-colors">
            <span className="material-symbols-outlined text-xl">arrow_back</span>
          </Link>
          <div>
            <h1 className="text-2xl font-display font-bold text-ink-900">Voting Decision Detail</h1>
            <p className="text-sm text-ink-500 mt-0.5">
              {detail.krithiTitle ?? 'Unknown Krithi'}
              {detail.field && <span className="text-ink-400"> &middot; {detail.field}</span>}
            </p>
          </div>
        </div>
        <span className={`px-3 py-1.5 rounded-full text-sm font-bold border ${consensusColor}`}>
          {detail.consensusType}
        </span>
      </div>

      {/* KPI Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
        <MetricCard label="Consensus Type" value={detail.consensusType} />
        <MetricCard label="Sources" value={detail.sourceCount ?? (detail.votes?.length ?? 0)} />
        <MetricCard label="Confidence" value={detail.confidence != null ? `${(detail.confidence * 100).toFixed(1)}%` : '—'} />
        <MetricCard
          label="Decided At"
          value={detail.decidedAt ? new Date(detail.decidedAt).toLocaleDateString() : '—'}
        />
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Winning Structure / Value */}
        <div className="lg:col-span-2 space-y-6">
          <div className="bg-white rounded-xl border border-border-light p-5">
            <h2 className="text-lg font-semibold text-ink-800 mb-4">Winning Structure</h2>
            {detail.winningStructure ? (
              <StructureVisualiser sections={detail.winningStructure} mode="full" />
            ) : detail.chosenValue ? (
              <div className="bg-slate-50 rounded-lg p-4">
                <span className="text-sm text-ink-700 font-medium">{detail.chosenValue}</span>
              </div>
            ) : (
              <p className="text-sm text-ink-400 italic">No winning structure recorded.</p>
            )}
            {detail.confidence != null && (
              <div className="mt-4">
                <span className="text-xs font-semibold text-ink-500 mb-1 block">Consensus Confidence</span>
                <div className="w-48">
                  <ConfidenceBar value={detail.confidence} showLabel />
                </div>
              </div>
            )}
          </div>

          {/* Participating Sources / Votes */}
          <div className="bg-white rounded-xl border border-border-light p-5">
            <h2 className="text-lg font-semibold text-ink-800 mb-4">Participating Sources</h2>
            {detail.votes && detail.votes.length > 0 ? (
              <div className="divide-y divide-border-light">
                {detail.votes.map((vote, idx) => (
                  <div key={idx} className="flex items-center justify-between py-3 first:pt-0 last:pb-0">
                    <div className="flex items-center gap-3">
                      {vote.sourceTier != null && <TierBadge tier={vote.sourceTier} />}
                      <div>
                        <span className="text-sm font-medium text-ink-800">{vote.sourceName ?? `Source ${idx + 1}`}</span>
                        {vote.proposedValue && (
                          <p className="text-xs text-ink-500 mt-0.5 truncate max-w-md">{vote.proposedValue}</p>
                        )}
                      </div>
                    </div>
                    <div className="flex items-center gap-3">
                      {vote.isWinner && (
                        <span className="text-xs font-bold text-emerald-600 bg-emerald-50 px-2 py-0.5 rounded-full">
                          Winner
                        </span>
                      )}
                      {vote.confidence != null && (
                        <div className="w-20">
                          <ConfidenceBar value={vote.confidence} showLabel />
                        </div>
                      )}
                    </div>
                  </div>
                ))}
              </div>
            ) : (
              <p className="text-sm text-ink-400 italic">No vote details available.</p>
            )}
          </div>

          {/* Raw Payload */}
          {detail.rawPayload && (
            <div className="bg-white rounded-xl border border-border-light p-5">
              <h2 className="text-lg font-semibold text-ink-800 mb-4">Raw Decision Payload</h2>
              <JsonViewer data={typeof detail.rawPayload === 'string' ? JSON.parse(detail.rawPayload) : detail.rawPayload} />
            </div>
          )}
        </div>

        {/* Sidebar */}
        <div className="space-y-6">
          {/* Voting Rationale */}
          <div className="bg-white rounded-xl border border-border-light p-5">
            <h2 className="text-sm font-semibold text-ink-800 mb-3">Voting Rationale</h2>
            {detail.rationale ? (
              <p className="text-sm text-ink-600">{detail.rationale}</p>
            ) : (
              <p className="text-sm text-ink-400 italic">
                {detail.consensusType === 'UNANIMOUS'
                  ? 'All sources agree on this structure.'
                  : detail.consensusType === 'MAJORITY'
                  ? 'Majority of sources selected this structure.'
                  : 'Manually overridden by an administrator.'}
              </p>
            )}
          </div>

          {/* Manual Override */}
          <div className="bg-white rounded-xl border border-border-light p-5">
            <h2 className="text-sm font-semibold text-ink-800 mb-3">Manual Override</h2>
            {detail.consensusType === 'MANUAL_OVERRIDE' && detail.overrideReason && (
              <div className="bg-amber-50 border border-amber-200 rounded-lg p-3 mb-3">
                <span className="text-xs font-semibold text-amber-800">Override Reason:</span>
                <p className="text-xs text-amber-700 mt-1">{detail.overrideReason}</p>
                {detail.overrideBy && (
                  <p className="text-[10px] text-amber-600 mt-1">By: {detail.overrideBy}</p>
                )}
              </div>
            )}
            {showOverrideForm ? (
              <div className="space-y-3">
                <div>
                  <label className="text-xs font-semibold text-ink-600 block mb-1">Override Structure/Value</label>
                  <textarea
                    value={overrideStructure}
                    onChange={e => setOverrideStructure(e.target.value)}
                    rows={3}
                    className="w-full px-3 py-2 text-xs border border-border-light rounded-lg focus:ring-2 focus:ring-primary focus:border-transparent"
                    placeholder="Enter the correct structure..."
                  />
                </div>
                <div>
                  <label className="text-xs font-semibold text-ink-600 block mb-1">Reason (required)</label>
                  <input
                    type="text"
                    value={overrideReason}
                    onChange={e => setOverrideReason(e.target.value)}
                    className="w-full px-3 py-2 text-xs border border-border-light rounded-lg focus:ring-2 focus:ring-primary focus:border-transparent"
                    placeholder="e.g., Verified against authoritative publication..."
                  />
                </div>
                <div className="flex gap-2">
                  <button
                    onClick={handleOverrideSubmit}
                    disabled={!overrideReason.trim() || overrideMutation.isPending}
                    className="flex-1 px-3 py-2 text-xs font-medium text-white bg-amber-600 rounded-lg hover:bg-amber-700 disabled:opacity-50"
                  >
                    {overrideMutation.isPending ? 'Submitting...' : 'Submit Override'}
                  </button>
                  <button
                    onClick={() => setShowOverrideForm(false)}
                    className="px-3 py-2 text-xs text-ink-500 border border-border-light rounded-lg hover:bg-slate-50"
                  >
                    Cancel
                  </button>
                </div>
              </div>
            ) : (
              <button
                onClick={() => setShowOverrideForm(true)}
                className="w-full px-3 py-2 text-xs font-medium text-amber-700 border border-amber-200 rounded-lg hover:bg-amber-50"
              >
                Apply Manual Override
              </button>
            )}
          </div>

          {/* Related Links */}
          <div className="bg-white rounded-xl border border-border-light p-5">
            <h2 className="text-sm font-semibold text-ink-800 mb-3">Related</h2>
            <div className="space-y-2">
              {detail.krithiId && (
                <Link
                  to={`/krithis/${detail.krithiId}`}
                  className="flex items-center gap-2 text-xs text-primary hover:underline"
                >
                  <span className="material-symbols-outlined text-sm">edit_note</span>
                  Open in Krithi Editor
                </Link>
              )}
              {detail.krithiId && (
                <Link
                  to={`/admin/sourcing/evidence?krithi=${detail.krithiId}`}
                  className="flex items-center gap-2 text-xs text-primary hover:underline"
                >
                  <span className="material-symbols-outlined text-sm">compare</span>
                  View Source Evidence
                </Link>
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default VotingDetailPage;
