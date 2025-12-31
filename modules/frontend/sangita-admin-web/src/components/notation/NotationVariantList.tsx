import React, { useState } from 'react';
import { NotationVariant, NotationType } from '../../types';

interface NotationVariantListProps {
    variants: NotationVariant[];
    selectedVariantId: string | null;
    onSelectVariant: (variant: NotationVariant) => void;
    onAddVariant: () => void;
    onEditVariant: (variant: NotationVariant) => void;
    onDeleteVariant: (variantId: string) => void;
    onSetPrimary: (variantId: string) => void;
}

const NotationVariantList: React.FC<NotationVariantListProps> = ({
    variants,
    selectedVariantId,
    onSelectVariant,
    onAddVariant,
    onEditVariant,
    onDeleteVariant,
    onSetPrimary
}) => {
    return (
        <div className="bg-surface-light border border-border-light rounded-xl shadow-sm h-full flex flex-col">
            <div className="p-4 border-b border-border-light flex justify-between items-center">
                <h3 className="font-display text-lg font-bold text-ink-900">Variants</h3>
                <button
                    onClick={onAddVariant}
                    className="flex items-center gap-1.5 px-3 py-1.5 bg-primary text-white rounded-lg text-xs font-bold hover:bg-primary-dark transition-colors"
                >
                    <span className="material-symbols-outlined text-[16px]">add</span>
                    Add
                </button>
            </div>
            <div className="flex-1 overflow-y-auto p-4 space-y-3">
                {variants.length === 0 && (
                    <div className="text-center py-8 text-ink-500 text-sm">
                        No notation variants yet. <br /> Click "Add" to create one.
                    </div>
                )}
                {variants.map(variant => (
                    <div
                        key={variant.id}
                        onClick={() => onSelectVariant(variant)}
                        className={`p-4 rounded-lg border cursor-pointer transition-all group ${selectedVariantId === variant.id
                            ? 'bg-primary-light border-primary ring-1 ring-primary'
                            : 'bg-white border-border-light hover:border-slate-300'
                            }`}
                    >
                        <div className="flex justify-between items-start mb-2">
                            <div>
                                <h4 className="font-bold text-ink-900 text-sm">{variant.variantLabel || 'Untitled Variant'}</h4>
                                <span className="text-xs text-ink-500 flex items-center gap-1 mt-0.5">
                                    <span className={`w-2 h-2 rounded-full ${variant.notationType === NotationType.SWARA ? 'bg-orange-400' : 'bg-purple-400'}`}></span>
                                    {variant.notationType === NotationType.SWARA ? 'Swara' : 'Jathi'}
                                </span>
                            </div>
                            {variant.isPrimary && (
                                <span className="px-2 py-0.5 bg-green-50 text-green-700 text-[10px] font-bold rounded uppercase border border-green-100">
                                    Primary
                                </span>
                            )}
                        </div>
                        <p className="text-xs text-ink-400 truncate mb-3">{variant.sourceReference || 'No source ref'}</p>

                        <div className="flex items-center gap-2 pt-2 border-t border-slate-100 opacity-60 group-hover:opacity-100 transition-opacity">
                            <button
                                onClick={(e) => { e.stopPropagation(); onEditVariant(variant); }}
                                className="p-1.5 text-ink-500 hover:text-primary hover:bg-slate-50 rounded"
                                title="Edit Metadata"
                            >
                                <span className="material-symbols-outlined text-[16px]">edit</span>
                            </button>
                            <button
                                onClick={(e) => { e.stopPropagation(); onSetPrimary(variant.id); }}
                                className={`p-1.5 rounded ${variant.isPrimary ? 'text-green-600 bg-green-50' : 'text-ink-500 hover:text-green-600 hover:bg-green-50'}`}
                                title="Toggle Primary"
                            >
                                <span className="material-symbols-outlined text-[16px]">verified</span>
                            </button>
                            <div className="flex-1"></div>
                            <button
                                onClick={(e) => { e.stopPropagation(); onDeleteVariant(variant.id); }}
                                className="p-1.5 text-ink-500 hover:text-red-600 hover:bg-red-50 rounded"
                                title="Delete Variant"
                            >
                                <span className="material-symbols-outlined text-[16px]">delete</span>
                            </button>
                        </div>
                    </div>
                ))}
            </div>
        </div>
    );
};

export default NotationVariantList;
