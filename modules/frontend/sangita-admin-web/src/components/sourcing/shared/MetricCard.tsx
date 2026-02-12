import React from 'react';
import type { ReactNode } from 'react';

interface MetricCardProps {
  label: string;
  value: string | number;
  subtitle?: string;
  icon?: ReactNode;
  trend?: 'up' | 'down' | 'flat';
  onClick?: () => void;
  loading?: boolean;
}

const trendConfig = {
  up: { icon: 'trending_up', color: 'text-emerald-600' },
  down: { icon: 'trending_down', color: 'text-red-600' },
  flat: { icon: 'trending_flat', color: 'text-ink-400' },
};

const MetricCard: React.FC<MetricCardProps> = ({
  label,
  value,
  subtitle,
  icon,
  trend,
  onClick,
  loading = false,
}) => {
  const Component = onClick ? 'button' : 'div';

  if (loading) {
    return (
      <div className="bg-white rounded-xl border border-border-light p-5 animate-pulse">
        <div className="h-3 w-20 bg-slate-200 rounded mb-3" />
        <div className="h-8 w-16 bg-slate-200 rounded mb-2" />
        <div className="h-2 w-full bg-slate-100 rounded" />
      </div>
    );
  }

  return (
    <Component
      className={`bg-white rounded-xl border border-border-light p-5 text-left transition-all ${
        onClick
          ? 'cursor-pointer hover:border-primary/30 hover:shadow-sm active:scale-[0.98]'
          : ''
      }`}
      onClick={onClick}
      aria-label={`${label}: ${value}${subtitle ? ` — ${subtitle}` : ''}`}
    >
      <div className="flex items-center justify-between mb-2">
        <span className="text-xs font-semibold text-ink-500 uppercase tracking-wider">{label}</span>
        {icon && <span className="text-ink-300">{icon}</span>}
      </div>
      <div className="flex items-end gap-2">
        <span className="text-2xl font-bold text-ink-900 tabular-nums">{value}</span>
        {trend && (
          <span className={`material-symbols-outlined text-lg ${trendConfig[trend].color}`}>
            {trendConfig[trend].icon}
          </span>
        )}
      </div>
      {subtitle && (
        <p className="text-xs text-ink-400 mt-1">{subtitle}</p>
      )}
    </Component>
  );
};

export default MetricCard;
