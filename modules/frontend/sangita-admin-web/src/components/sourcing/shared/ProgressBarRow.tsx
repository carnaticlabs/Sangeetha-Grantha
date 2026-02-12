import React from 'react';

interface ProgressBarRowProps {
  label: string;
  target: number;
  actual: number;
  inProgress?: number;
}

const ProgressBarRow: React.FC<ProgressBarRowProps> = ({
  label,
  target,
  actual,
  inProgress = 0,
}) => {
  const total = target || 1; // avoid division by zero
  const completedPct = Math.min(100, (actual / total) * 100);
  const inProgressPct = Math.min(100 - completedPct, (inProgress / total) * 100);
  const overallPct = Math.round(completedPct + inProgressPct);

  return (
    <div className="flex items-center gap-4">
      <div className="w-52 flex-shrink-0">
        <span className="text-sm font-medium text-ink-700">{label}</span>
      </div>
      <div className="flex-1 h-3 bg-slate-100 rounded-full overflow-hidden">
        <div className="h-full flex">
          {completedPct > 0 && (
            <div
              className="bg-emerald-500 h-full transition-all duration-500"
              style={{ width: `${completedPct}%` }}
            />
          )}
          {inProgressPct > 0 && (
            <div
              className="bg-amber-400 h-full transition-all duration-500"
              style={{ width: `${inProgressPct}%` }}
            />
          )}
        </div>
      </div>
      <div className="w-28 flex-shrink-0 text-right">
        <span className="text-xs tabular-nums text-ink-600">
          <span className="font-semibold">{actual}</span>
          {inProgress > 0 && <span className="text-amber-600"> +{inProgress}</span>}
          <span className="text-ink-400"> / {target}</span>
        </span>
      </div>
      <div className="w-12 flex-shrink-0 text-right">
        <span className="text-xs font-semibold text-ink-700 tabular-nums">{overallPct}%</span>
      </div>
    </div>
  );
};

export default ProgressBarRow;
