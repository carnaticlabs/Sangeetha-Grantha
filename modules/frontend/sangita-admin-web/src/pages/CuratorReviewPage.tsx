import React, { useState, useEffect, useCallback } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import {
    getCuratorStats,
    getCuratorSectionIssues,
    getImports,
    reviewImport,
    searchKrithis,
    type CuratorStats,
    type SectionIssuesPage,
} from '../api/client';
import { ImportedKrithi, KrithiSummary, ResolutionResult } from '../types';
import { useToast, ToastContainer } from '../components/Toast';
import { useSourceDetail } from '../hooks/useSourcingQueries';
import { TierBadge } from '../components/sourcing/shared';
import { AuthorityWarning } from '../components/import-review/AuthorityWarning';
import ConfirmationModal from '../components/ConfirmationModal';

type Tab = 'pending' | 'sections';

type ModalState =
    | { type: 'none' }
    | { type: 'approve'; importId: string; title: string }
    | { type: 'reject'; importId: string; title: string }
    | { type: 'bulkApprove'; count: number }
    | { type: 'bulkReject'; count: number }
    | { type: 'merge'; importId: string; importTitle: string; krithi: KrithiSummary };

const CuratorReviewPage: React.FC = () => {
    const { toasts, success, error: showError, removeToast } = useToast();
    const queryClient = useQueryClient();
    const [activeTab, setActiveTab] = useState<Tab>('pending');

    // List state
    const [imports, setImports] = useState<ImportedKrithi[]>([]);
    const [selectedId, setSelectedId] = useState<string | null>(null);
    const [selectedImportIds, setSelectedImportIds] = useState<Set<string>>(new Set());
    const [loading, setLoading] = useState(false);
    const [processing, setProcessing] = useState(false);

    // Form state
    const [overrideTitle, setOverrideTitle] = useState('');
    const [overrideComposer, setOverrideComposer] = useState('');
    const [overrideRaga, setOverrideRaga] = useState('');
    const [overrideTala, setOverrideTala] = useState('');
    const [overrideLanguage, setOverrideLanguage] = useState('');
    const [overrideDeity, setOverrideDeity] = useState('');
    const [overrideTemple, setOverrideTemple] = useState('');
    const [overrideLyrics, setOverrideLyrics] = useState('');
    const [warningDismissed, setWarningDismissed] = useState(false);

    // Krithi search state
    const [searchQuery, setSearchQuery] = useState('');
    const [searchResults, setSearchResults] = useState<KrithiSummary[]>([]);
    const [searching, setSearching] = useState(false);

    // Modal state
    const [modal, setModal] = useState<ModalState>({ type: 'none' });

    // Section issues state
    const [sectionPage, setSectionPage] = useState(0);
    const pageSize = 50;

    const selectedItem = imports.find(i => i.id === selectedId);
    const { data: sourceDetail } = useSourceDetail(selectedItem?.importSourceId ?? '');

    const { data: stats } = useQuery({
        queryKey: ['curatorStats'],
        queryFn: getCuratorStats,
    });

    const { data: sectionIssues, isLoading: sectionsLoading } = useQuery({
        queryKey: ['curatorSectionIssues', sectionPage],
        queryFn: () => getCuratorSectionIssues(sectionPage, pageSize),
        enabled: activeTab === 'sections',
    });

    // Load pending imports
    const loadImports = useCallback(async () => {
        setLoading(true);
        try {
            const data = await getImports('PENDING');
            setImports(data);
            if (data.length > 0 && !selectedId) {
                selectImport(data[0]);
            }
        } catch (e) {
            showError('Failed to load pending imports');
        } finally {
            setLoading(false);
        }
    }, [selectedId]);

    useEffect(() => { loadImports(); }, []);

    const selectImport = (item: ImportedKrithi) => {
        setSelectedId(item.id);
        setWarningDismissed(false);
        setOverrideTitle(item.rawTitle || '');
        setOverrideComposer(item.rawComposer || '');
        setOverrideRaga(item.rawRaga || '');
        setOverrideTala(item.rawTala || '');
        setOverrideLanguage(item.rawLanguage || '');
        setOverrideDeity(item.rawDeity || '');
        setOverrideTemple(item.rawTemple || '');
        setOverrideLyrics(item.rawLyrics || '');
        setSearchQuery('');
        setSearchResults([]);
    };

    // Keyboard shortcuts
    useEffect(() => {
        if (activeTab !== 'pending') return;
        const handler = (e: KeyboardEvent) => {
            if (e.target instanceof HTMLInputElement || e.target instanceof HTMLTextAreaElement) return;
            if (modal.type !== 'none') return;
            const idx = imports.findIndex(i => i.id === selectedId);

            switch (e.key) {
                case 'j':
                case 'ArrowDown':
                    e.preventDefault();
                    if (idx < imports.length - 1) selectImport(imports[idx + 1]);
                    break;
                case 'k':
                case 'ArrowUp':
                    e.preventDefault();
                    if (idx > 0) selectImport(imports[idx - 1]);
                    break;
                case 'a':
                    e.preventDefault();
                    if (selectedItem) setModal({ type: 'approve', importId: selectedItem.id, title: selectedItem.rawTitle || 'Untitled' });
                    break;
                case 'r':
                    e.preventDefault();
                    if (selectedItem) setModal({ type: 'reject', importId: selectedItem.id, title: selectedItem.rawTitle || 'Untitled' });
                    break;
            }
        };
        window.addEventListener('keydown', handler);
        return () => window.removeEventListener('keydown', handler);
    }, [activeTab, imports, selectedId, selectedItem, modal]);

    // Action handlers
    const handleApprove = async (notes?: string) => {
        if (modal.type !== 'approve') return;
        setProcessing(true);
        try {
            await reviewImport(modal.importId, {
                status: 'APPROVED',
                reviewerNotes: notes || null,
                overrides: {
                    title: overrideTitle, composer: overrideComposer, raga: overrideRaga,
                    tala: overrideTala, language: overrideLanguage, deity: overrideDeity,
                    temple: overrideTemple, lyrics: overrideLyrics,
                },
            });
            success('Import approved');
            setModal({ type: 'none' });
            setSelectedId(null);
            await loadImports();
            queryClient.invalidateQueries({ queryKey: ['curatorStats'] });
        } catch (e) {
            showError('Failed to approve import');
        } finally {
            setProcessing(false);
        }
    };

    const handleReject = async (notes?: string) => {
        if (modal.type !== 'reject') return;
        setProcessing(true);
        try {
            await reviewImport(modal.importId, { status: 'REJECTED', reviewerNotes: notes || null });
            success('Import rejected');
            setModal({ type: 'none' });
            setSelectedId(null);
            await loadImports();
            queryClient.invalidateQueries({ queryKey: ['curatorStats'] });
        } catch (e) {
            showError('Failed to reject import');
        } finally {
            setProcessing(false);
        }
    };

    const handleBulkAction = async (action: 'APPROVED' | 'REJECTED', notes?: string) => {
        setProcessing(true);
        try {
            const promises = Array.from(selectedImportIds).map(id =>
                reviewImport(id, { status: action, reviewerNotes: notes || null })
            );
            await Promise.all(promises);
            success(`${selectedImportIds.size} imports ${action.toLowerCase()}`);
            setSelectedImportIds(new Set());
            setModal({ type: 'none' });
            await loadImports();
            queryClient.invalidateQueries({ queryKey: ['curatorStats'] });
        } catch (e) {
            showError(`Failed to bulk ${action.toLowerCase()} imports`);
        } finally {
            setProcessing(false);
        }
    };

    const handleMerge = async (notes?: string) => {
        if (modal.type !== 'merge') return;
        setProcessing(true);
        try {
            await reviewImport(modal.importId, {
                status: 'MAPPED',
                mappedKrithiId: modal.krithi.id,
                reviewerNotes: notes || null,
            });
            success(`Merged into "${modal.krithi.name}"`);
            setModal({ type: 'none' });
            setSelectedId(null);
            await loadImports();
            queryClient.invalidateQueries({ queryKey: ['curatorStats'] });
        } catch (e) {
            showError('Failed to merge import');
        } finally {
            setProcessing(false);
        }
    };

    // Krithi search
    const handleSearch = async () => {
        if (!searchQuery.trim()) return;
        setSearching(true);
        try {
            const result = await searchKrithis({ query: searchQuery.trim(), pageSize: 10 });
            setSearchResults(result.items);
        } catch (e) {
            showError('Search failed');
        } finally {
            setSearching(false);
        }
    };

    useEffect(() => {
        if (!searchQuery.trim()) { setSearchResults([]); return; }
        const timer = setTimeout(handleSearch, 400);
        return () => clearTimeout(timer);
    }, [searchQuery]);

    const toggleSelectImport = (id: string) => {
        const newSet = new Set(selectedImportIds);
        if (newSet.has(id)) newSet.delete(id); else newSet.add(id);
        setSelectedImportIds(newSet);
    };

    const toggleSelectAll = () => {
        if (selectedImportIds.size === imports.length) setSelectedImportIds(new Set());
        else setSelectedImportIds(new Set(imports.map(i => i.id)));
    };

    // Resolution panel
    const renderResolutionPanel = () => {
        if (!selectedItem?.resolutionData) return null;
        let resolution: ResolutionResult;
        try { resolution = JSON.parse(selectedItem.resolutionData); } catch { return null; }

        const renderCandidates = (label: string, candidates: any[] | undefined, setter: (v: string) => void) => (
            <div>
                <div className="text-[10px] font-semibold text-indigo-500 mb-1">{label}</div>
                {!candidates?.length ? <span className="text-xs italic text-gray-400">No matches</span> : (
                    <div className="flex flex-col gap-1">
                        {candidates.map((c: any, i: number) => (
                            <button key={i} onClick={() => setter(c.entity.name)}
                                className="text-left text-xs px-2 py-1 bg-white border border-indigo-100 rounded hover:border-indigo-300 flex justify-between items-center">
                                <span>{c.entity.name}</span>
                                <span className={`text-[9px] font-bold ${c.confidence === 'HIGH' ? 'text-green-600' : 'text-amber-600'}`}>{c.score}%</span>
                            </button>
                        ))}
                    </div>
                )}
            </div>
        );

        return (
            <div className="p-3 bg-indigo-50 border border-indigo-100 rounded-lg">
                <h4 className="text-xs font-bold text-indigo-900 mb-2 uppercase tracking-wide">AI Resolution Candidates</h4>
                <div className="grid grid-cols-2 gap-4">
                    {renderCandidates('COMPOSER', resolution.composerCandidates, setOverrideComposer)}
                    {renderCandidates('RAGA', resolution.ragaCandidates, setOverrideRaga)}
                    {renderCandidates('DEITY', resolution.deityCandidates, setOverrideDeity)}
                    {renderCandidates('TEMPLE', resolution.templeCandidates, setOverrideTemple)}
                </div>
            </div>
        );
    };

    return (
        <div className="max-w-7xl mx-auto h-[calc(100vh-140px)] flex flex-col">
            <ToastContainer toasts={toasts} onRemove={removeToast} />

            {/* Header + Stats */}
            <div className="mb-4 space-y-4">
                <div className="flex items-center justify-between">
                    <h1 className="text-2xl font-display font-bold text-ink-900">Curator Review</h1>
                    {selectedImportIds.size > 0 && activeTab === 'pending' && (
                        <div className="flex gap-2 items-center">
                            <span className="px-3 py-1.5 text-sm font-semibold text-ink-700 bg-slate-100 rounded-lg">{selectedImportIds.size} selected</span>
                            <button onClick={() => setModal({ type: 'bulkApprove', count: selectedImportIds.size })}
                                className="px-4 py-1.5 text-sm font-semibold text-white bg-green-600 rounded-lg hover:bg-green-700">Approve Selected</button>
                            <button onClick={() => setModal({ type: 'bulkReject', count: selectedImportIds.size })}
                                className="px-4 py-1.5 text-sm font-semibold text-rose-700 bg-rose-50 border border-rose-200 rounded-lg hover:bg-rose-100">Reject Selected</button>
                        </div>
                    )}
                </div>

                {stats && (
                    <div className="grid grid-cols-2 md:grid-cols-5 gap-3">
                        <StatCard label="Total Krithis" value={stats.totalKrithis} />
                        <StatCard label="Pending" value={stats.totalPending} color="text-yellow-600" />
                        <StatCard label="Approved" value={stats.totalApproved} color="text-green-600" />
                        <StatCard label="Rejected" value={stats.totalRejected} color="text-red-600" />
                        <StatCard label="Section Issues" value={stats.sectionIssuesCount} color="text-orange-600" />
                    </div>
                )}

                {/* Tabs */}
                <div className="border-b border-border-light">
                    <nav className="flex gap-6">
                        <TabButton active={activeTab === 'pending'} onClick={() => setActiveTab('pending')} label="Pending Matches" count={stats?.totalPending} />
                        <TabButton active={activeTab === 'sections'} onClick={() => setActiveTab('sections')} label="Section Issues" count={stats?.sectionIssuesCount} />
                    </nav>
                </div>
            </div>

            {/* Tab Content */}
            {activeTab === 'pending' ? (
                <div className="flex-1 flex gap-0 overflow-hidden border border-border-light rounded-xl bg-white shadow-sm">
                    {/* Left: List Panel */}
                    <div className="w-1/3 border-r border-border-light flex flex-col">
                        <div className="p-3 border-b border-border-light bg-slate-50 flex justify-between items-center">
                            <div className="flex items-center gap-2">
                                <input type="checkbox" checked={imports.length > 0 && selectedImportIds.size === imports.length}
                                    onChange={toggleSelectAll} className="w-4 h-4 rounded border-gray-300 text-primary focus:ring-primary cursor-pointer" />
                                <span className="text-xs font-bold text-ink-500 uppercase">Pending ({imports.length})</span>
                            </div>
                            <button onClick={loadImports} className="text-primary hover:text-primary-dark">
                                <span className="material-symbols-outlined text-sm">refresh</span>
                            </button>
                        </div>
                        <div className="flex-1 overflow-y-auto">
                            {loading ? (
                                <div className="p-4 text-center text-sm text-gray-400">Loading...</div>
                            ) : imports.length === 0 ? (
                                <div className="p-4 text-center text-sm text-gray-400">Queue is empty.</div>
                            ) : imports.map(item => (
                                <div key={item.id}
                                    className={`p-3 border-b border-border-light hover:bg-slate-50 ${selectedId === item.id ? 'bg-primary-50 border-l-4 border-l-primary' : 'border-l-4 border-l-transparent'}`}>
                                    <div className="flex items-start gap-2">
                                        <input type="checkbox" checked={selectedImportIds.has(item.id)}
                                            onChange={(e) => { e.stopPropagation(); toggleSelectImport(item.id); }}
                                            className="mt-1 w-4 h-4 rounded border-gray-300 text-primary focus:ring-primary cursor-pointer" />
                                        <div className="flex-1 cursor-pointer" onClick={() => selectImport(item)}>
                                            <div className="font-semibold text-sm text-ink-900 truncate">{item.rawTitle || 'Untitled'}</div>
                                            <div className="text-xs text-ink-500 truncate">{item.rawComposer || 'Unknown'} {item.rawRaga ? `\u2022 ${item.rawRaga}` : ''}</div>
                                            <div className="text-[10px] text-ink-400 mt-1 truncate">{item.sourceKey}</div>
                                        </div>
                                    </div>
                                </div>
                            ))}
                        </div>
                    </div>

                    {/* Right: Detail Panel */}
                    <div className="flex-1 flex flex-col overflow-hidden">
                        {selectedItem ? (
                            <>
                                {/* Header */}
                                <div className="p-4 border-b border-border-light bg-slate-50 flex justify-between items-center">
                                    <div>
                                        <div className="text-xs text-ink-400 uppercase font-bold tracking-wider flex items-center gap-2">
                                            Source
                                            {sourceDetail?.sourceTier != null && <TierBadge tier={sourceDetail.sourceTier} />}
                                        </div>
                                        <a href={selectedItem.sourceKey || '#'} target="_blank" rel="noreferrer"
                                            className="text-sm text-primary hover:underline truncate block max-w-md">
                                            {selectedItem.sourceKey}
                                        </a>
                                    </div>
                                    <div className="flex gap-2">
                                        <button onClick={() => setModal({ type: 'reject', importId: selectedItem.id, title: selectedItem.rawTitle || 'Untitled' })}
                                            disabled={processing}
                                            className="px-3 py-1.5 text-xs font-semibold text-rose-700 bg-rose-50 border border-rose-200 rounded hover:bg-rose-100 disabled:opacity-50">
                                            Reject
                                        </button>
                                        <button onClick={() => setModal({ type: 'approve', importId: selectedItem.id, title: selectedItem.rawTitle || 'Untitled' })}
                                            disabled={processing}
                                            className="px-3 py-1.5 text-xs font-semibold text-white bg-primary border border-primary rounded hover:bg-primary-dark disabled:opacity-50">
                                            {processing ? 'Processing...' : 'Approve & Create'}
                                        </button>
                                    </div>
                                </div>

                                {/* Scrollable content */}
                                <div className="flex-1 overflow-y-auto p-6 space-y-6">
                                    {/* Manual Resolution alert */}
                                    {selectedItem.resolutionData && (() => {
                                        try { return !JSON.parse(selectedItem.resolutionData!).resolved; } catch { return false; }
                                    })() && (
                                        <div className="p-3 bg-amber-50 border border-amber-200 rounded-lg flex items-center gap-3">
                                            <span className="material-symbols-outlined text-amber-600">contact_support</span>
                                            <div className="text-sm text-amber-900">
                                                <p className="font-bold">Manual Resolution Required</p>
                                                <p className="text-xs">Some entities could not be automatically matched. Select candidates below or provide overrides.</p>
                                            </div>
                                        </div>
                                    )}

                                    {renderResolutionPanel()}

                                    {sourceDetail && sourceDetail.sourceTier != null && sourceDetail.sourceTier > 2 && (
                                        <AuthorityWarning currentTier={sourceDetail.sourceTier} conflicts={[]}
                                            onDismiss={() => setWarningDismissed(true)} dismissed={warningDismissed} />
                                    )}

                                    {/* Metadata form */}
                                    <div className="grid grid-cols-2 gap-6">
                                        <div className="space-y-3">
                                            <h3 className="text-sm font-bold text-ink-900 border-b pb-2">Primary Metadata</h3>
                                            <FormField label="Title" value={overrideTitle} onChange={setOverrideTitle} />
                                            <FormField label="Composer" value={overrideComposer} onChange={setOverrideComposer} />
                                            <FormField label="Raga" value={overrideRaga} onChange={setOverrideRaga} />
                                            <FormField label="Tala" value={overrideTala} onChange={setOverrideTala} />
                                            <div className="grid grid-cols-2 gap-3">
                                                <FormField label="Deity" value={overrideDeity} onChange={setOverrideDeity} />
                                                <FormField label="Temple" value={overrideTemple} onChange={setOverrideTemple} />
                                            </div>
                                            <FormField label="Language" value={overrideLanguage} onChange={setOverrideLanguage} />
                                        </div>
                                        <div className="space-y-3">
                                            <h3 className="text-sm font-bold text-ink-900 border-b pb-2">Lyrics & Content</h3>
                                            <div>
                                                <label className="block text-xs font-semibold text-ink-600 mb-1">Lyrics Preview</label>
                                                <textarea value={overrideLyrics} onChange={e => setOverrideLyrics(e.target.value)}
                                                    rows={16}
                                                    className="w-full px-3 py-2 text-xs font-mono border border-border-light rounded focus:ring-2 focus:ring-primary focus:border-transparent" />
                                            </div>
                                        </div>
                                    </div>

                                    {/* Find Existing Krithi */}
                                    <div className="border-t border-border-light pt-4">
                                        <h3 className="text-sm font-bold text-ink-900 mb-3 flex items-center gap-2">
                                            <span className="material-symbols-outlined text-base">search</span>
                                            Find Existing Krithi to Merge
                                        </h3>
                                        <input
                                            value={searchQuery}
                                            onChange={(e) => setSearchQuery(e.target.value)}
                                            placeholder="Search by title, composer, or raga..."
                                            className="w-full px-3 py-2 text-sm border border-border-light rounded-lg focus:ring-2 focus:ring-primary focus:border-transparent"
                                        />
                                        {searching && <p className="text-xs text-ink-400 mt-2">Searching...</p>}
                                        {searchResults.length > 0 && (
                                            <div className="mt-3 border border-border-light rounded-lg divide-y divide-border-light max-h-60 overflow-y-auto">
                                                {searchResults.map(k => (
                                                    <div key={k.id} className="p-3 flex items-center justify-between hover:bg-slate-50">
                                                        <div>
                                                            <div className="text-sm font-medium text-ink-900">{k.name}</div>
                                                            <div className="text-xs text-ink-500">
                                                                {k.composerName}
                                                                {k.ragas?.length > 0 && ` \u2022 ${k.ragas.map(r => r.name).join(', ')}`}
                                                            </div>
                                                        </div>
                                                        <button
                                                            onClick={() => setModal({
                                                                type: 'merge',
                                                                importId: selectedItem.id,
                                                                importTitle: selectedItem.rawTitle || 'Untitled',
                                                                krithi: k,
                                                            })}
                                                            className="px-3 py-1 text-xs font-semibold text-blue-700 bg-blue-50 border border-blue-200 rounded hover:bg-blue-100">
                                                            Merge
                                                        </button>
                                                    </div>
                                                ))}
                                            </div>
                                        )}
                                        {searchQuery.trim() && !searching && searchResults.length === 0 && (
                                            <p className="text-xs text-ink-400 mt-2">No matching krithis found.</p>
                                        )}
                                    </div>
                                </div>
                            </>
                        ) : (
                            <div className="flex-1 flex items-center justify-center text-ink-400 text-sm">
                                Select an import to review.
                            </div>
                        )}
                    </div>
                </div>
            ) : (
                /* Section Issues Tab */
                <SectionIssuesTab data={sectionIssues} loading={sectionsLoading} page={sectionPage} onPageChange={setSectionPage} pageSize={pageSize} />
            )}

            {/* Keyboard hint */}
            {activeTab === 'pending' && (
                <div className="mt-2 text-xs text-ink-400 flex gap-4">
                    <span><kbd className="px-1 py-0.5 bg-slate-100 rounded text-[10px]">j/k</kbd> navigate</span>
                    <span><kbd className="px-1 py-0.5 bg-slate-100 rounded text-[10px]">a</kbd> approve</span>
                    <span><kbd className="px-1 py-0.5 bg-slate-100 rounded text-[10px]">r</kbd> reject</span>
                </div>
            )}

            {/* Confirmation Modals */}
            <ConfirmationModal
                isOpen={modal.type === 'approve'}
                title="Approve Import"
                message={modal.type === 'approve' ? `Approve "${modal.title}" and create a new krithi?` : ''}
                confirmLabel="Approve & Create"
                confirmColor="green"
                showNotes
                notesPlaceholder="Reviewer notes (optional)..."
                onConfirm={handleApprove}
                onCancel={() => setModal({ type: 'none' })}
            />
            <ConfirmationModal
                isOpen={modal.type === 'reject'}
                title="Reject Import"
                message={modal.type === 'reject' ? `Reject "${modal.title}"? This will discard the import.` : ''}
                confirmLabel="Reject"
                confirmColor="red"
                showNotes
                notesRequired
                notesPlaceholder="Reason for rejection (required)..."
                onConfirm={handleReject}
                onCancel={() => setModal({ type: 'none' })}
            />
            <ConfirmationModal
                isOpen={modal.type === 'bulkApprove'}
                title="Bulk Approve"
                message={modal.type === 'bulkApprove' ? `Approve ${modal.count} selected imports?` : ''}
                confirmLabel="Approve All"
                confirmColor="green"
                showNotes
                onConfirm={(notes) => handleBulkAction('APPROVED', notes)}
                onCancel={() => setModal({ type: 'none' })}
            />
            <ConfirmationModal
                isOpen={modal.type === 'bulkReject'}
                title="Bulk Reject"
                message={modal.type === 'bulkReject' ? `Reject ${modal.count} selected imports?` : ''}
                confirmLabel="Reject All"
                confirmColor="red"
                showNotes
                notesRequired
                notesPlaceholder="Reason for rejection (required)..."
                onConfirm={(notes) => handleBulkAction('REJECTED', notes)}
                onCancel={() => setModal({ type: 'none' })}
            />
            <ConfirmationModal
                isOpen={modal.type === 'merge'}
                title="Merge into Existing Krithi"
                message={modal.type === 'merge' ? `Merge import "${modal.importTitle}" into existing krithi "${modal.krithi.name}"?\n\nThe import's lyrics and metadata will be linked to this krithi.` : ''}
                confirmLabel="Merge"
                confirmColor="blue"
                showNotes
                notesPlaceholder="Merge notes (optional)..."
                onConfirm={handleMerge}
                onCancel={() => setModal({ type: 'none' })}
            />
        </div>
    );
};

