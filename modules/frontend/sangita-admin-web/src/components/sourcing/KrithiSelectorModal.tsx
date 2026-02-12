// =============================================================================
// KrithiSelectorModal — Search and select a Krithi for manual match override
// TRACK-057 T57.7
// =============================================================================

import React, { useState, useEffect, useCallback } from 'react';

interface KrithiSelectorModalProps {
    isOpen: boolean;
    onClose: () => void;
    onSelect: (krithiId: string, krithiTitle: string) => void;
}

interface KrithiSearchResult {
    id: string;
    title: string;
    raga?: string | null;
    tala?: string | null;
    composer?: string | null;
}

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/v1';

const KrithiSelectorModal: React.FC<KrithiSelectorModalProps> = ({
    isOpen,
    onClose,
    onSelect,
}) => {
    const [query, setQuery] = useState('');
    const [results, setResults] = useState<KrithiSearchResult[]>([]);
    const [loading, setLoading] = useState(false);
    const [selected, setSelected] = useState<string | null>(null);

    useEffect(() => {
        if (isOpen) {
            setQuery('');
            setResults([]);
            setSelected(null);
        }
    }, [isOpen]);

    const search = useCallback(async (searchQuery: string) => {
        if (!searchQuery.trim() || searchQuery.length < 2) {
            setResults([]);
            return;
        }
        setLoading(true);
        try {
            const token = localStorage.getItem('authToken');
            const response = await fetch(
                `${API_BASE_URL}/admin/krithis?search=${encodeURIComponent(searchQuery)}&pageSize=20`,
                {
                    headers: {
                        'Content-Type': 'application/json',
                        ...(token ? { Authorization: `Bearer ${token}` } : {}),
                    },
                },
            );
            if (response.ok) {
                const data = await response.json();
                setResults(data.items || []);
            }
        } catch {
            // silently fail search
        } finally {
            setLoading(false);
        }
    }, []);

    useEffect(() => {
        const timer = setTimeout(() => search(query), 300);
        return () => clearTimeout(timer);
    }, [query, search]);

    const handleConfirm = () => {
        if (!selected) return;
        const match = results.find((r) => r.id === selected);
        if (match) {
            onSelect(match.id, match.title);
            onClose();
        }
    };

    if (!isOpen) return null;

    return (
        <div className="fixed inset-0 z-[60] flex items-center justify-center bg-black/50 backdrop-blur-sm" onClick={onClose}>
            <div
                className="bg-white rounded-2xl shadow-xl w-full max-w-md mx-4 max-h-[80vh] flex flex-col"
                onClick={(e) => e.stopPropagation()}
            >
                {/* Header */}
                <div className="flex items-center justify-between p-5 border-b border-border-light">
                    <h2 className="text-lg font-display font-bold text-ink-900">Select Krithi</h2>
                    <button onClick={onClose} className="text-ink-400 hover:text-ink-600 transition-colors">
                        <span className="material-symbols-outlined">close</span>
                    </button>
                </div>

                {/* Search */}
                <div className="p-4 border-b border-border-light">
                    <div className="relative">
                        <span className="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-ink-400 text-lg">search</span>
                        <input
                            type="text"
                            value={query}
                            onChange={(e) => setQuery(e.target.value)}
                            placeholder="Search by title, raga, or composer..."
                            className="w-full pl-10 pr-4 py-2.5 text-sm border border-border-light rounded-lg focus:outline-none focus:ring-2 focus:ring-primary/20"
                            autoFocus
                        />
                    </div>
                </div>

                {/* Results */}
                <div className="flex-1 overflow-y-auto min-h-0">
                    {loading && (
                        <div className="flex items-center justify-center py-8">
                            <div className="animate-spin rounded-full h-6 w-6 border-b-2 border-primary" />
                        </div>
                    )}
                    {!loading && results.length === 0 && query.length >= 2 && (
                        <div className="py-8 text-center text-sm text-ink-400 italic">No Krithis found for "{query}"</div>
                    )}
                    {!loading && results.length === 0 && query.length < 2 && (
                        <div className="py-8 text-center text-sm text-ink-400 italic">Type at least 2 characters to search</div>
                    )}
                    {results.map((krithi) => (
                        <button
                            key={krithi.id}
                            onClick={() => setSelected(krithi.id === selected ? null : krithi.id)}
                            className={`w-full text-left px-5 py-3 border-b border-border-light hover:bg-slate-50 transition-colors ${selected === krithi.id ? 'bg-primary/5 border-l-3 border-l-primary' : ''
                                }`}
                        >
                            <div className="text-sm font-semibold text-ink-800">{krithi.title}</div>
                            <div className="flex items-center gap-3 mt-0.5">
                                {krithi.raga && (
                                    <span className="text-xs text-ink-500">
                                        <span className="font-medium">Rāga:</span> {krithi.raga}
                                    </span>
                                )}
                                {krithi.tala && (
                                    <span className="text-xs text-ink-500">
                                        <span className="font-medium">Tāḷa:</span> {krithi.tala}
                                    </span>
                                )}
                                {krithi.composer && (
                                    <span className="text-xs text-ink-500">
                                        <span className="font-medium">Composer:</span> {krithi.composer}
                                    </span>
                                )}
                            </div>
                        </button>
                    ))}
                </div>

                {/* Footer */}
                <div className="p-4 border-t border-border-light flex justify-end gap-3">
                    <button
                        onClick={onClose}
                        className="px-4 py-2 text-sm font-medium text-ink-600 hover:text-ink-800 transition-colors"
                    >
                        Cancel
                    </button>
                    <button
                        onClick={handleConfirm}
                        disabled={!selected}
                        className="px-5 py-2 bg-primary text-white text-sm font-medium rounded-lg hover:bg-primary/90 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
                    >
                        Select Krithi
                    </button>
                </div>
            </div>
        </div>
    );
};

export default KrithiSelectorModal;
