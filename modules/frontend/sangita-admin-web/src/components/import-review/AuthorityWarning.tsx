import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { TierBadge } from '../sourcing/shared';

interface HigherTierConflict {
  sourceId: string;
  sourceName: string;
  sourceTier: number;
  field: string;
  conflictingValue: string;
}

interface AuthorityWarningProps {
  /** The tier of the source being reviewed */
  currentTier?: number;
  /** Higher-tier conflicts found for this Krithi */
  conflicts?: HigherTierConflict[];
  /** Callback when the user dismisses the warning with a reason */
  onDismiss?: (reason: string) => void;
  /** Whether the warning has already been dismissed */
  dismissed?: boolean;
}

/**
 * Authority Validation Warning for Import Review.
 * Shows a warning banner when approving data from a lower-tier source
 * while higher-tier source data exists for the same Krithi.
 */
export const AuthorityWarning: React.FC<AuthorityWarningProps> = ({
  currentTier,
  conflicts,
  onDismiss,
  dismissed = false,
}) => {
  const [showDismissForm, setShowDismissForm] = useState(false);
  const [dismissReason, setDismissReason] = useState('');

  if (!conflicts || conflicts.length === 0 || dismissed) {
    return null;
  }

  const highestConflictTier = Math.min(...conflicts.map(c => c.sourceTier));

  const handleDismiss = () => {
    if (!dismissReason.trim()) return;
    onDismiss?.(dismissReason.trim());
    setShowDismissForm(false);
    setDismissReason('');
  };

  return (
    <div className="bg-amber-50 border border-amber-300 rounded-lg p-4 mb-4">
      <div className="flex items-start gap-3">
        <span className="material-symbols-outlined text-amber-600 text-xl flex-shrink-0 mt-0.5">warning</span>
        <div className="flex-1">
          <h4 className="text-sm font-bold text-amber-900">Higher-Tier Source Data Exists</h4>
          <p className="text-xs text-amber-800 mt-1">
            You are reviewing data from a{' '}
            {currentTier != null && <TierBadge tier={currentTier} />}{' '}
            source, but higher-tier data exists from{' '}
            <TierBadge tier={highestConflictTier} />{' '}
            source(s) for the same Krithi. Please review before approving.
          </p>

          {/* Conflict Details */}
          <div className="mt-3 bg-white/60 rounded-md border border-amber-200 divide-y divide-amber-200">
            {conflicts.map((conflict, i) => (
              <div key={i} className="px-3 py-2 flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <TierBadge tier={conflict.sourceTier} />
                  <div>
                    <Link
                      to={`/admin/sourcing/sources/${conflict.sourceId}`}
                      className="text-xs font-medium text-amber-900 hover:text-primary"
                    >
                      {conflict.sourceName}
                    </Link>
                    <span className="text-xs text-amber-700 ml-1">
                      ({conflict.field}: <span className="italic">"{conflict.conflictingValue}"</span>)
                    </span>
                  </div>
                </div>
                <Link
                  to={`/admin/sourcing/evidence?source=${conflict.sourceId}`}
                  className="text-[10px] text-primary hover:underline font-medium"
                >
                  Compare
                </Link>
              </div>
            ))}
          </div>

          {/* Dismiss Section */}
          <div className="mt-3">
            {showDismissForm ? (
              <div className="flex items-end gap-2">
                <div className="flex-1">
                  <label className="text-[10px] font-semibold text-amber-700 block mb-1">
                    Reason for dismissal (logged to audit)
                  </label>
                  <input
                    type="text"
                    value={dismissReason}
                    onChange={e => setDismissReason(e.target.value)}
                    placeholder="e.g., Lower-tier source has more accurate lyrics..."
                    className="w-full px-2.5 py-1.5 text-xs border border-amber-300 rounded focus:ring-1 focus:ring-amber-400 focus:border-amber-400 bg-white"
                    autoFocus
                  />
                </div>
                <button
                  onClick={handleDismiss}
                  disabled={!dismissReason.trim()}
                  className="px-3 py-1.5 text-xs font-medium text-amber-800 bg-amber-100 border border-amber-300 rounded hover:bg-amber-200 disabled:opacity-50"
                >
                  Dismiss
                </button>
                <button
                  onClick={() => { setShowDismissForm(false); setDismissReason(''); }}
                  className="px-2 py-1.5 text-xs text-ink-500 hover:text-ink-700"
                >
                  Cancel
                </button>
              </div>
            ) : (
              <button
                onClick={() => setShowDismissForm(true)}
                className="text-xs text-amber-700 hover:text-amber-900 font-medium underline"
              >
                Dismiss warning with reason...
              </button>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};
