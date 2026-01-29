import React, { useEffect, useState } from 'react';
import { TabProps } from '../../types/krithi-editor.types';
import { getKrithiAuditLogs } from '../../api/client';
import { AuditLog } from '../../types';
import { Skeleton, SectionHeader } from '../common';

export const AuditTab: React.FC<TabProps> = ({ krithi }) => {
    const [logs, setLogs] = useState<AuditLog[]>([]);
    const [loading, setLoading] = useState(false);

    useEffect(() => {
        if (krithi.id) {
            setLoading(true);
            getKrithiAuditLogs(krithi.id)
                .then(setLogs)
                .catch(console.error)
                .finally(() => setLoading(false));
        }
    }, [krithi.id]);

    if (!krithi.id) {
        return <div className="text-center py-12 text-slate-500">Audit logs are available after saving the composition.</div>;
    }

    return (
        <div className="space-y-6">
            <div className="bg-white border border-border-light rounded-xl shadow-sm p-6 relative">
                <SectionHeader title="Change History" className="mb-4" />

                {loading ? (
                    <Skeleton count={5} height="4rem" />
                ) : logs.length === 0 ? (
                    <p className="text-slate-500 italic text-center py-8">No history available for this composition.</p>
                ) : (
                    <div className="flow-root mt-6">
                        <ul role="list" className="-mb-8">
                            {logs.map((log, logIdx) => (
                                <li key={log.id}>
                                    <div className="relative pb-8">
                                        {logIdx !== logs.length - 1 ? (
                                            <span className="absolute left-4 top-4 -ml-px h-full w-0.5 bg-slate-100" aria-hidden="true" />
                                        ) : null}
                                        <div className="relative flex space-x-3">
                                            <div className="flex h-8 w-8 items-center justify-center rounded-full bg-slate-50 ring-8 ring-white border border-slate-200">
                                                <span className="material-symbols-outlined text-sm text-slate-400">
                                                    {log.action === 'CREATE' ? 'add' : log.action === 'UPDATE' ? 'edit' : 'history'}
                                                </span>
                                            </div>
                                            <div className="flex min-w-0 flex-1 justify-between space-x-4 pt-1.5">
                                                <div>
                                                    <p className="text-sm text-slate-600">
                                                        <span className="font-semibold text-slate-900">{log.action}</span> by <span className="font-medium text-slate-900">{log.actor}</span>
                                                    </p>
                                                </div>
                                                <div className="whitespace-nowrap text-right text-sm text-slate-400 tabular-nums">
                                                    <time dateTime={log.timestamp}>{new Date(log.timestamp).toLocaleString()}</time>
                                                </div>
                                            </div>
                                        </div>
                                    </div>
                                </li>
                            ))}
                        </ul>
                    </div>
                )}
            </div>
        </div>
    );
};
