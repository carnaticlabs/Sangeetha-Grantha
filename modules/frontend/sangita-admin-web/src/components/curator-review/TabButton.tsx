import React from 'react';

export const TabButton: React.FC<{ active: boolean; onClick: () => void; label: string; count?: number }> = ({ active, onClick, label, count }) => (
    <button onClick={onClick}
        className={`pb-3 text-sm font-medium border-b-2 transition-colors ${active ? 'border-primary text-primary' : 'border-transparent text-ink-500 hover:text-ink-700'}`}>
        {label}
        {count != null && (
            <span className={`ml-2 px-2 py-0.5 rounded-full text-xs ${active ? 'bg-primary-light text-primary' : 'bg-slate-100 text-ink-500'}`}>{count}</span>
        )}
    </button>
);
