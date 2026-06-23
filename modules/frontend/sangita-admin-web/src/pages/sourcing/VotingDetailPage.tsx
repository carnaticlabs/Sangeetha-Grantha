import React, { useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { useVotingDetail, useSubmitOverride } from '../../hooks/useSourcingQueries';
import { TierBadge, ConfidenceBar, StructureVisualiser, MetricCard } from '../../components/sourcing/shared';
import type { ConfidenceLevel } from '../../types/sourcing';

// The API reports confidence as a level; map to a 0–1 value for the ConfidenceBar.
const CONFIDENCE_VALUE: Record<ConfidenceLevel, number> = { HIGH: 0.95, MEDIUM: 0.65, LOW: 0.35 };

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
    detail.consensusType === 'MANUAL' ? 'bg-amber-50 text-amber-700 border-amber-200' :
    'bg-slate-100 text-ink-600 border-border-light';

  const confidenceValue = CONFIDENCE_VALUE[detail.confidence] ?? 0;

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <div className="flex items-center gap-3">
          <Link to="/admin/sourcing/voting" className="text-ink-400 hover:text-primary transition-colors">
            <span className="material-symbols-outlined text-xl">arrow_back</span>
          </Link>
          <div>
            <h1 className="text-2xl font-display font-bold text-ink-900">Voting Decision Detail</h1>
            <p className="text-sm text-ink-500 mt-0.5">{detail.krithiTitle ?? 'Unknown Krithi'}</p>
          </div>
        </div>
        <span className={`px-3 py-1.5 rounded-full text-sm font-bold border ${consensusColor}`}>
          {detail.consensusType}
        </span>
      </div>

      {/* KPI Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
        <MetricCard label="Consensus Type" value={detail.consensusType} />
        <MetricCard label="Sources" value={detail.participants?.length ?? 0} />
        <MetricCard label="Confidence" value={detail.confidence} />
        <MetricCard
          label="Voted At"
          value={detail.votedAt ? new Date(detail.votedAt).toLocaleDateString() : '—'}
        />
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Consensus Structure */}
        <div className="lg:col-span-2 space-y-6">
          <div className="bg-white rounded-xl border border-border-light p-5">
            <h2 className="text-lg font-semibold text-ink-800 mb-4">Consensus Structure</h2>
            {detail.consensusStructure && detail.consensusStructure.length > 0 ? (
              <StructureVisualiser sections={detail.consensusStructure} />
            ) : (
              <p className="text-sm text-ink-400 italic">No consensus structure recorded.</p>
            )}
            <div className="mt-4">
              <span className="text-xs font-semibold text-ink-500 mb-1 block">Consensus Confidence</span>
              <div className="w-48">
                <ConfidenceBar value={confidenceValue} showLabel />
              </div>
            </div>
          </div>

          {/* Participating Sources */}
          <div className="bg-white rounded-xl border border-border-light p-5">
            <h2 className="text-lg font-semibold text-ink-800 mb-4">Participating Sources</h2>
            {detail.participants && detail.participants.length > 0 ? (
              <div className="divide-y divide-border-light">
                {detail.participants.map((participant, idx) => (
                  <div key={participant.sourceId ?? idx} className="flex items-center justify-between py-3 first:pt-0 last:pb-0">
                    <div className="flex items-center gap-3">
                      {participant.sourceTier != null && <TierBadge tier={participant.sourceTier} />}
                      <div>
                        <span className="text-sm font-medium text-ink-800">{participant.sourceName ?? `Source ${idx + 1}`}</span>
                        {participant.extractionMethod && (
                          <p className="text-xs text-ink-500 mt-0.5">{participant.extractionMethod}</p>
                        )}
                      </div>
                    </div>
                    <span
                      className={`text-xs font-bold px-2 py-0.5 rounded-full ${
                        participant.agrees
                          ? 'text-emerald-600 bg-emerald-50'
                          : 'text-rose-600 bg-rose-50'
                      }`}
                    >
                      {participant.agrees ? 'Agrees' : 'Dissents'}
                    </span>
                  </div>
                ))}
              </div>
            ) : (
              <p className="text-sm text-ink-400 italic">No participating sources available.</p>
            )}
          </div>
        </div>

        {/* Sidebar */}
        <div className="space-y-6">
          {/* Reviewer Notes */}
          <div className="bg-white rounded-xl border border-border-light p-5">
            <h2 className="text-sm font-semibold text-ink-800 mb-3">Reviewer Notes</h2>
            {detail.notes ? (
              <p className="text-sm text-ink-600">{detail.notes}</p>
            ) : (
              <p className="text-sm text-ink-400 italic">
                {detail.consensusType === 'UNANIMOUS'
                  ? 'All sources agree on this structure.'
                  : detail.consensusType === 'MAJORITY'
                  ? 'Majority of sources selected this structure.'
                  : detail.consensusType === 'MANUAL'
                  ? 'Manually overridden by an administrator.'
                  : 'No reviewer notes recorded.'}
              </p>
            )}
            {detail.reviewerName && (
              <p className="text-[10px] text-ink-500 mt-2">Reviewer: {detail.reviewerName}</p>
            )}
          </div>

          {/* Manual Override */}
          <div className="bg-white rounded-xl border border-border-light p-5">
            <h2 className="text-sm font-semibold text-ink-800 mb-3">Manual Override</h2>
            {detail.consensusType === 'MANUAL' && detail.notes && (
              <div className="bg-amber-50 border border-amber-200 rounded-lg p-3 mb-3">
                <span className="text-xs font-semibold text-amber-800">Override Note:</span>
                <p className="text-xs text-amber-700 mt-1">{detail.notes}</p>
                {detail.reviewerName && (
                  <p className="text-[10px] text-amber-600 mt-1">By: {detail.reviewerName}</p>
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
