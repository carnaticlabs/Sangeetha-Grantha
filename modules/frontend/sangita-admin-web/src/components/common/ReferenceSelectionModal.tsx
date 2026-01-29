import React, { useState, useMemo } from 'react';
import { Modal } from '../common';

interface ReferenceSelectionModalProps<T> {
    isOpen: boolean;
    onClose: () => void;
    title: string;
    items: T[];
    onSelect: (item: T) => void;
    getLabel: (item: T) => string;
    getId: (item: T) => string;
    getSubtitle?: (item: T) => string;
}

export function ReferenceSelectionModal<T>({
    isOpen,
    onClose,
    title,
    items,
    onSelect,
    getLabel,
    getId,
    getSubtitle
}: ReferenceSelectionModalProps<T>) {
    const [search, setSearch] = useState('');

    const filteredItems = useMemo(() => {
        if (!search) return items;
        const lowerSearch = search.toLowerCase();
        return items.filter(item =>
            getLabel(item).toLowerCase().includes(lowerSearch) ||
            (getSubtitle && getSubtitle(item).toLowerCase().includes(lowerSearch))
        );
    }, [items, search, getLabel, getSubtitle]);

    return (
        <Modal isOpen={isOpen} onClose={onClose} title={title} size="md">
            <div className="flex flex-col h-[60vh]">
                <div className="mb-4">
                    <div className="relative">
                        <input
                            type="text"
                            placeholder="Search..."
                            value={search}
                            onChange={(e) => setSearch(e.target.value)}
                            className="w-full px-4 py-2 pl-10 border border-slate-300 rounded-lg focus:ring-2 focus:ring-primary focus:border-transparent outline-none"
                            autoFocus
                        />
                        <span className="material-symbols-outlined absolute left-3 top-2.5 text-slate-400">search</span>
                    </div>
                </div>

                <div className="flex-1 overflow-y-auto min-h-0 border border-slate-200 rounded-lg">
                    {filteredItems.length === 0 ? (
                        <div className="p-8 text-center text-slate-500 italic">
                            No items match your search.
                        </div>
                    ) : (
                        <div className="divide-y divide-slate-100">
                            {filteredItems.map(item => (
                                <button
                                    key={getId(item)}
                                    onClick={() => {
                                        onSelect(item);
                                        onClose();
                                        setSearch('');
                                    }}
                                    className="w-full text-left px-4 py-3 hover:bg-slate-50 transition-colors flex flex-col items-start focus:outline-none focus:bg-slate-50"
                                >
                                    <span className="font-medium text-slate-900">{getLabel(item)}</span>
                                    {getSubtitle && (
                                        <span className="text-sm text-slate-500 mt-0.5">{getSubtitle(item)}</span>
                                    )}
                                </button>
                            ))}
                        </div>
                    )}
                </div>

                <div className="pt-4 text-right border-t mt-4 border-slate-100">
                    <button
                        onClick={onClose}
                        className="px-4 py-2 text-slate-600 hover:text-slate-800 font-medium"
                    >
                        Cancel
                    </button>
                </div>
            </div>
        </Modal>
    );
}
