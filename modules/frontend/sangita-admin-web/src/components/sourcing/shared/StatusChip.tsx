import React from 'react';
import type { ExtractionStatus } from '../../../types/sourcing';

interface StatusChipProps {
  status: ExtractionStatus | string;
  size?: 'sm' | 'md';
}

const statusConfig: Record<string, { colors: string; pulse?: boolean }> = {
  PENDING: { colors: 'bg-slate-100 text-slate-700 border-slate-200' },
  PROCESSING: { colors: 'bg-blue-50 text-blue-700 border-blue-200', pulse: true },
  DONE: { colors: 'bg-emerald-50 text-emerald-700 border-emerald-200' },
  FAILED: { colors: 'bg-red-50 text-red-700 border-red-200' },
  CANCELLED: { colors: 'bg-slate-200 text-slate-600 border-slate-300' },
  // Additional statuses for bulk import compatibility
  RUNNING: { colors: 'bg-blue-50 text-blue-700 border-blue-200', pulse: true },
  SUCCEEDED: { colors: 'bg-emerald-50 text-emerald-700 border-emerald-200' },
  PAUSED: { colors: 'bg-amber-50 text-amber-700 border-amber-200' },
};

const defaultConfig = { colors: 'bg-slate-100 text-slate-600 border-slate-200' };

const sizeClasses: Record<string, string> = {
  sm: 'text-[10px] px-1.5 py-0.5',
  md: 'text-xs px-2 py-0.5',
};

const StatusChip: React.FC<StatusChipProps> = ({ status, size = 'md' }) => {
  const config = statusConfig[status] || defaultConfig;

  return (
    <span
      className={`inline-flex items-center gap-1 font-semibold rounded-full border ${config.colors} ${sizeClasses[size]} leading-none`}
    >
      {config.pulse && (
        <span className="relative flex h-2 w-2">
          <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-blue-400 opacity-75" />
          <span className="relative inline-flex rounded-full h-2 w-2 bg-blue-500" />
        </span>
      )}
      {status}
    </span>
  );
};

export default StatusChip;
