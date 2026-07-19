import React from 'react';
import { Link } from 'react-router-dom';
import { type SectionIssuesPage } from '../../api/client';

export const SectionIssuesTab: React.FC<{
    data: SectionIssuesPage | undefined;
    loading: boolean;
    page: number;
    onPageChange: (p: number) => void;
    pageSize: number;
}> = ({ data, loading, page, onPageChange, pageSize }) => {
    if (loading) return <div className="flex items-center justify-center h-48"><div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary" /></div>;
    if (!data || !data.items.length) return <div className="flex flex-col items-center justify-center h-48 text-ink-400"><span className="material-symbols-outlined text-4xl mb-2">check_circle</span><p>No section issues found.</p></div>;

    const totalPages = Math.ceil(data.total / pageSize);

    return (
        <div className="space-y-4 flex-1">
            <div className="bg-white rounded-lg border border-border-light overflow-hidden">
                <table className="w-full text-sm">
                    <thead className="bg-slate-50 border-b border-border-light">
                        <tr>
                            <th className="text-left px-4 py-3 font-medium text-ink-600">Title</th>
                            <th className="text-left px-4 py-3 font-medium text-ink-600">Language</th>
                            <th className="text-center px-4 py-3 font-medium text-ink-600">Expected</th>
                            <th className="text-center px-4 py-3 font-medium text-ink-600">Actual</th>
                            <th className="text-left px-4 py-3 font-medium text-ink-600">Issue Type</th>
                        </tr>
                    </thead>
                    <tbody>
                        {data.items.map((issue, idx) => (
                            <tr key={`${issue.krithiId}-${issue.language}-${idx}`} className="border-b border-border-light hover:bg-slate-50">
                                <td className="px-4 py-3 font-medium">
                                    <Link
                                        to={`/krithis/${issue.krithiId}?tab=Lyrics`}
                                        className="text-primary hover:underline"
                                    >{issue.title}</Link>
                                </td>
                                <td className="px-4 py-3 text-ink-600">{issue.language}</td>
                                <td className="px-4 py-3 text-center font-mono">{issue.expectedSections}</td>
                                <td className={`px-4 py-3 text-center font-mono ${issue.actualSections === 0 ? 'text-red-600 font-bold' : 'text-yellow-600'}`}>{issue.actualSections}</td>
                                <td className="px-4 py-3">
                                    <span className={`inline-flex px-2 py-0.5 rounded-full text-xs font-medium ${
                                        issue.issueType === 'missing sections' ? 'bg-red-50 text-red-700'
                                        : issue.issueType.includes('dual-format') ? 'bg-yellow-50 text-yellow-700'
                                        : 'bg-orange-50 text-orange-700'
                                    }`}>{issue.issueType}</span>
                                </td>
                            </tr>
                        ))}
                    </tbody>
                </table>
            </div>
            {totalPages > 1 && (
                <div className="flex items-center justify-between">
                    <p className="text-sm text-ink-500">Showing {page * pageSize + 1}-{Math.min((page + 1) * pageSize, data.total)} of {data.total}</p>
                    <div className="flex gap-2">
                        <button onClick={() => onPageChange(page - 1)} disabled={page === 0}
                            className="px-3 py-1 text-sm rounded border border-border-light disabled:opacity-50 hover:bg-slate-50">Previous</button>
                        <button onClick={() => onPageChange(page + 1)} disabled={page >= totalPages - 1}
                            className="px-3 py-1 text-sm rounded border border-border-light disabled:opacity-50 hover:bg-slate-50">Next</button>
                    </div>
                </div>
            )}
        </div>
    );
};
