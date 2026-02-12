// =============================================================================
// VariantMatchRow — Per-match row with approve/override/skip actions
// TRACK-057 T57.6
// =============================================================================

import React, { useState } from 'react';
import type { VariantMatch, VariantMatchStatus } from '../../types/sourcing';
import { useReviewVariantMatch } from '../../hooks/useSourcingQueries';

interface VariantMatchRowProps {
    match: VariantMatch;
}

const statusConfig: Record<VariantMatchStatus, { label: string; color: string; icon: string }> = {
    PENDING: { label: 'Pending', color: 'bg-slate-100 text-slate-700', icon: 'hourglass_empty' },
    APPROVED: { label: 'Approved', color: 'bg-emerald-100 text-emerald-800', icon: 'check_circle' },
    AUTO_APPROVED: { label: 'Auto-Approved', color: 'bg-blue-100 text-blue-800', icon: 'smart_toy' },
    REJECTED: { label: 'Rejected', color: 'bg-red-100 text-red-800', icon: 'cancel' },
};

const tierColors: Record<string, string> = {
    HIGH: 'text-emerald-600',
    MEDIUM: 'text-amber-600',
    LOW: 'text-red-600',
};

const VariantMatchRow: React.FC<VariantMatchRowProps> = ({ match }) => {
    const reviewMutation = useReviewVariantMatch();
    const [notes, setNotes] = useState('');
    const [showNotes, setShowNotes] = useState(false);

    const status = statusConfig[match.matchStatus] || statusConfig.PENDING;
    const isPending = match.matchStatus === 'PENDING';
    const isActioned = match.matchStatus === 'APPROVED' || match.matchStatus === 'AUTO_APPROVED' || match.matchStatus === 'REJECTED';

    const handleAction = (action: 'approve' | 'reject' | 'skip') => {
        reviewMutation.mutate({
            matchId: match.id,
            payload: { action, notes: notes.trim() || null },
        });
        setShowNotes(false);
        setNotes('');
    };

    // Parse match signals JSON
    let signals: Record<string, number> = {};
    try {
        signals = typeof match.matchSignals === 'string'
            ? JSON.parse(match.matchSignals)
            : (match.matchSignals as Record<string, number>);
    } catch {
        // ignore parse errors
    }

    return (
        <div className={`px-5 py-4 hover:bg-slate-50/50 transition-colors ${match.isAnomaly ? 'bg-purple-50/30 border-l-3 border-purple-400' : ''}`}>
            <div className="flex items-start justify-between gap-4">
                {/* Left: Krithi info + signals */}
                <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-2 mb-1">
                        <span className="text-sm font-semibold text-ink-800 truncate">{match.krithiTitle}</span>
                        {match.isAnomaly && (
                            <span className="inline-flex items-center gap-0.5 px-1.5 py-0.5 text-[10px] font-bold bg-purple-100 text-purple-700 rounded-full">
                                <span className="material-symbols-outlined text-xs">report</span>
                                ANOMALY
                            </span>
                        )}
                        {match.structureMismatch && (
                            <span className="inline-flex items-center gap-0.5 px-1.5 py-0.5 text-[10px] font-bold bg-orange-100 text-orange-700 rounded-full">
                                <span className="material-symbols-outlined text-xs">difference</span>
                                MISMATCH
                            </span>
                        )}
                    </div>

                    {/* Confidence + Tier */}
                    <div className="flex items-center gap-3 mb-2">
                        <div className="flex items-center gap-1">
                            <span className={`text-sm font-bold tabular-nums ${tierColors[match.confidenceTier] || 'text-ink-600'}`}>
                                {(match.confidence * 100).toFixed(1)}%
                            </span>
                            <span className={`text-xs font-medium px-1.5 py-0.5 rounded ${match.confidenceTier === 'HIGH' ? 'bg-emerald-100 text-emerald-700'
                                    : match.confidenceTier === 'MEDIUM' ? 'bg-amber-100 text-amber-700'
                                        : 'bg-red-100 text-red-700'
                                }`}>
                                {match.confidenceTier}
                            </span>
                        </div>
                        <span className={`inline-flex items-center gap-1 px-2 py-0.5 text-xs font-medium rounded-full ${status.color}`}>
                            <span className="material-symbols-outlined text-xs">{status.icon}</span>
                            {status.label}
                        </span>
                    </div>

                    {/* Signal Breakdown */}
                    {Object.keys(signals).length > 0 && (
                        <div className="flex flex-wrap gap-2">
                            {Object.entries(signals).map(([key, value]) => (
                                <span
                                    key={key}
                                    className="inline-flex items-center gap-1 text-[11px] text-ink-500 bg-slate-50 px-2 py-0.5 rounded border border-border-light"
                                >
                                    <span className="font-medium text-ink-600">{formatSignalLabel(key)}:</span>
                                    <span className="tabular-nums">{typeof value === 'number' ? value.toFixed(2) : value}</span>
                                </span>
                            ))}
                        </div>
                    )}

                    {/* Reviewer notes */}
                    {match.reviewerNotes && (
                        <p className="text-xs text-ink-400 mt-1 italic">"{match.reviewerNotes}"</p>
                    )}
                </div>

                {/* Right: Actions */}
                <div className="flex items-center gap-1.5 flex-shrink-0">
                    {isPending && (
                        <>
                            <button
                                onClick={() => handleAction('approve')}
                                disabled={reviewMutation.isPending}
                                className="inline-flex items-center gap-1 px-2.5 py-1.5 text-xs font-medium text-emerald-700 bg-emerald-50 border border-emerald-200 rounded-lg hover:bg-emerald-100 disabled:opacity-50 transition-colors"
                                title="Approve match"
                            >
                                <span className="material-symbols-outlined text-sm">check</span>
                                Approve
                            </button>
                            <button
                                onClick={() => handleAction('reject')}
                                disabled={reviewMutation.isPending}
                                className="inline-flex items-center gap-1 px-2.5 py-1.5 text-xs font-medium text-red-700 bg-red-50 border border-red-200 rounded-lg hover:bg-red-100 disabled:opacity-50 transition-colors"
                                title="Reject match"
                            >
                                <span className="material-symbols-outlined text-sm">close</span>
                                Reject
                            </button>
                            <button
                                onClick={() => handleAction('skip')}
                                disabled={reviewMutation.isPending}
                                className="inline-flex items-center gap-1 px-2.5 py-1.5 text-xs font-medium text-ink-500 bg-slate-50 border border-border-light rounded-lg hover:bg-slate-100 disabled:opacity-50 transition-colors"
                                title="Skip for now"
                            >
                                <span className="material-symbols-outlined text-sm">skip_next</span>
                            </button>
                            <button
                                onClick={() => setShowNotes(!showNotes)}
                                className="inline-flex items-center px-1.5 py-1.5 text-xs text-ink-400 hover:text-ink-600 transition-colors"
                                title="Add notes"
                            >
                                <span className="material-symbols-outlined text-sm">comment</span>
                            </button>
                        </>
                    )}
                    {!isPending && (
                        <span className="text-xs text-ink-400 font-mono">{match.id.slice(0, 8)}</span>
                    )}
                </div>
            </div>

            {/* Notes Input */}
            {showNotes && isPending && (
                <div className="mt-3 flex gap-2">
                    <input
                        type="text"
                        value={notes}
                        onChange={(e) => setNotes(e.target.value)}
                        placeholder="Optional reviewer notes..."
                        className="flex-1 px-3 py-1.5 text-xs border border-border-light rounded-lg focus:outline-none focus:ring-2 focus:ring-primary/20"
                    />
                </div>
            )}
        </div>
    );
};

function formatSignalLabel(key: string): string {
    return key
        .replace(/([A-Z])/g, ' $1')
        .replace(/^./, (s) => s.toUpperCase())
        .trim();
}

export default VariantMatchRow;