// --- Sub-components ---

const StatCard: React.FC<{ label: string; value: number; color?: string }> = ({ label, value, color }) => (
    <div className="bg-white rounded-lg border border-border-light p-3">
        <p className="text-[10px] text-ink-500 uppercase tracking-wide">{label}</p>
        <p className={`text-xl font-bold mt-0.5 ${color || 'text-ink-900'}`}>{value.toLocaleString()}</p>
    </div>
);

const TabButton: React.FC<{ active: boolean; onClick: () => void; label: string; count?: number }> = ({ active, onClick, label, count }) => (
    <button onClick={onClick}
        className={`pb-3 text-sm font-medium border-b-2 transition-colors ${active ? 'border-primary text-primary' : 'border-transparent text-ink-500 hover:text-ink-700'}`}>
        {label}
        {count != null && (
            <span className={`ml-2 px-2 py-0.5 rounded-full text-xs ${active ? 'bg-primary-light text-primary' : 'bg-slate-100 text-ink-500'}`}>{count}</span>
        )}
    </button>
);

const FormField: React.FC<{ label: string; value: string; onChange: (v: string) => void }> = ({ label, value, onChange }) => (
    <div>
        <label className="block text-xs font-semibold text-ink-600 mb-1">{label}</label>
        <input value={value} onChange={e => onChange(e.target.value)}
            className="w-full px-3 py-2 text-sm border border-border-light rounded focus:ring-2 focus:ring-primary focus:border-transparent" />
    </div>
);

const SectionIssuesTab: React.FC<{
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
                                <td className="px-4 py-3 font-medium text-ink-900">{issue.title}</td>
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

export default CuratorReviewPage;
