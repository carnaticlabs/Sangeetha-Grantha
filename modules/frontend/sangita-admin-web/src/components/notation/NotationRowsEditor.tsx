import React, { useState } from 'react';
import { NotationRow } from '../../types';

interface NotationRowsEditorProps {
    variantId: string;
    sections: Array<{ id: string; sectionType: string; orderIndex: number; label?: string | null }>;
    rowsBySectionId: Record<string, NotationRow[]>;
    onAddRow: (sectionId: string) => void;
    onUpdateRow: (rowId: string, payload: Partial<NotationRow>) => void;
    onDeleteRow: (rowId: string) => void;
}

const NotationRowsEditor: React.FC<NotationRowsEditorProps> = ({
    variantId,
    sections,
    rowsBySectionId,
    onAddRow,
    onUpdateRow,
    onDeleteRow
}) => {
    // Basic local state for tracking unsaved inputs could go here, 
    // but for MVP we'll trigger update on blur/change directly or use a saving state.

    // Helper to sort rows
    const getRows = (sectionId: string) => {
        return (rowsBySectionId[sectionId] || []).sort((a, b) => a.orderIndex - b.orderIndex);
    };

    return (
        <div className="bg-white border border-border-light rounded-xl shadow-sm h-full flex flex-col overflow-hidden">
            <div className="p-4 border-b border-border-light bg-slate-50 flex justify-between items-center">
                <h3 className="font-display text-lg font-bold text-ink-900">
                    Notation Content <span className="text-sm font-normal text-ink-500 ml-2">(Variant ID: ...{variantId.slice(-4)})</span>
                </h3>
            </div>

            <div className="flex-1 overflow-y-auto p-6 space-y-8">
                {sections.map(section => (
                    <div key={section.id} className="animate-fadeIn">
                        <div className="flex items-center justify-between mb-4 border-b border-border-light pb-2">
                            <h4 className="font-display text-md font-bold text-ink-900 uppercase tracking-wide">
                                {section.label || section.sectionType}
                            </h4>
                            <button
                                onClick={() => onAddRow(section.id)}
                                className="text-xs font-bold text-primary hover:bg-primary-light px-2 py-1.5 rounded transition-colors flex items-center gap-1"
                            >
                                <span className="material-symbols-outlined text-[14px]">add</span>
                                Add Row
                            </button>
                        </div>

                        <div className="space-y-4">
                            {getRows(section.id).map((row, idx) => (
                                <div key={row.id} className="relative group bg-slate-50 border border-border-light rounded-lg p-3 hover:border-primary-light transition-colors">
                                    <div className="absolute -left-3 top-3 w-6 h-6 bg-white border border-border-light rounded-full flex items-center justify-center text-[10px] font-bold text-ink-400 shadow-sm z-10">
                                        {idx + 1}
                                    </div>

                                    {/* Action Header (on hover) */}
                                    <div className="absolute top-2 right-2 opacity-0 group-hover:opacity-100 transition-opacity flex gap-1">
                                        <button
                                            className="p-1 hover:bg-white rounded text-ink-400 hover:text-primary"
                                            title="Move Up"
                                        >
                                            <span className="material-symbols-outlined text-[16px]">arrow_upward</span>
                                        </button>
                                        <button
                                            className="p-1 hover:bg-white rounded text-ink-400 hover:text-primary"
                                            title="Move Down"
                                        >
                                            <span className="material-symbols-outlined text-[16px]">arrow_downward</span>
                                        </button>
                                        <button
                                            onClick={() => onDeleteRow(row.id)}
                                            className="p-1 hover:bg-red-50 rounded text-ink-400 hover:text-red-600"
                                            title="Delete Row"
                                        >
                                            <span className="material-symbols-outlined text-[16px]">delete</span>
                                        </button>
                                    </div>

                                    <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mt-2">
                                        <div>
                                            <label className="text-[10px] font-bold text-ink-400 uppercase mb-1 block">Swara</label>
                                            <textarea
                                                className="w-full p-2 text-sm font-mono bg-white border border-border-light rounded focus:ring-1 focus:ring-primary focus:border-primary resize-none"
                                                rows={2}
                                                value={row.swaraText}
                                                onChange={(e) => onUpdateRow(row.id, { swaraText: e.target.value })}
                                                placeholder="s r g m..."
                                            />
                                        </div>
                                        <div>
                                            <label className="text-[10px] font-bold text-ink-400 uppercase mb-1 block">Sahitya (Optional)</label>
                                            <textarea
                                                className="w-full p-2 text-sm font-serif bg-white border border-border-light rounded focus:ring-1 focus:ring-primary focus:border-primary resize-none"
                                                rows={2}
                                                value={row.sahityaText || ''}
                                                onChange={(e) => onUpdateRow(row.id, { sahityaText: e.target.value })}
                                                placeholder="Lyrics..."
                                            />
                                        </div>
                                    </div>
                                    <div className="mt-2">
                                        <label className="text-[10px] font-bold text-ink-400 uppercase mb-1 block">Tala Markers (Optional)</label>
                                        <input
                                            type="text"
                                            className="w-full p-1.5 text-xs font-mono bg-white border border-border-light rounded focus:ring-1 focus:ring-primary focus:border-primary"
                                            value={row.talaMarkers || ''}
                                            onChange={(e) => onUpdateRow(row.id, { talaMarkers: e.target.value })}
                                            placeholder="| , , ||"
                                        />
                                    </div>
                                </div>
                            ))}
                            {getRows(section.id).length === 0 && (
                                <div className="text-center py-4 border-2 border-dashed border-slate-200 rounded-lg text-ink-400 text-xs">
                                    No rows in this section. Add one to start.
                                </div>
                            )}
                        </div>
                    </div>
                ))}
            </div>
        </div>
    );
};

export default NotationRowsEditor;
