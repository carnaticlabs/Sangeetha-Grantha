import React from 'react';
import type { FieldComparisonRow } from '../../../types/sourcing';
import TierBadge from './TierBadge';
import type { SourceTier } from '../../../types/sourcing';

interface FieldComparisonTableProps {
  fields: FieldComparisonRow[];
  onVotingClick?: (votingDecisionId: string) => void;
}

const agreementColors: Record<string, string> = {
  agree: 'bg-emerald-50 border-emerald-200',
  variation: 'bg-amber-50 border-amber-200',
  conflict: 'bg-red-50 border-red-200',
};

const agreementLabels: Record<string, { label: string; color: string }> = {
  agree: { label: 'Agreement', color: 'text-emerald-700' },
  variation: { label: 'Variation', color: 'text-amber-700' },
  conflict: { label: 'Conflict', color: 'text-red-700' },
};

const FieldComparisonTable: React.FC<FieldComparisonTableProps> = ({ fields, onVotingClick }) => {
  if (fields.length === 0) {
    return <div className="text-sm text-ink-400 italic">No comparison data available</div>;
  }

  // Collect unique sources from the first field
  const sources = fields[0]?.cells || [];

  return (
    <div className="overflow-x-auto">
      <table className="w-full text-sm border-collapse">
        <thead>
          <tr className="border-b border-border-light">
            <th className="px-3 py-2 text-left text-xs font-semibold text-ink-500 uppercase tracking-wider w-32">Field</th>
            <th className="px-3 py-2 text-left text-xs font-semibold text-ink-500 uppercase tracking-wider w-20">Status</th>
            {sources.map((source) => (
              <th key={source.sourceId} className="px-3 py-2 text-left text-xs font-semibold text-ink-500">
                <div className="flex items-center gap-1.5">
                  {source.sourceName}
                  <TierBadge tier={source.sourceTier as SourceTier} size="sm" />
                </div>
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {fields.map((row) => {
            const config = agreementLabels[row.agreement] || agreementLabels.agree;
            return (
              <tr key={row.field} className="border-b border-border-light hover:bg-slate-50/50">
                <td className="px-3 py-2 font-medium text-ink-700 capitalize">{row.field}</td>
                <td className="px-3 py-2">
                  <span className={`text-xs font-semibold ${config.color}`}>{config.label}</span>
                  {row.agreement === 'conflict' && row.votingDecisionId && onVotingClick && (
                    <button
                      onClick={() => onVotingClick(row.votingDecisionId!)}
                      className="ml-1 text-primary text-xs hover:underline"
                    >
                      View Vote
                    </button>
                  )}
                </td>
                {row.cells.map((cell) => (
                  <td
                    key={cell.sourceId}
                    className={`px-3 py-2 border ${agreementColors[row.agreement] || ''}`}
                  >
                    <span className="text-ink-700">{cell.value || '—'}</span>
                  </td>
                ))}
              </tr>
            );
          })}
        </tbody>
      </table>
    </div>
  );
};

export default FieldComparisonTable;
