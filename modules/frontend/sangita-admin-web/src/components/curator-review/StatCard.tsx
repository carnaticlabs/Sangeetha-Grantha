import React from 'react';

export const StatCard: React.FC<{ label: string; value: number; color?: string }> = ({ label, value, color }) => (
    <div className="bg-white rounded-lg border border-border-light p-3">
        <p className="text-[10px] text-ink-500 uppercase tracking-wide">{label}</p>
        <p className={`text-xl font-bold mt-0.5 ${color || 'text-ink-900'}`}>{value.toLocaleString()}</p>
    </div>
);
