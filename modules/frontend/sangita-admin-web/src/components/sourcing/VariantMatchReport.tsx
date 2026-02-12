// =============================================================================
// VariantMatchReport — Summary bar + match table for variant matching results
// TRACK-057 T57.4
// =============================================================================

import React from 'react';
import type { VariantMatch, VariantMatchReport as VariantMatchReportType } from '../../types/sourcing';
import { useVariantMatches, useVariantMatchReport } from '../../hooks/useSourcingQueries';
import VariantMatchRow from './VariantMatchRow';

interface VariantMatchReportProps {
    extractionId: string;
}

const tierConfig = {
    HIGH: { label: 'High', color: 'bg-emerald-100 text-emerald-800', icon: 'check_circle' },
    MEDIUM: { label: 'Medium', color: 'bg-amber-100 text-amber-800', icon: 'help' },
    LOW: { label: 'Low', color: 'bg-red-100 text-red-800', icon: 'warning' },
} as const;

const VariantMatchReportComponent: React.FC<VariantMatchReportProps> = ({ extractionId }) => {
    const { data: report, isLoading: reportLoading } = useVariantMatchReport(extractionId);
    const { data: matchesData, isLoading: matchesLoading } = useVariantMatches(extractionId);

    if (reportLoading || matchesLoading) {
        return (
            <div className="flex items-center justify-center py-12">
                <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary" />
            </div>
        );
    }

    if (!report) {
        return (
            <div className="bg-slate-50 border border-border-light rounded-xl p-8 text-center">
                <span className="material-symbols-outlined text-3xl text-ink-300 mb-2">search_off</span>
                <p className="text-sm text-ink-500">No variant match report available for this extraction.</p>
            </div>
        );
    }

    const matches = matchesData?.items ?? [];
    const unmatched = report.totalMatches - report.highConfidence - report.mediumConfidence - report.lowConfidence;

    return (
        <div className="space-y-6">
            {/* Summary Bar */}
            <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-6 gap-3">
                <SummaryCard label="Total" value={report.totalMatches} color="bg-slate-100 text-ink-800" />
                <SummaryCard label="High" value={report.highConfidence} color="bg-emerald-50 text-emerald-700" />
                <SummaryCard label="Medium" value={report.mediumConfidence} color="bg-amber-50 text-amber-700" />
                <SummaryCard label="Low" value={report.lowConfidence} color="bg-red-50 text-red-700" />
                <SummaryCard label="Anomalies" value={report.anomalies} color="bg-purple-50 text-purple-700" />
                <SummaryCard label="Auto-Approved" value={report.autoApproved} color="bg-blue-50 text-blue-700" />
            </div>

            {/* Confidence Distribution Bar */}
            {report.totalMatches > 0 && (
                <div className="bg-white rounded-xl border border-border-light p-4">
                    <div className="flex items-center gap-2 mb-3">
                        <span className="text-xs font-semibold text-ink-500 uppercase tracking-wider">Confidence Distribution</span>
                    </div>
                    <div className="h-3 flex rounded-full overflow-hidden bg-slate-100">
                        {report.highConfidence > 0 && (
                            <div
                                className="bg-emerald-400 transition-all"
                                style={{ width: `${(report.highConfidence / report.totalMatches) * 100}%` }}
                                title={`High: ${report.highConfidence}`}
                            />
                        )}
                        {report.mediumConfidence > 0 && (
                            <div
                                className="bg-amber-400 transition-all"
                                style={{ width: `${(report.mediumConfidence / report.totalMatches) * 100}%` }}
                                title={`Medium: ${report.mediumConfidence}`}
                            />
                        )}
                        {report.lowConfidence > 0 && (
                            <div
                                className="bg-red-400 transition-all"
                                style={{ width: `${(report.lowConfidence / report.totalMatches) * 100}%` }}
                                title={`Low: ${report.lowConfidence}`}
                            />
                        )}
                        {unmatched > 0 && (
                            <div
                                className="bg-slate-300 transition-all"
                                style={{ width: `${(unmatched / report.totalMatches) * 100}%` }}
                                title={`Unmatched: ${unmatched}`}
                            />
                        )}
                    </div>
                    <div className="flex gap-4 mt-2 text-xs text-ink-500">
                        <span className="flex items-center gap-1"><span className="w-2 h-2 rounded-full bg-emerald-400" />High</span>
                        <span className="flex items-center gap-1"><span className="w-2 h-2 rounded-full bg-amber-400" />Medium</span>
                        <span className="flex items-center gap-1"><span className="w-2 h-2 rounded-full bg-red-400" />Low</span>
                        <span className="flex items-center gap-1"><span className="w-2 h-2 rounded-full bg-slate-300" />Unmatched</span>
                    </div>
                </div>
            )}

            {/* Match Table */}
            <div className="bg-white rounded-xl border border-border-light overflow-hidden">
                <div className="px-5 py-3 border-b border-border-light flex items-center justify-between">
                    <h3 className="text-sm font-semibold text-ink-800">
                        Match Details ({matches.length})
                    </h3>
                </div>
                {matches.length > 0 ? (
                    <div className="divide-y divide-border-light">
                        {matches.map((match) => (
                            <VariantMatchRow key={match.id} match={match} />
                        ))}
                    </div>
                ) : (
                    <div className="p-8 text-center text-sm text-ink-400 italic">
                        No matches found
                    </div>
                )}
            </div>
        </div>
    );
};

const SummaryCard: React.FC<{ label: string; value: number; color: string }> = ({ label, value, color }) => (
    <div className={`rounded-xl px-4 py-3 ${color}`}>
        <div className="text-2xl font-bold tabular-nums">{value}</div>
        <div className="text-xs font-medium opacity-75">{label}</div>
    </div>
);

export default VariantMatchReportComponent;
