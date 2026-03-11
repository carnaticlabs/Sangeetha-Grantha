import React, { useState, useMemo } from 'react';
import type { Raga } from '../../types';

interface RagaTreeGridProps {
    data: Raga[];
    loading: boolean;
    onEdit: (item: Raga) => void;
    onDelete?: (item: Raga) => void;
    onCreate: () => void;
    onBack: () => void;
}

interface MelakartaGroup {
    melakarta: Raga;
    janyas: Raga[];
}

const JANYAS_PER_PAGE = 20;

const GRID_STYLE: React.CSSProperties = {
    display: 'grid',
    gridTemplateColumns: 'minmax(0, 2fr) 60px minmax(0, 1.5fr) minmax(0, 1.5fr) 80px',
    gap: '0.5rem',
};

export const RagaTreeGrid: React.FC<RagaTreeGridProps> = ({
    data,
    loading,
    onEdit,
    onDelete,
    onCreate,
    onBack,
}) => {
    const [searchTerm, setSearchTerm] = useState('');
    const [expandedIds, setExpandedIds] = useState<Set<string>>(new Set());
    const [janyaPages, setJanyaPages] = useState<Record<string, number>>({});

    // Group ragas into melakarta hierarchy
    const { groups, orphans } = useMemo(() => {
        const melakartas = data
            .filter(r => r.melakartaNumber != null)
            .sort((a, b) => (a.melakartaNumber ?? 0) - (b.melakartaNumber ?? 0));

        const melakartaById = new Map<string, Raga>();
        melakartas.forEach(m => melakartaById.set(m.id, m));

        const janyasByParent = new Map<string, Raga[]>();
        const orphanList: Raga[] = [];

        data.forEach(r => {
            if (r.melakartaNumber != null) return; // skip melakartas
            if (r.parentRagaId && melakartaById.has(r.parentRagaId)) {
                const list = janyasByParent.get(r.parentRagaId) ?? [];
                list.push(r);
                janyasByParent.set(r.parentRagaId, list);
            } else {
                orphanList.push(r);
            }
        });

        // Sort janyas alphabetically within each group
        janyasByParent.forEach(list => list.sort((a, b) => a.name.localeCompare(b.name)));
        orphanList.sort((a, b) => a.name.localeCompare(b.name));

        const grps: MelakartaGroup[] = melakartas.map(m => ({
            melakarta: m,
            janyas: janyasByParent.get(m.id) ?? [],
        }));

        return { groups: grps, orphans: orphanList };
    }, [data]);

    // Filter groups by search term
    const { filteredGroups, filteredOrphans, autoExpandIds } = useMemo(() => {
        if (!searchTerm.trim()) {
            return { filteredGroups: groups, filteredOrphans: orphans, autoExpandIds: new Set<string>() };
        }
        const q = searchTerm.toLowerCase();
        const autoExp = new Set<string>();

        const fGroups = groups
            .map(g => {
                const melakartaMatches = g.melakarta.name.toLowerCase().includes(q) ||
                    g.melakarta.nameNormalized?.toLowerCase().includes(q);
                const matchingJanyas = g.janyas.filter(j =>
                    j.name.toLowerCase().includes(q) || j.nameNormalized?.toLowerCase().includes(q)
                );

                if (matchingJanyas.length > 0) {
                    autoExp.add(g.melakarta.id);
                    return { melakarta: g.melakarta, janyas: matchingJanyas };
                }
                if (melakartaMatches) {
                    return g; // show all janyas when melakarta matches
                }
                return null;
            })
            .filter((g): g is MelakartaGroup => g !== null);

        const fOrphans = orphans.filter(r =>
            r.name.toLowerCase().includes(q) || r.nameNormalized?.toLowerCase().includes(q)
        );

        return { filteredGroups: fGroups, filteredOrphans: fOrphans, autoExpandIds: autoExp };
    }, [groups, orphans, searchTerm]);

    const isExpanded = (id: string) => expandedIds.has(id) || autoExpandIds.has(id);

    const toggleExpand = (id: string) => {
        setExpandedIds(prev => {
            const next = new Set(prev);
            if (next.has(id)) next.delete(id);
            else next.add(id);
            return next;
        });
    };

    const getVisibleJanyas = (melakartaId: string, janyas: Raga[]) => {
        const page = janyaPages[melakartaId] ?? 1;
        return janyas.slice(0, page * JANYAS_PER_PAGE);
    };

    const loadMoreJanyas = (melakartaId: string) => {
        setJanyaPages(prev => ({ ...prev, [melakartaId]: (prev[melakartaId] ?? 1) + 1 }));
    };

    const totalMelakartas = groups.length;
    const totalJanyas = data.length - totalMelakartas;

    return (
        <div className="space-y-6 animate-fadeIn">
            {/* Header */}
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
                <nav className="flex items-center gap-2 text-sm text-ink-500">
                    <span onClick={onBack} className="hover:text-primary cursor-pointer transition-colors">Reference Data</span>
                    <span className="material-symbols-outlined text-[14px]">chevron_right</span>
                    <span className="font-medium text-ink-900">Ragas</span>
                </nav>
                <div className="flex items-center gap-3">
                    <span className="text-xs text-ink-400">{totalMelakartas} melakartas · {totalJanyas} janyas</span>
                    <button
                        onClick={onCreate}
                        className="px-4 py-2 bg-primary text-white rounded-lg text-sm font-medium hover:bg-primary-dark transition-colors flex items-center gap-2"
                    >
                        <span className="material-symbols-outlined text-[18px]">add</span>
                        Create New
                    </button>
                </div>
            </div>

            {/* Search */}
            <div className="relative">
                <span className="material-symbols-outlined absolute left-4 top-1/2 -translate-y-1/2 text-ink-400">search</span>
                <input
                    type="text"
                    placeholder="Search ragas by name..."
                    value={searchTerm}
                    onChange={(e) => {
                        setSearchTerm(e.target.value);
                        setJanyaPages({});
                    }}
                    className="w-full pl-11 pr-4 py-3 bg-white border border-border-light rounded-lg text-ink-900 focus:ring-2 focus:ring-primary focus:border-transparent outline-none transition-all"
                />
            </div>

            {/* Tree Grid */}
            {loading ? (
                <div className="text-center py-12 text-ink-500">Loading ragas...</div>
            ) : filteredGroups.length === 0 && filteredOrphans.length === 0 ? (
                <div className="text-center py-12 bg-slate-50 rounded-lg border border-border-light">
                    <p className="text-ink-500">No ragas found.</p>
                    {searchTerm && <button onClick={() => setSearchTerm('')} className="text-primary hover:underline mt-2 text-sm">Clear Search</button>}
                </div>
            ) : (
                <div className="bg-white rounded-xl border border-border-light overflow-hidden shadow-sm">
                    {/* Table header */}
                    <div className="px-4 py-3 bg-slate-50 border-b border-border-light text-xs font-bold text-ink-500 uppercase tracking-wider" style={GRID_STYLE}>
                        <div>Raga</div>
                        <div className="text-center">#</div>
                        <div>Arohanam</div>
                        <div>Avarohanam</div>
                        <div></div>
                    </div>

                    <div className="divide-y divide-border-light">
                        {/* Melakarta groups */}
                        {filteredGroups.map(({ melakarta, janyas }) => (
                            <div key={melakarta.id}>
                                {/* Melakarta row */}
                                <div
                                    className="px-4 py-3 hover:bg-blue-50/50 transition-colors cursor-pointer group items-center" style={GRID_STYLE}
                                    onClick={() => toggleExpand(melakarta.id)}
                                >
                                    <div className="flex items-center gap-2 min-w-0">
                                        <span className="material-symbols-outlined text-[18px] text-ink-400 flex-shrink-0 transition-transform duration-200"
                                              style={{ transform: isExpanded(melakarta.id) ? 'rotate(90deg)' : 'rotate(0deg)' }}>
                                            chevron_right
                                        </span>
                                        <span className="font-bold text-ink-900 truncate">{melakarta.name}</span>
                                        {janyas.length > 0 && (
                                            <span className="text-[10px] bg-blue-100 text-blue-700 px-1.5 py-0.5 rounded-full font-medium flex-shrink-0">
                                                {janyas.length}
                                            </span>
                                        )}
                                    </div>
                                    <div className="text-center">
                                        <span className="inline-flex items-center justify-center w-7 h-7 bg-blue-600 text-white text-xs font-bold rounded-md">
                                            {melakarta.melakartaNumber}
                                        </span>
                                    </div>
                                    <div className="text-xs text-ink-600 font-mono truncate" title={melakarta.arohanam ?? ''}>
                                        {melakarta.arohanam || '-'}
                                    </div>
                                    <div className="text-xs text-ink-600 font-mono truncate" title={melakarta.avarohanam ?? ''}>
                                        {melakarta.avarohanam || '-'}
                                    </div>
                                    <div className="flex items-center justify-end gap-1 opacity-0 group-hover:opacity-100 transition-opacity">
                                        <button
                                            onClick={(e) => { e.stopPropagation(); onEdit(melakarta); }}
                                            className="p-1.5 text-ink-400 hover:text-primary rounded transition-colors"
                                            title="Edit"
                                        >
                                            <span className="material-symbols-outlined text-[18px]">edit</span>
                                        </button>
                                    </div>
                                </div>

                                {/* Janya rows (expanded) */}
                                {isExpanded(melakarta.id) && janyas.length > 0 && (
                                    <div className="bg-slate-50/50">
                                        {getVisibleJanyas(melakarta.id, janyas).map((janya, idx) => (
                                            <div
                                                key={janya.id}
                                                className="px-4 py-2.5 hover:bg-slate-100 transition-colors group items-center border-t border-border-light/50" style={GRID_STYLE}
                                            >
                                                <div className="flex items-center gap-2 min-w-0 pl-7">
                                                    <span className="text-ink-300 text-xs flex-shrink-0">
                                                        {idx === getVisibleJanyas(melakarta.id, janyas).length - 1 &&
                                                         getVisibleJanyas(melakarta.id, janyas).length >= janyas.length ? '└' : '├'}
                                                    </span>
                                                    <span className="text-sm text-ink-800 truncate">{janya.name}</span>
                                                </div>
                                                <div></div>
                                                <div className="text-xs text-ink-500 font-mono truncate" title={janya.arohanam ?? ''}>
                                                    {janya.arohanam || '-'}
                                                </div>
                                                <div className="text-xs text-ink-500 font-mono truncate" title={janya.avarohanam ?? ''}>
                                                    {janya.avarohanam || '-'}
                                                </div>
                                                <div className="flex items-center justify-end gap-1 opacity-0 group-hover:opacity-100 transition-opacity">
                                                    <button
                                                        onClick={() => onEdit(janya)}
                                                        className="p-1.5 text-ink-400 hover:text-primary rounded transition-colors"
                                                        title="Edit"
                                                    >
                                                        <span className="material-symbols-outlined text-[18px]">edit</span>
                                                    </button>
                                                    {onDelete && (
                                                        <button
                                                            onClick={() => {
                                                                if (confirm(`Delete "${janya.name}"?`)) onDelete(janya);
                                                            }}
                                                            className="p-1.5 text-ink-400 hover:text-red-500 rounded transition-colors"
                                                            title="Delete"
                                                        >
                                                            <span className="material-symbols-outlined text-[18px]">delete</span>
                                                        </button>
                                                    )}
                                                </div>
                                            </div>
                                        ))}

                                        {/* Load more */}
                                        {getVisibleJanyas(melakarta.id, janyas).length < janyas.length && (
                                            <div className="px-4 py-2 pl-14 border-t border-border-light/50">
                                                <button
                                                    onClick={() => loadMoreJanyas(melakarta.id)}
                                                    className="text-xs text-primary hover:text-primary-dark font-medium hover:underline"
                                                >
                                                    Show more ({janyas.length - getVisibleJanyas(melakarta.id, janyas).length} remaining)
                                                </button>
                                            </div>
                                        )}
                                    </div>
                                )}
                            </div>
                        ))}

                        {/* Orphan / Unclassified ragas */}
                        {filteredOrphans.length > 0 && (
                            <div>
                                <div className="px-4 py-3 bg-amber-50/50 border-t border-border-light">
                                    <span className="text-xs font-bold text-amber-700 uppercase tracking-wider">
                                        Unclassified ({filteredOrphans.length})
                                    </span>
                                </div>
                                {filteredOrphans.map(raga => (
                                    <div
                                        key={raga.id}
                                        className="px-4 py-2.5 hover:bg-slate-50 transition-colors group items-center border-t border-border-light/50" style={GRID_STYLE}
                                    >
                                        <div className="flex items-center gap-2 min-w-0 pl-7">
                                            <span className="text-sm text-ink-800 truncate">{raga.name}</span>
                                        </div>
                                        <div></div>
                                        <div className="text-xs text-ink-500 font-mono truncate" title={raga.arohanam ?? ''}>
                                            {raga.arohanam || '-'}
                                        </div>
                                        <div className="text-xs text-ink-500 font-mono truncate" title={raga.avarohanam ?? ''}>
                                            {raga.avarohanam || '-'}
                                        </div>
                                        <div className="flex items-center justify-end gap-1 opacity-0 group-hover:opacity-100 transition-opacity">
                                            <button
                                                onClick={() => onEdit(raga)}
                                                className="p-1.5 text-ink-400 hover:text-primary rounded transition-colors"
                                                title="Edit"
                                            >
                                                <span className="material-symbols-outlined text-[18px]">edit</span>
                                            </button>
                                            {onDelete && (
                                                <button
                                                    onClick={() => {
                                                        if (confirm(`Delete "${raga.name}"?`)) onDelete(raga);
                                                    }}
                                                    className="p-1.5 text-ink-400 hover:text-red-500 rounded transition-colors"
                                                    title="Delete"
                                                >
                                                    <span className="material-symbols-outlined text-[18px]">delete</span>
                                                </button>
                                            )}
                                        </div>
                                    </div>
                                ))}
                            </div>
                        )}
                    </div>

                    {/* Footer */}
                    <div className="p-3 bg-slate-50 text-xs text-center text-ink-500 border-t border-border-light">
                        {filteredGroups.length} melakartas · {filteredGroups.reduce((sum, g) => sum + g.janyas.length, 0) + filteredOrphans.length} janyas
                        {searchTerm && ` (filtered from ${data.length} total)`}
                    </div>
                </div>
            )}
        </div>
    );
};
