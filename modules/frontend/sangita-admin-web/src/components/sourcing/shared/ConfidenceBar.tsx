import React from 'react';

interface ConfidenceBarProps {
  value: number; // 0.0 – 1.0
  showLabel?: boolean;
  size?: 'sm' | 'md';
}

function getBarColor(value: number): string {
  if (value >= 0.7) return 'bg-emerald-500';
  if (value >= 0.4) return 'bg-amber-500';
  return 'bg-red-500';
}

function getBarBg(value: number): string {
  if (value >= 0.7) return 'bg-emerald-100';
  if (value >= 0.4) return 'bg-amber-100';
  return 'bg-red-100';
}

const ConfidenceBar: React.FC<ConfidenceBarProps> = ({ value, showLabel = false, size = 'md' }) => {
  const clampedValue = Math.max(0, Math.min(1, value));
  const percentage = Math.round(clampedValue * 100);
  const heightClass = size === 'sm' ? 'h-1.5' : 'h-2.5';

  return (
    <div className="flex items-center gap-2" role="meter" aria-valuenow={percentage} aria-valuemin={0} aria-valuemax={100} aria-label={`Confidence: ${percentage}%`}>
      <div className={`flex-1 ${getBarBg(clampedValue)} rounded-full overflow-hidden ${heightClass}`}>
        <div
          className={`${getBarColor(clampedValue)} ${heightClass} rounded-full transition-all duration-300`}
          style={{ width: `${percentage}%` }}
        />
      </div>
      {showLabel && (
        <span className="text-xs font-medium text-ink-600 tabular-nums w-9 text-right">{percentage}%</span>
      )}
    </div>
  );
};

export default ConfidenceBar;
