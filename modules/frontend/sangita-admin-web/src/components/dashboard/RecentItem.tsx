import React from 'react';

interface RecentItemProps {
    title: string;
    subtitle: string;
    time: string;
    status: string;
}

export const RecentItem: React.FC<RecentItemProps> = ({ title, subtitle, time, status }) => {
    const displayTitle = title || 'Unknown';
    const firstChar = displayTitle && displayTitle.length > 0 ? displayTitle.charAt(0).toUpperCase() : '?';

    return (
        <div className="flex items-center justify-between py-4 border-b border-slate-100 last:border-0 hover:bg-slate-50 -mx-4 px-4 transition-colors cursor-pointer">
            <div className="flex items-center gap-4">
                <div className="w-10 h-10 rounded-full bg-slate-100 flex items-center justify-center text-ink-500 font-bold text-sm">
                    {firstChar}
                </div>
                <div>
                    <h4 className="text-sm font-bold text-ink-900">{displayTitle}</h4>
                    <p className="text-xs text-ink-500">{subtitle}</p>
                </div>
            </div>
            <div className="flex items-center gap-4 text-right">
                <span className={`px-2.5 py-0.5 rounded-full text-xs font-bold border ${status === 'Published' ? 'bg-green-50 text-green-700 border-green-200' :
                    status === 'Review' ? 'bg-amber-50 text-amber-700 border-amber-200' :
                        'bg-slate-100 text-slate-600 border-slate-200'
                    }`}>
                    {status}
                </span>
                <span className="text-xs text-ink-500 font-medium tabular-nums">{time}</span>
            </div>
        </div>
    );
};
