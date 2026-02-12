import React from 'react';
import type { ExtractionStatsResponse } from '../../types/sourcing';

interface StatusSummaryBarProps {
  stats: ExtractionStatsResponse | undefined;
  loading?: boolean;
}

const statusSegments: { key: keyof ExtractionStatsResponse; label: string; color: string; textColor: string }[] = [
  { key: 'pending', label: 'Pending', color: 'bg-slate-300', textColor: 'text-slate-700' },
  { key: 'processing', label: 'Processing', color: 'bg-blue-500', textColor: 'text-blue-700' },
  { key: 'done', label: 'Done', color: 'bg-emerald-500', textColor: 'text-emerald-700' },
  { key: 'failed', label: 'Failed', color: 'bg-red-500', textColor: 'text-red-700' },
  { key: 'cancelled', label: 'Cancelled', color: 'bg-slate-400', textColor: 'text-slate-600' },
];

const StatusSummaryBar: React.FC<StatusSummaryBarProps> = ({ stats, loading }) => {
  if (loading || !stats) {
    return (
      <div className="bg-white rounded-xl border border-border-light p-4 mb-6 animate-pulse">
        <div className="flex items-center gap-6">
          {statusSegments.map((s) => (
            <div key={s.key} className="flex items-center gap-2">
              <div className="h-3 w-16 bg-slate-200 rounded" />
              <div className="h-5 w-8 bg-slate-200 rounded" />
            </div>
          ))}
        </div>
      </div>
    );
  }

  const total = stats.total || 1;

  return (
    <div className="bg-white rounded-xl border border-border-light p-4 mb-6">
      <div className="flex items-center justify-between mb-3">
        <div className="flex items-center gap-6">
          {statusSegments.map((seg) => {
            const count = stats[seg.key] as number;
            return (
              <div key={seg.key} className="flex items-center gap-2">
                <div className={`w-3 h-3 rounded-full ${seg.color}`} />
                <span className={`text-xs font-medium ${seg.textColor}`}>{seg.label}</span>
                <span className="text-sm font-bold text-ink-800 tabular-nums">{count}</span>
              </div>
            );
          })}
        </div>
        <div className="text-xs text-ink-400">
          {stats.throughputPerHour.toFixed(1)}/hr (24h)
        </div>
      </div>

      {/* Progress bar */}
      <div className="h-2 bg-slate-100 rounded-full overflow-hidden flex">
        {statusSegments.map((seg) => {
          const count = stats[seg.key] as number;
          const pct = (count / total) * 100;
          if (pct === 0) return null;
          return (
            <div
              key={seg.key}
              className={`${seg.color} h-full transition-all duration-500`}
              style={{ width: `${pct}%` }}
              title={`${seg.label}: ${count}`}
            />
          );
        })}
      </div>
    </div>
  );
};

export default StatusSummaryBar;
