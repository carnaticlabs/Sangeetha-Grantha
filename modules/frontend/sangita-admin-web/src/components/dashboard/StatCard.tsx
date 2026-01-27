import React from 'react';

interface StatCardProps {
    label: string;
    value: string;
    icon: string;
    onClick?: () => void;
}

export const StatCard: React.FC<StatCardProps> = ({ label, value, icon, onClick }) => (
    <div
        onClick={onClick}
        className={`bg-surface-light p-6 rounded-xl border border-border-light shadow-sm flex items-start justify-between transition-all duration-200 ${onClick
            ? 'cursor-pointer hover:border-primary hover:shadow-md hover:-translate-y-0.5'
            : 'hover:border-slate-300'
            }`}
    >
        <div>
            <p className="text-sm font-bold text-ink-500 uppercase tracking-wide mb-1">{label}</p>
            <h3 className="text-3xl font-display font-bold text-ink-900 tracking-tight">{value}</h3>
        </div>
        <div className="p-3 bg-slate-50 rounded-lg text-ink-700">
            <span className="material-symbols-outlined text-[28px] text-primary">{icon}</span>
        </div>
    </div>
);
