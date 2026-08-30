import React, { useMemo, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
    attachRagaAlias,
    confirmNewRaga,
    disambiguateRaga,
    getRagas,
    getUnresolvedRagas,
    scanRagaScaleCollisions,
    type RagaQueueItem,
} from '../../api/client';
import { Raga } from '../../types';

function candidateIds(item: RagaQueueItem): string[] {
    if (!item.proposedLakshana) return [];
    try {
        const parsed = JSON.parse(item.proposedLakshana) as { candidateRagaIds?: unknown };
        return Array.isArray(parsed.candidateRagaIds)
            ? parsed.candidateRagaIds.filter((id): id is string => typeof id === 'string')
            : [];
    } catch {
        return [];
    }
}

export const UnresolvedRagasTab: React.FC<{
    page: number;
    pageSize: number;
    onPageChange: (p: number) => void;
    onError: (msg: string) => void;
    onSuccess: (msg: string) => void;
}> = ({ page, pageSize, onPageChange, onError, onSuccess }) => {
    const queryClient = useQueryClient();
    const [selectedId, setSelectedId] = useState<string | null>(null);
    const [ragaQuery, setRagaQuery] = useState('');
    const [attachRagaId, setAttachRagaId] = useState('');
    const [parentRagaId, setParentRagaId] = useState('');
    const [arohanam, setArohanam] = useState('');
    const [avarohanam, setAvarohanam] = useState('');

    const { data, isLoading } = useQuery({
        queryKey: ['unresolvedRagas', page],
        queryFn: () => getUnresolvedRagas(page, pageSize),
    });

    const { data: ragas = [] } = useQuery({
        queryKey: ['referenceRagas'],
        queryFn: getRagas,
    });

    const ragaById = useMemo(() => {
        const map = new Map<string, Raga>();
        ragas.forEach((r) => map.set(r.id, r));
        return map;
    }, [ragas]);

    const filteredRagas = useMemo(() => {
        const q = ragaQuery.trim().toLowerCase();
        if (!q) return ragas.slice(0, 40);
        return ragas.filter((r) => r.name.toLowerCase().includes(q) || r.nameNormalized.includes(q)).slice(0, 40);
    }, [ragas, ragaQuery]);

    const selected = data?.items.find((i) => i.id === selectedId) ?? data?.items[0] ?? null;

    const invalidate = () => {
        void queryClient.invalidateQueries({ queryKey: ['unresolvedRagas'] });
        void queryClient.invalidateQueries({ queryKey: ['curatorStats'] });
        void queryClient.invalidateQueries({ queryKey: ['referenceRagas'] });
    };

    const attach = useMutation({
        mutationFn: () => attachRagaAlias(selected!.id, attachRagaId),
        onSuccess: (raga) => {
            onSuccess(`Attached alias to ${raga.name}`);
            setAttachRagaId('');
            invalidate();
        },
        onError: (e: Error) => onError(e.message),
    });

    const confirm = useMutation({
        mutationFn: () => confirmNewRaga(selected!.id, {
            parentRagaId,
            arohanam,
            avarohanam,
        }),
        onSuccess: (raga) => {
            onSuccess(`Created ${raga.name}`);
            setParentRagaId('');
            setArohanam('');
            setAvarohanam('');
            invalidate();
        },
        onError: (e: Error) => onError(e.message),
    });

    const disambiguate = useMutation({
        mutationFn: (ragaId: string) => disambiguateRaga(selected!.id, ragaId),
        onSuccess: (raga) => {
            onSuccess(`Resolved as ${raga.name}`);
            invalidate();
        },
        onError: (e: Error) => onError(e.message),
    });

    const scan = useMutation({
        mutationFn: scanRagaScaleCollisions,
        onSuccess: (r) => {
            onSuccess(`Enqueued ${r.inserted} scale-collision group(s)`);
            invalidate();
        },
        onError: (e: Error) => onError(e.message),
    });

    if (isLoading) {
        return <div className="flex items-center justify-center h-48"><div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary" /></div>;
    }
    if (!data) {
        return <div className="flex flex-col items-center justify-center h-48 text-ink-400"><p>Failed to load unresolved ragas.</p></div>;
    }

    const totalPages = Math.max(1, Math.ceil(data.total / pageSize));

    return (
        <div className="flex-1 flex gap-0 overflow-hidden border border-border-light rounded-xl bg-white shadow-sm">
            <div className="w-1/3 border-r border-border-light flex flex-col">
                <div className="p-3 border-b border-border-light bg-slate-50 flex items-center justify-between">
                    <p className="text-xs font-semibold text-ink-600">{data.total} pending</p>
                    <button
                        type="button"
                        onClick={() => scan.mutate()}
                        disabled={scan.isPending}
                        className="px-2 py-1 text-xs font-semibold rounded border border-border-light hover:bg-white disabled:opacity-50"
                    >
                        Scan scale collisions
                    </button>
                </div>
                <ul className="flex-1 overflow-y-auto">
                    {data.items.length === 0 && (
                        <li className="p-6 text-sm text-ink-400 text-center">No unresolved ragas.</li>
                    )}
                    {data.items.map((item) => (
                        <li key={item.id}>
                            <button
                                type="button"
                                onClick={() => setSelectedId(item.id)}
                                className={`w-full text-left px-4 py-3 border-b border-border-light hover:bg-slate-50 ${
                                    (selected?.id === item.id) ? 'bg-indigo-50' : ''
                                }`}
                            >
                                <div className="flex items-center justify-between gap-2">
                                    <span className="font-medium text-sm text-ink-900">{item.rawName}</span>
                                    <span className={`text-[10px] uppercase font-semibold px-2 py-0.5 rounded-full ${
                                        item.kind === 'ambiguous' ? 'bg-amber-50 text-amber-700' : 'bg-slate-100 text-ink-600'
                                    }`}>{item.kind}</span>
                                </div>
                                <p className="text-xs text-ink-400 font-mono mt-0.5">{item.matchKey}</p>
                            </button>
                        </li>
                    ))}
                </ul>
                {data.total > pageSize && (
                    <div className="p-2 flex justify-between border-t border-border-light">
                        <button type="button" disabled={page === 0} onClick={() => onPageChange(page - 1)}
                            className="px-3 py-1 text-sm rounded border border-border-light disabled:opacity-50">Previous</button>
                        <button type="button" disabled={page >= totalPages - 1} onClick={() => onPageChange(page + 1)}
                            className="px-3 py-1 text-sm rounded border border-border-light disabled:opacity-50">Next</button>
                    </div>
                )}
            </div>
            <div className="flex-1 p-6 overflow-y-auto">
                {!selected ? (
                    <p className="text-sm text-ink-400">Select a queue item.</p>
                ) : selected.kind === 'ambiguous' ? (
                    <div className="space-y-4">
                        <h2 className="text-lg font-semibold text-ink-900">Disambiguate “{selected.rawName}”</h2>
                        <p className="text-sm text-ink-600">Pick which homonym each held link meant. Same-scale groups are merge candidates — never auto-merged.</p>
                        <ul className="space-y-2">
                            {candidateIds(selected).map((id) => {
                                const raga = ragaById.get(id);
                                return (
                                    <li key={id} className="flex items-center justify-between gap-3 border border-border-light rounded-lg px-3 py-2">
                                        <div>
                                            <p className="font-medium text-sm">{raga?.name ?? id}</p>
                                            {raga?.melakartaNumber != null && (
                                                <p className="text-xs text-ink-500">mela {raga.melakartaNumber}</p>
                                            )}
                                        </div>
                                        <button
                                            type="button"
                                            onClick={() => disambiguate.mutate(id)}
                                            disabled={disambiguate.isPending}
                                            className="px-3 py-1.5 text-sm font-semibold text-white bg-primary rounded-lg disabled:opacity-50"
                                        >
                                            Pick
                                        </button>
                                    </li>
                                );
                            })}
                        </ul>
                    </div>
                ) : (
                    <div className="space-y-8">
                        <div>
                            <h2 className="text-lg font-semibold text-ink-900">Attach alias “{selected.rawName}”</h2>
                            <p className="text-sm text-ink-600 mb-3">Link this spelling to an existing raga.</p>
                            <input
                                value={ragaQuery}
                                onChange={(e) => setRagaQuery(e.target.value)}
                                placeholder="Filter ragas…"
                                className="w-full px-3 py-2 text-sm border border-border-light rounded mb-2"
                            />
                            <select
                                value={attachRagaId}
                                onChange={(e) => setAttachRagaId(e.target.value)}
                                className="w-full px-3 py-2 text-sm border border-border-light rounded mb-3"
                            >
                                <option value="">Select raga…</option>
                                {filteredRagas.map((r) => (
                                    <option key={r.id} value={r.id}>{r.name}</option>
                                ))}
                            </select>
                            <button
                                type="button"
                                disabled={!attachRagaId || attach.isPending}
                                onClick={() => attach.mutate()}
                                className="px-4 py-2 text-sm font-semibold text-white bg-primary rounded-lg disabled:opacity-50"
                            >
                                Attach alias
                            </button>
                        </div>
                        <div className="border-t border-border-light pt-6">
                            <h2 className="text-lg font-semibold text-ink-900">Confirm new raga</h2>
                            <p className="text-sm text-ink-600 mb-3">Parent, arohana, and avarohana are required.</p>
                            <label className="block text-xs font-semibold text-ink-600 mb-1">Parent raga</label>
                            <select
                                value={parentRagaId}
                                onChange={(e) => setParentRagaId(e.target.value)}
                                className="w-full px-3 py-2 text-sm border border-border-light rounded mb-3"
                            >
                                <option value="">Select parent…</option>
                                {ragas.filter((r) => r.melakartaNumber != null).map((r) => (
                                    <option key={r.id} value={r.id}>{r.name} (mela {r.melakartaNumber})</option>
                                ))}
                            </select>
                            <label className="block text-xs font-semibold text-ink-600 mb-1">Arohanam</label>
                            <input value={arohanam} onChange={(e) => setArohanam(e.target.value)}
                                className="w-full px-3 py-2 text-sm border border-border-light rounded mb-3 font-mono" />
                            <label className="block text-xs font-semibold text-ink-600 mb-1">Avarohanam</label>
                            <input value={avarohanam} onChange={(e) => setAvarohanam(e.target.value)}
                                className="w-full px-3 py-2 text-sm border border-border-light rounded mb-3 font-mono" />
                            <button
                                type="button"
                                disabled={!parentRagaId || !arohanam.trim() || !avarohanam.trim() || confirm.isPending}
                                onClick={() => confirm.mutate()}
                                className="px-4 py-2 text-sm font-semibold text-white bg-emerald-600 rounded-lg disabled:opacity-50"
                            >
                                Confirm new
                            </button>
                        </div>
                    </div>
                )}
            </div>
        </div>
    );
};
